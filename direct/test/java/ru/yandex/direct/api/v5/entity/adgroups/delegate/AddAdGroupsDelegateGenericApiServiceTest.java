package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import java.util.List;
import java.util.function.Function;

import com.yandex.direct.api.v5.adgroups.AdGroupAddItem;
import com.yandex.direct.api.v5.adgroups.AddRequest;
import com.yandex.direct.api.v5.adgroups.AddResponse;
import com.yandex.direct.api.v5.adgroups.SmartAdGroupAdd;
import com.yandex.direct.api.v5.adgroups.TextAdGroupFeedParamsAdd;
import com.yandex.direct.api.v5.general.ActionResult;
import com.yandex.direct.api.v5.general.ArrayOfLong;
import com.yandex.direct.api.v5.general.ExceptionNotification;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.test.utils.TestUtils;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.entity.adgroups.AdGroupTestUtils.getTextAdGroupFeedParamsUpdate;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@Api5Test
@RunWith(SpringRunner.class)
public class AddAdGroupsDelegateGenericApiServiceTest {

    private static final Integer POSSIBLE_ONLY_ONE_FIELD_ERROR_CODE = 5009;
    private static final Integer FEED_DOES_NOT_EXIST = 8800;
    private static final Integer FEEDS_NOT_ALLOWED = 10165;

    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private BannerTypedRepository bannerTypedRepository;
    @Autowired
    private Steps steps;
    @Autowired
    private ApiAuthenticationSource auth;
    @Autowired
    private AddAdGroupsDelegate delegate;
    @Autowired
    private GenericApiService genericApiService;

