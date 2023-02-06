package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.excel.ExcelFileConverter;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

import static org.assertj.core.api.Assertions.assertThat;

public class ShopSkuKeyExcelServiceTest {
    private ShopSkuKeyExcelService excelService;

    @Before
    public void setup() {
        excelService = new ShopSkuKeyExcelService();
    }

    @Test
    public void parseWithShopSkuKeysSuccessfullyParsesFile() {
        ExcelFile.Builder inputExcel = new ExcelFile.Builder();
        inputExcel.addHeaders(List.of(MdmParamExcelAttributes.SUPPLIER_HEADER,
            MdmParamExcelAttributes.SHOP_SKU_HEADER));
        String supplierId = "123";
        String shopSku = "456";
        inputExcel.addLine(List.of(supplierId, shopSku));

        // Импортируем
        List<ShopSkuKey> result = new ArrayList<>();
        List<String> errors = excelService.parseWithShopSkuKeys(
            "filename", ExcelFileConverter.convertToBytes(inputExcel.build()), result
        ).getErrors();
        assertThat(errors).isEmpty();
        assertThat(result).containsExactlyInAnyOrderElementsOf(
            List.of(new ShopSkuKey(Integer.parseInt(supplierId), shopSku))
        );
    }

    @Test
    public void whenNoHeaderExistShouldReturnError() {
        ExcelFile.Builder inputExcel = new ExcelFile.Builder();
        String supplierId = "123";
        String shopSku = "456";
        inputExcel.addLine(List.of(supplierId, shopSku, 1));

        // Импортируем
        List<ShopSkuKey> result = new ArrayList<>();
        List<String> errors = excelService.parseWithShopSkuKeys(
            "filename", ExcelFileConverter.convertToBytes(inputExcel.build()), result
        ).getErrors();
        assertThat(errors).hasSize(1).contains("Не найдена колонка \"SupplierId\"");
    }

    @Test
    public void whenFirstHeaderIsIncorrectShouldReturnError() {
        ExcelFile.Builder inputExcel = new ExcelFile.Builder();
        inputExcel.addHeaders(List.of("some_header", MdmParamExcelAttributes.SHOP_SKU_HEADER));
        String supplierId = "123";
        String shopSku = "456";
        inputExcel.addLine(List.of(supplierId, shopSku));

        // Импортируем
        List<ShopSkuKey> result = new ArrayList<>();
        List<String> errors = excelService.parseWithShopSkuKeys(
            "filename", ExcelFileConverter.convertToBytes(inputExcel.build()), result
        ).getErrors();
        assertThat(errors).hasSize(1).contains("Не найдена колонка \"SupplierId\"");
    }

    @Test
    public void whenFirstColumnNotIntegerShouldReturnError() {
        ExcelFile.Builder inputExcel = new ExcelFile.Builder();
        inputExcel.addHeaders(List.of(MdmParamExcelAttributes.SUPPLIER_HEADER,
            MdmParamExcelAttributes.SHOP_SKU_HEADER));
        String supplierId = "stringVal"; //должно быть int
        String shopSku = "456";
        inputExcel.addLine(List.of(supplierId, shopSku));

        // Импортируем
        List<ShopSkuKey> result = new ArrayList<>();
        List<String> errors = excelService.parseWithShopSkuKeys(
            "filename", ExcelFileConverter.convertToBytes(inputExcel.build()), result
        ).getErrors();
        assertThat(errors).hasSize(1).contains("Некорректное значение SSKU: \"stringVal_456\"");
    }

    @Test
    public void whenShopSkuIsEmptyShouldReturnError() {
        ExcelFile.Builder inputExcel = new ExcelFile.Builder();
        inputExcel.addHeaders(List.of(MdmParamExcelAttributes.SUPPLIER_HEADER,
            MdmParamExcelAttributes.SHOP_SKU_HEADER));
        String supplierId = "123";
        String shopSku = ""; //не должно быть пустым или null
        inputExcel.addLine(List.of(supplierId, shopSku));

        // Импортируем
        List<ShopSkuKey> result = new ArrayList<>();
        List<String> errors = excelService.parseWithShopSkuKeys(
            "filename", ExcelFileConverter.convertToBytes(inputExcel.build()), result
        ).getErrors();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("Некорректное значение SSKU: \"123_null\"");
    }

    @Test
    public void whenSupplierIdIsEmptyShouldReturnError() {
        ExcelFile.Builder inputExcel = new ExcelFile.Builder();
        inputExcel.addHeaders(List.of(MdmParamExcelAttributes.SUPPLIER_HEADER,
            MdmParamExcelAttributes.SHOP_SKU_HEADER));
        String supplierId1 = "1", supplierId2 = ""; //не должно быть пустым
        String shopSku1 = "2", shopSku2 = "4";
        inputExcel.addLine(List.of(supplierId1, shopSku1));
        inputExcel.addLine(List.of(supplierId2, shopSku2));

        // Импортируем
        List<ShopSkuKey> result = new ArrayList<>();
        List<String> errors = excelService.parseWithShopSkuKeys(
            "filename", ExcelFileConverter.convertToBytes(inputExcel.build()), result
        ).getErrors();
        assertThat(errors).hasSize(1).contains("Некорректное значение SSKU: \"null_4\"");
    }

    @Test
    public void whenEmptyFileShouldReturnError() {
        ExcelFile.Builder emptyExcel = new ExcelFile.Builder();

        // Импортируем
        List<ShopSkuKey> result = new ArrayList<>();
        List<String> errors = excelService.parseWithShopSkuKeys(
            "filename", ExcelFileConverter.convertToBytes(emptyExcel.build()), result
        ).getErrors();
        assertThat(errors).hasSize(1).contains("Не найдена колонка \"SupplierId\"");
    }

    @Test
    public void generateEmptyExcelWithShopSkuHeaderShouldGenerateTemplateFile() {
        ExcelFile template = excelService.generateEmptyExcelWithShopSkuHeader();

        assertThat(template.getHeaders()).containsExactly(MdmParamExcelAttributes.SUPPLIER_HEADER,
            MdmParamExcelAttributes.SHOP_SKU_HEADER);
    }
}
