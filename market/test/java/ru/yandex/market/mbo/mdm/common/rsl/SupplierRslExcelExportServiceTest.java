package ru.yandex.market.mbo.mdm.common.rsl;


import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.excel.FullExcelFile;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.Rsl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.RslThreshold;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.SupplierRsl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl.SupplierRslRepository;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;

@SuppressWarnings("checkstyle:magicnumber")
public class SupplierRslExcelExportServiceTest extends MdmBaseDbTestClass {

    @Autowired
    private SupplierRslRepository supplierRslRepository;

    private SupplierRslExcelExportService supplierRslExcelExportService;

    @Before
    public void setup() {
        supplierRslExcelExportService = new SupplierRslExcelExportService(supplierRslRepository);
    }

    @Test
    public void testSimpleExport() {
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
        SupplierRsl rsl3 = new SupplierRsl()
            .setType(RslType.THIRD_PARTY)
            .setSupplierId(100500)
            .setRealId("пыбыщ")
            .setCategoryId(1333L)
            .setActivatedAt(Rsl.DEFAULT_START_DATE)
            .setCargoType750(false)
            .setRslThresholds(List.of(threshold1, threshold2));
        SupplierRsl rsl4 = new SupplierRsl()
            .setType(RslType.GLOBAL_FIRST_PARTY)
            .setSupplierId(11111)
            .setRealId("сысвсюжиж")
            .setCategoryId(2323L)
            .setActivatedAt(Rsl.DEFAULT_START_DATE)
            .setCargoType750(false)
            .setRslThresholds(List.of(threshold1, threshold2));
        supplierRslRepository.insert(rsl1);
        supplierRslRepository.insert(rsl2);
        supplierRslRepository.insert(rsl3);
        supplierRslRepository.insert(rsl4);

        FullExcelFile file = supplierRslExcelExportService.exportSupplierRslsOfNextType(
            Collections.singletonList(RslType.FIRST_PARTY));

        RslExcelTestBase.assertExcelFile(file.getAllSheets().get(RslExcelSheetConfig.NON_FOOD_PRODUCT_SHEET_NAME),
            List.of(
            List.of("142", "пущ", "12345", "01/01/2000", "0 дней", "30 дней", "10", "5", "40", "20", "", ""),
            List.of("142", "пущ", "12345", "01/01/2000", "1 месяц", "3 месяца", "15", "30", "25", "50", "", "")
        ));
        RslExcelTestBase.assertExcelFile(file.getAllSheets().get(RslExcelSheetConfig.FOOD_PRODUCT_SHEET_NAME), List.of(
            List.of("144", "пыщ", "12345", "01/01/2000", "0 дней", "30 дней", "10", "5", "40", "20", "", ""),
            List.of("144", "пыщ", "12345", "01/01/2000", "1 месяц", "3 месяца", "15", "30", "25", "50", "", "")
        ));

    }

}
