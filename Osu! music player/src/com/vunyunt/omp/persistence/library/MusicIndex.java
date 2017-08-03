package com.vunyunt.omp.persistence.library;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.QueryBuilder;

public class MusicIndex
{
	private static final Logger LOGGER = Logger.getLogger(MusicIndex.class);

	private Analyzer mAnalyzer;
	private Directory mIndex;
	private IndexWriter mWriter;

	/**
	 * Instantiate a music index using the given index file
	 *
	 * @param indexFile Index file for the music index
	 * @throws IOException
	 */
	protected MusicIndex(String indexFile) throws IOException
	{
		mAnalyzer = new SimpleAnalyzer();
		mIndex = new MMapDirectory(Paths.get(indexFile));
		mWriter = new IndexWriter(mIndex, new IndexWriterConfig(mAnalyzer));
	}

	/**
	 * A pending list of music to be added to the index
	 */
	private Map<String, Music> mPendingMusics = new HashMap<>();

	/**
	 * Adds a new music to the index.
	 * No change is made to the index until commit() is called.
	 *
	 * @param music Music to be added
	 * @return True if the music is added successfully, false if music already exists
	 */
	public boolean addNewMusic(Music music)
	{
		if(!exists(music))
		{
			mPendingMusics.put(music.getId(), music);
			return true;
		}

		return false;
	}

	/**
	 * Commit all pending music into the index
	 *
	 * @throws IOException
	 */
	public void commit() throws IOException
	{
		for (String k : mPendingMusics.keySet())
		{
			Music m = mPendingMusics.get(k);

			Document doc = new Document();

			Map<String, String> musicData = m.serializeToMap();
			// ID is added as string field as it needs to be matched exactly
			doc.add(new StringField(Music.FIELD_ID, Music.popFromMap(musicData, Music.FIELD_ID), Field.Store.YES));
			musicData.forEach(new BiConsumer<String, String>()
			{
				@Override
				public void accept(String k, String v)
				{
					doc.add(new TextField(k, v, Field.Store.YES));
				}
			});

			mWriter.updateDocument(new Term(Music.FIELD_ID, m.getId()), doc);
		}

		mWriter.commit();
		mPendingMusics.clear();
	}

	/**
	 * Converts a Lucene document to a Music object
	 */
	private Music documentToMusic(Document doc)
	{
		Map<String, String> musicData = new HashMap<String, String>();
		List<IndexableField> fields = doc.getFields();
		fields.forEach(new Consumer<IndexableField>()
		{
			@Override
			public void accept(IndexableField f)
			{
				musicData.put(f.name(), f.stringValue());
			}
		});
		return new Music(musicData);
	}

	/**
	 * Check if the given music already exists in this index or the pending list
	 *
	 * @throws IOException
	 */
	public boolean exists(Music music)
	{
		if(mPendingMusics.containsKey(music.getId()))
		{
			return true;
		}

		try
		{
			IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(mIndex));
			TopDocs result = searcher.search(new TermQuery(new Term(Music.FIELD_ID, music.getId())), 1);
			return result.totalHits == 1;
		}
		catch(IOException e)
		{
			return false;
		}
	}

	/**
	 * Gets all music in the index
	 *
	 * @return All music in the index
	 * @throws IOException
	 */
	public List<Music> getAllMusics() throws IOException
	{
		IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(mIndex));

		List<Music> allMusics = new ArrayList<>();

		MatchAllDocsQuery q = new MatchAllDocsQuery();
		TopDocs docs = searcher.search(q, Integer.MAX_VALUE);

		for (ScoreDoc doc : docs.scoreDocs)
		{
			Document d = searcher.doc(doc.doc);
			allMusics.add(this.documentToMusic(d));
		}

		return allMusics;
	}

	public List<Music> search(String searchQuery, int resultsToShow) throws IOException
	{
		IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(mIndex));

		List<Music> matches = new ArrayList<Music>();

		// Old searching method, only search one field
		// Query q = new QueryBuilder(mAnalyzer).createPhraseQuery(FIELD_NAME, searchQuery);

		try
		{
			StringBuilder querySb = new StringBuilder(searchQuery.trim());
			querySb.insert(0, "*");
			querySb.append("*");
			String finalQuery = querySb.toString();
			finalQuery.replace(" ", "* *");
			MultiFieldQueryParser parser = new MultiFieldQueryParser(
					new String[]{"Title", "AudioFilename", "Artist", "Tags"}, mAnalyzer);
			parser.setAllowLeadingWildcard(true);
			Query q = parser.parse(finalQuery);
			TopDocs docs = searcher.search(q, resultsToShow);

			for(ScoreDoc doc : docs.scoreDocs)
			{
				Document d = searcher.doc(doc.doc);
				matches.add(this.documentToMusic(d));
			}
		}
		catch (ParseException e)
		{
			LOGGER.error(this.getClass().getName() + ": Unable to parse query");
			LOGGER.debug(e);
		}

		return matches;
	}

	/**
	 * Remove the indexed music from the index
	 * This method is asynchronous.
	 *
	 * @throws IOException
	 */
	public void remove(Music music) throws IOException
	{
		mWriter.deleteDocuments(new TermQuery(new Term(Music.FIELD_ID, music.getId())));
	}

	/**
	 * Clears the index
	 *
	 * @throws IOException
	 */
	public void clear() throws IOException
	{
		IndexWriterConfig writerConfig = new IndexWriterConfig(mAnalyzer).setOpenMode(OpenMode.CREATE);
		mWriter = new IndexWriter(mIndex, writerConfig);
		mWriter.commit();
	}

	public void close()
	{
		try
		{
			mWriter.close();
		}
		catch (IOException e)
		{
			LOGGER.error("Unable to close index writer");
			LOGGER.debug(e.getMessage());
		}
	}
}
