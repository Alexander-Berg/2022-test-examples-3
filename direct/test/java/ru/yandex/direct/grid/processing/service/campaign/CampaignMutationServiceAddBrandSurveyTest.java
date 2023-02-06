package ru.yandex.direct.grid.processing.service.campaign;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.audience.client.YaAudienceClient;
import ru.yandex.direct.audience.client.model.CreateExperimentResponse;
import ru.yandex.direct.audience.client.model.CreateExperimentResponseEnvelope;
import ru.yandex.direct.audience.client.model.ExperimentSegmentResponse;
import ru.yandex.direct.core.entity.brandSurvey.BrandSurvey;
import ru.yandex.direct.core.entity.brandlift.repository.BrandSurveyRepository;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBrandLift;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignsAddBrandSurvey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.RECALC_BRAND_LIFT_CAMPAIGNS;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetMaxImpressionsCustomPeriodStrategy;
import static ru.yandex.direct.dbschema.ppc.Tables.BRAND_SURVEY;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignMutationServiceAddBrandSurveyTest {

    private static final String EXISTING_BRAND_SURVEY_ID = "brandSurveyIdExisting";

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignMutationService campaignMutationService;

    @Autowired
    private BrandSurveyRepository brandSurveyRepository;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private YaAudienceClient yaAudienceClient;

    private Long uid;
    private ClientId clientId;
    private int shard;
    private GridGraphQLContext context;
    private List<Long> campaignIds;


    @Before
    public void setUp() {
        var clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        uid = clientInfo.getUid();
        shard = clientInfo.getShard();
        campaignIds = createCampaigns(clientInfo);
        context = new GridGraphQLContext(
                new User().withUid(uid),
                new User().withClientId(clientId).withUid(uid)
        );

        steps.featureSteps().addClientFeature(clientId, FeatureName.BRAND_LIFT, true);
        steps.dbQueueSteps().registerJobType(RECALC_BRAND_LIFT_CAMPAIGNS);

        var exp = new CreateExperimentResponse()
                .withExperimentId(123L)
                .withExperimentSegments(List.of(new ExperimentSegmentResponse().withSegmentId(123L)));
        Mockito.when(yaAudienceClient.createExperiment(anyString(), any()))
                .thenReturn(new CreateExperimentResponseEnvelope().withCreateExperimentResponse(exp));

        brandSurveyRepository.addBrandSurvey(shard, new BrandSurvey()
                .withBrandSurveyId(EXISTING_BRAND_SURVEY_ID)
                .withName("brandSurveyName")
                .withRetargetingConditionId(1L)
                .withClientId(clientId.asLong())
                .withSegmentId(1L)
                .withExperimentId(1L)
        );
    }

    @After
    public void cleanUp() {
        Mockito.reset(yaAudienceClient);
        deleteBrandSurvey();
    }

    @Test
    public void addBrandSurvey_addNewForTwoCampaigns() {
        GdUpdateCampaignsAddBrandSurvey gdUpdateCampaignsAddBrandSurvey = new GdUpdateCampaignsAddBrandSurvey()
                .withCampaignIds(campaignIds)
                .withBrandSurveyId("brandSurveyId")
                .withBrandSurveyName("brandSurveyName");
        GdUpdateCampaignPayload gdUpdateCampaignPayload = campaignMutationService.addBrandSurvey(context,
                gdUpdateCampaignsAddBrandSurvey);

        List<BrandSurvey> brandSurveyId = brandSurveyRepository.getBrandSurvey(shard, "brandSurveyId");
        List<CampaignWithBrandLift> typedCampaigns = (List<CampaignWithBrandLift>) campaignTypedRepository.getTypedCampaigns(shard, campaignIds);

        assertThat(brandSurveyId).size().isEqualTo(1);
        assertThat(typedCampaigns.stream().map(CampaignWithBrandLift::getBrandSurveyId)).containsOnly("brandSurveyId");
        assertNull(gdUpdateCampaignPayload.getValidationResult());
    }

    @Test
    public void addBrandSurvey_addExistingForTwoCampaigns() {
        GdUpdateCampaignsAddBrandSurvey gdUpdateCampaignsAddBrandSurvey = new GdUpdateCampaignsAddBrandSurvey()
                .withCampaignIds(campaignIds)
                .withBrandSurveyId(EXISTING_BRAND_SURVEY_ID);
        GdUpdateCampaignPayload gdUpdateCampaignPayload = campaignMutationService.addBrandSurvey(context,
                gdUpdateCampaignsAddBrandSurvey);

        List<BrandSurvey> brandSurveyId = brandSurveyRepository.getBrandSurvey(shard, EXISTING_BRAND_SURVEY_ID);
        List<CampaignWithBrandLift> typedCampaigns = (List<CampaignWithBrandLift>) campaignTypedRepository.getTypedCampaigns(shard, campaignIds);

        assertThat(brandSurveyId).size().isEqualTo(1);
        assertThat(typedCampaigns.stream().map(CampaignWithBrandLift::getBrandSurveyId))
                .containsOnly(EXISTING_BRAND_SURVEY_ID);
        assertNull(gdUpdateCampaignPayload.getValidationResult());
    }

    @Test
    public void addBrandSurvey_addExistingAndRenameForTwoCampaigns() {
        GdUpdateCampaignsAddBrandSurvey gdUpdateCampaignsAddBrandSurvey = new GdUpdateCampaignsAddBrandSurvey()
                .withCampaignIds(campaignIds)
                .withBrandSurveyId(EXISTING_BRAND_SURVEY_ID)
                .withBrandSurveyName("newName");
        GdUpdateCampaignPayload gdUpdateCampaignPayload = campaignMutationService.addBrandSurvey(context,
                gdUpdateCampaignsAddBrandSurvey);

        List<BrandSurvey> brandSurveyId = brandSurveyRepository.getBrandSurvey(shard, EXISTING_BRAND_SURVEY_ID);
        List<CampaignWithBrandLift> typedCampaigns = (List<CampaignWithBrandLift>) campaignTypedRepository.getTypedCampaigns(shard, campaignIds);

        assertThat(brandSurveyId).size().isEqualTo(1);
        assertThat(brandSurveyId.stream().map(BrandSurvey::getName)).containsExactly("newName");
        assertThat(typedCampaigns.stream().map(CampaignWithBrandLift::getBrandSurveyId))
                .containsOnly(EXISTING_BRAND_SURVEY_ID);
        assertNull(gdUpdateCampaignPayload.getValidationResult());
    }


    private List<Long> createCampaigns(ClientInfo clientInfo) {
        CampaignInfo campaign = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(clientId, uid)
                        .withStatusModerate(StatusModerate.NEW)
                        .withStrategy(autobudgetMaxImpressionsCustomPeriodStrategy()),
                clientInfo);
        CampaignInfo secondCampaign = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(clientId, uid)
                        .withStatusModerate(StatusModerate.NEW)
                        .withStrategy(autobudgetMaxImpressionsCustomPeriodStrategy()),
                clientInfo);
        return List.of(campaign.getCampaignId(), secondCampaign.getCampaignId());
    }

    private void deleteBrandSurvey() {
        dslContextProvider.ppc(shard).deleteFrom(BRAND_SURVEY)
                .where(BRAND_SURVEY.CLIENT_ID.eq(clientId.asLong()))
                .execute();
    }
}
