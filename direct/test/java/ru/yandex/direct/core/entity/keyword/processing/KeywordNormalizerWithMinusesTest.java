package ru.yandex.direct.core.entity.keyword.processing;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.libs.keywordutils.model.KeywordWithMinuses;
import ru.yandex.direct.libs.keywordutils.parser.KeywordParser;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(Parameterized.class)
public class KeywordNormalizerWithMinusesTest {

    @Autowired
    private KeywordNormalizer keywordNormalizer;

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
    }

    @Parameterized.Parameters(name = "входные фразы: \"{0}\", ожидаемые фразы: \"{1}\"")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // приведение к нижнему регистру
                {"слон -вакуУмному -!Коню", "слон -!коню -вакуумный"},

                // числа
                {"слон -вакуумному -1 -1.25", "слон -1 -1.25 -вакуумный"},
                {"слон -вакуумному -+1 -!1.25", "слон -!1.25 -1 -вакуумный"},

                // удаление концевых точек
                {"слон -вакуумному. -+1. -!1.25", "слон -!1.25 -1 -вакуумный"},
                {"слон -!вакуумному. -!1. -!1.25", "слон -!1 -!1.25 -!вакуумному"},

                // замена слов на первые леммы и сортировка
                {"слон -конь", "слон -конь"},
                {"слон -коню", "слон -конь"},
                {"слон -вакуумному -коню", "слон -вакуумный -конь"},
                {"слон -коню -вакуумному", "слон -вакуумный -конь"},
                {"слон -вакуумному -!коню", "слон -!коню -вакуумный"},
                {"!вакуумному !коню", "!вакуумному !коню"},
                {"слон -+в -зубы -не -!смотрят -коню", "слон -!не -!смотрят -+в -зуб -конь"},

                // удаление "+" у НЕ-стоп-слов
                {"слон -+конь", "слон -конь"},
                {"слон -+коню", "слон -конь"},
                {"слон -+коню.", "слон -конь"}, // не мешает точка в конце
                {"слон -+Коню", "слон -конь"},  // не мешает регистр
                {"слон -+не -+вакуумному -!коню", "слон -!коню -+не -вакуумный"},

                // добавление "!" стоп-словам
                {"слон -+коню -в -+вакууме", "слон -!в -вакуум -конь"},
                {"слон -+коню -в. -+вакууме", "слон -!в -вакуум -конь"}, // не мешает точка в конце
                {"слон -+коню -В -+вакууме", "слон -!в -вакуум -конь"}, // не мешает регистр

                // удаление дубликатов
                {"слон -конь -конь", "слон -конь"},
                {"слон -конь -коню", "слон -конь"},
                {"слон -коню -конь", "слон -конь"},
                {"слон -коню -!конь", "слон -!конь -конь"},
                {"слон -коню -+конь", "слон -конь"},
                {"слон -+коню -конь", "слон -конь"},
                {"слон -!конь -конь -конь", "слон -!конь -конь"},

                //обработка дефисов в минус-слове
                {"купить -санкт-петербург", "купить -петербург -санкт"},
                {"купить -по-русски", "купить -!по -русски"},
                {"купить -!по-русски", "купить -!по -!русски"},
                {"купить -красно-черный", "купить -красный -черный"},
                {"купить -в -красно-черных -цветах", "купить -!в -красный -цвет -черные"},
        });
    }

    @Parameterized.Parameter
    public String inputKeyword;

    @Parameterized.Parameter(1)
    public String expectedNormalKeyword;

    @Test
    public void normalizeKeywordWithMinusWords_WorksFine() {
        KeywordWithMinuses keywordWithMinusWords = KeywordParser.parseWithMinuses(inputKeyword.trim());
        KeywordWithMinuses actualNormalKeyword = keywordNormalizer.normalizeKeywordWithMinuses(keywordWithMinusWords);
        assertThat("полученная нормализованная фраза не соответствует ожидаемой",
                actualNormalKeyword.toString(), equalTo(expectedNormalKeyword));
    }
}
