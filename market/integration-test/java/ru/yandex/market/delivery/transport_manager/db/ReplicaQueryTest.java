package ru.yandex.market.delivery.transport_manager.db;

import java.lang.reflect.Method;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.delivery.transport_manager.config.datasource.DatabaseConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
    classes = {DatabaseConfig.class, ReplicaQueryTest.Config.class}
)
@ContextConfiguration(classes = {DatabaseConfig.class, ReplicaQueryTest.Config.class})
@TestPropertySource("classpath:/replica_test/application.properties")
@ExtendWith(SpringExtension.class)
class ReplicaQueryTest {

    @Autowired
    @SpyBean
    @Qualifier("mainDataSource")
    DataSource mainDataSource;

    @Autowired
    @SpyBean
    @Qualifier("slaveDataSource")
    DataSource slaveDataSource;
    @Autowired
    SampleTransactionalService sampleTransactionalService;

    @Test
    @DisplayName("При вызове read-only-транзакционного метода используется соединение к слейв-реплике")
    void testReadingTransaction() {
        int masterCallsBefore = getGetConnectionCount(mainDataSource);
        int slaveCallsBefore = getGetConnectionCount(slaveDataSource);

        ignoreException(sampleTransactionalService::readingTransaction);

        int masterCallsAfter = getGetConnectionCount(mainDataSource);
        int slaveCallsAfter = getGetConnectionCount(slaveDataSource);

        assertEquals(masterCallsAfter, masterCallsBefore);
        assertEquals(slaveCallsAfter, slaveCallsBefore + 1);
    }

    @Test
    @DisplayName("При вызове non-read-only-транзакционного метода используется соединение к мастер-реплике")
    void testWritingTransaction() {
        int masterCallsBefore = getGetConnectionCount(mainDataSource);
        int slaveCallsBefore = getGetConnectionCount(slaveDataSource);

        ignoreException(sampleTransactionalService::writingTransaction);

        int masterCallsAfter = getGetConnectionCount(mainDataSource);
        int slaveCallsAfter = getGetConnectionCount(slaveDataSource);

        assertEquals(masterCallsAfter, masterCallsBefore + 1);
        assertEquals(slaveCallsAfter, slaveCallsBefore);
    }

    private static void ignoreException(Runnable invocation) {
        try {
            invocation.run();
        } catch (Exception e) {
            // Just do nothing
        }
    }

    private static int getGetConnectionCount(DataSource dataSourceMock) {
        return (int) Mockito.mockingDetails(dataSourceMock).getInvocations().stream()
            .map(InvocationOnMock::getMethod)
            .map(Method::getName)
            .filter(name -> name.equals("getConnection"))
            .count();
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @EnableAutoConfiguration(exclude = {
        SecurityAutoConfiguration.class,
        LiquibaseAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        JdbcRepositoriesAutoConfiguration.class
    })
    public static class Config {

        @Bean
        public SampleTransactionalService sampleTransactionalService() {
            return new SampleTransactionalService();
        }
    }
}
