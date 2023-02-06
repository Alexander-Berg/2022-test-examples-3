package ru.yandex.direct.core.entity.keyword.service;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.keyword.processing.KeywordNormalizer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.libs.keywordutils.model.KeywordWithMinuses;
import ru.yandex.direct.libs.keywordutils.parser.KeywordParser;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(Parameterized.class)
public class KeywordUtilsCoverageChangedTest {

    @Autowired
    private KeywordNormalizer keywordNormalizer;

    @Parameterized.Parameter
    public String oldPhrase;

    @Parameterized.Parameter(1)
    public String newPhrase;

    @Parameterized.Parameter(2)
    public Boolean expectedIsChanged;

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
    }

    @Parameterized.Parameters(name = "старая фраза: {0}, новая фраза: {1}, изменилась ли сильно {2}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                //нормальная форма не изменилась
                {"вакуумный конь", "вакуумному коню", false},
                {"[вакуумный конь]", "[вакуумному коню]", false},
                {"\"вакуумный конь\"", "\"вакуумному коню\"", false},

                //нормальная форма не изменилась с операторами
                {"вакуумный !конь", "вакуумному !конь", false},
                {"вакуумный +в конь", "вакуумному +в коню", false},

                //охват изменился
                {"вакуумный конь", "вакуумному слону", true},
                {"[вакуумный конь]", "[вакуумному слону]", true},
                {"\"вакуумный конь\"", "\"вакуумному слону\"", true},
                {"слоны [умеют летать] но не очень", "слоны [умеют летать]", true},

                //охват изменился с оператором !
                {"вакуумный !конь", "вакуумному конь", true},
                {"вакуумный конь", "вакуумному !конь", true},
                {"[вакуумный !конь]", "[вакуумному коню]", true},
                {"[вакуумный конь]", "[вакуумному !коню]", true},
                {"\"вакуумный !конь\"", "\"вакуумному коню\"", true},
                {"\"вакуумный конь\"", "\"вакуумному !коню\"", true},

                //охват изменился с оператором +
                {"вакуумный +в", "вакуумному в", true},
                {"[вакуумный +в]", "[вакуумному в]", true},
                {"[вакуумный в]", "[вакуумному +в]", true},
                {"\"вакуумный +в\"", "\"вакуумному в\"", true},
                {"\"вакуумный в\"", "\"вакуумному +в\"", true},

                //если не в скобках или кавычках в нормальной фразе удаляются предлоги без +
                //т.е. добавилось "в"
                {"вакуумный в", "вакуумному +в", false},

                //охват изменился + добавилось слово
                {"вакуумный конь", "вакуумному слону сердитому", true},
                {"[вакуумный конь]", "[вакуумному слону сердитому]", true},
                {"\"вакуумный конь\"", "\"вакуумному слону сердитому\"", true},

                //сузился охват
                {"вакуумный конь", "вакуумному коню сердитому", false},
                {"\"вакуумный конь\"", "\"вакуумному коню сердитому\"", false},
                {"слоны [умеют летать]", "слоны [умеют летать] но не очень", false},

                //расширился охват
                {"вакуумному коню сердитому", "вакуумный конь", true},
                {"\"вакуумному коню сердитому\"", "\"вакуумный конь\"", true},

                //слова в квадратных скобках считаются как одно слово
                {"[вакуумный конь]", "[вакуумному коню сердитому]", true},
                {"[вакуумному коню сердитому]", "[вакуумный конь]", true},

                //перестановка
                {"[вакуумный слон] [вакуумному коню]", "[вакуумный конь] [вакуумный слон]", false},

                //добавление/удаление скобок
                {"вакуумный конь", "[вакуумному коню]", true},
                {"[вакуумный конь]", "вакуумному коню", true},

                //добавление/удаление кавычек
                {"вакуумный конь", "\"вакуумному коню\"", false},
                {"\"вакуумный конь\"", "вакуумному коню", true},

                //минус-слова не влияют
                {"вакуумному коню", "вакуумный конь -сердитому", false},
                {"вакуумному коню -сердитому", "вакуумный конь", false},
        });
    }

    @Test
    public void checkIsPhraseCoverageSignificantlyChanged() {
        KeywordWithMinuses newKeywordWithMinuses =
                keywordNormalizer.normalizeKeywordWithMinuses(KeywordParser.parseWithMinuses(newPhrase));
        KeywordWithMinuses oldKeywordWithMinuses =
                keywordNormalizer.normalizeKeywordWithMinuses(KeywordParser.parseWithMinuses(oldPhrase));
        boolean isChanged =
                KeywordUtils.isPhraseCoverageSignificantlyChanged(oldKeywordWithMinuses, newKeywordWithMinuses);
        assertThat("результат сравнения соответствует ожиданию",
                isChanged, equalTo(expectedIsChanged));
    }

}
