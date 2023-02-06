package ru.yandex.market.mbo.excel;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import com.google.common.collect.ImmutableMap;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ExcelFileConverterTest {
    private static final long SEED = "MBO-15362".hashCode() + 1;
    private static final int RETRY_COUNT = 100;

    private final ExcelFileRandomizer excelFileRandomizer = new ExcelFileRandomizer(SEED);
    ExcelIgnoresConfig ignoresConfig = new ExcelIgnoresConfigImpl(
            Collections.emptySet(), Collections.emptySet(), Collections.emptySet());

    @Test
    public void testDoubleConversion() {
        for (int i = 0; i < RETRY_COUNT; i++) {
            ExcelFile excelFile = excelFileRandomizer.getRandomValue();

            InputStream stream = ExcelFileConverter.convert(excelFile);
            ExcelFile converted = ExcelFileConverter.convert(stream, ignoresConfig);

            Assertions.assertThat(converted).isEqualTo(excelFile);
        }
    }

    @Test
    public void testEmptyLines() {
        ExcelFile file = parseExcel("roxy.xlsx");

        Assertions.assertThat(file.getLastLine()).isEqualTo(2);
        Assertions.assertThat(file.getValue(2, 6)).isEqualTo(
            "4627086622481");
    }

    @Test
    public void testParseOfFile1() {
        ExcelFile file = parseExcel("KravanOtborka2404.xlsx");

        Assertions.assertThat(file.getHeaders())
            .containsExactly("shop_category", "shop_ean", "shop_sku", "shop_title", "shop_url", "brand", "vat",
                "quantum", "min_delivery_pieces", "delivery_days", "tn_ved_code", "delivery_weekdays",
                "country_of_production", "transport_unit", "warranty_days", "period_of_validity_days",
                "service_life_days");
        Assertions.assertThat(file.getLastLine()).isEqualTo(839);
        Assertions.assertThat(file.getValuesList(1)).containsExactly("аксессуары для машинок", "4810344064219",
            "64219", "Набор дорожных знаков №3 (24 элементов) (в пакете)", "https://my-shop.ru/shop/toys/2887169.html",
            "Полесье", "18%", "1", "1", "7", "0000 00 000 0", "чт,пт,сб,вс", "", "22", "201", "202", "203");
        Assertions.assertThat(file.getValuesList(2)).containsExactly("аксессуары к настольным играм", "",
            "04PB016", "Мешочек \"Птица\" для кубиков, карт, мелочей - белый, 10,5 на 11,5см",
            "https://my-shop.ru/shop/toys/3106160.html",
            "Pandora's Box", "18%", "2", "2", "7", "0000000000", "пн,вт", "", "", "", "", "");
    }

    @Test
    public void testParseSingleSheetOnly() {
        // Как только нашли один лист, второй уже не парсим
        ExcelFile file = parseExcel("TwoSheets.xls");
        Assertions.assertThat(file.getHeaders()).hasSize(9);
    }

    @Test(expected = ExcelFileConverterException.class)
    public void testParseOfFileWithBrokenBytes() {
        parseExcel("excel/WorkbookWithBrokenBytes.xlsx");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConstraints() throws IOException, InvalidFormatException {
        InputStream file = ExcelFileConverter.convert(ExcelFile.Builder
            .withHeaders("test1", "tes2")
            .addLine("a", "b")
            .addTextConstraint(0, Arrays.asList("a", "aa", "aaa"))
            .addTextConstraint(1, Arrays.asList("b", "bb", "bbb", "bbbb"))
            .build());

        Workbook workbook = WorkbookFactory.create(file);
        Assertions.assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
        Assertions.assertThat(workbook.getSheet("Constraints").getLastRowNum()).isEqualTo(3);
        Assertions.assertThat(workbook.getSheetAt(0).getDataValidations())
            .extracting(v -> v.getValidationConstraint().getFormula1())
            .containsExactly("Constraints!A$1:A$3", "Constraints!B$1:B$4");
    }

    /**
     * Should correctly parse int/float numbers in all formats.
     * Example: 2    205600000000000000    1.234    2,3456    0
     */
    @Test
    public void testParseNumbers() {
        ExcelFile file = parseExcel("NumberParseTestData.xlsx");
        Assertions.assertThat(file.getLines()).hasSize(3);
        Assertions.assertThat(file.getLines().values()).containsOnly(
            ImmutableMap.of(0, "2", 1, "205600000000000000", 2, "1.234", 3, "2.3456", 4, "0"));
    }

    private ExcelFile parseExcel(String resourceName) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName);
        return ExcelFileConverter.convert(inputStream, ignoresConfig);
    }
}
