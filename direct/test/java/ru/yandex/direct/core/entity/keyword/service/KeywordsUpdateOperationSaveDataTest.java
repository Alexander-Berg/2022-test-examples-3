package ru.yandex.direct.core.entity.keyword.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.keyword.container.StopwordsFixation;
import ru.yandex.direct.core.entity.keyword.container.UpdatedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.model.Place;
import ru.yandex.direct.core.entity.keyword.model.StatusModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.dbschema.ppc.enums.CampOptionsStrategy;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsAutobudget;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsPlatform;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStrategyName;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.libs.keywordutils.model.KeywordWithMinuses;
import ru.yandex.direct.libs.keywordutils.parser.KeywordParser;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.updatedInfoMatcher;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_OPTIONS;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdateOperationSaveDataTest extends KeywordsUpdateOperationBaseTest {

    private static final BigDecimal INIT_PRICE =
            BigDecimal.valueOf(DEFAULT_PRICE + 1).setScale(2, RoundingMode.DOWN);
    private static final BigDecimal DEFAULT_PRICE_CHANGE =
            BigDecimal.valueOf(DEFAULT_PRICE).setScale(2, RoundingMode.DOWN);

    @Autowired
    private DslContextProvider dslContextProvider;

    private static final CompareStrategy COMPARE_STRATEGY = DefaultCompareStrategies.allFields()
            .forFields(newPath("needCheckPlaceModified"), newPath("showsForecast")).useMatcher(notNullValue())
            .forFields(newPath("modificationTime")).useMatcher(approximatelyNow());

    @Test
    public void execute_NoChange_PhraseSavedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createDefaultKeyword(adGroupInfo1, PHRASE_1).getId();

        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        Keyword expectedKeyword = expectedKeyword(keywordIdToUpdate, PHRASE_1)
                .withPlace(null); // не поменялся, так как фраза не обновлялась
        assertUpdatedKeyword(keywordIdToUpdate, expectedKeyword);
    }

    // сохранение текста фразы

    @Test
    public void execute_ChangeOnePhraseWithoutModification_PhraseSavedCorrectly() {
        createOneActiveAdGroup();
        String existingPhrase = "пес";
        Long keywordIdToUpdate = createDefaultKeyword(adGroupInfo1, existingPhrase).getId();

        String phrase1 = "кот";
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, phrase1));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, phrase1)));

        Keyword expectedKeyword = expectedKeyword(keywordIdToUpdate, phrase1);
        assertUpdatedKeyword(keywordIdToUpdate, expectedKeyword);
    }

    @Test
    public void execute_ChangeOnePhraseWithOnlyNormalization_PhraseSavedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createDefaultKeyword(adGroupInfo1, PHRASE_1).getId();

        String phrase1 = "бегущий по лезвию";
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, phrase1));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, phrase1)));

        Keyword expectedKeyword = expectedKeyword(keywordIdToUpdate, phrase1);
        assertUpdatedKeyword(keywordIdToUpdate, expectedKeyword);
    }

    @Test
    public void execute_ChangeOnePhraseWithPrettifyingAndNormalization_PhraseSavedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createDefaultKeyword(adGroupInfo1, PHRASE_1).getId();

        String phrase1 = "бегущий  лезвие";
        String resultPhrase1 = "бегущий лезвие";
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, phrase1));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, resultPhrase1)));

        Keyword expectedKeyword = expectedKeyword(keywordIdToUpdate, resultPhrase1);
        assertUpdatedKeyword(keywordIdToUpdate, expectedKeyword);
    }

    @Test
    public void execute_ChangeOnePhraseWithUpperCaseWithCharReplacementWithEndingPoint_PhraseSavedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createDefaultKeyword(adGroupInfo1, PHRASE_1).getId();

        String phrase1 = "ёшкин ты Конь.";
        String resultPhrase1 = "ешкин ты Конь.";
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, phrase1));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, resultPhrase1)));

        Keyword expectedKeyword = expectedKeyword(keywordIdToUpdate, resultPhrase1);
        assertUpdatedKeyword(keywordIdToUpdate, expectedKeyword);
    }

    @Test
    public void execute_ChangeOnePhraseWithStopwordFixationAndNormalization_PhraseSavedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createDefaultKeyword(adGroupInfo1, PHRASE_1).getId();

        String phrase1 = " 123  на -в";
        String resultPhrase1 = "123 +на -!в";
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, phrase1));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        List<StopwordsFixation> expectedFixations = asList(
                new StopwordsFixation("123 на", "123 +на"),
                new StopwordsFixation("-в", "-!в"));
        assumeThat(result, isSuccessfulWithMatchers(
                updatedInfoMatcher(keywordIdToUpdate, resultPhrase1, resultPhrase1, false, expectedFixations, null,
                        false)));

        Keyword expectedKeyword = expectedKeyword(keywordIdToUpdate, resultPhrase1);
        assertUpdatedKeyword(keywordIdToUpdate, expectedKeyword);
    }

    // обновление невалидных фраз

    @Test
    public void execute_UnparsablePhraseFixed_PhraseSavedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate =
                keywordSteps.createKeyword(adGroupInfo1, defaultKeyword()
                        .withPhrase(PREINVALID_PHRASE_1)
                        .withPriceContext(INIT_PRICE)
                        .withPrice(INIT_PRICE)
                        .withAutobudgetPriority(3)
                        .withPlace(Place.PREMIUM1)).getId();

        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        Keyword expectedKeyword = expectedKeyword(keywordIdToUpdate, PHRASE_1);
        assertUpdatedKeyword(keywordIdToUpdate, expectedKeyword);
    }

    @Test
    public void execute_ParsableButInvalidPhraseFixed_PhraseSavedCorrectly() {
        createOneActiveAdGroup();
        String invalidPhrase = "парсится но содержит -минус фразу";
        Long keywordIdToUpdate =
                keywordSteps.createKeyword(adGroupInfo1, defaultKeyword()
                        .withPhrase(invalidPhrase)
                        .withPriceContext(INIT_PRICE)
                        .withPrice(INIT_PRICE)
                        .withAutobudgetPriority(3)
                        .withPlace(Place.PREMIUM1)).getId();

        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        Keyword expectedKeyword = expectedKeyword(keywordIdToUpdate, PHRASE_1);
        assertUpdatedKeyword(keywordIdToUpdate, expectedKeyword);
    }

    @Test
    public void execute_PhraseIsNotChangedAndUnparsableNormPhraseFixed_PhraseSavedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate =
                keywordSteps.createKeyword(adGroupInfo1, defaultKeyword()
                        .withPhrase(PHRASE_1)
                        .withNormPhrase(PHRASE_1 + "%^#*(")
                        .withPriceContext(INIT_PRICE)
                        .withPrice(INIT_PRICE)
                        .withAutobudgetPriority(3)
                        .withPlace(Place.PREMIUM1)).getId();

        Long newPrice = 10L;
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_1, newPrice));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        Keyword expectedKeyword = expectedKeyword(keywordIdToUpdate, PHRASE_1)
                .withPrice(BigDecimal.valueOf(newPrice).setScale(2, RoundingMode.DOWN))
                .withPlace(Place.ROTATION);
        assertUpdatedKeyword(keywordIdToUpdate, expectedKeyword);
    }

    @Test
    public void execute_PhraseIsChangedAndUnparsableNormPhraseFixed_PhraseSavedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate =
                keywordSteps.createKeyword(adGroupInfo1, defaultKeyword()
                        .withPhrase(PHRASE_1)
                        .withNormPhrase(PHRASE_1 + "%^#*(")
                        .withPriceContext(INIT_PRICE)
                        .withPrice(INIT_PRICE)
                        .withAutobudgetPriority(3)
                        .withPlace(Place.PREMIUM1)).getId();

        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_2)));

        Keyword expectedKeyword = expectedKeyword(keywordIdToUpdate, PHRASE_2);
        assertUpdatedKeyword(keywordIdToUpdate, expectedKeyword);
    }

    // сохранение hrefParam1 и hrefParam2

    @Test
    public void execute_ChangeHrefParams_PhraseSavedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createDefaultKeyword(adGroupInfo1, PHRASE_1).getId();

        String hrefParam1 = "123";
        String hrefParam2 = "второй";
        ModelChanges<Keyword> changesKeywords = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(hrefParam1, Keyword.HREF_PARAM1)
                .process(hrefParam2, Keyword.HREF_PARAM2);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeywords));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        Keyword expectedKeyword = expectedKeyword(keywordIdToUpdate, PHRASE_1)
                .withHrefParam1(hrefParam1)
                .withHrefParam2(hrefParam2)
                .withPlace(null);
        assertUpdatedKeyword(keywordIdToUpdate, expectedKeyword);
    }

    // сохранение финансовых полей

    @Test
    public void execute_OnePhraseWithOnlyChangeSearchPrice_MoneyFieldsSavedCorrectly() {
        createOneActiveAdGroup();
        makeCampaignManual(adGroupInfo1.getCampaignInfo());
        Long keywordIdToUpdate = createDefaultKeyword(adGroupInfo1, PHRASE_1).getId();

        ModelChanges<Keyword> changesKeywords = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(DEFAULT_PRICE_CHANGE, Keyword.PRICE)
                .process(null, Keyword.PRICE_CONTEXT)
                .process(null, Keyword.AUTOBUDGET_PRIORITY);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeywords));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        Keyword expectedKeyword = expectedKeyword(keywordIdToUpdate, PHRASE_1)
                .withPrice(DEFAULT_PRICE_CHANGE)
                .withPriceContext(null)
                .withAutobudgetPriority(null);
        assertUpdatedKeyword(keywordIdToUpdate, expectedKeyword);
    }

    @Test
    public void execute_OnePhraseWithOnlyChangeContextPrice_MoneyFieldsSavedCorrectly() {
        createOneActiveAdGroup();
        makeCampaignManualContextOnly(adGroupInfo1.getCampaignInfo());
        Long keywordIdToUpdate = createDefaultKeyword(adGroupInfo1, PHRASE_1).getId();

        ModelChanges<Keyword> changesKeywords = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(null, Keyword.PRICE)
                .process(DEFAULT_PRICE_CHANGE, Keyword.PRICE_CONTEXT)
                .process(null, Keyword.AUTOBUDGET_PRIORITY);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeywords));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        Keyword expectedKeyword = expectedKeyword(keywordIdToUpdate, PHRASE_1)
                .withPrice(null)
                .withPriceContext(DEFAULT_PRICE_CHANGE)
                .withAutobudgetPriority(null)
                .withPlace(Place.ROTATION);
        assertUpdatedKeyword(keywordIdToUpdate, expectedKeyword);
    }

    @Test
    public void execute_OnePhraseWithChangeSearchAndContextPrice_MoneyFieldsSavedCorrectly() {
        createOneActiveAdGroup();
        makeCampaignManualWithSeparatedControl(adGroupInfo1.getCampaignInfo());
        Long keywordIdToUpdate = createDefaultKeyword(adGroupInfo1, PHRASE_1).getId();

        BigDecimal priceSearch =
                BigDecimal.valueOf(100).setScale(2, RoundingMode.DOWN);
        BigDecimal priceContext = DEFAULT_PRICE_CHANGE;
        ModelChanges<Keyword> changesKeywords = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(priceSearch, Keyword.PRICE)
                .process(priceContext, Keyword.PRICE_CONTEXT)
                .process(null, Keyword.AUTOBUDGET_PRIORITY);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeywords));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        Keyword expectedKeyword = expectedKeyword(keywordIdToUpdate, PHRASE_1)
                .withPrice(priceSearch)
                .withPriceContext(priceContext)
                .withAutobudgetPriority(null);
        assertUpdatedKeyword(keywordIdToUpdate, expectedKeyword);
    }

    @Test
    public void execute_OnePhraseWithOnlyChangeAutobudgetPriority_MoneyFieldsSavedCorrectly() {
        createOneActiveAdGroup();
        makeCampaignAutobudget(adGroupInfo1.getCampaignInfo());
        Long keywordIdToUpdate = createDefaultKeyword(adGroupInfo1, PHRASE_1).getId();

        ModelChanges<Keyword> changesKeywords = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(null, Keyword.PRICE)
                .process(null, Keyword.PRICE_CONTEXT)
                .process(5, Keyword.AUTOBUDGET_PRIORITY);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeywords));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        Keyword expectedKeyword = expectedKeyword(keywordIdToUpdate, PHRASE_1)
                .withPrice(null)
                .withPriceContext(null)
                .withAutobudgetPriority(5)
                .withPlace(Place.ROTATION);
        assertUpdatedKeyword(keywordIdToUpdate, expectedKeyword);
    }

    @Test
    public void execute_ChangeBothPricesAndPriorityDefinedOnManualStrategy_AllFieldsSaved() {
        createOneActiveAdGroup();
        makeCampaignManual(adGroupInfo1.getCampaignInfo());
        Long keywordIdToUpdate = createDefaultKeyword(adGroupInfo1, PHRASE_1).getId();

        int autobudgetPriority = 5;
        BigDecimal searchPrice = BigDecimal.valueOf(100L).setScale(2, RoundingMode.DOWN);
        ModelChanges<Keyword> changesKeywords = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(searchPrice, Keyword.PRICE)
                .process(DEFAULT_PRICE_CHANGE, Keyword.PRICE_CONTEXT)
                .process(autobudgetPriority, Keyword.AUTOBUDGET_PRIORITY);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeywords));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        Keyword expectedKeyword = expectedKeyword(keywordIdToUpdate, PHRASE_1)
                .withPrice(searchPrice)
                .withPriceContext(DEFAULT_PRICE_CHANGE)
                .withAutobudgetPriority(autobudgetPriority);
        assertUpdatedKeyword(keywordIdToUpdate, expectedKeyword);
    }

    @Test
    public void execute_ChangeBothPricesAndPriorityDefinedOnAutoStrategy_AllFieldsSaved() {
        createOneActiveAdGroup();
        makeCampaignAutobudget(adGroupInfo1.getCampaignInfo());
        Long keywordIdToUpdate = createDefaultKeyword(adGroupInfo1, PHRASE_1).getId();

        int autobudgetPriority = 5;
        BigDecimal searchPrice = BigDecimal.valueOf(100L).setScale(2, RoundingMode.DOWN);
        ModelChanges<Keyword> changesKeywords = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(searchPrice, Keyword.PRICE)
                .process(DEFAULT_PRICE_CHANGE, Keyword.PRICE_CONTEXT)
                .process(autobudgetPriority, Keyword.AUTOBUDGET_PRIORITY);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeywords));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        Keyword expectedKeyword = expectedKeyword(keywordIdToUpdate, PHRASE_1)
                .withPrice(searchPrice)
                .withPriceContext(DEFAULT_PRICE_CHANGE)
                .withAutobudgetPriority(autobudgetPriority);
        assertUpdatedKeyword(keywordIdToUpdate, expectedKeyword);
    }

    @Test
    public void execute_ChangeSearchPriceIsOnPremium2PlaceAndContextPriceIsOnPremium1Place_PlaceComputedOverSearchPriceCorrectly() {
        createOneActiveAdGroup();
        makeCampaignManual(adGroupInfo1.getCampaignInfo());
        Long keywordIdToUpdate = createDefaultKeyword(adGroupInfo1, PHRASE_1).getId();

        int autobudgetPriority = 5;
        BigDecimal searchPrice = BigDecimal.valueOf(47L).setScale(2, RoundingMode.DOWN);
        BigDecimal contextPrice = BigDecimal.valueOf(200L).setScale(2, RoundingMode.DOWN);
        ModelChanges<Keyword> changesKeywords = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(searchPrice, Keyword.PRICE)
                .process(contextPrice, Keyword.PRICE_CONTEXT)
                .process(autobudgetPriority, Keyword.AUTOBUDGET_PRIORITY);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeywords));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        Keyword expectedKeyword = expectedKeyword(keywordIdToUpdate, PHRASE_1)
                .withPrice(searchPrice)
                .withPriceContext(contextPrice)
                .withAutobudgetPriority(autobudgetPriority)
                .withPlace(Place.PREMIUM2);
        assertUpdatedKeyword(keywordIdToUpdate, expectedKeyword);
    }

    // сохранение в несколько групп, кампаний

    @Test
    public void execute_ChangePhrase_SaveToDifferentAdGroupsInOneCampaign_DataSavedCorrectly() {
        createTwoActiveAdGroups();
        Long keywordIdToUpdate1 = createDefaultKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createDefaultKeyword(adGroupInfo2, PHRASE_1).getId();

        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, PHRASE_2),
                keywordModelChanges(keywordIdToUpdate2, PHRASE_2));

        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(
                isUpdated(keywordIdToUpdate1, PHRASE_2),
                isUpdated(keywordIdToUpdate2, PHRASE_2)));

        Keyword expectedKeyword1 = expectedKeyword(keywordIdToUpdate1, PHRASE_2);
        assertUpdatedKeyword(keywordIdToUpdate1, expectedKeyword1);

        Keyword expectedKeyword2 = expectedKeyword(keywordIdToUpdate2, PHRASE_2)
                .withAdGroupId(adGroupInfo2.getAdGroupId());
        assertUpdatedKeyword(keywordIdToUpdate2, expectedKeyword2);
    }

    @Test
    public void execute_ChangePhrase_SaveToDifferentCampaigns_DataSavedCorrectly() {
        createOneActiveAdGroup();
        AdGroupInfo adGroupInfo2 = steps.adGroupSteps().createActiveTextAdGroup(adGroupInfo1.getClientInfo());
        Long keywordIdToUpdate1 = createDefaultKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createDefaultKeyword(adGroupInfo2, PHRASE_1).getId();

        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, PHRASE_2),
                keywordModelChanges(keywordIdToUpdate2, PHRASE_2));

        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(
                isUpdated(keywordIdToUpdate1, PHRASE_2),
                isUpdated(keywordIdToUpdate2, PHRASE_2)));

        Keyword expectedKeyword1 = expectedKeyword(keywordIdToUpdate1, PHRASE_2);
        assertUpdatedKeyword(keywordIdToUpdate1, expectedKeyword1);

        Keyword expectedKeyword2 = expectedKeyword(keywordIdToUpdate2, PHRASE_2)
                .withAdGroupId(adGroupInfo2.getAdGroupId())
                .withCampaignId(adGroupInfo2.getCampaignId());
        assertUpdatedKeyword(keywordIdToUpdate2, expectedKeyword2);
    }

    private void assertUpdatedKeyword(Long id, Keyword expected) {
        Keyword actual = getKeyword(id);
        assertThat("сохраненная ключевая фраза отличается от ожидаемой",
                actual, beanDiffer(expected).useCompareStrategy(COMPARE_STRATEGY));
    }

    private Keyword expectedKeyword(Long id, String phrase) {
        KeywordWithMinuses parsedKeyword = KeywordParser.parseWithMinuses(phrase);
        KeywordWithMinuses parsedNormalKeyword = keywordNormalizer.normalizeKeywordWithMinuses(parsedKeyword);
        return new Keyword()
                .withId(id)
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withCampaignId(adGroupInfo1.getCampaignId())
                .withPhraseBsId(BigInteger.ZERO)
                .withPhrase(parsedKeyword.toString())
                .withIsAutotargeting(false)
                .withPrice(INIT_PRICE)
                .withPriceContext(INIT_PRICE)
                .withAutobudgetPriority(3)
                .withNormPhrase(parsedNormalKeyword.getKeyword().toString())
                .withWordsCount(parsedNormalKeyword.getKeyword().getAllKeywords().size())
                .withPlace(Place.PREMIUM1)
                .withStatusModerate(StatusModerate.NEW)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withNeedCheckPlaceModified(false)
                .withShowsForecast(0L)
                .withPhraseIdHistory(null)
                .withIsSuspended(false);
    }

    private KeywordInfo createDefaultKeyword(AdGroupInfo adGroupInfo, String phrase) {
        KeywordWithMinuses parsedKeyword = KeywordParser.parseWithMinuses(phrase);
        KeywordWithMinuses parsedNormalKeyword = keywordNormalizer.normalizeKeywordWithMinuses(parsedKeyword);
        return steps.keywordSteps()
                .createKeyword(adGroupInfo,
                        defaultKeyword().
                                withPrice(INIT_PRICE)
                                .withPriceContext(INIT_PRICE)
                                .withAutobudgetPriority(3)
                                .withPhrase(parsedKeyword.toString())
                                .withNormPhrase(parsedNormalKeyword.toString())
                                .withWordsCount(parsedNormalKeyword.getKeyword().getAllKeywords().size()));

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
