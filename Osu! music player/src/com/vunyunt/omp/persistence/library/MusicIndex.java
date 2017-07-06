package com.vunyunt.omp.persistence.library;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.surround.parser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.QueryBuilder;

import javafx.collections.ObservableList;

public class MusicIndex
{
	private static final Logger LOGGER = Logger.getLogger(MusicIndex.class);

	public static final String FIELD_NAME = "NAME";
	public static final String FIELD_AUDIO_FILENAME = "AUDIO_FILENAME";
	public static final String FIELD_ID = "ID";

	private Analyzer mAnalyzer;
	private Directory mIndex;

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
	 * @throws IOException
	 */
	public void commit() throws IOException
	{
		IndexWriter writer = new IndexWriter(mIndex, new IndexWriterConfig(mAnalyzer));

		for (String k : mPendingMusics.keySet())
		{
			Music m = mPendingMusics.get(k);

			Document doc = new Document();

			doc.add(new TextField(FIELD_NAME, m.getName(), Field.Store.YES));
			doc.add(new TextField(FIELD_AUDIO_FILENAME, m.getFilePath(), Field.Store.YES));
			doc.add(new StringField(FIELD_ID, m.getId(), Field.Store.YES));

			writer.updateDocument(new Term(FIELD_ID, m.getId()), doc);
		}

		writer.close();
		mPendingMusics.clear();
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
			TopDocs result = searcher.search(new TermQuery(new Term(FIELD_ID, music.getId())), 1);
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
			allMusics.add(new Music(d.get(FIELD_ID), d.get(FIELD_NAME), d.get(FIELD_AUDIO_FILENAME)));
		}

		return allMusics;
	}

	public List<Music> getMusic(String searchQuery, int resultsToShow) throws IOException
	{
		IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(mIndex));

		List<Music> matches = new ArrayList<Music>();

		Query q = new QueryBuilder(mAnalyzer).createPhraseQuery(FIELD_NAME, searchQuery);
		TopDocs docs = searcher.search(q, resultsToShow);

		for(ScoreDoc doc : docs.scoreDocs)
		{
			Document d = searcher.doc(doc.doc);
			matches.add(new Music(d.get(FIELD_ID), d.get(FIELD_NAME), d.get(FIELD_AUDIO_FILENAME)));
		}

		return matches;
	}

	/**
	 * Remove the indexed music from the index
	 * @throws IOException
	 */
	public void remove(Music music) throws IOException
	{
		IndexWriter writer = new IndexWriter(mIndex, new IndexWriterConfig(mAnalyzer));
		writer.deleteDocuments(new TermQuery(new Term(FIELD_ID, music.getId())));
		writer.close();
	}
}
