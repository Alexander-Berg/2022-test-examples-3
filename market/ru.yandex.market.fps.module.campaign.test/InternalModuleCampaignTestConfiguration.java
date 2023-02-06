package ru.yandex.market.fps.module.campaign.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import(ModuleCampaignTestConfiguration.class)
public class InternalModuleCampaignTestConfiguration extends AbstractModuleConfiguration {
    protected InternalModuleCampaignTestConfiguration() {
        super("module/campaign/test");
    }

}
