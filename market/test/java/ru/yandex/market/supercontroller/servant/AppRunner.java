package ru.yandex.market.supercontroller.servant;

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.utils.LogSignalHandler;


public abstract class AppRunner {
	public static void main(Class<?> clazz, String[] args) throws Exception {
		Properties props = loadProperties();
		final String propertiesPrefix = firstNonEmpty(props, "properties.prefix", "SERVANT_NAME", "");
		initLogger(propertiesPrefix, props);
		try {
			final String beanFile = firstNonEmpty(props, propertiesPrefix + ".bean.file", "bean.xml");

			ApplicationContext ctx = new ClassPathXmlApplicationContext(beanFile);
			AutowireCapableBeanFactory factory = ctx.getAutowireCapableBeanFactory();
			Runnable app = (Runnable) factory.createBean(clazz, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
			app.run();

		} catch(Throwable e) {
			System.err.println("Application crushed");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public abstract void run();

	/** Find the first non-empty value in properties, if none found - return last value from names array. */
	private static String firstNonEmpty(Properties props, String... names) {
		for (int i = 0; i < names.length - 1; ++i)
			if (props.containsKey(names[i]))
				return props.getProperty(names[i]);
		return names[names.length-1];
	}

	public static void initLogger(final String propertiesPrefix, final Properties props) {
		final String logFile = props.getProperty(propertiesPrefix + ".log.file");
		final String log4jConfig = "log4j-config.xml";
		LogSignalHandler.startLog(logFile, log4jConfig);
	}

	public static Properties loadProperties() {
		try {
			Properties properties = new Properties();
			properties.putAll(System.getenv());
			properties.putAll(System.getProperties());
			properties.load(
			  new ClassPathResource(System.getProperty("properties.file")).getInputStream());
			System.out.println(properties);
			return properties;
		} catch (IOException e) {
			throw new RuntimeException("Could not laod properties from file");
		}
	}
}
