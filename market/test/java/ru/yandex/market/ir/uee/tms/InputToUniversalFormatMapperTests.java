package ru.yandex.market.ir.uee.tms;

import Market.IR.Uee;
import lombok.SneakyThrows;
import org.junit.Test;
import org.xml.sax.SAXParseException;

import ru.yandex.market.ir.uee.tms.yt.InputToUniversalFormatMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InputToUniversalFormatMapperTests {

    @SneakyThrows
    @Test
    public void tabDelimitedParams_withJoinedUnits_shouldBeAccepted() {
        // arrange
        var tabDelimitedParams =
            "Учебный период\t4 класс\n" +
            "Дисциплины\tАнглийский язык\n" +
            "Высота предмета\t21.5 см\n" +
            "Глубина предмета\t0.6 см\n" +
            "Ширина предмета\t16.2 см\n" +
            "Страна производства\tРоссия\n" +
            "Комплектация\tкнига\n";

        // act
        var ymlParams = InputToUniversalFormatMapper.parseYmlParams(tabDelimitedParams, null);

        // assert
        assertEquals(7, ymlParams.size());
        assertTrue(ymlParams.stream().allMatch(p -> p.hasParamName() && p.hasParamValue() && !p.hasUnit()));
    }

    @SneakyThrows
    @Test
    public void tabDelimitedParams_withSeparateUnits_shouldBeAccepted() {
        // arrange
        var tabDelimitedParams =
            "Учебный период\t4 класс\n" +
            "Дисциплины\tАнглийский язык\n" +
            "Высота предмета\t21.5\tсм\n" +
            "Глубина предмета\t0.6\tсм\n" +
            "Ширина предмета\t16.2\tсм\n" +
            "Страна производства\tРоссия\n" +
            "Комплектация\tкнига\n";

        // act
        var ymlParams = InputToUniversalFormatMapper.parseYmlParams(tabDelimitedParams, null);

        // assert
        assertEquals(3, ymlParams.stream().filter(Uee.Param::hasUnit).count());
    }

    @SneakyThrows
    @Test
    public void tabDelimitedParams_withoutValues_shouldBeIgnored() {
        // arrange
        var tabDelimitedParams =
            "Учебный период\n" +
            "Дисциплины\t\n" +
            "Комплектация\tкнига\n";

        // act
        var ymlParams = InputToUniversalFormatMapper.parseYmlParams(tabDelimitedParams, null);

        // assert
        assertEquals(1, ymlParams.size());
    }

    @SneakyThrows
    @Test
    public void xmlParams_shouldBeAccepted() {
        // arrange
        var xmlParams =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<offer_params>" +
            "  <param name=\"delivery_height\" unit=\"см\">2</param>" +
            "  <param name=\"delivery_length\" unit=\"см\">20.5</param>" +
            "  <param name=\"delivery_weight\" unit=\"кг\">0.225</param>" +
            "  <param name=\"delivery_width\" unit=\"см\">5.5</param>" +
            "  <param name=\"vendor\" unit=\"\">Top Tools</param>" +
            "  <param name=\"Диэлектрическое покрытие\">нет</param>" +
            "  <param name=\"Длина\" unit=\"мм\">200</param>" +
            "  <param name=\"Рукоятки-чехлы\" unit=\"\">двухкомпонентные</param>" +
            "  <param name=\"Форма губок\" unit=\"\">удлиненная прямая</param>" +
            "  <param name=\"Функция &quot;антистатик&quot;\">нет</param>" +
            "</offer_params>\n"; // should be trimmed

        // act
        var ymlParams = InputToUniversalFormatMapper.parseYmlParams(xmlParams, null);

        // assert
        assertEquals(10, ymlParams.size());
    }

    @SneakyThrows
    @Test
    public void validXml_withNoMatchingElements_shouldYieldNoParams() {
        // arrange
        var xmlParams = "<root><shit bull=\"false\" dump=\"true\"/></root>";

        // act
        var ymlParams = InputToUniversalFormatMapper.parseYmlParams(xmlParams, null);

        // assert
        assertTrue(ymlParams.isEmpty());
    }

    @SneakyThrows
    @Test
    public void validXml_withSomeMatchingElements_shouldYieldParamsFromMatchingElements() {
        // arrange
        var xmlParams =
            "<root>" +
            "  <shit bull=\"false\" dump=\"true\"/>" +
            "  <param name=\"Length\" unit=\"mm\">200</param>" +
            "</root>";

        // act
        var ymlParams = InputToUniversalFormatMapper.parseYmlParams(xmlParams, null);

        // assert
        assertEquals(1, ymlParams.size());
        assertEquals("Length", ymlParams.get(0).getParamName());
    }

    @SneakyThrows
    @Test
    public void validXml_withNestedParamElements_shouldYieldParamsFromLeafElements() {
        // arrange
        var xmlParams =
            "<root>" +
            "  <param shouldBeIgnored=\"true\">" +
            "    <param name=\"Length\" unit=\"mm\">200</param>" +
            "  </param>" +
            "</root>";

        // act
        var ymlParams = InputToUniversalFormatMapper.parseYmlParams(xmlParams, null);

        // assert
        assertEquals(1, ymlParams.size());
        assertEquals("Length", ymlParams.get(0).getParamName());
    }

    @SneakyThrows
    @Test
    public void xmlParams_withoutNames_shouldBeIgnored() {
        // arrange
        var xmlParams =
            "<root>" +
            "  <param name=\"Length\" unit=\"mm\">200</param>" +
            "  <param unit=\"mm\">200</param>" +
            "</root>";

        // act
        var ymlParams = InputToUniversalFormatMapper.parseYmlParams(xmlParams, null);

        // assert
        assertEquals(1, ymlParams.size());
        assertEquals("Length", ymlParams.get(0).getParamName());
    }

    @SneakyThrows
    @Test
    public void xmlParams_withoutValues_shouldBeIgnored() {
        // arrange
        var xmlParams =
            "<root>" +
            "  <param name=\"Length\" unit=\"mm\">200</param>" +
            "  <param name=\"Width\" unit=\"mm\" />" +
            "</root>";

        // act
        var ymlParams = InputToUniversalFormatMapper.parseYmlParams(xmlParams, null);

        // assert
        assertEquals(1, ymlParams.size());
        assertEquals("Length", ymlParams.get(0).getParamName());
    }

    @SneakyThrows
    @Test
    public void xmlParams_withEmptyValues_shouldBeIgnored() {
        // arrange
        var xmlParams =
            "<root>" +
            "  <param name=\"Length\" unit=\"mm\">200</param>" +
            "  <param name=\"Width\" unit=\"mm\"></param>" +
            "</root>";

        // act
        var ymlParams = InputToUniversalFormatMapper.parseYmlParams(xmlParams, null);

        // assert
        assertEquals(1, ymlParams.size());
        assertEquals("Length", ymlParams.get(0).getParamName());
    }

    @SneakyThrows
    @Test
    public void xmlParams_withoutUnits_shouldBeAccepted() {
        // arrange
        var xmlParams =
            "<root>" +
            "  <param name=\"Length\">20 cm</param>" +
            "  <param name=\"Width\">10 cm</param>" +
            "</root>";

        // act
        var ymlParams = InputToUniversalFormatMapper.parseYmlParams(xmlParams, null);

        // assert
        assertEquals(2, ymlParams.size());
    }

    @SneakyThrows
    @Test(expected = SAXParseException.class)
    public void invalidXml_shouldYieldSaxParseException() {
        // arrange
        var jsonParams = "<Some shit>";

        // act
        InputToUniversalFormatMapper.parseYmlParams(jsonParams, null);

        // assert is in test attribute
    }

    @SneakyThrows
    @Test
    public void jsonParams_withArrayBrackets_shouldBeAccepted() {
        // arrange
        var jsonParams =
            "[ {\"name\":\"масса\",\"value\":\"0.42\",\"unit\":\"кг\"}," +
            "  {\"name\":\"высота\",\"value\":\"20\",\"unit\":\"см\"}," +
            "  {\"name\":\"ширина\",\"value\":\"10\",\"unit\":\"см\"}," +
            "  {\"name\":\"глубина\",\"value\":\"0.5\",\"unit\":\"см\"}  ]\n"; // should be trimmed

        // act
        var ymlParams = InputToUniversalFormatMapper.parseYmlParams(jsonParams, null);

        // assert
        assertEquals(4, ymlParams.size());
    }

    @SneakyThrows
    @Test
    public void jsonParams_withoutArrayBrackets_shouldBeAccepted() {
        // arrange
        var jsonParams =
            "{\"name\":\"масса\",\"value\":\"0.42\",\"unit\":\"кг\"}," +
            "{\"name\":\"высота\",\"value\":\"20\",\"unit\":\"см\"}," +
            "{\"name\":\"ширина\",\"value\":\"10\",\"unit\":\"см\"}," +
            "{\"name\":\"глубина\",\"value\":\"0.5\",\"unit\":\"см\"}";

        // act
        var ymlParams = InputToUniversalFormatMapper.parseYmlParams(jsonParams, null);

        // assert
        assertEquals(4, ymlParams.size());
    }

    @SneakyThrows
    @Test
    public void jsonParams_withoutNames_shouldBeIgnored() {
        // arrange
        var jsonParams =
            "{\"name\":\"Length\",\"value\":\"20\",\"unit\":\"cm\"}," +
            "{\"value\":\"10\",\"unit\":\"cm\"}";

        // act
        var ymlParams = InputToUniversalFormatMapper.parseYmlParams(jsonParams, null);

        // assert
        assertEquals(1, ymlParams.size());
        assertEquals("Length", ymlParams.get(0).getParamName());
    }

    @SneakyThrows
    @Test
    public void jsonParams_withoutValues_shouldBeIgnored() {
        // arrange
        var jsonParams =
            "{\"name\":\"Length\",\"value\":\"20\",\"unit\":\"cm\"}," +
            "{\"name\":\"Width\",\"unit\":\"cm\"}";

        // act
        var ymlParams = InputToUniversalFormatMapper.parseYmlParams(jsonParams, null);

        // assert
        assertEquals(1, ymlParams.size());
        assertEquals("Length", ymlParams.get(0).getParamName());
    }

    @SneakyThrows
    @Test
    public void jsonParams_withEmptyValues_shouldBeIgnored() {
        // arrange
        var jsonParams =
            "{\"name\":\"Length\",\"value\":\"20\",\"unit\":\"cm\"}," +
            "{\"name\":\"Width\",\"value\":\"\",\"unit\":\"cm\"}";

        // act
        var ymlParams = InputToUniversalFormatMapper.parseYmlParams(jsonParams, null);

        // assert
        assertEquals(1, ymlParams.size());
        assertEquals("Length", ymlParams.get(0).getParamName());
    }

    @SneakyThrows
    @Test
    public void jsonParams_withoutUnits_shouldBeAccepted() {
        // arrange
        var jsonParams =
            "{\"name\":\"Length\",\"value\":\"20\",\"unit\":\"cm\"}," +
            "{\"name\":\"Width\",\"value\":\"10 cm\"}";

        // act
        var ymlParams = InputToUniversalFormatMapper.parseYmlParams(jsonParams, null);

        // assert
        assertEquals(2, ymlParams.size());
    }

    @SneakyThrows
    @Test(expected = Exception.class)
    public void invalidJson_shouldYieldException() {
        // arrange
        var jsonParams = "{Some shit}";

        // act
        InputToUniversalFormatMapper.parseYmlParams(jsonParams, null);

        // assert is in test attribute
    }

    @SneakyThrows
    @Test
    public void unrecognizedFormat_shouldYieldEmptyParams() {
        // arrange
        var jsonParams = "Some shit {\"name\":\"масса\",\"value\":\"0.42\",\"unit\":\"кг\"}";

        // act
        var ymlParams = InputToUniversalFormatMapper.parseYmlParams(jsonParams, null);

        // assert
        assertTrue(ymlParams.isEmpty());
    }
}
