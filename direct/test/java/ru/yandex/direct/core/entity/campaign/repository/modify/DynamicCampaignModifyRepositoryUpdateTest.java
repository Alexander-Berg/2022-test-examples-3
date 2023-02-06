package ru.yandex.direct.core.entity.campaign.repository.modify;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.utils.CampaignModifyTestUtils.getDynamicCampaignModelChanges;
import static ru.yandex.direct.core.entity.campaign.utils.CampaignModifyTestUtils.getExpectedDynamicCampaign;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicCampaignModifyRepositoryUpdateTest {

    private static final List<String> SSP_LIST = List.of("ImSSP");

    @Autowired
    public CampaignModifyRepository campaignModifyRepository;
    @Autowired
    CampaignTypedRepository campaignTypedRepository;
    @Autowired
    public Steps steps;

    private CampaignInfo dynamicCampaignInfo;
    private DynamicCampaign updatingDynamicCampaign;
    private DynamicCampaign newDynamicCampaign;

    @Before
    public void before() {
        dynamicCampaignInfo = steps.campaignSteps().createActiveDynamicCampaign();
        steps.sspPlatformsSteps().addSspPlatforms(SSP_LIST);

        updatingDynamicCampaign = new DynamicCampaign()
                .withId(dynamicCampaignInfo.getCampaignId())
                .withName(dynamicCampaignInfo.getCampaign().getName());

        newDynamicCampaign = new DynamicCampaign()
                .withId(dynamicCampaignInfo.getCampaignId())
                .withName("newName" + RandomStringUtils.randomAlphabetic(7));

    }

    @Test
    public void update() {
        DynamicCampaign expectedCampaign = updateDynamicCampaign(updatingDynamicCampaign, null,
                newDynamicCampaign);

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(dynamicCampaignInfo.getShard(),
                        Collections.singletonList(dynamicCampaignInfo.getCampaignId()));
        List<DynamicCampaign> dynamicCampaigns = mapList(typedCampaigns, DynamicCampaign.class::cast);
        DynamicCampaign actualCampaign = dynamicCampaigns.get(0);

        assertThat(actualCampaign).is(matchedBy(beanDiffer(expectedCampaign)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }

    public DynamicCampaign updateDynamicCampaign(DynamicCampaign updatingDynamicCampaign,
                                                 List<Long> metrikaCounters,
                                                 DynamicCampaign newDynamicCampaign) {
        ModelChanges<DynamicCampaign> dynamicCampaignModelChanges =
                getDynamicCampaignModelChanges(metrikaCounters, newDynamicCampaign);

        AppliedChanges<DynamicCampaign> dynamicCampaignAppliedChanges =
                dynamicCampaignModelChanges.applyTo(updatingDynamicCampaign);

        RestrictedCampaignsUpdateOperationContainer updateParameters =
                RestrictedCampaignsUpdateOperationContainer.create(
                dynamicCampaignInfo.getShard(),
                dynamicCampaignInfo.getUid(),
                dynamicCampaignInfo.getClientId(),
                dynamicCampaignInfo.getUid(),
                dynamicCampaignInfo.getUid());

        campaignModifyRepository.updateCampaigns(updateParameters,
                Collections.singletonList(dynamicCampaignAppliedChanges));

        return getExpectedDynamicCampaign(dynamicCampaignInfo, dynamicCampaignModelChanges);
    }
}
