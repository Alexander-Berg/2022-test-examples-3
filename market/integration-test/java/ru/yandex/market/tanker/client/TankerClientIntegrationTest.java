package ru.yandex.market.tanker.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.http.impl.client.HttpClients;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.market.tanker.client.model.KeySetTranslation;
import ru.yandex.market.tanker.client.model.Language;
import ru.yandex.market.tanker.client.request.TranslationRequestBuilder;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;

/**
 * @author Vadim Lyalin
 */
public class TankerClientIntegrationTest {
    private static final String PROPERTIED_FILE_NAME = "/test.properties";
    private static final String TANKER_URL_KEY = "tanker.url";
    private static Properties properties = new Properties();
    private TankerClient tankerClient;

    @BeforeClass
    public static void onBeforeClass() {
        try (InputStream is = TankerClientIntegrationTest.class.getResourceAsStream(PROPERTIED_FILE_NAME)) {
            properties.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Could not load properties", e);
        }
    }

    @Before
    public void setUp() throws Exception {
        tankerClient = new TankerClient(null, HttpClients.createDefault(), properties.getProperty(TANKER_URL_KEY));
    }

    @Test
    public void testKeySet() throws IOException {
        KeySetTranslation translation = tankerClient.translations().keySet(new TranslationRequestBuilder()
                .withProject("market-partner")
                .withKeySet("common.urls")
                .withLanguage(Language.RU)
                .withFlatKaySet()
                .withUnapproved());
        assertThat(translation.getKeySet(Language.RU).getKeys(), hasItem("help.about-api"));
    }

    @Test/*(expected = TankerClientException.class)*/
    public void testIncorrectQuery() throws IOException {
        /*
        try {
            tankerClient.translations().keySet(
                    new TranslationRequestBuilder().withProject("fake-project").withKeySet("fake-keyset")
            );
            Assert.fail();
        } catch (TankerClientException e) {
            Assert.assertTrue(e.getMessage().contains("DOES_NOT_EXIST"));
        }
         */
    }
}
