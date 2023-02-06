package ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.libs.keywordutils.parser.KeywordParser;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator.minusKeywordIsValid;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

@RunWith(Parameterized.class)
public class MinusPhraseValidatorPositiveTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{

                // 1 плюс-слово с различными операторами
                {"слон"},
                {" слон "},
                {"!слон"},
                {" !слон "},
                {"+слон"},
                {" +слон "},

                {"[слон]"},
                {" [ слон ] "},
                {"[!слон]"},
                {" [ !слон ] "},

                {"\"слон\""},
                {"\" слон \""},
                {"\"!слон\""},
                {"\" !слон \""},
                {"\"+слон\""},
                {"\" +слон \""},

                {"\"[слон]\""},
                {"\" [ слон ] \""},
                {"\"[!слон]\""},
                {"\" [ !слон ] \""},

                // 1 плюс-слово с дефисом с различными операторами
                {"санкт-петебург"},
                {"!санкт-петебург"},
                {"+санкт-петебург"},

                {"[санкт-петебург]"},
                {"[!санкт-петебург]"},

                {"\"санкт-петебург\""},
                {"\"!санкт-петебург\""},
                {"\"+санкт-петебург\""},

                {"\"[санкт-петебург]\""},
                {"\"[!санкт-петебург]\""},

                // 2 плюс-слова с различными операторами
                {"розовый слон"},
                {"розовый !слон"},
                {"!розовый слон"},
                {"!розовый !слон"},
                {"розовый +слон"},
                {"+розовый слон"},
                {"+розовый +слон"},

                {"розовый [слон]"},
                {"[розовый] слон"},
                {"[розовый] [слон]"},
                {"[розовый слон]"},

                {"розовый [!слон]"},
                {"[!розовый] слон"},

                {"+розовый [слон]"},

                {"\"розовый слон\""},
                {"\"розовый !слон\""},
                {"\"!розовый слон\""},
                {"\"!розовый !слон\""},
                {"\"розовый +слон\""},
                {"\"+розовый слон\""},
                {"\"+розовый +слон\""},

                {"\"розовый [слон]\""},
                {"\"[розовый] слон\""},
                {"\"[розовый] [слон]\""},
                {"\"[розовый слон]\""},

                {"\"розовый [!слон]\""},
                {"\"!розовый [!слон]\""},
                {"\"[!розовый] слон\""},

                // разрешено более одного пробела между словами
                {"  слон [  купить  коня  ]"},
                {"\"  слон [  купить  коня  ]  \""},

                // сложные фразы
                {"слон [летающий как белка]"},
                {"слон [!летающий как белка]"},
                {"!слон [!летающий как белка] но [не крокодил]"},
                {"!слон [летающий как !белка] +но [не крокодил]"},
                {"!слон [летающий !как белка] [не крокодил] +но"},
                {"[!летающий как белка] !слон [не крокодил] +но"},
                {"\"слон [летающий как белка]\""},
                {"\"слон [!летающий как белка]\""},
                {"\"!слон [!летающий как белка] но [не крокодил]\""},
                {"\"!слон [летающий как !белка] +но [не крокодил]\""},
                {"\"!слон [летающий !как белка] [не крокодил] +но\""},
                {"\"[!летающий как белка] !слон [не крокодил] +но\""},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testValidator() {
        ValidationResult<List<String>, Defect> validationResult =
                minusKeywordIsValid(MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE).apply(singletonList(keyword));
        assertThat(validationResult, hasNoDefectsDefinitions());
    }

    /**
     * Все положительные кейсы должны успешно парситься
     */
    @Test
    public void testParsing() {
        try {
            KeywordParser.parse(keyword);
        } catch (Exception e) {
            fail(e.toString());
        }
    }
}
