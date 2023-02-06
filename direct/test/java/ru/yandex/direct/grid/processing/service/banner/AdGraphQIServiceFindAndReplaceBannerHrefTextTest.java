package ru.yandex.direct.grid.processing.service.banner;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import graphql.ExecutionResult;
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
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsHrefPayloadItem;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsHrefPreviewPayloadItemSitelink;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsHrefTargetType;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsHrefText;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceHrefTextHrefPart;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceHrefTextInstruction;
import ru.yandex.direct.grid.processing.model.common.GdCachedResult;
import ru.yandex.direct.grid.processing.model.common.GdResult;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestSitelinkSets.sitelinkSet;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelink;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQIServiceFindAndReplaceBannerHrefTextTest {

    private static final String FIND_AND_REPLACE_BANNERS_HREF_TEXT_PREVIEW_QUERY =
            "getFindAndReplaceHrefTextPreview";
    private static final String FIND_AND_REPLACE_BANNERS_HREF_TEXT_MUTATION = "findAndReplaceHrefText";
    private static final String PREVIEW_QUERY_TEMPLATE = ""
            + "query {\n"
            + "  %s (input: %s) {\n"
            + "    totalCount,\n"
            + "    rowset {\n"
            + "      adId,\n"
            + "      oldHref\n"
            + "      newHref,\n"
            + "      sitelinks {\n"
            + "        sitelinkId \n"
            + "        oldHref \n"
            + "        newHref \n"
            + "        title \n"
            + "        }\n"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final String FIND_AND_REPLACE_BANNERS_HREF_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    totalCount,\n"
            + "    successCount,\n"
            + "    rowset {\n"
            + "      adId,\n"
            + "      oldHref\n"
            + "      newHref,\n"
            + "      sitelinks {\n"
            + "        sitelinkId \n"
            + "        oldHref \n"
            + "        newHref \n"
            + "        title \n"
            + "        }\n"
            + "    }\n"
            + "  }\n"
            + "}";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;

    private GdFindAndReplaceAdsHrefText request;
    private User operator;

    private ClientInfo clientInfo;
    private OldTextBanner banner;
    private SitelinkSet sitelinkSet;
    private Sitelink sitelink11;
    private Sitelink sitelink12;

    @Before
    public void initTestData() {
        clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        sitelink11 = defaultSitelink().withDescription(null)
                .withHref("http://abc.ru/path/to?param1=value#frag");
        sitelink12 = defaultSitelink().withDescription(null)
                .withHref("http://abc.ru/path/to?param2=value#frag");
        sitelinkSet = sitelinkSet(clientInfo.getClientId(), asList(sitelink11, sitelink12));
        sitelinkSet = steps.sitelinkSetSteps().createSitelinkSet(sitelinkSet, clientInfo).getSitelinkSet();
        banner = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null)
                                .withHref("http://abc.ru?param1=value#frag")
                                .withSitelinksSetId(sitelinkSet.getId()),
                        campaignInfo).getBanner();
        operator = UserHelper.getUser(clientInfo.getClient());

        request = new GdFindAndReplaceAdsHrefText()
                .withAdIds(singletonList(banner.getId()))
                .withTargetTypes(Sets.newHashSet(GdFindAndReplaceAdsHrefTargetType.AD_HREF,
                        GdFindAndReplaceAdsHrefTargetType.SITELINK_HREF))
                .withAdIdsHrefExceptions(emptySet())
                .withSitelinkIdsHrefExceptions(emptyMap())
                .withReplaceInstruction(new GdFindAndReplaceHrefTextInstruction()
                        .withHrefParts(Sets.newHashSet(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH,
                                GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT))
                        .withSearch("path")
                        .withReplace("replace"));
    }

    @Test
    public void findAndReplaceBannerHrefTextPreview() {
        String query = String.format(PREVIEW_QUERY_TEMPLATE,
                FIND_AND_REPLACE_BANNERS_HREF_TEXT_PREVIEW_QUERY,
                graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdCachedResult expectedPayload = new GdCachedResult<GdFindAndReplaceAdsHrefPayloadItem>()
                .withTotalCount(request.getAdIds().size())
                .withRowset(singletonList(getExpectedPayloadItem().withNewHref(null)));

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(FIND_AND_REPLACE_BANNERS_HREF_TEXT_PREVIEW_QUERY);

        TypeReference<GdCachedResult<GdFindAndReplaceAdsHrefPayloadItem>> typeReference =
                new TypeReference<GdCachedResult<GdFindAndReplaceAdsHrefPayloadItem>>() {
                };

        GdCachedResult payload = GraphQlJsonUtils
                .convertValue(data.get(FIND_AND_REPLACE_BANNERS_HREF_TEXT_PREVIEW_QUERY), typeReference);

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void replaceBannerHrefTextPreview() {
        request.withReplaceInstruction(request.getReplaceInstruction()
                .withSearch("*")
                .withReplace("http://ya.ru"));
        String query = String.format(PREVIEW_QUERY_TEMPLATE,
                FIND_AND_REPLACE_BANNERS_HREF_TEXT_PREVIEW_QUERY,
                graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdCachedResult expectedPayload = new GdCachedResult<GdFindAndReplaceAdsHrefPayloadItem>()
                .withTotalCount(request.getAdIds().size())
                .withRowset(singletonList(getExpectedPayloadItem()));

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(FIND_AND_REPLACE_BANNERS_HREF_TEXT_PREVIEW_QUERY);

        TypeReference<GdCachedResult<GdFindAndReplaceAdsHrefPayloadItem>> typeReference =
                new TypeReference<GdCachedResult<GdFindAndReplaceAdsHrefPayloadItem>>() {
                };

        GdCachedResult payload = GraphQlJsonUtils
                .convertValue(data.get(FIND_AND_REPLACE_BANNERS_HREF_TEXT_PREVIEW_QUERY), typeReference);

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void findAndReplaceBannerHrefText() {
        TestAuthHelper.setDirectAuthentication(operator);

        request.getReplaceInstruction().withSearch("frag");
        String query = String.format(FIND_AND_REPLACE_BANNERS_HREF_MUTATION_TEMPLATE,
                FIND_AND_REPLACE_BANNERS_HREF_TEXT_MUTATION,
                graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdResult<GdFindAndReplaceAdsHrefPayloadItem> expectedPayload =
                new GdResult<GdFindAndReplaceAdsHrefPayloadItem>()
                        .withTotalCount(request.getAdIds().size())
                        .withSuccessCount(request.getAdIds().size())
                        .withRowset(singletonList(getExpectedPayloadItem()));

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(FIND_AND_REPLACE_BANNERS_HREF_TEXT_MUTATION);

        TypeReference<GdResult<GdFindAndReplaceAdsHrefPayloadItem>> typeReference =
                new TypeReference<GdResult<GdFindAndReplaceAdsHrefPayloadItem>>() {
                };

        GdResult payload =
                GraphQlJsonUtils.convertValue(data.get(FIND_AND_REPLACE_BANNERS_HREF_TEXT_MUTATION), typeReference);

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void replaceAllBannerHrefText() {
        TestAuthHelper.setDirectAuthentication(operator);

        request.getReplaceInstruction()
                .withSearch(null)
                .withReplace("http://ya.ru");

        String query = String.format(FIND_AND_REPLACE_BANNERS_HREF_MUTATION_TEMPLATE,
                FIND_AND_REPLACE_BANNERS_HREF_TEXT_MUTATION,
                graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdResult<GdFindAndReplaceAdsHrefPayloadItem> expectedPayload =
                new GdResult<GdFindAndReplaceAdsHrefPayloadItem>()
                        .withTotalCount(request.getAdIds().size())
                        .withSuccessCount(request.getAdIds().size())
                        .withRowset(singletonList(getExpectedPayloadItem()));

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(FIND_AND_REPLACE_BANNERS_HREF_TEXT_MUTATION);

        TypeReference<GdResult<GdFindAndReplaceAdsHrefPayloadItem>> typeReference =
                new TypeReference<GdResult<GdFindAndReplaceAdsHrefPayloadItem>>() {
                };

        GdResult payload =
                GraphQlJsonUtils.convertValue(data.get(FIND_AND_REPLACE_BANNERS_HREF_TEXT_MUTATION), typeReference);

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    private GdFindAndReplaceAdsHrefPayloadItem getExpectedPayloadItem() {
        return new GdFindAndReplaceAdsHrefPayloadItem()
                .withAdId(banner.getId())
                .withOldHref(banner.getHref())
                .withNewHref(newHref(banner.getHref(), request))
                .withSitelinks(asList(new GdFindAndReplaceAdsHrefPreviewPayloadItemSitelink()
                                .withSitelinkId(sitelink11.getId())
                                .withTitle(sitelink11.getTitle())
                                .withOldHref(sitelink11.getHref())
                                .withNewHref(newHref(sitelink11.getHref(), request)),
                        new GdFindAndReplaceAdsHrefPreviewPayloadItemSitelink()
                                .withSitelinkId(sitelink12.getId())
                                .withTitle(sitelink12.getTitle())
                                .withOldHref(sitelink12.getHref())
                                .withNewHref(newHref(sitelink12.getHref(), request))));
    }

    private static String newHref(String href, GdFindAndReplaceAdsHrefText request) {
        String search = request.getReplaceInstruction().getSearch();
        String replace = request.getReplaceInstruction().getReplace();
        if (search == null || search.equals("*")) {
            return replace;
        } else {
            return href.replace(search, replace);
        }
    }
}
