package ru.yandex.direct.core.entity.adgroup.service.complex.text.add;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.bsauction.BsTrafaretClient;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusShowsForecast;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupTestCommons;
import ru.yandex.direct.core.entity.adgroup.service.complex.text.AddComplexTextAdGroupValidationService;
import ru.yandex.direct.core.entity.adgroup.service.complex.text.ComplexTextAdGroupAddOperation;
import ru.yandex.direct.core.entity.banner.container.ComplexBanner;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory;
import ru.yandex.direct.core.entity.banner.service.DatabaseMode;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.bidmodifiers.service.ComplexBidModifierService;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.entity.keyword.service.KeywordOperationFactory;
import ru.yandex.direct.core.entity.offerretargeting.service.OfferRetargetingService;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchService;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionOperationFactory;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingService;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionAutoPriceParams;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionFixedAutoPrices;
import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.entity.sitelink.repository.SitelinkSetRepository;
import ru.yandex.direct.core.entity.sitelink.service.SitelinkSetService;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.entity.vcard.repository.VcardRepository;
import ru.yandex.direct.core.entity.vcard.service.VcardService;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.mock.BsTrafaretClientMockUtils;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currencies;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.COMPLEX_BANNERS;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.emptyAdGroupForAdd;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.emptyAdGroupWithModelForAdd;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.fullAdGroupForAdd;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.fullTextAdGroup;
import static ru.yandex.direct.integrations.configuration.IntegrationsConfiguration.BS_TRAFARET_AUCTION_CLIENT;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class ComplexTextAddTestBase {
    protected static final Currency CURRENCY = Currencies.getCurrency(CurrencyCode.RUB);
    private static final CompareStrategy AD_GROUP_COMPARE_STRATEGY_WITH_STATUSES = onlyExpectedFields()
            .forFields(newPath("statusBsSynced")).useMatcher(is(StatusBsSynced.NO))
            .forFields(newPath("statusShowsForecast")).useMatcher(is(StatusShowsForecast.NEW));
    protected static final BigDecimal FIXED_AUTO_PRICE = BigDecimal.valueOf(710L).setScale(2, RoundingMode.UNNECESSARY);

    @Autowired
    protected CampaignSteps campaignSteps;
    @Autowired
    protected Steps steps;
    protected UserInfo operator;
    protected CampaignInfo campaign;
    @Autowired
    private ClientService clientService;
    @Autowired
    private KeywordOperationFactory keywordOperationFactory;
    @Autowired
    private AdGroupService adGroupService;
    @Autowired
    private VcardService vcardService;
    @Autowired
    private SitelinkSetService sitelinkSetService;
    @Autowired
    private RelevanceMatchService relevanceMatchService;
    @Autowired
    private OfferRetargetingService offerRetargetingService;
    @Autowired
    protected RetargetingService retargetingService;
    @Autowired
    private BidModifierService bidModifierService;
    @Autowired
    private ComplexBidModifierService complexBidModifierService;
    @Autowired
    private AddComplexTextAdGroupValidationService addValidationService;
    @Autowired
    private GeoTreeFactory geoTreeFactory;
    @Autowired
    protected CampaignRepository campaignRepository;
    @Autowired
    protected RetargetingConditionOperationFactory retargetingConditionOperationFactory;
    @Autowired
    protected AdGroupRepository adGroupRepository;
    @Autowired
    protected BannerTypedRepository bannerTypedRepository;
    @Autowired
    private BannersAddOperationFactory bannersAddOperationFactory;
    @Autowired
    protected BannerRelationsRepository bannerRelationsRepository;
    @Autowired
    protected VcardRepository vcardRepository;
    @Autowired
    protected SitelinkSetRepository sitelinkSetRepository;
    @Autowired
    protected KeywordRepository keywordRepository;
    @Autowired
    private RelevanceMatchRepository relevanceMatchRepository;
    @Autowired
    @Qualifier(BS_TRAFARET_AUCTION_CLIENT)
    private BsTrafaretClient bsTrafaretClient;
    private GeoTree geoTree;

    @Autowired
    protected ComplexAdGroupTestCommons testCommons;

    protected Long campaignId;

    protected RetConditionInfo retConditionInfo;
    protected Long retConditionId;

    @Before
    public void before() {
        BsTrafaretClientMockUtils.setDefaultMockOnBsTrafaretClient(bsTrafaretClient);

        campaign = campaignSteps.createDefaultCampaign();
        campaignId = campaign.getCampaignId();

        retConditionInfo = steps.retConditionSteps().createDefaultRetCondition(campaign.getClientInfo());
        retConditionId = retConditionInfo.getRetConditionId();

        geoTree = geoTreeFactory.getGlobalGeoTree();
        steps.trustedRedirectSteps().addValidCounters();
    }

    protected void addAndCheckComplexAdGroups(ComplexTextAdGroup... complexAdGroups) {
        addAndCheckComplexAdGroups(asList(complexAdGroups), false);
    }

    protected void addAndCheckComplexAdGroups(List<ComplexTextAdGroup> complexAdGroups) {
        addAndCheckComplexAdGroups(complexAdGroups, false);
    }

    protected void addWithAutoPricesAndCheckComplexAdGroups(List<ComplexTextAdGroup> complexAdGroups) {
        addAndCheckComplexAdGroups(complexAdGroups, true);
    }

    private void addAndCheckComplexAdGroups(List<ComplexTextAdGroup> complexAdGroups, boolean autoPrices) {
        ComplexTextAdGroupAddOperation addOperation = createOperation(complexAdGroups, true, autoPrices);
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        complexAdGroups.forEach(this::checkComplexAdGroup);
    }

    /**
     * Проверяет, что группа корректно добавлена и к ней привязано корректное количество баннеров, затем каждый баннер
     * проевряется при помощи {@link this#checkComplexBanner(ComplexBanner)}
     */
    protected void checkComplexAdGroup(ComplexTextAdGroup complexAdGroup) {
        AdGroup expectedAdGroup = complexAdGroup.getAdGroup();
        List<AdGroup> adGroups =
                adGroupRepository.getAdGroups(campaign.getShard(), singletonList(expectedAdGroup.getId()));
        CompareStrategy compareStrategy = isEmpty(complexAdGroup.getKeywords()) ?
                onlyExpectedFields() : AD_GROUP_COMPARE_STRATEGY_WITH_STATUSES;
        assertThat("группа успешно добавлена", adGroups,
                contains(beanDiffer(expectedAdGroup).useCompareStrategy(compareStrategy)));

        Map<Long, Collection<Long>> bannerIdsByAdGroupId =
                bannerRelationsRepository
                        .getAdGroupIdToBannerIds(campaign.getShard(), singletonList(expectedAdGroup.getId()))
                        .asMap();
        if (isEmpty(complexAdGroup.getComplexBanners())) {
            assertThat("в группе не должно быть баннеров", bannerIdsByAdGroupId.keySet(), empty());
        } else {
            assertThat("должны найтись баннеры для одной группы", bannerIdsByAdGroupId.keySet(), hasSize(1));
            List<Long> bannerIds =
                    mapList(complexAdGroup.getComplexBanners(), complexBanner -> complexBanner.getBanner().getId());
            assertThat("в группу добавились правильные баннеры", bannerIdsByAdGroupId.get(expectedAdGroup.getId()),
                    containsInAnyOrder(bannerIds.toArray()));
            complexAdGroup.getComplexBanners().forEach(this::checkComplexBanner);
        }
        testCommons
                .checkKeywords(complexAdGroup.getKeywords(), complexAdGroup.getAdGroup().getId(), campaign.getShard());
        testCommons.checkRelevanceMatches(complexAdGroup.getRelevanceMatches(), complexAdGroup.getAdGroup().getId(),
                campaign.getClientId(), campaign.getShard());
        testCommons.checkOfferRetargetings(complexAdGroup.getOfferRetargetings(), complexAdGroup.getAdGroup().getId(),
                campaign.getClientId(), campaign.getShard());
        testCommons.checkRetargetings(complexAdGroup.getTargetInterests(), complexAdGroup.getAdGroup().getId(),
                campaign.getClientId(), campaign.getShard());
    }

    /**
     * Проверяет, что баннер корректно добавился и что к нему корректно привязалась визитка и набор сайтлинков
     * (если они должны были добавиться), а так же, что привязалась правильная визитка и набор сайтлинков.
     */
    private void checkComplexBanner(ComplexBanner complexBanner) {
        Banner expectedBanner = complexBanner.getBanner();
        Vcard expectedVcard = complexBanner.getVcard();
        SitelinkSet expectedSitelinkSet = getExpectedSitelinkSet(complexBanner);

        List<Banner> banners = bannerTypedRepository
                .getTyped(campaign.getShard(), singletonList(expectedBanner.getId()));
        CompareStrategy bannerCompareStrategy = onlyExpectedFields()
                .forFields(newPath("lastChange")).useMatcher(notNullValue());
        assertThat("баннер успешно добавлен", banners,
                contains(beanDiffer(expectedBanner).useCompareStrategy(bannerCompareStrategy)));

        if (complexBanner.getVcard() != null) {
            List<Vcard> vcards = filterList(
                    vcardRepository
                            .getVcards(campaign.getShard(), campaign.getUid(), singletonList(expectedVcard.getId())),
                    vcard -> vcard.getId().equals(expectedVcard.getId()));
            CompareStrategy vcardCompareStrategy = onlyExpectedFields()
                    .forFields(newPath("lastChange")).useMatcher(notNullValue())
                    .forFields(newPath("lastDissociation")).useMatcher(notNullValue());
            assertThat("визитка успешно добавлена", vcards,
                    contains(beanDiffer(expectedVcard).useCompareStrategy(vcardCompareStrategy)));
        }

        if (expectedSitelinkSet != null) {
            List<SitelinkSet> sitelinkSets =
                    sitelinkSetRepository.get(campaign.getShard(), singletonList(expectedSitelinkSet.getId()));
            assertThat("группа сайтлинков успешно добавлена", sitelinkSets,
                    contains(beanDiffer(expectedSitelinkSet).useCompareStrategy(onlyExpectedFields())));
        }
    }

    private SitelinkSet getExpectedSitelinkSet(ComplexBanner complexBanner) {
        if (complexBanner.getSitelinkSet() == null) {
            return null;
        }

        SitelinkSet expectedSitelinkSet = new SitelinkSet()
                .withId(complexBanner.getSitelinkSet().getId())
                .withClientId(campaign.getClientId().asLong())
                .withSitelinks(filterList(complexBanner.getSitelinkSet().getSitelinks(), Objects::nonNull));

        List<Sitelink> sitelinks = expectedSitelinkSet.getSitelinks();
        for (int sitelinkIndex = 0; sitelinkIndex < sitelinks.size(); ++sitelinkIndex) {
            sitelinks.get(sitelinkIndex).setOrderNum((long) sitelinkIndex);
        }

        return expectedSitelinkSet;
    }

    protected ComplexTextAdGroupAddOperation createOperation(List<ComplexTextAdGroup> complexAdGroups) {
        return createOperation(complexAdGroups, true, false);
    }

    protected ComplexTextAdGroupAddOperation createOperation(List<ComplexTextAdGroup> complexAdGroups,
                                                             boolean saveDraft) {
        return createOperation(complexAdGroups, saveDraft, false);
    }

    private ComplexTextAdGroupAddOperation createOperation(List<ComplexTextAdGroup> complexAdGroups,
                                                           boolean saveDraft, boolean autoPrices) {
        ShowConditionAutoPriceParams showConditionAutoPriceParams = null;
        if (autoPrices) {
            showConditionAutoPriceParams = new ShowConditionAutoPriceParams(
                    ShowConditionFixedAutoPrices.ofGlobalFixedPrice(FIXED_AUTO_PRICE),
                    keywords -> emptyMap()
            );
        }
        return new ComplexTextAdGroupAddOperation(saveDraft, clientService, adGroupService,
                keywordOperationFactory, bannersAddOperationFactory, vcardService, sitelinkSetService,
                relevanceMatchService, offerRetargetingService, retargetingService, bidModifierService, complexBidModifierService,
                addValidationService, campaignRepository, retargetingConditionOperationFactory, complexAdGroups,
                geoTree,
                autoPrices, showConditionAutoPriceParams,
                operator == null ? campaign.getUid() : operator.getUid(), campaign.getClientId(),
                campaign.getClientInfo().getUid(), campaign.getShard(), DatabaseMode.ONLY_MYSQL);
    }

    // test data

    protected ComplexTextAdGroup emptyTextAdGroup() {
        return emptyAdGroupForAdd(campaignId);
    }

    protected ComplexTextAdGroup fullAdGroup() {
        return fullAdGroupForAdd(campaignId, retConditionId);
    }

    protected ComplexTextAdGroup fullAdGroup(Long campaignId) {
        return fullAdGroupForAdd(campaignId, retConditionId);
    }

    protected ComplexTextAdGroup fullAdGroup(List<ComplexBanner> complexBanners) {
        return fullTextAdGroup(null, campaignId, retConditionId, complexBanners);
    }

    protected ComplexTextAdGroup adGroupWithBanners(List<ComplexBanner> complexBanners) {
        return emptyAdGroupWithModelForAdd(campaignId, complexBanners, COMPLEX_BANNERS);
    }
}
