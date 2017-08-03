package com.vunyunt.omp.ui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.vunyunt.omp.media.audio.AudioPlayer;
import com.vunyunt.omp.persistence.AppConfig;
import com.vunyunt.omp.persistence.PersistenceManager;
import com.vunyunt.omp.persistence.library.Music;
import com.vunyunt.omp.persistence.library.OsuMusicLibrary;
import com.vunyunt.omp.visualization.VisualizationEngine;
import com.vunyunt.omp.visualization.storyboard.StoryboardVE;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;

public class MainWindow implements Initializable
{
	private static final Logger LOGGER = Logger.getLogger(MainWindow.class);

	private PersistenceManager mPersistence = PersistenceManager.getInstance();
	private OsuMusicLibrary mMusicLibrary = mPersistence.getMusicLibrary();

	private AudioPlayer mAudioPlayer;
	private StoryboardVE mStoryboard;

	@FXML AnchorPane mRoot;
	@FXML ListView<Music> mMusicListView;
	@FXML Button mPlayBtn;
	@FXML Button mPauseBtn;
	@FXML Button mStopBtn;
	@FXML Slider mPlaybackProgress;
	@FXML TextField mSearchText;
	@FXML Canvas mVisualizationCanvas;

	/**
	 * Indicates if the change in the playback progress slider is caused by progress tracking
	 */
	private boolean progressTracking = false;

	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
		mAudioPlayer = new AudioPlayer();
		mStoryboard = new StoryboardVE(mVisualizationCanvas);

		mMusicListView.setCellFactory(new Callback<ListView<Music>, ListCell<Music>>()
		{
			@Override
			public ListCell<Music> call(ListView<Music> param)
			{
				ListCell<Music> cell = new ListCell<Music>()
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
							try
							{
								setText(item.getName());
							}
							catch(IllegalStateException ex)
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
					}
				};

				return cell;
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
					if(mAudioPlayer != null) mAudioPlayer.seek(newValue.doubleValue());
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
	public void onChooseOsuPath(ActionEvent e)
	{
		AppConfig cfg = mPersistence.getAppConfig();
		cfg.osuPath = chooseOsuPath().getAbsolutePath();
		cfg.clearLucene = true;
		new Alert(AlertType.INFORMATION, "The application needs to be restart to apply changes. Closing application now.", ButtonType.OK).showAndWait();
		mPersistence.saveConfig();
		Platform.exit();
		System.exit(0);
	}

	private File chooseOsuPath()
	{
		DirectoryChooser chooser = new DirectoryChooser();
		return chooser.showDialog(mRoot.getScene().getWindow());
	}

	@FXML
	public void onListClicked(MouseEvent e)
	{
		if(e.getClickCount() > 1)
		{
			this.onPlayAction(null);
		}
	}

	@FXML
	public void onPlayAction(ActionEvent e)
	{
		Music m = mMusicListView.getSelectionModel().getSelectedItem();
		if(m != null)
		{
			mAudioPlayer.play(m);
			try
			{
				mStoryboard.loadStoryboard(m);
			}
			catch (IOException e1)
			{
				LOGGER.debug(e1.getMessage());
			}
			mStoryboard.play(mAudioPlayer.getPlaybackProgress());
		}
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
		if(query.trim().length() > 0)
		{
			this.mMusicListView.setItems(mPersistence.getMusicLibrary().search(query));
		}
		else
		{
			this.mMusicListView.setItems(this.mMusicLibrary.getMusicsObservable());
		}
	}
}
