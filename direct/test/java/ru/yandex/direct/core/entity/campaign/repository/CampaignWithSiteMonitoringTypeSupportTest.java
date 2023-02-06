package ru.yandex.direct.core.entity.campaign.repository;

import java.util.Collections;
import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithSiteMonitoring;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.utils.CampaignModifyTestUtils.getDynamicCampaignModelChanges;
import static ru.yandex.direct.core.entity.campaign.utils.CampaignModifyTestUtils.getExpectedCampaignByCampaignType;
import static ru.yandex.direct.core.entity.campaign.utils.CampaignModifyTestUtils.getMcBannerCampaignModelChanges;
import static ru.yandex.direct.core.entity.campaign.utils.CampaignModifyTestUtils.getTextCampaignModelChanges;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCampaignByCampaignType;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class CampaignWithSiteMonitoringTypeSupportTest {

    @Autowired
    public CampaignModifyRepository campaignModifyRepository;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    public Steps steps;

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private CampaignInfo campaignInfo;
    private BaseCampaign expectedCampaign;
    private BaseCampaign actualCampaign;

    public void before(CampaignType campaignsType,
                       boolean statusMetricaControlBeforeUpdate,
                       boolean updateStatusMetricaControl) {
        Campaign campaign = activeCampaignByCampaignType(campaignsType, null, null)
                .withStatusMetricaControl(statusMetricaControlBeforeUpdate);
        campaignInfo = steps.campaignSteps().createCampaign(campaign);

        CampaignWithSiteMonitoring updatingCampaign =
                (CampaignWithSiteMonitoring) TestCampaigns.newCampaignByCampaignType(campaignsType)
                        .withId(campaignInfo.getCampaignId())
                        .withName(campaignInfo.getCampaign().getName());
        updatingCampaign.withHasSiteMonitoring(statusMetricaControlBeforeUpdate);

        CommonCampaign newCampaign = TestCampaigns.newCampaignByCampaignType(campaignsType)
                .withId(campaignInfo.getCampaignId())
                .withName("newName" + RandomStringUtils.randomAlphabetic(7));

        expectedCampaign = updateCampaign(campaignsType, (CommonCampaign) updatingCampaign, newCampaign, updateStatusMetricaControl);

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                        Collections.singletonList(campaignInfo.getCampaignId()));
        List<? extends BaseCampaign> campaigns =
                mapList(typedCampaigns, c -> TestCampaigns.getCampaignClassByCampaignType(campaignsType).cast(c));
        actualCampaign = campaigns.get(0);
    }

    private BaseCampaign updateCampaign(CampaignType campaignsType,
                                        CommonCampaign updatingCampaign,
                                        CommonCampaign newCampaign,
                                        boolean newHasSiteMonitoring) {
        ModelChanges<? extends CommonCampaign> campaignModelChanges =
                getCampaignModelChangesByCampaignType(campaignsType, newCampaign, newHasSiteMonitoring);

        AppliedChanges<? extends CommonCampaign> campaignAppliedChanges = applyTo(campaignsType, campaignModelChanges, updatingCampaign);

        RestrictedCampaignsUpdateOperationContainer updateParameters = RestrictedCampaignsUpdateOperationContainer.create(
                campaignInfo.getShard(),
                campaignInfo.getUid(),
                campaignInfo.getClientId(),
                campaignInfo.getUid(),
                campaignInfo.getUid()
        );

        campaignModifyRepository.updateCampaigns(updateParameters,
                Collections.singletonList(campaignAppliedChanges));

        return getExpectedCampaignByCampaignType(campaignsType, campaignInfo, campaignModelChanges);
    }

    @Test
    @TestCaseName("было {0}, стало {1}")
    @Parameters(method = "parametersForCheckUpdateHasSiteMonitoringFlag")
    public void checkUpdateHasSiteMonitoringFlag_textCampaign(boolean hasSiteMonitoringBeforeUpdate,
                                                              boolean updateHasSiteMonitoring) {
        before(CampaignType.TEXT, hasSiteMonitoringBeforeUpdate, updateHasSiteMonitoring);

        assertThat(actualCampaign).is(matchedBy(beanDiffer(expectedCampaign)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }

    @Test
    @TestCaseName("было {0}, стало {1}")
    @Parameters(method = "parametersForCheckUpdateHasSiteMonitoringFlag")
    public void checkUpdateHasSiteMonitoringFlag_dynamicCampaign(boolean hasSiteMonitoringBeforeUpdate,
                                                                 boolean updateHasSiteMonitoring) {
        before(CampaignType.DYNAMIC, hasSiteMonitoringBeforeUpdate, updateHasSiteMonitoring);

        assertThat(actualCampaign).is(matchedBy(beanDiffer(expectedCampaign)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }

    @Test
    @TestCaseName("было {0}, стало {1}")
    @Parameters(method = "parametersForCheckUpdateHasSiteMonitoringFlag")
    public void checkUpdateHasSiteMonitoringFlag_mcBannerCampaign(boolean hasSiteMonitoringBeforeUpdate,
                                                                  boolean updateHasSiteMonitoring) {
        before(CampaignType.MCBANNER, hasSiteMonitoringBeforeUpdate, updateHasSiteMonitoring);

        assertThat(actualCampaign).is(matchedBy(beanDiffer(expectedCampaign)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }

    public static Object[][] parametersForCheckUpdateHasSiteMonitoringFlag() {
        return new Object[][]{
                {true, true},
                {true, false},
                {false, true},
                {false, false}
        };
    }

    private ModelChanges<? extends CommonCampaign> getCampaignModelChangesByCampaignType(CampaignType campaignType,
                                                                                         CommonCampaign newCampaign,
                                                                                         boolean newHasSiteMonitoring) {
        if (campaignType == CampaignType.DYNAMIC) {
            return getDynamicCampaignModelChanges(null, (DynamicCampaign) newCampaign, newHasSiteMonitoring);
        } else if (campaignType == CampaignType.MCBANNER) {
            return getMcBannerCampaignModelChanges(null, (McBannerCampaign) newCampaign, newHasSiteMonitoring);
        }
        return getTextCampaignModelChanges(null, (TextCampaign) newCampaign, newHasSiteMonitoring);
    }

    private AppliedChanges<? extends CommonCampaign> applyTo(CampaignType campaignType,
                                                             ModelChanges<? extends CommonCampaign> campaignModelChanges,
                                                             CommonCampaign updatingCampaign) {
        if (campaignType == CampaignType.DYNAMIC) {
            return ((ModelChanges<DynamicCampaign>) campaignModelChanges).applyTo((DynamicCampaign) updatingCampaign);
        } else if (campaignType == CampaignType.MCBANNER) {
            return ((ModelChanges<McBannerCampaign>) campaignModelChanges).applyTo((McBannerCampaign) updatingCampaign);
        }
        return ((ModelChanges<TextCampaign>) campaignModelChanges).applyTo((TextCampaign) updatingCampaign);
    }
}
