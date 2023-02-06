package ru.yandex.market.mbo.mdm.common.rsl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.excel.ExcelFileConverter;
import ru.yandex.market.mbo.excel.FullExcelFile;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.Rsl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.RslThreshold;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.SupplierRsl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl.SupplierRslRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mbo.mdm.common.rsl.validation.RslValidatorService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.utils.TaskQueueRegistratorMock;

@SuppressWarnings("checkstyle:magicnumber")
public class SupplierRslExportAndThenImportTest extends MdmBaseDbTestClass {

    @Autowired
    private BeruId beruId;
    @Autowired
    private SupplierRslRepository supplierRslRepository;
    @Autowired
    private TransactionHelper transactionHelper;

    private SupplierRslExcelImportService supplierRslExcelImportService;

    private SupplierRslExcelExportService supplierRslExcelExportService;

    @Before
    public void setup() {
        supplierRslExcelImportService = new SupplierRslExcelImportService(transactionHelper, supplierRslRepository,
            new RslValidatorService(List.of()), new TaskQueueRegistratorMock(),beruId);
        supplierRslExcelExportService = new SupplierRslExcelExportService(supplierRslRepository);
    }

    @Test
    public void testExportAndThenImport() throws IOException {
        RslThreshold threshold1 = new RslThreshold()
            .setBeginningOfThreshold(new TimeInUnits(0, TimeInUnits.TimeUnit.DAY))
            .setEndOfThreshold(new TimeInUnits(30, TimeInUnits.TimeUnit.DAY))
            .setInRslDays(10)
            .setInRslPercents(40)
            .setOutRslDays(5)
            .setOutRslPercents(20);
        RslThreshold threshold2 = new RslThreshold()
            .setBeginningOfThreshold(new TimeInUnits(1, TimeInUnits.TimeUnit.MONTH))
            .setEndOfThreshold(new TimeInUnits(3, TimeInUnits.TimeUnit.MONTH))
            .setInRslDays(15)
            .setInRslPercents(25)
            .setOutRslDays(30)
            .setOutRslPercents(50);

        SupplierRsl rsl1 = new SupplierRsl()
            .setType(RslType.FIRST_PARTY)
            .setSupplierId(144)
            .setRealId("пыщ")
            .setCategoryId(12345L)
            .setActivatedAt(Rsl.DEFAULT_START_DATE)
            .setCargoType750(true)
            .setRslThresholds(List.of(threshold1, threshold2));
        SupplierRsl rsl2 = new SupplierRsl()
            .setType(RslType.FIRST_PARTY)
            .setSupplierId(142)
            .setRealId("пущ")
            .setCategoryId(12345L)
            .setActivatedAt(Rsl.DEFAULT_START_DATE)
            .setCargoType750(false)
            .setRslThresholds(List.of(threshold1, threshold2));
        supplierRslRepository.insert(rsl1);
        supplierRslRepository.insert(rsl2);

        FullExcelFile file = supplierRslExcelExportService.exportSupplierRslsOfNextType(
            Collections.singletonList(RslType.FIRST_PARTY));

        supplierRslRepository.deleteAll();

        supplierRslExcelImportService.importExcel("kek", ExcelFileConverter.convert(file).readAllBytes(),
            Collections.singletonList(RslType.FIRST_PARTY));

        Assertions.assertThat(supplierRslRepository.findAll()).containsExactlyInAnyOrder(rsl1,rsl2);
    }


}
