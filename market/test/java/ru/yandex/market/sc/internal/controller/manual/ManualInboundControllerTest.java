package ru.yandex.market.sc.internal.controller.manual;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundStatus;
import ru.yandex.market.sc.core.domain.inbound.repository.Registry;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistrySortableRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryType;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.BarcodeType;
import ru.yandex.market.sc.core.domain.sortable.model.enums.DirectFlowType;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.sc.internal.controller.manual.xdoc.DeleteInboundRequest;
import ru.yandex.market.sc.internal.controller.manual.xdoc.InboundIdentifierType;
import ru.yandex.market.sc.internal.model.CreateDemoInboundWithRegistryDto;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.util.ScIntControllerCaller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.inbound.repository.RegistryType.FACTUAL;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ManualInboundControllerTest {

    private final MockMvc mockMvc;
    private final TestFactory testFactory;
    private final Clock clock;
    private final InboundRepository inboundRepository;
    private final RegistryRepository registryRepository;
    private final RegistrySortableRepository registrySortableRepository;
    private final SortableRepository sortableRepository;
    private final SortableQueryService sortableQueryService;
    private final XDocFlow flow;
    private final SortableTestFactory sortableTestFactory;
    private final EntityManager entityManager;
    private final ScIntControllerCaller caller;

    private SortingCenter sortingCenter;
    private User user;

    @BeforeEach
    void setUp() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, TestFactory.USER_UID_LONG);
        testFactory.storedWarehouse();
    }

    @Test
    @DisplayName("Ручное создание демо-поставки")
    void createInboundManual() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.CREATE_LOTS_FROM_REGISTRY, true);

        String orderExternalId = "ext-1";
        String placeExternalId1 = "ext-1-p1";
        String placeExternalId2 = "ext-1-p2";
        String stampBarcode = "stamp-1";
        String lastMileLotExternalId = "SC_LOT_726182";

        String crossDockOrderExternalId = "ext-cross-dock";
        String crossDockLotExternalId = "SC_LOT_726182-cross-dock";

        String inboundExternalId = "inb-ext-1";
        String registryId = "reg-1";

        testFactory.create(order(sortingCenter)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .externalId(orderExternalId)
                        .places(placeExternalId1, placeExternalId2)
                        .build())
                .updateShipmentDate(LocalDate.now(clock))
                .get();
        var createInboundDto = new CreateDemoInboundWithRegistryDto.CreateDemoInboundDto(
                sortingCenter.getId(),
                "wh-1",
                "next-sc-1",
                "DS_SC",
                inboundExternalId
        );
        var createRegistryDto = new CreateDemoInboundWithRegistryDto.CreateDemoRegistryDto(
                registryId,
                Map.of(orderExternalId, List.of(placeExternalId1, placeExternalId2)),
                List.of(crossDockOrderExternalId),
                Map.of(
                        orderExternalId, lastMileLotExternalId,
                        crossDockOrderExternalId, crossDockLotExternalId
                ),
                Map.of(lastMileLotExternalId, stampBarcode),
                Map.of(crossDockLotExternalId, 10000000123L)
        );
        caller.createInboundDemo(new CreateDemoInboundWithRegistryDto(createInboundDto, createRegistryDto))
                .andExpect(status().isOk())
                .andDo(print());
        var inbound = inboundRepository.findByExternalId(inboundExternalId).orElseThrow();
        var registries = registryRepository.findAllByInboundId(inbound.getId());
        assertThat(registries).hasSize(1);
        assertThat(registries.get(0).getExternalId()).isEqualTo(registryId);

        var regSortables = registrySortableRepository.findAllByInboundExternalId(inboundExternalId);
        assertThat(regSortables).hasSize(2);
        var lastMileLot = sortableQueryService.findOrThrow(sortingCenter, lastMileLotExternalId);
        assertThat(lastMileLot.getCrossDock()).isFalse();
        var crossDockLot = sortableQueryService.findOrThrow(sortingCenter, crossDockLotExternalId);
        assertThat(crossDockLot.getCrossDock()).isTrue();

        var stamps = lastMileLot.getBarcodes().stream().filter(b -> b.getBarcodeType() == BarcodeType.STAMP).toList();
        assertThat(stamps).hasSize(1);
        var stamp = stamps.get(0);
        assertThat(stamp.getBarcode()).isEqualTo(stampBarcode);
    }

    @Test
    @DisplayName("Нельзя завешить поставку которой не существует")
    void itIsNotAllowedToFinishNonExistentInbound() throws Exception {
        finishInbound("99999")
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Завершение xDoc поставки для одного Sortable. Успешно созданы записи реестра")
    void sortablesAndRegistryAreAddedForXDocWithOneUnit() throws Exception {
        Inbound inbound = createdInbound("1", InboundType.XDOC_TRANSIT);
        testFactory.linkSortableToInbound(inbound, "XDOC-1", SortableType.XDOC_PALLET, user);
        finishInbound(inbound.getExternalId())
                .andExpect(status().isOk());

        Inbound after = inboundRepository.findByExternalId(inbound.getExternalId()).orElseThrow();
        assertThat(after.getInboundStatus())
                .isEqualTo(InboundStatus.FIXED);

        assertThat(registryRepository.findAllByInboundId(inbound.getId()))
                .allMatch(reg -> RegistryType.FACTUAL == reg.getType())
                .hasSize(1);
    }

    @Test
    @DisplayName("Завершение xDoc поставки для нескольких Sortable. Успешно созданы записи реестра")
    void sortablesAndRegistryAreAddedForXDocWithSeveralUnits() throws Exception {
        Inbound inbound = createdInbound("1", InboundType.XDOC_TRANSIT);
        testFactory.linkSortableToInbound(inbound, "XDOC-1", SortableType.XDOC_PALLET, user);
        testFactory.linkSortableToInbound(inbound, "XDOC-2", SortableType.XDOC_PALLET, user);
        finishInbound(inbound.getExternalId())
                .andExpect(status().isOk());

        Inbound after = inboundRepository.findByExternalId(inbound.getExternalId()).orElseThrow();
        assertThat(after.getInboundStatus())
                .isEqualTo(InboundStatus.FIXED);

        assertThat(registryRepository.findAllByInboundId(inbound.getId()))
                .allMatch(reg -> RegistryType.FACTUAL == reg.getType())
                .hasSize(1);
    }

    @Test
    @DisplayName("Не вилидное состояние xDoc поставки. В нее попали не xDoc Sortable. Нельзя завершить такую поставку")
    void serverErrorOnFinishInboundThatContainsBothXDocAndNonXDocSortables() throws Exception {
        Inbound inbound = createdInbound("1", InboundType.XDOC_TRANSIT);
        testFactory.linkSortableToInbound(inbound, "XDOC-1", SortableType.XDOC_PALLET, user);
        testFactory.linkSortableToInbound(inbound, "XDOC-2", SortableType.XDOC_PALLET, user);
        // выставляю не валидный тип для одного из unit'ов в поставке
        setTypeForSortableBy("XDOC-2", SortableType.PALLET);

        finishInbound(inbound.getExternalId())
                .andExpect(status().is5xxServerError());

        Inbound after = inboundRepository.findByExternalId(inbound.getExternalId()).orElseThrow();
        assertThat(after.getInboundStatus())
                .isNotEqualTo(InboundStatus.FIXED);

        assertThat(registryRepository.findAllByInboundId(inbound.getId())).hasSize(0);
        assertThat(registrySortableRepository.findAll()).hasSize(0);
    }

    @Test
    @DisplayName("Запрещено завершать одну и ту же поставку повторно")
    void attemptToFinishSameInboundSecondTime() throws Exception {
        Inbound inbound = createdInbound("1", InboundType.XDOC_TRANSIT);
        testFactory.linkSortableToInbound(inbound, "XDOC-1", SortableType.XDOC_PALLET, user);
        finishInbound(inbound.getExternalId())
                .andExpect(status().isOk());

        Inbound after = inboundRepository.findByExternalId(inbound.getExternalId()).orElseThrow();
        assertThat(after.getInboundStatus())
                .isEqualTo(InboundStatus.FIXED);

        finishInbound(inbound.getExternalId())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Завершение xDoc поставки. Реестр получит externalId равный id записи в таблице")
    void doIt2() throws Exception {
        Inbound inbound = createdInbound("1", InboundType.XDOC_TRANSIT);
        testFactory.linkSortableToInbound(inbound, "XDOC-1", SortableType.XDOC_PALLET, user);
        finishInbound(inbound.getExternalId())
                .andExpect(status().isOk());

        Inbound after = inboundRepository.findByExternalId(inbound.getExternalId()).orElseThrow();
        assertThat(after.getInboundStatus())
                .isEqualTo(InboundStatus.FIXED);

        assertThat(registryRepository.findAllByInboundId(inbound.getId()))
                .allMatch(reg -> Objects.areEqual(Long.toString(reg.getId()), reg.getExternalId()))
                .hasSize(1);
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешное изменение типа XDOC сущности")
    void changeSortableTypeSuccess() {
        flow.createInbound("in-1")
                .linkPallets("XDOC-1");

        mockMvc.perform(
                MockMvcRequestBuilders.put("/manual/inbound/in-1/changeSortableType")
                        .param("sortableType", String.valueOf(SortableType.XDOC_PALLET))
        ).andExpect(status().isOk());

        Sortable expected =
                sortableQueryService.findOrThrow(sortingCenter, "XDOC-1");

        assertThat(expected).hasFieldOrPropertyWithValue("type", SortableType.XDOC_PALLET);
    }

    @Test
    @SneakyThrows
    @DisplayName("Ручное создание и фиксация фактического реестра")
    void createAndFixFactualRegistryTest() {
        var inbound = testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundExternalId("in-1")
                .inboundType(InboundType.DS_SC)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId("warehouse-from-id")
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of("registry_1", Collections.emptyList()))
                .build());

        var registry =
                registryRepository.findAllByInboundId(inbound.getId()).stream().filter(it -> it.getType() == FACTUAL).findFirst();
        assertThat(registry).isEmpty();

        mockMvc.perform(
                MockMvcRequestBuilders.put("/manual/inbound/in-1/createAndFixFactualRegistry")
        ).andExpect(status().isOk());

        inbound = testFactory.getInbound(inbound.getExternalId());
        registry =
                registryRepository.findAllByInboundId(inbound.getId()).stream().filter(it -> it.getType() == FACTUAL).findFirst();
        assertThat(registry).isPresent();
    }

    @Test
    @SneakyThrows
    @DisplayName("Отправка реестра inbound в transferAct")
    void putInboundTransferTest() {
        var inbound = testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundExternalId("in-1")
                .inboundType(InboundType.DS_SC)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId("warehouse-from-id")
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of("plan-reg", Collections.emptyList()))
                .build());
        var registry = registryRepository.save(Registry.inboundRegistry("fact_reg", inbound, FACTUAL));

        mockMvc.perform(
                MockMvcRequestBuilders.put("/manual/inbound/in-1/putInboundTransfer")
                        .queryParam("registryId", String.valueOf(registry.getId()))
        ).andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    @DisplayName("неудачное изменение типа XDOC сущности")
    void changeSortableTypeInboundFixed() {
        flow.createInbound("in-1")
                .linkPallets("XDOC-1")
                .fixInbound();

        mockMvc.perform(
                MockMvcRequestBuilders.put("/manual/inbound/in-1/changeSortableType")
                        .param("sortableType", String.valueOf(SortableType.XDOC_PALLET))
        ).andExpect(status().is4xxClientError());

        Sortable expected =
                sortableQueryService.findOrThrow(sortingCenter, "XDOC-1");

        assertThat(expected).hasFieldOrPropertyWithValue("type", SortableType.XDOC_PALLET);
    }

    @Test
    @SneakyThrows
    @DisplayName("Изменение типа не XDOC сущности.")
    @Transactional
    void changeSortableTypeForNonXdoc() {
        var params = TestFactory.CreateInboundParams
                .builder()
                .inboundExternalId("in-1")
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .inboundType(InboundType.DS_SC)
                .sortingCenter(sortingCenter)
                .plainOrders(List.of(new Pair<>("ext-o-1", "ext-o-1-1")))
                .build();
        var inbound = testFactory.createInbound(params);

        Sortable sortable = sortableTestFactory.storeSortable(sortingCenter, SortableType.BOX, DirectFlowType.TRANSIT
                , "123", inbound, null).get();

        sortable.setStatus(SortableStatus.ARRIVED_DIRECT, null);
        sortableRepository.save(sortable);
        entityManager.flush();

        mockMvc.perform(
                        MockMvcRequestBuilders.put("/manual/inbound/in-1/changeSortableType")
                                .param("sortableType", String.valueOf(SortableType.XDOC_BOX))
                )
                .andExpect(status().is4xxClientError())
                .andExpect(content().json(String.format("{\"message\": \"Can not change sortable type for sortable " +
                        "[%s], not XDOC sortable\"}", sortable)));

    }

    @Disabled("Не проходят в CI - пока не понятно почему")
    @ParameterizedTest(name = "Ручка удаление inbound (используется для автотестов)")
    @MethodSource("deletionRequests")
    void deleteInbound(DeleteInboundRequest request) {
        Inbound inbound = flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-192929")
                .build()
                .linkPallets("XDOC-1", "XDOC-2")
                .fixInbound()
                .getInbound("in-1");

        assertThat(inboundRepository.findById(inbound.getId())).isNotEmpty();

        assertThat(
                sortableQueryService.findAllHavingAnyBarcode(sortingCenter, Set.of("XDOC-1", "XDOC-2"))
                        .stream().map(Sortable::getRequiredBarcodeOrThrow).toList()
        ).containsExactlyInAnyOrder("XDOC-1", "XDOC-2");

        caller.deleteInbound(request)
                .andExpect(status().isOk());

        assertThat(inboundRepository.findById(inbound.getId())).isEmpty();

        assertThat(
                sortableQueryService.findAllHavingAnyBarcode(sortingCenter, Set.of("XDOC-1", "XDOC-2"))
                        .stream().map(Sortable::getRequiredBarcodeOrThrow).toList()
        ).isEmpty();
    }

    private static List<Arguments> deletionRequests() {
        return List.of(
                Arguments.of(new DeleteInboundRequest(
                        TestFactory.SC_ID,
                        InboundIdentifierType.EXTERNAL_ID,
                        "in-1"
                )),
                Arguments.of(new DeleteInboundRequest(
                        TestFactory.SC_ID,
                        InboundIdentifierType.INFO_LIST_CODE,
                        "Зп-192929"
                ))
        );
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

    private void setTypeForSortableBy(String barcode, SortableType newType) {
        Sortable sortable = sortableQueryService.findOrThrow(sortingCenter, barcode);
        sortable.setType(newType);
        sortableRepository.save(sortable);
    }

    private ResultActions finishInbound(String externalId) throws Exception {
        return mockMvc.perform(put("/manual/inbounds/fixInbound")
                .contentType(MediaType.APPLICATION_JSON)
                .param("externalId", externalId));
    }

}
