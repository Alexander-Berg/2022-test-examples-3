package ru.yandex.market.sc.internal.controller.manual;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.domain.cell.model.CellRequestDto;
import ru.yandex.market.sc.core.domain.cell.model.CellStatus;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.inbound.model.RegistryUnitType;
import ru.yandex.market.sc.core.domain.inbound.repository.Registry;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistrySortable;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistrySortableRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryType;
import ru.yandex.market.sc.core.domain.lot.model.PartnerLotRequestDto;
import ru.yandex.market.sc.core.domain.lot.repository.LotRepository;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundStatus;
import ru.yandex.market.sc.core.domain.scan.ScanService;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableAPIAction;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableRepository;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.sc.internal.controller.dto.PartnerCellDtoWrapper;
import ru.yandex.market.sc.internal.controller.dto.PartnerLotDtoWrapper;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.util.ScIntControllerCaller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.outbound.model.OutboundType.XDOC;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.XDOC_ENABLED;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@ScIntControllerTest
@ExtendWith(DefaultScUserWarehouseExtension.class)
public class XDocOutboundShipTest {

    private static final String INBOUND_ID = "inbound-1";
    private static final String OUTBOUND_ID = "outbound-1";

    private final XDocFlow flow;
    private final ScIntControllerCaller caller;
    private final TestFactory testFactory;
    private final SortableTestFactory sortableTestFactory;
    private final SortableRepository sortableRepository;
    private final LotRepository lotRepository;
    private final RegistryRepository registryRepository;
    private final RegistrySortableRepository registrySortableRepository;
    private final SortableQueryService sortableQueryService;
    private final ScanService scanService;

    @MockBean
    Clock clock;

    @BeforeEach
    void init() {
        testFactory.setupMockClock(clock);
    }

    @DisplayName("Успешная валидация")
    @Test
    void validationIsSuccessful() {
        flow.createInbound(INBOUND_ID)
                .linkPallets("XDOC-1")
                .fixInbound()
                .createOutbound(OUTBOUND_ID)
                .buildRegistry("XDOC-1")
                .sortToAvailableCell("XDOC-1")
                .prepareToShip("XDOC-1")
                .callShip(OUTBOUND_ID, res -> res.andExpect(status().isOk()));
    }

    @DisplayName("Успешная валидации при XDOC_BASKET")
    @Test
    void validationIsSuccessfulForBasket() {
        var basket = flow.createBasketAndGet();

        flow.createInbound(INBOUND_ID)
                .linkPallets("XDOC-1")
                .fixInbound()
                .createOutbound(OUTBOUND_ID)
                .buildRegistry("XDOC-1", basket.getRequiredBarcodeOrThrow())
                .sortToAvailableCell("XDOC-1")
                .prepareToShip("XDOC-1")
                .shipAndGet(OUTBOUND_ID);
    }

    @DisplayName("При успешной отгрузке все sortable переходят в статус SHIPPED_DIRECT")
    @Test
    void successfulOutboundSortablesAreShipped() {
        flow.createInbound(INBOUND_ID)
                .linkPallets("XDOC-1")
                .fixInbound()
                .createOutbound(OUTBOUND_ID)
                .buildRegistry("XDOC-1")
                .sortToAvailableCell("XDOC-1")
                .prepareToShip("XDOC-1")
                .shipAndGet(OUTBOUND_ID);

        assertThat(sortableQueryService.findAllHavingAnyBarcode(flow.getSortingCenter(), Set.of("XDOC-1")))
                .hasSize(1)
                .allMatch(sortable -> sortable.getStatus() == SortableStatus.SHIPPED_DIRECT)
                .allMatch(sortable -> sortable.getCell() == null)
                .allMatch(sortable -> OUTBOUND_ID.equals(Objects.requireNonNull(sortable.getOutbound()).getExternalId()));
    }

