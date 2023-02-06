package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithMinusKeywords;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.CAMPAIGN_MINUS_KEYWORDS_MAX_LENGTH;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.WORDS_MAX_COUNT;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.maxCountWordsInKeyword;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.maxLengthMinusKeywords;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class CampaignWithMinusKeywordsValidatorTest {
    private CampaignWithMinusKeywordsValidator validator;

    @Before
    public void init() {
        validator = new CampaignWithMinusKeywordsValidator();
    }

    @Test
    public void testValidMinusPhrase() {
        ValidationResult vr = validator.apply(packMinusPhrases(List.of("valid phrase")));
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    @Parameters(method = "invalidPhrasesParameters")
    @TestCaseName("[{index}] {2}")
    public void testInvalidMinusPhrases(List<String> phrase, Defect expectedDefect,
                                        @SuppressWarnings("unused")  String description) {
        Path expectedPath = path(field(CampaignWithMinusKeywords.MINUS_KEYWORDS));
        ValidationResult vr = validator.apply(packMinusPhrases(phrase));
        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(expectedPath, expectedDefect))));
    }

    @SuppressWarnings("unused") // See @Parameters annotations usages here
    private static Object[] invalidPhrasesParameters() {
        return new Object[][]{
                {List.of("Минус фраза в которой вроде больше семи слов"),
                        maxCountWordsInKeyword(WORDS_MAX_COUNT, List.of("Минус фраза в которой вроде больше семи слов")),
                        "слишком много слов во фразе"},
                {tooLongListOfMinusPhrases(), maxLengthMinusKeywords(CAMPAIGN_MINUS_KEYWORDS_MAX_LENGTH),
                        "слишком большое суммарное количество символов в списке минус-фраз"}
        };
    }

    private static CampaignWithMinusKeywords packMinusPhrases(List<String> phrases) {
        return new TextCampaign().withMinusKeywords(phrases);
    }

    private static List<String> tooLongListOfMinusPhrases() {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            result.add(randomAlphabetic(20));
        }
        return result;
    }
}
