package ru.yandex.direct.grid.processing.service.group;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.util.RawValue;
import com.google.common.collect.Iterables;
import graphql.ExecutionResult;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusShowsForecast;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.feature.model.ClientFeature;
import ru.yandex.direct.core.entity.feature.model.Feature;
import ru.yandex.direct.core.entity.feature.model.FeatureState;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.model.StatusModerate;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.repository.TestMinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayloadItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateTextAdGroup;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.Region;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.minusWordsPackNotFound;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.grid.processing.service.group.AdGroupMutationService.PATH_FOR_UPDATE_TEXT_AD_GROUP;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.map;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.regions.Region.TURKEY_REGION_ID;
import static ru.yandex.direct.regions.Region.UKRAINE_REGION_ID;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UpdateAdGroupMutationTest extends UpdateAdGroupMutationBaseTest {

    private static final String MINUS_KEYWORD = "minuskeyword";
    private static final Long INCORRECT_REGION_ID = 777898L;

    @Autowired
    private BannerSteps bannerSteps;

    @Autowired
    private TestMinusKeywordsPackRepository minusKeywordsPackRepository;

    @Before
    public void init() {
        super.initTestData();

        relevanceMatch = steps.relevanceMatchSteps().addDefaultRelevanceMatch(textAdGroupInfo);
        retargetingInfo = steps.retargetingSteps().createDefaultRetargeting(textAdGroupInfo);
    }

    @Test
    public void checkUpdateAdGroup_rename() {
        String newGroupName = "New Group Name";

        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .setName(newGroupName)
                .build();

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)));

        processQueryAndCheck(request, expectedPayload);

        List<AdGroup> adGroups =
                adGroupService.getAdGroups(operator.getClientId(), singletonList(adGroupId));

        softAssertions.assertThat(adGroups)
                .hasSize(1);
        softAssertions.assertThat(adGroups)
                .element(0)
                .isEqualToIgnoringGivenFields(textAdGroupInfo.getAdGroup().withName(newGroupName), "lastChange");

        softAssertions.assertAll();
    }

    @Test
    public void checkUpdateAdGroup_changeGeo() {
        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .setGeo(singletonList(TURKEY_REGION_ID))
                .build();

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)));

        processQueryAndCheck(request, expectedPayload);

        List<AdGroup> adGroups =
                adGroupService.getAdGroups(operator.getClientId(), singletonList(adGroupId));

        AdGroup expectedGroup = textAdGroupInfo.getAdGroup()
                .withGeo(singletonList(TURKEY_REGION_ID))
                .withStatusBsSynced(StatusBsSynced.NO)
                .withStatusShowsForecast(StatusShowsForecast.NEW);

        softAssertions.assertThat(adGroups)
                .hasSize(1);
        softAssertions.assertThat(adGroups)
                .element(0)
                .isEqualToIgnoringGivenFields(expectedGroup, "lastChange");

        softAssertions.assertAll();
    }

    @Test
    public void checkUpdateAdGroup_changePageGroupAndTargetTags() {
        List<String> newPageGroupTags = singletonList("page_group_tag1");
        List<String> newTargetTags = singletonList("target_tag1");

        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .setPageGroupTags(newPageGroupTags)
                .setTargetTags(newTargetTags)
                .build();

        // need a target tag allow feature
        steps.featureSteps().addFeature(FeatureName.TARGET_TAGS_ALLOWED);
        Long featureId = steps.featureSteps().getFeatures().stream()
                .filter(f -> f.getFeatureTextId().equals(FeatureName.TARGET_TAGS_ALLOWED.getName()))
                .map(Feature::getId)
                .findFirst()
                .get();

        ClientFeature featureIdToClientId =
                new ClientFeature()
                        .withClientId(clientInfo.getClientId())
                        .withId(featureId)
                        .withState(FeatureState.ENABLED);
        steps.featureSteps().addClientFeature(featureIdToClientId);

        ExecutionResult result = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        softAssertions.assertThat(result.getErrors())
                .isEmpty();

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)));

        Map<String, Object> data = result.getData();
        softAssertions.assertThat(data)
                .containsOnlyKeys(MUTATION_NAME);

        GdUpdateAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdUpdateAdGroupPayload.class);
        softAssertions.assertThat(payload)
                .isEqualToComparingFieldByFieldRecursively(expectedPayload);

        List<AdGroup> adGroups =
                adGroupService.getAdGroups(operator.getClientId(), singletonList(adGroupId));

        AdGroup expectedGroup = textAdGroupInfo.getAdGroup()
                .withStatusBsSynced(StatusBsSynced.NO)
                .withPageGroupTags(newPageGroupTags)
                .withTargetTags(newTargetTags);

        softAssertions.assertThat(adGroups)
                .hasSize(1);
        softAssertions.assertThat(adGroups)
                .element(0)
                .isEqualToIgnoringGivenFields(expectedGroup, "lastChange");

        softAssertions.assertAll();
    }

    @Test
    public void checkUpdateAdGroup_nullPageGroupOrTargetTags_notUpdate() {
        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .build();

        ExecutionResult result = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        softAssertions.assertThat(result.getErrors())
                .isEmpty();

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)));

        Map<String, Object> data = result.getData();
        softAssertions.assertThat(data)
                .containsOnlyKeys(MUTATION_NAME);

        GdUpdateAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdUpdateAdGroupPayload.class);
        softAssertions.assertThat(payload)
                .isEqualToComparingFieldByFieldRecursively(expectedPayload);

        List<AdGroup> adGroups =
                adGroupService.getAdGroups(operator.getClientId(), singletonList(adGroupId));

        AdGroup expectedGroup = textAdGroupInfo.getAdGroup();

        softAssertions.assertThat(adGroups)
                .hasSize(1);
        softAssertions.assertThat(adGroups)
                .element(0)
                .isEqualToIgnoringGivenFields(expectedGroup, "lastChange");

        softAssertions.assertAll();
    }

    @Test
    public void checkUpdateAdGroup_changeGeo_invalidRegionId() {
        GeoTree geoTree = clientGeoService.getClientTranslocalGeoTree(operator.getClientId());
        softAssertions.assertThat(geoTree.getRegions())
                .doesNotContainKey(INCORRECT_REGION_ID);

        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .setGeo(singletonList(INCORRECT_REGION_ID))
                .build();

        ExecutionResult result = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        softAssertions.assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        softAssertions.assertThat(data)
                .containsOnlyKeys(MUTATION_NAME);

        GdUpdateAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdUpdateAdGroupPayload.class);
        softAssertions.assertThat(payload.getUpdatedAdGroupItems())
                .isEmpty();

        softAssertions.assertAll();
    }

    @Test
    public void checkUpdateAdGroup_changeGeo_inconsistentRegionId() {
        steps.adGroupSteps().setAdGroupProperty(textAdGroupInfo, AdGroup.GEO, singletonList(Region.TURKEY_REGION_ID));
        bannerSteps.setLanguage(textBannerInfo, Language.TR);

        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .setGeo(singletonList(Region.MOSCOW_REGION_ID))
                .build();

        ExecutionResult result = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();

        GdUpdateAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdUpdateAdGroupPayload.class);
        softAssertions.assertThat(payload.getValidationResult().getErrors()).hasSize(1);

        GdDefect defect = Iterables.getFirst(payload.getValidationResult().getErrors(), null);
        GdDefect expectedDefect = new GdDefect()
                .withCode("AdGroupDefectIds.Geo.BAD_GEO")
                .withParams(map("language", new RawValue("TURKISH"),
                        "bannerId", textBannerInfo.getBannerId()))
                .withPath("updateAdGroupItems[0].regionIds");
        softAssertions.assertThat(defect).as("error").isEqualTo(expectedDefect);

        //noinspection ConstantConditions
        List<Long> actualGeo = adGroupService.getAdGroup(adGroupId).getGeo();
        softAssertions.assertThat(actualGeo).as("actualGeo").containsExactly(Region.TURKEY_REGION_ID);

        softAssertions.assertAll();
    }

    @Test
    public void checkUpdateAdGroup_changeMinusKeywords() {
        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .setMinusKeywords(singletonList(MINUS_KEYWORD))
                .build();

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)));

        processQueryAndCheck(request, expectedPayload);

        List<AdGroup> adGroups =
                adGroupService.getAdGroups(operator.getClientId(), singletonList(adGroupId));

        AdGroup expectedGroup = textAdGroupInfo.getAdGroup()
                .withStatusBsSynced(StatusBsSynced.NO)
                .withStatusShowsForecast(StatusShowsForecast.NEW)
                .withMinusKeywords(singletonList(MINUS_KEYWORD));

        softAssertions.assertThat(adGroups)
                .hasSize(1);
        softAssertions.assertThat(adGroups)
                .element(0)
                .is(new Condition<>(a -> a.getMinusKeywordsId() > 0, "minusKeywordsId is validId"))
                .isEqualToIgnoringGivenFields(expectedGroup, "lastChange", "minusKeywordsId");

        softAssertions.assertAll();
    }

    @Test
    public void checkUpdateAdGroup_unchanged_keywords() {
        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .build();

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)));

        processQueryAndCheck(request, expectedPayload);

        checkAdGroupsDbState(textAdGroupInfo.getAdGroup());

        checkKeywordDbState(mapList(Arrays.asList(keywordInfo), KeywordInfo::getKeyword));

        softAssertions.assertAll();
    }

    @Test
    public void checkUpdateAdGroup_unchanged_keywordsWithAutotargeting() {
        // Проверяем, что проверка на дубликаты учитывает наличие ---autotargeting
        String keywordPhrase = "keyword";
        Keyword keyword = defaultKeyword()
                .withPhrase(keywordPhrase)
                .withIsAutotargeting(false);
        Keyword keywordWithAutotargeting = defaultKeyword()
                .withPhrase(keywordPhrase)
                .withIsAutotargeting(true);
        KeywordInfo keywordInfo1 = steps.keywordSteps().createKeyword(textAdGroupInfo, keyword);
        KeywordInfo keywordInfo2 = steps.keywordSteps().createKeyword(textAdGroupInfo, keywordWithAutotargeting);
        //ключевые фразы промодерированы
        steps.keywordSteps().updateKeywordsProperty(Arrays.asList(keywordInfo1, keywordInfo2),
                Keyword.STATUS_MODERATE, StatusModerate.YES);

        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .setKeywords(createKeywordInputItem(keywordInfo[0]),
                        createKeywordInputItem(keywordInfo[1]),
                        createKeywordInputItem(textAdGroupInfo.getAdGroupId(), keywordPhrase),
                        createKeywordInputItem(textAdGroupInfo.getAdGroupId(), "---autotargeting " + keywordPhrase)
                )
                .build();

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(singletonList(new GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)));

        processQueryAndCheck(request, expectedPayload);

        checkAdGroupsDbState(textAdGroupInfo.getAdGroup());

        softAssertions.assertAll();
    }

    private void processQueryAndCheck(GdUpdateTextAdGroup request, GdUpdateAdGroupPayload expectedPayload) {
        ExecutionResult result = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        softAssertions.assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        softAssertions.assertThat(data)
                .containsOnlyKeys(MUTATION_NAME);

        GdUpdateAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdUpdateAdGroupPayload.class);
        softAssertions.assertThat(payload)
                .isEqualToComparingFieldByFieldRecursively(expectedPayload);
    }

    @Test
    public void checkUpdateAdGroup_unchanged_retargetings() {
        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .build();

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)));

        processQueryAndCheck(request, expectedPayload);

        checkAdGroupRetargetingDbState(retargetingInfo.getRetargeting());

        softAssertions.assertAll();
    }

    @Test
    public void checkUpdateAdGroup_unchanged_relevanceMatch() {
        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .build();

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)));

        processQueryAndCheck(request, expectedPayload);

        checkRelevanceMatchDbState(relevanceMatch);

        softAssertions.assertAll();
    }

    @Test
    public void checkUpdateAdGroup_unchanged_banners() {
        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .build();

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)));

        processQueryAndCheck(request, expectedPayload);

        TextBanner expectedBanner = textBannerInfo.getBanner();
        if (expectedBanner.getCalloutIds() == null) {
            expectedBanner.setCalloutIds(emptyList());
        }
        checkBannerDbState(expectedBanner);

        softAssertions.assertAll();
    }

    @Test
    public void checkUpdateAdGroup_unsupportedAdGroupType() {
        AdGroupInfo dynamicAdGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup(clientInfo);

        dynamicAdGroupInfo.getAdGroup()
                .withName("MyDynamicAdGroup")
                .withGeo(singletonList(UKRAINE_REGION_ID));

        ExecutionResult result =
                processor.processQuery(null, getQuery(createRequest(dynamicAdGroupInfo)), null, buildContext(operator));
        softAssertions.assertThat(result.getErrors()).isNotEmpty();

        softAssertions.assertAll();
    }

    @Test
    @SuppressWarnings("squid:S2970")
    public void checkUpdateAdGroup_changeLibraryMinusKeywordsIdsFromEmpty_Success() {
        Long newPackId = createMinusKeywordsPack(clientInfo);
        List<Long> newPackIds = singletonList(newPackId);

        checkUpdateAdGroup_changeLibraryMinusKeywordsIds(newPackIds);

        softAssertions.assertAll();
    }

    @Test
    @SuppressWarnings("squid:S2970")
    public void checkUpdateAdGroup_changeLibraryMinusKeywordsIdsFromOneValueToAnother_Success() {
        Long newPackId = createMinusKeywordsPack(clientInfo);
        List<Long> newPackIds = singletonList(newPackId);

        Long oldPackId = createMinusKeywordsPack(clientInfo);
        minusKeywordsPackRepository.linkLibraryMinusKeywordPackToAdGroup(clientInfo.getShard(), oldPackId, adGroupId);

        checkUpdateAdGroup_changeLibraryMinusKeywordsIds(newPackIds);

        softAssertions.assertAll();
    }

    @Test
    @SuppressWarnings("squid:S2970")
    public void checkUpdateAdGroup_changeLibraryMinusKeywordsIdsFromOneValueToEmpty_Success() {
        List<Long> newPackIds = emptyList();

        Long oldPackId = createMinusKeywordsPack(clientInfo);
        minusKeywordsPackRepository.linkLibraryMinusKeywordPackToAdGroup(clientInfo.getShard(), oldPackId, adGroupId);

        checkUpdateAdGroup_changeLibraryMinusKeywordsIds(newPackIds);

        softAssertions.assertAll();
    }

    @SuppressWarnings("squid:S2970")
    private void checkUpdateAdGroup_changeLibraryMinusKeywordsIds(List<Long> newPackIds) {
        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .setLibraryMinusKeywordsIds(newPackIds)
                .build();

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(singletonList(new GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)));

        processQueryAndCheck(request, expectedPayload);

        List<AdGroup> adGroups =
                adGroupService.getAdGroups(operator.getClientId(), singletonList(adGroupId));

        AdGroup expectedGroup = textAdGroupInfo.getAdGroup()
                .withLibraryMinusKeywordsIds(newPackIds)
                .withStatusBsSynced(StatusBsSynced.NO);

        softAssertions.assertThat(adGroups)
                .hasSize(1);
        softAssertions.assertThat(adGroups)
                .element(0)
                .isEqualToIgnoringGivenFields(expectedGroup, "lastChange");
    }

    @Test
    public void checkUpdateAdGroup_changeLibraryMinusKeywordsIdsToOtherClientIds_MinusWordsPackNotFoundDefect() {
        ClientInfo otherClientInfo = steps.clientSteps().createDefaultClient();
        Long otherClientPackId = createMinusKeywordsPack(otherClientInfo);
        List<Long> newPackIds = singletonList(otherClientPackId);

        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .setLibraryMinusKeywordsIds(newPackIds)
                .build();

        GdValidationResult expectedValidationResult = toGdValidationResult(
                path(field(PATH_FOR_UPDATE_TEXT_AD_GROUP), index(0),
                        field(GdUpdateAdGroupItem.LIBRARY_MINUS_KEYWORDS_IDS), index(0)),
                minusWordsPackNotFound()
        ).withWarnings(emptyList());

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(emptyList())
                .withValidationResult(expectedValidationResult);

        processQueryAndCheck(request, expectedPayload);

        List<AdGroup> adGroups =
                adGroupService.getAdGroups(operator.getClientId(), singletonList(adGroupId));

        AdGroup expectedGroup = textAdGroupInfo.getAdGroup()
                .withLibraryMinusKeywordsIds(emptyList());

        softAssertions.assertThat(adGroups)
                .hasSize(1);
        softAssertions.assertThat(adGroups)
                .element(0)
                .isEqualToIgnoringGivenFields(expectedGroup, "lastChange");

        softAssertions.assertAll();
    }

    private Long createMinusKeywordsPack(ClientInfo info) {
        return steps.minusKeywordsPackSteps().createLibraryMinusKeywordsPack(info).getMinusKeywordPackId();
    }
}
