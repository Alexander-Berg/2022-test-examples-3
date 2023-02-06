package ru.yandex.market.pers.address.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import liquibase.integration.spring.SpringLiquibase;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import ru.yandex.market.pers.address.db.config.InMemoryDbConfig;
import ru.yandex.market.pers.address.db.config.LiquibaseChangelog;
import ru.yandex.market.pers.address.tvm.Actions;
import ru.yandex.market.pers.address.tvm.BackClient;
import ru.yandex.market.pers.address.tvm.LoggingTvmRequestAuthHandler;
import ru.yandex.market.pers.address.tvm.TvmBaseException;
import ru.yandex.market.pers.address.tvm.TvmClient;
import ru.yandex.market.pers.address.tvm.TvmClientsRegistry;
import ru.yandex.market.pers.address.tvm.TvmRequestAuthHandler;
import ru.yandex.market.pers.address.util.DbCleaner;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;


import javax.sql.DataSource;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Configuration
@Import({
    InMemoryDbConfig.class, // поднимаем PG
    InternalConfig.class,
    MockConfigurer.class,
    ShedlockConfig.class,
    SwaggerConfig.class
})
@PropertySource("classpath:/test-application.properties")
public class TestConfig {
    @Bean
    public MockMvc createMockMvc(WebApplicationContext wac) {
        return MockMvcBuilders
            .webAppContextSetup(wac)
            .addDispatcherServletCustomizer(
                dispatcherServlet -> dispatcherServlet.setThrowExceptionIfNoHandlerFound(true)
            )
            .apply(springSecurity())
            .build();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    protected RestTemplate tvmRestTemplate() {
        return Mockito.mock(RestTemplate.class);
    }

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(LiquibaseChangelog.ONLY_SCHEMA.getChangelog());
        return liquibase;
    }

    @Bean
    public DbCleaner dbCleaner(JdbcTemplate jdbcTemplate) {
        return new DbCleaner(jdbcTemplate);
    }

    @Bean
    public TestTvmRequestAuthHandler tvmRequestAuthHandler(TvmClient tvmClient, TvmClientsRegistry tvmClientsRegistry) {
        return new TestTvmRequestAuthHandler(tvmClient, tvmClientsRegistry);
    }

    @Bean
    public TestClient testClient(MockMvc mockMvc, ObjectMapper objectMapper) {
        return new TestClient(mockMvc, objectMapper);
    }

    public static class TestTvmRequestAuthHandler implements TvmRequestAuthHandler {
        private static final BackClient TEST_CLIENT = new BackClient() {
            @Override
            public String name() {
                return "test";
            }

            @Override
            public Set<String> getRoleSet() {
                return Actions.values;
            }
        };
        private TvmClient tvmClient;
        private TvmClientsRegistry tvmClientsRegistry;

        public enum TestMode {
            FAIR_CHECK,
            DEFAULT
        }

        private volatile TestMode testMode = TestMode.DEFAULT;
        private TvmRequestAuthHandler defaultTvmHandler;

        public TestTvmRequestAuthHandler(TvmClient tvmClient, TvmClientsRegistry tvmClientsRegistry) {
            this.tvmClient = tvmClient;
            this.tvmClientsRegistry = tvmClientsRegistry;
            this.defaultTvmHandler = new LoggingTvmRequestAuthHandler(tvmClient, tvmClientsRegistry);
        }

        public void setTestMode(TestMode testMode) {
            this.testMode = testMode;
        }

        @Override
        public int handleUnallowedClient(String clientHandle) {
            if (testMode == TestMode.DEFAULT) {
                return defaultTvmHandler.handleUnallowedClient(clientHandle);
            } else {
                return AccessDecisionVoter.ACCESS_DENIED;
            }
        }

        @Override
        public int handleUnallowedMethod(String clientHandle) {
            if (testMode == TestMode.DEFAULT) {
                return defaultTvmHandler.handleUnallowedMethod(clientHandle);
            } else {
                return AccessDecisionVoter.ACCESS_DENIED;
            }
        }

        @Override
        public int extractSourceClientHandleFromServiceTicket(String ticket) {
            if (testMode == TestMode.DEFAULT) {
                if (Objects.equals(ticket, "test")) {
                    Mockito.when(tvmClient.checkServiceTicket(Mockito.eq(ticket))).thenReturn(0);
                } else {
                    Mockito.when(tvmClient.checkServiceTicket(Mockito.any())).thenThrow(new TvmBaseException(""));
                }
                return defaultTvmHandler.extractSourceClientHandleFromServiceTicket(ticket);
            } else {
                if (Objects.equals(ticket, "test")) {
                    return 0;
                } else {
                    throw new TvmBaseException("");
                }
            }
        }

        @Override
        public Optional<BackClient> getClientByHandle(long clientHandle) {
            if(testMode == TestMode.DEFAULT) {
                Mockito.when(tvmClientsRegistry.findClientByHandle(Mockito.eq(0L))).thenReturn(Optional.of(TEST_CLIENT));
                return defaultTvmHandler.getClientByHandle(clientHandle);
            } else {
                if (clientHandle == 0) {
                    return Optional.of(new BackClient() {
                        @Override
                        public String name() {
                            return "test";
                        }

                        @Override
                        public Set<String> getRoleSet() {
                            return Actions.values;
                        }
                    });
                } else {
                    return Optional.empty();
                }
            }
        }
    }
}
