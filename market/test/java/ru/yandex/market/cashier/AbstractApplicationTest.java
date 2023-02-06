package ru.yandex.market.cashier;

import com.opentable.db.postgres.embedded.LiquibasePreparer;
import com.opentable.db.postgres.junit5.EmbeddedPostgresExtension;
import com.opentable.db.postgres.junit5.PreparedDbExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import ru.yandex.market.cashier.mocks.trust.TrustMockConfigurer;

@ExtendWith({SpringExtension.class})
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = TestConfiguration.class
)
@AutoConfigureMockMvc(secure = false)
@TestExecutionListeners({
        DBCleanerTestExecutionListener.class,
        DependencyInjectionTestExecutionListener.class,
        MockitoTestExecutionListener.class,
        ResetMocksTestExecutionListener.class,
        // можно помечать тест как транзакционный, чтобы транзакция начиналась в тесте
        TransactionalTestExecutionListener.class
})
public class AbstractApplicationTest {

    @RegisterExtension
    static PreparedDbExtension preparedDbExtension = EmbeddedPostgresExtension.preparedDatabase(
            LiquibasePreparer.forClasspathLocation("changelog.xml"));

    @Autowired
    protected TrustMockConfigurer trustMockConfigurer;

    @BeforeEach
    public void initMocks() {
        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockWholeTrust();
    }
}