    @DisplayName("фактический реестр будет создан после успешной отгрузки")
    @Test
    void factualRegistryCreatedAfterSuccessfulShip() {
        flow.createInbound(INBOUND_ID)
                .linkPallets("XDOC-1", "XDOC-2", "XDOC-3")
                .fixInbound()
                .createOutbound(OUTBOUND_ID)
                .buildRegistry("XDOC-1", "XDOC-2", "XDOC-3")
                .sortToAvailableCell("XDOC-1", "XDOC-2", "XDOC-3")
                .prepareToShip("XDOC-1", "XDOC-2", "XDOC-3");

        var outbound = flow.getOutbound(OUTBOUND_ID);

        // убедимся что до ship существует только один реестр
        List<Registry> before = registryRepository.findAllByOutboundId(outbound.getId());
        assertThat(before)
                .hasSize(1)
                .allMatch(reg -> reg.getType() == RegistryType.PLANNED);

        caller.shipOutbound(OUTBOUND_ID).andExpect(status().isOk());

        List<Registry> after = registryRepository.findAllByOutboundId(outbound.getId());
        assertThat(after)
                .hasSize(2);

        var factualRegistry = after.stream()
                .filter(reg -> reg.getType() == RegistryType.FACTUAL)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Отсутствет фактический реестр"));

        List<RegistrySortable> regToSorts = registrySortableRepository.findAllByRegistryIn(Set.of(factualRegistry));
        assertThat(regToSorts)
                .hasSize(3)
                .allMatch(regToSort -> RegistryUnitType.PALLET == regToSort.getUnitType())
                .matches(regSorts -> regSorts.stream()
                        .map(RegistrySortable::getSortableExternalId)
                        .collect(Collectors.toSet())
                        .containsAll(Set.of("XDOC-1", "XDOC-2", "XDOC-3"))
                );
    }

    @Test
    @DisplayName("У заказов, не отгруженных в outbound очистился outbound")
    void shipOneOutbound() {
        var outbound = flow.createInbound(INBOUND_ID)
                .linkPallets("XDOC-1")
                .fixInbound()
                .createInbound("inbound-2")
                .linkPallets("XDOC-2")
                .fixInbound()
                .createOutbound(OUTBOUND_ID)
                .buildRegistry("XDOC-1", "XDOC-2")
                .sortToAvailableCell("XDOC-1", "XDOC-2")
                .prepareToShip("XDOC-1")
                .shipAndGet(OUTBOUND_ID);
        var sc = testFactory.storedSortingCenter();
        var prepared = sortableQueryService.find(sc, "XDOC-2").orElseThrow();
        var sorted = sortableQueryService.find(sc, "XDOC-1").orElseThrow();

        assertThat(prepared.getOutbound()).isNull();
        assertThat(Objects.requireNonNull(sorted.getOutbound()).getId())
                .isNotNull()
                .isEqualTo(outbound.getId());
    }

    @ParameterizedTest(name = "error: outbound имеет не валидный тип {0}")
    @MethodSource("nonValidOutboundTypes")
    void outboundHasInvalidType(OutboundType type) {
        flow.createInbound(INBOUND_ID)
                .linkPallets("XDOC-1")
                .fixInbound()
                .outboundBuilder(OUTBOUND_ID)
                .type(type)
                .toRegistryBuilder()
                .buildRegistry("XDOC-1");

        shipAndExpectErrorMsg();
    }

    private static Set<OutboundType> nonValidOutboundTypes() {
        return Arrays.stream(OutboundType.values())
                .filter(type -> XDOC != type)
                .collect(Collectors.toSet());
    }

    @ParameterizedTest(name = "Outbound валидируется успешно со статусом {0}")
    @MethodSource("validOutboundStatuses")
    void outboundHasValidStatus(OutboundStatus outboundStatus) {
        flow.createInbound(INBOUND_ID)
                .linkPallets("XDOC-1")
                .fixInbound()
                .outboundBuilder(OUTBOUND_ID)
                .status(outboundStatus)
                .toRegistryBuilder()
                .buildRegistry("XDOC-1")
                .sortToAvailableCell("XDOC-1")
                .prepareToShip("XDOC-1")
                .shipAndGet(OUTBOUND_ID);
    }

    private static Set<OutboundStatus> validOutboundStatuses() {
        return Set.of(OutboundStatus.CREATED, OutboundStatus.ASSEMBLED);
    }

    @ParameterizedTest(name = "error: outbound имеет не валидный статус {0}")
    @MethodSource("nonValidOutboundStatuses")
    void outboundIsNotAValid(OutboundStatus outboundStatus) {
        flow.createInbound(INBOUND_ID)
                .linkPallets("XDOC-1")
                .fixInbound()
                .outboundBuilder(OUTBOUND_ID)
                .status(outboundStatus)
                .toRegistryBuilder()
                .buildRegistry("XDOC-1");

        shipAndExpectErrorMsg();
    }

