package ru.yandex.market.common.mds.s3.client.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.annotation.Nonnull;

/**
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
public final class TestProperties {

    public static final TestProperties PROPS = new TestProperties();

    private static final String PROP_ACCESS_KEY = "access.key";
    private static final String PROP_SECRET_KEY = "secret.key";
    private static final String PROP_ENDPOINT = "endpoint";
    private static final String PROP_TEST_BUCKET_NAME = "test.bucket.name";
    private static final String PROPERTIES_FILE = "/test.properties";

    private final Properties properties;


    private TestProperties() {
        properties = load();
    }


    @Nonnull
    public String endpoint() {
        return get(PROP_ENDPOINT);
    }

    @Nonnull
    public String secretKey() {
        return get(PROP_SECRET_KEY);
    }

    @Nonnull
    public String accessKey() {
        return get(PROP_ACCESS_KEY);
    }

    @Nonnull
    public String bucketName() {
        return get(PROP_TEST_BUCKET_NAME);
    }

    private String get(final String key) {
        return properties.getProperty(key);
    }

    private static Properties load() {
        final Properties prop = new Properties();
        try (InputStream stream = TestProperties.class.getResourceAsStream(PROPERTIES_FILE)) {
            prop.load(stream);
            return prop;
        } catch (final IOException ex) {
            throw new RuntimeException("Could not load properties", ex);
        }
    }

}
