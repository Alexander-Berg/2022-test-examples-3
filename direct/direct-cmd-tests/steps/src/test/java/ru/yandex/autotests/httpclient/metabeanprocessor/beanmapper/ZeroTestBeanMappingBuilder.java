package ru.yandex.autotests.httpclient.metabeanprocessor.beanmapper;

import ru.yandex.autotests.direct.httpclient.util.beanmapper.HierarchicBeanMappingBuilder;
import ru.yandex.autotests.direct.httpclient.util.beanmapper.InnerBuilder;
import ru.yandex.autotests.httpclient.metabeanprocessor.beans.ZeroTestBean;
import ru.yandex.autotests.httpclient.metabeanprocessor.beans.apilikebean.ApiLikeZeroTestBean;

/**
 * Created by shmykov on 09.02.15.
 */
public class ZeroTestBeanMappingBuilder extends HierarchicBeanMappingBuilder {

    @InnerBuilder
    protected FirstBeanMappingBuilder firstBeanMappingBuilder;

    @Override
    protected void configure() {
        mapping(ZeroTestBean.class, ApiLikeZeroTestBean.class)
                .fields("array", "list");
    }
}