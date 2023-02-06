package ru.yandex.direct.grid.processing.service.campaign;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignsAddBannerHrefParams;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignMutationGraphQlService.UPDATE_CAMPAIGNS_BANNER_HREF_PARAMS;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignMutationGraphqlServiceBannerHrefParamsTest {
    private static final String MUTATION_NAME = UPDATE_CAMPAIGNS_BANNER_HREF_PARAMS;
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
            + "    updatedCampaigns {\n"
            + "      id\n"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final GraphQlTestExecutor.TemplateMutation<GdUpdateCampaignsAddBannerHrefParams,
            GdUpdateCampaignPayload> UPDATE_BANNER_HREF_PARAMS_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
                    GdUpdateCampaignsAddBannerHrefParams.class, GdUpdateCampaignPayload.class);

    @Autowired
    private GraphQlTestExecutor testExecutor;

    @Autowired
    public Steps steps;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testSuccess() {
        var firstCampaignInfo = steps.campaignSteps().createActiveTextCampaign();
        var secondCampaignInfo = steps.campaignSteps().createActiveCampaign(firstCampaignInfo.getClientInfo());
        var firstCampaignId = firstCampaignInfo.getCampaignId();
        var secondCampaignId = secondCampaignInfo.getCampaignId();

        var operator = UserHelper.getUser(firstCampaignInfo.getClientInfo().getClient());
        TestAuthHelper.setDirectAuthentication(operator);

        var bannerHrefParams = "utm_mark=default";
        GdUpdateCampaignsAddBannerHrefParams input =
                new GdUpdateCampaignsAddBannerHrefParams()
                        .withCampaignIds(List.of(firstCampaignId, secondCampaignId))
                        .withBannerHrefParams(bannerHrefParams);

        var updatedCampaigns = testExecutor
                .doMutationAndGetPayload(UPDATE_BANNER_HREF_PARAMS_MUTATION, input, operator)
                .getUpdatedCampaigns();
        assertThat(updatedCampaigns)
                .as("Обе кампании успешно обновлены")
                .extracting(GdUpdateCampaignPayloadItem::getId)
                .containsExactlyInAnyOrder(firstCampaignId, secondCampaignId);
        var actualCampaign =
                campaignTypedRepository.getTypedCampaigns(firstCampaignInfo.getShard(),
                        Set.of(firstCampaignId, secondCampaignId));
        assertThat(actualCampaign)
                .as("Для всех кампаний обновился шаблон")
                .allMatch(campaign ->
                        ((TextCampaign) campaign).getBannerHrefParams().equals(bannerHrefParams)
                );
    }
}
