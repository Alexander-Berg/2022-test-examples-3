package ru.yandex.market.crm.triggers.test;

import org.apache.tika.Tika;
import org.mockito.Mockito;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.communication.proxy.client.CommunicationProxyClient;
import ru.yandex.market.crm.core.services.external.ClckClient;
import ru.yandex.market.crm.core.services.external.smspassport.SmsPassportClient;
import ru.yandex.market.crm.core.services.staff.StaffService;
import ru.yandex.market.crm.core.suppliers.SubscriptionsTypesSupplier;
import ru.yandex.market.crm.core.suppliers.TestSubscriptionsTypesSupplier;
import ru.yandex.market.crm.core.test.CoreTestSuppliersConfig;
import ru.yandex.market.crm.core.test.TestEnvironmentResolver;
import ru.yandex.market.crm.core.test.TestNetConfig;
import ru.yandex.market.crm.core.test.loggers.CoreTestLoggersConfig;
import ru.yandex.market.crm.external.blackbox.YandexBlackboxClient;
import ru.yandex.market.crm.external.personal.PersonalService;
import ru.yandex.market.crm.triggers.services.ServicesConfig;
import ru.yandex.market.crm.triggers.services.bpm.delegates.log.TriggerExternalLogger;
import ru.yandex.market.crm.triggers.services.carter.CarterClientConfig;
import ru.yandex.market.crm.triggers.services.checkouter.CheckouterConfig;
import ru.yandex.market.crm.triggers.services.marketb2b.MarketB2BConfig;
import ru.yandex.market.crm.triggers.test.helpers.MockTriggerExternalLogger;
import ru.yandex.market.crm.triggers.test.helpers.TestHelpersConfig;
import ru.yandex.market.mbi.api.client.config.MbiApiClientConfig;
import ru.yandex.market.mcrm.http.internal.TvmServiceMockImpl;
import ru.yandex.market.mcrm.http.tvm.TvmService;
import ru.yandex.market.mcrm.lock.LockService;
import ru.yandex.market.mcrm.lock.MockLockService;
import ru.yandex.market.mcrm.utils.PropertiesProvider;
import ru.yandex.market.sdk.userinfo.service.NoSideEffectUserService;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author apershukov
 */
@Configuration
@Import({
        CarterClientConfig.class,
        CheckouterConfig.class,
        MbiApiClientConfig.class,
        ServicesConfig.class,
        TestBpmConfig.class,
        TPLocalYtConfig.class,
        TestNetConfig.class,
        TestDatabaseConfig.class,
        TestEnvironmentResolver.class,
        TestHelpersConfig.class,
        CoreTestSuppliersConfig.class,
        CoreTestLoggersConfig.class,
        MarketB2BConfig.class
})
public class ServiceIntTestConfig {

    @Bean
    public NoSideEffectUserService noSideEffectUserService() {
        return Mockito.mock(NoSideEffectUserService.class);
    }

    @Bean
    public SubscriptionsTypesSupplier subscriptionsTypesSupplier() {
        return new TestSubscriptionsTypesSupplier();
    }

    @Bean
    public PropertiesProvider propertyProvider(ConfigurableBeanFactory beanFactory) {
        return new PropertiesProvider(beanFactory);
    }

    @Bean
    public LockService lockService() {
        return new MockLockService();
    }

    @Bean
    public YandexBlackboxClient yandexBlackboxClient() {
        return Mockito.mock(YandexBlackboxClient.class);
    }

    @Bean
    public TvmService tvmService() {
        return new TvmServiceMockImpl();
    }

    @Bean
    public SmsPassportClient smsPassportClient() {
        return Mockito.mock(SmsPassportClient.class);
    }

    @Bean
    public TriggerExternalLogger triggerExternalLogger() {
        return new MockTriggerExternalLogger();
    }

    @Bean
    public Tika tika() {
        return new Tika();
    }

    @Bean
    public ClckClient clckClient() {
        return Mockito.mock(ClckClient.class);
    }

    @Bean
    @Primary
    public StaffService staffService() {
        var mock = Mockito.mock(StaffService.class);
        when(mock.hasPuid(anyLong())).thenReturn(true);
        when(mock.hasEmail(anyString())).thenReturn(true);
        when(mock.hasCommonPhone(anyString())).thenReturn(true);
        return mock;
    }

    @Bean
    @Primary
    public CommunicationProxyClient communicationProxyClient() {
        return Mockito.mock(CommunicationProxyClient.class);
    }

    @Bean
    public PersonalService personalService() {
        return Mockito.mock(PersonalService.class);
    }
}
