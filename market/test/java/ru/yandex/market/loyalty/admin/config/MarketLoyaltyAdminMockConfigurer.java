package ru.yandex.market.loyalty.admin.config;

import Market.DataCamp.SyncAPI.SyncGetPromo;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.mockito.MockSettings;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.client.GeoSearchApiClient;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.acl.YtAcl;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.files.YtFiles;
import ru.yandex.inside.yt.kosher.impl.YtConfiguration;
import ru.yandex.inside.yt.kosher.impl.YtImpl;
import ru.yandex.inside.yt.kosher.impl.operations.jars.JarsProcessor;
import ru.yandex.inside.yt.kosher.operations.YtOperations;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.transactions.YtTransactions;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.loyalty.core.config.YtArnold;
import ru.yandex.market.loyalty.core.config.YtHahn;
import ru.yandex.market.loyalty.core.model.security.AdminRole;
import ru.yandex.market.loyalty.core.service.blackbox.domain.BlackBoxResponse;
import ru.yandex.market.loyalty.core.service.blackbox.domain.Uid;
import ru.yandex.market.loyalty.core.service.datacamp.DataCampStrollerClient;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.IssuesClient;
import ru.yandex.startrek.client.StartrekClient;
import ru.yandex.startrek.client.Statuses;
import ru.yandex.startrek.client.StatusesClient;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.Status;
import ru.yandex.startrek.client.model.StatusRef;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.loyalty.core.mock.MarketLoyaltyCoreMockConfigurer.MOCKS;
import static ru.yandex.market.loyalty.spring.utils.PreventAutowire.preventAutowire;

/**
 * @author ukchuvrus
 */
@Configuration
public class MarketLoyaltyAdminMockConfigurer {

    @Configuration
    public static class MdsClientConfig {
        @Bean
        public MdsS3Client mdsS3Client(MockSettings mockSettings) {
            MdsS3Client mdsS3Client = mock(MdsS3Client.class, mockSettings);
            MOCKS.put(mdsS3Client, null);
            return mdsS3Client;
        }
    }

    @Configuration
    public static class CypressConfig {
        @Bean
        @YtHahn
        public Yt hahnYt(MockSettings mockSettings) {
            return mockYt(mockSettings);
        }

        @Bean
        @YtArnold
        public Yt arnoldYt(MockSettings mockSettings) {
            return mockYt(mockSettings);
        }

        @NotNull
        private YtImpl mockYt(MockSettings mockSettings) {
            Cypress cypress = mock(Cypress.class, mockSettings);
            YtAcl acl = mock(YtAcl.class, mockSettings);
            YtFiles files = mock(YtFiles.class, mockSettings);
            YtTables tables = mock(YtTables.class, mockSettings);
            YtOperations operations = mock(YtOperations.class, mockSettings);
            YtTransactions transactions = mock(YtTransactions.class, mockSettings);
            JarsProcessor jarsProcessor = mock(JarsProcessor.class, mockSettings);
            YtConfiguration configuration = mock(YtConfiguration.class, mockSettings);

            MOCKS.put(cypress, null);
            MOCKS.put(acl, null);
            MOCKS.put(files, null);
            MOCKS.put(tables, null);
            MOCKS.put(operations, null);
            MOCKS.put(transactions, null);
            MOCKS.put(jarsProcessor, null);
            MOCKS.put(configuration, null);

            return new YtImpl(
                    cypress,
                    acl,
                    files,
                    tables,
                    operations,
                    transactions,
                    jarsProcessor,
                    configuration
            );
        }
    }

    @Configuration
    public static class MbiClientConfig {
        @Bean
        public FactoryBean<MbiApiClient> getMbiApiClientMock(MockSettings mockSettings) {
            MbiApiClient result = mock(MbiApiClient.class, mockSettings);
            MOCKS.put(result, null);
            return preventAutowire(result);
        }
    }


    @Configuration
    public static class YtJdbcConfig {
        @YtHahn
        @Bean
        public JdbcTemplate hahnJdbcTemplate(MockSettings mockSettings) {
            JdbcTemplate result = mock(JdbcTemplate.class, mockSettings);
            MOCKS.put(result, null);
            return result;
        }

        @YtArnold
        @Bean
        public JdbcTemplate arnoldJdbcTemplate(MockSettings mockSettings) {
            JdbcTemplate result = mock(JdbcTemplate.class, mockSettings);
            MOCKS.put(result, null);
            return result;
        }
    }


    @Configuration
    public static class GeoSearchApiConfig {
        @Bean
        public GeoClient geoClient() {
            GeoClient geoClient = mock(GeoSearchApiClient.class);
            MOCKS.put(geoClient, () -> {
            });
            return geoClient;
        }
    }

    @Configuration
    public static class DataCampStrollerConfig {
        @Bean
        public DataCampStrollerClient mockedDataCampClient() {
            DataCampStrollerClient dataCampStrollerClient = mock(DataCampStrollerClient.class);

            MOCKS.put(dataCampStrollerClient,
                    () -> when(dataCampStrollerClient.getPromo(any())).thenReturn(SyncGetPromo.GetPromoBatchResponse.getDefaultInstance()));
            return dataCampStrollerClient;
        }
    }

