package ru.yandex.direct.core.entity.keyword.processing;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.stopword.service.StopWordService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.libs.keywordutils.parser.KeywordParser;

import static org.junit.Assert.assertEquals;

@CoreTest
@RunWith(Parameterized.class)
public class KeywordProcessingUtilsKeywordCleaningTest {

    @Autowired
    private StopWordService stopWordService;

    @Parameterized.Parameter
    public String keyword;

    @Parameterized.Parameter(1)
    public String keywordAfterCleaning;

    @Parameterized.Parameters(name = "{0} -> {1}")
    public static Object[][] params() {
        return new Object[][]{
                {"[купить] [слона] дешево", "купить слона дешево"},
                {"где [купить слона] дешево", "где [купить слона] дешево"},
                {"[где] [купить слона] [дешево]", "где [купить слона] дешево"},
                {"[!купить] слона [+на] праздник", "!купить слона +на праздник"},
                {"[!купить] слона [на] праздник", "!купить слона +на праздник"},

                // проверка стоп-слов с учетом регистра
                {"[!купить] слона [+На] праздник", "!купить слона +На праздник"},
                {"[!купить] слона [На] праздник", "!купить слона +На праздник"},

                // проверка стоп-слов с учетом точки в конце
                {"[!купить] слона [+на.] праздник", "!купить слона +на. праздник"},
                {"[!купить] слона [на.] праздник", "!купить слона +на. праздник"},

                // проверка стоп-слов с учетом регистра и точки в конце
                {"[!купить] слона [+На.] праздник", "!купить слона +На. праздник"},
                {"[!купить] слона [На.] праздник", "!купить слона +На. праздник"},
        };
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
    }

    @Test
    public void cleanLoneWordsFromBrackets() {
        String actualKeyword = KeywordProcessingUtils
                .cleanLoneWordsFromBrackets(stopWordService, NormalizedKeyword.from(KeywordParser.parse(keyword)))
                .toString();
        assertEquals(actualKeyword, keywordAfterCleaning);
    }
}
