package ru.yandex.market.security.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;

import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.security.util.http.LoggingHandlerWrapper;
import ru.yandex.market.util.CacheHeaterTestExecutionListener;

@SpringJUnitConfig(
        locations = "classpath*:functional-test-context.xml"
)
@TestExecutionListeners(CacheHeaterTestExecutionListener.class)
@DbUnitDataSet(nonTruncatedTables = {})
@ActiveProfiles("functionalTest")
public abstract class FunctionalTest extends JupiterDbUnitTest {
    @Autowired
    public LoggingHandlerWrapper loggingHandlerWrapper;
    @Autowired
    private String baseUrl;

    public URI getUrl(String endpoint) {
        return getUrl(endpoint, Collections.emptyMap());
    }

    public URI getUrl(String endpoint, Map<String, String> params) {
        try {
            var builder = new URIBuilder(baseUrl);
            builder.setPath(endpoint);
            params.forEach(builder::addParameter);
            return builder.build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}

