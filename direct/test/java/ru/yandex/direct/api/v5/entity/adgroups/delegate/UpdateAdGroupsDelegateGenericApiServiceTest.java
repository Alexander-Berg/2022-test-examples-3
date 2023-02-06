package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;

import com.yandex.direct.api.v5.adgroups.AdGroupUpdateItem;
import com.yandex.direct.api.v5.adgroups.DynamicTextAdGroup;
import com.yandex.direct.api.v5.adgroups.ObjectFactory;
import com.yandex.direct.api.v5.adgroups.SmartAdGroupUpdate;
import com.yandex.direct.api.v5.adgroups.UpdateRequest;
import com.yandex.direct.api.v5.adgroups.UpdateResponse;
import com.yandex.direct.api.v5.general.ActionResult;
import com.yandex.direct.api.v5.general.ArrayOfLong;
import com.yandex.direct.api.v5.general.ExceptionNotification;
import one.util.streamex.StreamEx;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.entity.adgroups.container.AdGroupsContainer;
import ru.yandex.direct.api.v5.result.ApiMassResult;
import ru.yandex.direct.api.v5.result.ApiResult;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.banner.model.BannerLogoStatusModerate;
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.info.NewPerformanceMainBannerInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.repository.TestMinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.entity.adgroups.AdGroupDefectTypes.inconsistentAdGroupType;
import static ru.yandex.direct.api.v5.entity.adgroups.AdGroupTestUtils.getTextAdGroupFeedParamsUpdate;
import static ru.yandex.direct.api.v5.validation.DefectTypes.possibleOnlyOneField;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.core.testing.data.TestNewPerformanceMainBanners.fullPerformanceMainBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.utils.FunctionalUtils.selectList;
import static ru.yandex.direct.validation.result.PathHelper.path;

@Api5Test
@RunWith(SpringRunner.class)
public class UpdateAdGroupsDelegateGenericApiServiceTest {

    private static final Integer BAD_PARAMS = 4000;
    private static final Integer FEED_DOES_NOT_EXIST = 8800;
    private static final Integer FEEDS_NOT_ALLOWED = 10165;

    public static final ObjectFactory FACTORY = new ObjectFactory();
    @Autowired
    private TestMinusKeywordsPackRepository testMinusKeywordsPackRepository;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private AdGroupService adGroupService;
    @Autowired
    private Steps steps;
    @Autowired
    private ApiAuthenticationSource auth;
    @Autowired
    private UpdateAdGroupsDelegate delegate;
    @Autowired
    private GenericApiService genericApiService;

