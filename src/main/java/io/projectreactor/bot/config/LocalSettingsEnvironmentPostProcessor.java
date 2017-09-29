package io.projectreactor.bot.config;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.CommandLinePropertySource;
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
			if (propertySources.contains(
					CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME)) {
				propertySources.addAfter(
						CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME,
						new PropertiesPropertySource("reactor-bot-local", properties));
			}
			else {
				propertySources
						.addFirst(new PropertiesPropertySource("reactor-bot-local", properties));
			}
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