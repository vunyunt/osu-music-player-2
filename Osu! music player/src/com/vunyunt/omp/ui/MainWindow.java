package com.vunyunt.omp.ui;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.vunyunt.omp.media.audio.AudioPlayer;
import com.vunyunt.omp.persistence.PersistenceManager;
import com.vunyunt.omp.persistence.library.Music;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.util.Callback;

public class MainWindow implements Initializable
{
	private static final Logger LOGGER = Logger.getLogger(MainWindow.class);

	private PersistenceManager mPersistence = PersistenceManager.getInstance();

	private AudioPlayer mAudioPlayer;

	@FXML ListView<Music> mMusicListView;
	@FXML Button mPlayBtn;
	@FXML Button mPauseBtn;
	@FXML Button mStopBtn;
	@FXML Slider mPlaybackProgress;
	@FXML TextField mSearchText;

	/**
	 * Indicates if the change in the playback progress slider is caused by progress tracking
	 */
	private boolean progressTracking = false;

	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
		mAudioPlayer = new AudioPlayer();

		mMusicListView.setCellFactory(new Callback<ListView<Music>, ListCell<Music>>()
		{
			@Override
			public ListCell<Music> call(ListView<Music> param)
			{
				return new ListCell<Music>()
				{
					@Override
					public void updateItem(Music item, boolean empty)
					{
						super.updateItem(item, empty);
						if(empty || item == null)
						{
							setText(null);
							setGraphic(null);
						}
						else
						{
							Platform.runLater(new Runnable()
							{
								@Override
								public void run()
								{
									setText(item.getName());
								}
							});
						}
					}
				};
			}
		});
		mMusicListView.setItems(mPersistence.getMusicLibrary().getMusicsObservable());

		mPlaybackProgress.maxProperty().bind(mAudioPlayer.getLengthProperty());
		mAudioPlayer.getPlaybackProgress().addListener(new ChangeListener<Number>()
		{
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
			{
				progressTracking = true;
				mPlaybackProgress.setValue(newValue.doubleValue());
				progressTracking = false;
			}
		});

		mPlaybackProgress.valueProperty().addListener(new ChangeListener<Number>()
		{
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
			{
				if(!progressTracking)
				{
					mAudioPlayer.seek(newValue.doubleValue());
				}
			}
		});

		mSearchText.textProperty().addListener(new ChangeListener<String>()
		{
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
			{
				search(newValue);
			}
		});
	}

	@FXML
	public void onPlayAction(ActionEvent e)
	{
		mAudioPlayer.play(mMusicListView.getSelectionModel().getSelectedItem());
	}

	@FXML
	public void onPauseAction(ActionEvent e)
	{
		mAudioPlayer.pause();
	}

	@FXML
	public void onStopAction(ActionEvent e)
	{
		mAudioPlayer.stop();
	}

	private void search(String query)
	{
		mPersistence.getMusicLibrary().search(query);
	}
}
