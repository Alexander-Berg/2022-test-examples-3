package ru.yandex.market.adv.promo;

import java.util.Collections;
import java.util.TimeZone;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.adv.promo.config.SpringApplicationConfig;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.common.test.mockito.MockitoTestExecutionListener;

import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                SpringApplicationConfig.class
        }
)
@TestExecutionListeners(listeners = {
        DbUnitTestExecutionListener.class,
        MockitoTestExecutionListener.class
}, mergeMode = MERGE_WITH_DEFAULTS)
@ActiveProfiles(profiles = {"functionalTest", "development"}) // development для RAMSchedulerFactoryConfig
@TestPropertySource("classpath:functional-test.properties")
public class FunctionalTest {
    @LocalServerPort
    protected int port;

    @BeforeEach
    void setDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"));
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    protected HttpHeaders getDefaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
