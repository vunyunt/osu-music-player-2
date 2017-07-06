package com.vunyunt.omp.ui;

import java.awt.MenuBar;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.vunyunt.omp.persistence.AppConfig;
import com.vunyunt.omp.persistence.PersistenceManager;
import com.vunyunt.omp.persistence.library.Music;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;

public class Main extends Application
{
	public static void main(String[] args)
	{
		launch(args);
	}

	private static final Logger LOGGER = Logger.getLogger(Application.class);
	private PersistenceManager mPersistence = PersistenceManager.getInstance();

	private AppConfig mAppConfig;

	private Stage mStage;

	@Override
	public void start(Stage primaryStage)
	{
		mStage = primaryStage;

		LOGGER.info("Application started.");
		LOGGER.info("Initializing persistence...");
		try
		{
			mPersistence.initialize(this::chooseOsuPath);
		}
		catch (IllegalArgumentException | IOException e1)
		{
			LOGGER.debug(e1);
			LOGGER.fatal("Unable to initialize persistence.");
			new Alert(AlertType.ERROR, "Unable to initialize persistence.").showAndWait();
			Platform.exit();
		}

		mAppConfig = mPersistence.getAppConfig();

		try
		{
			AnchorPane root = FXMLLoader.load(getClass().getResource("MainWindow.fxml"));

			Scene scene = new Scene(root, mAppConfig.windowWidth, mAppConfig.windowHeight);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);

			initStageEvents(primaryStage);

			primaryStage.show();
		}
		catch(Exception e)
		{
			LOGGER.fatal("Unable to initialize javafx");
			LOGGER.error(e.getClass().getName() + e.getMessage());
			new Alert(AlertType.ERROR, "Unable to initialize javafx.").showAndWait();
			Platform.exit();
		}
	}

	private File chooseOsuPath()
	{
		DirectoryChooser chooser = new DirectoryChooser();
		return chooser.showDialog(mStage);
	}

	private void initStageEvents(Stage stage)
	{
		stage.setOnCloseRequest(this::onClose);
	}

	public void onClose(WindowEvent e)
	{
		mAppConfig.windowWidth = mStage.getScene().widthProperty().doubleValue();
		mAppConfig.windowHeight = mStage.getScene().heightProperty().doubleValue();

		mPersistence.saveConfig();
	}
}
