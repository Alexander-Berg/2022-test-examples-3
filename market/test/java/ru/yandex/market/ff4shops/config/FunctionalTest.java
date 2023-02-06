package ru.yandex.market.ff4shops.config;

import java.sql.SQLException;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import ru.yoomoney.tech.dbqueue.api.Task;
import ru.yoomoney.tech.dbqueue.config.QueueShardId;

import ru.yandex.market.checkout.pushapi.client.PushApi;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.common.test.mockito.MockitoTestExecutionListener;
import ru.yandex.market.ff4shops.util.TestUrlBuilder;
import ru.yandex.market.ff4shops.util.UtilsConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

/**
 * @author fbokovikov
 */
@ActiveProfiles(profiles = {"functionalTest", "development"})
@ExtendWith(SpringExtension.class)
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DbUnitTestExecutionListener.class,
        MockitoTestExecutionListener.class
})
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                ApplicationConfig.class,
                MockConfig.class,
                EmbeddedPostgresConfiguration.class,
                UtilsConfig.class,
        })
@TestPropertySource(locations = {"classpath:00_application.properties", "classpath:functional-test.properties"})
@SpyBean(PushApi.class)
public abstract class FunctionalTest {

    public static final String TUPLE_PARAMETERIZED_DISPLAY_NAME = "[" + INDEX_PLACEHOLDER + "] {0}";

    @LocalServerPort
    protected int randomServerPort;

    @Autowired
    protected TestUrlBuilder urlBuilder;

    @Autowired
    @Qualifier("dataSource")
    private DataSource dataSource;

    @AfterEach
    public void after() throws SQLException {
        assertEquals(
                0,
                dataSource.unwrap(HikariDataSource.class).getHikariPoolMXBean().getActiveConnections(),
                "All database connections must be released/closed after work"
        );
    }

    public static <T> Task<T> createDbQueueTask(T payload) {
        return Task.<T>builder(new QueueShardId("1"))
                .withPayload(payload)
                .build();
    }
}
