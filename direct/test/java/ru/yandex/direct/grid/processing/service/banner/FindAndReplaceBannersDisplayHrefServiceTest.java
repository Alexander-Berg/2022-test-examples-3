package ru.yandex.direct.grid.processing.service.banner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import graphql.ExecutionResult;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldDynamicBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.BannerInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.info.DynamicBannerInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.core.entity.banner.model.GdReplaceDisplayHrefBanner;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsDisplayHref;
import ru.yandex.direct.grid.processing.model.common.GdCachedResult;
import ru.yandex.direct.grid.processing.model.common.GdResult;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.validation.defect.CollectionDefects;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.banner.type.displayhref.BannerWithDisplayHrefConstraints.MAX_LENGTH_DISPLAY_HREF;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeDynamicBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.banner.converter.FindAndReplaceBannerDisplayHrefConverter.preparePreviewPayload;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasValidationResult;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;

@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class FindAndReplaceBannersDisplayHrefServiceTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final String GET_DISPLAY_HREFS_ARG_NAME = "adIds";
    private static final String ROOT_NAME = "input";
    private static final String DEFAULT_SEARCH_KEY = "tyt";
    private static final String DEFAULT_SEARCH_KEY2 = "secondkey";
    private static final String NOT_FOUND_SEARCH_TEXT = "notFoundText";
    private static final String PREVIEW_GRAPH_QL_NAME = "getFindAndReplaceDisplayHrefPreview";
    private static final String UPDATE_GRAPH_QL_NAME = "findAndReplaceDisplayHref";
    private static final String PREVIEW_QUERY_TEMPLATE = ""
            + "query {\n"
            + "  %s (input: %s) {\n"
            + "    totalCount,\n"
            + "    rowset{\n"
            + "      bannerId\n"
            + "      newDisplayHref\n"
            + "      sourceDisplayHref\n"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final String UPDATE_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    totalCount,\n"
            + "    successCount,\n"
            + "    rowset{\n"
            + "      bannerId\n"
            + "      newDisplayHref\n"
            + "      sourceDisplayHref\n"
            + "    }\n"
            + "  }\n"
            + "}";

    private GdFindAndReplaceAdsDisplayHref request;
    private ClientInfo clientInfo;
    private User operator;

    private TextBannerInfo textBanner;
    private OldTextBanner banner1;
    private OldTextBanner banner2;

    @Autowired
    private Steps steps;
    @Autowired
    private FindAndReplaceBannersDisplayHrefService serviceUnderTest;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private BannerService bannerService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void before() {
        textBanner = steps.bannerSteps().createActiveTextBanner();

        clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);

        banner1 = steps.bannerSteps()
                .createBanner(
                        activeTextBanner(null, null)
                                .withHref("http://ya.ru/")
                                .withDisplayHref(DEFAULT_SEARCH_KEY),
                        campaignInfo)
                .getBanner();

        banner2 = steps.bannerSteps()
                .createBanner(
                        activeTextBanner(null, null)
                                .withHref("http://ya.ru/" + DEFAULT_SEARCH_KEY2)
                                .withDisplayHref(DEFAULT_SEARCH_KEY2),
                        campaignInfo)
                .getBanner();
        operator = UserHelper.getUser(clientInfo.getClient());

        request = new GdFindAndReplaceAdsDisplayHref()
                .withAdIds(Arrays.asList(banner1.getId(), banner2.getId()))
                .withNewDisplayHref("tam")
                .withSearchDisplayHrefs(Arrays.asList(DEFAULT_SEARCH_KEY, DEFAULT_SEARCH_KEY2));
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }


    @Test
    public void getDisplayHrefs_OneTextBanner() {
        Set<String> displayHrefs = serviceUnderTest
                .getDisplayHrefs(singletonList(textBanner.getBannerId()), textBanner.getClientId(),
                        GET_DISPLAY_HREFS_ARG_NAME);

        List<String> expected = singletonList(textBanner.getBanner().getDisplayHref());
        assertThat(displayHrefs, containsInAnyOrder(expected.toArray()));
    }

    @Test
    public void getDisplayHrefs_TwoBannersWithDisplayHrefs() {
        DynamicBannerInfo dynamicBanner = steps.bannerSteps().createBanner(
                activeDynamicBanner(textBanner.getCampaignId(), textBanner.getAdGroupId())
                        .withDisplayHref("displayHref2"), textBanner.getAdGroupInfo());
        Set<String> displayHrefs = serviceUnderTest
                .getDisplayHrefs(asList(textBanner.getBannerId(), dynamicBanner.getBannerId()),
                        textBanner.getClientId(), GET_DISPLAY_HREFS_ARG_NAME);

        List<String> expected =
                asList(textBanner.getBanner().getDisplayHref(), dynamicBanner.getBanner().getDisplayHref());
        assertThat(displayHrefs, containsInAnyOrder(expected.toArray()));
    }

    @Test
    public void getDisplayHrefs_TwoBannersWithSameDisplayHrefs() {
        DynamicBannerInfo dynamicBanner = steps.bannerSteps().createActiveDynamicBanner(textBanner.getAdGroupInfo());
        Set<String> displayHrefs = serviceUnderTest
                .getDisplayHrefs(asList(textBanner.getBannerId(), dynamicBanner.getBannerId()),
                        textBanner.getClientId(), GET_DISPLAY_HREFS_ARG_NAME);

        List<String> expected = singletonList(textBanner.getBanner().getDisplayHref());
        assertThat(displayHrefs, containsInAnyOrder(expected.toArray()));
    }

    @Test
    public void getDisplayHrefs_OneBannerWithAndOneWithoutDisplayHref() {
        Long creativeId = steps.creativeSteps().addDefaultCanvasCreative(textBanner.getClientInfo()).getCreativeId();
        CpmBannerInfo cpmBanner = steps.bannerSteps().createActiveCpmBanner(
                activeCpmBanner(textBanner.getCampaignId(), textBanner.getAdGroupId(), creativeId),
                textBanner.getAdGroupInfo());

        Set<String> displayHrefs = serviceUnderTest
                .getDisplayHrefs(asList(cpmBanner.getBannerId(), textBanner.getBannerId()),
                        textBanner.getClientId(), GET_DISPLAY_HREFS_ARG_NAME);

        List<String> expected = singletonList(textBanner.getBanner().getDisplayHref());
        assertThat(displayHrefs, containsInAnyOrder(expected.toArray()));
    }

    @Test
    public void getDisplayHrefs_TwoBannersOneFromAnotherClient() {
        BannerInfo banner = steps.bannerSteps().createBanner(activeDynamicBanner(null, null)
                .withDisplayHref("displayHref2"));
        TextBannerInfo anotherClientBanner = steps.bannerSteps().createActiveTextBanner();

        Set<String> displayHrefs = serviceUnderTest
                .getDisplayHrefs(asList(anotherClientBanner.getBannerId(), banner.getBannerId()), banner.getClientId(),
                        GET_DISPLAY_HREFS_ARG_NAME);

        List<String> expected = singletonList(((OldDynamicBanner) banner.getBanner()).getDisplayHref());
        assertThat(displayHrefs, containsInAnyOrder(expected.toArray()));
    }

    Object[] params() {
        return new Object[]{
                new Object[]{"searchHrefs is " + NOT_FOUND_SEARCH_TEXT, List.of(NOT_FOUND_SEARCH_TEXT)},
                new Object[]{"searchHrefs is " + DEFAULT_SEARCH_KEY, List.of(DEFAULT_SEARCH_KEY)},
                new Object[]{"searchHrefs is null", null},
                new Object[]{"searchHrefs is all keys", List.of(DEFAULT_SEARCH_KEY, DEFAULT_SEARCH_KEY2)},
        };
    }

    @Test
    @Parameters(method = "params")
    @TestCaseName("{0}")
    public void findAndReplaceBannerDisplayHrefPreview_Replaced(String testName, List<String> searchDisplayHrefs) {
        request.setSearchDisplayHrefs(searchDisplayHrefs);

        String query = String.format(PREVIEW_QUERY_TEMPLATE, PREVIEW_GRAPH_QL_NAME, graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        Assertions.assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        Assertions.assertThat(data)
                .containsOnlyKeys(PREVIEW_GRAPH_QL_NAME);

        TypeReference<GdCachedResult<GdReplaceDisplayHrefBanner>> typeReference = new TypeReference<>() {
        };

        GdCachedResult payload = GraphQlJsonUtils
                .convertValue(data.get(PREVIEW_GRAPH_QL_NAME), typeReference);

        List<GdReplaceDisplayHrefBanner> expectedRowset = getExpectedRowset();
        GdCachedResult expectedPayload = new GdCachedResult<GdReplaceDisplayHrefBanner>()
                .withTotalCount(expectedRowset.size())
                .withRowset(expectedRowset);

        Assertions.assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    @Parameters(method = "params")
    @TestCaseName("{0}")
    public void findAndReplaceBannerDisplayHrefPreview_Deleted(String testName, List<String> searchDisplayHrefs) {
        request
                .withSearchDisplayHrefs(searchDisplayHrefs)
                .withNewDisplayHref(null);

        String query = String.format(PREVIEW_QUERY_TEMPLATE, PREVIEW_GRAPH_QL_NAME, graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        Assertions.assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        Assertions.assertThat(data)
                .containsOnlyKeys(PREVIEW_GRAPH_QL_NAME);

        TypeReference<GdCachedResult<GdReplaceDisplayHrefBanner>> typeReference = new TypeReference<>() {
        };

        GdCachedResult payload = GraphQlJsonUtils
                .convertValue(data.get(PREVIEW_GRAPH_QL_NAME), typeReference);

        List<GdReplaceDisplayHrefBanner> expectedRowset = getExpectedRowset();
        GdCachedResult expectedPayload = new GdCachedResult<GdReplaceDisplayHrefBanner>()
                .withTotalCount(expectedRowset.size())
                .withRowset(expectedRowset);

        Assertions.assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    @Parameters(method = "params")
    @TestCaseName("{0}")
    public void findAndReplaceBannerDisplayHref_Replaced(String testName,
                                                         List<String> searchDisplayHrefs) {
        TestAuthHelper.setDirectAuthentication(operator);

        request.setSearchDisplayHrefs(searchDisplayHrefs);

        String query = String.format(UPDATE_MUTATION_TEMPLATE, UPDATE_GRAPH_QL_NAME, graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        Assertions.assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        Assertions.assertThat(data)
                .containsOnlyKeys(UPDATE_GRAPH_QL_NAME);

        TypeReference<GdResult<GdReplaceDisplayHrefBanner>> typeReference = new TypeReference<>() {
        };

        GdResult payload = GraphQlJsonUtils
                .convertValue(data.get(UPDATE_GRAPH_QL_NAME), typeReference);

        List<GdReplaceDisplayHrefBanner> expectedRowset = getExpectedRowset();
        GdResult expectedPayload = new GdResult<GdReplaceDisplayHrefBanner>()
                .withTotalCount(expectedRowset.size())
                .withSuccessCount(expectedRowset.size())
                .withRowset(expectedRowset);

        Assertions.assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));

        List<BannerWithSystemFields> banners =
                bannerService.getBannersByIds(asList(banner1.getId(), banner2.getId()));

        Map<Long, Banner> actualBannerById = listToMap(banners, Banner::getId);
        String actualDisplayHref1 = ((TextBanner) actualBannerById.get(banner1.getId())).getDisplayHref();
        String actualDisplayHref2 = ((TextBanner) actualBannerById.get(banner2.getId())).getDisplayHref();

        assertEquals(actualDisplayHref1, getExpectedDisplayHrefForBanner(banner1));
        assertEquals(actualDisplayHref2, getExpectedDisplayHrefForBanner(banner2));
    }

    @Test
    @Parameters(method = "params")
    @TestCaseName("{0}")
    public void findAndReplaceBannerDisplayHref_Deleted(String testName,
                                                        List<String> searchDisplayHrefs) {
        TestAuthHelper.setDirectAuthentication(operator);

        request
                .withSearchDisplayHrefs(searchDisplayHrefs)
                .withNewDisplayHref(null);

        String query = String.format(UPDATE_MUTATION_TEMPLATE, UPDATE_GRAPH_QL_NAME, graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        Assertions.assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        Assertions.assertThat(data)
                .containsOnlyKeys(UPDATE_GRAPH_QL_NAME);

        TypeReference<GdResult<GdReplaceDisplayHrefBanner>> typeReference =
                new TypeReference<GdResult<GdReplaceDisplayHrefBanner>>() {
                };

        GdResult payload = GraphQlJsonUtils
                .convertValue(data.get(UPDATE_GRAPH_QL_NAME), typeReference);

        List<GdReplaceDisplayHrefBanner> expectedRowset = getExpectedRowset();
        GdResult expectedPayload = new GdResult<GdReplaceDisplayHrefBanner>()
                .withTotalCount(expectedRowset.size())
                .withSuccessCount(expectedRowset.size())
                .withRowset(expectedRowset);

        Assertions.assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));

        List<BannerWithSystemFields> banners =
                bannerService.getBannersByIds(asList(banner1.getId(), banner2.getId()));

        Map<Long, TextBanner> actualBannerById = listToMap(banners, Banner::getId, TextBanner.class::cast);
        String actualDisplayHref1 = actualBannerById.get(banner1.getId()).getDisplayHref();
        String actualDisplayHref2 = actualBannerById.get(banner2.getId()).getDisplayHref();

        assertEquals(actualDisplayHref1, getExpectedDisplayHrefForBanner(banner1));
        assertEquals(actualDisplayHref2, getExpectedDisplayHrefForBanner(banner2));
    }

    @Test
    public void checkFindAndReplaceBannerDisplayHrefPreview_ReturnEmptyPayload() {
        request.withSearchDisplayHrefs(singletonList(NOT_FOUND_SEARCH_TEXT));
        GdCachedResult payload = serviceUnderTest
                .preview(request, operator.getUid(), clientInfo.getClientId(), ROOT_NAME);

        String ignoredCacheKey = payload.getCacheKey();

        GdCachedResult<GdReplaceDisplayHrefBanner> expectedPayload = preparePreviewPayload(0, null);
        expectedPayload.withRowset(Collections.emptyList());
        expectedPayload.setCacheKey(ignoredCacheKey);

        Assertions.assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void checkFindAndReplaceBannerDisplayHrefPreview_RequestDisplayHrefValidation() {
        request.withAdIds(singletonList(banner1.getId()))
                .withSearchDisplayHrefs(singletonList(DEFAULT_SEARCH_KEY))
                .withNewDisplayHref(
                        RandomStringUtils.randomAlphanumeric(MAX_LENGTH_DISPLAY_HREF + 1));

        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(
                hasErrorsWith(gridDefect(ROOT_NAME + "." + GdFindAndReplaceAdsDisplayHref.NEW_DISPLAY_HREF.name(),
                        BannerDefects.maxLengthDisplayHref(MAX_LENGTH_DISPLAY_HREF)))));

        serviceUnderTest.preview(request, operator.getUid(), clientInfo.getClientId(), ROOT_NAME);
    }

    @Test
    public void checkFindAndReplaceBannerDisplayHrefPreview_RequestAdIdsValidation() {
        request.withAdIds(emptyList());
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(
                hasErrorsWith(gridDefect(ROOT_NAME + "." + GdFindAndReplaceAdsDisplayHref.AD_IDS.name(),
                        CollectionDefects.notEmptyCollection()))));

        serviceUnderTest
                .preview(request, operator.getUid(), clientInfo.getClientId(), ROOT_NAME);
    }

    private List<GdReplaceDisplayHrefBanner> getExpectedRowset() {
        GdReplaceDisplayHrefBanner expectedGdBanner1 = new GdReplaceDisplayHrefBanner()
                .withBannerId(banner1.getId())
                .withSourceDisplayHref(banner1.getDisplayHref())
                .withNewDisplayHref(request.getNewDisplayHref());

        GdReplaceDisplayHrefBanner expectedGdBanner2 = new GdReplaceDisplayHrefBanner()
                .withBannerId(banner2.getId())
                .withSourceDisplayHref(banner2.getDisplayHref())
                .withNewDisplayHref(request.getNewDisplayHref());

        List<GdReplaceDisplayHrefBanner> banners = asList(expectedGdBanner1, expectedGdBanner2);

        Set<String> searchDisplayHrefs = listToSet(request.getSearchDisplayHrefs(), identity());

        return banners.stream()
                .filter(banner -> searchDisplayHrefs == null ||
                        searchDisplayHrefs.contains(banner.getSourceDisplayHref()))
                .collect(Collectors.toList());
    }

    private String getExpectedDisplayHrefForBanner(OldTextBanner banner) {
        Set<String> searchDisplayHrefs = listToSet(request.getSearchDisplayHrefs(), identity());
        String bannerDisplayHref = banner.getDisplayHref();

        if (searchDisplayHrefs == null || searchDisplayHrefs.contains(bannerDisplayHref)) {
            return request.getNewDisplayHref();
        } else {
            return bannerDisplayHref;
        }
    }
}
