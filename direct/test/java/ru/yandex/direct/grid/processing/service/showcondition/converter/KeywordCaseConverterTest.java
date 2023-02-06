package ru.yandex.direct.grid.processing.service.showcondition.converter;

import java.util.Map;

import com.google.common.base.Function;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.grid.core.entity.showcondition.repository.GridKeywordsParser;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdChangeKeywordsCaseMode;
import ru.yandex.direct.libs.keywordutils.model.KeywordWithMinuses;

import static ru.yandex.direct.grid.processing.model.showcondition.mutation.GdChangeKeywordsCaseMode.CAPITALIZE_ALL_WORDS;
import static ru.yandex.direct.grid.processing.model.showcondition.mutation.GdChangeKeywordsCaseMode.CAPITALIZE_FIRST_WORD;
import static ru.yandex.direct.grid.processing.model.showcondition.mutation.GdChangeKeywordsCaseMode.LOWERCASE;
import static ru.yandex.direct.grid.processing.model.showcondition.mutation.GdChangeKeywordsCaseMode.UPPERCASE;

@RunWith(JUnitParamsRunner.class)
public class KeywordCaseConverterTest {

    private KeywordCaseConverter keywordCaseConverter;
    private GridKeywordsParser gridKeywordsParser;
    private static final Map<GdChangeKeywordsCaseMode, Function<TestCase, String>> CHANGE_CASE_MODES = Map.of(
            UPPERCASE, TestCase::getUppercase,
            LOWERCASE, TestCase::getLowercase,
            CAPITALIZE_ALL_WORDS, TestCase::getCapitalizedAllWords,
            CAPITALIZE_FIRST_WORD, TestCase::getCapitalizedFirstWord
    );

    @Before
    public void before() {
        gridKeywordsParser = new GridKeywordsParser();
        keywordCaseConverter = new KeywordCaseConverter();
    }

    Object[] parametersForTestChangingCase() {
        return new Object[]{
                // замена только в ключевой фразе
                new Object[]{TestCase.keyword("пРиВет")
                        .uppercase("ПРИВЕТ")
                        .lowercase("привет")
                        .capitalizedAllWords("Привет")
                        .capitalizedFirstWord("Привет"),
                        true, false
                },

                new Object[]{TestCase.keyword("раз ДВА три")
                        .uppercase("РАЗ ДВА ТРИ")
                        .lowercase("раз два три")
                        .capitalizedAllWords("Раз Два Три")
                        .capitalizedFirstWord("Раз два три"),
                        true, false
                },
                new Object[]{TestCase.keyword("раз [ДВА] три")
                        .uppercase("РАЗ [ДВА] ТРИ")
                        .lowercase("раз [два] три")
                        .capitalizedAllWords("Раз [Два] Три")
                        .capitalizedFirstWord("Раз [два] три"),
                        true, false
                },
                new Object[]{TestCase.keyword("[раз] ДВА три")
                        .uppercase("[РАЗ] ДВА ТРИ")
                        .lowercase("[раз] два три")
                        .capitalizedAllWords("[Раз] Два Три")
                        .capitalizedFirstWord("[Раз] два три"),
                        true, false
                },

                new Object[]{TestCase.keyword("[!дорого БОГАТО] +слон 10 -дешево")
                        .uppercase("[!ДОРОГО БОГАТО] +СЛОН 10 -дешево")
                        .lowercase("[!дорого богато] +слон 10 -дешево")
                        .capitalizedAllWords("[!Дорого Богато] +Слон 10 -дешево")
                        .capitalizedFirstWord("[!Дорого богато] +слон 10 -дешево"),
                        true, false
                },
                new Object[]{TestCase.keyword("\"Слова В КАВЫЧКАХ\"")
                        .uppercase("\"СЛОВА В КАВЫЧКАХ\"")
                        .lowercase("\"слова в кавычках\"")
                        .capitalizedAllWords("\"Слова В Кавычках\"")
                        .capitalizedFirstWord("\"Слова в кавычках\""),
                        true, false
                },

                // замена в минус словах
                new Object[]{TestCase.keyword("купить -слОна")
                        .uppercase("купить -СЛОНА")
                        .lowercase("купить -слона")
                        .capitalizedAllWords("купить -Слона")
                        .capitalizedFirstWord("купить -Слона"),
                        false, true
                },

                // замена и в ключевой фразе и в минус словах
                new Object[]{TestCase.keyword("купитЬ -слОна")
                        .uppercase("КУПИТЬ -СЛОНА")
                        .lowercase("купить -слона")
                        .capitalizedAllWords("Купить -Слона")
                        .capitalizedFirstWord("Купить -Слона"),
                        true, true
                },
        };
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "parametersForTestChangingCase")
    public void testChangingCase(TestCase testCase, boolean changeKeyword, boolean changeMinusKeywords) {
        SoftAssertions.assertSoftly(soft -> {
            CHANGE_CASE_MODES.forEach((changeCaseMode, getExpectedKeyword) -> {
                String expectedKeyword = getExpectedKeyword.apply(testCase);
                KeywordWithMinuses actualKeyword =
                        keywordCaseConverter.changeCase(gridKeywordsParser.parseKeyword(testCase.keyword),
                                changeCaseMode, changeKeyword, changeMinusKeywords);

                soft.assertThat(expectedKeyword).isEqualTo(actualKeyword.toString());
            });
        });
    }

    private static class TestCase {
        private String keyword;
        private String uppercase;
        private String lowercase;
        private String capitalizedAllWords;
        private String capitalizedFirstWord;

        private TestCase(String keyword) {
            this.keyword = keyword;
        }

        private static TestCase keyword(String keyword) {
            return new TestCase(keyword);
        }

        TestCase uppercase(String uppercase) {
            this.uppercase = uppercase;
            return this;
        }

        TestCase lowercase(String lowercase) {
            this.lowercase = lowercase;
            return this;
        }

        TestCase capitalizedAllWords(String capitalizedAllWords) {
            this.capitalizedAllWords = capitalizedAllWords;
            return this;
        }

        TestCase capitalizedFirstWord(String capitalizedFirstWord) {
            this.capitalizedFirstWord = capitalizedFirstWord;
            return this;
        }

        String getUppercase() {
            return uppercase;
        }

        String getLowercase() {
            return lowercase;
        }

        String getCapitalizedAllWords() {
            return capitalizedAllWords;
        }

        String getCapitalizedFirstWord() {
            return capitalizedFirstWord;
        }

        @Override
        public String toString() {
            return "keyword=" + keyword;
        }
    }

}
