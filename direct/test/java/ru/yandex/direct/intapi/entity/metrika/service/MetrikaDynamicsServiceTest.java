package ru.yandex.direct.intapi.entity.metrika.service;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.tables.records.BidsDynamicRecord;
import ru.yandex.direct.dbschema.ppc.tables.records.DynamicConditionsRecord;
import ru.yandex.direct.intapi.IntApiException;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.metrika.model.MetrikaDynamicsParam;
import ru.yandex.direct.intapi.entity.metrika.model.MetrikaDynamicsResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MetrikaDynamicsServiceTest {

    @Autowired
    private MetrikaDynamicsService metrikaDynamicsService;

    @Autowired
    private Steps steps;

    @Test
    public void getDynamics() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        AdGroupInfo adGroupInfo2 = steps.adGroupSteps().createActiveTextAdGroup();

        steps.bsFakeSteps().setOrderId(adGroupInfo.getCampaignInfo());

        DynamicConditionsRecord dynamicCondition =
                steps.dynamicConditionsFakeSteps().addDefaultDynamicCondition(adGroupInfo);

        BidsDynamicRecord dynamic =
                steps.dynamicConditionsFakeSteps().addDefaultBidsDynamic(adGroupInfo2, dynamicCondition.getDynCondId());

        steps.dynamicConditionsFakeSteps().addDefaultBidsDynamic(adGroupInfo, dynamicCondition.getDynCondId());

        MetrikaDynamicsParam param = new MetrikaDynamicsParam()
                .withDynCondId(dynamic.getDynCondId())
                .withOrderId(adGroupInfo.getCampaignInfo().getCampaign().getOrderId());

        List<MetrikaDynamicsResult> results =
                metrikaDynamicsService.getDynamics(Collections.singletonList(param));

        assumeThat("В списке результатов 1 элемент", results, hasSize(1));

        MetrikaDynamicsResult expected = (MetrikaDynamicsResult) new MetrikaDynamicsResult()
                .withConditionName(dynamicCondition.getConditionName())
                .withDynCondId(dynamic.getDynId())
                .withOrderId(param.getOrderId());

        assertThat("В ответе верные данные", results.get(0), beanDiffer(expected));
    }

    @Test(expected = IntApiException.class)
    public void throwsExceptionWhenValidationFails() {
        metrikaDynamicsService.getDynamics(null);
    }
}
