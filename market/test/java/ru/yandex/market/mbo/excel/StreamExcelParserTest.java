package ru.yandex.market.mbo.excel;


import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author yuramalinov
 * @created 16.05.18
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class StreamExcelParserTest {
    @Test
    public void testXlsx() {
        InputStream stream = readResource("briz.xlsx");
        List<StreamExcelParser.Sheet> sheets = StreamExcelParser.parse(stream);
        assertThat(sheets).hasSize(5);
        assertThat(sheets).extracting(StreamExcelParser.Sheet::getName)
            .containsExactly("Инструкция", "Ассортимент", "Скрытый", "Коммерческие данные", "Категории Маркета");
    }

    @Test
    public void testXlsxSkipHidden() {
        InputStream stream = readResource("briz.xlsx");
        ParseConfig parseConfig = new ParseConfig();
        parseConfig.setSkipHidden(true);
        List<StreamExcelParser.Sheet> sheets = StreamExcelParser.parse(stream, parseConfig);
        assertThat(sheets).hasSize(4);
        assertThat(sheets).extracting(StreamExcelParser.Sheet::getName)
            .containsExactly("Инструкция", "Ассортимент", "Коммерческие данные", "Категории Маркета");
    }

    @Test
    public void testXls() {
        InputStream stream = readResource("CorrectSample.xls");
        List<StreamExcelParser.Sheet> sheets = StreamExcelParser.parse(stream);
        assertThat(sheets).hasSize(3);
        assertThat(sheets.get(0).getRows().get(0).values())
            .containsExactly("Ваш SKU", "Название Товара", "Категория", "Артикул производителя", "Доступность",
                "Штрихкод",
                "Ваша цена", "НДС", "Квант поставки", "Дни поставок", "Срок поставки", "Минимальная партия поставки",
                "SKU на Яндексе", "Убрать из продажи", "Страна производства", "Код ТН ВЭД", "Гарантийный срок",
                "Срок годности", "Срок службы", "Комментарий к сроку годности", "Комментарий к сроку службы",
                "Комментарий к гарантийному сроку", "Кратность короба / Транспортная единица",
                "Тип документа соответствия качеству", "Номер документа соответствия качеству",
                "Дата начала действия", "Дата окончания действия", "Скан документа", "Страница товара на сайте",
                "Регистрационный номер сертифицирующей организации",
                "Габариты в сантиметрах с учетом упаковки", "Вес в килограммах с учетом упаковки",
                "Вес в килограммах без упаковки (нетто)", "Товар относится к продукции животного происхождения",
                "GUID в системе \"Меркурий\""
            );
        assertThat(sheets.get(1).getRows().get(0).values())
                .containsExactly("Скрытый лист");
        assertThat(sheets.get(2).getRows().get(0).values())
                .containsExactly("Лист2-кол1", "Лист2-кол2");
        assertThat(sheets.get(0).getRows())
            .hasSize(2);
        assertThat(sheets.get(1).getRows())
            .hasSize(1);
        assertThat(sheets.get(2).getRows())
            .hasSize(1);
    }

    @Test
    public void testXlsSkipHidden() {
        InputStream stream = readResource("CorrectSample.xls");

        ParseConfig parseConfig = new ParseConfig();
        parseConfig.setSkipHidden(true);

        List<StreamExcelParser.Sheet> sheets = StreamExcelParser.parse(stream, parseConfig);
        assertThat(sheets).hasSize(2);
        assertThat(sheets.get(0).getRows().get(0).values())
            .containsExactly("Ваш SKU", "Название Товара", "Категория", "Артикул производителя", "Доступность",
                "Штрихкод",
                "Ваша цена", "НДС", "Квант поставки", "Дни поставок", "Срок поставки", "Минимальная партия поставки",
                "SKU на Яндексе", "Убрать из продажи", "Страна производства", "Код ТН ВЭД", "Гарантийный срок",
                "Срок годности", "Срок службы", "Комментарий к сроку годности", "Комментарий к сроку службы",
                "Комментарий к гарантийному сроку", "Кратность короба / Транспортная единица",
                "Тип документа соответствия качеству", "Номер документа соответствия качеству",
                "Дата начала действия", "Дата окончания действия", "Скан документа", "Страница товара на сайте",
                "Регистрационный номер сертифицирующей организации",
                "Габариты в сантиметрах с учетом упаковки", "Вес в килограммах с учетом упаковки",
                "Вес в килограммах без упаковки (нетто)", "Товар относится к продукции животного происхождения",
                "GUID в системе \"Меркурий\""
            );
        assertThat(sheets.get(1).getRows().get(0).values())
                .containsExactly("Лист2-кол1", "Лист2-кол2");
        assertThat(sheets.get(0).getRows())
            .hasSize(2);
        assertThat(sheets.get(1).getRows())
            .hasSize(1);
    }

    @Test
    public void testParseEncryptedExcel() {
        // Main thing here is just to parse it somehow
        List<StreamExcelParser.Sheet> sheets = StreamExcelParser.parse(readResource("encrypted-xlsx.xlsm"));

        assertThat(sheets).hasSize(6);
        assertThat(sheets.get(2).getRows()).hasSize(838);
    }

    private InputStream readResource(String s) {
        return getClass().getClassLoader().getResourceAsStream(s);
    }
}