    @Configuration
    public static class BlackboxClientConfig {

        public static final String VIEWER_USER = "VIEWER_USER";
        public static final String COUPON_PROMO_EDITOR_USER = "COUPON_PROMO_EDITOR_USER";
        public static final String UNSAFE_TASK_SUBMITTER_ROLE = "UNSAFE_TASK_SUBMITTER_ROLE";
        public static final String QA_ENGINEER_ROLE = "QA_ENGINEER_ROLE";
        public static final String COUPON_GENERATOR_USER = "COUPON_GENERATOR_USER";
        public static final String COIN_GENERATOR_USER = "COIN_GENERATOR_USER";
        public static final String PROMO_TRIGGER_EDITOR_USER = "PROMO_TRIGGER_EDITOR_USER";
        public static final String SUPERUSER_USER = "SUPERUSER_USER";

        private static synchronized void initBlackboxAuthenticationService(RestTemplate restTemplate) {
            ImmutableMap.<String, AdminRole>builder()
                    .put(VIEWER_USER, AdminRole.VIEWER_ROLE)
                    .put(QA_ENGINEER_ROLE, AdminRole.QA_ENGINEER_ROLE)
                    .put(COUPON_PROMO_EDITOR_USER, AdminRole.COUPON_PROMO_EDITOR_ROLE)
                    .put(UNSAFE_TASK_SUBMITTER_ROLE, AdminRole.UNSAFE_TASK_SUBMITTER_ROLE)
                    .put(COUPON_GENERATOR_USER, AdminRole.COUPON_GENERATOR_ROLE)
                    .put(PROMO_TRIGGER_EDITOR_USER, AdminRole.PROMO_TRIGGER_EDITOR_ROLE)
                    .put(SUPERUSER_USER, AdminRole.SUPERUSER_ROLE)
                    .build()
                    .forEach((login, role) -> {
                        BlackBoxResponse user = BlackBoxResponse.builder()
                                .setLogin(login)
                                .setUid(Uid.builder()
                                        .setValue("0")
                                        .build())
                                .build();
                        when(restTemplate.exchange(
                                argThat(hasToString(
                                        containsString("sessionid=" + role.getCode())
                                        )
                                ),
                                eq(HttpMethod.GET),
                                eq(HttpEntity.EMPTY),
                                eq(BlackBoxResponse.class)
                        )).thenReturn(ResponseEntity.ok(user));
                    });
        }

        @Bean
        @BlackboxYandexTeam
        public RestTemplate getBlackboxRestTemplateMock(MockSettings mockSettings) {
            RestTemplate restTemplate = mock(RestTemplate.class, mockSettings);
            MOCKS.put(restTemplate, () -> initBlackboxAuthenticationService(restTemplate));
            return restTemplate;
        }

        @Bean
        public AuthorizationContext authorizationContext() {
            return new TestAuthorizationContext();
        }

    }

    @Configuration
    public static class StartrekServiceConfiguration {
        public final static String TEST_TICKET_QUEUE = "PROMOAPPROVAL";
        public final static String TEST_TICKET_OK = TEST_TICKET_QUEUE + "-200";
        public final static String TEST_TICKET_STATUS_KEY = "approved";
        public final static String TEST_TICKET_WRONG = TEST_TICKET_QUEUE + "-422";
        public final static String TEST_TICKET_MOCKED_STATUS = "Mocked status";
        public final static Status STATUS_CLIENT = mock(Status.class);

        @Bean
        public StartrekClient startrekClient() {
            StartrekClient client = mock(StartrekClient.class);
            Issues issues = mock(IssuesClient.class);
            Issue okIssue = mock(Issue.class);
            Issue wrongIssue = mock(Issue.class);
            StatusRef okStatusRef = mock(StatusRef.class);
            StatusRef wrongStatusRef = mock(StatusRef.class);
            Statuses statuses = mock(StatusesClient.class);

            MOCKS.put(client, () -> {
                when(client.issues(any())).thenReturn(issues);
                when(client.statuses(any())).thenReturn(statuses);
                when(issues.exists(anyString())).thenReturn(true); // ticket exists

                when(issues.get(TEST_TICKET_OK)).thenReturn(okIssue);
                when(okIssue.getStatus()).thenReturn(okStatusRef);
                when(okStatusRef.getKey()).thenReturn(TEST_TICKET_STATUS_KEY); // correct ticket status

                when(issues.get(TEST_TICKET_WRONG)).thenReturn(wrongIssue);
                when(wrongIssue.getStatus()).thenReturn(wrongStatusRef);
                when(wrongStatusRef.getKey()).thenReturn("wrong_status_key"); // wrong tiket status

                when(statuses.get(anyString())).thenReturn(STATUS_CLIENT);
                when(STATUS_CLIENT.getName()).thenReturn(TEST_TICKET_MOCKED_STATUS);
            });
            return client;
        }
    }

}
