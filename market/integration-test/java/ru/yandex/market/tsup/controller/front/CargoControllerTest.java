package ru.yandex.market.tsup.controller.front;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.delivery.gruzin.client.GruzinClient;
import ru.yandex.market.delivery.gruzin.model.CargoUnitDto;
import ru.yandex.market.delivery.gruzin.model.UnitCargoType;
import ru.yandex.market.delivery.gruzin.model.UnitType;
import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.delivery.transport_manager.model.dto.trip.CargoUnitIdWithDirection;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;
import ru.yandex.market.tpl.common.data_provider.meta.FrontHttpRequestMeta;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.controller.dto.cargo.InboundRemoveDto;
import ru.yandex.mj.generated.client.carrier.api.RunApiClient;
import ru.yandex.mj.generated.client.carrier.model.RunIdDto;
import ru.yandex.mj.generated.client.carrier.model.StringToLongMapEntry;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class CargoControllerTest extends AbstractContextualTest {

    private static final Instant INBOUND_TIME = LocalDateTime.of(2022, 5, 6, 9, 31, 0).toInstant(ZoneOffset.UTC);

    @Autowired
    GruzinClient gruzinClient;
    @Autowired
    TransportManagerClient tmClient;
    @Autowired
    RunApiClient runApiClient;
    @Autowired
    LMSClient lmsClient;

    @BeforeEach
    void setUp() {
        Mockito.when(lmsClient.getLogisticsPoints(Mockito.any()))
                .thenReturn(List.of(
                        LogisticsPointResponse.newBuilder().id(1001L).partnerId(1L).name("Point name 1").build(),
                        LogisticsPointResponse.newBuilder().id(1002L).partnerId(2L).name("Point name 2").build(),
                        LogisticsPointResponse.newBuilder().id(1003L).partnerId(3L).name("Point name 3").build(),
                        LogisticsPointResponse.newBuilder().id(1004L).partnerId(4L).name("Point name 4").build(),
                        LogisticsPointResponse.newBuilder().id(1005L).partnerId(5L).name("Point name 5").build(),
                        LogisticsPointResponse.newBuilder().id(1006L).partnerId(6L).name("Point name 6").build()
                ));

        ExecuteCall<List<StringToLongMapEntry>, RetryStrategy> call = Mockito.mock(ExecuteCall.class);
        Mockito.when(runApiClient.internalRunsByExternalIdPut(Mockito.any())).thenReturn(call);
        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(List.of(
                new StringToLongMapEntry().key("TMT0001").value(1L),
                new StringToLongMapEntry().key("TMT0002").value(2L),
                new StringToLongMapEntry().key("TMT0003").value(3L),
                new StringToLongMapEntry().key("TMT0004").value(4L)
        )));

        Mockito.when(tmClient.getMostRecentTripsByCargoUnitIdsWithDirection(Mockito.any()))
                .thenReturn(Map.of(
                        new CargoUnitIdWithDirection().setUnitId("DRP0001").setLogisticPointFromId(1001L)
                                .setLogisticPointToId(1002L), "TMT0001",
                        new CargoUnitIdWithDirection().setUnitId("DRP0002").setLogisticPointFromId(1001L)
                                .setLogisticPointToId(1002L), "TMT0001",
                        new CargoUnitIdWithDirection().setUnitId("DRP0003").setLogisticPointFromId(1003L)
                                .setLogisticPointToId(1004L), "TMT0002",
                        new CargoUnitIdWithDirection().setUnitId("DRP0004").setLogisticPointFromId(1003L)
                                .setLogisticPointToId(1004L), "TMT0002",
                        new CargoUnitIdWithDirection().setUnitId("DRP0005").setLogisticPointFromId(1003L)
                                .setLogisticPointToId(1004L), "TMT0002",
                        new CargoUnitIdWithDirection().setUnitId("DRP0006").setLogisticPointFromId(1003L)
                                .setLogisticPointToId(1004L), "TMT0001",
                        new CargoUnitIdWithDirection().setUnitId("DRP0009").setLogisticPointFromId(1003L)
                                .setLogisticPointToId(1004L), "TMT0003"
                ));

        Mockito.when(gruzinClient.search(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(List.of(
                        //tripId=TMT0001
                        new CargoUnitDto().setId(1L).setUnitId("DRP0001")
                                .setFrozen(true).setVolume(null).setSubUnitCount(1)
                                .setLogisticPointFrom(1001L).setLogisticPointTo(1002L)
                                .setUnitType(UnitType.PALLET).setUnitCargoType(UnitCargoType.XDOCK)
                                .setInboundTime(INBOUND_TIME).setInboundExternalId("TMU00001"),
                        new CargoUnitDto().setId(2L).setUnitId("DRP0002")
                                .setFrozen(true).setVolume(10).setSubUnitCount(3)
                                .setLogisticPointFrom(1001L).setLogisticPointTo(1002L)
                                .setUnitType(UnitType.PALLET).setUnitCargoType(UnitCargoType.XDOCK)
                                .setInboundTime(INBOUND_TIME).setInboundExternalId("TMU00001"),

                        //tripId=TMT0002
                        new CargoUnitDto().setId(3L).setUnitId("DRP0003")
                                .setFrozen(true).setVolume(10).setSubUnitCount(3)
                                .setLogisticPointFrom(1003L).setLogisticPointTo(1004L)
                                .setUnitType(UnitType.PALLET).setUnitCargoType(UnitCargoType.XDOCK)
                                .setInboundTime(INBOUND_TIME).setInboundExternalId("TMU00002"),
                        new CargoUnitDto().setId(4L).setUnitId("DRP0004")
                                .setFrozen(true).setVolume(10).setSubUnitCount(null)
                                .setLogisticPointFrom(1003L).setLogisticPointTo(1004L)
                                .setUnitType(UnitType.PALLET).setUnitCargoType(UnitCargoType.XDOCK)
                                .setInboundTime(INBOUND_TIME).setInboundExternalId("TMU00002"),
                        new CargoUnitDto().setId(5L).setUnitId("DRP0005")
                                .setFrozen(true).setVolume(10).setSubUnitCount(3)
                                .setLogisticPointFrom(1003L).setLogisticPointTo(1004L)
                                .setUnitType(UnitType.PALLET).setUnitCargoType(UnitCargoType.XDOCK)
                                .setInboundTime(INBOUND_TIME).setInboundExternalId("TMU00002"),

                        //tripId=TMT0001
                        new CargoUnitDto().setId(6L).setUnitId("DRP0006")
                                .setFrozen(true).setVolume(10).setSubUnitCount(3)
                                .setLogisticPointFrom(1003L).setLogisticPointTo(1004L)
                                .setUnitType(UnitType.PALLET).setUnitCargoType(UnitCargoType.XDOCK)
                                .setInboundTime(INBOUND_TIME).setInboundExternalId("TMU00003"),

                        //tripId=null
                        new CargoUnitDto().setId(6L).setUnitId("DRP0007")
                                .setFrozen(false).setVolume(10).setSubUnitCount(3)
                                .setLogisticPointFrom(1003L).setLogisticPointTo(1004L)
                                .setUnitType(UnitType.PALLET).setUnitCargoType(UnitCargoType.XDOCK)
                                .setInboundTime(INBOUND_TIME).setInboundExternalId("TMU00004"),

                        //inconsistent
                        new CargoUnitDto().setId(6L).setUnitId("DRP0008")
                                .setFrozen(false).setVolume(10).setSubUnitCount(3)
                                .setLogisticPointFrom(1003L).setLogisticPointTo(1004L)
                                .setUnitType(UnitType.PALLET).setUnitCargoType(UnitCargoType.XDOCK)
                                .setInboundTime(INBOUND_TIME).setInboundExternalId("TMU00005"),
                        new CargoUnitDto().setId(6L).setUnitId("DRP0009")
                                .setFrozen(true).setVolume(10).setSubUnitCount(3)
                                .setLogisticPointFrom(1003L).setLogisticPointTo(1004L)
                                .setUnitType(UnitType.PALLET).setUnitCargoType(UnitCargoType.XDOCK)
                                .setInboundTime(INBOUND_TIME).setInboundExternalId("TMU00005")


                ));

        ExecuteCall<List<RunIdDto>, RetryStrategy> callIds = Mockito.mock(ExecuteCall.class);
        Mockito.when(runApiClient.internalRunsIdsGet(
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any()
        )).thenReturn(callIds);
        Mockito.when(callIds.schedule()).thenReturn(CompletableFuture.completedFuture(List.of(
                new RunIdDto().id(1L).externalId("TMT0001"),
                new RunIdDto().id(2L).externalId("TMT0002")
        )));
    }

    @SneakyThrows
    @Test
    void shouldGetInbounds() {
        mockMvc.perform(get("/cargo/inbounds")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "staff-login")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(IntegrationTestUtils.jsonContent("fixture/cargo/inbounds_response.json"));
    }


    @SneakyThrows
    @Test
    void shouldAddInboundsIntoTrip() {
        var externalIds = List.of("TMU00001", "TMU00002");
        mockMvc.perform(post("/cargo/inbounds/trips/TMT0002")
                        .content(objectMapper.writeValueAsString(externalIds))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "staff-login")
                )
                .andDo(print())
                .andExpect(status().isOk());
    }


    @SneakyThrows
    @Test
    void shouldRemoveInboundsFromTrip() {
        InboundRemoveDto dto = new InboundRemoveDto()
                .setTripIds(List.of("TMT1"))
                .setInboundExternalIds(List.of("TMU00001", "TMU00002"));
        mockMvc.perform(delete("/cargo/inbounds")
                        .content(objectMapper.writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "staff-login")
                )
                .andDo(print())
                .andExpect(status().isOk());
    }


    @SneakyThrows
    @Test
    void shouldRemoveAndAddInboundsInTrip() {
        InboundRemoveDto dto = new InboundRemoveDto()
                .setTripIds(List.of("TMT1"))
                .setInboundExternalIds(List.of("TMU00001", "TMU00002"));
        mockMvc.perform(put("/cargo/inbounds/trips/TMT2")
                        .content(objectMapper.writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "staff-login")
                )
                .andDo(print())
                .andExpect(status().isOk());
    }


    @SneakyThrows
    @Test
    void shouldSuggestRuns() {
        mockMvc.perform(get("/cargo/inbounds/runs?logisticPointFromId=1003&logisticPointToId=1004")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "staff-login")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(IntegrationTestUtils.jsonContent("fixture/cargo/runs_suggest_response.json"));
    }
}
