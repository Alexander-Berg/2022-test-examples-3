package ru.yandex.direct.core.entity.keyword.processing;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.advq.query.ast.WordKind;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@CoreTest
@RunWith(Parameterized.class)
public class KeywordStopwordsFixerUnquoteAndFixStopwordsTest {

    @Autowired
    private KeywordStopwordsFixer keywordStopwordsFixer;

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
    }

    @Parameterized.Parameters(name = "входные фразы: \"{0}\", ожидаемые фразы: \"{1}\"")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{

                // ничего не делает с фразами, не заключенными в кавычки
                {
                        singletonList("на"), singletonList("на")
                },
                {
                        singletonList("На"), singletonList("На")
                },
                {
                        singletonList("на."), singletonList("на.")
                },
                {
                        singletonList("На."), singletonList("На.")
                },
                {
                        singletonList("+на"), singletonList("+на")
                },
                {
                        singletonList("!на"), singletonList("!на")
                },
                {
                        singletonList("конь на"), singletonList("конь на")
                },
                {
                        singletonList("на конь"), singletonList("на конь")
                },
                {
                        singletonList("конь на палка"), singletonList("конь на палка")
                },

                {
                        singletonList("[на]"), singletonList("[на]")
                },
                {
                        singletonList("[На]"), singletonList("[На]")
                },
                {
                        singletonList("[На.]"), singletonList("[На.]")
                },
                {
                        singletonList("[+на]"), singletonList("[+на]")
                },
                {
                        singletonList("[!на]"), singletonList("[!на]")
                },
                {
                        singletonList("[конь на]"), singletonList("[конь на]")
                },
                {
                        singletonList("[на конь]"), singletonList("[на конь]")
                },
                {
                        singletonList("[конь на палка]"), singletonList("[конь на палка]")
                },
                {
                        singletonList("дерево в [конь на]"), singletonList("дерево в [конь на]")
                },
                {
                        singletonList("по-русски"), singletonList("по-русски")
                },

                // у фраз в кавычках убирает их и фиксирует стоп-слова

                {
                        singletonList("\"на\""), singletonList("+на")
                },
                {
                        singletonList("\"На\""), singletonList("+На")
                },
                {
                        singletonList("\"На.\""), singletonList("+На")
                },
                {
                        singletonList("\"+на\""), singletonList("+на")
                },
                {
                        singletonList("\"!на\""), singletonList("!на")
                },
                {
                        singletonList("\"конь на\""), singletonList("конь +на")
                },
                {
                        singletonList("\"на конь\""), singletonList("+на конь")
                },
                {
                        singletonList("\"конь на палка\""), singletonList("конь +на палка")
                },
                {
                        singletonList("\"[конь на]\""), singletonList("[конь на]")
                },
                {
                        singletonList("\"дерево в [конь на]\""), singletonList("дерево +в [конь на]")
                },
                {
                        singletonList("\"по-русски\""), singletonList("+по русски")
                },
                {
                        asList("\"конь в вакууме\"", "конь в вакууме", "\"конь [в вакууме]\"", "\"конь не в вакууме\""),
                        asList("конь +в вакууме", "конь в вакууме", "конь [в вакууме]", "конь +не +в вакууме")
                }
        });
    }

    @Parameterized.Parameter(0)
    public List<String> inputKeywords;

    @Parameterized.Parameter(1)
    public List<String> expectedKeywordsWithFixedStopwords;

    @Test
    public void unquoteAndFixStopwords_WorksFine() {
        List<String> actualKeywordsWithFixedStopwords =
                keywordStopwordsFixer.unquoteAndFixStopwords(inputKeywords, WordKind.PLUS);
        assertThat("список фраз с зафиксированными стоп-словами соответствует ожидаемому",
                actualKeywordsWithFixedStopwords,
                beanDiffer(expectedKeywordsWithFixedStopwords));
    }

    @Test
    public void unquoteAndFixStopwords_SecondCallDoesNotChangeKeyword() {
        List<String> keywordsWithFixedStopwords =
                keywordStopwordsFixer.unquoteAndFixStopwords(inputKeywords, WordKind.PLUS);
        List<String> keywordsWithFixedStopwordsTwice =
                keywordStopwordsFixer.unquoteAndFixStopwords(keywordsWithFixedStopwords, WordKind.PLUS);
        assertThat("повторный вызов фиксации стоп-слов не должен изменять фразы",
                keywordsWithFixedStopwordsTwice,
                beanDiffer(keywordsWithFixedStopwords));
    }
}
