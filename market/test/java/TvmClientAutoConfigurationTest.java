package ru.yandex.market.starter.tvm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import ru.yandex.market.starter.integration.tvm.TvmConfigurerAdapter;
import ru.yandex.market.starter.tvm.config.TvmClientAutoConfiguration;
import ru.yandex.market.starter.tvm.config.TvmPropertiesAutoConfiguration;
import ru.yandex.market.starter.tvm.factory.TvmClientSettings;
import ru.yandex.passport.tvmauth.BlackboxEnv;
import ru.yandex.passport.tvmauth.CheckedServiceTicket;
import ru.yandex.passport.tvmauth.CheckedUserTicket;
import ru.yandex.passport.tvmauth.ClientStatus;
import ru.yandex.passport.tvmauth.TicketStatus;
import ru.yandex.passport.tvmauth.TvmClient;
import ru.yandex.passport.tvmauth.roles.Roles;

import static org.assertj.core.api.Assertions.assertThat;

public class TvmClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(TestTvmClient.class)
        .withConfiguration(AutoConfigurations.of(TvmPropertiesAutoConfiguration.class))
        .withConfiguration(AutoConfigurations.of(TvmClientAutoConfiguration.class));

    @Test
    void tvmClientSettings_TvmSecret_Test() {
        final String secret = "secret";
        final String fallbackSecret1 = "fallbackSecret1";
        final String fallbackSecret2 = "fallbackSecret2";

        contextRunner
            .withPropertyValues(TvmClientAutoConfiguration.TVM_SECRET_PROP_NAME + "=" + secret)
            .run(context -> assertThat(context.getBean(TvmClientSettings.class).getTvmSecret()).isEqualTo(secret));

        contextRunner
            .withPropertyValues(TvmClientAutoConfiguration.FALLBACK_TVM_SECRET_PROP_NAME_1 + "=" + fallbackSecret1)
            .run(context ->
                assertThat(context.getBean(TvmClientSettings.class).getTvmSecret()).isEqualTo(fallbackSecret1));

        contextRunner
            .withPropertyValues(TvmClientAutoConfiguration.FALLBACK_TVM_SECRET_PROP_NAME_2 + "=" + fallbackSecret2)
            .run(context ->
                assertThat(context.getBean(TvmClientSettings.class).getTvmSecret()).isEqualTo(fallbackSecret2));

        // Check Priority

        contextRunner
            .withPropertyValues(
                TvmClientAutoConfiguration.TVM_SECRET_PROP_NAME + "=" + secret,
                TvmClientAutoConfiguration.FALLBACK_TVM_SECRET_PROP_NAME_1 + "=" + fallbackSecret1,
                TvmClientAutoConfiguration.FALLBACK_TVM_SECRET_PROP_NAME_2 + "=" + fallbackSecret2
            )
            .run(context ->
                assertThat(context.getBean(TvmClientSettings.class).getTvmSecret()).isEqualTo(secret));

        contextRunner
            .withPropertyValues(
                TvmClientAutoConfiguration.FALLBACK_TVM_SECRET_PROP_NAME_1 + "=" + fallbackSecret1,
                TvmClientAutoConfiguration.FALLBACK_TVM_SECRET_PROP_NAME_2 + "=" + fallbackSecret2
            )
            .run(context ->
                assertThat(context.getBean(TvmClientSettings.class).getTvmSecret()).isEqualTo(fallbackSecret1));
    }

    @Test
    void tvmClientSettings_TvmConfigurers_Test() {
        contextRunner
            .withUserConfiguration(TestTvmConfigurerConfiguration.class)
            .run(context ->
                assertThat(context.getBean(TvmClientSettings.class).getSources())
                    .isEqualTo(new HashSet<>(TestTvmConfigurerConfiguration.resultSources)));
    }

    static class TestTvmConfigurer extends TvmConfigurerAdapter {

        private final Set<Integer> sources;

        TestTvmConfigurer(Set<Integer> sources) {
            this.sources = sources;
        }

        @Override
        public Set<Integer> getSources() {
            return sources;
        }
    }

    static class TestTvmConfigurerConfiguration {

        static final List<Integer> resultSources = List.of(100500, 342, 99);

        static final TestTvmConfigurer testTvmConfigurer1 =
            new TestTvmConfigurer(Set.of(
                resultSources.get(0),
                resultSources.get(1)
            ));

        static final TestTvmConfigurer testTvmConfigurer2 =
            new TestTvmConfigurer(Set.of(
                resultSources.get(2)
            ));

        @Bean
        public TestTvmConfigurer testTvmConfigurer1() {
            return testTvmConfigurer1;
        }

        @Bean
        public TestTvmConfigurer testTvmConfigurer2() {
            return testTvmConfigurer2;
        }
    }

    public static class TestTvmClient implements TvmClient {
        public static final String DELIMITER = "::";
        public static final String DEFAULT_SERVICE_TICKET = "testServiceTicket";
        public static final String DEFAULT_USER_TICKET = "testUserTicket";

        private final String serviceTicket;
        private final String userTicket;

        public TestTvmClient(String serviceTicket, String userTicket) {
            this.serviceTicket = serviceTicket;
            this.userTicket = userTicket;
        }

        public TestTvmClient() {
            this.serviceTicket = DEFAULT_SERVICE_TICKET;
            this.userTicket = DEFAULT_USER_TICKET;
        }

        @Override
        public ClientStatus getStatus() {
            return new ClientStatus(ClientStatus.Code.OK, "OK");
        }

        @Override
        public String getServiceTicketFor(String alias) {
            return serviceTicket;
        }

        @Override
        public String getServiceTicketFor(int tvmId) {
            return serviceTicket;
        }

        @Override
        public CheckedServiceTicket checkServiceTicket(String ticketBody) {
            final String[] parts = ticketBody.split(DELIMITER);
            return new CheckedServiceTicket(
                TicketStatus.valueOf(parts[0]), parts[0], Integer.parseInt(parts[1]), Long.parseLong(parts[2])
            );
        }

        @Override
        public CheckedUserTicket checkUserTicket(String ticketBody) {
            final String[] parts = ticketBody.split(DELIMITER);
            final String[] uidsString = parts[3].split(",");
            final long[] uids = new long[uidsString.length];
            for (int i = 0; i < uids.length; i++) {
                uids[i] = Long.parseLong(uidsString[i]);
                i++;
            }
            return new CheckedUserTicket(
                TicketStatus.valueOf(parts[0]), parts[0], parts[1].split(","), Long.parseLong(parts[2]), uids
            );
        }

        @Override
        public CheckedUserTicket checkUserTicket(String ticketBody, BlackboxEnv overridedBbEnv) {
            return checkUserTicket(ticketBody);
        }

        @Override
        public Roles getRoles() {
            return null;
        }

        @Override
        public void close() {

        }
    }

}
