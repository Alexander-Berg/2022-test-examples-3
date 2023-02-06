package ru.yandex.autotests.direct.httpclient.util.mappers.groupsapitocmd;

import ru.yandex.autotests.direct.httpclient.data.CmdBeans.LightGroupCmdBean;
import ru.yandex.autotests.direct.httpclient.util.beanmapper.HierarchicBeanMappingBuilder;
import ru.yandex.autotests.direct.httpclient.util.mappers.basicConverters.PriceConverter;
import ru.yandex.autotests.direct.httpclient.util.mappers.groupsapitocmd.converters.ApiToCmdBudgetConverter;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.directapi.common.api45.CampaignStrategy;


import static org.dozer.loader.api.FieldsMappingOptions.customConverter;

/**
 * Created by shmykov on 23.04.15.
 */
public class CampaignStrategyToBudgetMappingBuilder extends HierarchicBeanMappingBuilder {

    private Currency clientCurrency = null;

    public CampaignStrategyToBudgetMappingBuilder() {
    }

    public CampaignStrategyToBudgetMappingBuilder(Currency clientCurrency) {
        this.clientCurrency = clientCurrency;
    }

    @Override
    protected void configure() {
        mapping(CampaignStrategy.class, LightGroupCmdBean.class)
                .fields("strategyName", "budgetStrategy", customConverter(ApiToCmdBudgetConverter.class))
                .fields("weeklySumLimit", "manualAutobudgetSum", customConverter(PriceConverter.class, clientCurrency.toString()));
    }
}
