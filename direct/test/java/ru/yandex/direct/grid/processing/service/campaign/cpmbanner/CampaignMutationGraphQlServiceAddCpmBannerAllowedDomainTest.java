package ru.yandex.direct.grid.processing.service.campaign.cpmbanner;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCmpBannerCampaign;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.service.campaign.cpmbanner.DefaultGdAddCpmBannerCampaign.defaultGdAddCpmBannerCampaign;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignMutationGraphQlServiceAddCpmBannerAllowedDomainTest {
    private static final List<String> ALLOWED_DOMAINS = List.of("vk.com", "mail.ru");
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
    private CampaignRepository campaignRepository;

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
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.SET_CAMPAIGN_ALLOWED_PAGE_IDS, true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.DISABLE_BILLING_AGGREGATES, true);

        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void addCpmBannerCampaign() {
        GdAddCmpBannerCampaign gdAddCpmBannerCampaign =
                defaultGdAddCpmBannerCampaign(campaignConstantsService.getDefaultAttributionModel())
                        .withAllowedDomains(ALLOWED_DOMAINS);

        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion()
                .withCpmBannerCampaign(gdAddCpmBannerCampaign);
        GdAddCampaigns input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        GdAddCampaignPayload gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator);

        assertThat(gdAddCampaignPayload.getValidationResult()).isNull();
        assertThat(gdAddCampaignPayload.getAddedCampaigns()).hasSize(1);

        Campaign actualCampaign = campaignRepository.getCampaigns(clientInfo.getShard(),
                List.of(gdAddCampaignPayload.getAddedCampaigns().get(0).getId())).get(0);
        assertThat(actualCampaign.getAllowedDomains()).containsOnlyElementsOf(ALLOWED_DOMAINS);
    }
}
