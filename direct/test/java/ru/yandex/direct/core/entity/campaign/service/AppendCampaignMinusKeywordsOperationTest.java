package ru.yandex.direct.core.entity.campaign.service;

import java.math.BigInteger;
import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.advq.query.ast.WordKind;
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.MinusKeywordPreparingTool;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.campaign.container.CampaignNewMinusKeywords;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.UpdateCampaignValidationService;
import ru.yandex.direct.core.entity.keyword.processing.KeywordNormalizer;
import ru.yandex.direct.core.entity.keyword.processing.KeywordStopwordsFixer;
import ru.yandex.direct.core.entity.keyword.repository.KeywordCacheRepository;
import ru.yandex.direct.core.entity.keyword.repository.internal.DbAddedPhraseType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.utils.HashingUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.apache.commons.lang3.StringUtils.deleteWhitespace;
import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.CAMPAIGN_MINUS_KEYWORDS_MAX_LENGTH;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AppendCampaignMinusKeywordsOperationTest {

    private static final String OLD_MINUS_KEYWORD_1 = "старая фраза " + randomNumeric(5);
    private static final String OLD_MINUS_KEYWORD_2 = "бородатый анекдот " + randomNumeric(5);

    private static final String NEW_MINUS_KEYWORD_1 = "новая фраза " + randomNumeric(5);
    private static final String NEW_MINUS_KEYWORD_2 = "мемасик месяца " + randomNumeric(5);

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private BannerCommonRepository bannerCommonRepository;

    @Autowired
    private KeywordCacheRepository keywordCacheRepository;

    @Autowired
    private UpdateCampaignValidationService updateCampaignValidationService;

    @Autowired
    private MinusKeywordPreparingTool minusKeywordPreparingTool;

    @Autowired
    private KeywordStopwordsFixer keywordStopwordsFixer;

    @Autowired
    private KeywordNormalizer keywordNormalizer;

    private ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignWithKeywords;
    private ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignWithoutKeywords;
    private long operatorUid;
    private ClientId clientId;
    private int shard;

    @Before
    public void before() {
        campaignWithKeywords = TestCampaigns.activeTextCampaign(null, null)
                .withMinusKeywords(asList(OLD_MINUS_KEYWORD_1, OLD_MINUS_KEYWORD_2));
        campaignWithoutKeywords = TestCampaigns.activeTextCampaign(null, null)
                .withMinusKeywords(null);

        CampaignInfo campaignInfo1 = steps.campaignSteps().createCampaign(campaignWithKeywords);
        steps.campaignSteps().createCampaign(campaignWithoutKeywords, campaignInfo1.getClientInfo());

        operatorUid = campaignInfo1.getUid();
        clientId = campaignInfo1.getClientId();
        shard = campaignInfo1.getShard();
    }

    // возвращаемый результат при добавлении минус-фраз в одну кампанию

    @Test
    public void prepareAndApply_OneValidItemForNullMinusKeywords_ResultIsExpected() {
        updateOneItemAndAssert(createValidNewKeywords(campaignWithoutKeywords.getId()), true);
    }

    @Test
    public void prepareAndApply_OneValidItemForExistingMinusKeywords_ResultIsExpected() {
        updateOneItemAndAssert(createValidNewKeywords(campaignWithKeywords.getId()), true);
    }

    @Test
    public void prepareAndApply_OneItemWithFailedOnPreValidationOfMinusKeyword_ResultIsExpected() {
        updateOneItemAndAssert(createNewKeywordsWithInvalidItem(campaignWithKeywords.getId()), false);
    }

    @Test
    public void prepareAndApply_OneItemWithFailedOnValidationOfMinusKeyword_ResultIsExpected() {
        updateOneItemAndAssert(createTooLongNewKeywords(campaignWithoutKeywords.getId()), false);
    }

    // корректность сохраняемых данных при добавлении минус-фраз в одну кампанию

    // должен проверять, что была выполнена предобработка отдельных минус-фраз,
    // удаление дублей и сортировка
    @Test
    public void prepareAndApply_OneValidItem_SavedMinusKeywordsArePreparedBeforeSaving() {
        List<String> rawMinusKeywords = asList("купит слона", "как пройти в библиотеку ", "!купил слона", "бизнес");
        List<String> expectedPreparedMinusKeywords = asList("!как пройти !в библиотеку", "бизнес", "купит слона");
        CampaignNewMinusKeywords newMinusKeywords =
                new CampaignNewMinusKeywords(campaignWithoutKeywords.getId(), rawMinusKeywords);

        updateOneItemAndAssumeSuccess(newMinusKeywords);
        Campaign campaignAfterUpdate = getCampaign(shard, campaignWithoutKeywords.getId());
        assertThat("минус-слова после обновления не соответствуют ожидаемым",
                campaignAfterUpdate.getMinusKeywords(),
                BeanDifferMatcher.beanDiffer(expectedPreparedMinusKeywords));
    }

    @Test
    public void prepareAndApply_OneValidItemForNullMinusKeywords_SavedMinusKeywordsIsCorrect() {
        updateOneItemAndAssumeSuccess(createValidNewKeywords(campaignWithoutKeywords.getId()));
        Campaign campaignAfterUpdate = getCampaign(shard, campaignWithoutKeywords.getId());
        assertThat("минус-слова после обновления не соответствуют ожидаемым",
                campaignAfterUpdate.getMinusKeywords(),
                contains(NEW_MINUS_KEYWORD_2, NEW_MINUS_KEYWORD_1));
    }

    @Test
    public void prepareAndApply_OneValidItemForExistingMinusKeywords_SavedMinusKeywordsIsCorrect() {
        updateOneItemAndAssumeSuccess(createValidNewKeywords(campaignWithKeywords.getId()));
        Campaign campaignAfterUpdate = getCampaign(shard, campaignWithKeywords.getId());

        assertThat("минус-слова после обновления не соответствуют ожидаемым",
                campaignAfterUpdate.getMinusKeywords(),
                contains(OLD_MINUS_KEYWORD_2, NEW_MINUS_KEYWORD_2, NEW_MINUS_KEYWORD_1, OLD_MINUS_KEYWORD_1));
    }

    @Test
    public void prepareAndApply_OneValidItem_MinusKeywordsAreAddedToCache() {
        long campaignId = campaignWithKeywords.getId();
        CampaignNewMinusKeywords campaignNewMinusKeywords = new CampaignNewMinusKeywords(campaignId,
                asList("купить слона Москва", "пельмени по-русски", "васька санкт-петербург"));
        updateOneItemAndAssumeSuccess(campaignNewMinusKeywords);

        List<String> normalMinusKeywords =
                minusKeywordPreparingTool.preprocess(campaignNewMinusKeywords.getMinusKeywords());
        normalMinusKeywords = keywordStopwordsFixer
                .unquoteAndFixStopwords(normalMinusKeywords, WordKind.PLUS);
        normalMinusKeywords = keywordNormalizer.normalizeKeywords(normalMinusKeywords);
        List<BigInteger> expectedMinusKeywordsCache = StreamEx.of(normalMinusKeywords)
                .map(HashingUtils::getMd5HalfHashUtf8)
                .toList();

        List<BigInteger> actualMinusKeywordsCache = keywordCacheRepository.getCampaignCachedKeywords(shard,
                DbAddedPhraseType.MINUS, singletonList(campaignId)).get(campaignId);
        assertThat(actualMinusKeywordsCache, containsInAnyOrder(expectedMinusKeywordsCache.toArray()));
    }

    // возвращаемый результат при добавлении минус-фраз в две группы

    @Test
    public void prepareAndApply_TwoValidItems_ResultIsExpected() {
        CampaignNewMinusKeywords newMinusKeywords1 =
                new CampaignNewMinusKeywords(campaignWithKeywords.getId(), singletonList(NEW_MINUS_KEYWORD_1));
        CampaignNewMinusKeywords newMinusKeywords2 =
                new CampaignNewMinusKeywords(campaignWithoutKeywords.getId(), singletonList(NEW_MINUS_KEYWORD_2));
        updateManyItemsAndAssert(asList(newMinusKeywords1, newMinusKeywords2), true, true);
    }

    @Test
    public void prepareAndApply_OneItemIsValidAndOneIsFailed_ResultIsExpected() {
        CampaignNewMinusKeywords newMinusKeywords =
                new CampaignNewMinusKeywords(campaignWithKeywords.getId(), singletonList(NEW_MINUS_KEYWORD_1));
        CampaignNewMinusKeywords longNewMinusKeywords = createTooLongNewKeywords(campaignWithoutKeywords.getId());

        updateManyItemsAndAssert(asList(newMinusKeywords, longNewMinusKeywords), true, false);
    }

    // неизменность данных при ошибке в одном из элементов при обновлении нескольких групп

    @Test
    public void prepareAndApply_OneItemIsValidAndOneIsFailedOnPreValidation_ValidChangesIsNotApplied() {
        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignForValidChanges = campaignWithoutKeywords;
        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignForInvalidCanges = campaignWithKeywords;

        CampaignNewMinusKeywords newMinusKeywords1 = createValidNewKeywords(campaignForValidChanges.getId());
        CampaignNewMinusKeywords newMinusKeywords2 = createNewKeywordsWithInvalidItem(campaignForInvalidCanges.getId());

        updateManyItemsAndAssume(asList(newMinusKeywords1, newMinusKeywords2), true, false);

        Campaign validCampaignAfterUpdate = getCampaign(shard, campaignForValidChanges.getId());
        assertThat("в кампанию не должны быть добавлены валидные минус-фразы, "
                        + "когда в той же операции присутствуют невалидные минус-фразы для других кампаний",
                validCampaignAfterUpdate.getMinusKeywords(), empty());
    }

    @Test
    public void prepareAndApply_OneItemIsValidAndOneIsFailedOnValidation_ValidChangesIsNotApplied() {
        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignForValidChanges = campaignWithoutKeywords;
        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignForInvalidChanges = campaignWithKeywords;

        CampaignNewMinusKeywords campaignNewMinusKeywords1 =
                new CampaignNewMinusKeywords(campaignForValidChanges.getId(), singletonList(NEW_MINUS_KEYWORD_1));
        CampaignNewMinusKeywords campaignNewMinusKeywords2 =
                createTooLongNewKeywords(campaignForInvalidChanges.getId());

        updateManyItemsAndAssume(asList(campaignNewMinusKeywords1, campaignNewMinusKeywords2), true, false);

        Campaign campaignAfterUpdate = getCampaign(shard, campaignForValidChanges.getId());
        assertThat("в кампанию не должны быть добавлены валидные минус-фразы, "
                        + "когда в той же операции присутствуют невалидные минус-фразы для других кампаний",
                campaignAfterUpdate.getMinusKeywords(), empty());
    }

    // getActualAddedMinusKeywordsCount

    @Test
    public void getActualAddedMinusKeywordsCount_OneItemWithoutDuplicates_ReturnsValidCount() {
        long campaignId = campaignWithoutKeywords.getId();
        CampaignNewMinusKeywords keywordsWithoutDuplicates = createValidNewKeywords(campaignId);

        AppendCampaignMinusKeywordsOperation operation = updateOneItemAndAssumeSuccess(keywordsWithoutDuplicates);

        Integer addedKeywordsCount = operation.getActualAddedMinusKeywordsCount().get(campaignId);
        assertThat("возвращенное количество добавленных минус-фраз не соответствует ожидаемому",
                addedKeywordsCount, is(2));
    }

    @Test
    public void getActualAddedMinusKeywordsCount_OneItemWithDuplicates_ReturnsValidCount() {
        long campaignId = campaignWithoutKeywords.getId();
        CampaignNewMinusKeywords keywordsWithDuplicates =
                new CampaignNewMinusKeywords(campaignId, asList("летние шины", "летний сад", "летняя шина"));

        AppendCampaignMinusKeywordsOperation operation = updateOneItemAndAssumeSuccess(keywordsWithDuplicates);

        Integer addedKeywordsCount = operation.getActualAddedMinusKeywordsCount().get(campaignId);
        assertThat("возвращенное количество добавленных минус-фраз не соответствует ожидаемому",
                addedKeywordsCount, is(2));
    }

    @Test
    public void getActualAddedMinusKeywordsCount_OneItemWithNoAddedKeywords_ReturnsZero() {
        long campaignId = campaignWithKeywords.getId();
        CampaignNewMinusKeywords keywordsWithDuplicates =
                new CampaignNewMinusKeywords(campaignId, asList(OLD_MINUS_KEYWORD_1, OLD_MINUS_KEYWORD_2));

        AppendCampaignMinusKeywordsOperation operation = updateOneItemAndAssumeSuccess(keywordsWithDuplicates);

        Integer addedKeywordsCount = operation.getActualAddedMinusKeywordsCount().get(campaignId);
        assertThat("возвращенное количество добавленных минус-фраз не соответствует ожидаемому",
                addedKeywordsCount, is(0));
    }

    @Test
    public void getActualAddedMinusKeywordsCount_TwoItems_ReturnsValidCount() {
        long campaignId1 = campaignWithoutKeywords.getId();
        long campaignId2 = campaignWithKeywords.getId();
        CampaignNewMinusKeywords keywordsWithoutDuplicates =
                new CampaignNewMinusKeywords(campaignId1, singletonList("летние шины"));
        CampaignNewMinusKeywords keywordsWithDuplicates =
                new CampaignNewMinusKeywords(campaignId2, asList("летние шины", "летний сад", "летняя шина"));

        AppendCampaignMinusKeywordsOperation operation =
                updateManyItemsAndAssume(asList(keywordsWithoutDuplicates, keywordsWithDuplicates), true, true);

        Integer addedKeywordsCount1 = operation.getActualAddedMinusKeywordsCount().get(campaignId1);
        Integer addedKeywordsCount2 = operation.getActualAddedMinusKeywordsCount().get(campaignId2);
        assertThat("возвращенное количество добавленных минус-фраз не соответствует ожидаемому",
                addedKeywordsCount1, is(1));
        assertThat("возвращенное количество добавленных минус-фраз не соответствует ожидаемому",
                addedKeywordsCount2, is(2));
    }

    // getSumMinusKeywordsLength

    @Test
    public void getSumMinusKeywordsLength_CampaignWithoutKeywords_ReturnsLengthOfNewKeywordsExceptDuplicates() {
        long campaignId = campaignWithoutKeywords.getId();
        CampaignNewMinusKeywords keywordsWithDuplicates =
                new CampaignNewMinusKeywords(campaignId, asList("летние шины", "летний [сад]", "летняя шина"));

        AppendCampaignMinusKeywordsOperation operation = updateOneItemAndAssumeSuccess(keywordsWithDuplicates);

        Integer sumMinusKeywordLength = operation.getSumMinusKeywordsLength().get(campaignId);
        // добавляются 2 фразы: ["летний [сад]", "летняя шина"] -> 19 символов без учета спец-символов и пробелов
        assertThat("возвращаемая общая длина минус фраз кампании после обновления не соответствует ожидаемой",
                sumMinusKeywordLength, is(19));
    }

    @Test
    public void getSumMinusKeywordsLength_CampaignWithKeywords_ReturnsLengthOfAllKeywordsExceptDuplicates() {
        long campaignId = campaignWithKeywords.getId();
        CampaignNewMinusKeywords keywordsWithDuplicates =
                new CampaignNewMinusKeywords(campaignId, asList("летние шины", "летний [сад]", "летняя шина"));

        AppendCampaignMinusKeywordsOperation operation = updateOneItemAndAssumeSuccess(keywordsWithDuplicates);

        Integer sumMinusKeywordLength = operation.getSumMinusKeywordsLength().get(campaignId);
        // добавляются 2 фразы: ["летний [сад]", "летняя шина"] -> 19 символов без учета спец-символов и пробелов
        Integer expectedSumMinusKeywordLength =
                19 + deleteWhitespace(OLD_MINUS_KEYWORD_1).length() + deleteWhitespace(OLD_MINUS_KEYWORD_2).length();
        assertThat("возвращаемая общая длина минус фраз кампании после обновления не соответствует ожидаемой",
                sumMinusKeywordLength, is(expectedSumMinusKeywordLength));
    }

    @Test
    public void getSumMinusKeywordsLength_TwoItems_ReturnsValidLengthForEachItem() {
        long campaignId1 = campaignWithoutKeywords.getId();
        CampaignNewMinusKeywords keywordsWithDuplicates1 =
                new CampaignNewMinusKeywords(campaignId1, asList("летние шины", "летний [сад]", "летняя шина"));
        long campaignId2 = campaignWithKeywords.getId();
        CampaignNewMinusKeywords keywordsWithDuplicates2 =
                new CampaignNewMinusKeywords(campaignId2, asList("летние шины", "летний [сад]", "летняя шина"));

        AppendCampaignMinusKeywordsOperation operation =
                updateManyItemsAndAssume(asList(keywordsWithDuplicates1, keywordsWithDuplicates2), true, true);

        Integer sumMinusKeywordLength1 = operation.getSumMinusKeywordsLength().get(campaignId1);
        Integer sumMinusKeywordLength2 = operation.getSumMinusKeywordsLength().get(campaignId2);
        // добавляются 2 фразы: ["летний [сад]", "летняя шина"] -> 19 символов без учета спец-символов и пробелов
        Integer expectedSumMinusKeywordLength1 = 19;
        Integer expectedSumMinusKeywordLength2 =
                19 + deleteWhitespace(OLD_MINUS_KEYWORD_1).length() + deleteWhitespace(OLD_MINUS_KEYWORD_2).length();
        assertThat("возвращаемая общая длина минус фраз кампании после обновления не соответствует ожидаемой",
                sumMinusKeywordLength1, is(expectedSumMinusKeywordLength1));
        assertThat("возвращаемая общая длина минус фраз кампании после обновления не соответствует ожидаемой",
                sumMinusKeywordLength2, is(expectedSumMinusKeywordLength2));
    }

    private void updateOneItemAndAssert(CampaignNewMinusKeywords newMinusKeywords,
                                        boolean isElemSuccessful) {
        updateManyItemsAndAssert(singletonList(newMinusKeywords), isElemSuccessful);
    }

    private AppendCampaignMinusKeywordsOperation updateOneItemAndAssumeSuccess(
            CampaignNewMinusKeywords newMinusKeywords) {
        return updateManyItemsAndAssume(singletonList(newMinusKeywords), true);
    }

    private void updateManyItemsAndAssert(
            List<CampaignNewMinusKeywords> newMinusKeywordsList, Boolean... isElemsSuccessful) {
        AppendCampaignMinusKeywordsOperation operation = createUpdateOperation(newMinusKeywordsList);
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isSuccessful(isElemsSuccessful));
    }

    private AppendCampaignMinusKeywordsOperation updateManyItemsAndAssume(
            List<CampaignNewMinusKeywords> newMinusKeywordsList, Boolean... isElemsSuccessful) {
        AppendCampaignMinusKeywordsOperation operation = createUpdateOperation(newMinusKeywordsList);
        MassResult<Long> result = operation.prepareAndApply();
        assumeThat(result, isSuccessful(isElemsSuccessful));
        return operation;
    }

    private AppendCampaignMinusKeywordsOperation createUpdateOperation(
            List<CampaignNewMinusKeywords> newMinusKeywordsList) {
        return new AppendCampaignMinusKeywordsOperation(newMinusKeywordsList,
                campaignRepository, adGroupRepository, bannerCommonRepository,
                keywordCacheRepository, updateCampaignValidationService,
                minusKeywordPreparingTool, keywordStopwordsFixer, keywordNormalizer,
                operatorUid, clientId, shard);
    }

    private CampaignNewMinusKeywords createValidNewKeywords(Long id) {
        return new CampaignNewMinusKeywords(id, asList(NEW_MINUS_KEYWORD_1, NEW_MINUS_KEYWORD_2));
    }

    private CampaignNewMinusKeywords createNewKeywordsWithInvalidItem(Long id) {
        return new CampaignNewMinusKeywords(id, asList("[][][]", NEW_MINUS_KEYWORD_2));
    }

    private CampaignNewMinusKeywords createTooLongNewKeywords(Long id) {
        List<String> minusKeywords = singletonList(leftPad("word", CAMPAIGN_MINUS_KEYWORDS_MAX_LENGTH + 1, " other"));
        return new CampaignNewMinusKeywords(id, minusKeywords);
    }

    public Campaign getCampaign(int shard, long id) {
        return campaignRepository.getCampaigns(shard, singletonList(id)).iterator().next();
    }
}
