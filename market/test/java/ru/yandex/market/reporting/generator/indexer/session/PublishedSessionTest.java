package ru.yandex.market.reporting.generator.indexer.session;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author nettoyeur
 * @since 01.12.2017
 */
public class PublishedSessionTest {

    private static Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Test
    public void getLastActiveSessionShouldReturnLatestPublishedSession() throws Exception {
        PublishedSession publishedSession = readFeedSessions("feed_1069_sessions_published.json");
        assertThat(publishedSession, notNullValue(PublishedSession.class));
        assertThat(publishedSession.getFeedId(), is(1069L));
        assertThat(publishedSession.getMeta().getSessionName(), is("20180116_0658"));
        assertThat(publishedSession.getMeta().getFeedParser(), notNullValue(FeedParser.class));
        assertThat(publishedSession.getMeta().getParserStdout(), notNullValue(String.class));
        assertThat(publishedSession.getMeta().getParserStdout(), containsString("Audit report"));
    }

    private PublishedSession readFeedSessions(String testFile) throws IOException {
        try (InputStream inputStream = PublishedSessionTest.class.getResourceAsStream("/" + testFile)) {
            return gson.fromJson(IOUtils.toString(inputStream, StandardCharsets.UTF_8), PublishedSession.class);
        }
    }
}