    private static Set<OutboundStatus> nonValidOutboundStatuses() {
        var valid = validOutboundStatuses();
        return Arrays.stream(OutboundStatus.values())
                .filter(status -> !valid.contains(status))
                .collect(Collectors.toSet());
    }

    @DisplayName("error: за outbound не закреплен плановый реестр")
    @Test
    void absenceOfPlannedRegistryForOutbound() {
        flow.createInbound(INBOUND_ID)
                .linkPallets("XDOC-1")
                .fixInbound()
                .createOutbound(OUTBOUND_ID);

        shipAndExpectErrorMsg();
    }

    @DisplayName("error: один из сортабл входящий в outbound не прикреплен к inbound")
    @Test
    void sortableDoesNotHaveInbound() {
        flow.createInbound(INBOUND_ID)
                .linkPallets("XDOC-1", "XDOC-2")
                .fixInbound()
                .createOutbound(OUTBOUND_ID)
                .buildRegistry("XDOC-1", "XDOC-2")
                .sortToAvailableCell("XDOC-1", "XDOC-2")
                .prepareToShip("XDOC-1", "XDOC-2");

        var sc = testFactory.getSortingCenterById(TestFactory.SC_ID);
        var forUpdate = sortableQueryService.findOrThrow(sc, "XDOC-2");
        forUpdate.setMutableState(forUpdate.getMutableState()
                .withInbound(null));
        sortableRepository.save(forUpdate);

        shipAndExpectErrorMsg();
    }

    @DisplayName("error: outbound не содерижит ни один сортабл")
    @Test
    void outboundContainsZeroSortables() {
        flow.createOutbound(OUTBOUND_ID)
                .buildRegistry();

        shipAndExpectErrorMsg();
    }

    @DisplayName("Все Sortable однго inbound относятся к outbound, но не находятся в статусе готовности," +
                 " такой outbound можно отгружать")
    @Test
    void outboundContainsNotPreparedSortables() {
        flow.createInbound(INBOUND_ID)
                .linkPallets("XDOC-1")
                .fixInbound()
                .createInbound("inbound-2")
                .linkPallets("XDOC-2")
                .fixInbound()
                .createOutbound(OUTBOUND_ID)
                .buildRegistry("XDOC-1", "XDOC-2")
                .sortToAvailableCell("XDOC-1")
                .prepareToShip("XDOC-1")
                .shipAndGet(OUTBOUND_ID);
    }

    @DisplayName("error: inbound не полноятью готова к отгрузке")
    @Test
    void outboundContainsPreparedAndNotPreparedSortables() {
        flow.createInbound(INBOUND_ID)
                .linkPallets("XDOC-1", "XDOC-2")
                .fixInbound()
                .createOutbound(OUTBOUND_ID)
                .buildRegistry("XDOC-1", "XDOC-2")
                .sortToAvailableCell("XDOC-1")
                .prepareToShip("XDOC-1");

        shipAndExpectErrorMsg();
    }

    @DisplayName("error: не все сортабл в относятся к xDoc типам")
    @Test
    void outboundContainsNonXDocSortable() {
        flow.createInbound(INBOUND_ID)
                .linkPallets("XDOC-1")
                .fixInbound()
                .createOutbound(OUTBOUND_ID)
                .buildRegistry("XDOC-1")
                .sortToAvailableCell("XDOC-1")
                .prepareToShip("XDOC-1");

        var sc = testFactory.getSortingCenterById(TestFactory.SC_ID);
        var forUpdate = sortableQueryService.findOrThrow(sc, "XDOC-1");
        forUpdate.setType(SortableType.PALLET);
        sortableRepository.save(forUpdate);

        shipAndExpectServerErrorMsg();
    }

    @DisplayName("error: не все части inbound были перенесены в outbound")
    @Test
    void inboundIsNotFullyLoadedToOutbound() {
        flow.createInbound(INBOUND_ID)
                .linkPallets("XDOC-1", "XDOC-2")
                .fixInbound()
                .createOutbound(OUTBOUND_ID)
                .buildRegistry("XDOC-1")
                .sortToAvailableCell("XDOC-1")
                .prepareToShip("XDOC-1");

        shipAndExpectErrorMsg();
    }

