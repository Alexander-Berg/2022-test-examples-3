package ru.yandex.direct.core.entity.keyword.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.bshistory.History;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.keyword.container.AddedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.container.StopwordsFixation;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.model.Place;
import ru.yandex.direct.core.entity.keyword.model.StatusModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.dbschema.ppc.enums.CampOptionsStrategy;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsAutobudget;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsPlatform;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStrategyName;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.libs.keywordutils.parser.KeywordParser;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.addedInfoMatcher;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAddedToAdGroup;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_OPTIONS;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsAddOperationSaveDataTest extends KeywordsAddOperationBaseTest {

    private static final BigDecimal DEFAULT_PRICE_BD =
            BigDecimal.valueOf(DEFAULT_PRICE).setScale(2, RoundingMode.DOWN);

    @Autowired
    private DslContextProvider dslContextProvider;

    private static final CompareStrategy COMPARE_STRATEGY = DefaultCompareStrategies.allFields()
            .forFields(newPath("modificationTime")).useMatcher(approximatelyNow());

    // сохранение текста фразы

    @Test
    public void execute_OnePhraseWithoutModification_PhraseSavedCorrectly() {
        createOneActiveAdGroup();

        String phrase1 = "кот";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, phrase1, DEFAULT_PRICE));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAddedToAdGroup(phrase1, adGroupInfo1.getAdGroupId())));

        Long addedId = result.get(0).getResult().getId();
        Keyword expectedKeyword = expectedKeywordWithPrice(addedId, phrase1, phrase1, DEFAULT_PRICE_BD);
        assertSavedKeyword(addedId, expectedKeyword);
    }

    @Test
    public void execute_OnePhraseWithOnlyNormalization_PhraseSavedCorrectly() {
        createOneActiveAdGroup();

        String phrase1 = "бегущий по лезвию";
        String normPhrase1 = "бежать лезвие";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, phrase1, DEFAULT_PRICE));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAddedToAdGroup(phrase1, adGroupInfo1.getAdGroupId())));

        Long addedId = result.get(0).getResult().getId();
        Keyword expectedKeyword = expectedKeywordWithPrice(addedId, phrase1, normPhrase1, DEFAULT_PRICE_BD);
        assertSavedKeyword(addedId, expectedKeyword);
    }

    @Test
    public void execute_OnePhraseWithPrettifyingAndNormalization_PhraseSavedCorrectly() {
        createOneActiveAdGroup();

        String phrase1 = "бегущий  лезвие";
        String resultPhrase1 = "бегущий лезвие";
        String normPhrase1 = "бежать лезвие";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, phrase1, DEFAULT_PRICE));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAddedToAdGroup(resultPhrase1, adGroupInfo1.getAdGroupId())));

        Long addedId = result.get(0).getResult().getId();
        Keyword expectedKeyword = expectedKeywordWithPrice(addedId, resultPhrase1, normPhrase1, DEFAULT_PRICE_BD);
        assertSavedKeyword(addedId, expectedKeyword);
    }

    @Test
    public void execute_OnePhraseWithUpperCaseWithCharReplacementWithEndingPoint_PhraseSavedCorrectly() {
        createOneActiveAdGroup();

        String phrase1 = "ёшкин ты Конь.";
        String resultPhrase1 = "ешкин ты Конь.";
        String normPhrase1 = "ешкин конь";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, phrase1, DEFAULT_PRICE));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAddedToAdGroup(resultPhrase1, adGroupInfo1.getAdGroupId())));

        Long addedId = result.get(0).getResult().getId();
        Keyword expectedKeyword = expectedKeywordWithPrice(addedId, resultPhrase1, normPhrase1, DEFAULT_PRICE_BD);
        assertSavedKeyword(addedId, expectedKeyword);
    }

    @Test
    public void execute_OnePhraseWithStopwordFixationAndNormalization_PhraseSavedCorrectly() {
        createOneActiveAdGroup();

        String phrase1 = " 123  на -в";
        String resultPhrase1 = "123 +на -!в";
        String normPhrase1 = "+на 123";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, phrase1, DEFAULT_PRICE));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        List<StopwordsFixation> expectedFixations = asList(
                new StopwordsFixation("123 на", "123 +на"),
                new StopwordsFixation("-в", "-!в"));
        assumeThat(result, isSuccessfulWithMatchers(addedInfoMatcher(
                adGroupInfo1.getAdGroupId(), true, resultPhrase1, resultPhrase1, expectedFixations, null)));

        Long addedId = result.get(0).getResult().getId();
        Keyword expectedKeyword = expectedKeywordWithPrice(addedId, resultPhrase1, normPhrase1, DEFAULT_PRICE_BD);
        assertSavedKeyword(addedId, expectedKeyword);
    }

    // сохранение ---autotargetig

    @Test
    public void execute_OnePhraseWithAutotargetingPrefix_PhraseSavedCorrectly() {
        createOneActiveAdGroup();
        steps.featureSteps()
                .addClientFeature(clientInfo.getClientId(), FeatureName.AUTOTARGETING_KEYWORD_PREFIX_ALLOWED, true);

        String phrase = "кот";
        Keyword clientKeyword = clientKeyword(adGroupInfo1, phrase, DEFAULT_PRICE).withIsAutotargeting(true);
        List<Keyword> keywords = singletonList(clientKeyword);
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAddedToAdGroup(phrase, adGroupInfo1.getAdGroupId())));

        Long addedId = result.get(0).getResult().getId();
        Keyword expectedKeyword = expectedKeywordWithPrice(addedId, phrase, phrase, DEFAULT_PRICE_BD)
                .withIsAutotargeting(true);
        assertSavedKeyword(addedId, expectedKeyword);
    }

    // сохранение hrefParam1 и hrefParam2

    @Test
    public void execute_HrefParamsDefined_PhraseSavedCorrectly() {
        createOneActiveAdGroup();

        String phrase1 = "кот";
        String hrefParam1 = "123";
        String hrefParam2 = "ну все последний тест";
        Keyword keyword = clientKeyword(adGroupInfo1, phrase1, DEFAULT_PRICE)
                .withHrefParam1(hrefParam1)
                .withHrefParam2(hrefParam2);
        List<Keyword> keywords = singletonList(keyword);
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAddedToAdGroup(phrase1, adGroupInfo1.getAdGroupId())));

        Long addedId = result.get(0).getResult().getId();
        Keyword expectedKeyword = expectedKeywordWithPrice(addedId, phrase1, phrase1, DEFAULT_PRICE_BD)
                .withHrefParam1(hrefParam1)
                .withHrefParam2(hrefParam2);
        assertSavedKeyword(addedId, expectedKeyword);
    }

    // сохранение финансовых полей

    @Test
    public void execute_OnePhraseWithOnlySearchPrice_MoneyFieldsSavedCorrectly() {
        createOneActiveAdGroup();
        makeCampaignManual(adGroupInfo1.getCampaignInfo());

        String phrase1 = "бегущий по лезвию";
        String normPhrase1 = "бежать лезвие";
        BigDecimal searchPrice = DEFAULT_PRICE_BD;
        Keyword keyword = clientKeyword(adGroupInfo1, phrase1)
                .withPrice(searchPrice)
                .withPriceContext(null)
                .withAutobudgetPriority(null);
        List<Keyword> keywords = singletonList(keyword);
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAddedToAdGroup(phrase1, adGroupInfo1.getAdGroupId())));

        Long addedId = result.get(0).getResult().getId();
        Keyword expectedKeyword = expectedKeyword(addedId, phrase1, normPhrase1)
                .withPrice(searchPrice)
                .withPriceContext(null)
                .withAutobudgetPriority(null);
        assertSavedKeyword(addedId, expectedKeyword);
    }

    @Test
    public void execute_OnePhraseWithOnlyContextPrice_MoneyFieldsSavedCorrectly() {
        createOneActiveAdGroup();
        makeCampaignManualContextOnly(adGroupInfo1.getCampaignInfo());

        String phrase1 = "бегущий по лезвию";
        String normPhrase1 = "бежать лезвие";
        BigDecimal contextPrice = DEFAULT_PRICE_BD;
        Keyword keyword = clientKeyword(adGroupInfo1, phrase1)
                .withPrice(null)
                .withPriceContext(contextPrice)
                .withAutobudgetPriority(null);
        List<Keyword> keywords = singletonList(keyword);
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAddedToAdGroup(phrase1, adGroupInfo1.getAdGroupId())));

        Long addedId = result.get(0).getResult().getId();
        Keyword expectedKeyword = expectedKeyword(addedId, phrase1, normPhrase1)
                .withPrice(null)
                .withPriceContext(contextPrice)
                .withAutobudgetPriority(null)
                .withPlace(Place.ROTATION);
        assertSavedKeyword(addedId, expectedKeyword);
    }

    @Test
    public void execute_OnePhraseWithSearchAndContextPrice_MoneyFieldsSavedCorrectly() {
        createOneActiveAdGroup();
        makeCampaignManualWithSeparatedControl(adGroupInfo1.getCampaignInfo());

        String phrase1 = "бегущий по лезвию";
        String normPhrase1 = "бежать лезвие";
        BigDecimal searchPrice = BigDecimal.valueOf(100L).setScale(2, RoundingMode.DOWN);
        BigDecimal contextPrice = DEFAULT_PRICE_BD;
        Keyword keyword = clientKeyword(adGroupInfo1, phrase1)
                .withPrice(searchPrice)
                .withPriceContext(contextPrice)
                .withAutobudgetPriority(null);
        List<Keyword> keywords = singletonList(keyword);
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAddedToAdGroup(phrase1, adGroupInfo1.getAdGroupId())));

        Long addedId = result.get(0).getResult().getId();
        Keyword expectedKeyword = expectedKeyword(addedId, phrase1, normPhrase1)
                .withPrice(searchPrice)
                .withPriceContext(contextPrice)
                .withAutobudgetPriority(null);
        assertSavedKeyword(addedId, expectedKeyword);
    }

    @Test
    public void execute_OnePhraseWithOnlyAutobudgetPriority_MoneyFieldsSavedCorrectly() {
        createOneActiveAdGroup();
        makeCampaignAutobudget(adGroupInfo1.getCampaignInfo());

        String phrase1 = "бегущий по лезвию";
        String normPhrase1 = "бежать лезвие";
        int autobudgetPriority = 5;
        Keyword keyword = clientKeyword(adGroupInfo1, phrase1)
                .withPrice(null)
                .withPriceContext(null)
                .withAutobudgetPriority(autobudgetPriority);
        List<Keyword> keywords = singletonList(keyword);
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAddedToAdGroup(phrase1, adGroupInfo1.getAdGroupId())));

        Long addedId = result.get(0).getResult().getId();
        Keyword expectedKeyword = expectedKeyword(addedId, phrase1, normPhrase1)
                .withPrice(null)
                .withPriceContext(null)
                .withAutobudgetPriority(autobudgetPriority)
                .withPlace(Place.ROTATION);
        assertSavedKeyword(addedId, expectedKeyword);
    }

    @Test
    public void execute_BothPricesAndPriorityDefinedOnManualStrategy_AllFieldsSaved() {
        createOneActiveAdGroup();
        makeCampaignManual(adGroupInfo1.getCampaignInfo());

        String phrase1 = "бегущий по лезвию";
        String normPhrase1 = "бежать лезвие";
        int autobudgetPriority = 5;
        BigDecimal searchPrice = BigDecimal.valueOf(100L).setScale(2, RoundingMode.DOWN);
        Keyword keyword = clientKeyword(adGroupInfo1, phrase1)
                .withPrice(searchPrice)
                .withPriceContext(DEFAULT_PRICE_BD)
                .withAutobudgetPriority(autobudgetPriority);
        List<Keyword> keywords = singletonList(keyword);
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAddedToAdGroup(phrase1, adGroupInfo1.getAdGroupId())));

        Long addedId = result.get(0).getResult().getId();
        Keyword expectedKeyword = expectedKeyword(addedId, phrase1, normPhrase1)
                .withPrice(searchPrice)
                .withPriceContext(DEFAULT_PRICE_BD)
                .withAutobudgetPriority(autobudgetPriority);
        assertSavedKeyword(addedId, expectedKeyword);
    }

    @Test
    public void execute_BothPricesAndPriorityDefinedOnAutoStrategy_AllFieldsSaved() {
        createOneActiveAdGroup();
        makeCampaignAutobudget(adGroupInfo1.getCampaignInfo());

        String phrase1 = "бегущий по лезвию";
        String normPhrase1 = "бежать лезвие";
        int autobudgetPriority = 5;
        BigDecimal searchPrice = BigDecimal.valueOf(100L).setScale(2, RoundingMode.DOWN);
        Keyword keyword = clientKeyword(adGroupInfo1, phrase1)
                .withPrice(searchPrice)
                .withPriceContext(DEFAULT_PRICE_BD)
                .withAutobudgetPriority(autobudgetPriority);
        List<Keyword> keywords = singletonList(keyword);
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAddedToAdGroup(phrase1, adGroupInfo1.getAdGroupId())));

        Long addedId = result.get(0).getResult().getId();
        Keyword expectedKeyword = expectedKeyword(addedId, phrase1, normPhrase1)
                .withPrice(searchPrice)
                .withPriceContext(DEFAULT_PRICE_BD)
                .withAutobudgetPriority(autobudgetPriority);
        assertSavedKeyword(addedId, expectedKeyword);
    }

    @Test
    public void execute_SearchPriceIsOnPremium2PlaceAndContextPriceIsOnPremium1Place_PlaceComputedOverSearchPriceCorrectly() {
        createOneActiveAdGroup();
        makeCampaignManual(adGroupInfo1.getCampaignInfo());

        String phrase1 = "бегущий по лезвию";
        String normPhrase1 = "бежать лезвие";
        int autobudgetPriority = 5;
        BigDecimal searchPrice = BigDecimal.valueOf(47L).setScale(2, RoundingMode.DOWN);
        BigDecimal contextPrice = BigDecimal.valueOf(200L).setScale(2, RoundingMode.DOWN);
        Keyword keyword = clientKeyword(adGroupInfo1, phrase1)
                .withPrice(searchPrice)
                .withPriceContext(contextPrice)
                .withAutobudgetPriority(autobudgetPriority);
        List<Keyword> keywords = singletonList(keyword);
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAddedToAdGroup(phrase1, adGroupInfo1.getAdGroupId())));

        Long addedId = result.get(0).getResult().getId();
        Keyword expectedKeyword = expectedKeyword(addedId, phrase1, normPhrase1)
                .withPrice(searchPrice)
                .withPriceContext(contextPrice)
                .withAutobudgetPriority(autobudgetPriority)
                .withPlace(Place.PREMIUM2);
        assertSavedKeyword(addedId, expectedKeyword);
    }

    // сохранение в несколько групп, кампаний

    @Test
    public void execute_SaveToDifferentAdGroupsInOneCampaign_DataSavedCorrectly() {
        createTwoActiveAdGroups();

        String phrase1 = "кот";
        String phrase2 = "пес хатико";
        BigDecimal price1 = DEFAULT_PRICE_BD;
        BigDecimal price2 = BigDecimal.valueOf(43).setScale(2, RoundingMode.DOWN);
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1, price1.longValue()),
                clientKeyword(adGroupInfo2, phrase2, price2.longValue()));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(
                isAddedToAdGroup(phrase1, adGroupInfo1.getAdGroupId()),
                isAddedToAdGroup(phrase2, adGroupInfo2.getAdGroupId())));

        Long addedId1 = result.get(0).getResult().getId();
        Keyword expectedKeyword1 = expectedKeywordWithPrice(addedId1, phrase1, phrase1, price1);
        assertSavedKeyword(addedId1, expectedKeyword1);

        Long addedId2 = result.get(1).getResult().getId();
        Keyword expectedKeyword2 = expectedKeywordWithPrice(addedId2, phrase2, phrase2, price2)
                .withPlace(Place.PREMIUM3)
                .withAdGroupId(adGroupInfo2.getAdGroupId());
        assertSavedKeyword(addedId2, expectedKeyword2);
    }

    @Test
    public void execute_SaveToDifferentCampaigns_DataSavedCorrectly() {
        createOneActiveAdGroup();
        AdGroupInfo adGroupInfo2 = steps.adGroupSteps().createActiveTextAdGroup(adGroupInfo1.getClientInfo());

        String phrase1 = "кот";
        String phrase2 = "пес";
        BigDecimal price1 = DEFAULT_PRICE_BD;
        BigDecimal price2 = BigDecimal.valueOf(43).setScale(2, RoundingMode.DOWN);
        String hrefParam = "я параметр";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, phrase1, price1.longValue()),
                clientKeyword(adGroupInfo2, phrase2, price2.longValue()).withHrefParam1(hrefParam));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(
                isAddedToAdGroup(phrase1, adGroupInfo1.getAdGroupId()),
                isAddedToAdGroup(phrase2, adGroupInfo2.getAdGroupId())));

        Long addedId1 = result.get(0).getResult().getId();
        Keyword expectedKeyword1 = expectedKeywordWithPrice(addedId1, phrase1, phrase1, price1);
        assertSavedKeyword(addedId1, expectedKeyword1);

        Long addedId2 = result.get(1).getResult().getId();
        Keyword expectedKeyword2 = expectedKeywordWithPrice(addedId2, phrase2, phrase2, price2)
                .withPlace(Place.PREMIUM3)
                .withAdGroupId(adGroupInfo2.getAdGroupId())
                .withCampaignId(adGroupInfo2.getCampaignId())
                .withHrefParam1(hrefParam);
        assertSavedKeyword(addedId2, expectedKeyword2);
    }

    // недопустимые для установки поля должны затираться

    @Test
    public void execute_SuspendedPhrase_PhraseSavedCorrectly() {
        createOneActiveAdGroup();

        String phrase1 = "кот";
        Keyword keyword = clientKeyword(adGroupInfo1, phrase1, DEFAULT_PRICE)
                .withCampaignId(218942832L)
                .withPhraseBsId(BigInteger.valueOf(4L))
                .withPlace(Place.FIRST)
                .withStatusModerate(StatusModerate.YES)
                .withStatusBsSynced(StatusBsSynced.YES)
                .withNormPhrase("qerfknj")
                .withWordsCount(12)
                .withNeedCheckPlaceModified(false)
                .withShowsForecast(123L)
                .withPhraseIdHistory(History.parse("O1;G5"))
                .withIsSuspended(true)
                .withModificationTime(LocalDateTime.now().minusDays(10));
        List<Keyword> keywords = singletonList(keyword);
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAddedToAdGroup(phrase1, adGroupInfo1.getAdGroupId())));

        Long addedId = result.get(0).getResult().getId();
        Keyword expectedKeyword = expectedKeywordWithPrice(addedId, phrase1, phrase1, DEFAULT_PRICE_BD);
        assertSavedKeyword(addedId, expectedKeyword);
    }

    private void assertSavedKeyword(Long addedId, Keyword expected) {
        Keyword actual = getKeyword(addedId);
        assertThat("сохраненная ключевая фраза отличается от ожидаемой",
                actual, beanDiffer(expected).useCompareStrategy(COMPARE_STRATEGY));
    }

    private Keyword expectedKeywordWithPrice(Long id, String phrase, String normPhrase, BigDecimal price) {
        return expectedKeyword(id, phrase, normPhrase).withPrice(price).withPriceContext(price);
    }

    private Keyword expectedKeyword(Long id, String phrase, String normPhrase) {
        return new Keyword()
                .withId(id)
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withCampaignId(adGroupInfo1.getCampaignId())
                .withPhraseBsId(BigInteger.ZERO)
                .withPhrase(phrase)
                .withIsAutotargeting(false)
                .withNormPhrase(normPhrase)
                .withWordsCount(KeywordParser.parse(normPhrase).getAllKeywords().size())
                .withPlace(Place.PREMIUM1)
                .withStatusModerate(StatusModerate.NEW)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withNeedCheckPlaceModified(true)
                .withShowsForecast(DEFAULT_SHOWS_FORECAST)
                .withPhraseIdHistory(null)
                .withIsSuspended(false);
    }

    private void makeCampaignManual(CampaignInfo campaignInfo) {
        StrategyData strategyData = new StrategyData();
        dslContextProvider.ppc(campaignInfo.getShard())
                .update(CAMPAIGNS.join(CAMP_OPTIONS).on(CAMP_OPTIONS.CID.eq(CAMPAIGNS.CID)))
                .set(CAMPAIGNS.STRATEGY_NAME, CampaignsStrategyName.default_)
                .set(CAMPAIGNS.STRATEGY_DATA, toJson(strategyData))
                .set(CAMP_OPTIONS.STRATEGY, (CampOptionsStrategy) null)
                .where(CAMPAIGNS.CID.eq(campaignInfo.getCampaignId()))
                .execute();
    }

    private void makeCampaignAutobudget(CampaignInfo campaignInfo) {
        StrategyData strategyData = new StrategyData()
                .withSum(new BigDecimal("100000"))
                .withLimitClicks(1000L)
                .withVersion(1L);
        dslContextProvider.ppc(campaignInfo.getShard())
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.AUTOBUDGET, CampaignsAutobudget.Yes)
                .set(CAMPAIGNS.STRATEGY_NAME, CampaignsStrategyName.autobudget_avg_click)
                .set(CAMPAIGNS.STRATEGY_DATA, toJson(strategyData))
                .where(CAMPAIGNS.CID.eq(campaignInfo.getCampaignId()))
                .execute();
    }

    private void makeCampaignManualWithSeparatedControl(CampaignInfo campaignInfo) {
        StrategyData strategyData = new StrategyData()
                .withVersion(1L);
        dslContextProvider.ppc(campaignInfo.getShard())
                .update(CAMPAIGNS.join(CAMP_OPTIONS).on(CAMP_OPTIONS.CID.eq(CAMPAIGNS.CID)))
                .set(CAMPAIGNS.STRATEGY_NAME, CampaignsStrategyName.default_)
                .set(CAMPAIGNS.STRATEGY_DATA, toJson(strategyData))
                .set(CAMP_OPTIONS.STRATEGY, CampOptionsStrategy.different_places)
                .where(CAMPAIGNS.CID.eq(campaignInfo.getCampaignId()))
                .execute();
    }

    private void makeCampaignManualContextOnly(CampaignInfo campaignInfo) {
        StrategyData strategyData = new StrategyData()
                .withName("default")
                .withVersion(1L);
        dslContextProvider.ppc(campaignInfo.getShard())
                .update(CAMPAIGNS.join(CAMP_OPTIONS).on(CAMP_OPTIONS.CID.eq(CAMPAIGNS.CID)))
                .set(CAMPAIGNS.STRATEGY_NAME, CampaignsStrategyName.default_)
                .set(CAMPAIGNS.STRATEGY_DATA, toJson(strategyData))
                .set(CAMPAIGNS.PLATFORM, CampaignsPlatform.context)
                .set(CAMP_OPTIONS.STRATEGY, CampOptionsStrategy.different_places)
                .where(CAMPAIGNS.CID.eq(campaignInfo.getCampaignId()))
                .execute();
    }
}
