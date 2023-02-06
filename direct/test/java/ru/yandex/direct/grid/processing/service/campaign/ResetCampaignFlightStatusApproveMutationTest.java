package ru.yandex.direct.grid.processing.service.campaign;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusApprove;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusCorrect;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignIdsList;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.validation.result.Defect;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.campaignNotFound;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.inconsistentCampaignType;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.RECALC_BRAND_LIFT_CAMPAIGNS;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmPriceCampaignWithSystemFields;
import static ru.yandex.direct.core.validation.defects.RightsDefects.forbiddenToChange;
import static ru.yandex.direct.feature.FeatureName.CPM_PRICE_CAMPAIGN;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ResetCampaignFlightStatusApproveMutationTest {

    private static final String MUTATION_NAME = "resetCampaignFlightStatusApprove";
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    updatedCampaigns {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final GraphQlTestExecutor.TemplateMutation<GdCampaignIdsList, GdUpdateCampaignPayload>
            RESET_STATUS_APPROVE_MUTATION = new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
            GdCampaignIdsList.class, GdUpdateCampaignPayload.class);

    @Autowired
    private GraphQlTestExecutor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    private User operator;
    private ClientInfo client;
    private PricePackage pricePackage;

    @Before
    public void before() {
        client = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        steps.featureSteps().addClientFeature(client.getClientId(), CPM_PRICE_CAMPAIGN, true);

        steps.dbQueueSteps().registerJobType(RECALC_BRAND_LIFT_CAMPAIGNS);

        pricePackage = steps.pricePackageSteps().createApprovedPricePackageWithClients(client).getPricePackage();

        operator = UserHelper.getUser(client.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void successReset() {
        CpmPriceCampaign campaign = defaultCpmPriceCampaignWithSystemFields(client, pricePackage)
                .withShows(0L)
                .withFlightStatusApprove(PriceFlightStatusApprove.YES)
                .withFlightStatusCorrect(PriceFlightStatusCorrect.NO);
        steps.campaignSteps().createActiveCpmPriceCampaign(client, campaign);

        GdUpdateCampaignPayload response = resetStatusApproveGraphQl(campaign.getId());

        assertSuccessResponse(response, campaign.getId());
        assertCampaignFlightStatusApprove(campaign.getId(), PriceFlightStatusApprove.NEW);
    }

    @Test
    public void failReset() {
        client = steps.clientSteps().createDefaultClientWithRole(RbacRole.CLIENT);
        operator = UserHelper.getUser(client.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
        CpmPriceCampaign campaign = defaultCpmPriceCampaignWithSystemFields(client, pricePackage)
                .withShows(100L)
                .withFlightStatusApprove(PriceFlightStatusApprove.YES)
                .withFlightStatusCorrect(PriceFlightStatusCorrect.NO);
        steps.campaignSteps().createActiveCpmPriceCampaign(client, campaign);

        GdUpdateCampaignPayload response = resetStatusApproveGraphQl(campaign.getId());

        assertFailResponse(response, forbiddenToChange());
        assertCampaignFlightStatusApprove(campaign.getId(), PriceFlightStatusApprove.YES);
    }

    @Test
    public void wrongCampaignType() {
        CampaignInfo campaign = steps.campaignSteps().createActiveTextCampaign(client);
        GdUpdateCampaignPayload response = resetStatusApproveGraphQl(campaign.getCampaignId());
        assertFailResponse(response, inconsistentCampaignType());
    }

    @Test
    public void notFoundCampaign() {
        GdUpdateCampaignPayload response = resetStatusApproveGraphQl(Long.MAX_VALUE);
        assertFailResponse(response, campaignNotFound());
    }

    private GdUpdateCampaignPayload resetStatusApproveGraphQl(Long campaignId) {
        GdCampaignIdsList input = new GdCampaignIdsList().withCampaignIds(singletonList(campaignId));
        return processor.doMutationAndGetPayload(RESET_STATUS_APPROVE_MUTATION, input, operator);
    }

    private void assertSuccessResponse(GdUpdateCampaignPayload response, Long campaignId) {
        assertThat(response.getUpdatedCampaigns())
                .isEqualTo(singletonList(new GdUpdateCampaignPayloadItem().withId(campaignId)));
        assertThat(response.getValidationResult()).isNull();
    }

    private void assertFailResponse(GdUpdateCampaignPayload response, Defect<?> expectedDefect) {
        assertThat(response.getUpdatedCampaigns()).isEqualTo(singletonList(null));
        assertThat(response.getValidationResult()).is(matchedBy(hasErrorsWith(gridDefect(
                path(field(GdCampaignIdsList.CAMPAIGN_IDS), index(0)), expectedDefect))));
    }

    private void assertCampaignFlightStatusApprove(Long campaignId, PriceFlightStatusApprove expectedStatusApprove) {
        List<? extends BaseCampaign> campaigns =
                campaignTypedRepository.getTypedCampaigns(client.getShard(), singletonList(campaignId));
        CpmPriceCampaign campaign = (CpmPriceCampaign) campaigns.get(0);
        assertThat(campaign.getFlightStatusApprove()).isEqualTo(expectedStatusApprove);
    }
}
