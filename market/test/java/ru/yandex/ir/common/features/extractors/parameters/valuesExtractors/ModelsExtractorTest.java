package ru.yandex.ir.common.features.extractors.parameters.valuesExtractors;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import static ru.yandex.ir.common.features.extractors.parameters.valuesExtractors.ModelsExtractor.Model;

public class ModelsExtractorTest extends AbstractValuesExtractorTest<Model> {

    private static final String bigDescription = "|ОПИСАНИЕ: АРТИКУЛ: 12005744РЕМКОМПЛЕКТ ПОДДОНА ОРИГИНАЛЬНЫЙ" +
            ".АРТИКУЛ: 12005744.  СИЛА СОПРОТИВЛЕНИЯ 80-110N.ЗАМЕНА MTR515BO.  ПРИМЕНЕНИЕ: ДЛЯ ПОСУДОМОЕЧНЫХ МАШИН " +
            "BOSCH, SIEMENS, NEFF, GAGGENAU.  СОВМЕСТИМЫЕ " +
            "МОДЕЛИDF260161F/21DF261161/21DF460161/21DI260130/21DI260130/32DI260411/21DI260411/32S21M50N3EU" +
            "/32S21M53N3EU/21S21M53N3EU/25S21M53N4EU/32S21M58N3EU/21S21M58N3EU/23S21M58N3EU/25S21M58N7EU/25S21M68N2DE" +
            "/28S21M69N2EU/32S21M85N4DE/01S21M85N4DE/25S21M85N4DE/32S21M86N2DE/28S21M86N2DE/32S21N63N0EU/21S21N63N0EU" +
            "/25S21N63N0EU/28S21N65N1EU/21S21N65N1EU/22S21N65N1EU/25S21N69N1EU/21S21N69N1EU/25S21N85N0DE/21S21T69N5EU" +
            "/32S31M65B3EU/32S31M65W3EU/32S31M85B2DE/21S31M85W2DE/21S31N63B0EU/21S31N63B0EU/25S31N63W0EU/21S31N63W0EU" +
            "/25S41M50N0EU/21S41M50N0EU/28S41M50N1EU/21S41M50N3EU/32S41M53N0EU/21S41M53N0EU/28S41M53N1EU/25S41M53N2EU" +
            "/21S41M53N3EU/21S41M53N4EU/32S41M53N7EU/25S41M58N1EU/25S41M58N2EU/21S41M58N2EU/28S41M58N3EU/25S41M58N3EU" +
            "/28S41M58N5EU/32S41M58N7EU/32S41M65B4EU/25S41M65N4EU/01S41M65N4EU/25S41M65W4EU/25S41M65W4EU/32S41M68N1EU" +
            "/21S41M68N1EU/25S41M68N3EU/32S41M85B3DE/25S41M85B4DE/01S41M85B4DE/25S41M85B4DE/32S41M85N3DE/25S41M85N4DE" +
            "/25S41M85W3DE/21S41M85W3DE/25S41M85W4DE/25S41M86B2DE/32S41M86N1DE/32S41M86N2DE/28S41M86N2DE/32S41M86N3DE" +
            "/25S41M86W2DE/28S41N63B0EU/21S41N63N0EU/21S41N63S0EU/25S41N63W0EU/21S41N65N1EU/21S41N65N1EU/25S41N68N0EU" +
            "/21S41N69N1EU/21S41N85N0DE/21S41N85N0DE/28S41N85N1DE/21S41N85N1DE/28S41T69N0EU/21S41T69N3EU/25S41T69N3EU" +
            "/32S41T69N5EU/25S41T69N5EU/32S42M53N0EU/21S42M53N1EU/25S42M53N1EU/28S42M53N2EU/21S42M53N4EU/25S42M53N4EU" +
            "/32S42M58N1EU/25S42M69N3EU/32S42M69N5EU/32S42N63N0EU/21S42N65N1EU/21S42N65N1EU/25S42N68N0EU/21S42N69N1EU" +
            "/21S42N69N1EU/25S42T65N2EU/25S42T69N0EU/21S42T69N2EU/25S42T69N3EU/32S51E40X0EU/21S51E50X1EU/21S51M40X0RU" +
            "/32S51M50X0EU/21S51M50X1EU/21S51M50X1EU/26S51M53X0EU/21S51M53X1EU/28S51M53X2EU/21S51M53X3EU/25S51M53X4EU" +
            "/25S51M53X4EU/32S51M58X2EU/21S51M58X2EU/25S51M58X3EU/23S51M58X3EU/25S51M58X3EU/32S51M60X0EU/21S51M60X0EU" +
            "/32S51M63X5EU/01S51M65X4EU/32S51M68X1EU/21S51M69X2EU/21S51M69X3EU/25S51M69X4EU/25S51M69X4EU/32S51M69X5EU" +
            "/01S51M69X5EU/32S51M69X6EU/32S51M86X3DE/25S51N63X0EU/21S51N65X1EU/21S51N68X0EU/21S51N68X0EU/21S51N69X1EU" +
            "/21S51N69X1EU/22S51N85X0DE/21S51N85X0DE/22S51T59X0EU/21S51T65X0EU/21S51T65X2EU/25S51T65X3EU/25S51T65X3EU" +
            "/32S51T65X4EU/25S51T65X4RU/32S51T69X1EU/21S51T69X2EU/28S51T69X3EU/25S51T69X3EU/32S51T69X4EU/01S51T69X5EU" +
            "/32S52E50X1GB/21S52M50X0EU/21S52M50X3EU/32S52M53X0EU/21S52M53X2EU/21S52M53X3EU/25S52M53X5EU/32S52M58X1EU" +
            "/28S52M58X2EU/21S52M58X2EU/25S52M58X3EU/23S52M58X3EU/25S52M58X3EU/32S52M63X1EU/25S52M63X1EU/28S52M63X3EU" +
            "/32S52M63X4EU/01S52M65X3EU/32S52M65X6EU/32S52M68X1EU/21S52M68X1EU/25S52M68X3EU/32S52M69X1GB/21S52M69X1GB" +
            "/22S52M69X5EU/25S52M69X5EU/32S52N65X1EU/21S52N65X1EU/22S52N68X0EU/21S52N69X1EU/21S52T65X0EU/21S52T65X2EU" +
            "/25S52T69X1EU/21S52T69X2EU/28S52T69X3EU/25S52T69X3EU/32S52T69X5EU/23S52T69X5EU/25S66M63N1RU/03S71M65X3EU" +
            "/01S71M65X3EU/25S71M68X0EU/21S71T59X0EU/21S71T59X0EU/28S71T69X4EU/25S72M65X3EU/01S72M65X3EU/25S|КОД " +
            "ПРОИЗВОДИТЕЛЯ: 12005744";

