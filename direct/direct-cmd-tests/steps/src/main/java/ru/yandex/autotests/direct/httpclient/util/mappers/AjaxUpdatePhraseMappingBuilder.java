package ru.yandex.autotests.direct.httpclient.util.mappers;

import org.dozer.loader.api.BeanMappingBuilder;
import ru.yandex.autotests.direct.httpclient.data.phrases.AjaxUpdateShowConditionsBean;
import ru.yandex.autotests.direct.httpclient.util.mappers.basicConverters.TwoDigitScalePriceConverter;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.directapi.common.api45.BannerPhraseInfo;

import static org.dozer.loader.api.FieldsMappingOptions.customConverter;

/**
 * Created by shmykov on 28.05.15.
 */
public class AjaxUpdatePhraseMappingBuilder extends BeanMappingBuilder {

    private Currency clientCurrency = null;

    public AjaxUpdatePhraseMappingBuilder() {
    }

    public AjaxUpdatePhraseMappingBuilder(Currency clientCurrency) {
        this.clientCurrency = clientCurrency;
    }

    @Override
    protected void configure() {
        mapping(BannerPhraseInfo.class, AjaxUpdateShowConditionsBean.class)
                .fields("price", "price", customConverter(TwoDigitScalePriceConverter.class, clientCurrency.toString()));
    }
}