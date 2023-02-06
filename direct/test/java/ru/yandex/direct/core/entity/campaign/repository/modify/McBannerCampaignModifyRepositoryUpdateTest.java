package ru.yandex.direct.core.entity.campaign.repository.modify;

import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.utils.CampaignModifyTestUtils.getExpectedMcBannerCampaign;
import static ru.yandex.direct.core.entity.campaign.utils.CampaignModifyTestUtils.getMcBannerCampaignModelChanges;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class McBannerCampaignModifyRepositoryUpdateTest {

    private static final List<String> SSP_LIST = List.of("ImSSP");

    @Autowired
    public CampaignModifyRepository campaignModifyRepository;
    @Autowired
    CampaignTypedRepository campaignTypedRepository;
    @Autowired
    public Steps steps;

    private CampaignInfo mcBannerCampaignInfo;
    private McBannerCampaign updatingMcBannerCampaign;
    private McBannerCampaign newMcBannerCampaign;

    @Before
    public void before() {
        mcBannerCampaignInfo = steps.campaignSteps().createActiveMcBannerCampaign();
        steps.sspPlatformsSteps().addSspPlatforms(SSP_LIST);

        updatingMcBannerCampaign = new McBannerCampaign()
                .withId(mcBannerCampaignInfo.getCampaignId())
                .withName(mcBannerCampaignInfo.getCampaign().getName());

        newMcBannerCampaign = new McBannerCampaign()
                .withId(mcBannerCampaignInfo.getCampaignId())
                .withName("newName" + RandomStringUtils.randomAlphabetic(7));
    }

    @Test
    public void update() {
        McBannerCampaign expectedCampaign =
                updateMcBannerCampaign(updatingMcBannerCampaign, null, newMcBannerCampaign);

        List<? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaigns(
                mcBannerCampaignInfo.getShard(), singletonList(mcBannerCampaignInfo.getCampaignId()));
        List<McBannerCampaign> mcBannerCampaigns = mapList(typedCampaigns, McBannerCampaign.class::cast);
        McBannerCampaign actualCampaign = mcBannerCampaigns.get(0);

        assertThat(actualCampaign).is(matchedBy(beanDiffer(expectedCampaign)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }

    public McBannerCampaign updateMcBannerCampaign(McBannerCampaign updatingMcBannerCampaign,
                                                   List<Long> metrikaCounters,
                                                   McBannerCampaign newMcBannerCampaign) {
        ModelChanges<McBannerCampaign> mcBannerCampaignModelChanges =
                getMcBannerCampaignModelChanges(metrikaCounters, newMcBannerCampaign);

        AppliedChanges<McBannerCampaign> mcBannerCampaignAppliedChanges =
                mcBannerCampaignModelChanges.applyTo(updatingMcBannerCampaign);

        RestrictedCampaignsUpdateOperationContainer updateParameters =
                RestrictedCampaignsUpdateOperationContainer.create(
                        mcBannerCampaignInfo.getShard(),
                        mcBannerCampaignInfo.getUid(),
                        mcBannerCampaignInfo.getClientId(),
                        mcBannerCampaignInfo.getUid(),
                        mcBannerCampaignInfo.getUid());

        campaignModifyRepository
                .updateCampaigns(updateParameters, singletonList(mcBannerCampaignAppliedChanges));

        return getExpectedMcBannerCampaign(mcBannerCampaignInfo, mcBannerCampaignModelChanges);
    }
}
