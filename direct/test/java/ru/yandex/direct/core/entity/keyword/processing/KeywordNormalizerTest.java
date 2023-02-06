package ru.yandex.direct.core.entity.keyword.processing;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.libs.keywordutils.model.Keyword;
import ru.yandex.direct.libs.keywordutils.parser.KeywordParser;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(Parameterized.class)
public class KeywordNormalizerTest {

    @Autowired
    private KeywordNormalizer keywordNormalizer;

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
    }

    @Parameterized.Parameters(name = "входные фразы: \"{0}\", ожидаемые фразы: \"{1}\"")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{

                // обрезание пробелов
                {" вакуумному коню ", "вакуумный конь"},

                // приведение к нижнему регистру
                {" вакуУмному Коню ", "вакуумный конь"},

                // работа с числами
                {" вакуумному 1 1.25 коню ", "1 1.25 вакуумный конь"},
                {" вакуумному +1 !1.25 коню ", "!1.25 1 вакуумный конь"},

                // удаление концевой точки
                {"вакуумному коню.", "вакуумный конь"},
                {"вакуумному. коню", "вакуумный конь"},
                {"!вакуумному. коню", "!вакуумному конь"},     // когда не заменяем слово на лексему
                {"!к. вакуумному коню", "!к вакуумный конь"},
                {"+к. вакуумному коню", "+к вакуумный конь"},
                {"[вакуумный.]", "[вакуумный]"},
                {"\"вакуумный.\"", "\"вакуумный\""},

                // замена слов на первые леммы и сортировка
                {"конь", "конь"},
                {"коню", "конь"},
                {"вакуумному коню", "вакуумный конь"},
                {"коню вакуумному", "вакуумный конь"},
                {"вакуумному !коню", "!коню вакуумный"},
                {"!вакуумному коню", "!вакуумному конь"},
                {"!вакуумному !коню", "!вакуумному !коню"},

                {"[конь]", "[конь]"},
                {"[коню]", "[конь]"},
                {"смотрят [вакуумному коню] зуб", "[вакуумный конь] зуб смотреть"},
                {"!смотрят [вакуумному !коню] в зуб", "!смотрят [вакуумный !коню] зуб"},
                {"!смотрят [вакуумному !коню] +в зуб", "!смотрят +в [вакуумный !коню] зуб"},

                {"\"конь\"", "\"конь\""},
                {"\"коню\"", "\"конь\""},
                {"\"смотрят [вакуумному коню] зуб\"", "\"[вакуумный конь] зуб смотреть\""},
                {"\"!смотрят [вакуумному !коню] в зуб\"", "\"!смотрят [вакуумный !коню] в зуб\""},
                {"\"[вакуумному !коню] +в зубы не !смотрят\"", "\"!смотрят +в [вакуумный !коню] зуб не\""},

                // удаление "+" у НЕ-стоп-слов
                {"+конь", "конь"},
                {"+коню", "конь"},
                {"+Коню", "конь"},     // не мешает регистр
                {"+коню.", "конь"},    // не мешает концевая точка

                {"+На.", "+на"},       // не мешает регистр
                {"+на.", "+на"},       // не мешает концевая точка

                {"+вакуумному коню", "вакуумный конь"},
                {"коню +вакуумному", "вакуумный конь"},

                {"+Не. +Вакуумному. !коню", "!коню +не вакуумный"}, // не мешает регистр и концевая точка

                {"[+вакуумному коню]", "[вакуумный конь]"},
                {"[коню +вакуумному]", "[конь вакуумный]"},
                {"[+не +вакуумному !коню]", "[+не вакуумный !коню]"},

                {"[не +коню] в +вакууме", "[не конь] вакуум"},

                {"\"+вакуумному коню\"", "\"вакуумный конь\""},
                {"\"коню +вакуумному\"", "\"вакуумный конь\""},
                {"\"+не +вакуумному !коню\"", "\"!коню +не вакуумный\""},

                {"\"[не +коню] в +вакууме\"", "\"[не конь] в вакуум\""},

                // обработка дефисов в слове
                {"санкт-петербург", "петербург санкт"},
                {"по-русски", "русски"},
                {"красно-черный", "красный черный"},
                {"в красно-черных цветах", "красный цвет черные"},
                {"продать трактор ТР-ТР", "продавать тр трактор"},

                //разделитель '
                {"купить зебру'недорого", "зебра купить недорого"},

                // удаление стоп-слов вне кавычек и квадратных скобок
                {"на", ""},
                {"!на", "!на"},
                {"+на", "+на"},

                {"на.", ""},      // не мешает концевая точка
                {"!на.", "!на"},  // не мешает концевая точка
                {"+на.", "+на"},  // не мешает концевая точка

                {"конь на", "конь"},
                {"конь на.", "конь"},       // не мешает концевая точка
                {"конь !на", "!на конь"},
                {"конь +на", "+на конь"},
                {"на конь", "конь"},
                {"на. конь", "конь"},       // не мешает концевая точка
                {"!на конь", "!на конь"},
                {"+на конь", "+на конь"},

                {"конь в вакуум", "вакуум конь"},
                {"конь !в вакуум", "!в вакуум конь"},
                {"конь +в вакуум", "+в вакуум конь"},

                {"конь въ вакуум", "вакуум конь"}, // стоп-слово с опечаткой

                // удаление стоп-слов в квадратных скобках
                {"[на]", "[на]"},
                {"[!на]", "[!на]"},
                {"[+на]", "[+на]"},

                {"[конь на]", "[конь на]"},
                {"[конь !на]", "[конь !на]"},
                {"[конь +на]", "[конь +на]"},
                {"[на конь]", "[на конь]"},
                {"[!на конь]", "[!на конь]"},
                {"[+на конь]", "[+на конь]"},

                {"[конь в вакуум]", "[конь в вакуум]"},

                {"[по-русски]", "[по русски]"},
                {"[!по-русски]", "[!по !русски]"},
                {"[+по-русски]", "[+по русски]"},

                {"в вакуум [конь на]", "[конь на] вакуум"},
                {"[конь на] в вакуум", "[конь на] вакуум"},
                {"[конь на] !в вакуум", "!в [конь на] вакуум"},
                {"[конь на] +в вакуум", "+в [конь на] вакуум"},

                // удаление стоп-слов в кавычках
                {"\"на\"", "\"на\""},
                {"\"!на\"", "\"!на\""},
                {"\"+на\"", "\"+на\""},

                {"\"конь на\"", "\"конь на\""},
                {"\"конь !на\"", "\"!на конь\""},
                {"\"на конь\"", "\"конь на\""},
                {"\"конь +на\"", "\"+на конь\""},
                {"\"!на конь\"", "\"!на конь\""},
                {"\"+на конь\"", "\"+на конь\""},

                {"\"по-русски\"", "\"по русски\""},
                {"\"+по-русски\"", "\"+по русски\""},
                {"\"!по-русски\"", "\"!по !русски\""},

                {"\"конь в вакуум\"", "\"в вакуум конь\""},

                {"\"в вакуум на конь\"", "\"в вакуум конь на\""},

                {"\"в вакуум [на конь]\"", "\"[на конь] в вакуум\""},

                // удаление дубликатов
                {"конь конь", "конь"},
                {"конь коню", "конь"},
                {"коню конь", "конь"},
                {"коню !конь", "!конь"},
                {"коню +конь", "конь"},
                {"+коню конь", "конь"},
                {"конь [конь]", "[конь] конь"},
                {"[вакуумный конь] конь [вакуумный конь]", "[вакуумный конь] конь"},
                {"[вакуумный !конь] конь [вакуумный конь]", "[вакуумный !конь] конь"},
                {"[вакуумный +конь] конь [вакуумный конь]", "[вакуумный конь] конь"},
                {"[вакуумному коню] конь [вакуумный конь]", "[вакуумный конь] конь"},
                {"[вакуумному +коню] конь [вакуумный конь]", "[вакуумный конь] конь"},
                {"[вакуумный конь] конь [конь вакуумный]", "[вакуумный конь] [конь вакуумный] конь"},

                {"\"конь конь\"", "\"конь\""},
                {"\"конь коню\"", "\"конь\""},
                {"\"коню конь\"", "\"конь\""},
                {"\"коню !конь\"", "\"!конь\""},
                {"\"коню +конь\"", "\"конь\""},
                {"\"+коню конь\"", "\"конь\""},
                {"\"конь [конь]\"", "\"[конь] конь\""},
                {"\"[вакуумный конь] конь [вакуумный конь]\"", "\"[вакуумный конь] конь\""},
                {"\"[вакуумный !конь] конь [вакуумный конь]\"", "\"[вакуумный !конь] конь\""},
                {"\"[вакуумный +конь] конь [вакуумный конь]\"", "\"[вакуумный конь] конь\""},
                {"\"[вакуумному коню] конь [вакуумный конь]\"", "\"[вакуумный конь] конь\""},
                {"\"[вакуумному +коню] конь [вакуумный конь]\"", "\"[вакуумный конь] конь\""},
                {"\"[вакуумный конь] конь [конь вакуумный]\"", "\"[вакуумный конь] [конь вакуумный] конь\""},


                //Когда слово "будет" - стоп-слово со знаком "+", то не заменяем на первую лемму
                {"сколько +будет стоить диагностика автомобиля", "+будет автомобиль диагностик сколько стоить"},
                //у слова "бывшая" две леммы быть (стоп-слово) и бывший
                {"бывшая жена", "бывший жена"},
                {"[бывшая жена]", "[быть жена]"},
                {"\"бывшая жена\"", "\"бывший жена\""},

                // слова с символами из разного алфавита
                {"кocатка", "кocатка"},
                {"[кocатка]", "[кocатка]"},
                {"\"кocатка\"", "\"кocатка\""},
                {"кocатка на", "кocатка"},
                {"кocатка на.", "кocатка"},         // не мешает концевая точка
                {"кocатка !на", "!на кocатка"},
                {"кocатка +на", "+на кocатка"},
                {"на кocатка", "кocатка"},
                {"на. кocатка", "кocатка"},         // не мешает концевая точка
                {"!на кocатка", "!на кocатка"},
                {"+на кocатка", "+на кocатка"},
                {"!кocатка", "!кocатка"},
                {"+кocатка", "кocатка"},
                {"кocатка.", "кocатка"},            // не мешает концевая точка
                {"!кocатка.", "!кocатка"},          // не мешает концевая точка
                {"+кocатка.", "кocатка"},           // не мешает концевая точка
                {"[кocатка.]", "[кocатка]"},        // не мешает концевая точка
                {"[!по-руccки]", "[!по !руccки]"},
                {"[+по-руccки]", "[+по руccки]"},
        });
    }

    @Parameterized.Parameter(0)
    public String inputKeyword;

    @Parameterized.Parameter(1)
    public String expectedNormalKeyword;

    @Test
    public void normalizeKeyword_AsString_WorksFine() {
        String actualNormalKeyword = keywordNormalizer.normalizeKeyword(inputKeyword);
        assertThat("полученная нормализованная фраза не соответствует ожидаемой",
                actualNormalKeyword, equalTo(expectedNormalKeyword));
    }

    @Test
    public void normalizeKeyword_AsKeywordObject_WorksFine() {
        Keyword keywordObj = KeywordParser.parse(inputKeyword.trim());
        Keyword actualNormalKeywordObj = keywordNormalizer.normalizeKeyword(keywordObj).getNormalized();
        assertThat("полученная нормализованная фраза не соответствует ожидаемой",
                actualNormalKeywordObj.toString(), equalTo(expectedNormalKeyword));
    }
}