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
public class KeywordStopwordsFixerFixStopwordsTest {

    @Autowired
    private KeywordStopwordsFixer keywordStopwordsFixer;

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
    }

    @Parameterized.Parameters(name = "входные фразы: \"{0}\", ожидаемые фразы: \"{1}\"")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // комбинации с разными операторами + проверка учета регистра + проверка концевой точки
                {
                        singletonList("на"), singletonList("!на")
                },
                {
                        singletonList("На"), singletonList("!На")
                },
                {
                        singletonList("на."), singletonList("!на")
                },
                {
                        singletonList("+на"), singletonList("+на")
                },
                {
                        singletonList("+На"), singletonList("+На")
                },
                {
                        singletonList("+на."), singletonList("+на")
                },
                {
                        singletonList("!на"), singletonList("!на")
                },
                {
                        singletonList("!На"), singletonList("!На")
                },
                {
                        singletonList("!на."), singletonList("!на")
                },
                {
                        singletonList("конь на"), singletonList("конь !на")
                },
                {
                        singletonList("на конь"), singletonList("!на конь")
                },
                {
                        singletonList("конь на палка"), singletonList("конь !на палка")
                },
                {
                        singletonList("конь На палка"), singletonList("конь !На палка")
                },
                {
                        singletonList("конь На. палка"), singletonList("конь !На палка")
                },
                {
                        singletonList("конь !на палка"), singletonList("конь !на палка")
                },
                {
                        singletonList("на [конь]"), singletonList("!на [конь]")
                },
                {
                        singletonList("[конь] на"), singletonList("[конь] !на")
                },
                {
                        singletonList("[на]"), singletonList("[на]")
                },
                {
                        singletonList("[На]"), singletonList("[На]")
                },
                {
                        singletonList("[на.]"), singletonList("[на]")
                },
                {
                        singletonList("[ на]"), singletonList("[на]")
                },
                {
                        singletonList("[на ]"), singletonList("[на]")
                },
                {
                        singletonList("[конь на]"), singletonList("[конь на]")
                },
                {
                        singletonList("[конь на палка]"), singletonList("[конь на палка]")
                },
                {
                        singletonList("[конь На. палка]"), singletonList("[конь На палка]")
                },
                {
                        singletonList("дерево в [конь на]"), singletonList("дерево !в [конь на]")
                },
                {
                        singletonList("\"на\""), singletonList("\"на\"")
                },
                {
                        singletonList("\"На\""), singletonList("\"На\"")
                },
                {
                        singletonList("\" на\""), singletonList("\"на\"")
                },
                {
                        singletonList("\"на \""), singletonList("\"на\"")
                },
                {
                        singletonList("\"конь на\""), singletonList("\"конь на\"")
                },
                {
                        singletonList("\"[конь на]\""), singletonList("\"[конь на]\"")
                },
                {
                        singletonList("\"дерево в [конь на]\""), singletonList("\"дерево в [конь на]\"")
                },

                // другое стоп-слово
                {
                        singletonList("конь в вакууме"), singletonList("конь !в вакууме")
                },
                {
                        singletonList("[конь в вакууме]"), singletonList("[конь в вакууме]")
                },
                {
                        singletonList("\"конь в вакууме\""), singletonList("\"конь в вакууме\"")
                },

                // разделение по дефису + учет регистра
                {
                        singletonList("по-русски"), singletonList("!по русски")
                },
                {
                        singletonList("По-русски"), singletonList("!По русски")
                },
                {
                        singletonList("По-Русски"), singletonList("!По Русски")
                },
                {
                        singletonList("санкт-петербург"), singletonList("санкт петербург")
                },
                {
                        singletonList("Санкт Петербург"), singletonList("Санкт Петербург")
                },
                {
                        singletonList("Санкт Петербург."), singletonList("Санкт Петербург")
                },
                {
                        singletonList("\"по-русски\""), singletonList("\"по русски\"")
                },
                {
                        singletonList("[по-русски]"), singletonList("[по русски]")
                },
                {
                        singletonList("[По-Русски]"), singletonList("[По Русски]")
                },
                {
                        singletonList("[По-Русски.]"), singletonList("[По Русски]")
                },
                {
                        singletonList("\"По-Русски\""), singletonList("\"По Русски\"")
                },
                {
                        asList("\"конь в вакууме\"", "конь в вакууме", "конь [в вакууме]", "конь не в вакууме"),
                        asList("\"конь в вакууме\"", "конь !в вакууме", "конь [в вакууме]", "конь !не !в вакууме")
                }
        });
    }

    @Parameterized.Parameter(0)
    public List<String> inputKeywords;

    @Parameterized.Parameter(1)
    public List<String> expectedKeywordsWithFixedStopwords;

    @Test
    public void fixStopwords_WorksFine() {
        List<String> actualKeywordsWithFixedStopwords =
                keywordStopwordsFixer.fixStopwords(inputKeywords, WordKind.FIXED);
        assertThat("список фраз с зафиксированными стоп-словами соответствует ожидаемому",
                actualKeywordsWithFixedStopwords,
                beanDiffer(expectedKeywordsWithFixedStopwords));
    }

    @Test
    public void fixStopwords_SecondCallDoesNotChangeKeyword() {
        List<String> keywordsWithFixedStopwords =
                keywordStopwordsFixer.fixStopwords(inputKeywords, WordKind.FIXED);
        List<String> keywordsWithFixedStopwordsTwice =
                keywordStopwordsFixer.fixStopwords(keywordsWithFixedStopwords, WordKind.FIXED);
        assertThat("повторный вызов фиксации стоп-слов не должен изменять фразы",
                keywordsWithFixedStopwordsTwice,
                beanDiffer(keywordsWithFixedStopwords));
    }
}
