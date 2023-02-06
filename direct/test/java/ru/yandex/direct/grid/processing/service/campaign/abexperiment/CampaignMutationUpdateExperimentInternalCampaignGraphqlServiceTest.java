package ru.yandex.direct.grid.processing.service.campaign.abexperiment;

import java.util.EnumSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.InternalDistribCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaigns;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.rbac.ClientPerm;
import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.getGdUpdateInternalDistribCampaignRequest;

@GridProcessingTest
@RunWith(SpringRunner.class)
public class CampaignMutationUpdateExperimentInternalCampaignGraphqlServiceTest {
    private static final String UPDATE_MUTATION_NAME = "updateCampaigns";
    private static final String UPDATE_MUTATION_TEMPLATE = ""
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
    private static final GraphQlTestExecutor.TemplateMutation<GdUpdateCampaigns, GdUpdateCampaignPayload> UPDATE_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(UPDATE_MUTATION_NAME, UPDATE_MUTATION_TEMPLATE,
                    GdUpdateCampaigns.class, GdUpdateCampaignPayload.class);
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private GraphQlTestExecutor processor;

    @Autowired
    private Steps steps;

    private User operator;
    private ClientInfo clientInfo;
    private User subjectUser;

    @Before
    public void before() {
        clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();
        subjectUser = clientInfo.getChiefUserInfo().getUser()
                .withPerms(EnumSet.of(ClientPerm.INTERNAL_AD_PRODUCT));

        var operatorClientInfo =
                steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.INTERNAL_AD_ADMIN);
        operator = operatorClientInfo.getChiefUserInfo().getUser();
        TestAuthHelper.setDirectAuthentication(operator, subjectUser);

        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.AB_SEGMENTS, true);
    }

    @Test
    public void updateCampaign() {
        CampaignInfo campaign =
                steps.campaignSteps().createActiveInternalDistribCampaign(clientInfo);

        RetConditionInfo defaultABSegmentRetCondition =
                steps.retConditionSteps().createDefaultABSegmentRetCondition(clientInfo);

        var campaignRequest = getGdUpdateInternalDistribCampaignRequest(campaign.getCampaignId())
                .withAbSegmentRetargetingConditionId(defaultABSegmentRetCondition.getRetConditionId())
                .withAbSegmentStatisticRetargetingConditionId(defaultABSegmentRetCondition.getRetConditionId());

        var gdUpdateCampaignUnion = new GdUpdateCampaignUnion().withInternalDistribCampaign(campaignRequest);

        var input = new GdUpdateCampaigns()
                .withCampaignUpdateItems(List.of(gdUpdateCampaignUnion));

        var payload = processor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator, subjectUser);

        assertThat(payload.getValidationResult()).isNull();
        assertThat(payload.getUpdatedCampaigns()).hasSize(1);

        InternalDistribCampaign actualCampaign = (InternalDistribCampaign) campaignTypedRepository
                .getTypedCampaigns(clientInfo.getShard(), List.of(campaign.getCampaignId())).get(0);

        assertThat(actualCampaign.getAbSegmentRetargetingConditionId())
                .isEqualTo(defaultABSegmentRetCondition.getRetConditionId());
        assertThat(actualCampaign.getAbSegmentStatisticRetargetingConditionId())
                .isEqualTo(defaultABSegmentRetCondition.getRetConditionId());
    }
}
