package ru.yandex.market.rg.asyncreport.warehouse.mapping;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.rg.asyncreport.ReportFunctionalTest;

/**
 * Тест для {@link CreateWarehouseMappingsGenerator}.
 */
@DbUnitDataSet(before = "CreateWarehouseMappingsGeneratorTest.before.csv")
class CreateWarehouseMappingsGeneratorTest extends ReportFunctionalTest {

    @Autowired
    CreateWarehouseMappingsGenerator createWarehouseMappingsGenerator;

    @Autowired
    MdsS3Client mdsS3Client;

    @BeforeEach
    void setUp() throws IOException {
        Mockito.doReturn(FileUtils.toFile(getClass().getResource("CreateWarehouseMappingTest.data.xlsx")))
                .when(mdsS3Client).download(Mockito.any(), Mockito.any());
        Mockito.doNothing().when(mdsS3Client).upload(Mockito.any(), Mockito.any());
        Mockito.doNothing().when(mdsS3Client).delete(Mockito.any());
        Mockito.doReturn(new URL("http://some.url")).when(mdsS3Client).getUrl(Mockito.any());
    }

    @Test
    void test() {
        final String reportId = "2354bcd0-e53f-4b0b-8537-7cf5f13fbb09";

        List<String> expected = List.of(
                "1;OK",
                "2;ERROR: Warehouse 145 already mapped to supplier 2",        // такой маппинг уже есть
                "3;ERROR: 3 is not a valid partner: it must be FBY supplier", // это SHOP
                "4;ERROR: 4 is not a valid partner: it must be FBY supplier", // такого партнера вообще нет
                "the_best_supplier_ever;ERROR: Cannot parse supplier id from value: the_best_supplier_ever" // не парсящийся идентификатор
        );
        checkReport(reportId, 145, createWarehouseMappingsGenerator, inputStream -> xlsCheck(expected, inputStream, ";"));
    }
}
