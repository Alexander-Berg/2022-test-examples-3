package ru.yandex.direct.core.entity.adgroup.service.complex.contentpromotion.update;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.adgroup.container.ComplexContentPromotionAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupTestCommons;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupUpdateOperationFactory;
import ru.yandex.direct.core.entity.adgroup.service.complex.contentpromotion.ComplexContentPromotionAdGroupUpdateOperation;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
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
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.adgroup.ContentPromotionAdGroupInfo;
import ru.yandex.direct.core.testing.info.banner.ContentPromotionBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Path;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.defaultRelevanceMatch;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierAdjustment.PERCENT;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics.DEMOGRAPHICS_ADJUSTMENTS;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.onlyStopWords;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.nestedOrEmptySquareBrackets;
import static ru.yandex.direct.core.entity.relevancematch.valdiation.RelevanceMatchDefects.maxRelevanceMatchesInAdGroup;
import static ru.yandex.direct.core.entity.relevancematch.valdiation.RelevanceMatchValidationService.MAX_RELEVANCE_MATCHES_IN_GROUP;
import static ru.yandex.direct.core.testing.data.TestContentPromotionCommonData.defaultContentPromotion;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.validation.defect.NumberDefects.lessThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexContentPromotionUpdateTest {
    private static final BigDecimal FIXED_AUTO_PRICE = BigDecimal.valueOf(710L).setScale(2, RoundingMode.UNNECESSARY);
    private static final String VIDEO_HREF = "https://www.youtube.com/424234";

    @Autowired
    private ComplexAdGroupUpdateOperationFactory operationFactory;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private BannerTypedRepository bannerTypedRepository;
    @Autowired
    private Steps steps;
    @Autowired
    private ComplexAdGroupTestCommons commonChecks;
    @Autowired
    private RetargetingService retargetingService;
    @Autowired
    private GeoTreeFactory geoTreeFactory;
    @Autowired
    private ContentPromotionRepository contentPromotionRepository;
    @Autowired
    private TestContentPromotionBanners testNewContentPromotionBanners;

    private GeoTree geoTree;

    private ComplexContentPromotionAdGroup complexContentPromotionAdGroup;
    private ContentPromotionAdGroupInfo contentPromotionAdGroup;
    private ClientInfo clientInfo;
    private Long contentId;

    @Before
    public void before() {
        geoTree = geoTreeFactory.getGlobalGeoTree();
        clientInfo = steps.clientSteps().createDefaultClient();
        contentPromotionAdGroup = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(clientInfo, ContentPromotionAdgroupType.VIDEO);
        var content = defaultContentPromotion(clientInfo.getClientId(), ContentPromotionContentType.VIDEO)
                .withUrl("https://www.youtube.com/watch?v=jRGrNDV2mKc");
        contentId = contentPromotionRepository.insertContentPromotion(clientInfo.getClientId(), content);
        ContentPromotionBanner banner = testNewContentPromotionBanners.fullContentPromoBanner(contentId, VIDEO_HREF)
                .withCampaignId(contentPromotionAdGroup.getCampaignId())
                .withAdGroupId(contentPromotionAdGroup.getAdGroupId())
                .withTitle("English title");
        var contentPromotionVideoBannerInfo = steps.contentPromotionBannerSteps()
                .createBanner(new ContentPromotionBannerInfo()
                .withAdGroupInfo(contentPromotionAdGroup)
                .withBanner(banner)
                .withContent(content));

        complexContentPromotionAdGroup = new ComplexContentPromotionAdGroup()
                .withAdGroup(contentPromotionAdGroup.getAdGroup())
                .withBanners(singletonList(contentPromotionVideoBannerInfo.getBanner()));

        var keywordInfo =
                steps.newKeywordSteps().createKeyword(contentPromotionAdGroup, defaultKeyword());
        var bidModifierInfo =
                steps.newBidModifierSteps().createDefaultAdGroupBidModifierDemographics(contentPromotionAdGroup);
        ComplexBidModifier complexBidModifier = new ComplexBidModifier()
                .withDemographyModifier((BidModifierDemographics) bidModifierInfo.getBidModifier());

        complexContentPromotionAdGroup
                .withKeywords(singletonList(keywordInfo.getKeyword()))
                .withComplexBidModifier(complexBidModifier);
    }

    @Test
    public void updateContentPromotionAdGroupAndBannerSuccessTest() {
        complexContentPromotionAdGroup.getAdGroup().withName("new adgroup name");

        ComplexContentPromotionAdGroupUpdateOperation operation =
                createOperation(singletonList(complexContentPromotionAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        checkComplexAdGroup(complexContentPromotionAdGroup);
    }

    @Test
    public void addBannerToContentPromotionVideoAdGroupSuccessTest() {
        var banners = new ArrayList<>(complexContentPromotionAdGroup.getBanners());
        banners.add(TestContentPromotionBanners.clientContentPromoBanner(contentId)
                .withCampaignId(complexContentPromotionAdGroup.getAdGroup().getCampaignId())
                .withAdGroupId(complexContentPromotionAdGroup.getAdGroup().getId())
                .withBody("some new body")
                .withDomain(null)
                .withHref(null));
        complexContentPromotionAdGroup.setBanners(banners);

        ComplexContentPromotionAdGroupUpdateOperation operation =
                createOperation(singletonList(complexContentPromotionAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        checkComplexAdGroup(complexContentPromotionAdGroup);
    }

    @Test
    public void updateKeywordAndBidModifierSuccessTest() {
        complexContentPromotionAdGroup.getKeywords().get(0)
                .withPhrase("another phrase");
        complexContentPromotionAdGroup.getComplexBidModifier().getDemographyModifier()
                .getDemographicsAdjustments()
                .get(0)
                .withPercent(120);

        ComplexContentPromotionAdGroupUpdateOperation operation =
                createOperation(singletonList(complexContentPromotionAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        commonChecks.checkKeywords(
                complexContentPromotionAdGroup.getKeywords(),
                complexContentPromotionAdGroup.getAdGroup().getId(), clientInfo.getShard());
        commonChecks.checkBidModifiers(complexContentPromotionAdGroup.getComplexBidModifier(),
                contentPromotionAdGroup.getAdGroupId(), contentPromotionAdGroup.getCampaignId(), clientInfo.getShard());
    }

    @Test
    public void updateSetInvalidKeywordValidationErrorTest() {
        complexContentPromotionAdGroup.getKeywords().get(0)
                .withPhrase("и и и");
        ComplexContentPromotionAdGroupUpdateOperation operation =
                createOperation(singletonList(complexContentPromotionAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        Path errPath = path(index(0), field(ComplexContentPromotionAdGroup.KEYWORDS), index(0), field(Keyword.PHRASE));
        assertThat("ключевая фраза должна быть с ошибкой валидации", result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, onlyStopWords())));
    }

    @Test
    public void updateSetInvalidBidModifierValidationErrorTest() {
        complexContentPromotionAdGroup.getComplexBidModifier().getDemographyModifier()
                .getDemographicsAdjustments()
                .get(0)
                .withPercent(400_000);
        ComplexContentPromotionAdGroupUpdateOperation operation =
                createOperation(singletonList(complexContentPromotionAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        Path errPath = path(index(0), field(ComplexContentPromotionAdGroup.COMPLEX_BID_MODIFIER.name()),
                field(ComplexBidModifier.DEMOGRAPHY_MODIFIER), field(DEMOGRAPHICS_ADJUSTMENTS), index(0),
                field(PERCENT));
        assertThat("корректировка должна быть с ошибкой валидации", result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, lessThanOrEqualTo(1300))));
    }

    @Test
    public void updateAdGroupWithMinusKeywordsSuccessTest() {
        complexContentPromotionAdGroup.getAdGroup().withMinusKeywords(ImmutableList.of("word1", "word2"));
        ComplexContentPromotionAdGroupUpdateOperation operation =
                createOperation(singletonList(complexContentPromotionAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        checkComplexAdGroup(complexContentPromotionAdGroup);
    }

    @Test
    public void updateAdGroupWithInvalidMinusKeywordsValidationError() {
        complexContentPromotionAdGroup.getAdGroup().withMinusKeywords(singletonList("[]"));
        ComplexContentPromotionAdGroupUpdateOperation operation =
                createOperation(singletonList(complexContentPromotionAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        Path errPath = path(index(0), field(AdGroup.MINUS_KEYWORDS.name()), index(0));
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(errPath,
                nestedOrEmptySquareBrackets(singletonList("[]")))));
    }

    /**
     * Проверяем, что включение режима {@code autoPrices} корректно
     * прокидывается до {@link ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchModifyOperation}
     */
    @Test
    public void updateAdGroupWithRelevanceMatchWithAutoPricesSuccessTest() {
        RelevanceMatch relevanceMatch = steps.relevanceMatchSteps().getDefaultRelevanceMatch(contentPromotionAdGroup)
                .withPrice(null)
                .withPriceContext(null);
        complexContentPromotionAdGroup.withRelevanceMatches(singletonList(relevanceMatch));
        ShowConditionAutoPriceParams showConditionAutoPriceParams = new ShowConditionAutoPriceParams(
                ShowConditionFixedAutoPrices.ofGlobalFixedPrice(FIXED_AUTO_PRICE),
                keywords -> emptyMap());
        ComplexContentPromotionAdGroupUpdateOperation operation = createOperationWithAutoPriceParams(
                singletonList(complexContentPromotionAdGroup), showConditionAutoPriceParams);
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        checkComplexAdGroup(complexContentPromotionAdGroup);

        List<RelevanceMatch> relevanceMatches = commonChecks.getRelevanceMatchesOfAdGroup(
                contentPromotionAdGroup.getAdGroupId(), clientInfo.getClientId(), clientInfo.getShard());
        assertThat("в группе появился автотаргетинг", relevanceMatches, hasSize(1));
        relevanceMatch = relevanceMatches.get(0);
        assertThat("У автотаргетинга выставилась автоматическая ставка", relevanceMatch.getPrice(),
                is(FIXED_AUTO_PRICE));
    }

    @Test
    public void updateAdGroupWithTooManyRelevanceMatchesValidationError() {
        List<RelevanceMatch> relevanceMatches = new ArrayList<>();
        for (int i = 0; i < MAX_RELEVANCE_MATCHES_IN_GROUP + 1; ++i) {
            relevanceMatches.add(defaultRelevanceMatch());
        }
        complexContentPromotionAdGroup.withRelevanceMatches(relevanceMatches);
        ShowConditionAutoPriceParams showConditionAutoPriceParams = new ShowConditionAutoPriceParams(
                ShowConditionFixedAutoPrices.ofGlobalFixedPrice(FIXED_AUTO_PRICE),
                keywords -> emptyMap());
        ComplexContentPromotionAdGroupUpdateOperation operation = createOperationWithAutoPriceParams(
                singletonList(complexContentPromotionAdGroup), showConditionAutoPriceParams);
        MassResult<Long> result = operation.prepareAndApply();
        MatcherAssert.assertThat("превышено максимальное количество бесфразных таргетингов в группе",
                result.getValidationResult(),
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

    private ComplexContentPromotionAdGroupUpdateOperation createOperation(
            List<ComplexContentPromotionAdGroup> complexAdGroups) {
        return operationFactory.createContentPromotionAdGroupUpdateOperation(complexAdGroups, geoTree,
                false, null,
                clientInfo.getUid(), clientInfo.getClientId(), clientInfo.getUid(), false);
    }

    private ComplexContentPromotionAdGroupUpdateOperation createOperationWithAutoPriceParams(
            List<ComplexContentPromotionAdGroup> complexAdGroups,
            ShowConditionAutoPriceParams showConditionAutoPriceParams) {
        return operationFactory.createContentPromotionAdGroupUpdateOperation(complexAdGroups, geoTree,
                true, showConditionAutoPriceParams,
                clientInfo.getUid(), clientInfo.getClientId(), clientInfo.getUid(), false);
    }

    private void checkComplexAdGroup(ComplexContentPromotionAdGroup complexAdGroup) {
        AdGroup expectedAdGroup = complexAdGroup.getAdGroup();
        List<AdGroup> adGroups =
                adGroupRepository.getAdGroups(clientInfo.getShard(), singletonList(expectedAdGroup.getId()));
        assertThat("группа обновлена", adGroups,
                contains(beanDiffer(expectedAdGroup).useCompareStrategy(onlyFields(newPath("name")))));

        Long adGroupId = complexAdGroup.getAdGroup().getId();
        List<Banner> actualBanners =
                bannerTypedRepository.getBannersByGroupIds(clientInfo.getShard(), singletonList(adGroupId));
        if (complexAdGroup.getBanners() == null) {
            assertThat("в группе не должно быть баннеров", actualBanners, empty());
        } else {
            Map<Long, Banner> actualBannersMap = listToMap(actualBanners, Banner::getId);
            List<BannerWithSystemFields> expectedBanners = complexAdGroup.getBanners();
            Map<Long, Banner> expectedBannersMap = listToMap(expectedBanners, Banner::getId);

            CompareStrategy bannerCompareStrategy = onlyExpectedFields()
                    .forFields(newPath("lastChange")).useMatcher(notNullValue())
                    .forFields(newPath("statusBsSynced")).useMatcher(notNullValue());

            assertThat(actualBannersMap.size(), equalTo(expectedBannersMap.size()));
            expectedBannersMap.forEach((bannerId, expectedBanner) -> {
                var actualBanner = actualBannersMap.get(bannerId);
                assertThat(actualBanner, beanDiffer(expectedBanner).useCompareStrategy(bannerCompareStrategy));
            });
        }
        assertThat("в группе content promotion video не должно быть ретаргетингов",
                retargetingService.getTargetInterestsWithInterestByAdGroupIds(singletonList(adGroupId),
                        clientInfo.getClientId(), clientInfo.getShard()),
                empty());
        commonChecks
                .checkKeywords(complexAdGroup.getKeywords(), complexAdGroup.getAdGroup().getId(),
                        clientInfo.getShard());

        commonChecks.checkBidModifiers(complexAdGroup.getComplexBidModifier(), adGroupId,
                complexContentPromotionAdGroup.getAdGroup().getCampaignId(), clientInfo.getShard());

        commonChecks.checkRelevanceMatches(complexAdGroup.getRelevanceMatches(), adGroupId,
                clientInfo.getClientId(), clientInfo.getShard());
    }
}
