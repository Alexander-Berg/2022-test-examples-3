package ru.yandex.market.ff.service;

import java.io.IOException;
import java.io.InputStream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.repository.ShopRequestRepository;
import ru.yandex.market.ff.service.implementation.SupplyItemsCisesReportGenerationService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Тесты на {@link SupplyItemsCisesReportGenerationService}.
 */
public class SupplyItemsCisesReportGenerationServiceTest extends IntegrationTest {

    @Autowired
    private ShopRequestRepository shopRequestRepository;

    @Autowired
    private SupplyItemsCisesReportGenerationService reportGenerationService;

    @Test
    @DatabaseSetup("classpath:service/xlsx-report/supply-items-cises-requests.xml")
    void testReportGeneration() throws IOException {
        ShopRequest request = shopRequestRepository.findById(6L);

        InputStream inputStream = reportGenerationService.generateReport(request);

        Workbook resultWorkbook = WorkbookFactory.create(inputStream);
        Sheet sheet = resultWorkbook.getSheetAt(0);

        Row headerRow = sheet.getRow(0);
        assertThat(headerRow.getCell(0).getStringCellValue(), is("Ваш SKU"));
        assertThat(headerRow.getCell(1).getStringCellValue(), is("Код идентификации"));

        Row row1 = sheet.getRow(1);
        assertThat(row1.getCell(0).getStringCellValue(), is("art4"));
        assertThat(row1.getCell(1).getStringCellValue(), is("CIS1"));

        Row row2 = sheet.getRow(2);
        assertThat(row2.getCell(0).getStringCellValue(), is("art4"));
        assertThat(row2.getCell(1).getStringCellValue(), is("CIS2"));

        Row row3 = sheet.getRow(3);
        assertThat(row3.getCell(0).getStringCellValue(), is("art7"));
        assertThat(row3.getCell(1).getStringCellValue(), is("CIS3"));
    }
}