    @DisplayName("error: не все части inbound были перенесены в outbound (множество inbound)")
    @Test
    void oneOfInboundsIsNotFullyLoadedToOutbound() {
        flow.createInbound(INBOUND_ID)
                .linkPallets("XDOC-1", "XDOC-2")
                .fixInbound()
                .createInbound("inbound-2")
                .linkPallets("XDOC-3", "XDOC-4")
                .fixInbound()
                .createOutbound(OUTBOUND_ID)
                .buildRegistry("XDOC-1", "XDOC-2", "XDOC-3")
                .sortToAvailableCell("XDOC-1", "XDOC-2", "XDOC-3")
                .prepareToShip("XDOC-1", "XDOC-2", "XDOC-3");

        shipAndExpectErrorMsg();
    }

    @DisplayName("error: только палеты можно отгрузить, box отгружать нельзя")
    @Test
    void onlyPalletsIsValidForOutbound() {
        flow.createInbound(INBOUND_ID)
                .linkBoxes("XDOC-1")
                .fixInbound()
                .createOutbound(OUTBOUND_ID)
                .addRegistryBoxes("XDOC-1").buildRegistry();

        // нормальное xdoc flow не предусматривает переход коробки на отгрузку
        var sc = testFactory.getSortingCenterById(TestFactory.SC_ID);
        var forUpdate = sortableQueryService.findOrThrow(sc, "XDOC-1");
        forUpdate.setMutableState(forUpdate.getMutableState()
                .withStatus(SortableStatus.PREPARED_DIRECT)
        );
        sortableRepository.save(forUpdate);

        shipAndExpectErrorMsg();
    }

    @DisplayName("После отгрузки консолидированной палеты (XDOC_BASKET) коробки входящие в её состав перейдут в " +
                 "статус SHIPPED_DIRECT (все части outbound лежат в реестре)")
    @Test
    void boxesWillChangeTheirStatusAfterTheirPalletIsShipped() {
        var sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(sortingCenter, XDOC_ENABLED, true);
        var samaraWH = testFactory.storedWarehouse("samara-warehouse");

        flow.inboundBuilder(INBOUND_ID).nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkBoxes("XDOC-1", "XDOC-2", "XDOC-3")
                .fixInbound();

        var cellResponse = caller.createCell(
                        CellRequestDto.builder()
                                .number("samara-keep")
                                .type(CellType.BUFFER)
                                .subType(CellSubType.BUFFER_XDOC)
                                .status(CellStatus.ACTIVE)
                                .warehouseYandexId(samaraWH.getYandexId())
                                .build()
                ).andExpect(status().isOk())
                .getResponseAsClass(PartnerCellDtoWrapper.class);

        var lotResponse = caller.createLot(new PartnerLotRequestDto(cellResponse.getCell().getId(), 1))
                .andExpect(status().isOk())
                .getResponseAsClass(PartnerLotDtoWrapper.class);
        var lotDto = lotResponse.getLots().get(0);

        var user = testFactory.findUserByUid(TestFactory.USER_UID_LONG);
        sortableTestFactory.lotSort("XDOC-1", lotDto.getExternalId(), user);
        sortableTestFactory.lotSort("XDOC-2", lotDto.getExternalId(), user);
        sortableTestFactory.lotSort("XDOC-3", lotDto.getExternalId(), user);

        var lot = lotRepository.findByIdOrThrow(lotDto.getId());
        var basket = sortableRepository.findByIdOrThrow(lot.getSortableId());

        // упаковка палеты
        scanService.prepareToShipSortable(
                basket.getId(),
                SortableType.XDOC_BASKET,
                SortableAPIAction.READY_FOR_PACKING,
                new ScContext(user)
        );

        // создан outbound c коробками и палетами
        flow.createOutbound(OUTBOUND_ID)
                .addRegistryBoxes("XDOC-1", "XDOC-2", "XDOC-3")
                .addRegistryPallets(basket.getRequiredBarcodeOrThrow()).buildRegistry()
                .sortToAvailableCell(basket.getRequiredBarcodeOrThrow())
                .prepareToShip(basket.getRequiredBarcodeOrThrow())
                .callShip(OUTBOUND_ID, res -> res.andExpect(status().isOk()));

        var sortables = sortableQueryService.findAllHavingAnyBarcode(
                flow.getSortingCenter(),
                Set.of("XDOC-1", "XDOC-2", "XDOC-3", basket.getRequiredBarcodeOrThrow())
        );

        assertThat(sortables)
                .hasSize(4)
                .allMatch(s -> s.getMutableState().getStatus() == SortableStatus.SHIPPED_DIRECT);
    }

