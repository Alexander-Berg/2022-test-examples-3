package ru.yandex.autotests.direct.httpclient.util.mappers.groupsapitocmd;

import ru.yandex.autotests.direct.httpclient.data.CmdBeans.SiteLinksCmdBean;
import ru.yandex.autotests.direct.httpclient.util.beanmapper.HierarchicBeanMappingBuilder;
import ru.yandex.autotests.directapi.common.api45.Sitelink;

/**
 * Created by shmykov on 13.04.15.
 */
public class SiteLinksShowCampBeanMappingBuilder extends HierarchicBeanMappingBuilder {
    @Override
    protected void configure() {
        mapping(SiteLinksCmdBean.class, Sitelink.class)
                .fields("href", "href");

    }
}
