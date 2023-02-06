package ru.yandex.market.crm.operatorwindow.configuration;

import javax.annotation.Nonnull;

import org.mockito.Mockito;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.checkout.referee.CheckoutRefereeClient;
import ru.yandex.market.crm.RolesTestConfiguration;
import ru.yandex.market.crm.environment.Environment;
import ru.yandex.market.crm.environment.EnvironmentResolver;
import ru.yandex.market.crm.operatorwindow.ChatsTestConfiguration;
import ru.yandex.market.crm.operatorwindow.ModuleOwConfiguration;
import ru.yandex.market.crm.operatorwindow.external.platform.PlatformApiClient;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.SmartcallsClient;
import ru.yandex.market.crm.operatorwindow.informing.SmsCampaignService;
import ru.yandex.market.crm.operatorwindow.services.fraud.SmartcallsResults;
import ru.yandex.market.crm.operatorwindow.services.task.calltime.CustomerCallTimeService;
import ru.yandex.market.jmf.blackbox.support.YandexBlackboxClient;
import ru.yandex.market.jmf.blackbox.support.YandexTeamBlackboxClient;
import ru.yandex.market.jmf.db.test.TestDefaultDataSourceConfiguration;
import ru.yandex.market.jmf.module.angry.test.ModuleAngrySpaceTestConfiguration;
import ru.yandex.market.jmf.module.chat.ChatBotClient;
import ru.yandex.market.jmf.module.comment.SendCommentsStatusService;
import ru.yandex.market.jmf.module.comment.test.ModuleCommentTestConfiguration;
import ru.yandex.market.jmf.module.def.test.ModuleDefaultTestConfiguration;
import ru.yandex.market.jmf.module.metric.test.MetricsModuleTestConfiguration;
import ru.yandex.market.jmf.module.scheduling.ModuleSchedulingTestConfiguration;
import ru.yandex.market.jmf.module.toloka.ModuleTolokaTestConfiguration;
import ru.yandex.market.jmf.security.test.SecurityTestConfiguration;
import ru.yandex.market.jmf.startrek.support.impl.service.StartrekSecretSupplier;
import ru.yandex.market.jmf.telephony.voximplant.VoximplantHttpClient;
import ru.yandex.market.jmf.trigger.test.TriggerTestConfiguration;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;
import ru.yandex.market.jmf.utils.AutowireDisabledFactoryBean;
import ru.yandex.market.jmf.utils.html.RedirectContext;
import ru.yandex.market.jmf.utils.html.SafeUrlService;
import ru.yandex.market.jmf.utils.html.SbaSafeUrlService;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.ocrm.module.loyalty.ModuleLoyaltyTestConfiguration;
import ru.yandex.market.ocrm.module.order.arbitrage.ModuleOrderArbitrageTestConfiguration;
import ru.yandex.market.ocrm.module.taximl.ModuleTaximlTestConfiguration;
import ru.yandex.market.ocrm.module.yadelivery.ModuleYaDeliveryTestConfiguration;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.sdk.userinfo.service.NoSideEffectUserService;


@Configuration
@Import({
        ModuleOwConfiguration.class,
        ModuleCommentTestConfiguration.class,
        ModuleLoyaltyTestConfiguration.class,
        ModuleDefaultTestConfiguration.class,
        ModuleTaximlTestConfiguration.class,
        ModuleYaDeliveryTestConfiguration.class,
        ChatsTestConfiguration.class,
        TestDefaultDataSourceConfiguration.class,
        SecurityTestConfiguration.class,
        MetricsModuleTestConfiguration.class,
        ModuleSchedulingTestConfiguration.class,
        ModuleAngrySpaceTestConfiguration.class,
        ModuleTolokaTestConfiguration.class,
        ModuleOrderArbitrageTestConfiguration.class,
        RolesTestConfiguration.class,
        EmployeeRolesConfiguration.class,
        TriggerTestConfiguration.class
})
@TestPropertySource("classpath:ow_test.properties")
public class ModuleOwTestConfiguration extends AbstractModuleConfiguration {

    public ModuleOwTestConfiguration() {
        super("operator-window/test");
    }

    @Bean
    public EnvironmentResolver environmentResolver() {
        return new EnvironmentResolver() {
            @Nonnull
            @Override
            public Environment get() {
                return Environment.INTEGRATION_TEST;
            }
        };
    }

    @Bean
    @Primary
    public SendCommentsStatusService sendCommentsStatusService() {
        return Mockito.mock(SendCommentsStatusService.class);
    }

    @Bean
    public CheckoutRefereeClient checkoutRefereeClient() {
        return Mockito.mock(CheckoutRefereeClient.class);
    }

    @Bean
    public YandexBlackboxClient yandexBlackboxClient() {
        return Mockito.mock(YandexBlackboxClient.class);
    }

    @Bean
    public YandexTeamBlackboxClient yandexTeamBlackboxClient() {
        return Mockito.mock(YandexTeamBlackboxClient.class);
    }

    @Bean
    public MbiApiClient mbiApiClient() {
        return Mockito.mock(MbiApiClient.class);
    }

    @Bean
    public FactoryBean<PersNotifyClient> persNotifyClient() {
        return new AutowireDisabledFactoryBean<>(Mockito.mock(PersNotifyClient.class));
    }

    @Bean
    @Primary
    public VoximplantHttpClient testVoximplantHttpClient() {
        return Mockito.mock(VoximplantHttpClient.class);
    }

    @Bean
    @Primary
    public StartrekSecretSupplier testStartrekSecretSupplier() {
        return Mockito.mock(StartrekSecretSupplier.class);
    }

    @Bean
    @Primary
    public SmartcallsClient testSmartcallsClient() {
        return Mockito.mock(SmartcallsClient.class);
    }

    @Bean
    @Primary
    public ChatBotClient testChatBotClient() {
        return Mockito.mock(ChatBotClient.class);
    }

    @Bean
    @Primary
    public PlatformApiClient mockPlatformApiClient() {
        return Mockito.mock(PlatformApiClient.class);
    }

    @Bean
    @Primary
    public SmsCampaignService testSmsCampaignService() {
        return Mockito.mock(SmsCampaignService.class);
    }

    @Bean
    @Primary
    public CustomerCallTimeService testMockCustomerCallTimeService() {
        return Mockito.mock(CustomerCallTimeService.class);
    }

    @Bean
    @Primary
    public SmartcallsResults testMockSmartcallsResults() {
        return Mockito.mock(SmartcallsResults.class);
    }

    @Bean
    public NoSideEffectUserService noSideEffectUserService() {
        return Mockito.mock(NoSideEffectUserService.class);
    }

    @Bean
    @Primary
    public SafeUrlService realSafeUrlService(RedirectContext redirectContext) {
        return new SbaSafeUrlService(redirectContext);
    }
}
