package ru.yandex.direct.intapi.entity.absegment.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.absegment.model.AbSegmentModel;
import ru.yandex.direct.intapi.entity.absegment.model.AbSegmentsList;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;


@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AbSegmentServiceTest {

    @Autowired
    private AbSegmentService abSegmentService;
    @Autowired
    private Steps steps;

    private Long campaignId;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        campaignId = campaignInfo.getCampaignId();
    }

    @Test
    public void saveAbSegments_and_getAbSegments() {
        AbSegmentsList expected = createAbSegmentList(campaignId);
        abSegmentService.saveAbSegments(expected);
        AbSegmentsList result = abSegmentService.getAbSegments(singletonList(campaignId));
        assertThat(result, beanDiffer(expected));
    }

    @Test
    public void unbindAbSegments() {
        AbSegmentsList expected = createAbSegmentList(campaignId);
        abSegmentService.saveAbSegments(expected);
        AbSegmentsList result = abSegmentService.getAbSegments(singletonList(campaignId));
        assertThat(result, beanDiffer(expected));

        abSegmentService.unbindAbSegments(singletonList(campaignId));
        result = abSegmentService.getAbSegments(singletonList(campaignId));
        assertTrue(result.getConditions().isEmpty());
    }

    private static AbSegmentsList createAbSegmentList(Long campaignId) {
        AbSegmentModel abSegmentModel = new AbSegmentModel();
        abSegmentModel.setExperimentId(123L);
        abSegmentModel.setSegmentId(456L);
        abSegmentModel.setCampaignId(campaignId);
        AbSegmentsList abSegmentsList = new AbSegmentsList();
        abSegmentsList.setConditions(singletonList(abSegmentModel));
        return abSegmentsList;
    }
}
