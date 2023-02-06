package ru.yandex.direct.grid.processing.service.banner;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import graphql.ExecutionResult;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.entity.sitelink.service.SitelinkSetService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsHrefParams;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsHrefParamsInstruction;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsHrefPayloadItem;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsHrefPreviewPayloadItemSitelink;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsHrefTargetType;
import ru.yandex.direct.grid.processing.model.common.GdCachedResult;
import ru.yandex.direct.grid.processing.model.common.GdResult;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.result.Path;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.thymeleaf.util.SetUtils.singletonSet;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestSitelinkSets.sitelinkSet;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelink;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.banner.converter.FindAndReplaceBannerHrefConverter.getEmptyPayload;
import static ru.yandex.direct.grid.processing.service.banner.converter.FindAndReplaceBannerHrefConverter.getEmptyPreviewPayload;
import static ru.yandex.direct.grid.processing.service.validation.GridDefectDefinitions.invalidFindAndReplaceBannersHrefParamsText;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasValidationResult;
import static ru.yandex.direct.multitype.entity.LimitOffset.maxLimited;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class FindAndReplaceBannerHrefParamServiceTest {

    private static final String ROOT_NAME = "input";
    private static final String DEFAULT_SEARCH_KEY = "tyt";
    private static final String DEFAULT_SEARCH_KEY2 = "secondkey";
    private static final String NOT_FOUND_SEARCH_TEXT = "notFoundText";
    private static final String INVALID_CHANGE_HREF = "/?/";
    private static final String FIND_AND_REPLACE_BANNERS_HREF_PARAMS_PREVIEW_QUERY =
            "getFindAndReplaceAdsHrefParamsPreview";
    private static final String FIND_AND_REPLACE_BANNERS_HREF_PARAMS_MUTATION = "findAndReplaceAdsHrefParams";
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

    private GdFindAndReplaceAdsHrefParams request;
    private User operator;

    private ClientInfo clientInfo;

    private OldTextBanner banner;
    private OldTextBanner bannerWithSitelinks;
    private SitelinkSet sitelinkSet;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private BannerService bannerService;

    @Autowired
    private SitelinkSetService sitelinkSetService;

    @Autowired
    private FindAndReplaceBannerHrefParamService findAndReplaceBannerHrefParamService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void initTestData() {
        clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        Sitelink sitelink11 =
                defaultSitelink().withDescription(null)
                        .withHref("http://video.yandex.ru/?" + DEFAULT_SEARCH_KEY + "={campaign_id}");
        Sitelink sitelink12 =
                defaultSitelink().withDescription(null)
                        .withHref("http://music.yandex.ru/?" + DEFAULT_SEARCH_KEY + "={campaign_id}");
        sitelinkSet = sitelinkSet(clientInfo.getClientId(), asList(sitelink11, sitelink12));
        sitelinkSet = steps.sitelinkSetSteps().createSitelinkSet(sitelinkSet, clientInfo).getSitelinkSet();
        banner = steps.bannerSteps().createBanner(activeTextBanner(null, null)
                        .withHref("http://ya.ru/?" + DEFAULT_SEARCH_KEY + "={campaign_id}&" + DEFAULT_SEARCH_KEY2 +
                                "={par2}"),
                campaignInfo).getBanner();
        bannerWithSitelinks = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null)
                                .withHref("http://ya.ru/?" + DEFAULT_SEARCH_KEY + "={campaign_id}&" + DEFAULT_SEARCH_KEY2
                                        + "={par2}" + "&" + DEFAULT_SEARCH_KEY2)
                                .withSitelinksSetId(sitelinkSet.getId()),
                        campaignInfo).getBanner();
        operator = UserHelper.getUser(clientInfo.getClient());

        request = new GdFindAndReplaceAdsHrefParams()
                .withAdIds(Arrays.asList(banner.getId(), bannerWithSitelinks.getId()))
                .withTargetTypes(Sets.newHashSet(GdFindAndReplaceAdsHrefTargetType.AD_HREF,
                        GdFindAndReplaceAdsHrefTargetType.SITELINK_HREF))
                .withAdIdsHrefExceptions(emptySet())
                .withSitelinkIdsHrefExceptions(emptyMap())
                .withReplaceInstruction(new GdFindAndReplaceAdsHrefParamsInstruction()
                        .withSearchKey(DEFAULT_SEARCH_KEY)
                        .withReplaceKey(null)
                        .withReplaceValue("1"));
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }


    @Test
    public void findAndReplaceBannerHrefParamsPreview() {
        String query = String.format(PREVIEW_QUERY_TEMPLATE, FIND_AND_REPLACE_BANNERS_HREF_PARAMS_PREVIEW_QUERY,
                graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();

        GdCachedResult expectedPayload = new GdCachedResult<GdFindAndReplaceAdsHrefPayloadItem>()
                .withTotalCount(request.getAdIds().size())
                .withRowset(getExpectedPreviewRowset());
        Map<String, Object> data = result.getData();
        assertThat(data)
                .containsOnlyKeys(FIND_AND_REPLACE_BANNERS_HREF_PARAMS_PREVIEW_QUERY);

        TypeReference<GdCachedResult<GdFindAndReplaceAdsHrefPayloadItem>> typeReference =
                new TypeReference<GdCachedResult<GdFindAndReplaceAdsHrefPayloadItem>>() {
                };

        GdCachedResult payload = GraphQlJsonUtils
                .convertValue(data.get(FIND_AND_REPLACE_BANNERS_HREF_PARAMS_PREVIEW_QUERY), typeReference);

        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void findAndReplaceBannerHrefParams() {
        TestAuthHelper.setDirectAuthentication(operator);

        String query = String.format(FIND_AND_REPLACE_BANNERS_HREF_MUTATION_TEMPLATE,
                FIND_AND_REPLACE_BANNERS_HREF_PARAMS_MUTATION, graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();

        GdResult<GdFindAndReplaceAdsHrefPayloadItem> expectedPayload =
                new GdResult<GdFindAndReplaceAdsHrefPayloadItem>()
                        .withTotalCount(request.getAdIds().size())
                        .withSuccessCount(request.getAdIds().size())
                        .withRowset(getExpectedPreviewRowset());
        List<Banner> expectedBanners = mapList(expectedPayload.getRowset(), this::toBanner);
        List<SitelinkSet> expectedSitelinkSets = toSitelinkSets(expectedPayload.getRowset());
        Map<String, Object> data = result.getData();
        assertThat(data)
                .containsOnlyKeys(FIND_AND_REPLACE_BANNERS_HREF_PARAMS_MUTATION);

        TypeReference<GdResult<GdFindAndReplaceAdsHrefPayloadItem>> typeReference =
                new TypeReference<GdResult<GdFindAndReplaceAdsHrefPayloadItem>>() {
                };

        GdResult payload =
                GraphQlJsonUtils.convertValue(data.get(FIND_AND_REPLACE_BANNERS_HREF_PARAMS_MUTATION), typeReference);

        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));

        List<BannerWithSystemFields> banners =
                bannerService.getBannersByIds(asList(banner.getId(), bannerWithSitelinks.getId()));
        banners.sort(Comparator.comparing(Banner::getId));
        assertThat(banners)
                .is(matchedBy(beanDiffer(expectedBanners)
                        .useCompareStrategy(onlyExpectedFields()))
                );
        List<SitelinkSet> sitelinkSets = sitelinkSetService
                .getSitelinkSets(getSitelinkSetIds(banners), clientInfo.getClientId(), maxLimited());
        sitelinkSets.sort(Comparator.comparing(SitelinkSet::getId));
        assertThat(sitelinkSets)
                .is(matchedBy(beanDiffer(expectedSitelinkSets)
                        .useCompareStrategy(onlyExpectedFields()))
                );
    }

    @Test
    public void checkFindAndReplaceBannerHrefsPreview_RequestValidation() {
        request.withAdIds(emptyList());
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(
                hasErrorsWith(gridDefect(ROOT_NAME + "." + GdFindAndReplaceAdsHrefParams.AD_IDS.name(),
                        CollectionDefects.notEmptyCollection()))));

        findAndReplaceBannerHrefParamService
                .preview(request, operator.getUid(), clientInfo.getClientId(),
                        ROOT_NAME);
    }

    @Test
    public void checkFindAndReplaceBannerHrefsPreview_ReturnEmptyPayload() {
        request.getReplaceInstruction().withSearchKey(NOT_FOUND_SEARCH_TEXT);
        GdCachedResult payload = findAndReplaceBannerHrefParamService
                .preview(request, operator.getUid(), clientInfo.getClientId(),
                        ROOT_NAME);

        String ignoredCacheKey = payload.getCacheKey();

        GdCachedResult expectedPayload = getEmptyPreviewPayload();
        expectedPayload.setCacheKey(ignoredCacheKey);

        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void checkFindAndReplaceBannerHrefsPreview_ReplaceSitelink_BannerNewHrefIsNull() {
        request.withTargetTypes(singletonSet(GdFindAndReplaceAdsHrefTargetType.SITELINK_HREF));
        GdCachedResult<GdFindAndReplaceAdsHrefPayloadItem> payload = findAndReplaceBannerHrefParamService
                .preview(request, operator.getUid(), clientInfo.getClientId(),
                        ROOT_NAME);
        assertThat(payload.getRowset().get(0).getNewHref())
                .is(matchedBy(nullValue()));
    }

    @Test
    public void checkFindAndReplaceBannerHrefsPreview_CoreValidation_SortPreview() {
        request.withAdIds(asList(banner.getId(), bannerWithSitelinks.getId()));
        request.getReplaceInstruction()
                .withSearchKey(DEFAULT_SEARCH_KEY2)
                .withReplaceValue(RandomStringUtils.randomAlphanumeric(5));
        GdCachedResult payload = findAndReplaceBannerHrefParamService
                .preview(request, operator.getUid(), clientInfo.getClientId(),
                        ROOT_NAME);

        String ignoredCacheKey = payload.getCacheKey();

        Path expectedPath = path(index(0), field(OldBanner.HREF.name()));
        //bannerWithSitelinks - невалидный вначале
        GdCachedResult expectedPayload = new GdCachedResult<GdFindAndReplaceAdsHrefPayloadItem>()
                .withCacheKey(ignoredCacheKey)
                .withTotalCount(request.getAdIds().size())
                .withSuccessCount(2)
                .withRowset(asList(toPreviewItem(request.getReplaceInstruction(), banner, null),
                        toPreviewItem(request.getReplaceInstruction(), bannerWithSitelinks, null)))
                .withValidationResult(null);

        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void checkFindAndReplaceBannerHrefs_RequestValidation() {
        request.getReplaceInstruction().withSearchKey(INVALID_CHANGE_HREF);
        thrown.expect(GridValidationException.class);

        Path expectedPath = path(field(ROOT_NAME), field(GdFindAndReplaceAdsHrefParams.REPLACE_INSTRUCTION),
                field(GdFindAndReplaceAdsHrefParamsInstruction.SEARCH_KEY.name()));
        thrown.expect(hasValidationResult(
                hasErrorsWith(gridDefect(expectedPath.toString(), invalidFindAndReplaceBannersHrefParamsText()))));

        findAndReplaceBannerHrefParamService
                .replace(request, operator.getUid(), clientInfo.getClientId(), ROOT_NAME);
    }

    @Test
    public void checkFindAndReplaceBannerHrefs_ReturnEmptyPayload() {
        request.getReplaceInstruction().withSearchKey(NOT_FOUND_SEARCH_TEXT);
        GdResult<GdFindAndReplaceAdsHrefPayloadItem> payload = findAndReplaceBannerHrefParamService
                .replace(request, operator.getUid(), clientInfo.getClientId(), ROOT_NAME);

        GdResult<GdFindAndReplaceAdsHrefPayloadItem> expectedPayload = getEmptyPayload();
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    //helper methods
    private List<GdFindAndReplaceAdsHrefPayloadItem> getExpectedPreviewRowset() {
        GdFindAndReplaceAdsHrefPayloadItem expectedPreviewForBanner =
                toPreviewItem(request.getReplaceInstruction(), banner, null);

        GdFindAndReplaceAdsHrefPayloadItem expectedPreviewForBanner2 =
                toPreviewItem(request.getReplaceInstruction(), bannerWithSitelinks, sitelinkSet);

        return Arrays.asList(expectedPreviewForBanner, expectedPreviewForBanner2);
    }

    private static GdFindAndReplaceAdsHrefPayloadItem toPreviewItem(
            GdFindAndReplaceAdsHrefParamsInstruction instruction, OldTextBanner banner,
            @Nullable SitelinkSet sitelinkSet) {
        return new GdFindAndReplaceAdsHrefPayloadItem()
                .withAdId(banner.getId())
                .withOldHref(banner.getHref())
                .withNewHref(banner.getHref().replaceFirst(instruction.getSearchKey(),
                        defaultIfNull(instruction.getReplaceValue(), "")))
                .withSitelinks(toSitelinksPreview(instruction, sitelinkSet));
    }

    private static List<GdFindAndReplaceAdsHrefPreviewPayloadItemSitelink> toSitelinksPreview(
            GdFindAndReplaceAdsHrefParamsInstruction instruction, @Nullable SitelinkSet sitelinkSet) {
        if (sitelinkSet == null) {
            return emptyList();
        }
        return mapList(sitelinkSet.getSitelinks(),
                sl -> new GdFindAndReplaceAdsHrefPreviewPayloadItemSitelink()
                        .withSitelinkId(sl.getId())
                        .withOldHref(sl.getHref())
                        .withNewHref(sl.getHref().replace(instruction.getSearchKey(),
                                defaultIfNull(instruction.getReplaceKey(), instruction.getReplaceValue())))
                        .withTitle(sl.getTitle()));
    }

    private Banner toBanner(GdFindAndReplaceAdsHrefPayloadItem previewItem) {
        return new TextBanner()
                .withId(previewItem.getAdId())
                .withHref(previewItem.getNewHref());
    }

    private List<SitelinkSet> toSitelinkSets(List<GdFindAndReplaceAdsHrefPayloadItem> previewItems) {
        return previewItems
                .stream()
                .filter(pr -> !pr.getSitelinks().isEmpty())
                .map(pr -> new SitelinkSet()
                        .withSitelinks(mapList(pr.getSitelinks(),
                                sl -> new Sitelink().withTitle(sl.getTitle()).withHref(sl.getNewHref()))))
                .collect(Collectors.toList());
    }

    private List<Long> getSitelinkSetIds(List<BannerWithSystemFields> banners) {
        return StreamEx.of(banners)
                .select(TextBanner.class)
                .map(TextBanner::getSitelinksSetId)
                .nonNull()
                .toList();
    }
}
