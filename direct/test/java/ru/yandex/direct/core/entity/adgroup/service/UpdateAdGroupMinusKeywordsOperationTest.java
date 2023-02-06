package ru.yandex.direct.core.entity.adgroup.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.AdGroupNewMinusKeywords;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.client.service.ClientGeoService;
import ru.yandex.direct.core.entity.keyword.processing.KeywordNormalizer;
import ru.yandex.direct.core.entity.keyword.processing.KeywordStopwordsFixer;
import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.apache.commons.lang3.StringUtils.deleteWhitespace;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.privateMinusKeywordsPack;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UpdateAdGroupMinusKeywordsOperationTest {

    private static final String OLD_MINUS_KEYWORD_1 = "старая фраза " + randomNumeric(5);
    private static final String OLD_MINUS_KEYWORD_2 = "бородатый анекдот " + randomNumeric(5);

    private static final String NEW_MINUS_KEYWORD_1 = "новая фраза " + randomNumeric(5);
    private static final String NEW_MINUS_KEYWORD_2 = "мемасик месяца " + randomNumeric(5);

    @Autowired
    private Steps steps;
    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private MinusKeywordPreparingTool minusKeywordPreparingTool;

    @Autowired
    private KeywordStopwordsFixer keywordStopwordsFixer;

    @Autowired
    private KeywordNormalizer keywordNormalizer;

    @Autowired
    private GeoTreeFactory geoTreeFactory;

    @Autowired
    ClientGeoService clientGeoService;

    @Autowired
    private UpdateAdGroupMinusKeywordsOperationFactory appendAdGroupMinusKeywordsOperationFactory;

    private MinusKeywordsPack adGroupMinusKeywordsPack;
    private AdGroup adGroupWithKeywords;
    private AdGroup adGroupWithoutKeywords;
    private GeoTree geoTree;
    private long operatorUid;
    private ClientId clientId;
    private int shard;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        adGroupMinusKeywordsPack = steps.minusKeywordsPackSteps().createMinusKeywordsPack(
                privateMinusKeywordsPack().withMinusKeywords(asList(OLD_MINUS_KEYWORD_1, OLD_MINUS_KEYWORD_2)),
                clientInfo).getMinusKeywordsPack();
        adGroupWithKeywords = activeTextAdGroup(null).withMinusKeywordsId(adGroupMinusKeywordsPack.getId());
        adGroupWithoutKeywords = activeTextAdGroup(null);

        adGroupSteps.createAdGroup(adGroupWithKeywords, clientInfo);
        adGroupSteps.createAdGroup(adGroupWithoutKeywords, clientInfo);

        geoTree = geoTreeFactory.getGlobalGeoTree();

        operatorUid = clientInfo.getUid();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
    }

    // возвращаемый результат при добавлении минус-фраз в одну группу

    @Test
    public void prepareAndApply_OneValidItemForNullMinusKeywords_ResultIsExpected() {
        updateOneItemAndAssert(createValidNewKeywords(adGroupWithoutKeywords.getId()), true);
    }

    @Test
    public void prepareAndApply_OneValidItemForExistingMinusKeywords_ResultIsExpected() {
        updateOneItemAndAssert(createValidNewKeywords(adGroupWithKeywords.getId()), true);
    }

    @Test
    public void prepareAndApply_OneItemWithFailedOnPreValidationOfMinusKeyword_ResultIsExpected() {
        updateOneItemAndAssert(createNewKeywordsWithInvalidItem(adGroupWithKeywords.getId()), false);
    }

    @Test
    public void prepareAndApply_OneItemWithFailedOnValidationOfMinusKeyword_ResultIsExpected() {
        updateOneItemAndAssert(createTooLongNewKeywords(adGroupWithoutKeywords.getId()), false);
    }

    // корректность сохраняемых данных при добавлении минус-фраз в одну группу

    // должен проверять, что была выполнена предобработка отдельных минус-фраз,
    // удаление дублей и сортировка
    @Test
    public void prepareAndApply_OneValidItem_SavedMinusKeywordsArePreparedBeforeSaving() {
        List<String> rawMinusKeywords = asList("купит слона", "как пройти в библиотеку ", "!купил слона", "бизнес");
        List<String> expectedPreparedMinusKeywords = asList("!как пройти !в библиотеку", "бизнес", "купит слона");
        AdGroupNewMinusKeywords adGroupNewKeywords =
                new AdGroupNewMinusKeywords(adGroupWithoutKeywords.getId(), rawMinusKeywords);

        updateOneItemAndAssumeSuccess(adGroupNewKeywords);
        AdGroup actualAdGroup =
                adGroupRepository.getAdGroups(shard, singletonList(adGroupWithoutKeywords.getId())).get(0);
        assertThat("минус-слова в группе после обновления не соответствуют ожидаемым",
                actualAdGroup.getMinusKeywords(),
                beanDiffer(expectedPreparedMinusKeywords));
    }

    @Test
    public void prepareAndApply_OneValidItemForNullMinusKeywords_SavedMinusKeywordsIsCorrect() {
        updateOneItemAndAssumeSuccess(createValidNewKeywords(adGroupWithoutKeywords.getId()));
        AdGroup actualAdGroup =
                adGroupRepository.getAdGroups(shard, singletonList(adGroupWithoutKeywords.getId())).get(0);
        assertThat("минус-слова в группе после обновления не соответствуют ожидаемым",
                actualAdGroup.getMinusKeywords(),
                contains(NEW_MINUS_KEYWORD_2, NEW_MINUS_KEYWORD_1));
    }

    @Test
    public void prepareAndApply_OneValidItemForExistingMinusKeywords_SavedMinusKeywordsIsCorrect() {
        updateOneItemAndAssumeSuccess(createValidNewKeywords(adGroupWithKeywords.getId()));
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupWithKeywords.getId())).get(0);
        assertThat("минус-слова в группе после обновления не соответствуют ожидаемым",
                actualAdGroup.getMinusKeywords(),
                contains(OLD_MINUS_KEYWORD_2, NEW_MINUS_KEYWORD_2, NEW_MINUS_KEYWORD_1, OLD_MINUS_KEYWORD_1));
    }

    // возвращаемый результат при добавлении минус-фраз в две группы

    @Test
    public void prepareAndApply_TwoValidItems_ResultIsExpected() {
        AdGroupNewMinusKeywords adGroupNewKeywords1 =
                new AdGroupNewMinusKeywords(adGroupWithKeywords.getId(), singletonList(NEW_MINUS_KEYWORD_1));
        AdGroupNewMinusKeywords adGroupNewKeywords2 =
                new AdGroupNewMinusKeywords(adGroupWithoutKeywords.getId(), singletonList(NEW_MINUS_KEYWORD_2));
        updateManyItemsAndAssert(asList(adGroupNewKeywords1, adGroupNewKeywords2), true, true);
    }

    @Test
    public void prepareAndApply_OneItemIsValidAndOneIsFailed_ResultIsExpected() {
        AdGroupNewMinusKeywords adGroupNewKeywords1 =
                new AdGroupNewMinusKeywords(adGroupWithKeywords.getId(), singletonList(NEW_MINUS_KEYWORD_1));
        AdGroupNewMinusKeywords adGroupNewKeywords2 = createTooLongNewKeywords(adGroupWithoutKeywords.getId());

        updateManyItemsAndAssert(asList(adGroupNewKeywords1, adGroupNewKeywords2), true, false);
    }

    // неизменность данных при ошибке в одном из элементов при обновлении нескольких групп

    @Test
    public void prepareAndApply_OneItemIsValidAndOneIsFailedOnPreValidation_ValidChangesIsNotApplied() {
        AdGroup adGroupForValidChanges = adGroupWithoutKeywords;
        AdGroup adGroupForInvalidChanges = adGroupWithKeywords;
        AdGroupNewMinusKeywords adGroupNewKeywords1 = createValidNewKeywords(adGroupForValidChanges.getId());
        AdGroupNewMinusKeywords adGroupNewKeywords2 =
                createNewKeywordsWithInvalidItem(adGroupForInvalidChanges.getId());

        updateManyItemsAndAssume(asList(adGroupNewKeywords1, adGroupNewKeywords2), true, false);

        AdGroup actualAdGroupForValidChanges =
                adGroupRepository.getAdGroups(shard, singletonList(adGroupForValidChanges.getId())).get(0);
        assertThat("в группу не должны быть добавлены валидные минус-фразы, "
                        + "когда в той же операции присутствуют невалидные минус-фразы для других групп",
                actualAdGroupForValidChanges.getMinusKeywords(), empty());
    }

    @Test
    public void prepareAndApply_PartialAndMinusKeywordsWithPreValidationError_SingleError() {
        AdGroup adGroupForValidChanges = adGroupWithoutKeywords;
        AdGroup adGroupForInvalidChanges = adGroupWithKeywords;
        AdGroupNewMinusKeywords adGroupNewKeywords1 = createValidNewKeywords(adGroupForValidChanges.getId());
        AdGroupNewMinusKeywords adGroupNewKeywords2 =
                createNewKeywordsWithInvalidItem(adGroupForInvalidChanges.getId());

        UpdateAdGroupMinusKeywordsOperation operation = appendAdGroupMinusKeywordsOperationFactory.newInstance(
                Applicability.PARTIAL,
                asList(adGroupNewKeywords1, adGroupNewKeywords2),
                geoTree,
                UpdateMinusKeywordsMode.ADD,
                operatorUid,
                clientId,
                shard);
        MassResult<Long> result = operation.prepareAndApply();
        assumeThat(result, isSuccessful(true, false));

        //noinspection unchecked
        ValidationResult<List<String>, Defect> minusKeywordsValidationResult =
                (ValidationResult<List<String>, Defect>) result.getValidationResult().getSubResults().get(index(1))
                        .getSubResults().get(field(AdGroup.MINUS_KEYWORDS));
        assertThat(minusKeywordsValidationResult.flattenErrors(), hasSize(1));
    }

    @Test
    public void prepareAndApply_OneItemIsValidAndOneIsFailedOnValidation_ValidChangesIsNotApplied() {
        AdGroup adGroupForValidChanges = adGroupWithKeywords;
        AdGroup adGroupForInvalidChanges = adGroupWithoutKeywords;
        AdGroupNewMinusKeywords adGroupNewKeywords1 =
                new AdGroupNewMinusKeywords(adGroupForValidChanges.getId(), singletonList(NEW_MINUS_KEYWORD_1));
        AdGroupNewMinusKeywords adGroupNewKeywords2 = createTooLongNewKeywords(adGroupForInvalidChanges.getId());

        updateManyItemsAndAssume(asList(adGroupNewKeywords1, adGroupNewKeywords2), true, false);

        AdGroup actualAdGroupForValidChanges =
                adGroupRepository.getAdGroups(shard, singletonList(adGroupForValidChanges.getId())).get(0);
        assertThat("в группу не должны быть добавлены валидные минус-фразы, "
                        + "когда в той же операции присутствуют невалидные минус-фразы для других групп",
                actualAdGroupForValidChanges.getMinusKeywords(),
                beanDiffer(adGroupMinusKeywordsPack.getMinusKeywords()));
    }

    // getActualAddedMinusKeywordsCount

    @Test
    public void getActualAddedMinusKeywordsCount_OneItemWithoutDuplicates_ReturnsValidCount() {
        long adGroupId = adGroupWithoutKeywords.getId();
        AdGroupNewMinusKeywords keywordsWithoutDuplicates = createValidNewKeywords(adGroupId);

        UpdateAdGroupMinusKeywordsOperation operation = updateOneItemAndAssumeSuccess(keywordsWithoutDuplicates);

        Integer addedKeywordsCount = operation.getActualAddedMinusKeywordsCount().get(adGroupId);
        assertThat("возвращенное количество добавленных минус-фраз не соответствует ожидаемому",
                addedKeywordsCount, is(2));
    }

    @Test
    public void getActualAddedMinusKeywordsCount_OneItemWithDuplicates_ReturnsValidCount() {
        long adGroupId = adGroupWithoutKeywords.getId();
        AdGroupNewMinusKeywords keywordsWithDuplicates =
                new AdGroupNewMinusKeywords(adGroupId, asList("летние шины", "летний [сад]", "летняя шина"));

        UpdateAdGroupMinusKeywordsOperation operation = updateOneItemAndAssumeSuccess(keywordsWithDuplicates);

        Integer addedKeywordsCount = operation.getActualAddedMinusKeywordsCount().get(adGroupId);
        assertThat("возвращенное количество добавленных минус-фраз не соответствует ожидаемому",
                addedKeywordsCount, is(2));
    }

    @Test
    public void getActualAddedMinusKeywordsCount_OneItemWithNoAddedKeywords_ReturnsZero() {
        long adGroupId = adGroupWithKeywords.getId();
        AdGroupNewMinusKeywords keywordsWithDuplicates =
                new AdGroupNewMinusKeywords(adGroupId, asList(OLD_MINUS_KEYWORD_1, OLD_MINUS_KEYWORD_2));

        UpdateAdGroupMinusKeywordsOperation operation = updateOneItemAndAssumeSuccess(keywordsWithDuplicates);

        Integer addedKeywordsCount = operation.getActualAddedMinusKeywordsCount().get(adGroupId);
        assertThat("возвращенное количество добавленных минус-фраз не соответствует ожидаемому",
                addedKeywordsCount, is(0));
    }

    @Test
    public void getActualAddedMinusKeywordsCount_TwoItems_ReturnsValidCount() {
        long adGroupId1 = adGroupWithoutKeywords.getId();
        long adGroupId2 = adGroupWithKeywords.getId();
        AdGroupNewMinusKeywords keywordsWithoutDuplicates =
                new AdGroupNewMinusKeywords(adGroupId1, singletonList("летние шины"));
        AdGroupNewMinusKeywords keywordsWithDuplicates =
                new AdGroupNewMinusKeywords(adGroupId2, asList("летние шины", "летний [сад]", "летняя шина"));

        UpdateAdGroupMinusKeywordsOperation operation =
                updateManyItemsAndAssume(asList(keywordsWithoutDuplicates, keywordsWithDuplicates), true, true);

        Integer addedKeywordsCount1 = operation.getActualAddedMinusKeywordsCount().get(adGroupId1);
        Integer addedKeywordsCount2 = operation.getActualAddedMinusKeywordsCount().get(adGroupId2);
        assertThat("возвращенное количество добавленных минус-фраз не соответствует ожидаемому",
                addedKeywordsCount1, is(1));
        assertThat("возвращенное количество добавленных минус-фраз не соответствует ожидаемому",
                addedKeywordsCount2, is(2));
    }

    // getSumMinusKeywordsLength

    @Test
    public void getSumMinusKeywordsLength_AdGroupWithoutKeywords_ReturnsLengthOfNewKeywordsExceptDuplicates() {
        long adGroupId = adGroupWithoutKeywords.getId();
        AdGroupNewMinusKeywords keywordsWithDuplicates =
                new AdGroupNewMinusKeywords(adGroupId, asList("летние шины", "летний [сад]", "летняя шина"));

        UpdateAdGroupMinusKeywordsOperation operation = updateOneItemAndAssumeSuccess(keywordsWithDuplicates);

        Integer sumMinusKeywordLength = operation.getSumMinusKeywordsLength().get(adGroupId);
        // добавляются 2 фразы: ["летний [сад]", "летняя шина"] -> 19 символов без учета спец-символов и пробелов
        assertThat("возвращаемая общая длина минус фраз группы после обновления не соответствует ожидаемой",
                sumMinusKeywordLength, is(19));
    }

    @Test
    public void getSumMinusKeywordsLength_AdGroupWithKeywords_ReturnsLengthOfAllKeywordsExceptDuplicates() {
        long adGroupId = adGroupWithKeywords.getId();
        AdGroupNewMinusKeywords keywordsWithDuplicates =
                new AdGroupNewMinusKeywords(adGroupId, asList("летние шины", "летний [сад]", "летняя шина"));

        UpdateAdGroupMinusKeywordsOperation operation = updateOneItemAndAssumeSuccess(keywordsWithDuplicates);

        Integer sumMinusKeywordLength = operation.getSumMinusKeywordsLength().get(adGroupId);
        // добавляются 2 фразы: ["летний [сад]", "летняя шина"] -> 19 символов без учета спец-символов и пробелов
        Integer expectedSumMinusKeywordLength =
                19 + deleteWhitespace(OLD_MINUS_KEYWORD_1).length() + deleteWhitespace(OLD_MINUS_KEYWORD_2).length();
        assertThat("возвращаемая общая длина минус фраз группы после обновления не соответствует ожидаемой",
                sumMinusKeywordLength, is(expectedSumMinusKeywordLength));
    }

    @Test
    public void getSumMinusKeywordsLength_TwoItems_ReturnsValidLengthForEachItem() {
        long adGroupId1 = adGroupWithoutKeywords.getId();
        AdGroupNewMinusKeywords keywordsWithDuplicates1 =
                new AdGroupNewMinusKeywords(adGroupId1, asList("летние шины", "летний [сад]", "летняя шина"));
        long campaignId2 = adGroupWithKeywords.getId();
        AdGroupNewMinusKeywords keywordsWithDuplicates2 =
                new AdGroupNewMinusKeywords(campaignId2, asList("летние шины", "летний [сад]", "летняя шина"));

        UpdateAdGroupMinusKeywordsOperation operation =
                updateManyItemsAndAssume(asList(keywordsWithDuplicates1, keywordsWithDuplicates2), true, true);

        Integer sumMinusKeywordLength1 = operation.getSumMinusKeywordsLength().get(adGroupId1);
        Integer sumMinusKeywordLength2 = operation.getSumMinusKeywordsLength().get(campaignId2);
        // добавляются 2 фразы: ["летний [сад]", "летняя шина"] -> 19 символов без учета спец-символов и пробелов
        Integer expectedSumMinusKeywordLength1 = 19;
        Integer expectedSumMinusKeywordLength2 =
                19 + deleteWhitespace(OLD_MINUS_KEYWORD_1).length() + deleteWhitespace(OLD_MINUS_KEYWORD_2).length();
        assertThat("возвращаемая общая длина минус фраз группы после обновления не соответствует ожидаемой",
                sumMinusKeywordLength1, is(expectedSumMinusKeywordLength1));
        assertThat("возвращаемая общая длина минус фраз группы после обновления не соответствует ожидаемой",
                sumMinusKeywordLength2, is(expectedSumMinusKeywordLength2));
    }


    private void updateOneItemAndAssert(AdGroupNewMinusKeywords adGroupNewKeywords,
                                        boolean isElemSuccessful) {
        updateManyItemsAndAssert(singletonList(adGroupNewKeywords), isElemSuccessful);
    }

    private UpdateAdGroupMinusKeywordsOperation updateOneItemAndAssumeSuccess(
            AdGroupNewMinusKeywords adGroupNewKeywords) {
        return updateManyItemsAndAssume(singletonList(adGroupNewKeywords), true);
    }

    private void updateManyItemsAndAssert(
            List<AdGroupNewMinusKeywords> adGroupNewKeywordsList,
            Boolean... isElemsSuccessful) {
        UpdateAdGroupMinusKeywordsOperation operation = createUpdateOperation(adGroupNewKeywordsList);
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isSuccessful(isElemsSuccessful));
    }

    private UpdateAdGroupMinusKeywordsOperation updateManyItemsAndAssume(
            List<AdGroupNewMinusKeywords> adGroupNewKeywordsList,
            Boolean... isElemsSuccessful) {
        UpdateAdGroupMinusKeywordsOperation operation = createUpdateOperation(adGroupNewKeywordsList);
        MassResult<Long> result = operation.prepareAndApply();
        assumeThat(result, isSuccessful(isElemsSuccessful));
        return operation;
    }

    private UpdateAdGroupMinusKeywordsOperation createUpdateOperation(
            List<AdGroupNewMinusKeywords> adGroupNewKeywordsList) {
        return appendAdGroupMinusKeywordsOperationFactory.newInstance(
                Applicability.FULL,
                adGroupNewKeywordsList,
                geoTree,
                UpdateMinusKeywordsMode.ADD,
                operatorUid,
                clientId,
                shard);
    }

    private AdGroupNewMinusKeywords createValidNewKeywords(Long adGroupId) {
        return new AdGroupNewMinusKeywords(adGroupId, asList(NEW_MINUS_KEYWORD_1, NEW_MINUS_KEYWORD_2));
    }

    private AdGroupNewMinusKeywords createNewKeywordsWithInvalidItem(Long adGroupId) {
        return new AdGroupNewMinusKeywords(adGroupId, asList("[][][]", NEW_MINUS_KEYWORD_2));
    }

    private AdGroupNewMinusKeywords createTooLongNewKeywords(Long adGroupId) {
        List<String> minusKeywords = createTooLongKeywords();
        return new AdGroupNewMinusKeywords(adGroupId, minusKeywords);
    }

    private static List<String> createTooLongKeywords() {
        // суммарная длина всех минус-слов = 10 * 5 * 82 = 4100
        List<String> minusKeywords = new ArrayList<>();
        for (int i = 0; i < 82; i++) {
            minusKeywords.add(StringUtils.repeat(randomAlphanumeric(10), " ", 5));
        }
        return minusKeywords;
    }
}
