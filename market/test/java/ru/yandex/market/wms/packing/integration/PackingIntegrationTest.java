package ru.yandex.market.wms.packing.integration;

import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import ru.yandex.market.wms.auth.core.model.InforAuthentication;
import ru.yandex.market.wms.auth.core.websocket.interceptor.AuthInboundChannelInterceptor;
import ru.yandex.market.wms.auth.core.websocket.interceptor.AuthOutboundChannelInterceptor;
import ru.yandex.market.wms.common.model.enums.InforRole;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.TestSecurityDataProvider;
import ru.yandex.market.wms.common.spring.helper.NullableColumnsDataSetLoader;
import ru.yandex.market.wms.packing.TestManagerWrapper;
import ru.yandex.market.wms.packing.utils.PackingFlow;
import ru.yandex.market.wms.packing.utils.PackingWebsocket;
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class PackingIntegrationTest extends IntegrationTest {

    @Autowired
    protected TestManagerWrapper manager;
    @Autowired
    protected ApplicationContext context;
    @Autowired
    protected CacheManager cacheManager;
    @Autowired
    private DataSource dataSource;

    @Override
    @BeforeEach
    public void init() {
        super.init();
        manager.reload();
        cacheManager.getCacheNames().forEach(cache -> cacheManager.getCache(cache).invalidate());
    }

    public void assertDatabase(String xmlPath) throws Exception {
        IDataSet expectedDataSet = new NullableColumnsDataSetLoader().createDataSet(new ClassPathResource(xmlPath));
        IDataSet actualDataSet = new DatabaseConnection(dataSource.getConnection(), "wmwhse1").createDataSet();
        NON_STRICT_UNORDERED.getDatabaseAssertion().assertEquals(expectedDataSet, actualDataSet, List.of());
    }

    public PackingWebsocket createSocket() {
        return context.getBean(PackingWebsocket.class);
    }

    public PackingFlow createPackingFlow() {
        return context.getBean(PackingFlow.class);
    }

    @Configuration
    @Profile(Profiles.TEST)
    @Order(-1)
    static class CustomConfiguration {
        private static final String TOKEN = "TEST_TOKEN";

        @Bean
        @Primary
        PrometheusMeterRegistry prometheusMeterRegistry() {
            return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        }

        @Bean
        @Primary
        AuthOutboundChannelInterceptor authOutboundChannelInterceptor() {
            return new AuthOutboundChannelInterceptor() {
            };
        }

        @Bean
        @Primary
        AuthInboundChannelInterceptor authInboundChannelInterceptor(TestSecurityDataProvider securityDataProvider) {
            return new AuthInboundChannelInterceptor() {
                @Override
                public Message<?> preSend(Message<?> message, MessageChannel channel) {
                    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message,
                            StompHeaderAccessor.class);
                    Optional.ofNullable(accessor)
                            .map(StompHeaderAccessor::getLogin)
                            .ifPresent(user -> {
                                InforAuthentication authentication = new InforAuthentication(user, TOKEN,
                                        InforRole.ALL_ROLES);
                                authentication.setAuthenticated(true);
                                if (accessor.isMutable()) {
                                    accessor.setUser(authentication);
                                    securityDataProvider.setUser(user);
                                }
                            });
                    return message;
                }
            };
        }
    }

}
