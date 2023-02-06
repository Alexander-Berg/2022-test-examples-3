package ru.yandex.direct.core.entity.bids.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.direct.core.entity.adgroup.service.AdGroupsShowsForecastService;
import ru.yandex.direct.core.entity.auction.container.AdGroupForAuction;
import ru.yandex.direct.core.entity.auction.container.bs.Block;
import ru.yandex.direct.core.entity.auction.container.bs.KeywordBidBsAuctionData;
import ru.yandex.direct.core.entity.auction.container.bs.KeywordTrafaretData;
import ru.yandex.direct.core.entity.auction.container.bs.Position;
import ru.yandex.direct.core.entity.auction.container.bs.TrafaretBidItem;
import ru.yandex.direct.core.entity.auction.service.BsAuctionService;
import ru.yandex.direct.core.entity.bids.container.BidTargetType;
import ru.yandex.direct.core.entity.bids.container.KeywordBidPokazometerData;
import ru.yandex.direct.core.entity.bids.container.SetAutoBidCalculationType;
import ru.yandex.direct.core.entity.bids.container.SetAutoBidItem;
import ru.yandex.direct.core.entity.bids.container.SetAutoNetworkByCoverage;
import ru.yandex.direct.core.entity.bids.container.SetAutoSearchByPosition;
import ru.yandex.direct.core.entity.bids.container.SetAutoSearchByTrafficVolume;
import ru.yandex.direct.core.entity.bids.model.Bid;
import ru.yandex.direct.core.entity.bids.repository.BidRepository;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.model.Place;
import ru.yandex.direct.core.entity.keyword.service.KeywordService;
import ru.yandex.direct.core.testing.configuration.CoreTestingConfiguration;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.NewKeywordInfo;
import ru.yandex.direct.core.testing.info.adgroup.ContentPromotionAdGroupInfo;
import ru.yandex.direct.core.testing.info.campaign.ContentPromotionCampaignInfo;
import ru.yandex.direct.core.testing.repository.TestAdGroupRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.pokazometer.PhraseResponse;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.VIDEO;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.adGroupTypeNotSupported;
import static ru.yandex.direct.core.entity.bids.validation.BidsDefects.bidChangeNotAllowedForBsRarelyLoadedAdGroup;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeContentPromotionCampaign;
import static ru.yandex.direct.core.testing.data.adgroup.TestContentPromotionAdGroups.fullContentPromotionAdGroup;
import static ru.yandex.direct.core.testing.data.campaign.TestContentPromotionCampaigns.fullContentPromotionCampaign;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFailed;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.duplicatedObject;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxCollectionSize;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;


