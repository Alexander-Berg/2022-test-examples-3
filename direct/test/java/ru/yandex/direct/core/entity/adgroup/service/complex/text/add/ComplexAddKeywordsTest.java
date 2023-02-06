package ru.yandex.direct.core.entity.adgroup.service.complex.text.add;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.complex.text.ComplexTextAdGroupAddOperation;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.validation.defects.params.CurrencyAmountDefectParams;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.KEYWORDS;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.emptyAdGroupWithModelForAdd;
import static ru.yandex.direct.core.entity.bids.validation.BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_GREATER_THAN_MIN;
import static ru.yandex.direct.core.entity.keyword.model.Keyword.PHRASE;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.illegalCharacters;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.incorrectUseOfParenthesis;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.invalidBrackets;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultClientKeyword;
import static ru.yandex.direct.core.testing.data.TestKeywords.keywordWithText;
import static ru.yandex.direct.currency.Money.valueOf;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.defect.StringDefects.mustContainLettersOrDigitsOrPunctuations;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexAddKeywordsTest extends ComplexTextAddTestBase {

    private static final BigDecimal MIN_AUTOBUDGET_BID = new BigDecimal("0.3");

    @Test
    public void oneAdGroupWithKeyword() {
        ComplexTextAdGroup adGroup = adGroupWithKeywords(singletonList(defaultClientKeyword()));
        addAndCheckComplexAdGroups(singletonList(adGroup));
    }

    @Test
    public void oneAdGroupWithSeveralKeywords() {
        ComplexTextAdGroup adGroupWithKeywords =
                adGroupWithKeywords(asList(defaultClientKeyword(), defaultClientKeyword(), defaultClientKeyword()));
        addAndCheckComplexAdGroups(singletonList(adGroupWithKeywords));
    }

    @Test
    public void oneAdGroupWithoutKeywordsAndOneWith() {
        ComplexTextAdGroup emptyComplexAdGroup = emptyTextAdGroup();
        ComplexTextAdGroup adGroupWithKeywords =
                adGroupWithKeywords(asList(defaultClientKeyword(), defaultClientKeyword(), defaultClientKeyword()));
        addAndCheckComplexAdGroups(asList(emptyComplexAdGroup, adGroupWithKeywords));
    }

    // позитивные кейсы с круглыми скобками

    @Test
    public void adGroupWithKeywordsWithParenthesis() {
        ComplexTextAdGroup adGroup = new ComplexTextAdGroup()
                .withAdGroup(defaultTextAdGroup(campaign.getCampaignId()))
                .withKeywords(singletonList(keywordWithText("(одна две|три четыре)фразы")
                        .withPrice(BigDecimal.TEN).withPriceContext(BigDecimal.TEN)
                ));

        ComplexTextAdGroupAddOperation addOperation = createOperation(singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();

        List<Keyword> keywords =
                keywordRepository.getKeywordsByAdGroupId(campaign.getShard(), result.get(0).getResult());
        List<String> phrases = mapList(keywords, Keyword::getPhrase);
        assertThat(phrases, containsInAnyOrder(asList("одна две фразы", "три четыре фразы").toArray()));
    }

    @Test
    public void twoAdGroupsWithKeywordsWithParenthesis() {
        ComplexTextAdGroup adGroup1 = new ComplexTextAdGroup()
                .withAdGroup(defaultTextAdGroup(campaign.getCampaignId()))
                .withKeywords(asList(
                        keywordWithText("обычная фраза"),
                        keywordWithText("(три|два) один"),
                        keywordWithText("необычная фраза")
                ));
        ComplexTextAdGroup adGroup2 = new ComplexTextAdGroup()
                .withAdGroup(defaultTextAdGroup(campaign.getCampaignId()))
                .withKeywords(asList(
                        keywordWithText("фраза с (разветвлением | несколькими вариантами)"),
                        keywordWithText("новая фраза"),
                        keywordWithText("(сто|двести) килограмм")
                ));

        ComplexTextAdGroupAddOperation addOperation = createOperation(asList(adGroup1, adGroup2));
        MassResult<Long> result = addOperation.prepareAndApply();

        List<Keyword> keywords1 =
                keywordRepository.getKeywordsByAdGroupId(campaign.getShard(), result.get(0).getResult());
        List<String> phrases1 = mapList(keywords1, Keyword::getPhrase);
        assertThat("фразы корректно добавились в первую группу",
                phrases1,
                containsInAnyOrder(asList("обычная фраза", "три один", "два один", "необычная фраза").toArray()));

        List<Keyword> keywords2 =
                keywordRepository.getKeywordsByAdGroupId(campaign.getShard(), result.get(1).getResult());
        List<String> phrases2 = mapList(keywords2, Keyword::getPhrase);
        assertThat("фразы корректно добавились во вторую группу",
                phrases2,
                containsInAnyOrder(asList("фраза с разветвлением",
                        "фраза с несколькими вариантами", "новая фраза",
                        "сто килограмм", "двести килограмм").toArray()));
    }

    /**
     * Проверяем, что режим {@code autoPrices} корректно прокидывается в
     * {@link ru.yandex.direct.core.entity.keyword.service.KeywordsAddOperation}
     */
    @Test
    public void adGroupWithKeywordWithAutoPrices() {
        Keyword keyword = defaultClientKeyword()
                .withPriceContext(null)
                .withPrice(null);
        ComplexTextAdGroup adGroup = adGroupWithKeywords(singletonList(keyword));
        addWithAutoPricesAndCheckComplexAdGroups(singletonList(adGroup));

        List<Keyword> actualKeywords =
                keywordRepository.getKeywordsByAdGroupId(campaign.getShard(), adGroup.getAdGroup().getId());
        assertThat("у фразы выставилась автоматическая ставка", actualKeywords.get(0).getPrice(), is(FIXED_AUTO_PRICE));
    }

    //ошибки валидации

    @Test
    public void oneAdGroupWithOneInvalidKeyword() {
        ComplexTextAdGroup adGroup = new ComplexTextAdGroup()
                .withAdGroup(activeTextAdGroup(campaign.getCampaignId()))
                .withKeywords(singletonList(defaultClientKeyword().withPrice(new BigDecimal(0.2))));
        ComplexTextAdGroupAddOperation addOperation = createOperation(singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();
        Path errPath = path(index(0), field(KEYWORDS.name()), index(0), field(Keyword.PRICE.name()));
        Defect error = new Defect<>(SEARCH_PRICE_IS_NOT_GREATER_THAN_MIN,
                new CurrencyAmountDefectParams(valueOf(MIN_AUTOBUDGET_BID, CurrencyCode.RUB)));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, error)));
    }

    @Test
    public void adGroupsWithValidAndInvalidKeywords() {
        ComplexTextAdGroup adGroup1 = new ComplexTextAdGroup()
                .withAdGroup(defaultTextAdGroup(campaign.getCampaignId()))
                .withKeywords(asList(defaultClientKeyword(), defaultClientKeyword().withHrefParam1("åß∂"),
                        defaultClientKeyword()));
        ComplexTextAdGroup adGroup2 = new ComplexTextAdGroup()
                .withAdGroup(defaultTextAdGroup(campaign.getCampaignId()))
                .withKeywords(asList(defaultClientKeyword().withPrice(new BigDecimal(0.2)), defaultClientKeyword()));
        ComplexTextAdGroupAddOperation addOperation = createOperation(asList(adGroup1, adGroup2));
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result.getValidationResult(), allOf(
                //href param matcher
                hasDefectDefinitionWith(validationError(path(index(0), field(KEYWORDS.name()), index(1),
                        field(Keyword.HREF_PARAM1.name())), mustContainLettersOrDigitsOrPunctuations())),
                //price matcher
                hasDefectDefinitionWith(validationError(
                        path(index(1), field(KEYWORDS.name()), index(0), field(Keyword.PRICE.name())),
                        new Defect<>(
                                SEARCH_PRICE_IS_NOT_GREATER_THAN_MIN,
                                new CurrencyAmountDefectParams(valueOf(MIN_AUTOBUDGET_BID, CurrencyCode.RUB)))))
        ));
    }

    // ошибки валидации в круглых скобках

    @Test
    public void adGroupWithKeywordsWithInvalidParenthesis() {
        ComplexTextAdGroup adGroup = new ComplexTextAdGroup()
                .withAdGroup(defaultTextAdGroup(campaign.getCampaignId()))
                .withKeywords(singletonList(keywordWithText("(одна|)фраза")));

        ComplexTextAdGroupAddOperation addOperation = createOperation(singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();

        Path path = path(index(0), field(KEYWORDS), index(0), field(PHRASE));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path, incorrectUseOfParenthesis())));
    }

    @Test
    public void severalAdGroupsWithKeywordsWithValidAndInvalidParenthesis() {
        ComplexTextAdGroup adGroup1 = new ComplexTextAdGroup()
                .withAdGroup(defaultTextAdGroup(campaign.getCampaignId()))
                .withKeywords(asList(
                        keywordWithText("обычная фраза"),
                        keywordWithText("(три|) один"),
                        keywordWithText("необычная фраза")
                ));
        ComplexTextAdGroup adGroup2 = new ComplexTextAdGroup()
                .withAdGroup(defaultTextAdGroup(campaign.getCampaignId()))
                .withKeywords(asList(
                        keywordWithText("фраза с (| ошибкой)"),
                        keywordWithText("новая фраза"),
                        keywordWithText("(сто|двести) килограмм")
                ));

        ComplexTextAdGroupAddOperation addOperation = createOperation(asList(adGroup1, adGroup2));
        MassResult<Long> result = addOperation.prepareAndApply();

        Path path1 = path(index(0), field(KEYWORDS), index(1), field(PHRASE));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path1, incorrectUseOfParenthesis())));
        Path path2 = path(index(1), field(KEYWORDS), index(0), field(PHRASE));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path2, incorrectUseOfParenthesis())));

        assertThat(result.getValidationResult().flattenErrors(), hasSize(2));
    }

    // ошибки валидации после разделения фраз по круглым скобкам

    // после разделения ошибка будет во второй фразе, но должна прилинковаться к исходной
    @Test
    public void adGroupWithKeywordsWithValidParenthesisAndInvalidOnePhraseInside() {
        ComplexTextAdGroup adGroup = new ComplexTextAdGroup()
                .withAdGroup(defaultTextAdGroup(campaign.getCampaignId()))
                .withKeywords(singletonList(keywordWithText("(одна|вторая*)фраза")));

        ComplexTextAdGroupAddOperation addOperation = createOperation(singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();

        Path path = path(index(0), field(KEYWORDS), index(0), field(PHRASE));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path, illegalCharacters(singletonList("вторая*")))));
    }

    @Test
    public void adGroupWithKeywordsWithValidParenthesisAndInvalidTwoPhrasesInside() {
        ComplexTextAdGroup adGroup = new ComplexTextAdGroup()
                .withAdGroup(defaultTextAdGroup(campaign.getCampaignId()))
                .withKeywords(singletonList(keywordWithText("(одна*|вторая*)фраза")));

        ComplexTextAdGroupAddOperation addOperation = createOperation(singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();

        Path path = path(index(0), field(KEYWORDS), index(0), field(PHRASE));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path, illegalCharacters(singletonList("одна*")))));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path, illegalCharacters(singletonList("вторая*")))));
    }

    @Test
    public void severalAdGroupsWithKeywordsWithValidParenthesisAndInvalidPhrasesInside() {
        ComplexTextAdGroup adGroup1 = new ComplexTextAdGroup()
                .withAdGroup(defaultTextAdGroup(campaign.getCampaignId()))
                .withKeywords(asList(
                        keywordWithText("обычная фраза"),
                        keywordWithText("(три|;четыре) один"),
                        keywordWithText("необычная фраза")
                ));
        ComplexTextAdGroup adGroup2 = new ComplexTextAdGroup()
                .withAdGroup(defaultTextAdGroup(campaign.getCampaignId()))
                .withKeywords(asList(
                        keywordWithText("фраза с ([]| ошибкой)"),
                        keywordWithText("новая фраза"),
                        keywordWithText("(сто|двести) килограмм")
                ));

        ComplexTextAdGroupAddOperation addOperation = createOperation(asList(adGroup1, adGroup2));
        MassResult<Long> result = addOperation.prepareAndApply();

        Path path1 = path(index(0), field(KEYWORDS), index(1), field(PHRASE));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path1, illegalCharacters(singletonList(";четыре")))));
        Path path2 = path(index(1), field(KEYWORDS), index(0), field(PHRASE));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path2, invalidBrackets())));

        assertThat(result.getValidationResult().flattenErrors(), hasSize(2));
    }

    protected ComplexTextAdGroup adGroupWithKeywords(List<Keyword> keywords) {
        return emptyAdGroupWithModelForAdd(campaignId, keywords, KEYWORDS);
    }
}
