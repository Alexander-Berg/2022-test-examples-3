package ru.yandex.market.crm.operatorwindow.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.common.rest.TvmTicketProvider;
import ru.yandex.market.ocrm.module.checkouter.CheckouterService;
import ru.yandex.market.ocrm.module.checkouter.CheckouterServiceHelper;
import ru.yandex.market.ocrm.module.checkouter.InteractiveCheckouterService;
import ru.yandex.market.ocrm.module.checkouter.ModuleCheckouterTestConfiguration;
import ru.yandex.market.ocrm.module.checkouter.RobotCheckouterService;

@Configuration
@Import({
        ModuleCheckouterTestConfiguration.class
})
public class CheckouterTestConfiguration {

    @Bean
    RobotCheckouterService robotCheckouterService(CheckouterAPI checkouterAPI,
                                                  TvmTicketProvider tvmTicketProvider,
                                                  CheckouterServiceHelper checkouterServiceHelper) {
        return new RobotCheckouterService(checkouterAPI, tvmTicketProvider, checkouterServiceHelper);
    }

    @Bean
    InteractiveCheckouterService interactiveCheckouterService(CheckouterAPI checkouterAPI,
                                                              TvmTicketProvider tvmTicketProvider,
                                                              CheckouterServiceHelper checkouterServiceHelper) {
        return new InteractiveCheckouterService(checkouterAPI, tvmTicketProvider, checkouterServiceHelper);
    }

    @Bean
    CheckouterService checkouterService(CheckouterAPI checkouterAPI,
                                        TvmTicketProvider tvmTicketProvider,
                                        CheckouterServiceHelper checkouterServiceHelper) {
        return new CheckouterService(checkouterAPI, tvmTicketProvider, checkouterServiceHelper);
    }

}
