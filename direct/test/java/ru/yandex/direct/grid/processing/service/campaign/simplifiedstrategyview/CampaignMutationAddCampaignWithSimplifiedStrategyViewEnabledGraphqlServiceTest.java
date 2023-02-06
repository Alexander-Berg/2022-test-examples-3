package ru.yandex.direct.grid.processing.service.campaign.simplifiedstrategyview;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddTextCampaign;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdAddDynamicCampaign;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdAddSmartCampaign;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdAddTextCampaign;

@GridProcessingTest
@RunWith(SpringRunner.class)
public class CampaignMutationAddCampaignWithSimplifiedStrategyViewEnabledGraphqlServiceTest {
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

    @Autowired
    private MetrikaClientStub metrikaClient;

    @Autowired
    private CampaignConstantsService campaignConstantsService;

    private User operator;
    private ClientInfo clientInfo;

    private static CampaignAttributionModel defaultAttributionModel;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);

        defaultAttributionModel = campaignConstantsService.getDefaultAttributionModel();
    }

    @Test
    public void shouldAddTextCampaignWithSimplifiedStrategyViewEnabled() {
        GdAddTextCampaign gdAddTextCampaign = defaultGdAddTextCampaign(defaultAttributionModel)
                .withIsSimplifiedStrategyViewEnabled(true);

        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion()
                .withTextCampaign(gdAddTextCampaign);
        GdAddCampaigns input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        GdAddCampaignPayload gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator);

        assertThat(gdAddCampaignPayload.getValidationResult()).isNull();
        assertThat(gdAddCampaignPayload.getAddedCampaigns()).hasSize(1);

        TextCampaign actualCampaign = (TextCampaign) campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(),
                List.of(gdAddCampaignPayload.getAddedCampaigns().get(0).getId())).get(0);
        assertThat(actualCampaign.getIsSimplifiedStrategyViewEnabled()).isTrue();
    }

    @Test
    public void shouldAddSmartCampaignWithSimplifiedStrategyViewEnabled() {
        //given
        var campaign = defaultGdAddSmartCampaign(defaultAttributionModel)
                .withIsSimplifiedStrategyViewEnabled(true);

        var gdAddCampaignUnion = new GdAddCampaignUnion()
                .withSmartCampaign(campaign);
        var input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        metrikaClient.addUserCounterIds(clientInfo.getUid(), campaign.getMetrikaCounters());

        //when
        var result = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator);

        //then
        assertThat(result.getValidationResult()).isNull();
        assertThat(result.getAddedCampaigns()).hasSize(1);
        SmartCampaign actualCampaign = (SmartCampaign) campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(),
                List.of(result.getAddedCampaigns().get(0).getId())).get(0);
        assertThat(actualCampaign.getIsSimplifiedStrategyViewEnabled()).isTrue();
    }

    @Test
    public void shouldAddDynamicCampaignWithSimplifiedStrategyViewEnabled() {
        //given
        var campaign = defaultGdAddDynamicCampaign(defaultAttributionModel)
                .withIsSimplifiedStrategyViewEnabled(true);

        var gdAddCampaignUnion = new GdAddCampaignUnion()
                .withDynamicCampaign(campaign);
        var input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        //when
        var result = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator);

        //then
        assertThat(result.getValidationResult()).isNull();
        assertThat(result.getAddedCampaigns()).hasSize(1);
        var actualCampaign = (DynamicCampaign) campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(),
                List.of(result.getAddedCampaigns().get(0).getId())).get(0);
        assertThat(actualCampaign.getIsSimplifiedStrategyViewEnabled()).isTrue();
    }
}
