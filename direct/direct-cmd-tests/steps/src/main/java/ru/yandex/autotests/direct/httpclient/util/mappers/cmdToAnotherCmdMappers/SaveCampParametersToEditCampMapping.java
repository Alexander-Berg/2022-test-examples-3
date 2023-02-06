package ru.yandex.autotests.direct.httpclient.util.mappers.cmdToAnotherCmdMappers;

import ru.yandex.autotests.direct.httpclient.data.campaigns.SaveCampParameters;
import ru.yandex.autotests.direct.httpclient.data.campaigns.editcamp.EditCampResponse;
import ru.yandex.autotests.direct.httpclient.util.beanmapper.HierarchicBeanMappingBuilder;
import ru.yandex.autotests.direct.httpclient.util.mappers.cmdToAnotherCmdMappers.converters.DeviceTargetingConverter;
import ru.yandex.autotests.direct.httpclient.util.mappers.cmdToAnotherCmdMappers.converters.IntegerInverterConverter;

import static org.dozer.loader.api.FieldsMappingOptions.customConverter;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 15.04.15
 */
public class SaveCampParametersToEditCampMapping extends HierarchicBeanMappingBuilder {

    @Override
    protected void configure() {
        mapping(SaveCampParameters.class, EditCampResponse.class)
                .fields("cid", "campaign.campaignID")
                .fields("name", "campaign.name")
                .fields("fio", "campaign.fio")
                .fields("start_date", "campaign.startDate")
                .fields("finish_date", "campaign.finishDate")
                .fields("email", "campaign.email")
                .fields("jsonStartegy", "campaign.strategy")
                .fields("currency", "campaign.campaignCurrency")
                .fields("metrika_counters", "campaign.additionalMetrikaCounters")
                .fields("smsTime", "campaign.smsTime")

                .fields("time_target_holiday", "campaign.timeTarget.showOnHolidays")
                .fields("time_target_holiday_from", "campaign.timeTarget.holidayShowFrom")
                .fields("time_target_holiday_to", "campaign.timeTarget.holidayShowTo")
                .fields("timeTarget", "campaign.timeTarget.timeTarget")
                .fields("timezone_id", "campaign.timeTarget.timeZone")
                .fields("timezone_text", "campaign.timeTarget.timeZoneText")
                .fields("time_target_working_holiday", "campaign.timeTarget.workingHolidays")

                .fields("ContextLimit", "campaign.contextLimitSum")
                .fields("ContextPriceCoef", "campaign.contextPricePercent")
                .fields("autoOptimization", "campaign.autoOptimization")
                .fields("DontShow", "campaign.disabledDomains")
                .fields("disabledIps", "campaign.disabledIps")
                .fields("json_campaign_minus_words.minusWords", "campaign.minusKeywords")
                .fields("geo", "campaign.geo")

                .fields("country", "campaign.contactInfo.country")
                .fields("city", "campaign.contactInfo.city")
                .fields("country_code", "campaign.contactInfo.countryCode")
                .fields("city_code", "campaign.contactInfo.cityCode")
                .fields("phone", "campaign.contactInfo.phone")
                .fields("ci_name", "campaign.contactInfo.companyName")
                .fields("contactperson", "campaign.contactInfo.contactPerson")
                .fields("worktime", "campaign.contactInfo.worktime")
                .fields("street", "campaign.contactInfo.street")
                .fields("house", "campaign.contactInfo.house")
                .fields("build", "campaign.contactInfo.build")
                .fields("apart", "campaign.contactInfo.apart")
                .fields("contact_email", "campaign.contactInfo.contactEmail")
                .fields("im_client", "campaign.contactInfo.IMClient")
                .fields("im_login", "campaign.contactInfo.IMLogin")
                .fields("extra_message", "campaign.contactInfo.extraMessage")
                .fields("OGRN", "campaign.contactInfo.OGRN")

                .fields("broad_match_flag", "campaign.broadMatchFlag")
                .fields("broad_match_limit", "campaign.broadMatchLimit")
                .fields("broadMatchRate", "campaign.broadMatchRate")
                .fields("mediaType", "campaign.mediaType")
                .fields("isRelatedKeywordsEnabled", "campaign.isRelatedKeywordsEnabled")
                .fields("active_orders_money_out_sms", "campaign.activeOrdersMoneyOutSms")
                .fields("moderate_result_sms", "campaign.moderateResultSms")
                .fields("notify_order_money_in_sms", "campaign.notifyOrderMoneyInSms")
                .fields("banners_per_page", "campaign.bannersPerPage")
                .fields("extendedGeotargeting", "campaign.noExtendedGeotargeting",
                        customConverter(IntegerInverterConverter.class))
                .fields("device_targeting", "campaign.deviceTargeting", customConverter(DeviceTargetingConverter.class))
                .fields("competitors_domains", "campaign.competitorsDomains");


    }
}