@ContextHierarchy({
        @ContextConfiguration(classes = CoreTestingConfiguration.class),
        @ContextConfiguration(classes = BidServiceSetAutoTest.OverridingConfiguration.class)
})
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class BidServiceSetAutoTest {

    private static final CurrencyCode CURRENCY_CODE = CurrencyCode.RUB;

    @Autowired
    private Steps steps;

    @Autowired
    private KeywordService keywordService;

    @Autowired
    private TestAdGroupRepository testAdGroupRepository;

    @Autowired
    private BidService testingBidService;

    @Autowired
    private BidRepository bidRepository;

    private KeywordInfo keywordInfo;
    private KeywordInfo anotherKeywordInfo;
    private NewKeywordInfo contentPromotionKeywordInfo;
    private ClientId clientId;
    private Long clientUid;

    @Before
    public void setUp() throws Exception {
        keywordInfo = steps.keywordSteps().createDefaultKeyword();
        steps.bannerSteps().createBanner(activeTextBanner(keywordInfo.getCampaignId(), keywordInfo.getAdGroupId()),
                keywordInfo.getAdGroupInfo());

        ClientInfo clientInfo = keywordInfo.getAdGroupInfo().getClientInfo();
        clientId = clientInfo.getClientId();
        clientUid = clientInfo.getUid();

        CampaignInfo anotherCampaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        AdGroupInfo anotherAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(anotherCampaignInfo);
        anotherKeywordInfo = steps.keywordSteps().createKeyword(anotherAdGroupInfo);

        var contentPromotionAdGroupInfo = steps.contentPromotionAdGroupSteps()
                .createAdGroup(new ContentPromotionAdGroupInfo()
                        .withAdGroup(fullContentPromotionAdGroup(VIDEO))
                        .withCampaignInfo(new ContentPromotionCampaignInfo()
                                .withTypedCampaign(fullContentPromotionCampaign()
                                        .withStrategy(TestCampaigns.manualBothDifferentStrategy()))
                                .withCampaign(activeContentPromotionCampaign(null, null))
                                .withClientInfo(clientInfo)));
        contentPromotionKeywordInfo = steps.newKeywordSteps().createKeyword(contentPromotionAdGroupInfo);
        steps.contentPromotionBannerSteps().createDefaultBanner(contentPromotionKeywordInfo.getAdGroupInfo());
    }

    @Test
    public void setAutoBids_success_forNetworkByCoverage() {
        Long id = keywordInfo.getId();

        MassResult<SetAutoBidItem> result =
                testingBidService.setAutoBids(clientId, clientUid,
                        singletonList(createEmptySetAutoBidItemByKeywordId(id)
                                .withScope(EnumSet.of(BidTargetType.CONTEXT))
                                .withNetworkByCoverage(new SetAutoNetworkByCoverage()
                                        .withContextCoverage(50)
                                        .withIncreasePercent(50))
                        ), false);

        assertCorrectResultForSingleKeywordIsWrittenToDb(id, result, null, "75" /*50 + 25 (50%)*/);
    }

    @Test
    public void setAutoBids_ContentPromotionKeyword_ForNetworkByCoverage_NothingChanged() {
        Long id = contentPromotionKeywordInfo.getKeywordId();

        MassResult<SetAutoBidItem> result =
                testingBidService.setAutoBids(clientId, clientUid,
                        singletonList(createEmptySetAutoBidItemByKeywordId(id)
                                .withScope(EnumSet.of(BidTargetType.CONTEXT))
                                .withNetworkByCoverage(new SetAutoNetworkByCoverage()
                                        .withContextCoverage(50)
                                        .withIncreasePercent(50))
                        ), false);

        assertThat(result)
                .describedAs("setAuto должен пройти валидацию")
                .is(matchedBy(isSuccessful()));
        assertThat(result.getValidationResult())
                .describedAs("Ошибка первого элемента должна соответствовать ожидаемой")
                .is(matchedBy(hasDefectDefinitionWith(validationError(path(index(0)), adGroupTypeNotSupported()))));
    }

    @Test
    public void setAutoBids_success_forSearchByPosition() {
        Long id = keywordInfo.getId();

        MassResult<SetAutoBidItem> result =
                testingBidService.setAutoBids(clientId, clientUid,
                        singletonList(createEmptySetAutoBidItemByKeywordId(id)
                                .withScope(EnumSet.of(BidTargetType.SEARCH))
                                .withSearchByPosition(new SetAutoSearchByPosition()
                                        .withPosition(Place.PREMIUM)
                                        .withIncreasePercent(50)
                                        .withCalculatedBy(SetAutoBidCalculationType.VALUE))
                        ), false);

        assertCorrectResultForSingleKeywordIsWrittenToDb(id, result, "165" /*110 + 55(50%)*/, null);
    }

    @Test
    public void setAutoBids_ContentPromotionKeyword_ForSearchByPosition_NothingChanged() {
        Long id = contentPromotionKeywordInfo.getKeywordId();

        MassResult<SetAutoBidItem> result =
                testingBidService.setAutoBids(clientId, clientUid,
                        singletonList(createEmptySetAutoBidItemByKeywordId(id)
                                .withScope(EnumSet.of(BidTargetType.SEARCH))
                                .withSearchByPosition(new SetAutoSearchByPosition()
                                        .withPosition(Place.PREMIUM)
                                        .withIncreasePercent(50)
                                        .withCalculatedBy(SetAutoBidCalculationType.VALUE))
                        ), false);

        assertThat(result)
                .describedAs("setAuto должен пройти валидацию")
                .is(matchedBy(isSuccessful()));
        assertThat(result.getValidationResult())
                .describedAs("Ошибка первого элемента должна соответствовать ожидаемой")
                .is(matchedBy(hasDefectDefinitionWith(validationError(path(index(0)), adGroupTypeNotSupported()))));
    }

    @Test
    public void setAutoBids_success_forSearchByTrafficVolumeWithNoMax() {
        Long id = keywordInfo.getId();

        MassResult<SetAutoBidItem> result =
                testingBidService.setAutoBids(clientId, clientUid,
                        singletonList(createEmptySetAutoBidItemByKeywordId(id)
                                .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                        .withTargetTrafficVolume(50)
                                        .withIncreasePercent(50))
                        ), false);

        assertCorrectResultForSingleKeywordIsWrittenToDb(id, result, "75" /*50 + 25(50%)*/, null);
    }

    @Test
    public void setAutoBids_ContentPromotionKeyword_ForSearchByTrafficVolumeWithNoMax_NothingChanged() {
        Long id = contentPromotionKeywordInfo.getKeywordId();

        MassResult<SetAutoBidItem> result =
                testingBidService.setAutoBids(clientId, clientUid,
                        singletonList(createEmptySetAutoBidItemByKeywordId(id)
                                .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                        .withTargetTrafficVolume(50)
                                        .withIncreasePercent(50))
                        ), false);

        assertThat(result)
                .describedAs("setAuto должен пройти валидацию")
                .is(matchedBy(isSuccessful()));
        assertThat(result.getValidationResult())
                .describedAs("Ошибка первого элемента должна соответствовать ожидаемой")
                .is(matchedBy(hasDefectDefinitionWith(validationError(path(index(0)), adGroupTypeNotSupported()))));
    }

    @Test
    public void setAutoBids_success_forSearchByTrafficVolumeWithMaxFalse() {
        Long id = keywordInfo.getId();

        MassResult<SetAutoBidItem> result =
                testingBidService.setAutoBids(clientId, clientUid,
                        singletonList(createEmptySetAutoBidItemByKeywordId(id)
                                .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                        .withTargetTrafficVolume(50)
                                        .withSetMaximum(false)
                                        .withIncreasePercent(50))
                        ), false);

        assertCorrectResultForSingleKeywordIsWrittenToDb(id, result, "75" /*50 + 25(50%)*/, null);
    }

    @Test
    public void setAutoBids_ContentPromotionKeyword_ForSearchByTrafficVolumeWithMaxFalse_NothingChanged() {
        Long id = contentPromotionKeywordInfo.getKeywordId();

        MassResult<SetAutoBidItem> result =
                testingBidService.setAutoBids(clientId, clientUid,
                        singletonList(createEmptySetAutoBidItemByKeywordId(id)
                                .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                        .withTargetTrafficVolume(50)
                                        .withSetMaximum(false)
                                        .withIncreasePercent(50))
                        ), false);

        assertThat(result)
                .describedAs("setAuto должен пройти валидацию")
                .is(matchedBy(isSuccessful()));
        assertThat(result.getValidationResult())
                .describedAs("Ошибка первого элемента должна соответствовать ожидаемой")
                .is(matchedBy(hasDefectDefinitionWith(validationError(path(index(0)), adGroupTypeNotSupported()))));
    }

    @Test
    public void setAutoBids_success_forSearchByTrafficVolumeWithMaxTrue() {
        Long id = keywordInfo.getId();

        MassResult<SetAutoBidItem> result =
                testingBidService.setAutoBids(clientId, clientUid,
                        singletonList(createEmptySetAutoBidItemByKeywordId(id)
                                .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                        .withTargetTrafficVolume(50)
                                        .withSetMaximum(true)
                                        .withIncreasePercent(50))
                        ), false);

        assertCorrectResultForSingleKeywordIsWrittenToDb(id, result, "150" /*100 + 50(50%)*/, null);
    }

    @Test
    public void setAutoBids_ContentPromotionKeyword_ForSearchByTrafficVolumeWithMaxTrue_NothingChanged() {
        Long id = contentPromotionKeywordInfo.getKeywordId();

        MassResult<SetAutoBidItem> result =
                testingBidService.setAutoBids(clientId, clientUid,
                        singletonList(createEmptySetAutoBidItemByKeywordId(id)
                                .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                        .withTargetTrafficVolume(50)
                                        .withSetMaximum(true)
                                        .withIncreasePercent(50))
                        ), false);

        assertThat(result)
                .describedAs("setAuto должен пройти валидацию")
                .is(matchedBy(isSuccessful()));
        assertThat(result.getValidationResult())
                .describedAs("Ошибка первого элемента должна соответствовать ожидаемой")
                .is(matchedBy(hasDefectDefinitionWith(validationError(path(index(0)), adGroupTypeNotSupported()))));
    }

    @Test
    public void setAutoBids_ContentPromotionAndTextKeywords_ForSearchByTrafficVolumeWithMaxTrue_TextBidChanged() {
        MassResult<SetAutoBidItem> result =
                testingBidService.setAutoBids(clientId, clientUid,
                        asList(createEmptySetAutoBidItemByKeywordId(keywordInfo.getId())
                                        .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                        .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                                .withTargetTrafficVolume(50)
                                                .withSetMaximum(true)
                                                .withIncreasePercent(50)),
                                createEmptySetAutoBidItemByKeywordId(contentPromotionKeywordInfo.getKeywordId())
                                        .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                        .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                                .withTargetTrafficVolume(75)
                                                .withSetMaximum(true)
                                                .withIncreasePercent(75))
                        ), false);

        assertCorrectResultForSingleKeywordIsWrittenToDb(keywordInfo.getId(), result, "150", null);
        assertThat(result)
                .describedAs("setAuto должен пройти валидацию")
                .is(matchedBy(isSuccessful()));
        assertThat(result.getValidationResult())
                .describedAs("Ошибка первого элемента должна соответствовать ожидаемой")
                .is(matchedBy(hasDefectDefinitionWith(validationError(path(index(1)), adGroupTypeNotSupported()))));
    }

    @Test
    public void setAutoBids_KeywordBsRarelyLoaded_ItemsValidationFailure() {
        CampaignInfo campaignInfo = keywordInfo.getAdGroupInfo().getCampaignInfo();
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);
        KeywordInfo bsRarelyLoadedKeywordInfo = steps.keywordSteps().createKeyword(adGroupInfo);
        testAdGroupRepository.updateAdGroupBsRarelyLoaded(bsRarelyLoadedKeywordInfo.getShard(),
                bsRarelyLoadedKeywordInfo.getAdGroupId(), true);

        Long id = bsRarelyLoadedKeywordInfo.getId();

        MassResult<SetAutoBidItem> result =
                testingBidService.setAutoBids(clientId, clientUid,
                        singletonList(createEmptySetAutoBidItemByKeywordId(id)
                                .withScope(EnumSet.of(BidTargetType.CONTEXT))
                                .withNetworkByCoverage(new SetAutoNetworkByCoverage()
                                        .withContextCoverage(3)
                                        .withIncreasePercent(50)
                                        .withMaxBid(BigDecimal.TEN))
                        ), false);

        assertThat(result)
                .describedAs("setAuto должен пройти валидацию")
                .is(matchedBy(isSuccessful()));
        assertThat(result.getValidationResult())
                .describedAs("Ошибка первого элемента должна соответствовать ожидаемой")
                .is(matchedBy(hasDefectDefinitionWith(validationError(path(index(0)),
                        bidChangeNotAllowedForBsRarelyLoadedAdGroup()))));
    }

    @Test
    public void setAutoBids_WithDuplicateAdGroupIds_ItemsValidationFailure() {
        Long adGroupId = keywordInfo.getAdGroupId();

        MassResult<SetAutoBidItem> result =
                testingBidService.setAutoBids(clientId, clientUid,
                        asList(createEmptySetAutoBidItemByAdGroupId(adGroupId)
                                        .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                        .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                                .withTargetTrafficVolume(50)
                                                .withSetMaximum(true)
                                                .withIncreasePercent(50)),
                                createEmptySetAutoBidItemByAdGroupId(adGroupId)
                                        .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                        .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                                .withTargetTrafficVolume(75)
                                                .withSetMaximum(true)
                                                .withIncreasePercent(75))
                        ), false);

        assertThat(result)
                .describedAs("setAuto должен пройти валидацию")
                .is(matchedBy(isSuccessful()));
        assertThat(result.getValidationResult())
                .describedAs("Ошибка первого элемента должна соответствовать ожидаемой")
                .is(matchedBy(hasDefectDefinitionWith(validationError(path(index(0)), duplicatedObject()))));
        assertThat(result.getValidationResult())
                .describedAs("Ошибка второго элемента должна соответствовать ожидаемой")
                .is(matchedBy(hasDefectDefinitionWith(validationError(path(index(1)), duplicatedObject()))));
    }

    @Test
    public void setAutoBids_WithDuplicateKeywordIds_ItemsValidationFailure() {
        Long keywordId = keywordInfo.getId();

        MassResult<SetAutoBidItem> result =
                testingBidService.setAutoBids(clientId, clientUid,
                        asList(createEmptySetAutoBidItemByKeywordId(keywordId)
                                        .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                        .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                                .withTargetTrafficVolume(50)
                                                .withSetMaximum(true)
                                                .withIncreasePercent(50)),
                                createEmptySetAutoBidItemByKeywordId(keywordId)
                                        .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                        .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                                .withTargetTrafficVolume(75)
                                                .withSetMaximum(true)
                                                .withIncreasePercent(75))
                        ), false);

        assertThat(result)
                .describedAs("setAuto должен пройти валидацию")
                .is(matchedBy(isSuccessful()));
        assertThat(result.getValidationResult())
                .describedAs("Ошибка первого элемента должна соответствовать ожидаемой")
                .is(matchedBy(hasDefectDefinitionWith(validationError(path(index(0)), duplicatedObject()))));
        assertThat(result.getValidationResult())
                .describedAs("Ошибка второго элемента должна соответствовать ожидаемой")
                .is(matchedBy(hasDefectDefinitionWith(validationError(path(index(1)), duplicatedObject()))));
    }

    @Test
    public void setAutoBids_WithDuplicateCampaignIds_ItemsValidationFailure() {
        Long campaignId = keywordInfo.getCampaignId();

        MassResult<SetAutoBidItem> result =
                testingBidService.setAutoBids(clientId, clientUid,
                        asList(createEmptySetAutoBidItemByCampaignId(campaignId)
                                        .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                        .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                                .withTargetTrafficVolume(50)
                                                .withSetMaximum(true)
                                                .withIncreasePercent(50)),
                                createEmptySetAutoBidItemByCampaignId(campaignId)
                                        .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                        .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                                .withTargetTrafficVolume(75)
                                                .withSetMaximum(true)
                                                .withIncreasePercent(75))
                        ), false);

        assertThat(result)
                .describedAs("setAuto должен пройти валидацию")
                .is(matchedBy(isSuccessful()));
        assertThat(result.getValidationResult())
                .describedAs("Ошибка первого элемента должна соответствовать ожидаемой")
                .is(matchedBy(hasDefectDefinitionWith(validationError(path(index(0)), duplicatedObject()))));
        assertThat(result.getValidationResult())
                .describedAs("Ошибка второго элемента должна соответствовать ожидаемой")
                .is(matchedBy(hasDefectDefinitionWith(validationError(path(index(1)), duplicatedObject()))));
    }

    @Test
    public void setAutoBids_WithNullItemAndItemsWithDuplicateCampaignIds_NoExceptionAndValidationFailed() {
        Long campaignId = keywordInfo.getCampaignId();

        MassResult<SetAutoBidItem> result =
                testingBidService.setAutoBids(clientId, clientUid,
                        asList(null,
                                createEmptySetAutoBidItemByCampaignId(campaignId)
                                        .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                        .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                                .withTargetTrafficVolume(50)
                                                .withSetMaximum(true)
                                                .withIncreasePercent(50)),
                                createEmptySetAutoBidItemByCampaignId(campaignId)
                                        .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                        .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                                .withTargetTrafficVolume(75)
                                                .withSetMaximum(true)
                                                .withIncreasePercent(75))
                        ), false);

        assertThat(result)
                .describedAs("setAuto не должен пройти валидацию")
                .is(matchedBy(isFailed()));
    }

    @Test
    public void setAutoBids_ItemsWithDuplicateCampaignIdsAndItemWithAdGroupId_ValidationFailed() {
        Long campaignId = keywordInfo.getCampaignId();
        Long adGroupId = keywordInfo.getAdGroupId();

        MassResult<SetAutoBidItem> result =
                testingBidService.setAutoBids(clientId, clientUid,
                        asList(createEmptySetAutoBidItemByCampaignId(campaignId)
                                        .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                        .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                                .withTargetTrafficVolume(50)
                                                .withSetMaximum(true)
                                                .withIncreasePercent(50)),
                                createEmptySetAutoBidItemByCampaignId(campaignId)
                                        .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                        .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                                .withTargetTrafficVolume(75)
                                                .withSetMaximum(true)
                                                .withIncreasePercent(75)),
                                createEmptySetAutoBidItemByAdGroupId(adGroupId)
                                        .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                        .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                                .withTargetTrafficVolume(50)
                                                .withSetMaximum(true)
                                                .withIncreasePercent(50))
                        ), false);

        assertThat(result)
                .describedAs("setAuto не должен пройти валидацию")
                .is(matchedBy(isFailed()));
    }

    @Test
    public void setAutoBids_ItemsWithDifferentCampaignIds_ValidationSucceeded() {
        Long campaignId = keywordInfo.getCampaignId();
        Long anotherCampaignId = anotherKeywordInfo.getCampaignId();

        MassResult<SetAutoBidItem> result =
                testingBidService.setAutoBids(clientId, clientUid,
                        asList(createEmptySetAutoBidItemByCampaignId(campaignId)
                                        .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                        .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                                .withTargetTrafficVolume(50)
                                                .withSetMaximum(true)
                                                .withIncreasePercent(50)),
                                createEmptySetAutoBidItemByCampaignId(anotherCampaignId)
                                        .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                        .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                                .withTargetTrafficVolume(50)
                                                .withSetMaximum(true)
                                                .withIncreasePercent(50))
                        ), false);

        assertThat(result)
                .describedAs("setAuto должен пройти валидацию")
                .is(matchedBy(isFullySuccessful()));
    }

    @Test
    public void setAutoBids_ItemsWithDifferentAdGroupIds_ValidationSucceeded() {
        Long adGroupId = keywordInfo.getAdGroupId();
        Long anotherAdGroupId = anotherKeywordInfo.getAdGroupId();

        MassResult<SetAutoBidItem> result =
                testingBidService.setAutoBids(clientId, clientUid,
                        asList(createEmptySetAutoBidItemByAdGroupId(adGroupId)
                                        .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                        .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                                .withTargetTrafficVolume(50)
                                                .withSetMaximum(true)
                                                .withIncreasePercent(50)),
                                createEmptySetAutoBidItemByAdGroupId(anotherAdGroupId)
                                        .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                        .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                                .withTargetTrafficVolume(50)
                                                .withSetMaximum(true)
                                                .withIncreasePercent(50))
                        ), false);


        assertThat(result)
                .describedAs("setAuto должен пройти валидацию")
                .is(matchedBy(isFullySuccessful()));
    }

    @Test
    public void setAutoBids_ItemsWithDifferentKeywordIds_ValidationSucceeded() {
        Long keywordId = keywordInfo.getId();
        Long anotherKeywordId = anotherKeywordInfo.getId();

        MassResult<SetAutoBidItem> result =
                testingBidService.setAutoBids(clientId, clientUid,
                        asList(createEmptySetAutoBidItemByKeywordId(keywordId)
                                        .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                        .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                                .withTargetTrafficVolume(50)
                                                .withSetMaximum(true)
                                                .withIncreasePercent(50)),
                                createEmptySetAutoBidItemByKeywordId(anotherKeywordId)
                                        .withScope(EnumSet.of(BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                        .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                                .withTargetTrafficVolume(50)
                                                .withSetMaximum(true)
                                                .withIncreasePercent(50))
                        ), false);

        assertThat(result)
                .describedAs("setAuto должен пройти валидацию")
                .is(matchedBy(isFullySuccessful()));
    }

    @Test
    public void setAutoBids_KeywordBidsService_WithSeveralRules_ItemValidationFailure() {
        Long id = keywordInfo.getId();

        MassResult<SetAutoBidItem> result =
                testingBidService.setAutoBids(clientId, clientUid,
                        singletonList(createEmptySetAutoBidItemByKeywordId(id)
                                .withScope(EnumSet.of(BidTargetType.CONTEXT, BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                .withNetworkByCoverage(new SetAutoNetworkByCoverage()
                                        .withContextCoverage(3)
                                        .withIncreasePercent(50)
                                        .withMaxBid(BigDecimal.TEN))
                                .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                        .withTargetTrafficVolume(10)
                                        .withIncreasePercent(50)
                                        .withMaxBid(BigDecimal.TEN))
                        ), true);

        assertThat(result)
                .describedAs("setAuto должен пройти валидацию")
                .is(matchedBy(isSuccessful()));
        assertThat(result.getValidationResult())
                .describedAs("Ошибка элемента должна соответствовать ожидаемой")
                .is(matchedBy(hasDefectDefinitionWith(
                        validationError(path(index(0), field("scope")), maxCollectionSize(1)))));
    }

    @Test
    public void setAutoBids_BidsService_WithSeveralRules_ValidationSucceeded() {
        Long id = keywordInfo.getId();

        MassResult<SetAutoBidItem> result =
                testingBidService.setAutoBids(clientId, clientUid,
                        singletonList(createEmptySetAutoBidItemByKeywordId(id)
                                .withScope(EnumSet.of(BidTargetType.CONTEXT, BidTargetType.SEARCH_BY_TRAFFIC_VOLUME))
                                .withNetworkByCoverage(new SetAutoNetworkByCoverage()
                                        .withContextCoverage(3)
                                        .withIncreasePercent(50)
                                        .withMaxBid(BigDecimal.TEN))
                                .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume()
                                        .withTargetTrafficVolume(10)
                                        .withIncreasePercent(50)
                                        .withMaxBid(BigDecimal.TEN))
                        ), false);

        assertThat(result)
                .describedAs("setAuto должен пройти валидацию без ошибок элементов")
                .is(matchedBy(isFullySuccessful()));
    }

    private SetAutoBidItem createEmptySetAutoBidItemByKeywordId(Long id) {
        return new SetAutoBidItem()
                .withId(id)
                .withSearchByPosition(new SetAutoSearchByPosition())
                .withNetworkByCoverage(new SetAutoNetworkByCoverage())
                .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume());
    }

    private SetAutoBidItem createEmptySetAutoBidItemByAdGroupId(Long adGroupId) {
        return new SetAutoBidItem()
                .withAdGroupId(adGroupId)
                .withSearchByPosition(new SetAutoSearchByPosition())
                .withNetworkByCoverage(new SetAutoNetworkByCoverage())
                .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume());
    }

    private SetAutoBidItem createEmptySetAutoBidItemByCampaignId(Long campaignId) {
        return new SetAutoBidItem()
                .withCampaignId(campaignId)
                .withSearchByPosition(new SetAutoSearchByPosition())
                .withNetworkByCoverage(new SetAutoNetworkByCoverage())
                .withSearchByTrafficVolume(new SetAutoSearchByTrafficVolume());
    }

    private void assertCorrectResultForSingleKeywordIsWrittenToDb(Long keywordId, MassResult<SetAutoBidItem> result,
                                                                  String expectedPrice, String expectedPriceContext) {

        assertThat(result).is(matchedBy(isSuccessful()));

        //проверяем, что правильный результат записался в BIDS
        List<Keyword> keywords =
                keywordService.getKeywords(clientId, singletonList(keywordId));
        Assert.assertThat(keywords, hasSize(1));

        //проверяем, что из BIDS_BASE не получаем keywords
        List<Bid> resultFromBidsBase =
                bidRepository.getRelevanceMatchByIds(keywordInfo.getShard(), singletonList(keywordId));
        Assert.assertThat(resultFromBidsBase, empty());

        if (expectedPrice != null) {
            assertThat(keywords.get(0).getPrice())
                    .isEqualByComparingTo(expectedPrice);
        }
        if (expectedPriceContext != null) {
            assertThat(keywords.get(0).getPriceContext())
                    .isEqualByComparingTo(expectedPriceContext);
        }
    }

    /**
     * В тестах на setAuto необходимо подменять ответ Торгов и Показометра.
     * Делать это на уровне клиентов неудобно, поскольку на уровне сервисов данные сильно меняются.
     * Для того, чтобы подменить ответ, делаем mock'и для сервисов.
     * Чтобы mock'и не приходилось вручную передвать в тестируемый {@link BidService}, создаём конфигурацию,
     * в которой {@code bidService} будет подменяться на тот, что с mock'ами.
     */
    @Configuration
    @ComponentScan(basePackageClasses = BidService.class,
            // Чтобы не подтягивались конфигурации с соседних тестов
            excludeFilters = {
                    @ComponentScan.Filter(value = Configuration.class, type = FilterType.ANNOTATION)
            })
    static class OverridingConfiguration {

        private static Money money(double value) {
            return Money.valueOf(value, CURRENCY_CODE);
        }

        private static Position buildPosition(int price, int amnesty) {
            return new Position(money(amnesty),
                    money(price));
        }

        private static TrafaretBidItem bidItem(long ctrCorrection, double money) {
            return new TrafaretBidItem()
                    .withPositionCtrCorrection(ctrCorrection)
                    .withBid(money(money))
                    .withPrice(money(money));
        }

        /**
         * Подменить {@link AdGroupsShowsForecastService} на mock чуть проще,
         * чем настраивать ответ {@link ru.yandex.direct.advq.AdvqClient}
         */
        @Bean
        @Primary
        public AdGroupsShowsForecastService adGroupsShowsForecastService() {
            AdGroupsShowsForecastService mock = mock(AdGroupsShowsForecastService.class);
            doNothing().when(mock).updateShowsForecastIfNeeded(any(), anyList(), any());
            return mock;
        }

        @Bean
        @Primary
        public PokazometerService pokazometerService() {
            PokazometerService mock = mock(PokazometerService.class);
            Answer<Object> stubAnswer = invocation -> {
                List<AdGroupForAuction> adGroupsForAuction = invocation.getArgument(0);
                return adGroupsForAuction
                        .stream()
                        .map(AdGroupForAuction::getKeywords)
                        .flatMap(Collection::stream)
                        .map(Keyword::getId)
                        .map(id -> new KeywordBidPokazometerData(id, ImmutableMap.of(
                                PhraseResponse.Coverage.LOW, Money.valueOf(20, CURRENCY_CODE),
                                PhraseResponse.Coverage.MEDIUM, Money.valueOf(50, CURRENCY_CODE),
                                PhraseResponse.Coverage.HIGH, Money.valueOf(100, CURRENCY_CODE)
                        )))
                        .collect(Collectors.toList());
            };
            when(mock.getPokazometerResults(anyList()))
                    .thenAnswer(stubAnswer);
            when(mock.safeGetPokazometerResults(anyList()))
                    .thenAnswer(stubAnswer);
            return mock;
        }

        @Bean
        @Primary
        public BsAuctionService bsAuctionService() {
            BsAuctionService mock = mock(BsAuctionService.class);
            when(mock.getBsResults(any(), anyList()))
                    .thenAnswer(invocation -> {
                        List<AdGroupForAuction> keywords = invocation.getArgument(1);
                        return keywords.stream()
                                .map(AdGroupForAuction::getKeywords)
                                .flatMap(Collection::stream)
                                .map(keyword -> new KeywordBidBsAuctionData()
                                        .withKeyword(keyword)
                                        .withPremium(new Block(asList(
                                                buildPosition(140, 140),
                                                buildPosition(130, 130),
                                                buildPosition(120, 120),
                                                buildPosition(110, 110)
                                        )))
                                        .withGuarantee(new Block(asList(
                                                buildPosition(40, 40),
                                                buildPosition(30, 30),
                                                buildPosition(20, 20),
                                                buildPosition(10, 10)
                                        ))))
                                .collect(Collectors.toList());
                    });
            when(mock.getBsTrafaretResults(any(), anyList()))
                    .thenAnswer(invocation -> {
                        List<AdGroupForAuction> keywords = invocation.getArgument(1);
                        return keywords.stream()
                                .map(AdGroupForAuction::getKeywords)
                                .flatMap(Collection::stream)
                                .map(keyword -> new KeywordTrafaretData()
                                        .withKeyword(keyword)
                                        .withBidItems(asList(
                                                bidItem(100_0000L, 100),
                                                bidItem(90_0000L, 90),
                                                bidItem(80_0000L, 80),
                                                bidItem(70_0000L, 70),
                                                bidItem(10_0000L, 10),
                                                bidItem(7_5000L, 7.5),
                                                bidItem(6_2000L, 6.2),
                                                bidItem(5_5000L, 5.5)
                                        )))
                                .collect(Collectors.toList());
                    });
            return mock;
        }
    }
}
