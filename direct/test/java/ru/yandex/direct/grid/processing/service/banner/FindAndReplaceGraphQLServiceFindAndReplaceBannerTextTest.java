package ru.yandex.direct.grid.processing.service.banner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceLinkMode;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceOptions;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceText;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceTextInstruction;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdReplacementMode;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestSitelinkSets.sitelinkSet;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelink;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsTargetType.BODY;
import static ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsTargetType.SITELINK_DESCRIPTION;
import static ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsTargetType.SITELINK_TITLE;
import static ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsTargetType.TITLE;
import static ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsTargetType.TITLE_EXTENSION;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FindAndReplaceGraphQLServiceFindAndReplaceBannerTextTest {

    private static final String DEFAULT_SITELINK_TITLE = "Default";
    private static final String DEFAULT_SITELINK_DESCRIPTION = "Default description";
    private static final String DEFAULT_BANNER_TITLE = "Default banner title";
    private static final String DEFAULT_BANNER_TITLE_EXTENSION = "Default banner title extension";
    private static final String DEFAULT_BANNER_BODY = "Default banner body";
    private static final String REPLACE_TARGET = "Default";
    private static final String REPLACEMENT = "Gorgeous";
    private static final Set<Long> FULL_HOUSE = Set.of(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L);

    private static final String FIND_AND_REPLACE_BANNERS_TEXT_PREVIEW_QUERY =
            "getFindAndReplaceTextPreview";
    private static final String FIND_AND_REPLACE_BANNERS_TEXT_MUTATION = "findAndReplaceText";

    private static final String PREVIEW_QUERY_TEMPLATE = ""
            + "query {\n"
            + "  %s (input: %s) {\n"
            + "    totalCount,\n"
            + "    rowset {\n"
            + "      adId\n"
            + "      changes{\n"
            + "        target\n"
            + "        oldValue\n"
            + "        newValue\n"
            + "        ... on GdFindAndReplaceSitelinkChangeItem {\n"
            + "          sitelinkId\n"
            + "          orderNum\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final String FIND_AND_REPLACE_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    totalCount,\n"
            + "    successCount,\n"
            + "    rowset {\n"
            + "      adId\n"
            + "      changes{\n"
            + "        target\n"
            + "        oldValue\n"
            + "        newValue\n"
            + "        ... on GdFindAndReplaceSitelinkChangeItem {\n"
            + "          sitelinkId\n"
            + "          orderNum\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;

    private GdFindAndReplaceText request;
    private User operator;

    private OldTextBanner banner;
    private Sitelink sitelink;

    @Before
    public void initTestData() {
        var clientInfo = steps.clientSteps().createDefaultClient();
        var campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        sitelink = defaultSitelink()
                .withTitle(DEFAULT_SITELINK_TITLE)
                .withDescription(DEFAULT_SITELINK_DESCRIPTION)
                .withHref("https://yandex.ru/");
        SitelinkSet sitelinkSet = sitelinkSet(clientInfo.getClientId(), List.of(sitelink));
        sitelinkSet = steps.sitelinkSetSteps().createSitelinkSet(sitelinkSet, clientInfo).getSitelinkSet();
        banner = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null)
                                .withTitle(DEFAULT_BANNER_TITLE)
                                .withTitleExtension(DEFAULT_BANNER_TITLE_EXTENSION)
                                .withBody(DEFAULT_BANNER_BODY)
                                .withSitelinksSetId(sitelinkSet.getId()),
                        campaignInfo).getBanner();
        operator = UserHelper.getUser(clientInfo.getClient());

        request = new GdFindAndReplaceText()
                .withAdIds(List.of(banner.getId()))
                .withTargetTypes(Set.of(TITLE, TITLE_EXTENSION, BODY, SITELINK_TITLE, SITELINK_DESCRIPTION))
                .withReplaceInstruction(new GdFindAndReplaceTextInstruction()
                        .withSearch(REPLACE_TARGET)
                        .withReplace(REPLACEMENT)
                        .withOptions(new GdFindAndReplaceOptions()
                                .withCaseSensitive(true)
                                .withReplacementMode(GdReplacementMode.FIND_AND_REPLACE)
                                .withLinkReplacementMode(GdFindAndReplaceLinkMode.FULL)
                                .withSitelinkOrderNumsToUpdateDescription(FULL_HOUSE)
                                .withSitelinkOrderNumsToUpdateHref(emptySet())
                                .withSitelinkOrderNumsToUpdateTitle(FULL_HOUSE))
                );
    }

    @Test
    public void testPreview() {
        var query = String.format(PREVIEW_QUERY_TEMPLATE,
                FIND_AND_REPLACE_BANNERS_TEXT_PREVIEW_QUERY,
                graphQlSerialize(request));

        var result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        Map<String, Object> expectedPayload = Map.of(
                FIND_AND_REPLACE_BANNERS_TEXT_PREVIEW_QUERY,
                Map.of(
                        "totalCount", request.getAdIds().size(),
                        "rowset", List.of(expectedItem())));

        Map<String, Object> data = result.getData();
        assertThat(data).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void testReplace() {
        request.getReplaceInstruction().withReplace("Wow");
        var query = String.format(FIND_AND_REPLACE_MUTATION_TEMPLATE,
                FIND_AND_REPLACE_BANNERS_TEXT_MUTATION,
                graphQlSerialize(request));

        var result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        Map<String, Object> expectedPayload = Map.of(
                FIND_AND_REPLACE_BANNERS_TEXT_MUTATION,
                Map.of(
                        "totalCount", request.getAdIds().size(),
                        "successCount", request.getAdIds().size(),
                        "rowset", List.of(expectedItem())));

        Map<String, Object> data = result.getData();
        assertThat(data).is(matchedBy(beanDiffer(expectedPayload)));
    }

    private Map<String, Object> expectedItem() {
        return Map.of(
                "adId", banner.getId(),
                "changes", expectedChangeItems());
    }

    private List<Map<String, Object>> expectedChangeItems() {
        var search = request.getReplaceInstruction().getSearch();
        var replace = request.getReplaceInstruction().getReplace();
        List<Map<String, Object>> changes = new ArrayList<>();
        changes.add(Map.of(
                "target", TITLE.name(),
                "oldValue", banner.getTitle(),
                "newValue", banner.getTitle().replace(search, replace)));
        changes.add(Map.of(
                "target", TITLE_EXTENSION.name(),
                "oldValue", banner.getTitleExtension(),
                "newValue", banner.getTitleExtension().replace(search, replace)));
        changes.add(Map.of(
                "target", BODY.name(),
                "oldValue", banner.getBody(),
                "newValue", banner.getBody().replace(search, replace)));
        changes.add(Map.of(
                "target", SITELINK_TITLE.name(),
                "sitelinkId", sitelink.getId(),
                "orderNum", sitelink.getOrderNum(),
                "oldValue", sitelink.getTitle(),
                "newValue", sitelink.getTitle().replace(search, replace)));
        changes.add(Map.of(
                "target", SITELINK_DESCRIPTION.name(),
                "sitelinkId", sitelink.getId(),
                "orderNum", sitelink.getOrderNum(),
                "oldValue", sitelink.getDescription(),
                "newValue", sitelink.getDescription().replace(search, replace)));
        return changes;
    }
}
