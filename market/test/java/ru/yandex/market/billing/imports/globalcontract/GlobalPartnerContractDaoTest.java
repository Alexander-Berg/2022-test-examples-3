package ru.yandex.market.billing.imports.globalcontract;

import java.time.LocalDate;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.core.OperatingUnit;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.partner.model.PartnerContract;
import ru.yandex.market.core.partner.model.PartnerContractType;

@ParametersAreNonnullByDefault
class GlobalPartnerContractDaoTest extends FunctionalTest {

    @Autowired
    private GlobalPartnerContractDao globalPartnerContractDao;

    @DbUnitDataSet(before = "GlobalPartnerContractDao_saveContracts.before.csv",
            after = "GlobalPartnerContractDao_saveContracts.after.csv")
    @Test
    void saveContracts() {
        globalPartnerContractDao.saveContracts(List.of(
                partnerContract(1, 111, 1001L, PartnerContractType.INCOME)
                        .setPersonId(11L)
                        .setExternalId("1407001/21")
                        .setOperatingUnit(OperatingUnit.YANDEX_MARKET_ISRAEL)
                        .build(),
                partnerContract(1, 111, 1002L, PartnerContractType.OUTCOME)
                        .setPersonId(12L)
                        .setOperatingUnit(OperatingUnit.YANDEX_MARKET_ISRAEL)
                        .build(),
                // Два клиента в Балансе у одного магазина - нештатная, но технически возможная ситуация
                partnerContract(1, 112, 1003L, PartnerContractType.INCOME)
                        .setOperatingUnit(OperatingUnit.YANDEX_MARKET_ISRAEL)
                        .setPersonId(13L).build(),
                partnerContract(1, 112, 1004L, PartnerContractType.OUTCOME)
                        .setOperatingUnit(OperatingUnit.YANDEX_MARKET_ISRAEL)
                        .setPersonId(14L).build(),

                partnerContract(2, 222, 2001L, PartnerContractType.INCOME)
                        .setOperatingUnit(OperatingUnit.YANDEX_MARKET_ISRAEL)
                        .setPersonId(21L).build(),
                // даже если в БД у магазина уже есть контракты, мы добавим новые
                // главное - чтобы contract_id был уникальным
                partnerContract(3, 111, 4003L, PartnerContractType.INCOME)
                        .setOperatingUnit(OperatingUnit.YANDEX_MARKET_ISRAEL)
                        .setPersonId(11L).build(),
                partnerContract(3, 333, 4004L, PartnerContractType.INCOME)
                        .setOperatingUnit(OperatingUnit.YANDEX_MARKET_ISRAEL)
                        .setPersonId(12L).build()
        ));
    }

    @DbUnitDataSet(
            before = "GlobalPartnerContractDao_saveContracts.before.csv",
            after = "GlobalPartnerContractDao_saveContractsWithDuplicate.after.csv"
    )
    @Test
    void saveContracts_withDuplicateContractId() {
        globalPartnerContractDao.saveContracts(List.of(
                partnerContract(1, 111, 1001L, PartnerContractType.INCOME)
                        .setOperatingUnit(OperatingUnit.YANDEX_MARKET_ISRAEL)
                        .setPersonId(11L)
                        .build(),

                // контракт из предыдущей строки
                partnerContract(2, 222, 1001L, PartnerContractType.INCOME)
                        .setOperatingUnit(OperatingUnit.YANDEX_MARKET_ISRAEL)
                        .setPersonId(21L)
                        .build()
        ));
    }

    @DbUnitDataSet(
            before = "GlobalPartnerContractDao_saveContracts.before.csv",
            after = "GlobalPartnerContractDao_saveContractsWithReused.after.csv"
    )
    @Test
    void saveContracts_withReusedContractId() {
        globalPartnerContractDao.saveContracts(List.of(
                partnerContract(1, 111, 1001L, PartnerContractType.INCOME)
                        .setPersonId(11L)
                        .setOperatingUnit(OperatingUnit.YANDEX_MARKET_ISRAEL)
                        .build(),

                // контракт с таким id уже есть в таблице
                partnerContract(4, 444, 3001L, PartnerContractType.INCOME)
                        .setPersonId(42L)
                        .setOperatingUnit(OperatingUnit.YANDEX_MARKET_ISRAEL)
                        .build()
        ));
    }

    @DbUnitDataSet(before = "GlobalPartnerContractDao_deleteContracts.before.csv",
            after = "GlobalPartnerContractDao_deleteContracts.after.csv")
    @Test
    void deleteContracts() {
        globalPartnerContractDao.deleteShopContracts(List.of(1L, 2L));
    }

    @NotNull
    private PartnerContract.Builder partnerContract(
            long partnerId, long clientId, Long contractId, PartnerContractType contractType
    ) {
        return PartnerContract.builder()
                .setPartnerId(partnerId)
                .setClientId(clientId)
                .setType(contractType)
                .setContractId(contractId)
                .setContractDate(LocalDate.now())
                // глобальный чекаутер пока не присылает request_id, поэтому у нас всегда будет -1
                .setRequestId(-1)
                .setCurrency(Currency.ILS)
                .setOperatingUnit(OperatingUnit.YANDEX_MARKET_ISRAEL);
    }
}
