package ru.yandex.market.logistics.werewolf;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.logistics.werewolf.config.IntegrationTestConfiguration;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;

@ActiveProfiles("integration-test")
@SpringBootTest(
    classes = IntegrationTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "spring.config.name=integration-test"
)
@AutoConfigureMockMvc
@TestExecutionListeners({
    MockitoTestExecutionListener.class,
    ResetMocksTestExecutionListener.class,
    DependencyInjectionTestExecutionListener.class,
})
@ComponentScan({
    "ru.yandex.market.logistics.werewolf.config",
    "ru.yandex.market.logistics.werewolf.controller",
    "ru.yandex.market.logistics.werewolf.util",
    "ru.yandex.market.logistics.werewolf.facade",
    "ru.yandex.market.logistics.werewolf.converter",
})
@ExtendWith(SoftAssertionsExtension.class)
public abstract class AbstractTest {
    protected static final String REQUEST_ID = "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd";

    @Autowired
    protected MockMvc mockMvc;

    @InjectSoftAssertions
    protected SoftAssertions softly;

    @BeforeEach
    void setup() {
        RequestContextHolder.setContext(new RequestContext(REQUEST_ID));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.clearContext();
    }
}
