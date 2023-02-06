package ru.yandex.direct.web.entity.adgroup.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import ru.yandex.direct.bsauction.BsRequest;
import ru.yandex.direct.bsauction.BsRequestPhrase;
import ru.yandex.direct.bsauction.BsResponse;
import ru.yandex.direct.bsauction.FullBsTrafaretResponsePhrase;
import ru.yandex.direct.core.entity.adgroup.container.AdGroupsSelectionCriteria;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PageGroupTagEnum;
import ru.yandex.direct.core.entity.adgroup.model.TargetTagEnum;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLanding;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktop;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargeting;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeather;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.feature.model.ClientFeature;
import ru.yandex.direct.core.entity.feature.model.Feature;
import ru.yandex.direct.core.entity.feature.model.FeatureState;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.repository.SitelinkSetRepository;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLanding;
import ru.yandex.direct.core.entity.turbolanding.repository.TurboLandingRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.security.DirectAuthentication;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.web.configuration.mock.auth.DirectWebAuthenticationSourceMock;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;
import ru.yandex.direct.web.entity.bidmodifier.model.WebDemographicsBidModifier;
import ru.yandex.direct.web.entity.bidmodifier.model.WebDesktopBidModifier;
import ru.yandex.direct.web.entity.bidmodifier.model.WebMobileBidModifier;
import ru.yandex.direct.web.entity.bidmodifier.model.WebRetargetingBidModifier;
import ru.yandex.direct.web.entity.bidmodifier.model.WebWeatherBidModifier;
import ru.yandex.direct.web.validation.model.ValidationResponse;
import ru.yandex.direct.web.validation.model.WebDefect;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static ru.yandex.direct.core.entity.bidmodifiers.Constants.ALL_TYPES;
import static ru.yandex.direct.core.testing.data.TestBsResponses.defaultTrafaretResponsePhrase;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.utils.JsonUtils.toJson;

public class AdGroupControllerTestBase {

    @Autowired
    protected Steps steps;

    @Autowired
    protected AdGroupRepository adGroupRepository;

    @Autowired
    protected KeywordRepository keywordRepository;

    @Autowired
    protected RelevanceMatchRepository relevanceMatchRepository;

    @Autowired
    protected RetargetingRepository retargetingRepository;

    @Autowired
    protected BidModifierRepository bidModifierRepository;

    @Autowired
    protected OldBannerRepository bannerRepository;

    @Autowired
    protected SitelinkSetRepository sitelinkSetRepository;

    @Autowired
    protected DirectWebAuthenticationSource authenticationSource;

    @Autowired
    protected CampaignRepository campaignRepository;

    @Autowired
    protected ClientService clientService;

    @Autowired
    private TurboLandingRepository turboLandingRepository;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    protected ClientInfo clientInfo;
    protected int shard;
    protected ClientId clientId;
    protected long retCondId;

    RetConditionInfo retConditionInfo;
    Currency clientCurrency;
    protected List<OldBannerTurboLanding> bannerTurboLandings = new ArrayList<>();

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        retConditionInfo = steps.retConditionSteps().createDefaultRetCondition(clientInfo);
        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();
        retCondId = retConditionInfo.getRetConditionId();
        clientCurrency = clientService.getWorkCurrency(clientId);
        setAuthData();

