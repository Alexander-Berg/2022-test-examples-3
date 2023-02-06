package ru.yandex.market.wms.timetracker.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.timetracker.config.TtsTestBase;
import ru.yandex.market.wms.timetracker.dao.clickhouse.ScanningOperationDao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class ScanningOperationControllerTest extends TtsTestBase {

    @Autowired
    @MockBean
    private ScanningOperationDao scanningOperationDao;

    @Test
    void newScanningOperation() throws Exception {
        Mockito.doNothing().when(scanningOperationDao).insert(Mockito.any());

        perform(
                "controller/scanning-operation-controller/1/request.json"
        );
    }

    private void perform(String requestFileName) throws Exception {
        mockMvc
                .perform(
                        post("/scanningOperation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(getFileContent(requestFileName))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
    }
}
