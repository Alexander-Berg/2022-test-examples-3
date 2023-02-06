package ru.yandex.market.logistics.tarifficator;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistics.tarifficator.configuration.IntegrationTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase;
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener;
import ru.yandex.market.logistics.test.integration.jpa.HibernateQueriesExecutionListener;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;

@ActiveProfiles("integration-test")
@ExtendWith({
    SpringExtension.class,
    SoftAssertionsExtension.class,
})
@SpringBootTest(
    classes = IntegrationTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    ResetDatabaseTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    DbUnitTestExecutionListener.class,
    MockitoTestExecutionListener.class,
    ResetMocksTestExecutionListener.class,
    HibernateQueriesExecutionListener.class,
})
@CleanDatabase
@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = {"dbUnitDatabaseConnection", "dbUnitQualifiedDatabaseConnection"}
)
public class AbstractContextualTest {

    public static final String REQUEST_ID = "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd";

    @Autowired
    protected MockMvc mockMvc;

    @InjectSoftAssertions
    protected SoftAssertions softly;

    @Autowired
    protected TestableClock clock;

    @BeforeEach
    public void createContext() {
        RequestContextHolder.setContext(new RequestContext(REQUEST_ID));
    }

    @AfterEach
    public void resetContext() {
        RequestContextHolder.clearContext();
        clock.clearFixed();
    }
}
