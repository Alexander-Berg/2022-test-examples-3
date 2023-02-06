package ru.yandex.direct.grid.processing.service.campaign;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignsMinusKeywords;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignsMinusKeywordsAction;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignMutationGraphQlService.UPDATE_CAMPAIGNS_MINUS_KEYWORDS;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampMutationGraphqlServiceMinusKeywordsTest {

    private static final String MUTATION_NAME = UPDATE_CAMPAIGNS_MINUS_KEYWORDS;
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

    private static final GraphQlTestExecutor.TemplateMutation<GdUpdateCampaignsMinusKeywords,
            GdUpdateCampaignPayload> UPDATE_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
                    GdUpdateCampaignsMinusKeywords.class, GdUpdateCampaignPayload.class);

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

    private int shard;
    private User operator;
    private Long textCampaignId;
    private Long textCampaignId2;
    private Long mcBannerCampaignId;
    private Long cpmBannerCampaignId;
    private String minusKeyword;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        minusKeyword = RandomStringUtils.randomAlphabetic(20);
        textCampaignId = steps.campaignSteps().createActiveTextCampaign(clientInfo).getCampaignId();
        textCampaignId2 = steps.campaignSteps().createCampaign(
                activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withMinusKeywords(List.of(minusKeyword)), clientInfo).getCampaignId();

        mcBannerCampaignId = steps.mcBannerCampaignSteps().createDefaultCampaign(clientInfo).getId();
        cpmBannerCampaignId = steps.cpmBannerCampaignSteps().createDefaultCampaign(clientInfo).getId();
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void testSuccess_add() {
        var input = createRequest(textCampaignId, List.of(minusKeyword), GdUpdateCampaignsMinusKeywordsAction.ADD);
        var payload = testExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);

        var expectedPayloadItem = new GdUpdateCampaignPayloadItem().withId(textCampaignId);
        var expectedPayload = new GdUpdateCampaignPayload().withUpdatedCampaigns(List.of(expectedPayloadItem));

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));

        TextCampaign actualCampaign = (TextCampaign) campaignTypedRepository
                .getTypedCampaigns(shard, Set.of(textCampaignId, textCampaignId2)).get(0);

        assertThat(actualCampaign.getMinusKeywords()).contains(minusKeyword);
    }

    @Test
    public void testMcBannerCampaign_add() {
        var input = createRequest(mcBannerCampaignId, List.of(minusKeyword), GdUpdateCampaignsMinusKeywordsAction.ADD);
        var payload = testExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);

        var expectedPayloadItem = new GdUpdateCampaignPayloadItem().withId(mcBannerCampaignId);
        var expectedPayload = new GdUpdateCampaignPayload().withUpdatedCampaigns(List.of(expectedPayloadItem));

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));

        McBannerCampaign actualCampaign =
                (McBannerCampaign) campaignTypedRepository.getTypedCampaigns(shard, Set.of(mcBannerCampaignId)).get(0);

        assertThat(actualCampaign.getMinusKeywords()).contains(minusKeyword);
    }

    @Test
    public void testSuccess_remove() {
        var input = createRequest(textCampaignId2, List.of(minusKeyword), GdUpdateCampaignsMinusKeywordsAction.REMOVE);
        var payload = testExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);

        var expectedPayloadItem = new GdUpdateCampaignPayloadItem().withId(textCampaignId2);
        var expectedPayload = new GdUpdateCampaignPayload().withUpdatedCampaigns(List.of(expectedPayloadItem));

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));

        TextCampaign actualCampaign = (TextCampaign) campaignTypedRepository
                .getTypedCampaigns(shard, Set.of(textCampaignId2)).get(0);

        assertThat(actualCampaign.getMinusKeywords()).isEmpty();
    }


    @Test
    public void testCpmBanner_WithDisabledFeature() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.IS_CPM_BANNER_CAMPAIGN_DISABLED, true);
        var input = createRequest(cpmBannerCampaignId, List.of(minusKeyword), GdUpdateCampaignsMinusKeywordsAction.ADD);
        var result = testExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);

        assertThat(result.getValidationResult().getErrors()).hasSize(1);
        GdDefect defect = new GdDefect()
                .withPath("campaignUpdateItems[0].id")
                .withCode("CampaignDefectIds.Gen.CAMPAIGN_NO_WRITE_RIGHTS");
        assertThat(result.getValidationResult().getErrors().get(0)).isEqualTo(defect);
    }

    private static GdUpdateCampaignsMinusKeywords createRequest(Long campaignId, List<String> minusKeywords,
                                                                GdUpdateCampaignsMinusKeywordsAction action) {
        return new GdUpdateCampaignsMinusKeywords()
                .withCampaignIds(List.of(campaignId))
                .withMinusKeywords(minusKeywords)
                .withAction(action);
    }

}
