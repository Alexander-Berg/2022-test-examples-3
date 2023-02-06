package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;

import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithMinusKeywords;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.validation.result.Defect;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.type.bean.CampaignWithMinusKeywordsPreValidators.CAMPAIGN_VALIDATOR;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.WORDS_MAX_COUNT;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.WORD_MAX_LENGTH;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.illegalMinusKeywordChars;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.imbalancedSquareBrackets;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.invalidDot;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.invalidExclamationMark;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.invalidMinusMark;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.invalidOperatorsInsideSquareBrackets;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.invalidPlusMark;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.invalidWordFirstCharacter;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.maxCountWordsInKeyword;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.maxLengthMinusWord;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.nestedOrEmptySquareBrackets;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.separateDot;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class CampaignsWithMinusKeywordsPreValidatorsInvalidPhrasesTest {

    @Parameterized.Parameters(name = "minus phrases: {0}, defect path: {1}, defect: {2}")
    public static Collection<Object[]> invalidPhrasesParameters() {
        return List.of(new Object[][]{
                {"[новая [невалидная] минус-фраза]",
                        nestedOrEmptySquareBrackets(List.of("[новая [невалидная] минус-фраза]")), "Вложенные квадратные скобки"},
                {"@фраза", illegalMinusKeywordChars(List.of("@фраза")), "недопустимый символ"},
                {"'фраза'", illegalMinusKeywordChars(List.of("'фраза'")), "недопустимые кавычки"},
                {"фраза . с . какими-то . точками", separateDot(List.of("фраза . с . какими-то . точками")), "висящие точки"},
                {".фраза", invalidWordFirstCharacter(List.of(".фраза")), "недопустимое начало фразы"},
                {"3.14 фраза 2.71828", invalidDot(List.of("3.14 фраза 2.71828")), "две точки во фразе"},
                {"в этой минус фразе должно быть больше семи слов",
                        maxCountWordsInKeyword(WORDS_MAX_COUNT, List.of("в этой минус фразе должно быть больше семи слов")),
                        "слишком много слов во фразе"},
                {"вЭтойМинусФразеСодержитсяСловоКотороеСостоитИзБолееТридцатиПятиСимволов",
                        maxLengthMinusWord(WORD_MAX_LENGTH,
                                List.of("вЭтойМинусФразеСодержитсяСловоКотороеСостоитИзБолееТридцатиПятиСимволов")),
                        "Слишком много символов по фразе"},
                {"[фраза", imbalancedSquareBrackets(List.of("[фраза")), "отсутствует закрывающая квадратная скобка"},
                {"фраза []", nestedOrEmptySquareBrackets(List.of("фраза []")), "пустые квадратные скобки"},
                {"[+фраза]", invalidOperatorsInsideSquareBrackets(List.of("[+фраза]")), "недопустимый оператор внутри скобок"},
                {"фра!за", invalidExclamationMark(List.of("фра!за")), "восклицание в середине фразы"},
                {"фра--за", invalidMinusMark(List.of("фра--за")), "двойной минус в середине фразы"},
                {"фра+за", invalidPlusMark(List.of("фра+за")), "плюс в середине фразы"}
        });
    }

    @Test
    @Parameters(method = "invalidPhrasesParameters")
    public void testInvalidPhrases(String minusPhrase, Defect defect, @SuppressWarnings("unused") String description) {
        var minusPhrases = List.of(minusPhrase);
        var expectedPath = path(field(CampaignWithMinusKeywords.MINUS_KEYWORDS), index(0));
        var vr = CAMPAIGN_VALIDATOR.apply(new TextCampaign().withMinusKeywords(minusPhrases));
        assertThat(vr, hasDefectDefinitionWith(validationError(expectedPath, defect)));
    }
}