    @DisplayName("После отгрузки в статус SHIPPED_DIRECT перейдут только PREPARED_DIRECT sortable," +
                 " и они же попадут в фактический реестр")
    @Test
    void preparedDirectSortablesChangesToShippedRegistryConsistOnlyOfThisSortables() {
        var samaraWH = testFactory.storedWarehouse("samara-warehouse");
        flow.inboundBuilder("inbound-1").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkPallets("XDOC-p-1", "XDOC-p-2")
                .fixInbound();
        flow.inboundBuilder("inbound-2").nextLogisticPoint(samaraWH.getYandexId()).build()
                .linkPallets("XDOC-p-3", "XDOC-p-4")
                .fixInbound();

        var outbound = flow.createOutbound(OUTBOUND_ID)
                .addRegistryPallets("XDOC-p-1", "XDOC-p-2", "XDOC-p-3", "XDOC-p-4").buildRegistry()
                .sortToAvailableCell("XDOC-p-1", "XDOC-p-2", "XDOC-p-3", "XDOC-p-4")
                .and()
                .getOutbound(OUTBOUND_ID);

        List<Sortable> inCourierCell = sortableQueryService.findAllHavingAnyBarcode(
                flow.getSortingCenter(),
                Set.of("XDOC-p-1", "XDOC-p-2", "XDOC-p-3", "XDOC-p-4")
        );
        // все палеты в ячейке отгрузки
        assertThat(inCourierCell)
                .hasSize(4)
                .allMatch(s -> s.getMutableState().getStatus() == SortableStatus.SORTED_DIRECT);


        var user = testFactory.findUserByUid(TestFactory.USER_UID_LONG);
        sortableTestFactory.preship(
                inCourierCell.stream()
                        .filter(sortable -> "XDOC-p-1".equals(sortable.getRequiredBarcodeOrThrow()))
                        .findAny().orElseThrow().getId(),
                SortableType.XDOC_PALLET,
                SortableAPIAction.READY_FOR_SHIPMENT,
                user);
        sortableTestFactory.preship(
                inCourierCell.stream()
                        .filter(sortable -> "XDOC-p-2".equals(sortable.getRequiredBarcodeOrThrow()))
                        .findAny().orElseThrow().getId(),
                SortableType.XDOC_PALLET,
                SortableAPIAction.READY_FOR_SHIPMENT,
                user);

        caller.shipOutbound(OUTBOUND_ID).andExpect(status().isOk());

        assertThat(sortableQueryService.findAllHavingAnyBarcode(
                flow.getSortingCenter(),
                Set.of("XDOC-p-1", "XDOC-p-2"))
        )
                .hasSize(2)
                .allMatch(sortable -> sortable.getStatus() == SortableStatus.SHIPPED_DIRECT);

        assertThat(sortableQueryService.findAllHavingAnyBarcode(
                flow.getSortingCenter(),
                Set.of("XDOC-p-3", "XDOC-p-4"))
        )
                .hasSize(2)
                .allMatch(sortable -> sortable.getStatus() != SortableStatus.SHIPPED_DIRECT);

        var factReg = StreamEx.of(registryRepository.findAllByOutboundId(outbound.getId()))
                .filterBy(Registry::getType, RegistryType.FACTUAL)
                .findFirst()
                .orElseThrow();

        var registrySortables = registrySortableRepository.findAllByRegistryIn(Set.of(factReg));
        assertThat(registrySortables.stream().map(RegistrySortable::getSortableExternalId))
                .containsExactlyInAnyOrder("XDOC-p-1", "XDOC-p-2");
    }

    @SneakyThrows
    private void shipAndExpectErrorMsg() {
        this.caller.shipOutbound(XDocOutboundShipTest.OUTBOUND_ID)
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").exists());
    }

    @SneakyThrows
    private void shipAndExpectServerErrorMsg() {
        this.caller.shipOutbound(XDocOutboundShipTest.OUTBOUND_ID)
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").exists());
    }
}
