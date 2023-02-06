package ru.yandex.direct.core.entity.performancefilter.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.direct.core.configuration.CoreConfiguration;
import ru.yandex.direct.core.entity.feed.model.BusinessType;
import ru.yandex.direct.core.entity.feed.model.FeedType;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterTab;
import ru.yandex.direct.core.entity.performancefilter.repository.PerformanceFilterRepository;
import ru.yandex.direct.core.entity.performancefilter.schema.FilterSchema;
import ru.yandex.direct.core.entity.performancefilter.validation.FilterConditionsValidator;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.validation.wrapper.ModelItemValidationBuilder;

import static ru.yandex.direct.dbschema.ppc.Tables.ADGROUPS_PERFORMANCE;
import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_PERFORMANCE;
import static ru.yandex.direct.dbschema.ppc.Tables.FEEDS;

@ContextConfiguration(classes = CoreConfiguration.class)
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore("Запускать вручную с выставленным -Dyandex.environment.type=development")
public class ManualPerformanceFilterServiceTest {
    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private PerformanceFilterStorage filterSchemaServiceStorage;

    @Autowired
    private PerformanceFilterRepository performanceFilterRepository;

    File file = new File("filter_errors.csv");

    @Test
    public void getPerformanceFilter() throws IOException {
        for (int i = 1; i < 473900; i += 1000) {
            check(i);
        }
    }

    private void check(long id) throws IOException {
        BusinessType businessType = BusinessType.RETAIL;
        FeedType feedType = FeedType.YANDEX_MARKET;

        List<PerformanceFilter> filters = dslContextProvider.ppc(1)
                .select(performanceFilterRepository.jooqMapper.getFieldsToRead())
                .from(BIDS_PERFORMANCE)
                .join(ADGROUPS_PERFORMANCE).on(ADGROUPS_PERFORMANCE.PID.eq(BIDS_PERFORMANCE.PID))
                .join(FEEDS).on(FEEDS.FEED_ID.eq(ADGROUPS_PERFORMANCE.FEED_ID))
                .where(FEEDS.BUSINESS_TYPE.eq(BusinessType.toSource(businessType))
                        .and(FEEDS.FEED_TYPE.eq(feedType.getTypedValue()))
                        .and(BIDS_PERFORMANCE.PERF_FILTER_ID.between(id, id + 1000))

                )
                .fetch(r -> performanceFilterRepository.fromDb(r));

        List<String> lst = new ArrayList<>();

        for (PerformanceFilter filter : filters) {
            ModelItemValidationBuilder<PerformanceFilter> vb = ModelItemValidationBuilder.of(filter);
            FilterSchema filterSchema = filterSchemaServiceStorage.getFilterSchema(filter);
            vb.list(PerformanceFilter.CONDITIONS)
                    .checkBy(new FilterConditionsValidator(filterSchema, PerformanceFilterTab.CONDITION));
            ValidationResult<PerformanceFilter, Defect> vr = vb.getResult();
            if (vr.hasAnyErrors()) {
                lst.add(
                        String.valueOf(filter.getId())
                                + ","
                                + vr.flattenErrors().size()
                                + ","
                                + vr.flattenErrors()
                                + ","
                                + PerformanceFilterConditionDBFormatSerializer.INSTANCE
                                .serialize(filter.getConditions())
                );
            }
        }

        FileUtils.writeLines(file, "utf-8", lst, true);
    }
}
