package ru.yandex.autotests.direct.httpclient.util.mappers.groupsapitocmd;

import ru.yandex.autotests.direct.httpclient.data.CmdBeans.GroupCmdBean;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.LightGroupCmdBean;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.PhraseCmdBean;
import ru.yandex.autotests.direct.httpclient.util.mappers.basicConverters.PriceConverter;
import ru.yandex.autotests.direct.httpclient.util.mappers.groupsapitocmd.converters.ApiToCmdMinusKeywordsConverter;
import ru.yandex.autotests.direct.httpclient.util.beanmapper.HierarchicBeanMappingBuilder;
import ru.yandex.autotests.direct.httpclient.util.beanmapper.InnerBuilder;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.directapi.common.api4.BannerPhraseInfo;
import ru.yandex.autotests.directapi.common.api45.BannerInfo;

import static org.dozer.loader.api.FieldsMappingOptions.customConverter;

public class GroupApiToCmdBeanMappingBuilder extends HierarchicBeanMappingBuilder {

    @InnerBuilder
    BannerApiToCmdBeanMappingBuilder bannerApiToCmdBeanMappingBuilder;

    public GroupApiToCmdBeanMappingBuilder() {
    }

    public GroupApiToCmdBeanMappingBuilder(Currency clientCurrency) {
        this.clientCurrency = clientCurrency;
    }

    private Currency clientCurrency = null;

    @Override
    protected void configure() {
        mapping(BannerInfo.class, GroupCmdBean.class)
//                .fields("minusKeywords", "minusKeywords", customConverter(ApiToCmdMinusKeywordsConverter.class))
                .exclude("phrases");
        mapping(BannerInfo.class, LightGroupCmdBean.class)
                .exclude("phrases");
    }
}