    @Override
    protected String[] getTitles() {
        return new String[]{
                "Дефлекторы окон Vinguru Kia Picanto II 2011-2017 хетчбек накладные скотч 4 шт.,материал акрил, " +
                        "AFV50211",
                "Аккумулятор TopON для электроинструмента AEG 12V 1.5Ah (Ni-Cd) PN: B1214G, B1215R, B1220R, M1230R.",
                "Чайник 2,7л ZILLINGER ZL-5001-27 зеленый",
                "Подвесной светильник ST-Luce Gocce SL874.503.01-M",
                "22*31 / 66x 42",
                "Противень 320*350*48мм средний МТ-043",
                "Abracadabra clx2544"
        };
    }

    @Override
    protected List<List<Model>> getExpectedValues() {
        return Arrays.asList(
                Arrays.asList(new Model(Arrays.asList("AFV", "50211"))),
                Arrays.asList(new Model(Arrays.asList("B", "1214", "G")), new Model(Arrays.asList("B", "1215", "R")),
                        new Model(Arrays.asList("B", "1220", "R")), new Model(Arrays.asList("M", "1230", "R"))),
                Arrays.asList(new Model(Arrays.asList("ZL", "5001", "27"))),
                Arrays.asList(new Model(Arrays.asList("SL", "874", "503", "01", "M"))),
                Arrays.asList(),
                Arrays.asList(new Model(Arrays.asList("МТ", "043"))),
                Arrays.asList(new Model(Arrays.asList("CLX", "2544")))
        );
    }

    @Override
    protected ValuesExtractor<Model> getValuesExtractor() {
        return new ModelsExtractor();
    }

    @ParameterizedTest()
    @org.junit.jupiter.params.provider.ArgumentsSource(ModelsExtractorTest.class)
    public void testExtractValues(String title, List<Model> expectedValues) {
        super.testExtractValues(title, expectedValues);
    }

    @Test
    public void bigDescriptionTest() {
        getValuesExtractor().extractParamValues(bigDescription);
    }


}
