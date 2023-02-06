package ru.yandex.autotests.direct.cmd.util.mappers.perfGroupsRequestToResponse;

import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.editAdGroupsPerformance.GetPerformanceGroup;
import ru.yandex.autotests.direct.httpclient.util.beanmapper.HierarchicBeanMappingBuilder;

import static org.dozer.loader.api.FieldsMappingOptions.customConverter;

/**
 * Created by aleran on 23.11.2015.
 */
public class PerfGroupsBeanMappingBuilder extends HierarchicBeanMappingBuilder {
    @Override
    protected void configure() {
        mapping(Group.class, GetPerformanceGroup.class)
                .fields("minusKeywords", "minusKeywords", customConverter(MinusKeywordsConverter.class));

    }
}
