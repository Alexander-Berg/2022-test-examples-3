package ru.yandex.autotests.direct.httpclient.util.mappers.groupsapitocmd;

import ru.yandex.autotests.direct.httpclient.data.CmdBeans.SiteLinksCmdBean;
import ru.yandex.autotests.direct.httpclient.util.mappers.basicConverters.ApiToCmdHrefConverter;
import ru.yandex.autotests.direct.httpclient.util.mappers.basicConverters.ApiToCmdHrefUrlProtocolConverter;
import ru.yandex.autotests.direct.httpclient.util.beanmapper.HierarchicBeanMappingBuilder;
import ru.yandex.autotests.directapi.common.api45.Sitelink;

import static org.dozer.loader.api.FieldsMappingOptions.customConverter;

/**
 * Created by shmykov on 13.04.15.
 */
public class SiteLinksBeanMappingBuilder extends HierarchicBeanMappingBuilder {
    @Override
    protected void configure() {
        mapping(SiteLinksCmdBean.class, Sitelink.class)
                .fields("urlProtocol", "href", customConverter(ApiToCmdHrefUrlProtocolConverter.class))
                .fields("href", "href", customConverter(ApiToCmdHrefConverter.class));

    }
}