    private UpdateRequest updateRequest;
    private AdGroupUpdateItem adGroupUpdateItem;
    private Long defaultAdGroupId;
    private Long defaultPackId;
    private int shard;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createClient(new ClientInfo());
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.ALLOW_USE_FEEDS_IN_TGO_GROUPS,
                true);
        ApiUser apiUser = new ApiUser().withClientId(clientInfo.getClientId()).withUid(clientInfo.getUid());
        when(auth.getSubclient()).thenReturn(apiUser);
        when(auth.getChiefSubclient()).thenReturn(apiUser);
        when(auth.getOperator()).thenReturn(apiUser);

        shard = clientInfo.getShard();
        defaultAdGroupId = steps.adGroupSteps().createDefaultAdGroup(clientInfo).getAdGroupId();
        defaultPackId = steps.minusKeywordsPackSteps().createMinusKeywordsPack(clientInfo).getMinusKeywordPackId();

        adGroupUpdateItem = new AdGroupUpdateItem().withId(defaultAdGroupId);
        updateRequest = new UpdateRequest().withAdGroups(adGroupUpdateItem);
    }

    @After
    public void resetMocks() {
        Mockito.reset(auth);
    }

    @Test
    public void doAction_LinkLibraryMinusKeywords_LibraryMinusKeywordsLinked() {

        JAXBElement<ArrayOfLong> negativeKeywordSharedSetIds = FACTORY
                .createAdGroupBaseNegativeKeywordSharedSetIds(new ArrayOfLong().withItems(defaultPackId));

        adGroupUpdateItem.withNegativeKeywordSharedSetIds(negativeKeywordSharedSetIds);

        AdGroup updatedAdGroup = doAction(defaultAdGroupId, updateRequest);
        assertThat(updatedAdGroup.getLibraryMinusKeywordsIds()).contains(defaultPackId);
    }

    @Test
    public void doAction_UnlinkLibraryMinusKeywords_LibraryMinusKeywordsUnlinked() {

        JAXBElement<ArrayOfLong> emptyArray = FACTORY
                .createAdGroupBaseNegativeKeywordSharedSetIds(new ArrayOfLong());

        adGroupUpdateItem.withNegativeKeywordSharedSetIds(emptyArray);

        testMinusKeywordsPackRepository.linkLibraryMinusKeywordPackToAdGroup(shard, defaultPackId, defaultAdGroupId);

        AdGroup updatedAdGroup = doAction(defaultAdGroupId, updateRequest);
        assertThat(updatedAdGroup.getLibraryMinusKeywordsIds()).isEmpty();
    }

    @Test
    public void processRequest_oneInexistentOneValid() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);

        UpdateRequest request = new UpdateRequest().withAdGroups(
                new AdGroupUpdateItem().withId(adGroupInfo.getAdGroupId() + 10000),
                new AdGroupUpdateItem().withId(adGroupInfo.getAdGroupId()));

        UpdateResponse updateResponse = genericApiService.doAction(delegate, request);

        List<ActionResult> results = updateResponse.getUpdateResults();

        assertSoftly(softly -> {
            softly.assertThat(results).hasSize(2);
            softly.assertThat(results.get(0)).satisfies(result -> {
                assertThat(result).isNotNull();
                softly.assertThat(result.getId()).isNull();
                softly.assertThat(result.getErrors())
                        .extracting(ExceptionNotification::getCode)
                        .containsExactly(8800);
            });
            softly.assertThat(results.get(1)).satisfies(result -> {
                assertThat(result).isNotNull();
                softly.assertThat(result.getId()).isEqualTo(adGroupInfo.getAdGroupId());
                softly.assertThat(result.getErrors()).isEmpty();
            });
        });
    }

    @Test
    public void processRequest_performanceAdGroup_withCustomFields() {
        var adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        var feedTitle = "aaabbc";
        var feedId = steps.feedSteps().createDefaultFeed(clientInfo).getFeedId();
        var smartAdGroupUpdate = new SmartAdGroupUpdate()
                .withAdTitleSource(FACTORY.createSmartAdGroupGetAdTitleSource(feedTitle))
                .withFeedId(feedId);
        var adGroupUpdateItem = new AdGroupUpdateItem()
                .withId(adGroupInfo.getAdGroupId())
                .withSmartAdGroup(smartAdGroupUpdate);
        var externalRequest = new UpdateRequest().withAdGroups(adGroupUpdateItem);
        List<AdGroupsContainer> modelChanges = delegate.convertRequest(externalRequest);
        ValidationResult<List<AdGroupsContainer>, DefectType> vr =
                delegate.validateInternalRequest(modelChanges);
        ApiResult<Long> apiResult = delegate.processRequest(vr.getValue()).getResult().get(0);
        var adGroup = (PerformanceAdGroup) adGroupService.getAdGroup(apiResult.getResult());

        assertThat(adGroup).isNotNull();
        assertThat(adGroup.getFieldToUseAsName()).isEqualTo(feedTitle);
        assertThat(adGroup.getFeedId()).isEqualTo(feedId);
    }

    @Test
    public void processRequest_performanceAdGroup_withTextAdGroupFilteredFeedFields_Negative() {
        var adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        var feedTitle = "aaabbc";
        var smartAdGroupUpdate = new SmartAdGroupUpdate()
                .withAdTitleSource(FACTORY.createSmartAdGroupGetAdTitleSource(feedTitle));
        var textAdGroupFilteredFeedParams = getTextAdGroupFeedParamsUpdate(
                adGroupInfo.getFeedId(), List.of(1L, 3L, 9L));

        //ошибочный запрос на обновление smartAdGroup c параметром TextAdGroupFeedParam
        var adGroupUpdateItem = new AdGroupUpdateItem()
                .withId(adGroupInfo.getAdGroupId())
                .withName(adGroupInfo.getAdGroup().getName().concat(RandomStringUtils.randomAlphabetic(4)))
                .withSmartAdGroup(smartAdGroupUpdate)
                .withTextAdGroupFeedParams(textAdGroupFilteredFeedParams);
        var externalRequest = new UpdateRequest().withAdGroups(adGroupUpdateItem);
        List<AdGroupsContainer> modelChanges = delegate.convertRequest(externalRequest);
        ValidationResult<List<AdGroupsContainer>, DefectType> vr =
                delegate.validateInternalRequest(modelChanges);
        ApiResult<Long> apiResult = delegate.processRequest(vr.getValue()).getResult().get(0);

        assumeThat("Результат содержит ошибку", apiResult.isSuccessful(), is(false));
        assertThat(apiResult.getErrors())
                .is(matchedBy(contains(validationError(path(), possibleOnlyOneField()))));

        var adGroup = (PerformanceAdGroup) adGroupService.getAdGroup(adGroupInfo.getAdGroupId());
        assertThat(adGroup).isNotNull();
        assertThat(adGroup.getName()).isEqualTo(adGroupInfo.getAdGroup().getName());
    }

    @Test
    public void processRequest_performanceAdGroup_withOnlyTextAdGroupFilteredFeedFields_Negative() {
        var adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        var textAdGroupFilteredFeedParams = getTextAdGroupFeedParamsUpdate(
                adGroupInfo.getFeedId(), List.of(1L, 3L, 9L));

        //ошибочный запрос на обновление smartAdGroup c параметром TextAdGroupFeedParam
        var adGroupUpdateItem = new AdGroupUpdateItem()
                .withId(adGroupInfo.getAdGroupId())
                .withName(adGroupInfo.getAdGroup().getName().concat(RandomStringUtils.randomAlphabetic(4)))
                .withTextAdGroupFeedParams(textAdGroupFilteredFeedParams);
        var externalRequest = new UpdateRequest().withAdGroups(adGroupUpdateItem);
        List<AdGroupsContainer> modelChanges = delegate.convertRequest(externalRequest);
        ValidationResult<List<AdGroupsContainer>, DefectType> vr =
                delegate.validateInternalRequest(modelChanges);
        ApiResult<Long> apiResult = delegate.processRequest(vr.getValue()).getResult().get(0);

        assumeThat("Результат содержит ошибку", apiResult.isSuccessful(), is(false));
        assertThat(apiResult.getErrors())
                .is(matchedBy(contains(validationError(path(), inconsistentAdGroupType()))));

        var adGroup = (PerformanceAdGroup) adGroupService.getAdGroup(adGroupInfo.getAdGroupId());
        assumeThat(adGroup, notNullValue());

        assertThat(adGroup.getName()).isEqualTo(adGroupInfo.getAdGroup().getName());
    }

    @Test
    public void processRequest_performanceAdGroup_withDynamicAdGroupFields_Negative() {
        var adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);

        //ошибочный запрос на обновление smartAdGroup c параметром DynamicTextAdGroup
        var adGroupUpdateItem = new AdGroupUpdateItem()
                .withId(adGroupInfo.getAdGroupId())
                .withName(adGroupInfo.getAdGroup().getName().concat("_" + RandomStringUtils.randomAlphabetic(4)))
                .withSmartAdGroup(new SmartAdGroupUpdate()
                        .withAdTitleSource(FACTORY.createSmartAdGroupGetAdTitleSource("newFeedTitle")))
                .withDynamicTextAdGroup(new DynamicTextAdGroup().withDomainUrl("http://abyrvalg.com"));
        var externalRequest = new UpdateRequest().withAdGroups(adGroupUpdateItem);
        List<AdGroupsContainer> modelChanges = delegate.convertRequest(externalRequest);
        ValidationResult<List<AdGroupsContainer>, DefectType> vr =
                delegate.validateInternalRequest(modelChanges);
        ApiResult<Long> apiResult = delegate.processRequest(vr.getValue()).getResult().get(0);

        assumeThat("Результат содержит ошибку", apiResult.isSuccessful(), is(false));
        assertThat(apiResult.getErrors())
                .is(matchedBy(contains(validationError(path(), possibleOnlyOneField()))));

        var adGroup = (PerformanceAdGroup) adGroupService.getAdGroup(adGroupInfo.getAdGroupId());
        assumeThat(adGroup, notNullValue());

        assertThat(adGroup.getName()).isEqualTo(adGroupInfo.getAdGroup().getName());
    }

    @Test
    public void processRequest_performanceAdGroup_noCreativesEnabled() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.SMART_NO_CREATIVES, true);

        String fieldToUseAsName = "adTitleSource";
        String logoImageHash = steps.bannerSteps().createLogoImageFormat(clientInfo).getImageHash();

        PerformanceAdGroupInfo firstAdGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        steps.performanceMainBannerSteps().createPerformanceMainBanner(firstAdGroupInfo);

        PerformanceAdGroupInfo secondAdGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        steps.performanceMainBannerSteps().createPerformanceMainBanner(new NewPerformanceMainBannerInfo()
                .withAdGroupInfo(secondAdGroupInfo)
                .withBanner(fullPerformanceMainBanner()
                        .withLogoImageHash(logoImageHash)
                        .withLogoStatusModerate(BannerLogoStatusModerate.YES)));

        PerformanceAdGroupInfo emptyAdGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);

        UpdateRequest request = new UpdateRequest().withAdGroups(
                new AdGroupUpdateItem()
                        .withId(firstAdGroupInfo.getAdGroupId())
                        .withSmartAdGroup(new SmartAdGroupUpdate()
                                .withAdTitleSource(FACTORY.createSmartAdGroupGetAdTitleSource(fieldToUseAsName))
                                .withLogoExtensionHash(FACTORY.createSmartAdGroupGetLogoExtensionHash("invalid"))),
                new AdGroupUpdateItem()
                        .withId(secondAdGroupInfo.getAdGroupId())
                        .withSmartAdGroup(new SmartAdGroupUpdate()
                                .withAdTitleSource(FACTORY.createSmartAdGroupGetAdTitleSource(fieldToUseAsName))
                                .withLogoExtensionHash(FACTORY.createSmartAdGroupGetLogoExtensionHash(null))),
                new AdGroupUpdateItem()
                        .withId(emptyAdGroupInfo.getAdGroupId())
                        .withSmartAdGroup(new SmartAdGroupUpdate()
                                .withAdTitleSource(FACTORY.createSmartAdGroupGetAdTitleSource(fieldToUseAsName))
                                .withLogoExtensionHash(FACTORY.createSmartAdGroupGetLogoExtensionHash(logoImageHash))));

        UpdateResponse updateResponse = genericApiService.doAction(delegate, request);

        List<ActionResult> results = updateResponse.getUpdateResults();

        assertSoftly(softly -> {
            softly.assertThat(results).hasSize(3);
            softly.assertThat(results.get(0)).satisfies(result -> {
                assertThat(result).isNotNull();
                softly.assertThat(result.getId()).isNull();
                softly.assertThat(result.getErrors())
                        .extracting(ExceptionNotification::getCode)
                        .containsExactly(8800);

                PerformanceAdGroup adGroup = getAdGroup(firstAdGroupInfo.getAdGroupId(), PerformanceAdGroup.class);
                softly.assertThat(adGroup.getFieldToUseAsName())
                        .isEqualTo(firstAdGroupInfo.getPerformanceAdGroup().getFieldToUseAsName());

                List<PerformanceBannerMain> banners = selectList(adGroup.getBanners(), PerformanceBannerMain.class);
                softly.assertThat(banners).hasSize(1);
                softly.assertThat(banners.get(0)).satisfies(banner -> {
                    assertThat(banner).isNotNull();
                    softly.assertThat(banner.getLogoImageHash()).isNull();
                });
            });
            softly.assertThat(results.get(1)).satisfies(result -> {
                assertThat(result).isNotNull();
                softly.assertThat(result.getId()).isEqualTo(secondAdGroupInfo.getAdGroupId());
                softly.assertThat(result.getErrors()).isEmpty();

                PerformanceAdGroup adGroup = getAdGroup(secondAdGroupInfo.getAdGroupId(), PerformanceAdGroup.class);
                softly.assertThat(adGroup.getFieldToUseAsName()).isEqualTo(fieldToUseAsName);

                List<PerformanceBannerMain> banners = selectList(adGroup.getBanners(), PerformanceBannerMain.class);
                softly.assertThat(banners).hasSize(1);
                softly.assertThat(banners.get(0)).satisfies(banner -> {
                    assertThat(banner).isNotNull();
                    softly.assertThat(banner.getLogoImageHash()).isNull();
                });
            });
            softly.assertThat(results.get(2)).satisfies(result -> {
                assertThat(result).isNotNull();
                softly.assertThat(result.getId()).isEqualTo(emptyAdGroupInfo.getAdGroupId());
                softly.assertThat(result.getErrors()).isEmpty();

                PerformanceAdGroup adGroup = getAdGroup(emptyAdGroupInfo.getAdGroupId(), PerformanceAdGroup.class);
                softly.assertThat(adGroup.getFieldToUseAsName()).isEqualTo(fieldToUseAsName);

                List<PerformanceBannerMain> banners = selectList(adGroup.getBanners(), PerformanceBannerMain.class);
                softly.assertThat(banners).hasSize(1);
                softly.assertThat(banners.get(0)).satisfies(banner -> {
                    assertThat(banner).isNotNull();
                    softly.assertThat(banner.getLogoImageHash()).isEqualTo(logoImageHash);
                });
            });
        });
    }

    @Test
    public void processList_ContentPromotionAdGroups() {
        var contentPromotionVideoAdGroup = steps.adGroupSteps().createDefaultContentPromotionAdGroup(clientInfo,
                ContentPromotionAdgroupType.VIDEO);
        var contentPromotionCollectionsAdGroup = steps.adGroupSteps().createDefaultContentPromotionAdGroup(clientInfo,
                ContentPromotionAdgroupType.COLLECTION);
        var contentPromotionEdaAdGroup = steps.adGroupSteps().createDefaultContentPromotionAdGroup(clientInfo,
                ContentPromotionAdgroupType.EDA);
        List<Long> adGroupIds = mapList(Arrays.asList(contentPromotionVideoAdGroup, contentPromotionCollectionsAdGroup,
                contentPromotionEdaAdGroup), AdGroupInfo::getAdGroupId);
        var updateItems = mapList(adGroupIds, id -> new AdGroupUpdateItem().withName("someRandomName").withId(id));
        List<AdGroupsContainer> modelChanges = delegate.convertRequest(
                new UpdateRequest().withAdGroups(updateItems));
        ValidationResult<List<AdGroupsContainer>, DefectType> vr = delegate.validateInternalRequest(modelChanges);
        ApiMassResult<Long> apiResult = delegate.processList(vr.getValue());
        assertThat(apiResult.getErrorCount()).isEqualTo(0);
        List<String> actualNames = adGroupRepository.getAdGroups(clientInfo.getShard(), adGroupIds).stream()
                .map(AdGroup::getName)
                .collect(Collectors.toList());
        assertThat(actualNames).allMatch("someRandomName"::equals);
    }

    @Test
    public void processList_TextAdGroups_addFilteredFeedParams() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.ALLOW_USE_FEEDS_IN_TGO_GROUPS,
                false);

        var textAdGroupWithoutFeeds = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);

        var adGroupId = textAdGroupWithoutFeeds.getAdGroupId();
        var textAdGroupFeedParamsUpdate = getTextAdGroupFeedParamsUpdate(
                feedInfo.getFeedId(), List.of(1L, 2L, 3L));

        var updateItem = new AdGroupUpdateItem()
                .withId(adGroupId)
                .withName("textAdgroupWithFeeds")
                .withTextAdGroupFeedParams(textAdGroupFeedParamsUpdate);

        List<AdGroupsContainer> modelChanges = delegate.convertRequest(
                new UpdateRequest().withAdGroups(updateItem));
        ValidationResult<List<AdGroupsContainer>, DefectType> vr = delegate.validateInternalRequest(modelChanges);
        ApiMassResult<Long> apiResult = delegate.processList(vr.getValue());
        assertThat(apiResult.getErrorCount()).isEqualTo(0);

        TextAdGroup textAdGroup = StreamEx.of(adGroupRepository.getAdGroups(clientInfo.getShard(),
                        List.of(adGroupId)))
                .select(TextAdGroup.class).findFirst().orElse(new TextAdGroup());
        assertThat(textAdGroup).matches(g -> g.getOldFeedId() == null &&
                g.getFilteredFeedId() == null &&
                g.getFeedFilterCategories() == null);
    }

    @Test
    public void processList_TextAdGroupsWithFeature_updateWithoutFilteredFeedParamsWithWarning() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.ALLOW_USE_FEEDS_IN_TGO_GROUPS,
                false);

        var textAdGroupWithoutFeeds = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);

        var adGroupId = textAdGroupWithoutFeeds.getAdGroupId();
        var textAdGroupFeedParamsUpdate = getTextAdGroupFeedParamsUpdate(
                feedInfo.getFeedId(), List.of(1L, 2L, 3L));

        var updateItem = new AdGroupUpdateItem()
                .withId(adGroupId)
                .withName("textAdgroupWithFeeds")
                .withTextAdGroupFeedParams(textAdGroupFeedParamsUpdate);

        var textAdGroupWithoutFeeds2 = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        var updateItem2 = new AdGroupUpdateItem()
                .withId(textAdGroupWithoutFeeds2.getAdGroupId())
                .withName("textAdgroupWithoutFeeds");

        List<AdGroupsContainer> modelChanges = delegate.convertRequest(
                new UpdateRequest().withAdGroups(updateItem, updateItem2));
        ValidationResult<List<AdGroupsContainer>, DefectType> vr = delegate.validateInternalRequest(modelChanges);
        assertThat(vr.hasAnyWarnings()).isTrue();

        assertThat(vr.getSubResults().values())
                .extracting(v -> mapList(v.getWarnings(), w -> ifNotNull(w, DefectType::getCode)))
                .isEqualTo(List.of(List.of(FEEDS_NOT_ALLOWED), List.of()));

        ApiMassResult<Long> apiResult = delegate.processList(vr.getValue());
        assertThat(apiResult.getErrorCount()).isEqualTo(0);

        TextAdGroup textAdGroup = StreamEx.of(adGroupRepository.getAdGroups(clientInfo.getShard(),
                        List.of(adGroupId)))
                .select(TextAdGroup.class).findFirst().orElse(new TextAdGroup());
        assertThat(textAdGroup).matches(g -> g.getOldFeedId() == null &&
                g.getFilteredFeedId() == null &&
                g.getFeedFilterCategories() == null);
    }

    private AdGroup doAction(Long adGroupId, UpdateRequest request) {
        UpdateResponse updateResponse = genericApiService.doAction(delegate, request);
        List<ActionResult> results = updateResponse.getUpdateResults();

        assumeThat("ожидается один результат", results, hasSize(1));
        Long updatedAdGroupId = results.get(0).getId();
        assumeThat(updatedAdGroupId, equalTo(adGroupId));

        return getAdGroup(adGroupId, AdGroup.class);
    }

    private <T extends AdGroup> T getAdGroup(Long adGroupId, Class<T> clazz) {
        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, List.of(adGroupId));
        assertThat(adGroups).hasSize(1);
        AdGroup adGroup = adGroups.get(0);
        assertThat(adGroup).isInstanceOf(clazz);
        adGroupService.enrichPerformanceAdGroups(Objects.requireNonNull(clientInfo.getClientId()), List.of(adGroup));
        return clazz.cast(adGroup);
    }
}
