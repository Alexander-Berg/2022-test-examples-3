package ru.yandex.direct.grid.processing.service.campaign;


import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.data.TestBanners;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.TurboLandingSteps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static java.util.Collections.singleton;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.steps.ClientSteps.DEFAULT_SHARD;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignsGraphQLServiceCalltrackingTest {

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {userId: \"%s\"}) {\n"
            + "    campaigns(input: %s) {\n"
            + "      rowset {\n"
            + "        ... on GdTextCampaign {\n"
            + "          id\n"
            + "          calltrackingSettingsId\n"
            + "          domains\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";
    private static final String CAMPAIGN_HREF = "http://test.ru";
    private static final String CAMPAIGN_DOMAIN = "test.ru";

    private static final String FIRST_BANNER_HREF = "https://xn--d1aqf.xn--p1ai";
    private static final String FIRST_BANNER_DOMAIN = "дом.рф";

    private static final String SECOND_BANNER_HREF = "https://домик.рф";
    private static final String SECOND_BANNER_DOMAIN = "домик.рф";

    private static final String TURBOLANDING_DOMAIN = "url_for_calltracking.turbo.site";
    private static final String TURBOLANDING_HREF = TURBOLANDING_DOMAIN + "/homepage";


    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private GridContextProvider gridContextProvider;
    @Autowired
    private UserService userService;
    @Autowired
    private Steps steps;

    @Test
    public void testWithCalltrackingSettings() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        steps.campAdditionalDataSteps().addHref(clientInfo.getShard(), campaignInfo.getCampaignId(), CAMPAIGN_HREF);

        var firstTextBannerInfo = new TextBannerInfo()
                .withBanner(TestBanners.activeTextBanner().withHref(FIRST_BANNER_HREF))
                .withCampaignInfo(campaignInfo);
        steps.bannerSteps().createBannerInActiveTextAdGroup(firstTextBannerInfo);

        var secondTextBannerInfo = new TextBannerInfo()
                .withBanner(TestBanners.activeTextBanner().withHref(SECOND_BANNER_HREF))
                .withCampaignInfo(campaignInfo);
        steps.bannerSteps().createBannerInActiveTextAdGroup(secondTextBannerInfo);

        steps.campCalltrackingSettingsSteps().link(DEFAULT_SHARD, campaignInfo.getCampaignId(), 45L);

        Map<String, Object> data = sendRequest(campaignInfo);

        Long id = getDataValue(data, "client/campaigns/rowset/0/id");
        Long calltrackingSettingsId = getDataValue(data, "client/campaigns/rowset/0/calltrackingSettingsId");
        List<String> domains = getDataValue(data, "client/campaigns/rowset/0/domains");
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(id).as("id").isEqualTo(campaignInfo.getCampaignId());
            soft.assertThat(calltrackingSettingsId).as("calltrackingSettingsId").isEqualTo(45L);
            soft.assertThat(domains).as("domains")
                    .isEqualTo(List.of(CAMPAIGN_DOMAIN, FIRST_BANNER_DOMAIN, SECOND_BANNER_DOMAIN));
        });
    }

    @Test
    public void testWithoutCalltrackingSettings() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);

        Map<String, Object> data = sendRequest(campaignInfo);

        Long id = getDataValue(data, "client/campaigns/rowset/0/id");
        List<String> domains = getDataValue(data, "client/campaigns/rowset/0/domains");
        Long calltrackingSettingsId = getDataValue(data, "client/campaigns/rowset/0/calltrackingSettingsId");
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(id).as("id").isEqualTo(campaignInfo.getCampaignId());
            soft.assertThat(calltrackingSettingsId).as("calltrackingSettingsId").isNull();
            soft.assertThat(domains).as("domains").isEmpty();
        });
    }

    @Test
    public void testWithOnlyTurbolandingsOnBanner() {
        // Проверяем случай, когда на баннере задан только турболендинг

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        var turbolandingToCreate =
                TurboLandingSteps.defaultBannerTurboLanding(clientInfo.getClientId())
                        .withTurboSiteHref(TURBOLANDING_HREF);
        var turbolanding = steps.turboLandingSteps().createTurboLanding(clientInfo.getClientId(), turbolandingToCreate);

        var bannerToCreate = activeTextBanner()
                .withHref(null) // затрём ссылку на баннере
                .withDisplayHref(null)
                .withTurboLandingId(turbolanding.getId())
                .withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.YES);
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(bannerToCreate, clientInfo);
        CampaignInfo campaignInfo = bannerInfo.getCampaignInfo();

        Map<String, Object> data = sendRequest(campaignInfo);

        Long id = getDataValue(data, "client/campaigns/rowset/0/id");
        List<String> domains = getDataValue(data, "client/campaigns/rowset/0/domains");
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(id).as("id").isEqualTo(campaignInfo.getCampaignId());
            softly.assertThat(domains).as("domains")
                    .containsExactly(TURBOLANDING_DOMAIN);
        });
    }

    private Map<String, Object> sendRequest(CampaignInfo campaignInfo) {
        GridGraphQLContext context = getGridGraphQLContext(campaignInfo.getClientInfo().getUid());
        GdCampaignsContainer campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();
        campaignsContainer.getFilter().setCampaignIdIn(singleton(campaignInfo.getCampaignId()));
        String containerAsString = graphQlSerialize(campaignsContainer);
        String query = String.format(QUERY_TEMPLATE, campaignInfo.getClientInfo().getUid(), containerAsString);
        gridContextProvider.setGridContext(context);
        ExecutionResult result = processor.processQuery(null, query, null, context);
        checkErrors(result.getErrors());
        return result.getData();
    }

    private GridGraphQLContext getGridGraphQLContext(Long uid) {
        User user = userService.getUser(uid);
        return ContextHelper.buildContext(user)
                .withFetchedFieldsReslover(null);
    }
}
