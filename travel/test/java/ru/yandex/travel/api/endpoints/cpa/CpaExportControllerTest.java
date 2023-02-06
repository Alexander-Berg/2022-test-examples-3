package ru.yandex.travel.api.endpoints.cpa;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.travel.api.endpoints.cpa.req_rsp.CpaOrderSnapshotReqV2;
import ru.yandex.travel.api.endpoints.cpa.req_rsp.CpaOrderSnapshotRspV2;
import ru.yandex.travel.api.models.cpa.CpaOrderType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class CpaExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CpaExportImpl cpaExport;

    @Test
    public void testParametersParsing() throws Exception {

        when(cpaExport.getCpaOrderSnapshotsV2(any())).thenReturn(CompletableFuture.completedFuture(new CpaOrderSnapshotRspV2()));

        mockMvc.perform(get("/api/cpa_export/v2/get_cpa_order_snapshots?updated_at_from_utc=2020-03-20T00:10:00" +
            "&updated_at_to_utc=2020-03-21T00:30:31.303&order_type=TRAIN&limit=100"))
            .andExpect(status().isOk());

        ArgumentCaptor<CpaOrderSnapshotReqV2> argumentCaptor = ArgumentCaptor.forClass(CpaOrderSnapshotReqV2.class);
        verify(cpaExport).getCpaOrderSnapshotsV2(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue().getOrderType()).isEqualTo(CpaOrderType.TRAIN);
        assertThat(argumentCaptor.getValue().getUpdatedAtFromUtc()).isEqualTo(LocalDateTime.of(2020, 3, 20, 0, 10, 0));
        assertThat(argumentCaptor.getValue().getUpdatedAtToUtc())
            .isEqualTo(LocalDateTime.of(2020, 3, 21, 0, 30, 31, 303_000_000));
        assertThat(argumentCaptor.getValue().getLimit()).isEqualTo(100);
    }

}