        List<TurboLanding> turboLandings = new ArrayList<>();
        turboLandings.add(steps.turboLandingSteps().defaultTurboLanding(clientInfo.getClientId())
                .withMetrikaCounters(
                        "[{\"id\": 123, \"goals\": [563]}, {\"id\": 456, \"goals\": [8], \"isUserCounter\": true}]"));
        turboLandings.add(steps.turboLandingSteps().defaultTurboLanding(clientInfo.getClientId())
                .withMetrikaCounters("[{\"id\": 777, \"goals\": [121]}]"));
        bannerTurboLandings.add(getBannerTurboLanding(turboLandings.get(0)));
        bannerTurboLandings.add(getBannerTurboLanding(turboLandings.get(1)));
        turboLandingRepository.add(shard, turboLandings);

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
    }

    protected static IdentityHashMap<BsRequest<BsRequestPhrase>, BsResponse<BsRequestPhrase, FullBsTrafaretResponsePhrase>> generateDefaultBsAuctionResponse(
            List<BsRequest<BsRequestPhrase>> requests) {
        return StreamEx.of(requests)
                .mapToEntry(r -> {
                    IdentityHashMap<BsRequestPhrase, FullBsTrafaretResponsePhrase> successResult =
                            StreamEx.of(r.getPhrases())
                                    .mapToEntry(phr -> defaultTrafaretResponsePhrase(CurrencyCode.RUB, phr))
                                    .toCustomMap(IdentityHashMap::new);
                    return BsResponse.success(successResult);
                })
                .toCustomMap(IdentityHashMap::new);
    }

    protected OldBannerTurboLanding getBannerTurboLanding(TurboLanding turboLanding) {
        return new OldBannerTurboLanding()
                .withId(turboLanding.getId())
                .withMetrikaCounters(turboLanding.getMetrikaCounters())
                .withStatusModerate(OldBannerTurboLandingStatusModerate.NEW);
    }

    protected void checkResponse(WebResponse response) {
        if (!response.isSuccessful()) {
            ValidationResponse validationResponse = (ValidationResponse) response;
            System.out.println("Ошибки в запросе:");
            System.out.println(toJson(validationResponse.validationResult().getErrors()));
            fail("запрос завершился неудачей");
        }
    }

    protected void checkMobileBidModifier(List<BidModifier> actualBidModifiers, Long addedAdGroupId,
                                          WebMobileBidModifier requestBidModifier) {
        BidModifierMobile bidModifierMobile =
                extractOnlyOneBidModifierOfType(actualBidModifiers, BidModifierMobile.class);

        assertThat("мобильная корректировка должна быть привязана к добавляемой группе",
                bidModifierMobile.getAdGroupId(),
                equalTo(addedAdGroupId));

        Integer expectedPercent = requestBidModifier.getPercent();
        assertThat("os_type мобильной корректировки отличается от ожидаемого",
                ifNotNull(bidModifierMobile.getMobileAdjustment().getOsType(), t -> t.name().toLowerCase()),
                equalTo(requestBidModifier.getOsType()));
        assertThat("данные мобильной корректировки отличаются от ожидаемых",
                bidModifierMobile.getMobileAdjustment().getPercent(),
                equalTo(expectedPercent));
    }

    protected void checkDesktopBidModifier(List<BidModifier> actualBidModifiers, Long addedAdGroupId,
                                           WebDesktopBidModifier requestBidModifier) {
        BidModifierDesktop bidModifierDesktop =
                extractOnlyOneBidModifierOfType(actualBidModifiers, BidModifierDesktop.class);

        assertThat("корректировка должна быть привязана к добавляемой группе",
                bidModifierDesktop.getAdGroupId(),
                equalTo(addedAdGroupId));
        Integer expectedPercent = requestBidModifier.getPercent();
        assertThat("данные корректировки отличаются от ожидаемых",
                bidModifierDesktop.getDesktopAdjustment().getPercent(),
                equalTo(expectedPercent));
    }

    protected void checkWeatherBidModifier(List<BidModifier> actualBidModifiers, Long addedAdGroupId,
                                           WebWeatherBidModifier requestBidModifier) {
        BidModifierWeather bidModifierWeather =
                extractOnlyOneBidModifierOfType(actualBidModifiers, BidModifierWeather.class);

        assertThat("корректировка должна быть привязана к добавляемой группе",
                bidModifierWeather.getAdGroupId(),
                equalTo(addedAdGroupId));
        Integer expectedPercent = requestBidModifier.getAdjustments().get(0).getPercent();
        assertThat("данные корректировки отличаются от ожидаемых",
                bidModifierWeather.getWeatherAdjustments().get(0).getPercent(),
                equalTo(expectedPercent));
    }

    protected void checkRetargetingBidModifier(List<BidModifier> actualBidModifiers, Long addedAdGroupId,
                                               WebRetargetingBidModifier requestBidModifier) {
        BidModifierRetargeting bidModifierRetargeting =
                extractOnlyOneBidModifierOfType(actualBidModifiers, BidModifierRetargeting.class);

        assertThat("корректировка по условию ретаргетинга должна быть привязана к добавляемой группе",
                bidModifierRetargeting.getAdGroupId(),
                equalTo(addedAdGroupId));
        Integer expectedPercent = requestBidModifier.getAdjustments().get(String.valueOf(retCondId)).getPercent();
        assertThat("данные корректировки по ретаргетингу отличаются от ожидаемых",
                bidModifierRetargeting.getRetargetingAdjustments().get(0).getPercent(),
                equalTo(expectedPercent));
    }

    protected void checkDemographyBidModifier(List<BidModifier> actualBidModifiers, Long addedAdGroupId,
                                              WebDemographicsBidModifier requestBidModifier) {
        BidModifierDemographics bidModifierDemographics =
                extractOnlyOneBidModifierOfType(actualBidModifiers, BidModifierDemographics.class);

        assertThat("корректировка по демографии должна быть привязана к добавляемой группе",
                bidModifierDemographics.getAdGroupId(),
                equalTo(addedAdGroupId));
        Integer expectedPercent = requestBidModifier.getAdjustments().get(0).getPercent();
        assertThat("данные корректировки по демографии отличаются от ожидаемых",
                bidModifierDemographics.getDemographicsAdjustments().get(0).getPercent(),
                equalTo(expectedPercent));
    }

    protected void checkErrorResponse(WebResponse response, String path, String code) {
        assertThat(response.isSuccessful(), is(false));
        ValidationResponse validationResponse = (ValidationResponse) response;
        List<WebDefect> defects = validationResponse.validationResult().getErrors();
        assertThat(defects, hasSize(1));
        assertThat(defects.get(0).getPath(), is(path));
        assertThat(defects.get(0).getCode(), is(code));
    }

    protected List<AdGroup> findAdGroups(long campaignId) {
        AdGroupsSelectionCriteria criteria = new AdGroupsSelectionCriteria()
                .withCampaignIds(campaignId);
        List<Long> adGroupIds = adGroupRepository
                .getAdGroupIdsBySelectionCriteria(shard, criteria, LimitOffset.maxLimited());
        return adGroupRepository.getAdGroups(shard, adGroupIds);
    }

    protected List<AdGroup> findAdGroup(long adGroupId) {
        return adGroupRepository.getAdGroups(shard, singleton(adGroupId));
    }

    protected List<Keyword> findKeywords(long campaignId) {
        return keywordRepository.getKeywordsByCampaignId(shard, campaignId);
    }

    protected List<Keyword> findKeywordsInAdGroup(long adGroupId) {
        return keywordRepository.getKeywordsByAdGroupId(shard, adGroupId);
    }

    protected List<RelevanceMatch> findRelevanceMatches(long adGroupId) {
        Map<Long, RelevanceMatch> relevanceMatchMap = relevanceMatchRepository
                .getRelevanceMatchesByAdGroupIds(shard, clientId, singleton(adGroupId));
        return new ArrayList<>(relevanceMatchMap.values());
    }

    protected List<Retargeting> findRetargetings(long adGroupId) {
        return retargetingRepository.getRetargetingsByAdGroups(shard, singletonList(adGroupId));
    }

    protected List<BidModifier> findBidModifiers(long campaignId) {
        return bidModifierRepository.getByCampaignIds(shard, ImmutableSet.of(campaignId),
                ALL_TYPES, ImmutableSet.of(BidModifierLevel.ADGROUP, BidModifierLevel.CAMPAIGN));
    }

    protected List<OldBanner> findOldBanners(long adGroupId) {
        return bannerRepository.getBannersByGroupIds(shard, singletonList(adGroupId));
    }

    protected List<Banner> findBanners(long adGroupId) {
        return bannerTypedRepository.getBannersByGroupIds(shard, singletonList(adGroupId));
    }

    protected List<OldBanner> findBannersByCampaignId(long campaignId) {
        return bannerRepository.getBannersByCampaignIds(shard, singletonList(campaignId));
    }

    protected ListMultimap<Long, Sitelink> findSitelinks() {
        List<Long> sitelinkSetIds = sitelinkSetRepository
                .getIdsByClientId(shard, clientId, LimitOffset.maxLimited());
        return sitelinkSetRepository.getSitelinksBySetIds(shard, sitelinkSetIds);
    }

    protected void setAuthData() {
        DirectWebAuthenticationSourceMock authSource =
                (DirectWebAuthenticationSourceMock) authenticationSource;
        authSource.withOperator(new User()
                .withUid(clientInfo.getUid()));
        authSource.withSubjectUser(new User()
                .withClientId(clientInfo.getClientId())
                .withUid(clientInfo.getUid()));

        User user = clientInfo.getChiefUserInfo().getUser();
        SecurityContextHolder.getContext()
                .setAuthentication(new DirectAuthentication(user, user));
    }

    protected <T extends BidModifier> T extractOnlyOneBidModifierOfType(List<BidModifier> bidModifiers,
                                                                        Class<T> typeOfBidModifier) {
        List<T> bidModifiersOfType = StreamEx.of(bidModifiers)
                .filter(bm -> typeOfBidModifier.isAssignableFrom(bm.getClass()))
                .map(typeOfBidModifier::cast)
                .toList();
        assertThat("ожидается наличие только 1 корректировки типа " + typeOfBidModifier,
                bidModifiersOfType, hasSize(1));
        return bidModifiersOfType.get(0);
    }

    protected Money moneyOf(double price) {
        return Money.valueOf(price, clientCurrency.getCode());
    }

    protected Money moneyOf(BigDecimal price) {
        return Money.valueOf(price, clientCurrency.getCode());
    }

    protected String getValueOfPageGroupOrTargetTagEnum(PageGroupTagEnum pageGroupTagEnum) {
        return JsonUtils.fromJson(JsonUtils.toJson(pageGroupTagEnum), String.class);
    }

    protected String getValueOfPageGroupOrTargetTagEnum(TargetTagEnum targetTagEnum) {
        return JsonUtils.fromJson(JsonUtils.toJson(targetTagEnum), String.class);
    }
}
