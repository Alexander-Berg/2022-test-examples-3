package ru.yandex.market.logistics.cte.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.cte.client.dto.QualityGroupWithCategoryDTO;
import ru.yandex.market.logistics.cte.client.dto.QualityMatrixDTO;
import ru.yandex.market.logistics.cte.client.dto.ServiceCenterItemDocumentDTO;
import ru.yandex.market.logistics.cte.client.dto.ServiceCenterItemToSendFlatDTO;
import ru.yandex.market.logistics.cte.client.dto.SupplyItemFlatDTO;
import ru.yandex.market.logistics.cte.client.enums.MatrixType;
import ru.yandex.market.logistics.cte.client.enums.QualityAttributeType;
import ru.yandex.market.logistics.cte.client.enums.QualityAttributeValueType;
import ru.yandex.market.logistics.cte.client.enums.StockType;
import ru.yandex.market.logistics.cte.service.grid.GridGenerationCategoryTable;
import ru.yandex.market.logistics.cte.service.grid.GridGenerationItemsTable;
import ru.yandex.market.logistics.cte.service.grid.GridGenerationQualityMatrixTable;
import ru.yandex.market.logistics.cte.service.grid.GridGenerationServiceCenterItemsToSendService;

class ExcelGenerationOfCteAdminTablesServiceTest extends ExcelFilesComparingBaseTest {

    GridGenerationItemsTable excelGenerationItemsTable = new GridGenerationItemsTable();
    GridGenerationQualityMatrixTable excelGenerationQualityMatrixTable = new GridGenerationQualityMatrixTable();
    GridGenerationCategoryTable excelGenerationCategoryTable = new GridGenerationCategoryTable();
    GridGenerationServiceCenterItemsToSendService excelGenerationServiceCenterItemsToSendService =
            new GridGenerationServiceCenterItemsToSendService();

    @Test
    void testQualityMatrixTableGeneration() throws IOException {

        List<QualityMatrixDTO> qualityMatrixList = new ArrayList<>();
        QualityMatrixDTO qualityMatrixDTO = new
                QualityMatrixDTO(1, 4, "Коробки", 2,
                "Царапины", "Царапины",
                QualityAttributeType.ITEM, "Царапины",
                QualityAttributeValueType.UTIL,
                ZonedDateTime.of(2021, 12, 7, 16, 18,
                        0, 0, ZoneId.of("UTC")),
                MatrixType.RETURNS);

        qualityMatrixList.add(qualityMatrixDTO);

        XSSFWorkbook workbook =
                excelGenerationQualityMatrixTable.generateExcelTable(qualityMatrixList);

        assertXlsx(workbook, "quality_matrix_table_excel.xlsx");
    }

    @Test
    void testQualityCategoryTableGeneration() throws IOException {

        List<QualityGroupWithCategoryDTO> categoryTableList = new ArrayList<>();
        QualityGroupWithCategoryDTO categoryDTO = new
                QualityGroupWithCategoryDTO(0L, "Other", 91, "Плинтусы и пороги");

        categoryTableList.add(categoryDTO);

        XSSFWorkbook workbook =
                excelGenerationCategoryTable.generateExcelTable(categoryTableList);

        assertXlsx(workbook, "quality_category_table.xlsx");
    }

    @Test
    void testItemsTableExcelGeneration() throws IOException {

        List<SupplyItemFlatDTO> itemsTableList = new ArrayList<>();
        SupplyItemFlatDTO supplyItemFlatDTO = new
                SupplyItemFlatDTO(1L, "стул для купания Baby", "688C3FAE-C7EC",
                465852, "6182231s", "6182231s", 55,
                0L, "Other", StockType.DAMAGE, "00026.1001s",
                "ROV0000000000000000007", "12345uit", 123L, 12L,
                "Повреждена этикетка", "dsfsd", "Запах",
                160481, "31029s", "172s",
                ZonedDateTime.of(2021, 6, 8, 22, 51,
                        0, 0, ZoneId.of("UTC")), StockType.DAMAGE);

        itemsTableList.add(supplyItemFlatDTO);

        XSSFWorkbook workbook =
                excelGenerationItemsTable.generateExcelTable(itemsTableList);

        assertXlsx(workbook, "items_table.xlsx");
    }

    @Test
    void testServiceCenterItemsToSendExcelGeneration() throws IOException {

        ServiceCenterItemToSendFlatDTO item = new
                ServiceCenterItemToSendFlatDTO(
                1L,
                "123",
                "name",
                "brand",
                123L,
                "categoryName",
                "warrantyPeriod",
                222L,
                "111",
                "realSupplierName",
                "uit",
                "imei",
                "serialNumber",
                "attributes",
                "defectDescription",
                "clientName",
                "clientPhone",
                LocalDateTime.of(2022, 6, 1, 12, 0),
                LocalDateTime.of(2022, 6, 1, 11, 0),
                "productAppearance",
                "components",
                "serviceCenterName",
                555L,
                LocalDateTime.of(2022, 6, 1, 1, 0),
                LocalDateTime.of(2022, 6, 1, 2, 0),
                "PROCESSED",
                List.of(
                        new ServiceCenterItemDocumentDTO(1L,"CACHE_RECEIPT", "https://CACHE_RECEIPT", "cacheReceipt"),
                        new ServiceCenterItemDocumentDTO(2L, "REFUND_RECEIPT", "https://REFUND_RECEIPT", "refundReceipt"),
                        new ServiceCenterItemDocumentDTO(3L, "CLIENT_APPLICATION_FOR_RETURN", "https://APP_FOR_RETURN",
                                "cliApplication"),
                        new ServiceCenterItemDocumentDTO(4L, "NRP_PROTOCOL", "https://NRP_PROTOCOL", "nrp-protocol"),
                        new ServiceCenterItemDocumentDTO(5L, "OTHER", "https://OTHER1", "other"),
                        new ServiceCenterItemDocumentDTO(6L, "OTHER", "https://OTHER2", "other")
                ),
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.of(2022, 6, 1, 1, 0)
        );
        List<ServiceCenterItemToSendFlatDTO> itemsTableList = List.of(item);

        XSSFWorkbook workbook =
                excelGenerationServiceCenterItemsToSendService.generateExcelTable(itemsTableList);

        assertXlsx(workbook, "service_center_items.xlsx");
    }

}


