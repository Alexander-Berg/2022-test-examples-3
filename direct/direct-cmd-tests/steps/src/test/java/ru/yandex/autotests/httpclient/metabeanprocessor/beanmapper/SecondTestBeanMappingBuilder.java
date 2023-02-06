package ru.yandex.autotests.httpclient.metabeanprocessor.beanmapper;

import org.dozer.loader.api.BeanMappingBuilder;
import ru.yandex.autotests.httpclient.metabeanprocessor.beans.SecondTestBean;
import ru.yandex.autotests.httpclient.metabeanprocessor.beans.apilikebean.ApiLikeSecondTestBean;

/**
 * Created by shmykov on 09.02.15.
 */
public class SecondTestBeanMappingBuilder extends BeanMappingBuilder {

    @Override
    protected void configure() {
        mapping(SecondTestBean.class, ApiLikeSecondTestBean.class)
                .fields("cid", "campaignId")
                .fields("fio", "credentials");
    }
}
