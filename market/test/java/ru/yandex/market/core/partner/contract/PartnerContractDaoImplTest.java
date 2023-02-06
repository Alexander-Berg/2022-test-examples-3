package ru.yandex.market.core.partner.contract;

import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.partner.model.PartnerContract;
import ru.yandex.market.core.supplier.PartnerContractType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PartnerContractDaoImpl}
 *
 * @author m-bazhenov
 */
@DbUnitDataBaseConfig(@DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, value = "true"))
@DbUnitDataSet(before = "SupplierContractDao.before.csv")
class PartnerContractDaoImplTest extends FunctionalTest {

    @Autowired
    private PartnerContractDao supplierContractDao;

    @Test
    void shouldGetSupplierContracts() {
        List<PartnerContract> contracts = supplierContractDao.getActiveContracts(2);
        assertThat(contracts)
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.getClientId()).isEqualTo(2L);
                    assertThat(e.getPersonId()).isEqualTo(2L);
                    assertThat(e.getContractId()).hasValue(2L);
                });
    }

    @Test
    void shouldReturnNoContractsForSupplierWithoutPrepayRequest() {
        List<PartnerContract> contracts = supplierContractDao.getActiveContracts(3);
        assertThat(contracts).isEmpty();
    }

    @Test
    void shouldReturnAllActiveContractsForSupplier() {
        Set<Long> supplierIds = Collections.singleton(4L);
        List<PartnerContract> contracts = supplierContractDao.getAllActiveContracts(supplierIds);
        assertThat(contracts.stream().flatMapToLong(x -> x.getContractId().stream()))
                .containsExactlyInAnyOrder(40000L, 40001L);
    }

    @Test
    void shouldReturnAllContractsForSupplier() {
        Set<Long> supplierIds = Collections.singleton(4L);
        List<PartnerContract> contracts = supplierContractDao.getAllContracts(supplierIds);
        assertThat(contracts.stream().flatMapToLong(x -> x.getContractId().stream()))
                .containsExactlyInAnyOrder(40000L, 40009L, 40001L);
    }

    @Test
    @DbUnitDataSet(after = "SupplierContractDaoSave.after.csv")
    void shouldCreateSupplierContracts() {
        PartnerContract contract = PartnerContract.builder()
                .setContractId(1L)
                .setPersonId(1L)
                .setClientId(1)
                .setRequestId(1)
                .setSupplierId(1)
                .setType(PartnerContractType.INCOME)
                .setCurrency(Currency.RUR)
                .setExternalId("income-1")
                .setActive(true)
                .setContractDate(LocalDate.of(2018, Month.JANUARY, 1))
                .build();

        supplierContractDao.saveContracts(Collections.singletonList(contract), false);
    }

    @Test
    @DbUnitDataSet(after = "SupplierContractDaoSaveOverride.after.csv")
    void shouldCreateSupplierContractsWithInactivation() {
        PartnerContract contractActive = PartnerContract.builder()
                .setContractId(1L)
                .setSupplierId(2)
                .setPersonId(1L)
                .setClientId(1)
                .setRequestId(2)
                .setType(PartnerContractType.INCOME)
                .setContractDate(LocalDate.of(2018, Month.JANUARY, 1))
                .setExternalId("income-1")
                .setActive(true)
                .setCurrency(Currency.RUR)
                .build();

        PartnerContract contractInactive = PartnerContract.builder()
                .setContractId(2L)
                .setSupplierId(2)
                .setPersonId(2L)
                .setClientId(2)
                .setRequestId(2)
                .setType(PartnerContractType.INCOME)
                .setContractDate(LocalDate.of(2016, Month.JANUARY, 1))
                .setExternalId("income-2")
                .setActive(false)
                .setCurrency(Currency.RUR)
                .build();

        supplierContractDao.saveContracts(List.of(contractActive, contractInactive), true);
    }
}
