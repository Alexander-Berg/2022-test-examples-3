package ru.yandex.direct.core.entity.adgroup.service.complex.contentpromotion.add;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.container.ComplexContentPromotionAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusShowsForecast;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupAddOperationFactory;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupTestCommons;
import ru.yandex.direct.core.entity.adgroup.service.complex.contentpromotion.ComplexContentPromotionAdGroupAddOperation;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.entity.contentpromotion.repository.ContentPromotionRepository;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingService;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionAutoPriceParams;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionFixedAutoPrices;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners;
import ru.yandex.direct.core.testing.info.campaign.ContentPromotionCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.validation.defects.params.CurrencyAmountDefectParams;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.defaultRelevanceMatch;
import static ru.yandex.direct.core.entity.adgroup.service.complex.contentpromotion.ComplexContentPromotionAdGroupTestData.contentPromotionAdGroupWithComplexBidModifier;
import static ru.yandex.direct.core.entity.adgroup.service.complex.contentpromotion.ComplexContentPromotionAdGroupTestData.contentPromotionAdGroupWithKeywords;
import static ru.yandex.direct.core.entity.adgroup.service.complex.contentpromotion.ComplexContentPromotionAdGroupTestData.emptyAdGroup;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierAdjustment.PERCENT;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics.DEMOGRAPHICS_ADJUSTMENTS;
import static ru.yandex.direct.core.entity.bids.validation.BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_GREATER_THAN_MIN;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.nestedOrEmptySquareBrackets;
import static ru.yandex.direct.core.entity.relevancematch.valdiation.RelevanceMatchDefects.maxRelevanceMatchesInAdGroup;
import static ru.yandex.direct.core.entity.relevancematch.valdiation.RelevanceMatchValidationService.MAX_RELEVANCE_MATCHES_IN_GROUP;
import static ru.yandex.direct.core.testing.data.TestContentPromotionCommonData.defaultContentPromotion;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultClientKeyword;
import static ru.yandex.direct.currency.Money.valueOf;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.defect.NumberDefects.lessThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexContentPromotionAddTest {
    private static final CompareStrategy AD_GROUP_NO_KEYWORDS_COMPARE_STRATEGY_WITH_STATUSES = onlyExpectedFields()
            .forFields(newPath("statusBsSynced")).useMatcher(notNullValue())
            .forFields(newPath("statusShowsForecast")).useMatcher(notNullValue());
    private static final CompareStrategy AD_GROUP_COMPARE_STRATEGY_WITH_STATUSES = onlyExpectedFields()
            .forFields(newPath("statusBsSynced")).useMatcher(is(StatusBsSynced.NO))
            .forFields(newPath("statusShowsForecast")).useMatcher(is(StatusShowsForecast.NEW));
    private static final BigDecimal FIXED_AUTO_PRICE = BigDecimal.valueOf(710L).setScale(2, RoundingMode.UNNECESSARY);


    @Autowired
    private ComplexAdGroupAddOperationFactory addOperationFactory;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private BannerTypedRepository bannerTypedRepository;
    @Autowired
    private ComplexAdGroupTestCommons commonChecks;
    @Autowired
    private Steps steps;
    @Autowired
    private RetargetingService retargetingService;
    @Autowired
    private GeoTreeFactory geoTreeFactory;
    @Autowired
    private ContentPromotionRepository contentPromotionRepository;

    private GeoTree geoTree;

    private ContentPromotionCampaignInfo campaign;
    private Long contentId;

    @Before
    public void before() {
        geoTree = geoTreeFactory.getGlobalGeoTree();
        campaign = steps.contentPromotionCampaignSteps().createDefaultCampaign();
        contentId = contentPromotionRepository.insertContentPromotion(campaign.getClientId(),
                defaultContentPromotion(campaign.getClientId(), ContentPromotionContentType.VIDEO)
                        .withUrl("https://www.youtube.com/watch?v=jRGrNDV2mKc"));
    }

    @Test
    public void addEmptyContentPromotionAdGroupSuccessTest() {
        ComplexContentPromotionAdGroup complexAdGroup = emptyAdGroup(campaign.getCampaignId());
        ComplexContentPromotionAdGroupAddOperation operation = createOperation(singletonList(complexAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        checkComplexAdGroup(complexAdGroup);
    }

    @Test
    public void addContentPromotionAdGroupWithKeywordsSuccessTest() {
        ComplexContentPromotionAdGroup complexAdGroup =
                contentPromotionAdGroupWithKeywords(campaign.getCampaignId());
        ((ContentPromotionBanner) complexAdGroup.getBanners().get(0)).withContentPromotionId(contentId);

        ComplexContentPromotionAdGroupAddOperation operation = createOperation(singletonList(complexAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        checkComplexAdGroup(complexAdGroup);
    }

    @Test
    public void addContentPromotionAdGroupWithComplexBidModifierSuccessTest() {
        ComplexContentPromotionAdGroup complexAdGroup =
                contentPromotionAdGroupWithComplexBidModifier(campaign.getCampaignId());
        ((ContentPromotionBanner) complexAdGroup.getBanners().get(0)).withContentPromotionId(contentId);

        ComplexContentPromotionAdGroupAddOperation operation = createOperation(singletonList(complexAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        checkComplexAdGroup(complexAdGroup);
    }

    @Test
    public void addContentPromotionAdGroupWithInvalidComplexBidModifierErrorTest() {
        ComplexContentPromotionAdGroup complexAdGroup =
                contentPromotionAdGroupWithKeywords(campaign.getCampaignId());
        complexAdGroup.getComplexBidModifier().getDemographyModifier().getDemographicsAdjustments()
                .get(0)
                .withPercent(400_000);

        ComplexContentPromotionAdGroupAddOperation operation = createOperation(singletonList(complexAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        Path errPath = path(index(0), field(ComplexContentPromotionAdGroup.COMPLEX_BID_MODIFIER.name()),
                field(ComplexBidModifier.DEMOGRAPHY_MODIFIER), field(DEMOGRAPHICS_ADJUSTMENTS), index(0),
                field(PERCENT));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, lessThanOrEqualTo(1300))));
    }

    @Test
    public void addContentPromotionAdGroupWithInvalidKeywordErrorTest() {
        BigDecimal price = BigDecimal.valueOf(0.0001);
        ComplexContentPromotionAdGroup complexAdGroup = emptyAdGroup(campaign.getCampaignId())
                .withKeywords(singletonList(defaultClientKeyword().withPrice(price)));
        ComplexContentPromotionAdGroupAddOperation operation = createOperation(singletonList(complexAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        Path errPath = path(index(0), field(ComplexContentPromotionAdGroup.KEYWORDS.name()), index(0),
                field(Keyword.PRICE.name()));
        Defect error = new Defect<>(SEARCH_PRICE_IS_NOT_GREATER_THAN_MIN,
                new CurrencyAmountDefectParams(valueOf(0.3, CurrencyCode.RUB)));
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(errPath, error)));
    }

    @Test
    public void addAdGroupWithMinusKeywordsSuccessTest() {
        ComplexContentPromotionAdGroup complexAdGroup = contentPromotionAdGroupWithKeywords(campaign.getCampaignId());
        complexAdGroup.getAdGroup().withMinusKeywords(ImmutableList.of("word1", "word2"));
        ((ContentPromotionBanner) complexAdGroup.getBanners().get(0)).withContentPromotionId(contentId);

        ComplexContentPromotionAdGroupAddOperation operation = createOperation(singletonList(complexAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        checkComplexAdGroup(complexAdGroup);
    }

    @Test
    public void addAdGroupWithInvalidMinusKeywordsValidationError() {
        ComplexContentPromotionAdGroup complexAdGroup = contentPromotionAdGroupWithKeywords(campaign.getCampaignId());
        complexAdGroup.getAdGroup().withMinusKeywords(singletonList("[]"));
        ComplexContentPromotionAdGroupAddOperation operation = createOperation(singletonList(complexAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        Path errPath = path(index(0), field(AdGroup.MINUS_KEYWORDS.name()), index(0));
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(errPath,
                nestedOrEmptySquareBrackets(singletonList("[]")))));
    }

    @Test
    public void contentPromotionVideoDomainSetCorrectlyTest() {
        var banner = TestContentPromotionBanners.clientContentPromoBanner(contentId);
        ComplexContentPromotionAdGroup complexAdGroup =
                contentPromotionAdGroupWithKeywords(campaign.getCampaignId())
                        .withBanners(singletonList(banner));

        ComplexContentPromotionAdGroupAddOperation operation = createOperation(singletonList(complexAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        checkComplexAdGroup(complexAdGroup);
        Map<Long, Banner> actualBanners = listToMap(bannerTypedRepository.getBannersByGroupIds(campaign.getShard(),
                singletonList(complexAdGroup.getAdGroup().getId())), Banner::getId);
        ContentPromotionBanner expectedBanner = (ContentPromotionBanner) complexAdGroup.getBanners().get(0);
        ContentPromotionBanner actualBanner = (ContentPromotionBanner) actualBanners.get(expectedBanner.getId());
        assertThat("в domain должен записаться домен видеоссылки",
                actualBanner.getDomain(), equalTo("www.youtube.com"));
    }

    /**
     * Проверяем, что режим {@code autoPrices} корректно прокидывается до
     * {@link ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchAddOperation}
     */
    @Test
    public void addAdGroupWithRelevanceMatchWithAutoPricesSuccessTest() {
        RelevanceMatch relevanceMatch = defaultRelevanceMatch()
                .withPrice(null)
                .withPriceContext(null);
        ComplexContentPromotionAdGroup complexAdGroup = contentPromotionAdGroupWithKeywords(campaign.getCampaignId())
                .withRelevanceMatches(singletonList(relevanceMatch));
        ((ContentPromotionBanner) complexAdGroup.getBanners().get(0)).withContentPromotionId(contentId);

        ShowConditionAutoPriceParams showConditionAutoPriceParams = new ShowConditionAutoPriceParams(
                ShowConditionFixedAutoPrices.ofGlobalFixedPrice(FIXED_AUTO_PRICE),
                keywords -> emptyMap());

        ComplexContentPromotionAdGroupAddOperation operation = createOperationWithAutoPriceParams(
                singletonList(complexAdGroup), showConditionAutoPriceParams);
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        checkComplexAdGroup(complexAdGroup);
        List<RelevanceMatch> relevanceMatches = commonChecks.getRelevanceMatchesOfAdGroup(
                complexAdGroup.getAdGroup().getId(),
                campaign.getClientId(), campaign.getShard()
        );
        assertThat("у автотаргетинга проставилась автоматическая ставка",
                relevanceMatches.get(0).getPrice(),
                Matchers.is(FIXED_AUTO_PRICE)
        );
    }

    @Test
    public void addAdGroupWithTooManyRelevanceMatchesValidationError() {
        List<RelevanceMatch> relevanceMatches = new ArrayList<>();
        for (int i = 0; i < MAX_RELEVANCE_MATCHES_IN_GROUP + 1; ++i) {
            relevanceMatches.add(defaultRelevanceMatch());
        }
        ComplexContentPromotionAdGroup complexAdGroup = contentPromotionAdGroupWithKeywords(campaign.getCampaignId())
                .withRelevanceMatches(relevanceMatches);
        ComplexContentPromotionAdGroupAddOperation addOperation = createOperation(singletonList(complexAdGroup));
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat("превышено максимальное количество бесфразных таргетингов в группе", result.getValidationResult(),
                allOf(
                        hasDefectDefinitionWith(validationError(
                                path(index(0), field(ComplexContentPromotionAdGroup.RELEVANCE_MATCHES.name()),
                                        index(0)),
                                maxRelevanceMatchesInAdGroup())),
                        hasDefectDefinitionWith(validationError(
                                path(index(0), field(ComplexContentPromotionAdGroup.RELEVANCE_MATCHES.name()),
                                        index(1)),
                                maxRelevanceMatchesInAdGroup()))
                )
        );
    }

    private ComplexContentPromotionAdGroupAddOperation createOperation(
            List<ComplexContentPromotionAdGroup> complexAdGroups) {
        return addOperationFactory.createContentPromotionAdGroupAddOperation(true, complexAdGroups,
                geoTree, false, null, campaign.getUid(), campaign.getClientId(), campaign.getUid());
    }

    private ComplexContentPromotionAdGroupAddOperation createOperationWithAutoPriceParams(
            List<ComplexContentPromotionAdGroup> complexAdGroups,
            ShowConditionAutoPriceParams showConditionAutoPriceParams) {
        return addOperationFactory.createContentPromotionAdGroupAddOperation(true, complexAdGroups, geoTree, true,
                showConditionAutoPriceParams, campaign.getUid(), campaign.getClientId(), campaign.getUid());
    }


    private void checkComplexAdGroup(ComplexContentPromotionAdGroup complexAdGroup) {
        AdGroup expectedAdGroup = complexAdGroup.getAdGroup();
        List<AdGroup> adGroups =
                adGroupRepository.getAdGroups(campaign.getShard(), singletonList(expectedAdGroup.getId()));
        CompareStrategy compareStrategy = isEmpty(complexAdGroup.getKeywords()) ?
                AD_GROUP_NO_KEYWORDS_COMPARE_STRATEGY_WITH_STATUSES : AD_GROUP_COMPARE_STRATEGY_WITH_STATUSES;
        assertThat("группа успешно добавлена", adGroups,
                contains(beanDiffer(expectedAdGroup).useCompareStrategy(compareStrategy)));

        Long adGroupId = complexAdGroup.getAdGroup().getId();
        List<Banner> banners =
                bannerTypedRepository.getBannersByGroupIds(campaign.getShard(), singletonList(adGroupId));
        if (complexAdGroup.getBanners() == null) {
            assertThat("в группе не должно быть баннеров", banners, empty());
        } else {
            CompareStrategy bannerCompareStrategy = onlyExpectedFields()
                    .forFields(newPath("lastChange")).useMatcher(notNullValue());
            assertThat("добавлены правильные баннеры", complexAdGroup.getBanners(),
                    contains(mapList(banners,
                            banner -> beanDiffer(banner).useCompareStrategy(bannerCompareStrategy))));
        }

        assertThat("в группе content promotion video не должно быть ретаргетингов",
                retargetingService.getTargetInterestsWithInterestByAdGroupIds(singletonList(adGroupId),
                        campaign.getClientId(), campaign.getShard()),
                empty());
        commonChecks
                .checkKeywords(complexAdGroup.getKeywords(), complexAdGroup.getAdGroup().getId(), campaign.getShard());

        commonChecks.checkBidModifiers(complexAdGroup.getComplexBidModifier(), adGroupId, campaign.getCampaignId(),
                campaign.getShard());

        commonChecks.checkRelevanceMatches(complexAdGroup.getRelevanceMatches(), adGroupId,
                campaign.getClientId(), campaign.getShard());

    }
}
