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
public class PhrasePredicatesJustStopWordsPositiveTest {

    @Autowired
    private StopWordService stopWordService;

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"фраза"},
                {"!фраза"},
                {"+фраза"},
                {"[фраза]"},
                {"[!фраза]"},
                {"[+фраза]"},
                {"\"фраза\""},
                {"\"!фраза\""},
                {"\"+фраза\""},
                {"\"[фраза]\""},
                {"\"[!фраза]\""},
                {"\"[+фраза]\""},

                {"фраза -минус"},

                {"фраза с"},
                {"фраза !с"},
                {"фраза +с"},
                {"[фраза] +с"},
                {"\"[фраза] +с\""},

                {"c фраза"},

                {"длинная фраза"},
                {"длинная фраза с"},
                {"длинная фраза с -минус"},
                {"длинная фраза -с"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(notOnlyStopWords(stopWordService, parseWithMinuses(keyword)).test(keyword), is(true));
    }
}
