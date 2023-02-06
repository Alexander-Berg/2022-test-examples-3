package ru.yandex.market.fps.module.campaign.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.fps.module.campaign.ModuleCampaignConfiguration;
import ru.yandex.market.fps.module.mbi.test.ModuleMbiTestConfiguration;
import ru.yandex.market.fps.module.supplier.communication.test.ModuleSupplierCommunicationTestConfiguration;
import ru.yandex.market.fps.module.vendor.partner.test.ModuleVendorPartnerTestConfiguration;
import ru.yandex.market.fps.ticket.test.ModuleFpsTicketTestConfiguration;

@Configuration
@Import({
        ModuleCampaignConfiguration.class,
        ModuleFpsTicketTestConfiguration.class,
        ModuleMbiTestConfiguration.class,
        ModuleVendorPartnerTestConfiguration.class,
        ModuleSupplierCommunicationTestConfiguration.class,
})
@PropertySource(value = "classpath:/module/campaign/test/test.properties")
public class ModuleCampaignTestConfiguration {
}
