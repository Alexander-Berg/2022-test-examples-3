package ru.yandex.market.antifraud.orders.test;

import java.time.Instant;

import com.fasterxml.jackson.databind.node.BooleanNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.antifraud.orders.config.TvmIdentity;
import ru.yandex.market.antifraud.orders.service.ConfigurationService;
import ru.yandex.market.antifraud.orders.storage.entity.configuration.ConfigEnum;
import ru.yandex.market.antifraud.orders.storage.entity.configuration.ConfigurationEntity;
import ru.yandex.market.antifraud.orders.test.annotations.IntegrationTest;
import ru.yandex.market.antifraud.orders.test.providers.OrderRequestProvider;
import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyBuyerRestrictionsRequestDto;
import ru.yandex.passport.tvmauth.CheckedServiceTicket;
import ru.yandex.passport.tvmauth.TicketStatus;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author dzvyagin
 */
@IntegrationTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AuthTest {

    private static final int CHECKOUTER_TVM_ID = 24001;
    private static final int LOYALTY_TVM_ID = 24002;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TvmClient iTvmClient;

    @Autowired
    private ConfigurationService configurationService;

    @Before
    public void init() {
        configurationService.save(ConfigurationEntity.builder()
            .config(AntifraudJsonUtil.toJsonTree(new TvmIdentity[]{TvmIdentity.LOYALTY, TvmIdentity.LOYALTY_ADMIN}))
            .parameter(ConfigEnum.FORCE_TVM_FOR_CLIENTS)
            .updatedAt(Instant.now())
            .build());
        configurationService.save(ConfigurationEntity.builder()
            .config(BooleanNode.FALSE)
            .parameter(ConfigEnum.VOLVA_CHECK)
            .build());
    }

    @Test
    public void shouldRestrict() throws Exception {
        String json = AntifraudJsonUtil.OBJECT_MAPPER
            .writeValueAsString(new LoyaltyBuyerRestrictionsRequestDto(1L, null));
        mockMvc.perform(
                post("/antifraud/loyalty/restrictions")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void shouldPermitToken() throws Exception {
        String json = AntifraudJsonUtil.OBJECT_MAPPER
            .writeValueAsString(new LoyaltyBuyerRestrictionsRequestDto(1L, null));
        var st = new CheckedServiceTicket(TicketStatus.OK, "", LOYALTY_TVM_ID, 0);
        when(iTvmClient.checkServiceTicket(anyString())).thenReturn(st);
        mockMvc.perform(
                post("/antifraud/loyalty/restrictions")
                        .content(json)
                        .header("X-Ya-Service-Ticket", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldRestrictMissingToken() throws Exception {
        String json = AntifraudJsonUtil.OBJECT_MAPPER
            .writeValueAsString(new LoyaltyBuyerRestrictionsRequestDto(1L, null));
        when(iTvmClient.checkServiceTicket(anyString())).thenReturn(null);
        mockMvc.perform(
                post("/antifraud/loyalty/restrictions")
                    .content(json)
                    .header("X-Ya-Service-Ticket", "token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    public void shouldRestrictMalformedToken() throws Exception {
        String json = AntifraudJsonUtil.OBJECT_MAPPER
            .writeValueAsString(new LoyaltyBuyerRestrictionsRequestDto(1L, null));
        CheckedServiceTicket st = new CheckedServiceTicket(TicketStatus.MALFORMED, "", LOYALTY_TVM_ID, 0);
        when(iTvmClient.checkServiceTicket(anyString())).thenReturn(st);
        mockMvc.perform(
                post("/antifraud/loyalty/restrictions")
                    .content(json)
                    .header("X-Ya-Service-Ticket", "token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    public void shouldRestrictUnauthorizedToken() throws Exception {
        String json = AntifraudJsonUtil.OBJECT_MAPPER
            .writeValueAsString(new LoyaltyBuyerRestrictionsRequestDto(1L, null));
        CheckedServiceTicket st = new CheckedServiceTicket(TicketStatus.OK, "", CHECKOUTER_TVM_ID, 0);
        when(iTvmClient.checkServiceTicket(anyString())).thenReturn(st);
        mockMvc.perform(
                post("/antifraud/loyalty/restrictions")
                    .content(json)
                    .header("X-Ya-Service-Ticket", "token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    public void shouldPermitManualAccessPath() throws Exception {
        mockMvc.perform(
                get("/metadata/roles")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void shouldPermitNonForcedPath() throws Exception {
        String json = AntifraudJsonUtil.OBJECT_MAPPER
            .writeValueAsString(OrderRequestProvider.getOrderRequest());
        mockMvc.perform(
                post("/antifraud/detect/many")
                    .content(json)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
