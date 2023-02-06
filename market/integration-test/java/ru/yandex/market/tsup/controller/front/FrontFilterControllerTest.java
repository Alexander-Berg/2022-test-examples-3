package ru.yandex.market.tsup.controller.front;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerTransportDto;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.util.UserDtoUtils;
import ru.yandex.mj.generated.client.carrier.api.UserApiClient;
import ru.yandex.mj.generated.client.carrier.model.PageOfUserDto;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.management.entity.type.PartnerType.FULFILLMENT;

/**
 * Тесты для {@link FrontFilterController}
 */
public class FrontFilterControllerTest extends AbstractContextualTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private UserApiClient userApiClient;

    @Test
    void suggestRoutePoint() throws Exception {
        setUpGetLogisticPoints();
        mockMvc.perform(get("/filter/suggest")
                .param("entityType", "LOGISTIC_POINT")
                .param("substring", "ерв")
                .header("action", "get")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(IntegrationTestUtils.jsonContent("fixture/trip/route_points_response.json"));
    }

    @Test
    void suggestRoutePointSorting() throws Exception {
        setUpGetLogisticPoints();
        mockMvc.perform(get("/filter/suggest")
                .param("entityType", "LOGISTIC_POINT")
                .param("substring", "т")
                .header("action", "get")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(IntegrationTestUtils.jsonContent("fixture/trip/route_points_multi_response.json"));
    }

    @Test
    void routePointCreateNew() throws Exception {
        setUpGetLogisticPoints();
        mockMvc.perform(get("/filter/suggest")
                .param("entityType", "LOGISTIC_POINT")
                .param("substring", "ерв")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }

    @Test
    void suggestPartner() throws Exception {
        Mockito.when(lmsClient.searchPartners(any()))
            .thenReturn(List.of(
                partnerResponse(1L, "Софьино", "SOFINO", FULFILLMENT)
            ));
        Mockito.when(lmsClient.getLogisticsPoints(any())).thenReturn(
            List.of(LogisticsPointResponse.newBuilder()
                .partnerId(1L)
                .id(101L)
                .name("LOG_POINT_SOFINO")
                .build())
        );
        mockMvc.perform(get("/filter/suggest")
                .param("entityType", "PARTNER")
                .param("substring", "Соф")
                .header("action", "get")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(IntegrationTestUtils.jsonContent("fixture/partner/partner_response.json"));
    }

    @Test
    void suggestPartnerNoVal() throws Exception {
        Mockito.when(lmsClient.searchPartners(any()))
            .thenReturn(List.of(
                partnerResponse(1L, "Софьино", "SOFINO", FULFILLMENT)
            ));
        Mockito.when(lmsClient.getLogisticsPoints(any())).thenReturn(
            List.of()
        );
        mockMvc.perform(get("/filter/suggest")
                .param("entityType", "PARTNER")
                .param("substring", "Соф")
                .header("action", "get")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(IntegrationTestUtils.jsonContent("fixture/partner/partner_response_empty.json"));
    }

    @Test
    void suggestMovingPartner() throws Exception {
        mockMvc.perform(get("/filter/suggest")
                .param("entityType", "MOVING_PARTNER")
                .param("substring", "ЭК")
                .header("action", "create")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(IntegrationTestUtils.jsonContent("fixture/moving_partner/moving_partner_2.json",
                JSONCompareMode.LENIENT));
    }

    @Test
    void suggestMovingPartnerEmpty() throws Exception {
        mockMvc.perform(get("/filter/suggest")
                .param("entityType", "MOVING_PARTNER")
                .param("substring", "ЭК123")
                .header("action", "create")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(IntegrationTestUtils.jsonContent("fixture/moving_partner/moving_partner_empty.json",
                JSONCompareMode.LENIENT));
    }


    @Test
    void suggestMovementCourier() throws Exception {
        ExecuteCall<PageOfUserDto, RetryStrategy> result = Mockito.mock(ExecuteCall.class);
        Mockito.when(result.schedule()).thenReturn(CompletableFuture.completedFuture(
                UserDtoUtils.page(
                        UserDtoUtils.userDto(123L, "Виталий", "Улитка"),
                        UserDtoUtils.userDto(23L, "Василий", "Черепаха")
                )
        ));

        Mockito.when(userApiClient.internalUsersGet(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                                                    Mockito.any(), Mockito.any(), Mockito.any(),
                                                    Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(result);

        mockMvc.perform(get("/filter/suggest")
                .param("entityType", "MOVEMENT_COURIER")
                .param("substring", "В")
                .header("action", "get")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(IntegrationTestUtils.jsonContent("fixture/transport/movement_partner_response.json"));
    }

    private void setUpGetLogisticPoints() {
        Mockito.when(lmsClient.searchPartners(any()))
                .thenReturn(List.of(
                        partnerResponse(100L, "Софьино 1", "SOFINO", FULFILLMENT),
                        partnerResponse(200L, "Софьино 2", "SOFINO", FULFILLMENT),
                        partnerResponse(300L, "Софьино 3", "SOFINO", FULFILLMENT),
                        partnerResponse(400L, "Софьино 4", "SOFINO", FULFILLMENT)
                ));

        Mockito.when(lmsClient.getLogisticsPoints(any()))
            .thenReturn(List.of(
                logisticsPointResponse(1L, "первый", 100L),
                logisticsPointResponse(2L, "второй", 200L),
                logisticsPointResponse(3L, "третий", 300L),
                logisticsPointResponse(4L, "четвертый", 400L)
            ));
    }

    private static LogisticsPointFilter logisticsPointFilter(
        Set<PartnerType> types,
        Set<Long> subtypes,
        boolean active
    ) {
        return LogisticsPointFilter.newBuilder()
            .partnerTypes(types)
            .partnerSubtypesToExclude(subtypes)
            .active(active)
            .type(PointType.WAREHOUSE)
            .build();
    }

    private static LogisticsPointResponse logisticsPointResponse(Long id,
                                                                 String name,
                                                                 Long partnerId) {
        return LogisticsPointResponse.newBuilder()
            .id(id)
            .name(name)
            .partnerId(partnerId)
            .build();
    }

    private static PartnerTransportDto partnerTransportDto(long id, long partnerId, String name) {
        return PartnerTransportDto.newBuilder()
            .id(id)
            .partner(PartnerResponse.newBuilder()
                .id(partnerId)
                .name(name)
                .status(PartnerStatus.ACTIVE)
                .build()
            )
            .logisticsPointFrom(logisticsPointResponse(1L, "1", 1L))
            .logisticsPointTo(logisticsPointResponse(2L, "2", 1L))
            .build();
    }

    private static PartnerResponse partnerResponse(long id, String name, String readableName, PartnerType partnerType) {
        return PartnerResponse.newBuilder()
            .partnerType(partnerType)
            .readableName(readableName)
            .name(name)
            .id(id)
            .build();
    }

}
