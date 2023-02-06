package ru.yandex.market.sc.core.test;

import java.util.List;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.common.cache.memcached.client.MemCachedClient;
import ru.yandex.market.common.test.mockito.MemCachedClientFactoryMock;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.sc.core.config.external.DataSourceConfig;
import ru.yandex.market.sc.core.external.delivery_service.TplClient;
import ru.yandex.market.sc.core.external.delivery_service.TplClientImpl;
import ru.yandex.market.sc.core.external.juggler.JugglerNotificationClient;
import ru.yandex.market.sc.core.external.s3.S3Client;
import ru.yandex.market.sc.core.external.sqs.SqsQueueProperties;
import ru.yandex.market.sc.core.external.taxi.TaxiLogPlatformClient;
import ru.yandex.market.sc.core.external.telegram.TelegramNotificationService;
import ru.yandex.market.sc.core.external.tm.TmClient;
import ru.yandex.market.sc.core.external.transfermanager.TransferManagerService;
import ru.yandex.market.sc.core.external.yt.ArchiveYtClient;
import ru.yandex.market.tpl.common.logbroker.config.LogbrokerTestExternalConfig;
import ru.yandex.market.tpl.common.transferact.client.api.SignatureApi;
import ru.yandex.market.tpl.common.transferact.client.api.TransferApi;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxClient;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxDisplayDto;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxUser;
import ru.yandex.market.tpl.common.web.blackbox.OAuthUser;
import ru.yandex.market.tpl.common.web.config.TplBlackboxConfiguration;
import ru.yandex.market.tpl.common.web.config.TplProfiles;
import ru.yandex.passport.tvmauth.TvmClient;
import ru.yandex.yql.YqlDataSource;

import static org.mockito.Mockito.mock;

/**
 * @author valter
 */
@Import({
        TestDatabaseConfiguration.class,
        LogbrokerTestExternalConfig.class

})
@Configuration
public class TestExternalConfiguration {

    @Bean
    @Profile(TplProfiles.TESTS)
    public AllowRouteFieldReadingAspect allowRouteFieldReadingAspect() {
        return new AllowRouteFieldReadingAspect();
    }

    @Bean("blackboxClient")
    BlackboxClient testBlackboxClient() {
        // delegate all work to wrapper
        return new TplBlackboxConfiguration.BlackboxClientWrapper(new BlackboxClient() {
            @Override
            public OAuthUser oauth(String oauthToken, @Nullable String ip) {
                throw new UnsupportedOperationException();
            }

            @Override
            public long getUidForLogin(String login) {
                throw new UnsupportedOperationException();
            }

            @Override
            public BlackboxUser invokeUserinfo(Long uid) {
                throw new UnsupportedOperationException();
            }

            @Override
            public BlackboxDisplayDto getDisplayInfo(String login) {
                throw new UnsupportedOperationException();
            }
        });
    }

    @Bean("innerBlackboxClient")
    BlackboxClient testInnerBlackboxClient() {
        // delegate all work to wrapper
        return new TplBlackboxConfiguration.BlackboxClientWrapper(new BlackboxClient() {
            @Override
            public OAuthUser oauth(String oauthToken, @Nullable String ip) {
                throw new UnsupportedOperationException();
            }

            @Override
            public long getUidForLogin(String login) {
                throw new UnsupportedOperationException();
            }

            @Override
            public BlackboxUser invokeUserinfo(Long uid) {
                throw new UnsupportedOperationException();
            }

            @Override
            public BlackboxDisplayDto getDisplayInfo(String login) {
                throw new UnsupportedOperationException();
            }
        });
    }

    @Bean
    public TvmClient tvmClient() {
        return mock(TvmClient.class);
    }

    @Bean
    public TplClient tplClient() {
        return mock(TplClientImpl.class);
    }

    @Bean
    public TmClient tmClient() {
        return mock(TmClient.class);
    }

    @Bean
    public TransferManagerService transferManagerService() {
        return mock(TransferManagerService.class);
    }

    @Bean
    public ArchiveYtClient archiveYtClient() {
        return mock(ArchiveYtClient.class);
    }

    @Bean
    public LMSClient lmsClient() {
        return mock(LMSClient.class);
    }

    @Bean
    public JugglerNotificationClient jugglerNotificationClient() {
        return mock(JugglerNotificationClient.class);
    }

    @Bean
    public TelegramNotificationService telegramNotificationService() {
        return mock(TelegramNotificationService.class);
    }

    @Bean
    public MemCachedClientFactoryMock memCachedClientFactoryMock() {
        return new MemCachedClientFactoryMock();
    }

    @Bean
    public JmsTemplate mockJmsTemplate() {
        return mock(JmsTemplate.class);
    }

    @Bean
    public SqsQueueProperties sqsQueueProperties() {
        return mock(SqsQueueProperties.class);
    }

    @Bean
    public MemCachedClient scApiMemcachedClient(MemCachedClientFactoryMock memCachedClientFactoryMock) {
        return memCachedClientFactoryMock.newClient(List.of("sc-api"));
    }

    @Bean
    public MemCachedClient scIntMemcachedClient(MemCachedClientFactoryMock memCachedClientFactoryMock) {
        return memCachedClientFactoryMock.newClient(List.of("sc-int"));
    }

    @Bean
    public TransferApi transferApi() {
        return mock(TransferApi.class);
    }

    @Bean
    public SignatureApi signatureApi() {
        return mock(SignatureApi.class);
    }

    @Bean
    public DataSourceConfig ytDataSourceConfig() {
        return new DataSourceConfig() {
            @Override
            public DataSource dataSource() {
                return mock(YqlDataSource.class);
            }

            @Override
            public JdbcTemplate jdbcTemplate() {
                return mock(JdbcTemplate.class);
            }
        };
    }

    @Bean
    public S3Client s3Client() {
        return mock(S3Client.class);
    }

    @Bean
    public TaxiLogPlatformClient taxiLogPlatformClient() {
        return mock(TaxiLogPlatformClient.class);
    }

}
