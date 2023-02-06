package ru.yandex.direct.grid.processing.service.campaign;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignsOrganization;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignMutationGraphQlService.UPDATE_CAMPAIGNS_ORGANIZATION;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignMutationGraphqlServiceOrganizationTest {
    private static final String MUTATION_NAME = UPDATE_CAMPAIGNS_ORGANIZATION;
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

    private static final GraphQlTestExecutor.TemplateMutation<GdUpdateCampaignsOrganization,
            GdUpdateCampaignPayload> UPDATE_ORGANIZATION_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
                    GdUpdateCampaignsOrganization.class, GdUpdateCampaignPayload.class);

    @Autowired
    private GraphQlTestExecutor testExecutor;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    public Steps steps;

    @Autowired
    public DslContextProvider dslContextProvider;

    @Autowired
    CampaignTypedRepository campaignTypedRepository;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private User operator;
    private CampaignInfo validCampaignInfo;
    private Long defaultPermalinkId;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClientAndUser();
        validCampaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        CampaignInfo validCampaignInfo2 = steps.campaignSteps().createActiveTextCampaign(clientInfo);

        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
        defaultPermalinkId = validCampaignInfo2.getCampaign().getDefaultPermalink();
    }

    @Test
    public void testSuccess() {

        var input = createRequest(validCampaignInfo, defaultPermalinkId);
        var payload = testExecutor.doMutationAndGetPayload(UPDATE_ORGANIZATION_MUTATION, input, operator);

        var expectedPayloadItem = new GdUpdateCampaignPayloadItem().withId(validCampaignInfo.getCampaignId());
        var expectedPayload = new GdUpdateCampaignPayload().withUpdatedCampaigns(List.of(expectedPayloadItem));

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));

        TextCampaign actualCampaign =
                (TextCampaign) campaignTypedRepository.getTypedCampaigns(validCampaignInfo.getShard(),
                        Set.of(validCampaignInfo.getCampaignId())).get(0);

        assertThat(actualCampaign.getDefaultPermalinkId()).isEqualTo(defaultPermalinkId);
    }

    private static GdUpdateCampaignsOrganization createRequest(CampaignInfo info, Long defaultPermalinkId) {
        return new GdUpdateCampaignsOrganization()
                .withCampaignIds(List.of(info.getCampaignId()))
                .withDefaultPermalinkId(defaultPermalinkId);
    }
}
