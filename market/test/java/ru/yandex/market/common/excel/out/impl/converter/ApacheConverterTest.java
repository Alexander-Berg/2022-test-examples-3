package ru.yandex.market.common.excel.out.impl.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.market.common.excel.MarketTemplate;
import ru.yandex.market.common.excel.out.impl.template.AbstractTemplateTest;

import static ru.yandex.market.common.excel.converter.Xls2CsvConverter.DELIMITER;

/**
 * Тесты на конвертацию excel-документов
 * {@link ru.yandex.market.common.excel.converter.impl.apache.ApacheConverter#convert(InputStream, Writer)
 * apache-конвертером}.
 *
 * @author ashevenkov
 */
@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
class ApacheConverterTest extends AbstractTemplateTest {

    @DisplayName("Обработка общего excel-документа (не маркетного шаблона).")
    @CsvSource({
            "xls",
            "xlsm",
    })
    @ParameterizedTest(name = "{0}")
    void convert_nonMarketTemplate_correct(String extension) {
        String convertedFile = getConvertedFileResourceAsString("xls/non_market_template." + extension);

        String[] delimitedLines = convertedFile
                .split("\n");
        Assertions.assertThat(delimitedLines)
                .hasSize(218);

        Stream.of(delimitedLines)
                .forEach(line -> Assertions.assertThat(StringUtils.countMatches(line, DELIMITER))
                        .isEqualTo(4));

        Assertions.assertThat(delimitedLines[1].split(DELIMITER))
                .containsExactlyInAnyOrder(
                        "Наименование (кликабельно)",
                        "\"Цена, руб.\"",
                        "Обычная цена",
                        "Комментарий",
                        "Столбец 5"
                );

        String[] lastLineValues = delimitedLines[217]
                .split(";");
        Assertions.assertThat(lastLineValues)
                .hasSize(5);
        Assertions.assertThat(lastLineValues[0])
                .isEqualTo("\"Вынос Mongoose 31,8\"");
        Assertions.assertThat(lastLineValues[1])
                .isEqualTo("300");
    }

    @DisplayName("Проверка контента при ковертации маркетного шаблона из excel в csv")
    @Test
    void convert_marketTemplate_correctValidationOfContent() {
        //проверяем, что в каждой строке правильное количеству значений
        validateContent("xls/common_template.xls");
    }

    @CsvSource({
            "kupivip_feed,.xlsm,6,KUPIVIP_ASSORTMENT,Обработка шаблона kupivip",
            "goods_feed,.xlsx,10,GOODS_ASSORTMENT,Обработка шаблона goods",
            "wlb_feed,.xlsx,4,WLB_ASSORTMENT,Обработка шаблона wildberries",
            "ozon_feed,.xlsx,9,OZON_ASSORTMENT,Обработка шаблона ozon"
    })
    @ParameterizedTest(name = "{4}")
    @DisplayName("Проверка конвертации excel файла в csv")
    void convert_externalTemplate_correctCsv(String fileName,
                                             String extension,
                                             int length,
                                             MarketTemplate marketTemplate,
                                             String testName) throws Exception {
        String convertedFile = getConvertedExternalTemplate("xls/" + fileName + extension, marketTemplate);
        assertResultCsv(fileName, length, convertedFile);
    }

    @CsvSource({
            "common_template,.xlsm,5,Обработка маркетного шаблона xlsm",
            "common_template,.xls,5,Обработка маркетного шаблона xls"
    })
    @ParameterizedTest(name = "{3}")
    @DisplayName("Проверка конвертации excel файла в csv")
    void convert_marketTemplate_correctCsv(String fileName,
                                           String extension,
                                           int length,
                                           String testName) throws Exception {
        String convertedFile = getConvertedFileResourceAsString("xls/" + fileName + extension);
        assertResultCsv(fileName, length, convertedFile);
    }

    @CsvSource({
            "price_template,.xlsm,Обработка шаблона с ценами",
            "stock_template,.xlsx,Обработка шаблона со стоками",
            "mbi_37804,.xlsm,Обработка шаблона с кредитами xlsm",
            "mbi_37804,.xls,Обработка шаблона с кредитами xls",
            "mbi_37804_alcohol,.xlsm,Обработка шаблона с кредитами alcohol xlsm",
            "mbi_38078,.xlsm,Обработка алкогольного шаблона с уценкой xlsm",
            "mbi_38078,.xls,Обработка алкогольного шаблона с уценкой xls",
            "mbi_36598,.xlsm,Обработка маркетного шаблона с уценкой xlsm",
            "mbi_36598,.xls,Обработка маркетного шаблона с уценкой xls",
            "mbi_31803,.xlsm,Обработка маркетного шаблона с уценкой xlsm",
            "mbi_31803,.xls,Обработка маркетного шаблона с СиС самовывоза xls",
            "alcohol_template,.xlsm,Обработка маркетного шаблона с типом alcohol xlsm",
            "white-with-type-and-age,.xlsx,Белый шаблон со спец. категориями",
            "mbi_70979,.xls,Обработка маркетного шаблона со специфичным маппингом *",
            "marketplace-auction-list,.xlsm,Обработка рекламного шаблона магазина со ставками"
    })
    @ParameterizedTest(name = "{2}")
    @DisplayName("Проверка конвертации excel файла в csv")
    void convert_marketTemplate_correctCsv(String fileName,
                                           String extension,
                                           String testName) throws Exception {
        String convertedFile = getConvertedFileResourceAsString("xls/" + fileName + extension);
        Assertions.assertThat(convertedFile)
                .isEqualTo(readFile("xls/" + fileName + ".converted"));
    }

    private void assertResultCsv(String fileName, int length, String convertedFile) throws IOException {
        String[] delimitedLines = convertedFile.split("\n");

        //Проверяем, что количество записей на выходе равно количеству строк в файле -1
        Assertions.assertThat(delimitedLines)
                .hasSize(length);
        //контрольным выстрелом проверяем содержимое файла целиком
        Assertions.assertThat(convertedFile)
                .isEqualTo(readFile("xls/" + fileName + ".converted"));
    }
}
