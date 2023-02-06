package ru.yandex.market.wrap.infor.configuration;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase;
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = {IntegrationTestConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    ResetDatabaseTestExecutionListener.class,
    DbUnitTestExecutionListener.class,
    MockitoTestExecutionListener.class,
    ResetMocksTestExecutionListener.class,
})
@CleanDatabase
@TestPropertySource("classpath:integration-test.properties")
@DbUnitConfiguration(databaseConnection = {"wrapConnection", "wmsConnection", "secondWmsConnection"})
public abstract class AbstractContextualTest extends BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    protected final String httpOperationWithResult(MockHttpServletRequestBuilder httpOperation,
                                                   ResultMatcher... matchers) throws Exception {
        ResultActions resultActions = mockMvc.perform(httpOperation);
        for (ResultMatcher matcher : matchers) {
            resultActions.andExpect(matcher);
        }

        return resultActions
            .andReturn().getResponse()
            .getContentAsString();
    }
}
