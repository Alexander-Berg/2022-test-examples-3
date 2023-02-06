package ru.yandex.autotests.direct.httpclient.util.mappers.groupsapitocmd;

import ru.yandex.autotests.direct.httpclient.data.CmdBeans.PhraseCmdBean;
import ru.yandex.autotests.direct.httpclient.util.beanmapper.HierarchicBeanMappingBuilder;
import ru.yandex.autotests.direct.httpclient.util.mappers.basicConverters.PriceConverter;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.directapi.common.api45.BannerPhraseInfo;

import static org.dozer.loader.api.FieldsMappingOptions.customConverter;

/**
 * Created by shmykov on 15.05.15.
 * TESTIRT-4953
 */
public class PhraseApiToCmdBeanMappingBuilder extends HierarchicBeanMappingBuilder {

    private Currency clientCurrency = null;

    public PhraseApiToCmdBeanMappingBuilder(Currency clientCurrency) {
        this.clientCurrency = clientCurrency;
    }

    @Override
    protected void configure() {
        String currencyAbbreviation = clientCurrency != null ? clientCurrency.toString() : null;
        mapping(BannerPhraseInfo.class, PhraseCmdBean.class)
                .fields("price", "price", customConverter(PriceConverter.class, currencyAbbreviation));

    }
}