    private AdGroupAddItem adGroupAddItem;
    private AddRequest addRequest;
    private Long defaultPackId;
    private Long defaultCampaignId;
    private int shard;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createClient(new ClientInfo());
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.ALLOW_USE_FEEDS_IN_TGO_GROUPS, true);
        ApiUser apiUser = new ApiUser().withClientId(clientInfo.getClientId()).withUid(clientInfo.getUid());
        when(auth.getSubclient()).thenReturn(apiUser);
        when(auth.getChiefSubclient()).thenReturn(apiUser);
        when(auth.getOperator()).thenReturn(apiUser);

        shard = clientInfo.getShard();
        defaultPackId = steps.minusKeywordsPackSteps().createMinusKeywordsPack(clientInfo).getMinusKeywordPackId();
        defaultCampaignId = steps.campaignSteps().createActiveTextCampaign(clientInfo).getCampaignId();

        adGroupAddItem = new AdGroupAddItem()
                .withName("default")
                .withRegionIds(Region.MOSCOW_REGION_ID)
                .withCampaignId(defaultCampaignId);

        addRequest = new AddRequest().withAdGroups(adGroupAddItem);
    }

    @After
    public void resetMocks() {
        Mockito.reset(auth);
    }

    @Test
    public void doAction_AddAdGroupWithLibraryMinusKeywords_LibraryMinusKeywordsLinked() {
        ArrayOfLong negativeKeywordSharedSetIds = new ArrayOfLong().withItems(defaultPackId);
        adGroupAddItem.withNegativeKeywordSharedSetIds(negativeKeywordSharedSetIds);

        AdGroup addedAdGroup = doAction(addRequest);
        assertThat(addedAdGroup)
                .extracting(AdGroup::getLibraryMinusKeywordsIds).asList()
                .as("У группы должны быть библиотечные наборы")
                .containsExactly(defaultPackId);
    }

    @Test
    public void doAction_AddAdGroupWithoutLibraryMinusKeywords_LibraryMinusKeywordsNotLinked() {
        ArrayOfLong emptyArray = new ArrayOfLong();
        adGroupAddItem.withNegativeKeywordSharedSetIds(emptyArray);

        AdGroup addedAdGroup = doAction(addRequest);
        assertThat(addedAdGroup)
                .extracting(AdGroup::getLibraryMinusKeywordsIds).asList()
                .as("У группы не должно быть библиотечных наборов")
                .isEmpty();
    }

    @Test
    public void doAction_AddAdGroupWithFilteredFeedParams_feedExists() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);

        var textAdGroupFeedParamsAdd = new TextAdGroupFeedParamsAdd()
                .withFeedId(feedInfo.getFeedId())
                .withFeedCategoryIds(new ArrayOfLong().withItems(1L, 2L, 3L));

        adGroupAddItem.withTextAdGroupFeedParams(textAdGroupFeedParamsAdd);

        var addRequestWithSeveralGroups = new AddRequest().withAdGroups(adGroupAddItem);

        AdGroup addedAdGroup = doAction(addRequestWithSeveralGroups);

        assertThat(addedAdGroup)
                .as("Созданы текстовые группы с фидами")
                .extracting(AdGroup::getType)
                .isEqualTo(AdGroupType.BASE);

        assertThat((TextAdGroup) addedAdGroup)
                .as("У текстовой группы есть фид")
                .extracting(TextAdGroup::getOldFeedId)
                .isNull();
    }

    @Test
    public void doAction_AddAdGroupWithFilteredFeedParamsWithOnlyFeedId_feedExists() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);

        var textAdGroupFeedParamsAdd = new TextAdGroupFeedParamsAdd()
                .withFeedId(feedInfo.getFeedId());

        adGroupAddItem.withTextAdGroupFeedParams(textAdGroupFeedParamsAdd);

        var addRequestWithSeveralGroups = new AddRequest().withAdGroups(adGroupAddItem);

        AdGroup addedAdGroup = doAction(addRequestWithSeveralGroups);

        assertThat(addedAdGroup.getType())
                .as("Созданы текстовые группы с фидами")
                .isEqualTo(AdGroupType.BASE);

        assertThat((TextAdGroup) addedAdGroup)
                .as("У текстовой группы есть фид")
                .extracting(TextAdGroup::getOldFeedId)
                .isNull();
    }

    @Test
    public void doAction_AddAdGroupWithSmartAdGroupAndFilteredFeedParams_Negative() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        var feedId = feedInfo.getFeedId();

        var smartAdGroupUpdate = new SmartAdGroupAdd()
                .withFeedId(feedId)
                .withAdTitleSource("testFeedTitle");
        var textAdGroupFeedParamsAdd = new TextAdGroupFeedParamsAdd()
                .withFeedId(feedId)
                .withFeedCategoryIds(new ArrayOfLong().withItems(1L, 2L, 3L));

        adGroupAddItem
                .withSmartAdGroup(smartAdGroupUpdate)
                .withTextAdGroupFeedParams(textAdGroupFeedParamsAdd);

        var addRequestWithSeveralGroups = new AddRequest().withAdGroups(adGroupAddItem);

        AddResponse addResponse = genericApiService.doAction(delegate, addRequestWithSeveralGroups);
        checkFirstElementHasError(addResponse, POSSIBLE_ONLY_ONE_FIELD_ERROR_CODE);
    }

    @Test
    public void doAction_AddAdGroupWithFilteredFeedParamsWithDisabledFeature_positiveWithWarning() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.ALLOW_USE_FEEDS_IN_TGO_GROUPS, false);

        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);

        var textAdGroupFeedParamsAdd = new TextAdGroupFeedParamsAdd()
                .withFeedId(feedInfo.getFeedId());

        adGroupAddItem.withTextAdGroupFeedParams(textAdGroupFeedParamsAdd);

        var adGroupAddItem2 = new AdGroupAddItem()
                .withName("group without feed params")
                .withRegionIds(Region.MOSCOW_REGION_ID)
                .withCampaignId(defaultCampaignId);

        var addRequestWithSeveralGroups = new AddRequest().withAdGroups(adGroupAddItem, adGroupAddItem2);

        AddResponse addResponse = genericApiService.doAction(delegate, addRequestWithSeveralGroups);

        var results = addResponse.getAddResults();

        assertThat(results.size()).isEqualTo(2);
        assertThat(results)
                .extracting(r -> mapList(r.getWarnings(), w -> ifNotNull(w, ExceptionNotification::getCode)))
                .isEqualTo(List.of(List.of(FEEDS_NOT_ALLOWED), List.of()));

        AdGroup addedAdGroup = adGroupRepository.getAdGroups(shard, singleton(results.get(0).getId())).get(0);

        assertThat((TextAdGroup) addedAdGroup)
                .as("У текстовой группы нет фида")
                .extracting(TextAdGroup::getOldFeedId)
                .isNull();
    }

    @Test
    public void doAction_AddMultipleAdGroupWithVariousFilteredFeedParams_feedExists() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        var oldFeedId = feedInfo.getFeedId();

        var textAdGroupFeedParamsAdd = new TextAdGroupFeedParamsAdd()
                .withFeedId(oldFeedId)
                .withFeedCategoryIds(new ArrayOfLong().withItems(1L, 2L, 3L));

        adGroupAddItem.withTextAdGroupFeedParams(textAdGroupFeedParamsAdd);

        var adGroupAddItem2 = new AdGroupAddItem()
                .withName("group without feed params")
                .withRegionIds(Region.MOSCOW_REGION_ID)
                .withCampaignId(defaultCampaignId);

        var textAdGroupFeedParamsAdd3 = new TextAdGroupFeedParamsAdd()
                .withFeedId(oldFeedId)
                .withFeedCategoryIds(new ArrayOfLong().withItems(1L, 3L, 5L, 7L));
        getTextAdGroupFeedParamsUpdate(
                feedInfo.getFeedId(), List.of(1L, 3L, 5L, 7L));

        var adGroupAddItem3 = new AdGroupAddItem()
                .withName("group without feed params")
                .withRegionIds(Region.MOSCOW_REGION_ID)
                .withCampaignId(defaultCampaignId)
                .withTextAdGroupFeedParams(textAdGroupFeedParamsAdd3);

        var addRequestWithSeveralGroups = new AddRequest().withAdGroups(adGroupAddItem, adGroupAddItem2,
                adGroupAddItem3);

        List<AdGroup> addedAdGroups = doActionWithMultipleObjects(addRequestWithSeveralGroups);

        assertThat(addedAdGroups)
                .extracting(AdGroup::getType)
                .as("Созданы текстовые группы с фидом")
                .allMatch(AdGroupType.BASE::equals);

        assertThat((TextAdGroup) addedAdGroups.get(0))
                .extracting(TextAdGroup::getOldFeedId)
                .isNull();

        assertThat((TextAdGroup) addedAdGroups.get(1))
                .extracting(TextAdGroup::getOldFeedId)
                .isNull();

        assertThat((TextAdGroup) addedAdGroups.get(2))
                .extracting(TextAdGroup::getOldFeedId)
                .isNull();
    }

    public void doAction_AddAdGroupWithFilteredFeedParams_feedDoesntExist() {

    }

    @Test
    public void doAction_AddMultiplePerformanceAdGroups_noCreativesEnabled() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.SMART_NO_CREATIVES, true);

        String logoImageHash = steps.bannerSteps().createLogoImageFormat(clientInfo).getImageHash();
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        CampaignInfo campaignInfo = steps.campaignSteps().createActivePerformanceCampaign(clientInfo);

        Function<SmartAdGroupAdd, AdGroupAddItem> addItemConstructor = (item) ->
                new AdGroupAddItem()
                        .withName("name")
                        .withRegionIds(225L)
                        .withCampaignId(campaignInfo.getCampaignId())
                        .withSmartAdGroup(item);

        AddRequest request = new AddRequest().withAdGroups(
                addItemConstructor.apply(new SmartAdGroupAdd()
                        .withFeedId(feedInfo.getFeedId())
                        .withLogoExtensionHash(logoImageHash)
                        .withNoCreatives(true)),
                addItemConstructor.apply(new SmartAdGroupAdd()
                        .withFeedId(feedInfo.getFeedId())
                        .withLogoExtensionHash(logoImageHash)),
                addItemConstructor.apply(new SmartAdGroupAdd()
                        .withFeedId(feedInfo.getFeedId())
                        .withLogoExtensionHash("invalidLogoImageHash")
                        .withNoCreatives(true)),
                addItemConstructor.apply(new SmartAdGroupAdd()
                        .withFeedId(0)
                        .withLogoExtensionHash(logoImageHash)
                        .withNoCreatives(true)));

        AddResponse addResponse = genericApiService.doAction(delegate, request);

        List<ActionResult> results = addResponse.getAddResults();

        assertSoftly(softly -> {
            softly.assertThat(results).hasSize(4);
            softly.assertThat(results.get(0)).satisfies(result -> {
                assertThat(result).isNotNull();
                Long adGroupId = result.getId();
                assertThat(adGroupId).isNotNull();
                softly.assertThat(result.getErrors()).isEmpty();
                List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, List.of(adGroupId));
                softly.assertThat(adGroups).hasSize(1);
                softly.assertThat(adGroups.get(0)).satisfies(adGroup -> {
                    assertThat(adGroup).isInstanceOf(PerformanceAdGroup.class);
                    PerformanceAdGroup performanceAdGroup = (PerformanceAdGroup) adGroup;
                    softly.assertThat(performanceAdGroup.getFeedId()).isEqualTo(feedInfo.getFeedId());
                });
                List<PerformanceBannerMain> banners = bannerTypedRepository
                        .getBannersByGroupIds(shard, List.of(adGroupId), PerformanceBannerMain.class);
                softly.assertThat(banners).hasSize(1);
                softly.assertThat(banners.get(0)).satisfies(banner -> {
                    assertThat(banner).isNotNull();
                    softly.assertThat(banner.getLogoImageHash()).isEqualTo(logoImageHash);
                });
            });
            softly.assertThat(results.get(1)).satisfies(result -> {
                assertThat(result).isNotNull();
                softly.assertThat(result.getId()).isNull();
                softly.assertThat(result.getErrors())
                        .extracting(ExceptionNotification::getCode)
                        .containsExactly(4000);
            });
            softly.assertThat(results.get(2)).satisfies(result -> {
                assertThat(result).isNotNull();
                softly.assertThat(result.getId()).isNull();
                softly.assertThat(result.getErrors())
                        .extracting(ExceptionNotification::getCode)
                        .containsExactly(8800);
            });
            softly.assertThat(results.get(3)).satisfies(result -> {
                assertThat(result).isNotNull();
                softly.assertThat(result.getId()).isNull();
                softly.assertThat(result.getErrors())
                        .extracting(ExceptionNotification::getCode)
                        .containsExactly(5004);
            });
        });
    }

    private AdGroup doAction(AddRequest request) {
        AddResponse addResponse = genericApiService.doAction(delegate, request);
        List<ActionResult> results = addResponse.getAddResults();

        TestUtils.assumeThat(sa -> sa.assertThat(results).hasSize(1));
        Long addedAdGroupId = results.get(0).getId();

        return adGroupRepository.getAdGroups(shard, singleton(addedAdGroupId)).get(0);
    }

    private List<AdGroup> doActionWithMultipleObjects(AddRequest request) {
        AddResponse addResponse = genericApiService.doAction(delegate, request);
        List<ActionResult> results = addResponse.getAddResults();

        TestUtils.assumeThat(sa -> sa.assertThat(results)
                .as("кол-во объектов в ответе соответствует кол-ву обектов в запросе")
                .hasSize(request.getAdGroups().size()));

        List<Long> addedAdGroupId = mapList(results, ActionResult::getId);

        return adGroupRepository.getAdGroups(shard, addedAdGroupId);
    }

    private void checkFirstElementHasError(AddResponse response, Integer expectedErrorCode) {
        assertThat(response)
                .extracting(AddResponse::getAddResults)
                .asInstanceOf(InstanceOfAssertFactories.list(ActionResult.class))
                .singleElement()
                .as("Для первого элемента ожидаем ошибку с кодом [%s]", expectedErrorCode)
                .extracting(ActionResult::getErrors)
                .asInstanceOf(InstanceOfAssertFactories.list(ExceptionNotification.class))
                .extracting(ExceptionNotification::getCode)
                .containsExactly(expectedErrorCode);
    }

}
