package ru.yandex.market.crm.operatorwindow;

import java.io.IOException;
import java.util.List;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.b2bcrm.module.config.B2bAccountTestConfig;
import ru.yandex.market.b2bcrm.module.ticket.test.config.B2bTicketTestConfig;
import ru.yandex.market.crm.operatorwindow.jmf.script.CustomerScriptServiceApi;
import ru.yandex.market.crm.operatorwindow.jmf.trigger.SendCourierPlatformPushOnNewChatMessageActionStrategy;
import ru.yandex.market.crm.operatorwindow.services.customer.CustomerService;
import ru.yandex.market.jmf.http.HttpClientFactory;
import ru.yandex.market.jmf.module.chat.ChatClientService;
import ru.yandex.market.jmf.module.chat.ModuleChatTestConfiguration;
import ru.yandex.market.jmf.module.comment.test.ModuleCommentTestConfiguration;
import ru.yandex.market.jmf.module.def.test.ModuleDefaultTestConfiguration;
import ru.yandex.market.jmf.module.mail.test.ModuleMailTestConfiguration;
import ru.yandex.market.jmf.module.ticket.test.ModuleTicketTestConfiguration;
import ru.yandex.market.jmf.script.ScriptServiceApi;
import ru.yandex.market.jmf.security.test.SecurityTestConfiguration;
import ru.yandex.market.jmf.telephony.voximplant.secret.VoximplantCredentialsSecretSupplier;
import ru.yandex.market.jmf.telephony.voximplant.secret.VoximplantErrorsSecret;
import ru.yandex.market.jmf.telephony.voximplant.test.VoximplantTestConfiguration;
import ru.yandex.market.jmf.utils.serialize.ObjectSerializeService;
import ru.yandex.market.ocrm.module.loyalty.ModuleLoyaltyTestConfiguration;
import ru.yandex.market.ocrm.module.order.impl.CustomerMarkerProvider;
import ru.yandex.market.ocrm.module.order.impl.GlueProvider;
import ru.yandex.market.ocrm.module.tpl.HttpMarketTplClient;
import ru.yandex.market.ocrm.module.tpl.MarketTplClient;

import static org.mockito.Mockito.mock;

@Import({
        ModuleChatTestConfiguration.class,
        B2bTicketTestConfig.class,
        ModuleCommentTestConfiguration.class,
        ModuleDefaultTestConfiguration.class,
        B2bAccountTestConfig.class,
        ModuleMailTestConfiguration.class,
        ModuleTicketTestConfiguration.class,
        VoximplantTestConfiguration.class,
        SecurityTestConfiguration.class,
        ModuleLoyaltyTestConfiguration.class
})
@ComponentScan("ru.yandex.market.crm.operatorwindow.jmf.chat")
public class ChatsTestConfiguration {

    @Bean
    public VoximplantCredentialsSecretSupplier testVoximplantServiceAccountCredentialsSecretSupplier(
            ResourceLoader resourceLoader
    ) throws IOException {
        var mock = Mockito.mock(VoximplantCredentialsSecretSupplier.class);
        Mockito.when(mock.get()).thenReturn(
                IOUtils.readInputStream(resourceLoader.getResource("classpath:vox.auth.json").getInputStream())
        );
        return mock;
    }

    @Bean
    public VoximplantErrorsSecret testVoximplantErrorsSecret() {
        var mock = Mockito.mock(VoximplantErrorsSecret.class);
        Mockito.when(mock.get()).thenReturn("123");
        return mock;
    }


    @Bean
    public ChatClientService chatClientService() {
        return mock(ChatClientService.class);
    }

    @Bean
    public CustomerService customerService() {
        return mock(CustomerService.class);
    }

    @Bean
    public MarketTplClient testMarketTplClient() {
        return Mockito.mock(MarketTplClient.class);
    }

    @Bean
    @Primary
    public SendCourierPlatformPushOnNewChatMessageActionStrategy testSendPushOnNewChatMessageActionStrategy(
            MarketTplClient marketTplClient) {
        return Mockito.spy(new SendCourierPlatformPushOnNewChatMessageActionStrategy(marketTplClient));
    }

    @Bean
    @Primary
    public MarketTplClient marketTplClient(HttpClientFactory factory,
                                           ObjectSerializeService serializeService) {
        return Mockito.spy(new HttpMarketTplClient(factory, serializeService));
    }

    @Bean(name = "customerScriptServiceApi")
    public ScriptServiceApi ocrmCustomerScriptServiceApiProvider(CustomerService customerService,
                                                                 List<CustomerMarkerProvider> customerMarkerProviders,
                                                                 GlueProvider glueProvider) {
        return new CustomerScriptServiceApi(customerService, customerMarkerProviders, glueProvider);
    }
}
