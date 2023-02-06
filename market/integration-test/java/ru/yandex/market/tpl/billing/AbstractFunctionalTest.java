package ru.yandex.market.tpl.billing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;
import ru.yandex.market.tpl.billing.config.IntegrationTestConfig;

@ExtendWith({
        SpringExtension.class
})
@SpringBootTest(
        classes = IntegrationTestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.config.name=integration-test"
)
@ActiveProfiles(profiles = "integration-test")
@AutoConfigureDataJpa
@AutoConfigureMockMvc
@EnableWebMvc
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        MockitoTestExecutionListener.class,
        ResetMocksTestExecutionListener.class,
})
@ComponentScan({
        "ru.yandex.market.tpl.billing.controller",
        "ru.yandex.market.tpl.billing.converter",
        "ru.yandex.market.tpl.billing.dao",
        "ru.yandex.market.tpl.billing.service",
        "ru.yandex.market.tpl.billing.queue",
        "ru.yandex.market.tpl.billing.checker",
        "ru.yandex.market.tpl.common.startrek",
        "ru.yandex.market.tpl.common.db",
        "ru.yandex.market.tpl.common.util",
})
public class AbstractFunctionalTest extends JupiterDbUnitTest {

    protected static final String REQUEST_ID = "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd";

    @Autowired
    protected MockMvc mockMvc;

    @BeforeEach
    public void beforeTest() {
        RequestContextHolder.setContext(new RequestContext(REQUEST_ID));
    }

    protected String asJsonString(Object requestBody) {
        try {
            return new ObjectMapper().writeValueAsString(requestBody);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
