package ru.yandex.market.wms.api.controller;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.api.config.LostWriteOffAsyncConfig;
import ru.yandex.market.wms.api.model.dto.FixLostWriteOffDTO;
import ru.yandex.market.wms.api.service.async.LostWriteOffAsyncService;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig;
import ru.yandex.market.wms.common.spring.config.LostWriteOffServiceTestConfig;
import ru.yandex.market.wms.common.spring.domain.dto.OrderDTO;
import ru.yandex.market.wms.common.spring.service.CycleInventoryService;
import ru.yandex.market.wms.common.spring.service.LostWriteOffService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {IntegrationTestConfig.class, LostWriteOffAsyncConfig.class,
        LostWriteOffServiceTestConfig.class})
public class WriteOffOrderControllerTest extends IntegrationTest {

    @Autowired
    private LostWriteOffService lostWriteOffService;

    @Autowired
    @SpyBean
    private LostWriteOffAsyncService lostWriteOffAsyncService;

    @Autowired
    @SpyBean
    private CycleInventoryService cycleInventoryService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    @Override
    public void init() {
        super.init();
        lostWriteOffService.setWriteOffInNewTransaction(false);
        Mockito.reset(cycleInventoryService);
    }

    @Test
    public void putOutboundFixLostSuccess() throws Exception {
        String externOrderKey = "putoutbound-fixlost-1";
        OrderDTO orderDTO = OrderDTO.builder()
                .orderkey(externOrderKey)
                .externorderkey(externOrderKey)
                .type("24")
                .storerkey("")
                .orderdetails(Collections.emptyList())
                .build();
        mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/losts/writeoff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDTO))
                                .queryParam("storerType", "3P")
                                .queryParam("periodEnd", "2021-09-19 23:59:59")
                                .queryParam("dryRun", "false")
                        )
                .andExpect(status().isOk());

        LocalDateTime periodEnd = LocalDateTime.of(2021, 9, 19, 23, 59, 59);
        FixLostWriteOffDTO expected = new FixLostWriteOffDTO("3P", periodEnd, false, externOrderKey);
        verify(lostWriteOffAsyncService).writeOffFixLost(expected);
        verify(lostWriteOffAsyncService).onWriteOff(eq(expected), any());
    }

    @Test
    @DatabaseSetup("/cycle-inventory/before.xml")
    @DatabaseSetup("/cycle-inventory/performance-invent-cycle.xml")
    @ExpectedDatabase(value = "/cycle-inventory/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/cycle-inventory/performance-invent-cycle.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/cycle-inventory/losts-log-1p.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testDryRunTrue() throws Exception {
        String externOrderKey = "outbound-fixlost-12";
        OrderDTO orderDTO = OrderDTO.builder()
                .orderkey(externOrderKey)
                .externorderkey(externOrderKey)
                .type("24")
                .storerkey("")
                .orderdetails(Collections.emptyList())
                .build();
        mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/losts/writeoff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDTO))
                        .queryParam("storerType", "1P")
                        .queryParam("periodEnd", "2020-06-20 00:00:00")
                        .queryParam("dryRun", "true")
                )
                .andExpect(status().isOk());

        LocalDateTime periodEnd = LocalDateTime.of(2020, Month.JUNE, 20,
                0, 0, 0);
        verify(cycleInventoryService).writeOffFixLost("1P", periodEnd, true, null);
    }


    @Test
    @DatabaseSetup("/cycle-inventory/before.xml")
    @DatabaseSetup("/cycle-inventory/performance-invent-cycle.xml")
    @ExpectedDatabase(value = "/cycle-inventory/after-1P.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/cycle-inventory/performance-invent-cycle.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/cycle-inventory/losts-log-1p.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testDryRunFalse1P() throws Exception {
        String externOrderKey = "outbound-fixlost-12";
        OrderDTO orderDTO = OrderDTO.builder()
                .orderkey(externOrderKey)
                .externorderkey(externOrderKey)
                .type("24")
                .storerkey("")
                .orderdetails(Collections.emptyList())
                .build();
        mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/losts/writeoff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDTO))
                        .queryParam("storerType", "1P")
                        .queryParam("periodEnd", "2020-06-20 00:00:00")
                        .queryParam("dryRun", "false")
                )
                .andExpect(status().isOk());

        LocalDateTime periodEnd = LocalDateTime.of(2020, Month.JUNE, 20,
                0, 0, 0);
        verify(cycleInventoryService).writeOffFixLost("1P", periodEnd, false, "outbound-fixlost-12");
    }

}
