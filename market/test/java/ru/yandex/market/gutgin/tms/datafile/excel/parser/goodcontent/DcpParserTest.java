package ru.yandex.market.gutgin.tms.datafile.excel.parser.goodcontent;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.gutgin.tms.datafile.excel.parser.ExcelFileParser;
import ru.yandex.market.gutgin.tms.datafile.excel.parser.ParseResult;
import ru.yandex.market.gutgin.tms.datafile.excel.parser.dcp.DcpParserV1;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.helpers.BookCategoryHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.ir.excel.generator.param.MainParamCreator;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.partner.content.common.entity.goodcontent.RawSku;
import ru.yandex.market.partner.content.common.message.Messages;
import ru.yandex.market.partner.content.common.service.RawParamValueProcessor;
import ru.yandex.market.robot.db.ParameterValueComposer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DcpParserTest {
    private static final long HID = 91491;

    private ExcelFileParser fileParser;

    @Before
    public void init() {
        MboParameters.Category category = MboParameters.Category.newBuilder()
            .setHid(HID)
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(ParameterValueComposer.VENDOR_ID)
                .setXslName(CategoryData.VENDOR)
                .setValueType(MboParameters.ValueType.ENUM)
                )
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(ParameterValueComposer.BARCODE_ID)
                .setXslName(CategoryData.BAR_CODE)
                .setValueType(MboParameters.ValueType.STRING)
                .setMultivalue(true))
            .build();

        CategoryDataKnowledgeMock categoryDataKnowledgeMock = new CategoryDataKnowledgeMock();
        categoryDataKnowledgeMock.addCategoryData(HID, CategoryData.build(category));
        BookCategoryHelper bookCategoryHelper = mock(BookCategoryHelper.class);
        CategoryDataHelper categoryDataHelper = new CategoryDataHelper(categoryDataKnowledgeMock, bookCategoryHelper);
        RawParamValueProcessor rawParamValueProcessor = new RawParamValueProcessor(categoryDataHelper, false);

        fileParser = new ExcelFileParser();
        fileParser.addExcelParser(new DcpParserV1(rawParamValueProcessor));
    }

    @Test
    public void testGoodFile() {
        String path = "/dcp-valid-file-test.xlsx";
        ParseResult<RawSku> result = fileParser.parse(DcpParserTest.class.getResourceAsStream(path), path);
        assertOk(result, 7);
        assertPics(result.getValues().get(1));
    }

    @Test
    public void testGoodOldFileWORating() {
        String path = "/dcp-valid-file-test_old.xlsx";
        ParseResult<RawSku> result = fileParser.parse(DcpParserTest.class.getResourceAsStream(path), path);
        assertOk(result, 7);
        assertPics(result.getValues().get(1));
    }

    @Test
    public void testFileWithStrippedHeader() {
        String path = "/dcp-stripped-header-test.xlsx";
        ParseResult<RawSku> result = fileParser.parse(DcpParserTest.class.getResourceAsStream(path), path);
        assertOk(result, 6);
    }

    @Test
    public void testFileWithoutParamIdsRow() {
        String path = "/dcp-no-param-ids-test.xlsx";
        ParseResult<RawSku> result = fileParser.parse(DcpParserTest.class.getResourceAsStream(path), path);

        assertFailure(result, "Hidden row with parameter ids has not been found in file");
    }

    @Test
    public void testFileWithoutParamNamesRow() {
        String path = "/dcp-no-param-names-test.xlsx";
        ParseResult<RawSku> result = fileParser.parse(DcpParserTest.class.getResourceAsStream(path), path);

        assertFailure(result, "Row with parameter names has not been found in file");
    }

    @Test
    public void testFileWithoutMetaColumn() {
        String path = "/dcp-no-meta-column-test.xlsx";
        ParseResult<RawSku> result = fileParser.parse(DcpParserTest.class.getResourceAsStream(path), path);

        assertFailure(result, "No header rows found (probably the last hidden column was removed from file)");
    }

    @Test
    public void testFileWithParamNamesWithoutParamIds() {
        String path = "/dcp-no-param-ids-then-param-names-exists.xlsx";
        ParseResult<RawSku> result = fileParser.parse(DcpParserTest.class.getResourceAsStream(path), path);
        assertFailure(
            result,
            "Hidden row with parameter has missed parameter ids for parameter names in file"
        );
    }

    /**
     * в поле '5T' перетёрт id параметра, там должно быть значение 4925693L, а лежит "Да"
     */
    @Test
    public void fileWithCorruptedParamId() {
        String path = "/dcp-no-param-ids-in-single-column.xlsx";
        ParseResult<RawSku> result = fileParser.parse(DcpParserTest.class.getResourceAsStream(path), path);
        assertFailure(
            result,
            "Hidden row with parameter has missed parameter ids for parameter names in file"
        );
    }

    private void assertOk(ParseResult<RawSku> result, int rowIndex) {
        assertThat(result).isNotNull();
        assertThat(result.getMessages()).isEmpty();
        assertThat(result.getProblems()).isEmpty();

        List<RawSku> skuList = result.getValues();

        RawSku expectedSku1 = RawSku.newBuilder()
            .setCategoryId(91491L)
            .setRowIndex(rowIndex)
            .setShopSku("123456")
            .setName("3310")
            .setMainPictureUrl("https://avatars.mds.yandex.net/get-mpic/195452/img_id3033528711231967337/orig")
            .addRawParamValue(MainParamCreator.SHOP_SKU_PARAM_ID, "Ваш SKU", "123456")
            .addRawParamValue(7351771L, "Название товара", "3310")
            .addRawParamValue(7893318L, "Торговая марка", "Nokia")
            .addRawParamValue(15341921L, "Описание", "Самый крепкий телефон")
            .addRawParamValue(ParameterValueComposer.BARCODE_ID, "Баркод", "1234")
            .addRawParamValue(ParameterValueComposer.BARCODE_ID, "Баркод", "5678")
            .addRawParamValue(ParameterValueComposer.BARCODE_ID, "Баркод", "9101112")
            .addRawParamValue(13887626L, "Цвет товара для фильтра", "Красный")
            .addRawParamValue(14871214L, "Цвет товара для карточки", "Красный")
            .addRawParamValue(4940921L, "Тип (подробно)", "телефон для детей")
            .addRawParamValue(4925670L, "Операционная система", "Linux")
            .addRawParamValue(4925693L, "Основная камера", "Да")
            .addRawParamValue(16230465L, "Количество основных камер", "1")
            .addRawParamValue(4925734L, "Частота процессора, МГц", "100500")
            .addRawParamValue(7853111L, "Количество ядер процессора", "8")
            .addRawParamValue(4925735L, "Оперативная память (точно), МБ", "1")
            .addRawParamValue(16395709L, "Является шлюзом", "Нет")
            .addRawParamValue(16395390L, "Работает в системе \"умный дом\"", "Да")
            .addRawParamValue(16395671L, "Тип соединения устройств", "веревкой")
            .build();
        assertThat(skuList.get(0)).isEqualTo(expectedSku1);
    }

    private void assertPics(RawSku rawSku) {
        assertThat(rawSku.getMainPictureUrl()).isEqualTo("https://avatars.mds.yandex.net/get-marketpic/5484019/pic3f798830d89122be2eec2ba5abdb93ec/orig");
        assertThat(rawSku.getOtherPictureUrls()).isEqualTo("https://avatars.mds.yandex.net/get-marketpic/5475418/pic8f72626c627cacad99c85d2ea047563f/orig,https://avatars.mds.yandex.net/get-marketpic/6022418/picb659515ac6a1c1535bc1dacd95bea7e6/orig,https://avatars.mds.yandex.net/get-marketpic/1860871/pic09cc8aecc02081b1e8b576f2021e37e6/orig");
        assertThat(rawSku.getOtherPictureUrlList().get(0)).isEqualTo("https://avatars.mds.yandex.net/get-marketpic/5475418/pic8f72626c627cacad99c85d2ea047563f/orig");
        assertThat(rawSku.getOtherPictureUrlList().get(1)).isEqualTo("https://avatars.mds.yandex.net/get-marketpic/6022418/picb659515ac6a1c1535bc1dacd95bea7e6/orig");
        assertThat(rawSku.getOtherPictureUrlList().get(2)).isEqualTo("https://avatars.mds.yandex.net/get-marketpic/1860871/pic09cc8aecc02081b1e8b576f2021e37e6/orig");
    }

    private void assertFailure(ParseResult<RawSku> result, String message) {
        assertThat(result).isNotNull();
        assertThat(result.getValues()).isEmpty();
        assertThat(result.getProblems()).isEmpty();
        assertThat(result.getMessages())
            .hasSize(1)
            .element(0)
            .isEqualTo(Messages.get().excelWrongFileFormat(message));
    }
}
