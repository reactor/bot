/*
 * Copyright (c) 2017-2021 VMware Inc. or its affiliates, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.projectreactor.bot.config;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * @author Simon Baslé
 */
public class LocalSettingsEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private static final String LOCATION = ".reactor-bot/reactor-bot.properties";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment configurableEnvironment,
			SpringApplication springApplication) {
		File file = new File(System.getProperty("user.home"), LOCATION);
		if (file.exists()) {
			MutablePropertySources propertySources = configurableEnvironment.getPropertySources();
			System.out.println("Loading local settings from " + file.getAbsolutePath());
			Properties properties = loadProperties(file);
			propertySources.addBefore(
				"Config resource 'class path resource [application.properties]' via location 'optional:classpath:/'",
					new PropertiesPropertySource("reactor-bot-local", properties));
		}
		else {
			System.out.println("Could not find local settings, no " + file.getAbsolutePath());
		}
	}

	private Properties loadProperties(File f) {
		FileSystemResource resource = new FileSystemResource(f);
		try {
			return PropertiesLoaderUtils.loadProperties(resource);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to load local settings from " + f.getAbsolutePath(), ex);
		}
	}

}