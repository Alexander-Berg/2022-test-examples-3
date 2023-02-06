package ru.yandex.market.mbo.mdm.common.rsl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.google.common.io.ByteStreams;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

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

public class SupplierRslExcelImportServiceTest extends MdmBaseDbTestClass {

    @Autowired
    private BeruId beruId;
    @Autowired
    private SupplierRslRepository supplierRslRepository;
    @Autowired
    private TransactionHelper transactionHelper;

    private SupplierRslExcelImportService supplierRslExcelImportService;

    @Before
    public void setup() {
        supplierRslExcelImportService = new SupplierRslExcelImportService(transactionHelper,
            supplierRslRepository, new RslValidatorService(List.of()), new TaskQueueRegistratorMock(), beruId);
    }

    @Test
    public void testSimpleImportFirstParty() throws IOException {
        String file = "rsl/supplier-rsl-import.xlsx";
        byte[] fileBytes = readResource(file);
        supplierRslExcelImportService.importExcel(file, fileBytes, Collections.singletonList(RslType.FIRST_PARTY));

        List<SupplierRsl> supplierRsls = supplierRslRepository.findAll();
        Assertions.assertThat(supplierRsls).containsExactlyInAnyOrderElementsOf(getFirstPartyRsls());
    }

    @Test
    public void testSimpleImportGlobal() throws IOException {
        String file = "rsl/supplier-rsl-global-import.xlsx";
        byte[] fileBytes = readResource(file);
        supplierRslExcelImportService.importExcel(file, fileBytes, List.of(RslType.GLOBAL_FIRST_PARTY,
            RslType.GLOBAL_THIRD_PARTY));

        List<SupplierRsl> supplierRsls = supplierRslRepository.findAll();
        Assertions.assertThat(supplierRsls).containsExactlyInAnyOrderElementsOf(getGlobalRsls());
    }

    @Test
    public void testDelete() throws IOException {
        String file = "rsl/supplier-rsl-import-delete.xlsx";
        byte[] fileBytes = readResource(file);
        supplierRslExcelImportService.importExcel(file, fileBytes, Collections.singletonList(RslType.FIRST_PARTY));

        List<SupplierRsl> supplierRsls = supplierRslRepository.findAll();
        Assertions.assertThat(supplierRsls).isEmpty();
    }

    @Test
    public void testUpdate() throws IOException {
        RslThreshold threshold1 = new RslThreshold()
            .setBeginningOfThreshold(new TimeInUnits(0, TimeInUnits.TimeUnit.DAY))
            .setEndOfThreshold(new TimeInUnits(30, TimeInUnits.TimeUnit.DAY))
            .setInRslDays(3)
            .setInRslPercents(43)
            .setOutRslDays(7)
            .setOutRslPercents(21);
        RslThreshold threshold2 = new RslThreshold()
            .setBeginningOfThreshold(new TimeInUnits(1, TimeInUnits.TimeUnit.MONTH))
            .setEndOfThreshold(new TimeInUnits(3, TimeInUnits.TimeUnit.MONTH))
            .setInRslDays(12)
            .setInRslPercents(23)
            .setOutRslDays(20)
            .setOutRslPercents(10);

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

        supplierRslRepository.insertBatch(List.of(rsl1,rsl2));

        String file = "rsl/supplier-rsl-import.xlsx";
        byte[] fileBytes = readResource(file);
        supplierRslExcelImportService.importExcel(file, fileBytes, Collections.singletonList(RslType.FIRST_PARTY));

        List<SupplierRsl> supplierRsls = supplierRslRepository.findAll();
        Assertions.assertThat(supplierRsls).containsExactlyInAnyOrderElementsOf(getFirstPartyRsls());
    }

    public List<SupplierRsl> getFirstPartyRsls() {
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

        return List.of(rsl1, rsl2);
    }

    public List<SupplierRsl> getGlobalRsls() {
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
            .setType(RslType.GLOBAL_FIRST_PARTY)
            .setActivatedAt(Rsl.DEFAULT_START_DATE)
            .setRealId("")
            .setCargoType750(true)
            .setRslThresholds(List.of(threshold1, threshold2));
        SupplierRsl rsl2 = new SupplierRsl()
            .setType(RslType.GLOBAL_THIRD_PARTY)
            .setActivatedAt(Rsl.DEFAULT_START_DATE.plusDays(100))
            .setRealId("")
            .setCargoType750(false)
            .setRslThresholds(List.of(threshold1, threshold2));
        SupplierRsl rsl3 = new SupplierRsl()
            .setType(RslType.GLOBAL_FIRST_PARTY)
            .setActivatedAt(Rsl.DEFAULT_START_DATE)
            .setRealId("")
            .setCargoType750(false)
            .setRslThresholds(List.of(threshold1, threshold2));
        return List.of(rsl1, rsl2, rsl3);
    }

    protected byte[] readResource(String fileName) throws IOException {
        return ByteStreams.toByteArray(getClass().getClassLoader().getResourceAsStream(fileName));
    }
}
