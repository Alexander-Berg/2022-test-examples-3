package ru.yandex.market.mbo.gwt.server.remote.upload;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Measure;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Unit;

import static org.junit.Assert.assertEquals;

/**
 * @author sergtru
 * @since 03.08.2017
 */
@SuppressWarnings({"checkstyle:magicNumber", "checkstyle:LineLength"})
public class ParametersExcelLoaderTest {
    private static final String[][] LINES = new String[][] {
        {"", "Параметр1", "Тип привода", "DriveType", "ENUM", "Модель", "TRUE", "TRUE", "", "", "", "",
                "Комментарий", "", "1", "FALSE", "TRUE", "TRUE", "FALSE", "", "123", "cluster-bone", "ременной", "TRUE"},
        {"", "", "Привод", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "ремешок", ""},
        {"", "", "Технология", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""},
        {"11", "Параметр2", "Масса тонарма", "TonearmMass", "NUMERIC", "Модель", "TRUE", "TRUE", "Длина", "сантиметр", "5", "30",
                "тестовый комментарий", "Описание", "0", "FALSE", "TRUE", "TRUE", "FALSE", "", "", "", "", ""},
        {"", "", "вес тонарма", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""},
        {"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""},
        {"", "Параметр3", "Автомат", "Automatic", "ENUM", "Модель", "TRUE", "TRUE", "", "", "", "",
                "", "", "1", "FALSE", "TRUE", "TRUE", "FALSE", "", "1", "value1", "полный", "TRUE"},
        {"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "2", "value2", "автомат", "FALSE"},
        {"22", "Название тонарма в комплекте", "", "TonearmName", "TEXT", "Модель", "TRUE", "FALSE", "", "", "", "",
                "", "", "0", "FALSE", "TRUE", "TRUE", "FALSE", "", "", "", "", ""},
        {"", "Картридж в комплекте", "картридж", "CartridgeIncluded", "BOOLEAN", "Модель", "TRUE", "TRUE", "", "", "", "",
                    "", "", "1", "FALSE", "TRUE", "TRUE", "FALSE", "", "5", "value1", "boolValue", ""}
    };

    private static final String[][] EMPTY_UNIT = new String[][] {
        {"Масса тонарма", "вес тонарма", "TonearmMass", "NUMERIC", "Модель", "TRUE", "TRUE", "Длина", "", "5", "30",
         "тестовый комментарий", "тестовое описание", "", "", "TRUE",  "", "1", "", "", "", "", "", ""}
    };

    private static final String[][] EMPTY_NAME_PARAM = new String[][] {
        {"30265112", "тест 1503", "", "test1503", "ENUM", "Оффер", "TRUE", "FALSE", "", "", "", "", "", "", "", "FALSE", "FALSE", "FALSE", "FALSE", "", "30265113", "тест1", "", ""},
        {"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "30265114", "тест2", "", ""},
        {"", "", "", "test150301", "ENUM", "Модель", "TRUE", "FALSE", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""}
    };

    private static final String[][] EMPTY_IS_OPTION_NAME = new String[][] {
            {"30265112", "тест 1503", "", "test1503", "ENUM", "Оффер", "TRUE", "FALSE", "", "", "", "", "", "", "", "FALSE", "FALSE", "FALSE", "FALSE", "", "30265113", "test", "грузовые2", "FALSE"},
            {"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "30265114", "легковые автомобили", "", ""},
            {"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "30265115", "", "", ""}
    };

    private static final long MEASURE_ID = 1;
    private static final long UNIT_ID = 100;

    @Test
    public void testImport() throws Exception {
        ParametersExcelLoader loader = initParametersExcelLoader();
        initWorkbook(loader, LINES);
        List<CategoryParam> params = loader.loadParameters();
        Assert.assertNotNull(params);
        List<String> expected = Arrays.asList("DriveType", "TonearmMass", "Automatic", "TonearmName",
                "CartridgeIncluded");
        List<String> actual = params.stream().map(CategoryParam::getXslName).collect(Collectors.toList());
        assertEquals(actual, expected);
        Map<String, CategoryParam> paramByXsl = new HashMap<>();
        for (CategoryParam param : params) {
            paramByXsl.put(param.getXslName(), param);
        }
        assertEquals(Param.Type.ENUM, paramByXsl.get("DriveType").getType());
        assertEquals(Param.Type.NUMERIC, paramByXsl.get("TonearmMass").getType());
        assertEquals(Param.Type.STRING, paramByXsl.get("TonearmName").getType());
        assertEquals(3, paramByXsl.get("DriveType").getDefaultAliases().size());
        assertEquals(1, paramByXsl.get("DriveType").getOptions().size());
    }

    @Test(expected = ParametersExcelLoader.ImportParametersException.class)
    public void testNumericWithOptions() throws Exception {
        ParametersExcelLoader loader = initParametersExcelLoader();
        Sheet sheet = initWorkbook(loader, LINES);
        sheet.getRow(1).createCell(4).setCellValue("NUMERIC");
        loader.loadParameters();
    }

    @Test(expected = ParametersExcelLoader.ImportParametersException.class)
    public void testUnknownType() throws Exception {
        ParametersExcelLoader loader = initParametersExcelLoader();
        Sheet sheet = initWorkbook(loader, LINES);
        sheet.getRow(1).createCell(4).setCellValue("WRONG_TYPE");
        loader.loadParameters();
    }

    @Test
    public void testMeasure() throws Exception {
        ParametersExcelLoader loader = initParametersExcelLoader();
        initWorkbook(loader, LINES);
        List<CategoryParam> params = loader.loadParameters();
        CategoryParam tonearmMass = params.stream()
                                          .filter(p -> p.getXslName().equals("TonearmMass"))
                                          .findFirst()
                                          .orElse(null);
        Assert.assertNotEquals(null, tonearmMass);
        assertEquals("тестовый комментарий", tonearmMass.getCommentForOperator());
        assertEquals("Описание", tonearmMass.getDescription());
        assertEquals(MEASURE_ID, tonearmMass.getMeasureId().longValue());
        assertEquals(UNIT_ID, tonearmMass.getUnitId().longValue());
    }

    @Test(expected = ParametersExcelLoader.ImportParametersException.class)
    public void testEmptyUnits() throws Exception {
        ParametersExcelLoader loader = initParametersExcelLoader();
        initWorkbook(loader, EMPTY_UNIT);
        loader.loadParameters();
    }

    @Test(expected = ParametersExcelLoader.ImportParametersException.class)
    public void testParamEmptyName() throws Exception {
        ParametersExcelLoader loader = initParametersExcelLoader();
        initWorkbook(loader, EMPTY_NAME_PARAM);
        loader.loadParameters();
    }

    @Test(expected = ParametersExcelLoader.ImportParametersException.class)
    public void testOptionEmptyName() throws Exception {
        ParametersExcelLoader loader = initParametersExcelLoader();
        initWorkbook(loader, EMPTY_IS_OPTION_NAME);
        loader.loadParameters();
    }

    private ParametersExcelLoader initParametersExcelLoader() {
        return new ParametersExcelLoader(Arrays.asList(createMeasure()));
    }

    private Measure createMeasure() {
        Measure measure = new Measure(MEASURE_ID, UNIT_ID);
        measure.addMeasureUnit(new Unit("сантиметр", "сантиметр",
                                        new BigDecimal(0), MEASURE_ID, UNIT_ID));
        measure.addName(Language.RUSSIAN.getId(), "Длина");
        return measure;
    }

    private Sheet initWorkbook(ParametersExcelLoader loader, String[][] lines) {
        Sheet sheet = loader.getWorkbook().getSheetAt(0);
        int rowIdx = 0;
        for (String[] line : lines) {
            Row row = sheet.createRow(++rowIdx);
            for (int col = 0; col < line.length; ++col) {
                row.createCell(col).setCellValue(line[col]);
            }
        }
        return sheet;
    }
}
