package ru.yandex.direct.grid.processing.service.campaign.isuniversal;

import java.util.List;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddTextCampaign;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdAddTextCampaign;

@GridProcessingTest
@RunWith(SpringRunner.class)
public class CampaignMutationAddUniversalTextCampaignGraphqlServiceTest {
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
    private CampaignConstantsService campaignConstantsService;

    private User operator;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void addTextCampaignWithIsUniversalSavedCorrectly() {
        testUniversalityFlagBaseTest(true, true);
    }

    @Test
    public void addTextCampaignNullUniversalitySavedCorrectly() {
        testUniversalityFlagBaseTest(null, false);
    }

    @Test
    public void addTextCampaignFalseUniversalitySavedCorrectly() {
        testUniversalityFlagBaseTest(false, false);
    }

    private void testUniversalityFlagBaseTest(Boolean addedValue, Boolean expectedReturnValue) {
        GdAddTextCampaign gdAddTextCampaign =
                defaultGdAddTextCampaign(campaignConstantsService.getDefaultAttributionModel()).withIsUniversalCamp(addedValue);

        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion()
                .withTextCampaign(gdAddTextCampaign);
        GdAddCampaigns input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        GdAddCampaignPayload gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator);

        assertThat(gdAddCampaignPayload.getValidationResult(), Matchers.nullValue());
        assertThat(gdAddCampaignPayload.getAddedCampaigns(), Matchers.hasSize(1));

        TextCampaign actualCampaign = (TextCampaign) campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(),
                List.of(gdAddCampaignPayload.getAddedCampaigns().get(0).getId())).get(0);
        Matcher isUniversalFieldMatcher = expectedReturnValue == null ? Matchers.nullValue() :
                Matchers.is(expectedReturnValue);
        assertThat(actualCampaign.getIsUniversal(), isUniversalFieldMatcher);

    }
}
