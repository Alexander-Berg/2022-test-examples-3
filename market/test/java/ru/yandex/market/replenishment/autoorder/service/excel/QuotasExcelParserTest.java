package ru.yandex.market.replenishment.autoorder.service.excel;

import java.io.InputStream;
import java.util.List;

import lombok.SneakyThrows;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.market.replenishment.autoorder.dto.QuotaDto;
import ru.yandex.market.replenishment.autoorder.exception.UserWarningException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class QuotasExcelParserTest {

    private static final String EXPECTED_QUANTITY_VALIDATION_MESSAGE = "Не заполнена квота " +
        "(dep - Остальное, date - 2022-07-02, warehouse - Томилино)";

    private static final String EXPECTED_DEPARTMENT_VALIDATION_MESSAGE = "Не заполнен департамент " +
        "(date - 2022-06-27, quantity - 1200, warehouse - Софьино)";

    private static final String EXPECTED_WAREHOUSE_VALIDATION_MESSAGE = "Не заполнено имя склада " +
        "(dep - DIY, date - 2022-06-27, quantity - 1200)";

    @Test
    public void parseQuotasTest() {
        MultipartFile multipartFile = readFile("quotasFile.xlsx");
        QuotasExcelParser quotasExcelParser = new QuotasExcelParser();
        List<QuotaDto> quotaDtoList = quotasExcelParser.parseQuotas(multipartFile);
        Assert.notNull(quotaDtoList);
        Assert.isTrue(quotaDtoList.size() == 30);
    }

    @Test
    public void parseQuotasQuantityValidationTest() {
        MultipartFile multipartFile = readFile("TestValidationQuantityExcel.xlsx");
        QuotasExcelParser quotasExcelParser = new QuotasExcelParser();
        List<QuotaDto> quotaDtoList = quotasExcelParser.parseQuotas(multipartFile);
        Assert.notNull(quotaDtoList);
        Assert.isTrue(quotaDtoList.size() == 29);
    }

    @Test
    public void parseQuotasDateValidationTest() {
        MultipartFile multipartFile = readFile("TestValidationDateExcel.xlsx");
        QuotasExcelParser quotasExcelParser = new QuotasExcelParser();

        Exception actual = assertThrows(UserWarningException.class,
            () -> quotasExcelParser.parseQuotas(multipartFile));

        assertEquals("Не заполнена дата", actual.getMessage());
    }

    @Test
    public void parseQuotasDepartmentValidationTest() {
        MultipartFile multipartFile = readFile("TestValidationDepartmentExcel.xlsx");
        QuotasExcelParser quotasExcelParser = new QuotasExcelParser();

        Exception actual = assertThrows(UserWarningException.class,
            () -> quotasExcelParser.parseQuotas(multipartFile));

        assertEquals(EXPECTED_DEPARTMENT_VALIDATION_MESSAGE, actual.getMessage());
    }

    @Test
    public void parseQuotasWarehouseValidationTest() {
        MultipartFile multipartFile = readFile("TestValidationWarehouseExcel.xlsx");
        QuotasExcelParser quotasExcelParser = new QuotasExcelParser();

        Exception actual = assertThrows(UserWarningException.class,
            () -> quotasExcelParser.parseQuotas(multipartFile));

        assertEquals(EXPECTED_WAREHOUSE_VALIDATION_MESSAGE, actual.getMessage());
    }

    @SneakyThrows
    private MultipartFile readFile(String fileName) {
        InputStream inputStream = this.getClass().getResourceAsStream(fileName);
        return new MockMultipartFile("quotasFile.xlsx", inputStream);
    }

}
