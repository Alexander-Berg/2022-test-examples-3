package ru.yandex.ir.common.features.extractors.parameters;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ExtractedParametersSpacerTest implements ArgumentsProvider {
    protected String[] getTitles() {
        return new String[]{
                "22*31 / 66x 42",
                "22*31 / 66x 42, высота 24см",
                "Противень Демидово МТ043 средний алюминиевый\"",
                "Размеры: 320х350х36мм<br />\\nсредний<br />\\nвес - 500г",
                "Противень 320*350*48мм средний МТ-043",
                "Противень",
                "Спицы круговые 2мм/40см, Prym, 211200",
                "Характеристики: Высота, мм: 117.5; Длина, мм: 150; Ширина, мм: 150; Мощность, эквивалентная лампам накаливания, Вт: 50; Вес, кг: 0.73; Степень защиты, IP: 54;",
                "Набор для вышивания крестом Dimensions \"Вид на кафе\", 18x13 см, арт. 65093",
                "Дефлекторы окон Vinguru Kia Picanto II 2011-2017 хетчбек накладные скотч 4 шт.,материал акрил, AFV50211",
                "Аккумулятор TopON для электроинструмента AEG 12V 1.5Ah (Ni-Cd) PN: B1214G, B1215R, B1220R, M1230R.",
                "Чайник 2,7л ZILLINGER ZL-5001-27 зеленый",
                "Подвесной светильник ST-Luce Gocce SL874.503.01-M",
                "Abracadabra clx2544",
                "Состав: 25% мохер, 24% шерсть, 51% акрилДлина: 200 мВес: 100 г",
                "45 смлово на см",
                "словом: 45"//, TODO the next test fails due to colors extractor, not supporting word boundaries
                //"Кофе молотый İstanbul Türk Kahvesi, мягкая упаковка, 100 гр"
        };
    }

    protected List<String> getExpectedValues() {
        return Arrays.asList(
                "22 * 31 / 66 x 42",
                "22 * 31 / 66 x 42 , высота 24 см",
                "Противень Демидово МТ 043 средний алюминиевый\"",
                "Размеры: 320 х 350 х 36 мм <br />\\nсредний<br />\\nвес - 500 г",
                "Противень 320 * 350 * 48 мм средний МТ - 043",
                "Противень",
                "Спицы круговые 2 мм / 40 см , Prym, 211200",
                "Характеристики: Высота, мм : 117 . 5 ; Длина, мм : 150 ; Ширина, мм : 150 ; Мощность, эквивалентная лампам накаливания, Вт : 50 ; Вес, кг : 0 . 73 ; Степень защиты, IP: 54 ;",
                "Набор для вышивания крестом Dimensions \"Вид на кафе\", 18 x 13 см , арт. 65093",
                "Дефлекторы окон Vinguru Kia Picanto II 2011 - 2017 хетчбек накладные скотч 4 шт .,материал акрил, AFV 50211",
                "Аккумулятор TopON для электроинструмента AEG 12 V 1 . 5 Ah (Ni-Cd) PN: B 1214 G , B 1215 R , B 1220 R , M 1230 R .",
                "Чайник 2 , 7 л ZILLINGER ZL - 5001 - 27 зеленый",
                "Подвесной светильник ST-Luce Gocce SL 874 . 503 . 01 - M",
                "Abracadabra clx 2544",
                "Состав: 25 % мохер, 24 % шерсть, 51 % акрилДлина: 200 мВес: 100 г",
                "45 смлово на см",
                "словом: 45"//,
                //"Кофе молотый İstanbul Türk Kahvesi, мягкая упаковка, 100 гр"
        );
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        String[] titles = getTitles();
        List<String> expectedValues = getExpectedValues();
        return IntStream.range(0, titles.length).mapToObj(i -> Arguments.of(titles[i], expectedValues.get(i)));
    }

    @ParameterizedTest()
    @org.junit.jupiter.params.provider.ArgumentsSource(ExtractedParametersSpacerTest.class)
    public void testSurroundParameterValuesWithSpaces(String title, String expectedProcessesTitle) {
        Assertions.assertEquals(expectedProcessesTitle, ExtractedParametersSpacer.surroundParameterValuesWithSpaces(title));
    }
}
