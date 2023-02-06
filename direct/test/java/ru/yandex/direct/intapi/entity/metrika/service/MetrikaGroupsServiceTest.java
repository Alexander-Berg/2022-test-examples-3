package ru.yandex.direct.intapi.entity.metrika.service;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.IntApiException;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.metrika.model.MetrikaGroupsParam;
import ru.yandex.direct.intapi.entity.metrika.model.MetrikaGroupsResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MetrikaGroupsServiceTest {

    @Autowired
    private MetrikaGroupsService metrikaGroupsService;

    @Autowired
    private Steps steps;

    @Test
    public void getGroups() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner();
        steps.bsFakeSteps().setOrderId(bannerInfo.getCampaignInfo());

        MetrikaGroupsParam param = new MetrikaGroupsParam()
                .withOrderId(bannerInfo.getCampaignInfo().getCampaign().getOrderId())
                .withGroupId(bannerInfo.getAdGroupId());

        List<MetrikaGroupsResult> results = metrikaGroupsService.getGroups(Collections.singletonList(param));

        assumeThat("В списке результатов 1 элемент", results, hasSize(1));

        MetrikaGroupsResult expected = (MetrikaGroupsResult) new MetrikaGroupsResult()
                .withGroupName(bannerInfo.getAdGroupInfo().getAdGroup().getName())
                .withGroupId(param.getGroupId())
                .withOrderId(param.getOrderId());

        assertThat("В ответе верные данные", results.get(0), beanDiffer(expected));
    }

    @Test(expected = IntApiException.class)
    public void throwsExceptionWhenValidationFails() {
        metrikaGroupsService.getGroups(null);
    }
}
