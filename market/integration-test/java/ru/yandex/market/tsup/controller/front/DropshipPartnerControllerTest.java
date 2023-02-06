package ru.yandex.market.tsup.controller.front;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.page.PageResult;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.legalInfo.LegalInfoResponse;
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentDto;
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentMetaInfoValueDto;
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentServiceDto;
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.service.data_provider.entity.dropship.dto.DimensionsClassDto;
import ru.yandex.market.tsup.service.data_provider.entity.dropship.dto.RoutingConfigDto;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DropshipPartnerControllerTest extends AbstractContextualTest {

    public static final long PARTNER_ID = 123L;
    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    public void setUp() {
        LogisticSegmentServiceDto logisticSegmentServiceDto = LogisticSegmentServiceDto.builder()
                .setCode(ServiceCodeName.TRANSPORT_MANAGER_MOVEMENT)
                .setMeta(List.of(
                        LogisticSegmentMetaInfoValueDto.builder()
                                .setKey("ROUTING_ENABLED")
                                .setValue("true")
                                .build()
                ))
                .build();

        Mockito.when(lmsClient.searchLogisticSegments(Mockito.any()))
                .thenReturn(List.of(
                        new LogisticSegmentDto()
                                .setServices(List.of(
                                        logisticSegmentServiceDto
                                ))
                                .setPreviousSegmentPartnerIds(List.of(PARTNER_ID))
                ));
        Mockito.when(lmsClient.searchLogisticSegments(Mockito.any(), Mockito.any()))
                .thenReturn(new PageResult<LogisticSegmentDto>().setData(List.of(
                        new LogisticSegmentDto()
                                .setServices(List.of(
                                        logisticSegmentServiceDto
                                ))
                                .setPreviousSegmentPartnerIds(List.of(PARTNER_ID))
                )).setPage(1).setSize(10).setTotalPages(1).setTotalElements(1));
        Mockito.when(lmsClient.getPartnerLegalInfo(PARTNER_ID))
                .thenReturn(Optional.of(new LegalInfoResponse(
                        1L,
                        PARTNER_ID,
                        "inc",
                        4567L,
                        "url",
                        "legalForm",
                        "legalInn",
                        "phone",
                        Address.newBuilder().build(),
                        Address.newBuilder().build(),
                        "email",
                        "kpp",
                        "bik",
                        "account"
                )));
    }

    @SneakyThrows
    @Test
    void shouldGetDropships() {
        mockMvc.perform(MockMvcRequestBuilders.get("/dropships"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").value(Matchers.hasSize(1)));
    }

    @SneakyThrows
    @Test
    void shouldGetDropship() {
        mockMvc.perform(MockMvcRequestBuilders.get("/dropships/{id}", 1))
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    void shouldUpdateDropshipRoutingConfig() {
        mockMvc.perform(MockMvcRequestBuilders.put("/dropships/{id}/routing-config", 1)
                .content(objectMapper.writeValueAsString(new RoutingConfigDto(
                        true,
                        DimensionsClassDto.REGULAR_CARGO,
                        new BigDecimal("1.2"),
                        false,
                        ""
                )))
                .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk());
    }
}
