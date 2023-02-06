package ru.yandex.market.pvz.core.test;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;

import com.amazonaws.services.s3.AmazonS3;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.geobase.HttpGeobase;
import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterReturnClient;
import ru.yandex.market.delivery.mdbclient.MdbClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.pvz.core.domain.operator_window.OwClient;
import ru.yandex.market.sc.internal.client.ScLogisticsClient;
import ru.yandex.market.tpl.client.pvz.TplPvzClient;
import ru.yandex.market.tpl.common.covid.VaccinationValidator;
import ru.yandex.market.tpl.common.ds.client.DeliveryClient;
import ru.yandex.market.tpl.common.logbroker.config.LogbrokerTestExternalConfig;
import ru.yandex.market.tpl.common.lrm.client.api.ReturnsApi;
import ru.yandex.market.tpl.common.personal.client.tpl.PersonalExternalService;
import ru.yandex.market.tpl.common.sms.YaSmsClient;
import ru.yandex.market.tpl.common.startrek.configuration.StartrekExternalMocksConfiguration;
import ru.yandex.market.tpl.common.startrek.configuration.StartrekListenerTestClassesConfiguration;
import ru.yandex.market.tpl.common.transferact.client.api.DocumentApi;
import ru.yandex.market.tpl.common.transferact.client.api.SignatureApi;
import ru.yandex.market.tpl.common.transferact.client.api.TransferApi;
import ru.yandex.market.tpl.common.util.logging.Tracer;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxClient;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxDisplayDto;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxUser;
import ru.yandex.market.tpl.common.web.blackbox.OAuthUser;
import ru.yandex.market.tpl.common.web.config.TplBlackboxConfiguration;
import ru.yandex.market.tpl.common.web.go_zora.GoZoraClient;
import ru.yandex.market.tpl.common.yt.config.DataSourceConfig;
import ru.yandex.market.tpl.common.yt.tables.download.YtDownloader;

import static org.mockito.Mockito.mock;
import static ru.yandex.market.pvz.core.config.PvzCoreInternalConfiguration.TRACER_UID;

@Import({
        StartrekExternalMocksConfiguration.class,
        StartrekListenerTestClassesConfiguration.class,
        TestDatabaseConfiguration.class,
        LogbrokerTestExternalConfig.class
})
@Configuration
public class TestExternalConfiguration {

    public static final long DEFAULT_UID = 1L;

    @PostConstruct
    public void setUpDefaultTracerUid() {
        Tracer.global().put(TRACER_UID, String.valueOf(DEFAULT_UID));
    }

    public void setTracerUid(long uid) {
        Tracer.global().put(TRACER_UID, String.valueOf(uid));
    }

    @Bean
    DeliveryClient deliveryClient() {
        return mock(DeliveryClient.class);
    }

    @Bean
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

    @Bean
    MdbClient mdbClient() {
        return mock(MdbClient.class);
    }

    @Bean
    MbiApiClient mbiApiClient() {
        return mock(MbiApiClient.class);
    }

    @Bean
    LMSClient lmsClient() {
        return mock(LMSClient.class);
    }

    @Bean
    TarifficatorClient tarifficatorClient() {
        return mock(TarifficatorClient.class);
    }

    @Bean
    public TplPvzClient tplClient() {
        return mock(TplPvzClient.class);
    }

    @Bean
    public ScLogisticsClient scLogisticsClient() {
        return mock(ScLogisticsClient.class);
    }

    @Bean
    public OwClient owClient() {
        return mock(OwClient.class);
    }

    @Bean
    public CheckouterReturnClient checkouterReturnClient() {
        return Mockito.mock(CheckouterReturnClient.class);
    }

    @Bean
    public CheckouterClient checkouterClient() {
        return Mockito.mock(CheckouterClient.class);
    }

    @Bean
    public YaSmsClient yaSmsClient() {
        return mock(YaSmsClient.class);
    }

    @Bean
    public HttpGeobase httpGeobase() {
        return mock(HttpGeobase.class);
    }

    @Bean
    DataSourceConfig ytDataSourceConfig() {
        return mock(DataSourceConfig.class);
    }

    @Bean
    public static VaccinationValidator vaccinationValidator() {
        return mock(VaccinationValidator.class);
    }

    @Bean
    public ReturnsApi lrmClient() {
        return mock(ReturnsApi.class);
    }

    @Bean
    public AmazonS3 amazonS3Client() {
        return mock(AmazonS3.class);
    }

    @Bean
    public YtDownloader ytDownloader() {
        return mock(YtDownloader.class);
    }

    @Bean
    public GoZoraClient goZoraClient() {
        return mock(GoZoraClient.class);
    }

    @Bean
    public NesuClient nesuClient() {
        return mock(NesuClient.class);
    }

    @Bean
    ComplexMonitoring complexMonitoring() {
        return mock(ComplexMonitoring.class);
    }

    @Bean
    public static TransferApi mockTransferApiClient() {
        return mock(TransferApi.class);
    }

    @Bean
    public static SignatureApi mockSignatureApiClient() {
        return mock(SignatureApi.class);
    }

    @Bean
    public static DocumentApi mockTransferActDocumentApiClient() {
        return mock(DocumentApi.class);
    }

    @Bean
    PersonalExternalService personalExternalService() {
        return mock(PersonalExternalService.class);
    }
}
