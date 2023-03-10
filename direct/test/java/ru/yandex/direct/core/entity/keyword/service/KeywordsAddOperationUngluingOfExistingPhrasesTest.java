package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.keyword.container.AddedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.container.AffectedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.container.KeywordsAddOperationParams;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFields;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAdded;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAddedWithFixation;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAddedWithMinus;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isNotAdded;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.resultPhrase;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.isAfter;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsAddOperationUngluingOfExistingPhrasesTest extends KeywordsAddOperationBaseTest {

    // ?????????????????? ???? ???????????????? ?????? ???????????? ??????????

    @Test
    public void execute_ExistingPhraseAndNewPhraseAreSuitableForUngluingButInDifferentAdGroups() {
        String existingPhrase = "??????";
        createTwoActiveAdGroups();
        createKeyword(adGroupInfo2, existingPhrase);

        String newPhrase = "???????????????? ??????";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isAdded(newPhrase)));
        assertThat(operation.getAffectedKeywordInfoList(), emptyIterable());
    }

    @Test
    public void execute_OneExistingPhraseIsAppendedByMinusAndOtherIsNotAppendedByInDifferentAdGroup() {
        String existingPhrase1 = "????????";
        String existingPhrase2 = "???????????????? ??????";
        createTwoActiveAdGroups();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase1).getId();
        createKeyword(adGroupInfo2, existingPhrase2);

        String newPhrase = "?????? ??????????";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isAdded(newPhrase)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase1, "????????????"));
    }

    @Test
    public void execute_UnglueWontRunIfItIsNotEnabled() {
        String existingPhrase1 = "????????";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase1).getId();

        String newPhrase = "?????? ??????????";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));

        KeywordsAddOperation operation = createOperationWithoutUnglue(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isAdded(newPhrase)));
        assertThat(operation.getAffectedKeywordInfoList(), empty());
    }

    // ?????????????????? ???????????????? ???? ???????????????????? ???????????? ?????????? ???????????????? ????????-????????

    @Test
    public void execute_OneExistingPhraseIsAppendedByMinusOfNewPhraseInNormalForm() {
        String existingPhrase = "????????";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();

        String newPhrase = "?????? ??????????";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isAdded(newPhrase)));
        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase, "????????????"));
    }

    // ?????????????????????????? ???????????????? ?????? ??????????????????

    @Test
    public void execute_OneExistingPhraseWithUpperCaseIsAppendedByMinusOfNewPhraseInNormalForm() {
        String existingPhrase = "????????";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();

        String newPhrase = "?????? ??????????";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isAdded(newPhrase)));
        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase, "????????????"));
    }

    @Test
    public void execute_OneExistingPhraseIsAppendedByMinusOfNewPhraseWithUpperCaseInNormalForm() {
        String existingPhrase = "????????";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();

        String newPhrase = "?????? ??????????";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isAdded(newPhrase)));
        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase, "????????????"));
    }

    // ?????????????????????????? ???????????????? ?????????? ?????? ??????????????????

    @Test
    public void execute_OneExistingPhraseWithEndingPointIsAppendedByMinusOfNewPhrase() {
        String existingPhrase = "????????.";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();

        String newPhrase = "?????? ??????????.";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isAdded(newPhrase)));
        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase, "????????????"));
    }

    // ?????????????????? ??????????

    @Test
    public void execute_OneExistingPhraseAndOneNewPhraseIsAppendedByTheSameMinusOfNewPhraseInNormalForm() {
        String existingPhrase = "????????";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();

        String newPhrase1 = "?????? -??????";
        String newPhrase2 = "?????? ??????????";
        String minus = "????????????";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, newPhrase1),
                clientKeyword(adGroupInfo1, newPhrase2));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result,
                isSuccessfulWithMatchers(
                        isAddedWithMinus(newPhrase1, minus),
                        isAdded(newPhrase2)));
        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase, minus));
    }

    @Test
    public void execute_PhrasesAreSentToUngluingInNormalForms() {
        String existingPhrase = "???? ????????";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();

        String newPhrase = "?????? ???? ??????????";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isAdded(newPhrase)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase, "????????????"));
    }

    @Test
    public void execute_OneExistingPhraseIsAppendedByFixedStopWordMinusOfNewPhrase() {
        String existingPhrase = "123";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();

        String newPhrase = "123 ????";
        String newPhraseResult = "123 +????";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result,
                isSuccessfulWithMatchers(isAddedWithFixation(newPhraseResult, newPhrase, newPhraseResult)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase, "!????"));
    }

    @Test
    public void execute_OneExistingPhraseIsAppendedByFixedMinusOfNewPhrase() {
        String existingPhrase = "??????";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();

        String newPhrase = "!?????????? ????????";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isAdded(newPhrase)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase, "!??????????"));
    }

    @Test
    public void execute_OneExistingPhraseWithMinusIsAppendedByMinusOfNewPhrase() {
        String existingPhrase = "?????? -??????????";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();

        String newPhrase = "?????? ??????????";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isAdded(newPhrase)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase, "????????????"));
    }

    @Test
    public void execute_OneExistingSuspendedPhraseWithMinusIsAppendedByMinusOfNewPhrase() {
        String existingPhrase = "?????? -??????????";
        createOneActiveAdGroup();
        Long existingKeywordId =
                keywordSteps.createKeyword(adGroupInfo1, getDefaultActiveKeyword(existingPhrase).withIsSuspended(true))
                        .getId();

        String newPhrase = "?????? ??????????";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isAdded(newPhrase)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase, "????????????", true));
    }

    @Test
    public void execute_OneExistingPhraseIsAppendedByTwoMinusesOfNewPhrases() {
        String existingPhrase = "??????";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();

        String newPhrase1 = "?????????? ????????";
        String newPhrase2 = "?????? ??????????";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, newPhrase1),
                clientKeyword(adGroupInfo1, newPhrase2));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isAdded(newPhrase1), isAdded(newPhrase2)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase,
                        asList("????????????", "??????????"), false));
    }

    // ?????????????????? ???? ???????????? ?????????????????????? ?????? ??????????????????

    @Test
    public void execute_OneExistingPhraseIsAppendedByMinusOfNewDuplicatedPhrases() {
        String existingPhrase = "??????";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();

        String newPhrase1 = "?????? ??????????";
        String newPhrase2 = "???????????? ????????";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, newPhrase1),
                clientKeyword(adGroupInfo1, newPhrase2));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isAdded(newPhrase1), isNotAdded(newPhrase2)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase, "????????????"));
    }

    @Test
    public void execute_OneExistingPhraseIsNotAppendedByMinusOfNewPhraseDuplicatedWithExistingPhrase() {
        String existingPhrase1 = "??????";
        String existingPhrase2 = "?????? ?????????? -????????????";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase1);
        createKeyword(adGroupInfo1, existingPhrase2);

        String newPhrase1 = "?????? ?????????? -??????????";
        String newPhrase2 = "?????? ?????????? ???????????????????? ??????";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, newPhrase1),
                clientKeyword(adGroupInfo1, newPhrase2));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isNotAdded(newPhrase1), isAdded(newPhrase2)));

        assertThat(operation.getAffectedKeywordInfoList(), emptyIterable());
    }

    // ?????? ?????????????????? ???? ???????????? ???????????????????????????? ?????????? ????????????????????

    @Test
    public void execute_OneExistingPhraseIsNotAppendedByMinusToAvoidDuplicationWithExistingPhrases() {
        String existingPhrase1 = "??????";
        String existingPhrase2 = "?????? -??????????";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase1);
        createKeyword(adGroupInfo1, existingPhrase2);

        String newPhrase = "?????? ??????????";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isAdded(newPhrase)));

        assertThat(operation.getAffectedKeywordInfoList(), emptyIterable());
    }

    @Test
    public void execute_OneExistingPhraseIsNotAppendedByMinusToAvoidDuplicationWithNewPhrases() {
        String existingPhrase1 = "??????";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase1);

        String newPhrase1 = "?????? ??????????";
        String newPhrase2 = "?????? -??????????";
        List<Keyword> keywords =
                asList(clientKeyword(adGroupInfo1, newPhrase1), clientKeyword(adGroupInfo1, newPhrase2));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isAdded(newPhrase1), isAdded(newPhrase2)));

        assertThat(operation.getAffectedKeywordInfoList(), emptyIterable());
    }

    @Test
    public void execute_OneExistingPhraseIsPartiallyAppendedByMinusToAvoidDuplicationWithExistingPhrases() {
        String existingPhrase1 = "??????";
        String existingPhrase2 = "?????? -?????????? -??????????";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase1).getId();
        createKeyword(adGroupInfo1, existingPhrase2);

        String newPhrase1 = "?????? ??????????";
        String newPhrase2 = "?????????? ??????";
        List<Keyword> keywords =
                asList(clientKeyword(adGroupInfo1, newPhrase1), clientKeyword(adGroupInfo1, newPhrase2));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isAdded(newPhrase1), isAdded(newPhrase2)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase1, "??????????"));
    }

    @Test
    public void execute_OneExistingPhraseIsPartiallyAppendedByMinusToAvoidDuplicationWithNewPhrases() {
        String existingPhrase = "??????";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();

        String newPhrase1 = "?????? ??????????";
        String newPhrase2 = "?????????? ??????";
        String newPhrase3 = "?????? -?????????? -??????????";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, newPhrase1),
                clientKeyword(adGroupInfo1, newPhrase2),
                clientKeyword(adGroupInfo1, newPhrase3));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result,
                isSuccessfulWithMatchers(
                        isAdded(newPhrase1),
                        isAdded(newPhrase2),
                        isAdded(newPhrase3)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase, "??????????"));
    }

    // ??????????, ???? ???????????????????? ?????????????????? ?? ?????????? ????????????

    @Test
    public void execute_ExistingPhraseAndNewPhraseNotSuitableForUngluing() {
        String existingPhrase = "?????????? ??????";
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, existingPhrase);

        String newPhrase = "???????????????? ??????";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isAdded(newPhrase)));
        assertThat(operation.getAffectedKeywordInfoList(), emptyIterable());
    }

    // ????????????????????

    @Test
    public void execute_UngluedExistingPhraseIsUpdatedCorrectly() {
        String existingPhrase = "???????? -??????";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase).getId();
        Keyword expectedExistingKeyword = getKeyword(existingKeywordId);
        rewindKeywordModificationTime(expectedExistingKeyword);

        String newPhrase = "?????? ??????????";
        String minus = "????????????";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isAdded(newPhrase)));
        assumeThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase, minus));

        expectedExistingKeyword.withPhrase(resultPhrase(existingPhrase, minus));

        Keyword actualExistingKeyword = getKeyword(existingKeywordId);
        assertThat("???????????????????????? ?????????? ?????????? ?????????????????? ?????????????????????????? ??????????????????",
                actualExistingKeyword,
                beanDiffer(expectedExistingKeyword)
                        .useCompareStrategy(allFields()
                                .forFields(newPath("modificationTime"))
                                .useMatcher(isAfter(expectedExistingKeyword.getModificationTime()))));
    }

    //??????????, ?????????????? ???? ?????????? ???????? ????????????????????, ???? ?????????????????? ?? ??????????????????

    @Test
    public void execute_NotParsedPhraseIsNotSentToUngluing() {
        String existingPhrase = "?????? --????????????";
        createTwoActiveAdGroups();
        keywordSteps.createKeywordWithText(existingPhrase, adGroupInfo1);

        String newPhrase = "???????????????? ??????";
        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, newPhrase));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result, isSuccessfulWithMatchers(isAdded(newPhrase)));
        assertThat(operation.getAffectedKeywordInfoList(), emptyIterable());
    }

    /**
     * ???????? ???????????????????????? ?? ?????????????????? ???????????????????????? ?????????? ???? ??????????, ?????? ?????????????? ?????? ?????????? ?? ?????????????? ?????????????? ??????????????????,
     * ?????????? npe ?????? ???????????????? ????????????????????, ?????? ?????? ???????????? ???????????? ?????????? null ????-???? ????????, ?????? ?????????????? ???????????????????? ????????????
     * ?????? ???????????????? ??????????
     */
    @Test
    public void prepare_NoExceptionsInUnglueWhenExternalPhrasesFromInvalidAdGroup() {
        createTwoActiveAdGroups();

        String newPhraseInGroup1 = "?????????????? ??????";
        String invalidNewPhraseInGroup2 = "???????????? ?????????? %$&";
        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, newPhraseInGroup1),
                clientKeyword(adGroupInfo2, invalidNewPhraseInGroup2));

        String existingPhrase = "???????????? ????????";
        List<Keyword> existingKeywords = singletonList(clientKeyword(adGroupInfo2, existingPhrase));

        KeywordsAddOperationParams operationParams = KeywordsAddOperationParams.builder()
                .withAdGroupsNonexistentOnPrepare(false)
                .withUnglueEnabled(true)
                .withIgnoreOversize(false)
                .withAutoPrices(false)
                .withWeakenValidation(false)
                .build();
        KeywordsAddOperation operation =
                createOperation(Applicability.PARTIAL, operationParams, keywords, existingKeywords,
                        operatorClientInfo.getUid());
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();
        assertThat(result, isSuccessful());
    }


    // ?????????????????? ????????

    @Test
    public void execute_ComplexCaseWithSuitableAndUnsuitableForUngluingPhrases() {
        String existingPhrase1 = "????????";
        String existingPhrase2 = "???????????????? ??????";
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, existingPhrase1).getId();
        createKeyword(adGroupInfo1, existingPhrase2);

        String newPhrase1 = "?????? ??????????";
        String newPhrase2 = "??????";
        String newPhrase2Minus = "????????????????";
        String newPhrase3 = "?????? -????????????????";
        String newPhrase3Minus = "????????????";
        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, newPhrase1),
                clientKeyword(adGroupInfo1, newPhrase2),
                clientKeyword(adGroupInfo1, newPhrase3));

        KeywordsAddOperation operation = createOperation(Applicability.PARTIAL, keywords);
        MassResult<AddedKeywordInfo> result = operation.prepareAndApply();

        assumeThat(result,
                isSuccessfulWithMatchers(
                        isAdded(newPhrase1),
                        isAddedWithMinus(newPhrase2, newPhrase2Minus),
                        isAddedWithMinus(newPhrase3, newPhrase3Minus)));

        assertThat(operation.getAffectedKeywordInfoList(),
                onePhraseIsAffected(existingKeywordId, adGroupInfo1.getAdGroupId(), existingPhrase1, newPhrase3Minus));

        Keyword existingKeyword = getKeyword(existingKeywordId);
        assertThat(existingKeyword.getPhrase(), equalTo(resultPhrase(existingPhrase1, newPhrase3Minus)));

        Keyword newKeyword1 = getKeyword(result.get(0).getResult().getId());
        assertThat(newKeyword1.getPhrase(), equalTo(newPhrase1));

        Keyword newKeyword2 = getKeyword(result.get(1).getResult().getId());
        assertThat(newKeyword2.getPhrase(), equalTo(resultPhrase(newPhrase2, newPhrase2Minus)));

        Keyword newKeyword3 = getKeyword(result.get(2).getResult().getId());
        assertThat(newKeyword3.getPhrase(), equalTo(resultPhrase(newPhrase3, newPhrase3Minus)));
    }

    private Matcher<List<AffectedKeywordInfo>> onePhraseIsAffected(Long id, Long adGroupId, String sourcePhrase,
                                                                   String addedMinus) {
        return onePhraseIsAffected(id, adGroupId, sourcePhrase, singletonList(addedMinus), false);
    }


    private Matcher<List<AffectedKeywordInfo>> onePhraseIsAffected(Long id, Long adGroupId, String sourcePhrase,
                                                                   String addedMinus, Boolean isSuspended) {
        return onePhraseIsAffected(id, adGroupId, sourcePhrase, singletonList(addedMinus), isSuspended);
    }

    private Matcher<List<AffectedKeywordInfo>> onePhraseIsAffected(Long id, Long adGroupId, String sourcePhrase,
                                                                   List<String> addedMinuses, Boolean isSuspended) {
        return beanDiffer(
                singletonList(new AffectedKeywordInfo(id, adGroupId, sourcePhrase, addedMinuses, isSuspended)));
    }
}
