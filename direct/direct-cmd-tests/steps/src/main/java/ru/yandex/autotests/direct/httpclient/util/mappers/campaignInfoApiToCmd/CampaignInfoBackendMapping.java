package ru.yandex.autotests.direct.httpclient.util.mappers.campaignInfoApiToCmd;

import ru.yandex.autotests.direct.httpclient.data.campaigns.campaignInfo.CampaignInfoCmd;
import ru.yandex.autotests.direct.httpclient.util.mappers.ContactInfoApiToCmd.ContactInfoApiToCmdBeanMappingBuilder;
import ru.yandex.autotests.direct.httpclient.util.mappers.basicConverters.StringToDateConverter;
import ru.yandex.autotests.direct.httpclient.util.mappers.timeTargetInfoApiToCmd.TimeTargetInfoApiToCmdMapping;
import ru.yandex.autotests.direct.httpclient.util.beanmapper.HierarchicBeanMappingBuilder;
import ru.yandex.autotests.direct.httpclient.util.beanmapper.InnerBuilder;
import ru.yandex.autotests.direct.httpclient.util.mappers.basicConverters.YesNoToOneZeroConverter;
import ru.yandex.autotests.direct.httpclient.util.mappers.campaignInfoApiToCmd.converters.EmailNotificationConverter;
import ru.yandex.autotests.direct.httpclient.util.mappers.campaignInfoApiToCmd.converters.MinusKeywordsConverter;
import ru.yandex.autotests.direct.httpclient.util.mappers.campaignInfoApiToCmd.converters.MobileBidAdjustmentConverter;
import ru.yandex.autotests.direct.httpclient.util.mappers.campaignInfoApiToCmd.converters.SmsNotificationConverter;
import ru.yandex.autotests.directapi.common.api45.CampaignInfo;

import static org.dozer.loader.api.FieldsMappingOptions.customConverter;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 03.04.15
 */
public class CampaignInfoBackendMapping extends HierarchicBeanMappingBuilder {

    @InnerBuilder
    protected TimeTargetInfoApiToCmdMapping timeTargetInfoApiToCmdMapping;

    @InnerBuilder
    protected ContactInfoApiToCmdBeanMappingBuilder contactInfoApiToCmdBeanMappingBuilder;

    @Override
    protected void configure() {
        mapping(CampaignInfoCmd.class, CampaignInfo.class)
                .fields("email", "emailNotification", customConverter(EmailNotificationConverter.class))
                .fields("smsTime", "smsNotification", customConverter(SmsNotificationConverter.class))
                .fields("autoOptimization", "autoOptimization", customConverter(YesNoToOneZeroConverter.class))
                .fields("mobileBidAdjustment", "mobileBidAdjustment", customConverter(MobileBidAdjustmentConverter.class))
                .fields("statusContextStop", "statusContextStop", customConverter(YesNoToOneZeroConverter.class))
                .fields("statusMetricaControl", "statusMetricaControl", customConverter(YesNoToOneZeroConverter.class))
                .fields("statusOpenStat", "statusOpenStat", customConverter(YesNoToOneZeroConverter.class))
               // .fields("minusKeywords", "minusKeywords", customConverter(MinusKeywordsConverter.class))
                .fields("startDate", "startDate", customConverter(StringToDateConverter.class));
    }
}
