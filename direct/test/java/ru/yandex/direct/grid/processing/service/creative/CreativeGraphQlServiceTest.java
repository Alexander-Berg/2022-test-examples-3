package ru.yandex.direct.grid.processing.service.creative;

import java.util.Map;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldPerformanceBanner;
import ru.yandex.direct.core.entity.creative.model.BannerStorageDictLayoutItem;
import ru.yandex.direct.core.entity.creative.model.BannerStorageDictThemeItem;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.CreativeBusinessType;
import ru.yandex.direct.core.entity.creative.model.StatusModerate;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestBannerStorageDictRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activePerformanceBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activePerformanceCampaign;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.list;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.map;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CreativeGraphQlServiceTest {

    @Autowired
    private GridContextProvider gridContextProvider;
    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private TestBannerStorageDictRepository testBannerStorageDictRepository;
    private UserInfo userInfo;
    private CreativeInfo creativeInfo;
    private GridGraphQLContext context;

    @Before
    public void setUp() {
        userInfo = steps.userSteps().createUser(generateNewUser());
        ClientInfo clientInfo = userInfo.getClientInfo();

        BannerStorageDictThemeItem theme = new BannerStorageDictThemeItem()
                .withId(19L)
                .withName("Розничная торговля");
        testBannerStorageDictRepository.addTheme(theme);
        BannerStorageDictLayoutItem layout = new BannerStorageDictLayoutItem()
                .withId(31L)
                .withImgSrc("http://storage.mds.yandex.net/get-bstor/63248/8___big+description+carousel_3.svg")
                .withName("Большое изображение товара с описанием и каруселью");
        testBannerStorageDictRepository.addLayout(layout);

        Creative creative = defaultPerformanceCreative(null, null)
                .withBusinessType(CreativeBusinessType.HOTELS)
                .withStatusModerate(StatusModerate.YES)
                .withThemeId(theme.getId())
                .withLayoutId(layout.getId());
        creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);

        context = ContextHelper.buildContext(userInfo.getUser());
        gridContextProvider.setGridContext(context);
    }

    @Test
    public void creatives_returnsLayoutAndThemeIfRequested() {
        ExecutionResult result =
                processor.processQuery(null, "query CreativesQuery($input: GdCreativesContainerInput!) {\n"
                                + "  creatives(input: $input) {\n"
                                + "    rowset {\n"
                                + "      __typename \n"
                                + "      ...creativeFragment\n"
                                + "    }\n"
                                + "  }\n"
                                + "}\n"
                                + "\n"
                                + "fragment creativeFragment on GdTypedCreative {\n"
                                + "  statusModerate\n"
                                + "  ... on GdSmartCreative {\n"
                                + "    businessType\n"
                                + "    layoutId\n"
                                + "    themeId\n"
                                + "    layout {\n"
                                + "      id\n"
                                + "      imgSrc\n"
                                + "      name\n"
                                + "    }\n"
                                + "    theme {\n"
                                + "      id\n"
                                + "      name\n"
                                + "    }\n"
                                + "  }\n"
                                + "}",
                        map("input", map(
                                "searchBy", map("userId", userInfo.getUid()),
                                "limitOffset", map(
                                        "offset", 0L,
                                        "limit", 1000L
                                ),
                                "filter", map("creativeIdIn", list(creativeInfo.getCreativeId())),
                                "cacheKey", ""
                        )),
                        context);
        assertThat(result.getErrors()).isEmpty();
        Map<String, Object> data = result.getData();
        assertThat(data).containsKey("creatives");

        DocumentContext context = JsonPath.parse(toJson(data.get("creatives")));
        String layoutName = context.read("$.rowset[0].layout.name");
        assertThat(layoutName).isEqualTo("Большое изображение товара с описанием и каруселью");
        String themeName = context.read("$.rowset[0].theme.name");
        assertThat(themeName).isEqualTo("Розничная торговля");
    }

    @Test
    public void creatives_returnsUsedInCampaigns() {
        ClientInfo clientInfo = userInfo.getClientInfo();
        // Создаём группу в кампании с заданным названием
        CampaignInfo campaignInfo = new CampaignInfo()
                .withClientInfo(clientInfo)
                .withCampaign(activePerformanceCampaign(null, null)
                        .withName("Тестовая смарт-кампания"));
        PerformanceAdGroupInfo adGroupInfo = new PerformanceAdGroupInfo()
                .withClientInfo(clientInfo)
                .withCampaignInfo(campaignInfo);
        PerformanceAdGroupInfo groupInfo = steps.adGroupSteps().addPerformanceAdGroup(adGroupInfo);
        // и привязываем к группе наш креатив через создание баннера
        OldPerformanceBanner banner =
                activePerformanceBanner(groupInfo.getCampaignId(), groupInfo.getAdGroupId(),
                        creativeInfo.getCreativeId());
        steps.bannerSteps().createBanner(banner, groupInfo);

        ExecutionResult result =
                processor.processQuery(null, "query CreativesQuery($input: GdCreativesContainerInput!) {\n"
                                + "  creatives(input: $input) {\n"
                                + "    rowset {\n"
                                + "      __typename \n"
                                + "      ...creativeFragment\n"
                                + "    }\n"
                                + "  }\n"
                                + "}\n"
                                + "\n"
                                + "fragment creativeFragment on GdTypedCreative {\n"
                                + "  ... on GdSmartCreative {\n"
                                + "    usedInCampaigns {\n"
                                + "      id\n"
                                + "      name\n"
                                + "    }\n"
                                + "  }\n"
                                + "}",
                        map("input", map(
                                "searchBy", map("userId", userInfo.getUid()),
                                "limitOffset", map(
                                        "offset", 0L,
                                        "limit", 1000L
                                ),
                                "filter", map("creativeIdIn", list(creativeInfo.getCreativeId())),
                                "cacheKey", ""
                        )),
                        context);
        assertThat(result.getErrors()).isEmpty();
        Map<String, Object> data = result.getData();
        assertThat(data).containsKey("creatives");

        DocumentContext context = JsonPath.parse(toJson(data.get("creatives")));
        Integer usedInCampaignId = context.read("$.rowset[0].usedInCampaigns[0].id");
        assertThat(usedInCampaignId).isEqualTo(groupInfo.getCampaignId().intValue());
        String usedInCampaignName = context.read("$.rowset[0].usedInCampaigns[0].name");
        assertThat(usedInCampaignName).isEqualTo(groupInfo.getCampaignInfo().getCampaign().getName());
    }
}
