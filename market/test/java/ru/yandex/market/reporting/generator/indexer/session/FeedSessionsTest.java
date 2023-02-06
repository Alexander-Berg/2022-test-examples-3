package ru.yandex.market.reporting.generator.indexer.session;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author nettoyeur
 * @since 01.12.2017
 */
public class FeedSessionsTest {

    private static Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Test
    public void getLastActiveSessionShouldReturnLatestPublishedSession() throws Exception {
        FeedSessions feedSessions = readFeedSessions("feed_1069_sessions_published.json");
        assertLastSession(feedSessions.getLastActiveSession(), "20180116_0658");
    }

    private void assertLastSession(FeedSession lastActiveSession, String expectedSessionName) {
        assertThat(lastActiveSession, notNullValue(FeedSession.class));
        assertThat(lastActiveSession.getDataPublished(), is("True"));
        assertThat(lastActiveSession.getSession(), is(expectedSessionName));
    }

    private FeedSessions readFeedSessions(String testFile) throws IOException {
        try (InputStream inputStream = FeedSessionsTest.class.getResourceAsStream("/" + testFile)) {
            PublishedSession publishSession = gson.fromJson(IOUtils.toString(inputStream, StandardCharsets.UTF_8), PublishedSession.class);
            return FeedSessions.from(publishSession);
        }
    }
}
