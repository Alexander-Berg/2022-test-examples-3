package ru.yandex.market.reporting.generator.indexer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.reporting.generator.indexer.session.AuditReport;
import ru.yandex.market.reporting.generator.indexer.session.FeedSessions;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Aleksandr Kormushin &lt;kormushin@yandex-team.ru&gt;
 */
@ContextConfiguration(classes = IndexerApiServiceTest.IndexerApiServiceTestConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class IndexerApiServiceTest {

    @Inject
    private CloseableHttpClient httpClient;

    @Inject
    private IndexerApiService indexerApiService;

    @Ignore
    @Test
    public void getFeeds() throws Exception {
        when(httpClient.execute(any(HttpGet.class))).thenAnswer(i -> {
            CloseableHttpResponse response = mock(CloseableHttpResponse.class);

            when(response.getStatusLine()).thenReturn(
                    new BasicStatusLine(new ProtocolVersion("HTTP/1.1", 1, 1), HttpStatus.SC_OK, "OK"));
            HttpEntity httpEntity = mock(HttpEntity.class);
            when(httpEntity.getContent()).thenAnswer(i1 ->
                    new ReaderInputStream(new BufferedReader(new InputStreamReader(
                            IndexerApiServiceTest.class.getResourceAsStream("/feed_1069_sessions_published.json"))), StandardCharsets.UTF_8));
            when(response.getEntity()).thenReturn(httpEntity);

            return response;
        });

        FeedSessions feedSessions = indexerApiService.getFeedSessions(1069L);
        assertThat(feedSessions.getFeedSessions().size(), is(12));
    }

    @Test
    public void testGetFeedParserAuditReport() throws IOException {
        AuditReport auditReport = indexerApiService.getFeedParserAuditReport(
                IOUtils.toString(IndexerApiServiceTest.class.getResourceAsStream("/audit_report_process.log"), Charset.defaultCharset()));

        assertThat(auditReport, notNullValue());
        assertThat(auditReport.getTotalOffers(), is(2331));
    }

    @Configuration
    @ComponentScan(basePackages = "ru.yandex.market.reporting.generator.indexer")
    @TestPropertySource({"classpath:indexer-api-integration-tests.properties"})
    public static class IndexerApiServiceTestConfig {
        @Bean
        public CloseableHttpClient httpClient() {
            return mock(CloseableHttpClient.class);
        }
    }
}
