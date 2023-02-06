package ru.yandex.autotests.httpclient.metabeanprocessor.beanmapper;

import ru.yandex.autotests.direct.httpclient.util.beanmapper.HierarchicBeanMappingBuilder;
import ru.yandex.autotests.direct.httpclient.util.beanmapper.InnerBuilder;
import ru.yandex.autotests.httpclient.metabeanprocessor.beans.TestBean;
import ru.yandex.autotests.httpclient.metabeanprocessor.beans.apilikebean.ApiLikeFirstTestBean;

import static org.dozer.loader.api.FieldsMappingOptions.customConverter;

/**
 * Created by shmykov on 09.02.15.
 */
public class FirstBeanMappingBuilder extends HierarchicBeanMappingBuilder {

    @InnerBuilder
    protected SecondTestBeanMappingBuilder secondTestBeanMappingBuilder;

    @Override
    protected void configure() {
        mapping(TestBean.class, ApiLikeFirstTestBean.class)
                .fields("secondTestBean", "nestedTestBean")
                .fields("canUseDayBudget", "canUseDayBudget", customConverter(BooleanToStringConverter.class));
    }
}