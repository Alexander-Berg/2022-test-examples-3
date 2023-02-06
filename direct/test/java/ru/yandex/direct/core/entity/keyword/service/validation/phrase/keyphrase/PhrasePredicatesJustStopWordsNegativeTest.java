package ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.stopword.service.StopWordService;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhrasePredicates.notOnlyStopWords;
import static ru.yandex.direct.libs.keywordutils.parser.KeywordParser.parseWithMinuses;

@CoreTest
@RunWith(Parameterized.class)
public class PhrasePredicatesJustStopWordsNegativeTest {

    @Autowired
    private StopWordService stopWordService;

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"с"},
                {"!с"},
                {"+с"},
                {"[с]"},
                {"[!с]"},
                {"\"с\""},
                {"\"!с\""},
                {"\"[с]\""},
                {"\"[!с]\""},

                {"с на"},
                {"[с] на"},
                {"с [на]"},
                {"[с] [на]"},
                {"[с на]"},
                {"с -на"},
                {"с -!на"},
                {"с -+на"},

                {"с -минус"},
                {"с -минус -два"},
                {"с на -минус"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(notOnlyStopWords(stopWordService, parseWithMinuses(keyword)).test(keyword), is(false));
    }
}
