package ru.yandex.direct.intapi.entity.metrika.service;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.tables.records.BidsPerformanceRecord;
import ru.yandex.direct.intapi.IntApiException;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.metrika.model.MetrikaPerformanceFiltersParam;
import ru.yandex.direct.intapi.entity.metrika.model.MetrikaPerformanceFiltersResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MetrikaPerformanceFiltersServiceTest {

    @Autowired
    private MetrikaPerformanceFiltersService metrikaPerformanceFiltersService;

    @Autowired
    private Steps steps;

    @Test
    public void getPerformanceFilters() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        steps.bsFakeSteps().setOrderId(adGroupInfo.getCampaignInfo());
        BidsPerformanceRecord perfFilter = steps.performanceFilterSteps().addDefaultBidsPerformance(adGroupInfo);
        steps.performanceFilterSteps().addDefaultBidsPerformance(adGroupInfo);

        MetrikaPerformanceFiltersParam param = new MetrikaPerformanceFiltersParam()
                .withOrderId(adGroupInfo.getCampaignInfo().getCampaign().getOrderId())
                .withPerfFilterId(perfFilter.getPerfFilterId());

        List<MetrikaPerformanceFiltersResult> results =
                metrikaPerformanceFiltersService.getPerformanceFilters(Collections.singletonList(param));

        assumeThat("В списке результатов 1 элемент", results, hasSize(1));

        MetrikaPerformanceFiltersResult expected =
                (MetrikaPerformanceFiltersResult) new MetrikaPerformanceFiltersResult()
                        .withName(perfFilter.getName())
                        .withPerfFilterId(param.getPerfFilterId())
                        .withOrderId(param.getOrderId());

        assertThat("В ответе верные данные", results.get(0), beanDiffer(expected));
    }

    @Test(expected = IntApiException.class)
    public void throwsExceptionWhenValidationFails() {
        metrikaPerformanceFiltersService.getPerformanceFilters(null);
    }
}
