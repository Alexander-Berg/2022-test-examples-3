package ru.yandex.autotests.direct.httpclient.util.mappers.groupsapitocmd;

import ru.yandex.autotests.direct.httpclient.data.CmdBeans.BannerCmdBean;
import ru.yandex.autotests.direct.httpclient.util.beanmapper.HierarchicBeanMappingBuilder;
import ru.yandex.autotests.direct.httpclient.util.beanmapper.InnerBuilder;
import ru.yandex.autotests.direct.httpclient.util.mappers.ContactInfoApiToCmd.ContactInfoApiToCmdBeanMappingBuilder;
import ru.yandex.autotests.direct.httpclient.util.mappers.basicConverters.PresenseToZeroOneConverter;
import ru.yandex.autotests.direct.httpclient.util.mappers.groupsapitocmd.converters.StatusBannerModerateConverter;
import ru.yandex.autotests.directapi.common.api45.BannerInfo;

import static org.dozer.loader.api.FieldsMappingOptions.customConverter;

/**
 * Created by shmykov on 13.04.15.
 */
public class BannerApiToShowCampCmdBeanMappingBuilder extends HierarchicBeanMappingBuilder {

    @InnerBuilder
    ContactInfoApiToCmdBeanMappingBuilder contactInfoApiToCmdBeanMappingBuilder;

    @InnerBuilder
    SiteLinksShowCampBeanMappingBuilder siteLinksApiToCmdBeanMappingBuilder;


    @Override
    protected void configure() {
        mapping(BannerInfo.class, BannerCmdBean.class)
                .fields("href", "href")
                .fields("href", "hasHref", customConverter(PresenseToZeroOneConverter.class))
                .fields("contactInfo", "hasVcard", customConverter(PresenseToZeroOneConverter.class))
                .fields("contactInfo", "contactInfo")
                .fields("statusBannerModerate", "statusBannerModerate", customConverter(StatusBannerModerateConverter.class));

    }
}
