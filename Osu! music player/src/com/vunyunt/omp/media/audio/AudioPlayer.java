package com.vunyunt.omp.media.audio;

import java.io.File;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import com.vunyunt.omp.Callback;
import com.vunyunt.omp.persistence.PersistenceManager;
import com.vunyunt.omp.persistence.library.Music;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

public class AudioPlayer
{
	private static final int FADE_MILLIS = 500;

	private PersistenceManager mPersistenceManager = PersistenceManager.getInstance();

	private File mMusicsPath;
	private MediaPlayer mMediaPlayer;
	private MediaPlayer mBackPlayer;
	private Music mCurrentlyPlaying;

	private int mFadeMillis = FADE_MILLIS;

	/**
	 * Duration of the music, in milliseconds
	 */
	private DoubleProperty mLength;

	/**
	 * Playback progress, in milliseconds elapsed
	 */
	private DoubleProperty mPlaybackProgress;
	private ChangeListener<Duration> mPlaybackProgressListener;

	public AudioPlayer()
	{
		mMusicsPath = new File(mPersistenceManager.getMusicsBasePath());
		mPlaybackProgress = new SimpleDoubleProperty();
		mLength = new SimpleDoubleProperty();

		mPlaybackProgressListener = new ChangeListener<Duration>()
		{
			@Override
			public void changed(ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue)
			{
				mPlaybackProgress.set(newValue.toMillis());
			}
		};
	}

	/**
	 * Plays a music
	 */
	public void play(Music music)
	{
		if(mMediaPlayer != null)
		{
			if(mCurrentlyPlaying == music)
			{
				playAndFadeIn(mMediaPlayer, mFadeMillis);
				return;
			}
			else
			{
				mMediaPlayer.currentTimeProperty().removeListener(mPlaybackProgressListener);
				fadeOutAndStop(mMediaPlayer, mFadeMillis);
			}
		}

		mBackPlayer = createPlayer(music);
		mBackPlayer.play();
		mCurrentlyPlaying = music;
		swapPlayers();
		bindProgress(mMediaPlayer);
	}

	private void bindProgress(MediaPlayer player)
	{
		player.currentTimeProperty().addListener(mPlaybackProgressListener);
		player.setOnReady(new Runnable()
		{
			@Override
			public void run()
			{
				mLength.set(mMediaPlayer.getMedia().getDuration().toMillis());
				mMediaPlayer.currentTimeProperty().addListener(mPlaybackProgressListener);
			}
		});
	}

	private MediaPlayer createPlayer(Music music)
	{
		File musicFile = getFile(music);
		MediaPlayer player = new MediaPlayer(new Media(musicFile.toURI().toASCIIString()));
		mPlayerFaderTimer.put(player, new Timer());
		return player;
	}

	private void swapPlayers()
	{
		MediaPlayer mp = mMediaPlayer;
		mMediaPlayer = mBackPlayer;
		mBackPlayer = mp;
	}

	private void playAndFadeIn(MediaPlayer player, int timeMilli)
	{
		player.setVolume(0);
		player.play();
		fadeIn(player, timeMilli, null);
	}

	private void fadeOutAndStop(MediaPlayer player, int timeMilli)
	{
		fadeOut(player, timeMilli, new Callback()
		{
			@Override
			public void call()
			{
				player.stop();
			}
		});
	}

	/**
	 * Each media player is bound to one and only one timer for fading
	 */
	private HashMap<MediaPlayer, Timer> mPlayerFaderTimer = new HashMap<>();

	/**
	 * Fades a player out.
	 *
	 * @param player	MediaPlayer to fade out
	 * @param timeMilli Duration of the fade in milliseconds
	 * @param completed Callback to call once the fading is complete
	 */
	private void fadeOut(MediaPlayer player, int timeMilli, Callback completed)
	{
		long startTime = System.nanoTime();
		mPlayerFaderTimer.get(player).cancel();
		Timer t = new Timer();
		mPlayerFaderTimer.put(player, t);
		t.scheduleAtFixedRate(new TimerTask()
		{
			@Override
			public void run()
			{
				long currentTime = System.nanoTime();
				double elapsedTime = (currentTime - startTime) / 1000000;
				player.setVolume(Math.max(1 - (double)(elapsedTime / timeMilli), 0));
				if(elapsedTime > timeMilli)
				{
					if(completed != null)
					{
						completed.call();
					}
					t.cancel();
				}
			}
		}, 0, 50);
	}

	/**
	 * Fades a player in.
	 *
	 * @param player	MediaPlayer to fade in
	 * @param timeMilli Duration of the fade in milliseconds
	 * @param completed Callback to call once the fading is complete
	 */
	private void fadeIn(MediaPlayer player, int timeMilli, Callback completed)
	{
		long startTime = System.nanoTime();
		mPlayerFaderTimer.get(player).cancel();
		Timer t = new Timer();
		mPlayerFaderTimer.put(player, t);
		t.scheduleAtFixedRate(new TimerTask()
		{
			@Override
			public void run()
			{
				long currentTime = System.nanoTime();
				double elapsedTime = (currentTime - startTime) / 1000000;
				player.setVolume(Math.max((double)(elapsedTime / timeMilli), 0));
				if(elapsedTime > timeMilli)
				{
					if(completed != null)
					{
						completed.call();
					}
					t.cancel();
				}
			}
		}, 0, 50);
	}

	public void pause()
	{
		if(mMediaPlayer != null)
		{
			mMediaPlayer.pause();
		}
	}

	public void stop()
	{
		this.fadeOutAndStop(mMediaPlayer, mFadeMillis);
	}

	/**
	 * Locks seeking when not ready
	 * true = locked.
	 */
	private boolean mSeeklock = false;

	public void seek(double milli)
	{
		if(!mSeeklock)
		{
			mBackPlayer = createPlayer(mCurrentlyPlaying);

			mMediaPlayer.currentTimeProperty().removeListener(mPlaybackProgressListener);
			fadeOutAndStop(mMediaPlayer, mFadeMillis);
			mSeeklock = true;

			mBackPlayer.setOnReady(new Runnable()
			{
				@Override
				public void run()
				{
					mBackPlayer.seek(new Duration(milli));
					playAndFadeIn(mBackPlayer, mFadeMillis);
					swapPlayers();
					bindProgress(mMediaPlayer);
					mSeeklock = false;
				}
			});
		}

	}

	/**
	 * Gets the audio file of the given music
	 */
	private File getFile(Music music)
	{
		return new File(mMusicsPath, music.getFilePath());
	}

	/**
	 * {@link AudioPlayer#mPlaybackProgress}
	 */
	public DoubleProperty getPlaybackProgress()
	{
		return mPlaybackProgress;
	}

	/**
	 * {@link AudioPlayer#mLength}
	 */
	public DoubleProperty getLengthProperty()
	{
		return mLength;
	}
}
