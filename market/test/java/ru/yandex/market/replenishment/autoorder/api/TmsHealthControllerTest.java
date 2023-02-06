package ru.yandex.market.replenishment.autoorder.api;

import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;
import ru.yandex.market.replenishment.autoorder.service.PdbReplenishmentService;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WithMockLogin
public class TmsHealthControllerTest extends ControllerTest {

    @Autowired
    PdbReplenishmentService pdbReplenishmentService;

    @Autowired
    TmsHealthController tmsHealthController;

    @Before
    public void setUp() {
        setTestTime(LocalDateTime.of(2021, 10, 14, 10, 10));
    }

    @Test
    @DbUnitDataSet(before = "TmsHealthControllerTest_testGetReplenishmentImportFails.before.csv")
    public void testGetReplenishmentImportFails() throws Exception {
        mockMvc.perform(get("/health/replenishment-import-fails")
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$")
                        .value("2;Jobs failed while importing 1p recommendations: " +
                                "demandsProcessorExecutor, recommendationCountryInfo1pExecutor"));
    }

    @Test
    @DbUnitDataSet(before = "TmsHealthControllerTest_testGetReplenishmentImportFailsOK.before.csv")
    public void testGetReplenishmentImportFailsOK() throws Exception {
        mockMvc.perform(get("/health/replenishment-import-fails")
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("0;OK"));
    }

    @Test
    @DbUnitDataSet(before = "TmsHealthControllerTest_testExportToAxFail.before.csv")
    public void testFailNotExportedToAx() throws Exception {
        mockMvc.perform(get("/health/ax-export-delay-status")
            .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("2;Found exported to AX demands before " +
                "2021-10-14T09:55. These oorders were not processed by AX."));
    }

    @Test
    @DbUnitDataSet(before = "TmsHealthControllerTest_testExportToAxOKProcessed.before.csv")
    public void testExportToAxOKProcessed() throws Exception {
        mockMvc.perform(get("/health/ax-export-delay-status")
            .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("0;OK"));
    }

    @Test
    @DbUnitDataSet(before = "TmsHealthControllerTest_testExportToAxOKHandledWithoutOrder.before.csv")
    public void testExportToAxOKHandledWithoutOrder() throws Exception {
        mockMvc.perform(get("/health/ax-export-delay-status")
            .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("0;OK"));
    }

    @Test
    @DbUnitDataSet(before = "TmsHealthControllerTest_testExportToAxOKSent.before.csv")
    public void testExportToAxOKSent() throws Exception {
        mockMvc.perform(get("/health/ax-export-delay-status")
            .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("0;OK"));
    }

}
