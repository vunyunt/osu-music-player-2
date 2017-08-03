package com.vunyunt.omp.persistence;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class AppConfig
{
	public transient static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	public transient static final String APP_CONFIG_FILE_PATH = "./AppConfig.json";

	public static AppConfig fromJson(Gson gson, String json)
	{
		return gson.fromJson(json, AppConfig.class);
	}

	public String toJson(Gson gson)
	{
		return gson.toJson(this);
	}

	public transient Charset defaultCharset = DEFAULT_CHARSET;
	public boolean clearLucene = false;
	public double windowWidth = 1280;
	public double windowHeight = 720;
	public String osuPath = "";
}
