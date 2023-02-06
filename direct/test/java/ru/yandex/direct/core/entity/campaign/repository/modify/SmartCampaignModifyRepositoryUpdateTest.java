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
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
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
import static ru.yandex.direct.core.entity.campaign.utils.CampaignModifyTestUtils.getExpectedSmartCampaign;
import static ru.yandex.direct.core.entity.campaign.utils.CampaignModifyTestUtils.getSmartCampaignModelChanges;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SmartCampaignModifyRepositoryUpdateTest {

    private static final List<String> SSP_LIST = List.of("ImSSP");

    @Autowired
    public CampaignModifyRepository campaignModifyRepository;
    @Autowired
    CampaignTypedRepository campaignTypedRepository;
    @Autowired
    public Steps steps;

    private CampaignInfo smartCampaignInfo;
    private SmartCampaign updatingSmartCampaign;
    private SmartCampaign newSmartCampaign;

    @Before
    public void before() {
        smartCampaignInfo = steps.campaignSteps().createActivePerformanceCampaign();
        steps.sspPlatformsSteps().addSspPlatforms(SSP_LIST);

        updatingSmartCampaign = new SmartCampaign()
                .withId(smartCampaignInfo.getCampaignId())
                .withName(smartCampaignInfo.getCampaign().getName());

        newSmartCampaign = new SmartCampaign()
                .withId(smartCampaignInfo.getCampaignId())
                .withName("newName" + RandomStringUtils.randomAlphabetic(7));


    }

    @Test
    public void update() {
        SmartCampaign expectedCampaign = updateSmartCampaign(updatingSmartCampaign, null,
                newSmartCampaign);

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(smartCampaignInfo.getShard(),
                        Collections.singletonList(smartCampaignInfo.getCampaignId()));
        List<SmartCampaign> smartCampaigns = mapList(typedCampaigns, SmartCampaign.class::cast);
        SmartCampaign actualCampaign = smartCampaigns.get(0);

        assertThat(actualCampaign).is(matchedBy(beanDiffer(expectedCampaign)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }

    public SmartCampaign updateSmartCampaign(SmartCampaign updatingSmartCampaign,
                                             List<Long> metrikaCounters,
                                             SmartCampaign newSmartCampaign) {
        ModelChanges<SmartCampaign> smartCampaignModelChanges =
                getSmartCampaignModelChanges(metrikaCounters, newSmartCampaign);

        AppliedChanges<SmartCampaign> smartCampaignAppliedChanges =
                smartCampaignModelChanges.applyTo(updatingSmartCampaign);

        RestrictedCampaignsUpdateOperationContainer updateParameters =
                RestrictedCampaignsUpdateOperationContainer.create(
                smartCampaignInfo.getShard(),
                smartCampaignInfo.getUid(),
                smartCampaignInfo.getClientId(),
                smartCampaignInfo.getUid(),
                smartCampaignInfo.getUid());

        campaignModifyRepository.updateCampaigns(updateParameters,
                Collections.singletonList(smartCampaignAppliedChanges));

        return getExpectedSmartCampaign(smartCampaignInfo, smartCampaignModelChanges);
    }
}
