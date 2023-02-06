package ru.yandex.market.sc.internal.controller.manual;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.Registry;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryType;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundRepository;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.DirectFlowType;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.inbound.repository.RegistryType.FACTUAL;

@ScIntControllerTest
class ManualOutboundControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    SortableRepository sortableRepository;
    @Autowired
    OutboundRepository outboundRepository;

    @Autowired
    TestFactory testFactory;
    @Autowired
    SortableTestFactory sortableTestFactory;
    @Autowired
    private RegistryRepository registryRepository;
    @Autowired
    XDocFlow flow;
    @Autowired
    Clock clock;

    SortingCenter sortingCenter;
    User user;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, TestFactory.USER_UID_LONG);
        warehouse = testFactory.storedWarehouse(TestFactory.WAREHOUSE_YANDEX_ID);
    }

    @Test
    @DisplayName("Две поставки, все приняты, всё ок")
    void manualShipInbounds() throws Exception {
        flow.createInbound("in-1")
                .linkPallets("XDOC-1", "XDOC-2")
                .fixInbound()
                .createInbound("in-2")
                .linkPallets("XDOC-3")
                .fixInbound()
                .createOutbound("out-1")
                .buildRegistry("XDOC-1", "XDOC-2", "XDOC-3");

        finish("out-1", List.of("in-1", "in-2"))
                .andExpect(status().isOk());
        assertThat(sortableRepository.findAll())
                .allMatch(sortable -> sortable.getStatus() == SortableStatus.SHIPPED_DIRECT)
                .hasSize(3);
        assertThat(outboundRepository.findByExternalId("out-1").orElseThrow().getStatus())
                .isEqualTo(OutboundStatus.SHIPPED);
    }

    @Test
    @DisplayName("Одна из поставок не завершена => нельзя её отгрузить")
    void manualShipNotFixedInbound() throws Exception {
        Inbound first = createdInbound("1", InboundType.XDOC_TRANSIT);
        Inbound second = createdInbound("2", InboundType.XDOC_TRANSIT);
        testFactory.linkSortableToInbound(first, "XDOC-1", SortableType.XDOC_PALLET, user);
        testFactory.linkSortableToInbound(first, "XDOC-2", SortableType.XDOC_PALLET, user);
        testFactory.linkSortableToInbound(second, "XDOC-3", SortableType.XDOC_PALLET, user);
        testFactory.finishInbound(first);
        testFactory.createOutbound("1", OutboundStatus.CREATED, OutboundType.XDOC, clock.instant(), clock.instant(),
                "partnerId", sortingCenter, null);
        finish("1", List.of("1", "2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Нельзя вручную отгрузить не xdoc поставку")
    void manualShipNonXdoc() throws Exception {
        Inbound first = createdInbound("1", InboundType.DEFAULT);
        Inbound second = createdInbound("2", InboundType.XDOC_TRANSIT);
        testFactory.linkSortableToInbound(second, "XDOC-3", SortableType.XDOC_PALLET, user);
        testFactory.finishInbound(first);
        testFactory.finishInbound(second);
        testFactory.createOutbound("1", OutboundStatus.CREATED, OutboundType.XDOC, clock.instant(), clock.instant(),
                "partnerId", sortingCenter, null);
        finish("1", List.of("1", "2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Нельзя отгрузить неизвестную поставку")
    void manualShipUnknownInbound() throws Exception {
        Inbound first = createdInbound("1", InboundType.XDOC_TRANSIT);
        testFactory.linkSortableToInbound(first, "XDOC-1", SortableType.XDOC_PALLET, user);
        testFactory.linkSortableToInbound(first, "XDOC-2", SortableType.XDOC_PALLET, user);
        testFactory.createOutbound("1", OutboundStatus.CREATED, OutboundType.XDOC, clock.instant(), clock.instant(),
                "partnerId", sortingCenter, null);
        finish("1", List.of("1", "2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Нельзя вручную отгрузить неизвестный outbound")
    void manualShipUnknownOutbound() throws Exception {
        Inbound first = createdInbound("1", InboundType.XDOC_TRANSIT);
        testFactory.linkSortableToInbound(first, "XDOC-1", SortableType.XDOC_PALLET, user);
        finish("2", List.of("1"));
    }

    @Test
    @DisplayName("Нельзя вручную отгрузить не xdoc outbound")
    void manualShipNonXdocOutbound() throws Exception {
        Inbound first = createdInbound("1", InboundType.XDOC_TRANSIT);
        testFactory.linkSortableToInbound(first, "XDOC-1", SortableType.XDOC_PALLET, user);
        testFactory.createOutbound("1", OutboundStatus.CREATED, OutboundType.DS_SC, clock.instant(), clock.instant(),
                "partnerId", sortingCenter, null);
        finish("1", List.of("1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDemo() throws Exception {
        var externalId = mockMvc.perform(post("/manual/outbounds/createDemo?scId=" + sortingCenter.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        externalId = fixExternalId(externalId);

        assertThat(outboundRepository.findByExternalId(externalId)).isNotEmpty();
    }

    @Test
    void updateDemo() throws Exception {
        var externalId = mockMvc.perform(post("/manual/outbounds/createDemo?scId=" + sortingCenter.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        externalId = fixExternalId(externalId);

        var newExternalId = mockMvc.perform(post(String.format("/manual/outbounds/createDemo?scId=%d&externalId=%s",
                sortingCenter.getId(), externalId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        newExternalId = fixExternalId(newExternalId);

        assertThat(newExternalId).isEqualTo(externalId);
        assertThat(outboundRepository.findByExternalId(newExternalId)).isNotEmpty();
    }

    @Test
    void createDemoWithBody() throws Exception {
        var externalId = mockMvc.perform(post("/manual/outbounds/createDemo?scId=" + sortingCenter.getId())
                .param("logisticPointToExternalId", warehouse.getYandexId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "outboundType": "XDOC"
                        }"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        externalId = fixExternalId(externalId);

        var outbound = outboundRepository.findByExternalId(externalId);

        assertThat(outbound).isNotEmpty();
        assertThat(outbound.get().getType()).isEqualTo(OutboundType.XDOC);
    }

    @Test
    @DisplayName("Изменить номер машины в руте")
    void changeCarNumberInRoute() throws Exception {
        Inbound first = createdInbound("1", InboundType.XDOC_TRANSIT);
        testFactory.linkSortableToInbound(first, "XDOC-1", SortableType.XDOC_PALLET, user);
        TestFactory.CreateOutboundParams params = TestFactory.CreateOutboundParams.builder()
                .externalId("1")
                .partnerToExternalId(warehouse.getPartnerId())
                .logisticPointToExternalId(warehouse.getYandexId())
                .type(OutboundType.XDOC)
                .fromTime(clock.instant())
                .toTime(clock.instant())
                .sortingCenter(sortingCenter)
                .build();

        var outbound = testFactory.createOutbound(params);
        var routeSo = sortableTestFactory.getRouteSo(outbound);

        String newCarNumber = "В 007 ОР 77";
        changeCarNumber(routeSo.getId(), newCarNumber)
                .andExpect(status().isOk());

        routeSo = sortableTestFactory.getRouteSo(outbound);
        assertThat(routeSo.getCarNumber()).isEqualTo(newCarNumber);
    }

    @Test
    @DisplayName("Ручное создание реестра на отгрузку")
    void putOutboundRegistry() throws Exception {
        var outbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("1001")
                .type(OutboundType.XDOC)
                .fromTime(clock.instant())
                .toTime(clock.instant())
                .sortingCenter(sortingCenter)
                .partnerToExternalId(warehouse.getPartnerId())
                .logisticPointToExternalId(warehouse.getYandexId())
                .build()
        );
        mockMvc.perform(post("/manual/outbounds/" + outbound.getExternalId() + "/plannedRegistry")
                .contentType(MediaType.APPLICATION_JSON)
                .param("scId", sortingCenter.getId().toString())
                .param("registryExternalId", "123")
                .param("palletExternalIds", "XDOC-109")
                .param("palletExternalIds", "XDOC-110")
        );
        var registries = registryRepository.findAllByOutboundId(outbound.getId());
        assertThat(registries)
                .hasSize(1)
                .allMatch(registry -> registry.getType() == RegistryType.PLANNED);

    }

    @SneakyThrows
    @Test
    void cancelOutbound() {
        Inbound inbound = testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundType(InboundType.XDOC_TRANSIT)
                .nextLogisticPointId(warehouse.getYandexId())
                .toDate(OffsetDateTime.now(clock))
                .fromDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .warehouseFromExternalId(warehouse.getYandexId())
                .build());
        var sortable = sortableTestFactory.storeSortable(sortingCenter, SortableType.XDOC_PALLET,
                DirectFlowType.TRANSIT, "XDOC-111"
                , inbound, user).get();
        var outbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("1000")
                .sortingCenter(sortingCenter)
                .logisticPointToExternalId(warehouse.getYandexId())
                .partnerToExternalId(warehouse.getPartnerId())
                .carNumber("A777MP77")
                .type(OutboundType.XDOC)
                .fromTime(Instant.now(clock))
                .toTime(Instant.now(clock))
                .build()
        );

        sortableTestFactory.createOutboundRegistry(SortableTestFactory.CreateOutboundRegistryParams.builder()
                .outboundExternalId(outbound.getExternalId())
                .sortingCenter(sortingCenter)
                .sortables(List.of(sortable))
                .build());

        mockMvc.perform(put("/manual/outbounds/" + outbound.getExternalId() + "/cancel"))
                .andExpect(status().isOk());

        assertThat(outboundRepository.findByIdOrThrow(outbound.getId()).getStatus())
                .isEqualTo(OutboundStatus.SHIPPED);
    }

    @Test
    @SneakyThrows
    @DisplayName("Отправка реестра outbound в transferAct")
    void putOutboundTransferTest() {
        var outbound = testFactory.createOutbound(sortingCenter);
        var registry = registryRepository.save(Registry.outboundRegistry("factual_reg", outbound, FACTUAL, null));

        mockMvc.perform(
                MockMvcRequestBuilders.put("/manual/outbounds/" + outbound.getExternalId() + "/putOutboundTransfer")
                        .queryParam("registryId", String.valueOf(registry.getId()))
        ).andExpect(status().isOk());
    }

    private static String fixExternalId(String externalId) {
        return externalId.substring(1, externalId.length() - 1);
    }

    private Inbound createdInbound(String externalId, InboundType inboundType) {
        var params = TestFactory.CreateInboundParams.builder()
                .inboundExternalId(externalId)
                .inboundType(inboundType)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId("warehouse-from-id")
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of())
                .confirmed(true)
                .build();
        return testFactory.createInbound(params);
    }

    private ResultActions finish(String outboundId, List<String> inboundExternalIds) throws Exception {
        return mockMvc.perform(post("/manual/outbounds/" + outboundId + "/customShip")
                .contentType(MediaType.APPLICATION_JSON)
                .content(StreamEx.of(inboundExternalIds)
                        .map(id -> "\"" + id + "\"")
                        .joining(",", "{\"inboundExternalIds\": [", "]}")));
    }

    private ResultActions changeCarNumber(long routeSoId, String carNumber) throws Exception {
        return mockMvc.perform(post("/manual/so/routes/" + routeSoId + "/car")
                .contentType(MediaType.APPLICATION_JSON)
                .param("carNumber", carNumber));
    }
}
