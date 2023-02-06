package ru.yandex.autotests.direct.httpclient.util.mappers.ContactInfoApiToCmd;

import org.dozer.loader.api.TypeMappingBuilder;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.ContactInfoCmdBean;
import ru.yandex.autotests.direct.httpclient.util.beanmapper.HierarchicBeanMappingBuilder;
import ru.yandex.autotests.direct.httpclient.util.mappers.ContactInfoApiToCmd.converters.ContactInfoApiToCmdWorkTimeConverter;
import ru.yandex.autotests.directapi.common.api45.ContactInfo;

import static org.dozer.loader.api.FieldsMappingOptions.customConverter;

/**
 * Created by shmykov on 13.04.15.
 */
public class ContactInfoApiToCmdBeanMappingBuilder extends HierarchicBeanMappingBuilder {

    @Override
    protected void configure() {
        TypeMappingBuilder typeMappingBuilder = mapping(ContactInfo.class, ContactInfoCmdBean.class)
                .fields("workTime", "workTime", customConverter(ContactInfoApiToCmdWorkTimeConverter.class));
    }
}
