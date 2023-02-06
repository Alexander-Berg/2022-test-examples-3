package ru.yandex.direct.grid.processing.service.campaign.abexperiment;

import java.util.EnumSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaigns;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.rbac.ClientPerm;
import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.getGdAddInternalAutobudgetCampaignRequest;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringRunner.class)
public class CampaignMutationAddExperimentInternalCampaignGraphqlServiceTest {
    private static final String MUTATION_NAME = "addCampaigns";
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
            + "    addedCampaigns {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final GraphQlTestExecutor.TemplateMutation<GdAddCampaigns, GdAddCampaignPayload> ADD_CAMPAIGN_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
                    GdAddCampaigns.class, GdAddCampaignPayload.class);

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
    public void addCampaignWithExperiment() {
        RetConditionInfo defaultABSegmentRetCondition =
                steps.retConditionSteps().createDefaultABSegmentRetCondition(clientInfo);
        var campaign = getGdAddInternalAutobudgetCampaignRequest()
                .withAbSegmentRetargetingConditionId(defaultABSegmentRetCondition.getRetConditionId())
                .withAbSegmentStatisticRetargetingConditionId(defaultABSegmentRetCondition.getRetConditionId());

        var input = new GdAddCampaigns()
                .withCampaignAddItems(List.of(new GdAddCampaignUnion()
                        .withInternalAutobudgetCampaign(campaign)));

        var payload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION, input, operator, subjectUser);
        assertThat(payload.getValidationResult()).isNull();
        assertThat(payload.getAddedCampaigns()).hasSize(1);

        InternalAutobudgetCampaign actualCampaign =
                (InternalAutobudgetCampaign) campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(),
                mapList(payload.getAddedCampaigns(), GdAddCampaignPayloadItem::getId)).get(0);

        assertThat(actualCampaign.getAbSegmentRetargetingConditionId())
                .isEqualTo(defaultABSegmentRetCondition.getRetConditionId());
        assertThat(actualCampaign.getAbSegmentStatisticRetargetingConditionId())
                .isEqualTo(defaultABSegmentRetCondition.getRetConditionId());
    }

}
