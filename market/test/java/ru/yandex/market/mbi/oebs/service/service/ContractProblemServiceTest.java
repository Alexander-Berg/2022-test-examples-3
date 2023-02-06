package ru.yandex.market.mbi.oebs.service.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.mbi.oebs.service.FunctionalTest;
import ru.yandex.market.mbi.oebs.service.model.ContractProblemDTO;
import ru.yandex.market.mbi.oebs.service.yt.oebs.OebsContractRecord;
import ru.yandex.market.mbi.oebs.service.yt.oebs.OebsContractYtDao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;

public class ContractProblemServiceTest extends FunctionalTest {

    @MockBean
    OebsContractYtDao dao;

    ContractProblemService service;

    @BeforeEach
    void setUp() {
        Collection<OebsContractRecord> records = List.of(
                OebsContractRecord.builder()
                        .setBusinessId(850803L)
                        .setPartnerId(601017L)
                        .setProgram("DROPSHIP")
                        .setExternalId("ОФ-489135")
                        .setContractId(1259671L)
                        .setHoldBill(true)
                        .setDocOrigNotPresent(true)
                        .setBankAccNotValid(false)
                        .setUnpaidAmountCent(9397725L)
                        .setCurrency("RUR")
                        .build(),
                OebsContractRecord.builder()
                        .setBusinessId(850803L)
                        .setPartnerId(2383387L)
                        .setProgram("FULFILLMENT")
                        .setExternalId("ОФ-489135")
                        .setContractId(1259671L)
                        .setHoldBill(true)
                        .setDocOrigNotPresent(true)
                        .setBankAccNotValid(false)
                        .setUnpaidAmountCent(9397725L)
                        .setCurrency("RUR")
                        .build()
        );

        Mockito.when(dao.getByBusiness(anyLong())).thenReturn(records);
        Mockito.when(dao.getByPartners(anyCollection())).thenReturn(records);
        Mockito.when(dao.getByContracts(anyCollection())).thenReturn(records);

        service = new ContractProblemService(new OebsContractCache(dao));
    }

    @Test
    void fetchRecordsBusiness() {
        List<ContractProblemDTO> contractProblems = service.getContractProblems(850803L, null, null);

        assertEquals(contractProblems.size(), 2);
        assertEquals(contractProblems.get(0).getContractId(), "ОФ-489135");
        assertEquals(contractProblems.get(0).getUnpaidAmount(), new BigDecimal("93977.25"));

        ContractProblemDTO partner1 =
                contractProblems.stream().filter(c -> c.getPartnerId().equals(601017L)).findFirst().get();

        assertEquals(partner1.getModel(), "DROPSHIP");
    }

    @Test
    void fetchRecordsPartners() {
        List<ContractProblemDTO> contractProblems =
                service.getContractProblems(null, List.of(601017L, 2383387L), null);

        assertEquals(contractProblems.size(), 2);
        assertEquals(contractProblems.get(0).getContractId(), "ОФ-489135");
        assertEquals(contractProblems.get(0).getUnpaidAmount(), new BigDecimal("93977.25"));
    }

    @Test
    void fetchRecordsContracts() {
        List<ContractProblemDTO> contractProblems =
                service.getContractProblems(null, null, List.of("ОФ-489135"));

        assertEquals(contractProblems.size(), 1);
        assertEquals(contractProblems.get(0).getContractId(), "ОФ-489135");
        assertEquals(contractProblems.get(0).getUnpaidAmount(), new BigDecimal("93977.25"));
    }
}
