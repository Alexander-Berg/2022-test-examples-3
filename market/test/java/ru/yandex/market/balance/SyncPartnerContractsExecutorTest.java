package ru.yandex.market.balance;

import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientContractInfo;
import ru.yandex.market.core.partner.contract.PartnerContractService;
import ru.yandex.market.common.balance.xmlrpc.model.ClientContractsStructure;
import ru.yandex.market.common.balance.xmlrpc.model.ContractType;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.shop.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class SyncPartnerContractsExecutorTest extends FunctionalTest {

    @Autowired
    private PartnerContractService supplierContractService;

    @Autowired
    @Qualifier("patientBalanceService")
    private BalanceService balanceService;

    @Autowired
    private EnvironmentService environmentService;

    private SyncPartnerContractsExecutor syncPartnerContractsExecutor;

    @BeforeEach
    void setUp() {
        syncPartnerContractsExecutor =
                new SyncPartnerContractsExecutor(supplierContractService, environmentService);
    }

    @Test
    @DbUnitDataSet(
            before = "SyncPartnerContractsExecutorTest.before.csv",
            after = "SyncPartnerContractsExecutorTest.after.csv")
    void testImportWithoutDuplicates() {
        mockBalance(getContracts());
        syncPartnerContractsExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "SyncPartnerContractsExecutorTest.before.csv",
            after = "SyncPartnerContractsExecutorTest.after.csv")
    void testImportWithIncomeDuplicates() {
        var contracts = new HashMap<>(getContracts());
        contracts.put(Pair.of(70000L, ContractType.GENERAL), List.of(
                buildContract(700, "7/1", LocalDate.of(2020, Month.AUGUST, 3), true, 70),
                buildContract(702, "7/2", LocalDate.of(2020, Month.AUGUST, 4), true, 71)
        ));
        mockBalance(contracts);

        assertThrows(RuntimeException.class, () -> syncPartnerContractsExecutor.doJob(null));
    }

    @Test
    @DbUnitDataSet(
            before = "SyncPartnerContractsExecutorTest.before.csv",
            after = "SyncPartnerContractsExecutorTest.after.csv")
    void testImportWithOutcomeDuplicates() {
        var contracts = new HashMap<>(getContracts());
        contracts.put(Pair.of(70000L, ContractType.SPENDABLE), List.of(
                buildContract(711, "OF7", LocalDate.of(2020, Month.AUGUST, 3), true, 71),
                buildContract(712, "OF72", LocalDate.of(2020, Month.AUGUST, 4), true, 72)
        ));
        mockBalance(contracts);

        assertThrows(RuntimeException.class, () -> syncPartnerContractsExecutor.doJob(null));
    }


    @Test
    @DbUnitDataSet(
            before = "SyncPartnerContractsExecutorTest.before.csv",
            after = "SyncPartnerContractsExecutorTest.after.csv")
    void testImportForceInactivate() {
        environmentService.setValue(SyncPartnerContractsExecutor.SYNC_CONTACTS_FORCE_INACTIVE_CONTRACTS_IDS, "403,401");

        var contracts = new HashMap<>(getContracts());
        contracts.put(Pair.of(40000L, ContractType.GENERAL), List.of(
                buildContract(400, "4/1", LocalDate.of(2019, Month.SEPTEMBER, 1), true, 40),
                buildContract(403, "4/1", LocalDate.of(2017, Month.SEPTEMBER, 1), true, 43))
        );
        contracts.put(Pair.of(40000L, ContractType.SPENDABLE), List.of(
                buildContract(401, "OF4", LocalDate.of(2019, Month.SEPTEMBER, 1), true, 49),
                buildContract(402, "OF4", LocalDate.of(2021, Month.MARCH, 1), true, 41))
        );
        mockBalance(contracts);

        syncPartnerContractsExecutor.doJob(null);
    }

    Map<Pair<Long, ContractType>, List<ClientContractInfo>> getContracts() {
        return Map.ofEntries(
                Map.entry(Pair.of(10000L, ContractType.GENERAL), List.of(
                        buildContract(100, "1/1", LocalDate.of(2020, Month.AUGUST, 3), true, 10))
                ),
                Map.entry(Pair.of(10000L, ContractType.SPENDABLE), List.of(
                        buildContract(101, "OF1", LocalDate.of(2020, Month.AUGUST, 3), true, 11))
                ),
                Map.entry(Pair.of(20000L, ContractType.GENERAL), List.of(
                        buildContract(200, "2/1", LocalDate.of(2018, Month.SEPTEMBER, 3), false, 20))
                ),
                Map.entry(Pair.of(20000L, ContractType.SPENDABLE), List.of(
                        buildContract(201, "OF2", LocalDate.of(2018, Month.SEPTEMBER, 3), false, 21))
                ),
                Map.entry(Pair.of(30000L, ContractType.GENERAL), Collections.emptyList()),
                Map.entry(Pair.of(30000L, ContractType.SPENDABLE), Collections.emptyList()),
                Map.entry(Pair.of(40000L, ContractType.GENERAL), List.of(
                        buildContract(400, "4/1", LocalDate.of(2019, Month.SEPTEMBER, 1), true, 40),
                        buildContract(403, "4/1", LocalDate.of(2017, Month.SEPTEMBER, 1), false, 43))
                ),
                Map.entry(Pair.of(40000L, ContractType.SPENDABLE), List.of(
                        buildContract(401, "OF4", LocalDate.of(2019, Month.SEPTEMBER, 1), false, 49),
                        buildContract(402, "OF4", LocalDate.of(2021, Month.MARCH, 1), true, 41))),
                Map.entry(Pair.of(50000L, ContractType.GENERAL), List.of(
                        buildContract(500, "5/1", LocalDate.of(2020, Month.JANUARY, 1), true, 50))
                ),
                Map.entry(Pair.of(50000L, ContractType.SPENDABLE), Collections.emptyList()),
                Map.entry(Pair.of(70000L, ContractType.GENERAL), List.of(
                        buildContract(700, "7/1", LocalDate.of(2020, Month.AUGUST, 3), true, 70))
                ),
                Map.entry(Pair.of(70000L, ContractType.SPENDABLE), List.of(
                        buildContract(701, "OF7", LocalDate.of(2020, Month.AUGUST, 3), true, 71))
                ),
                Map.entry(Pair.of(80000L, ContractType.GENERAL), List.of(
                        buildContract(800, "8/1", LocalDate.of(2018, Month.AUGUST, 3), false, 80),
                        buildContract(799, "8/1", LocalDate.of(2016, Month.AUGUST, 3), false, 80),
                        buildContract(802, "8/1", LocalDate.of(2020, Month.AUGUST, 3), true, 80))
                ),
                Map.entry(Pair.of(80000L, ContractType.SPENDABLE), List.of(
                        buildContract(801, "OF8", LocalDate.of(2020, Month.AUGUST, 3), true, 81))
                )
        );
    }


    private void mockBalance(Map<Pair<Long, ContractType>, List<ClientContractInfo>> contracts) {
        contracts.forEach((idWithType, list) ->
                when(balanceService.getClientContracts(eq(idWithType.getFirst()), eq(idWithType.getSecond())))
                        .thenReturn(list)
        );
    }

    private ClientContractInfo buildContract(int id, String externalId, LocalDate contractDate, boolean isActive,
                                             int personId) {
        ClientContractsStructure contract = new ClientContractsStructure();
        contract.setId(id);
        contract.setExternalId(externalId);
        contract.setIsActive(BooleanUtils.toInteger(isActive));
        contract.setCurrency("RUR");
        contract.setDt(DateUtil.asDate(contractDate));
        contract.setPersonId(personId);
        return ClientContractInfo.fromBalanceStructure(contract);
    }
}
