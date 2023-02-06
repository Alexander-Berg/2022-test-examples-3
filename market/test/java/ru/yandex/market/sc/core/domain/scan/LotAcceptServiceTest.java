package ru.yandex.market.sc.core.domain.scan;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.flow.repository.Flow;
import ru.yandex.market.sc.core.domain.flow.repository.FlowSystemName;
import ru.yandex.market.sc.core.domain.flow_operation_context.FlowOperationContextCommandService;
import ru.yandex.market.sc.core.domain.flow_operation_context.repository.FlowOperationContext;
import ru.yandex.market.sc.core.domain.flow_operation_context.repository.LotAcceptOperationContextData;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.operation.repository.Operation;
import ru.yandex.market.sc.core.domain.operation.repository.OperationSystemName;
import ru.yandex.market.sc.core.domain.operation_log.model.OperationLogResult;
import ru.yandex.market.sc.core.domain.operation_log.repository.OperationLog;
import ru.yandex.market.sc.core.domain.operation_log.repository.OperationLogRepository;
import ru.yandex.market.sc.core.domain.order.model.AcceptLotRequestDto;
import ru.yandex.market.sc.core.domain.order.model.AcceptStatusCode;
import ru.yandex.market.sc.core.domain.order.model.ApiPreAcceptedLotDto;
import ru.yandex.market.sc.core.domain.order.model.FlowTraceDto;
import ru.yandex.market.sc.core.domain.place.PlaceNonBlockingQueryService;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.process.repository.Process;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.stage.StageLoader;
import ru.yandex.market.sc.core.domain.stage.model.ApiStageDto;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.zone.repository.Zone;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.exception.ScException;
import ru.yandex.market.sc.core.exception.ScNotFoundException;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.sc.core.domain.inbound.repository.InboundStatus.CREATED;
import static ru.yandex.market.sc.core.domain.inbound.repository.InboundStatus.FIXED;
import static ru.yandex.market.sc.core.domain.inbound.repository.InboundStatus.IN_PROGRESS;
import static ru.yandex.market.sc.core.domain.scan.FlowOperationAcceptService.REVERT_SUFFIX;
import static ru.yandex.market.sc.core.domain.scan.SortableAcceptSimpleService.FINISH_SUFFIX;
import static ru.yandex.market.sc.core.domain.scan.SortableAcceptSimpleService.PREACCEPT_SUFFIX;
import static ru.yandex.market.sc.core.domain.stage.Stages.ACCEPTING_DIRECT;
import static ru.yandex.market.sc.core.domain.stage.Stages.ACCEPTING_RETURN;
import static ru.yandex.market.sc.core.domain.stage.Stages.AWAITING_DIRECT;
import static ru.yandex.market.sc.core.domain.stage.Stages.AWAITING_RETURN;
import static ru.yandex.market.sc.core.domain.stage.Stages.FINAL_ACCEPT_DIRECT;
import static ru.yandex.market.sc.core.domain.stage.Stages.FINAL_ACCEPT_RETURN;
import static ru.yandex.market.sc.core.domain.stage.Stages.FIRST_ACCEPT_DIRECT;
import static ru.yandex.market.sc.core.domain.stage.Stages.FIRST_ACCEPT_RETURN;
import static ru.yandex.market.sc.core.domain.stage.Stages.SORTED_DIRECT;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LotAcceptServiceTest {

    private static final String FLOW_TRACE_ID = "54f134dd-a458-4950-8ba7-c7890060d028";
    private static final String LOT_BARCODE = "SC_LOT_43124";

    private final LotAcceptService lotAcceptService;
    private final TestFactory testFactory;
    private final PlaceRepository placeRepository;
    private final OperationLogRepository operationLogRepository;
    private final SortableRepository sortableRepository;
    private final SortableLotService sortableLotService;
    private final PlaceNonBlockingQueryService placeNonBlockingQueryService;
    private final FlowOperationContextCommandService flowOperationContextCommandService;
    private final TransactionTemplate transactionTemplate;

    @MockBean
    private Clock clock;

    private SortingCenter sortingCenter;
    private User user;
    private Zone zone;
    private Process process;
    private Flow flow;
    private Operation operation;

    @BeforeEach
    void init() {
        TestFactory.setupMockClock(clock);
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, 456L);
        operation = testFactory.storedOperation(
                OperationSystemName.LOT_ACCEPT.name(),
                OperationSystemName.LOT_ACCEPT.name());
        flow = testFactory.storedFlow(
                FlowSystemName.LOT_INITIAL_ACCEPTANCE.name(),
                FlowSystemName.LOT_INITIAL_ACCEPTANCE.name(),
                List.of(operation));
        process = testFactory.storedProcess(
                "process_name",
                "process_name",
                List.of(flow));
        zone = testFactory.storedZone(sortingCenter, "zone-1", List.of(process));

        var flowOperation = flow.getFlowOperations().stream().findFirst().orElseThrow();


        flowOperationContextCommandService.save(
                new FlowOperationContext(flowOperation, flow, FLOW_TRACE_ID, false,
                        new LotAcceptOperationContextData())
        );
    }

    @DisplayName("Приемка лота, прямой поток")
    @Test
    void acceptLotDirect() {
        var warehouse = testFactory.storedWarehouse("234234-123");
        var inbound = testFactory.createInbound(
                TestFactory.CreateInboundParams.builder()
                        .warehouseFromExternalId(warehouse.getYandexId())
                        .inboundType(InboundType.DS_SC)
                        .sortingCenter(sortingCenter)
                        .fromDate(OffsetDateTime.now(clock))
                        .toDate(OffsetDateTime.now(clock))
                        .inboundStatus(CREATED)
                        .build()
        );
        var lotWithPlaces = createLotWithPlacesDirect(LOT_BARCODE, inbound);
        var sortableLot = lotWithPlaces.lot();

        var apiPreAcceptedLotDto = lotAcceptService.preAccept(
                new AcceptLotRequestDto(sortableLot.getBarcode()),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        inbound = testFactory.getInbound(inbound.getId());
        var places = placeRepository.findAllById(lotWithPlaces.getPlaceIds());
        var preAcceptedLot = sortableLotService.findBySortableId(sortableLot.getSortableId()).orElseThrow();

        assertThat(inbound.getInboundStatus()).isEqualTo(IN_PROGRESS);
        assertThat(places).allMatch(it -> it.getStageId() == ACCEPTING_DIRECT.getId());
        assertThat(preAcceptedLot.getStageId()).isEqualTo(ACCEPTING_DIRECT.getId());

        assertThat(apiPreAcceptedLotDto).isEqualTo(
                new ApiPreAcceptedLotDto(
                        sortableLot.getBarcode(),
                        AcceptStatusCode.OK,
                        warehouse.getIncorporation(),
                        sortingCenter.getScName(),
                        sortableLot.getLotStatus(),
                        map(ACCEPTING_DIRECT.getId()),
                        places.size(),
                        false,
                        inbound.getToDate().toLocalDate(),
                        inbound.getExternalId()
                )
        );
        assertSuccessOperationLog(PREACCEPT_SUFFIX, AWAITING_DIRECT.getId(), preAcceptedLot, places);
    }

    @DisplayName("Приемка лота прямого потока с 3 посылками: отмененной, прямого и обратного потока")
    @Test
    void preAcceptLotWithDirectReturnAndCancelledPlaces() {
        var lotWithPlaces = createDirectLotWithDirectReturnAndCancelledPlaces(LOT_BARCODE, null);
        var sortableLot = lotWithPlaces.lot();

        lotAcceptService.preAccept(
                new AcceptLotRequestDto(sortableLot.getBarcode()),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        var preAcceptedLot = sortableLotService.findBySortableId(sortableLot.getSortableId()).orElseThrow();
        assertThat(preAcceptedLot.getStageId()).isEqualTo(ACCEPTING_DIRECT.getId());

        var places = placeRepository.findAllById(lotWithPlaces.getPlaceIds());
        var placeStages = places.stream()
                .map(Place::getStageId)
                .collect(Collectors.toList());
        assertThat(placeStages).containsExactlyInAnyOrder(
                ACCEPTING_DIRECT.getId(),
                ACCEPTING_RETURN.getId(),
                ACCEPTING_RETURN.getId()
        );
        assertSuccessOperationLog(PREACCEPT_SUFFIX, AWAITING_DIRECT.getId(), preAcceptedLot, places);
    }

    @DisplayName("Откат приемки лота прямого потока с 3 посылками: отмененной, прямого и обратного потока")
    @Test
    void revertPreAcceptLotWithDirectReturnAndCancelledPlaces() {
        var lotWithPlaces = createDirectLotWithDirectReturnAndCancelledPlaces(LOT_BARCODE, null);
        var sortableLot = lotWithPlaces.lot();

        lotAcceptService.preAccept(
                new AcceptLotRequestDto(sortableLot.getBarcode()),
                flowTraceDto(PREACCEPT_SUFFIX)
        );
        lotAcceptService.revertAccept(flowTraceDto(REVERT_SUFFIX));

        sortableLot = sortableLotService.findBySortableId(sortableLot.getSortableId()).orElseThrow();
        var places = placeRepository.findAllById(lotWithPlaces.getPlaceIds());
        var placeStages = places.stream()
                .map(Place::getStageId)
                .collect(Collectors.toList());
        assertThat(placeStages).containsExactlyInAnyOrder(
                AWAITING_DIRECT.getId(),
                AWAITING_RETURN.getId(),
                AWAITING_RETURN.getId()
        );
        assertSuccessOperationLog(REVERT_SUFFIX, ACCEPTING_DIRECT.getId(), sortableLot, places);
    }

    @DisplayName("Завершение приемки лота прямого потока с 3 посылками: отмененной, прямого и обратного потока")
    @Test
    void finishAcceptLotWithDirectReturnAndCancelledPlaces() {
        var lotWithPlaces = createDirectLotWithDirectReturnAndCancelledPlaces(LOT_BARCODE, null);
        var sortableLot = lotWithPlaces.lot();

        lotAcceptService.preAccept(
                new AcceptLotRequestDto(sortableLot.getBarcode()),
                flowTraceDto(PREACCEPT_SUFFIX)
        );
        lotAcceptService.finish(null, flowTraceDto(FINISH_SUFFIX));

        sortableLot = sortableLotService.findBySortableId(sortableLot.getSortableId()).orElseThrow();
        var places = placeRepository.findAllById(lotWithPlaces.getPlaceIds());
        var placeStages = places.stream()
                .map(Place::getStageId)
                .collect(Collectors.toList());
        assertThat(placeStages).containsExactlyInAnyOrder(
                FIRST_ACCEPT_DIRECT.getId(),
                FIRST_ACCEPT_RETURN.getId(),
                FIRST_ACCEPT_RETURN.getId()
        );

        assertSuccessOperationLog(FINISH_SUFFIX, ACCEPTING_DIRECT.getId(), sortableLot, places);
    }

    @DisplayName("Приемка лота, возвратный поток")
    @Test
    void acceptLotReturn() {
        var warehouse = testFactory.storedWarehouse("234234-123");
        var inbound = testFactory.createInbound(
                TestFactory.CreateInboundParams.builder()
                        .warehouseFromExternalId(warehouse.getYandexId())
                        .inboundType(InboundType.DS_SC)
                        .sortingCenter(sortingCenter)
                        .fromDate(OffsetDateTime.now(clock))
                        .toDate(OffsetDateTime.now(clock))
                        .inboundStatus(CREATED)
                        .build()
        );
        var lotWithPlaces = createLotWithPlacesReturn(LOT_BARCODE, inbound);
        var apiPreAcceptedLotDto = lotAcceptService.preAccept(
                new AcceptLotRequestDto(lotWithPlaces.lot().getBarcode()),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        inbound = testFactory.getInbound(inbound.getId());
        var places = placeRepository.findAllById(lotWithPlaces.getPlaceIds());
        var preAcceptedLot = sortableLotService.findBySortableId(lotWithPlaces.lot().getSortableId()).orElseThrow();

        assertThat(inbound.getInboundStatus()).isEqualTo(IN_PROGRESS);
        assertThat(places).allMatch(it -> it.getStageId() == ACCEPTING_RETURN.getId());
        assertThat(preAcceptedLot.getStageId()).isEqualTo(ACCEPTING_RETURN.getId());
        assertThat(apiPreAcceptedLotDto).isEqualTo(
                new ApiPreAcceptedLotDto(
                        preAcceptedLot.getBarcode(),
                        AcceptStatusCode.OK,
                        warehouse.getIncorporation(),
                        sortingCenter.getScName(),
                        preAcceptedLot.getLotStatus(),
                        map(ACCEPTING_RETURN.getId()),
                        places.size(),
                        false,
                        inbound.getToDate().toLocalDate(),
                        inbound.getExternalId()
                )
        );
        assertSuccessOperationLog(PREACCEPT_SUFFIX, AWAITING_RETURN.getId(), preAcceptedLot, places);
    }

    @DisplayName("Приемка нескольких лотов в одной Поставке")
    @Test
    void acceptTwoLotsInSameInbound() {
        var warehouse = testFactory.storedWarehouse("234234-123");
        var inbound = testFactory.createInbound(
                TestFactory.CreateInboundParams.builder()
                        .warehouseFromExternalId(warehouse.getYandexId())
                        .inboundType(InboundType.DS_SC)
                        .sortingCenter(sortingCenter)
                        .fromDate(OffsetDateTime.now(clock))
                        .toDate(OffsetDateTime.now(clock))
                        .inboundStatus(CREATED)
                        .build()
        );
        var lotWithPlaces1 = createLotWithPlacesDirect(LOT_BARCODE + "-1", inbound);
        var lot1 = lotWithPlaces1.lot();
        var places1 = lotWithPlaces1.places();
        var lotWithPlaces2 = createLotWithPlacesDirect(LOT_BARCODE + "-2", inbound);
        var lot2 = lotWithPlaces2.lot();
        var places2 = lotWithPlaces2.places();

        var preAcceptedLotDto1 = lotAcceptService.preAccept(
                new AcceptLotRequestDto(lot1.getBarcode()),
                flowTraceDto(PREACCEPT_SUFFIX)
        );
        assertThat(preAcceptedLotDto1).isEqualTo(
                new ApiPreAcceptedLotDto(
                        lot1.getBarcode(),
                        AcceptStatusCode.OK,
                        warehouse.getIncorporation(),
                        sortingCenter.getScName(),
                        lot1.getLotStatus(),
                        map(ACCEPTING_DIRECT.getId()),
                        2,
                        false,
                        inbound.getToDate().toLocalDate(),
                        inbound.getExternalId()
                )
        );

        var preAcceptedLotDto2 = lotAcceptService.preAccept(
                new AcceptLotRequestDto(lot2.getBarcode()),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        assertThat(preAcceptedLotDto2).isEqualTo(
                new ApiPreAcceptedLotDto(
                        lot2.getBarcode(),
                        AcceptStatusCode.OK,
                        warehouse.getIncorporation(),
                        sortingCenter.getScName(),
                        lot2.getLotStatus(),
                        map(ACCEPTING_DIRECT.getId()),
                        2,
                        false,
                        inbound.getToDate().toLocalDate(),
                        inbound.getExternalId()
                )
        );

        inbound = testFactory.getInbound(inbound.getId());
        assertThat(inbound.getInboundStatus()).isEqualTo(IN_PROGRESS);
        lot1 = sortableLotService.findBySortableId(lot1.getSortableId()).orElseThrow();
        lot2 = sortableLotService.findBySortableId(lot2.getSortableId()).orElseThrow();
        assertSuccessOperationLog(PREACCEPT_SUFFIX, AWAITING_DIRECT.getId(), lot1, places1);
        assertSuccessOperationLog(PREACCEPT_SUFFIX, AWAITING_DIRECT.getId(), lot2, places2);
    }

    @DisplayName("Не удалось найти лот")
    @Test
    void canNotFindLot() {
        assertThrows(
                ScNotFoundException.class,
                () -> lotAcceptService.preAccept(
                        new AcceptLotRequestDto("ABSENT_LOT_BARCODE"),
                        flowTraceDto(PREACCEPT_SUFFIX)
                ),
                "Необходимые лоты не найдены"
        );
        assertErrorOperationLog(PREACCEPT_SUFFIX, null, ScErrorCode.LOT_CANT_FIND);
    }

    @DisplayName("Лот в неверном статусе")
    @Test
    void lotInWrongStage() {
        var sortableLot = testFactory.storedLot(
                LOT_BARCODE,
                sortingCenter,
                user,
                SORTED_DIRECT.getId(),
                true,
                null,
                null
        );

        assertThrows(
                ScException.class,
                () -> lotAcceptService.preAccept(
                        new AcceptLotRequestDto(LOT_BARCODE),
                        flowTraceDto(PREACCEPT_SUFFIX)
                ),
                "Лот в неверном статусе: Подготовлен к отгрузке, прямой поток"
        );
        sortableLot = sortableLotService.findBySortableId(sortableLot.getSortableId()).orElseThrow();
        assertErrorOperationLog(PREACCEPT_SUFFIX, sortableLot, ScErrorCode.LOT_INVALID_STATE);
    }

    @DisplayName("Посылка в лоте в неверном статусе")
    @Test
    void placeInWrongStage() {
        var lotWithPlaces = createLotWithPlacesReturn(LOT_BARCODE, null);
        var firstPlace = lotWithPlaces.places.stream().findFirst().orElseThrow();
        testFactory.acceptPlace(firstPlace);

        assertThrows(
                ScException.class,
                () -> lotAcceptService.preAccept(
                        new AcceptLotRequestDto(lotWithPlaces.lot().getBarcode()),
                        flowTraceDto(PREACCEPT_SUFFIX)
                ),
                "Коробка находится в неверном статусе: Вторая приемка завершена, прямой поток"
        );
        assertErrorOperationLog(PREACCEPT_SUFFIX, lotWithPlaces.lot(), ScErrorCode.PLACE_IN_WRONG_STATUS);
    }

    @DisplayName("Приёмка лота в случае, если Поставка в терминальном статусе -- принимаем вне Поставки")
    @Test
    void acceptLotForFixedInbound() {
        var warehouse = testFactory.storedWarehouse("234234-123");
        var inbound = testFactory.createInbound(
                TestFactory.CreateInboundParams.builder()
                        .warehouseFromExternalId(warehouse.getYandexId())
                        .inboundType(InboundType.DS_SC)
                        .sortingCenter(sortingCenter)
                        .fromDate(OffsetDateTime.now(clock))
                        .toDate(OffsetDateTime.now(clock))
                        .inboundStatus(FIXED)
                        .build()
        );
        var lotWithPlaces = createLotWithPlacesDirect(LOT_BARCODE, inbound);

        var preAcceptedLotDto = lotAcceptService.preAccept(
                new AcceptLotRequestDto(LOT_BARCODE),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        var places = placeRepository.findAllById(lotWithPlaces.getPlaceIds());
        var lot = sortableLotService.findBySortableId(lotWithPlaces.lot().getSortableId()).orElseThrow();

        assertThat(preAcceptedLotDto).isEqualTo(
                new ApiPreAcceptedLotDto(
                        lot.getBarcode(),
                        AcceptStatusCode.OK,
                        "Неизвестный поставщик",
                        sortingCenter.getScName(),
                        lot.getLotStatus(),
                        map(ACCEPTING_DIRECT.getId()),
                        2,
                        false,
                        null,
                        null
                )
        );
        assertSuccessOperationLog(PREACCEPT_SUFFIX, AWAITING_DIRECT.getId(), lot, places);
    }

    @DisplayName("Нельзя принять пустой некроссдочный лот")
    @Test
    void cantAcceptEmptyNonCrossDockLot() {
        var sortableLot = createLot(LOT_BARCODE, null, AWAITING_DIRECT.getId());

        assertThrows(ScException.class,
                () -> lotAcceptService.preAccept(
                        new AcceptLotRequestDto(sortableLot.getBarcode()),
                        flowTraceDto(PREACCEPT_SUFFIX)
                ),
                "Нельзя принять пустой лот"
        );
        assertErrorOperationLog(PREACCEPT_SUFFIX, sortableLot, ScErrorCode.CANT_ACCEPT_EMPTY_LOTS);
    }

    @DisplayName("Приемка лота без Поставки")
    @Test
    void acceptWithoutInbound() {
        var lotWithPlaces = createLotWithPlacesDirect(LOT_BARCODE, null);

        var apiPreAcceptedLotDto = lotAcceptService.preAccept(
                new AcceptLotRequestDto(lotWithPlaces.lot().getBarcode()),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        var preAcceptedLot = sortableLotService.findBySortableId(lotWithPlaces.lot().getSortableId()).orElseThrow();
        var places = placeRepository.findAllById(lotWithPlaces.getPlaceIds());

        assertThat(places).allMatch(it -> it.getStageId() == ACCEPTING_DIRECT.getId());
        assertThat(preAcceptedLot.getStageId()).isEqualTo(ACCEPTING_DIRECT.getId());

        assertThat(apiPreAcceptedLotDto).isEqualTo(
                new ApiPreAcceptedLotDto(
                        preAcceptedLot.getBarcode(),
                        AcceptStatusCode.OK,
                        "Неизвестный поставщик",
                        sortingCenter.getScName(),
                        preAcceptedLot.getLotStatus(),
                        map(ACCEPTING_DIRECT.getId()),
                        places.size(),
                        false,
                        null,
                        null
                )
        );
    }

    @DisplayName("Лот уже принят другим кладовщиком")
    @Test
    void alreadyAccepted() {
        var lotWithPlaces = createLotWithPlacesDirect(LOT_BARCODE, null);

        lotAcceptService.preAccept(
                new AcceptLotRequestDto(lotWithPlaces.lot().getBarcode()),
                flowTraceDto(PREACCEPT_SUFFIX, user)
        );

        var preAcceptedLotDto = lotAcceptService.preAccept(
                new AcceptLotRequestDto(lotWithPlaces.lot().getBarcode()),
                flowTraceDto(PREACCEPT_SUFFIX, user)
        );

        //noinspection ConstantConditions
        assertThat(preAcceptedLotDto).isEqualToIgnoringNullFields(
                new ApiPreAcceptedLotDto(lotWithPlaces.lot().getBarcode(),
                        AcceptStatusCode.ALREADY_ACCEPTED,
                        null,
                        user.getName())
        );

        var anotherUser = testFactory.storedUser(sortingCenter, 123903L);
        var preAcceptedLotDtoByAnotherUser = lotAcceptService.preAccept(
                new AcceptLotRequestDto(lotWithPlaces.lot().getBarcode()),
                flowTraceDto(PREACCEPT_SUFFIX, anotherUser)
        );

        //noinspection ConstantConditions
        assertThat(preAcceptedLotDtoByAnotherUser).isEqualToIgnoringNullFields(
                new ApiPreAcceptedLotDto(lotWithPlaces.lot().getBarcode(),
                        AcceptStatusCode.ALREADY_ACCEPTED,
                        null,
                        user.getName())
        );
        var sortableLot = sortableLotService.findBySortableId(lotWithPlaces.lot().getSortableId()).orElseThrow();
        assertErrorOperationLog(PREACCEPT_SUFFIX, sortableLot, ScErrorCode.SORTABLE_ALREADY_ACCEPTED);
    }

    @DisplayName("Приемка кроссдок-лота")
    @Test
    void preAcceptCrossDockLot() {
        var warehouse = testFactory.storedWarehouse("best-warehouse");
        var sortingCenterTo = testFactory.storedSortingCenter(62461245213L);
        var courierWithDs = testFactory.magistralCourier(String.valueOf(sortingCenterTo.getId()));
        var cell = testFactory.storedMagistralCell(sortingCenter, courierWithDs.courier().getId());
        testFactory.storedOutgoingCourierRoute(LocalDate.now(clock), sortingCenter, courierWithDs.courier(), cell);
        var inbound = testFactory.createInbound(
                TestFactory.CreateInboundParams.builder()
                        .warehouseFromExternalId(warehouse.getYandexId())
                        .inboundType(InboundType.DS_SC)
                        .sortingCenter(sortingCenter)
                        .fromDate(OffsetDateTime.now(clock))
                        .toDate(OffsetDateTime.now(clock))
                        .inboundStatus(CREATED)
                        .build()
        );
        var sortableLot = testFactory.storedLot(
                LOT_BARCODE,
                sortingCenter,
                user,
                AWAITING_DIRECT.getId(),
                true,
                inbound, cell);

        var apiPreAcceptedLotDto = lotAcceptService.preAccept(
                new AcceptLotRequestDto(sortableLot.getBarcode()),
                flowTraceDto(PREACCEPT_SUFFIX)
        );
        inbound = testFactory.getInbound(inbound.getId());
        var preAcceptedLot = sortableLotService.findBySortableId(sortableLot.getSortableId()).orElseThrow();

        assertThat(inbound.getInboundStatus()).isEqualTo(IN_PROGRESS);
        assertThat(preAcceptedLot.getStageId()).isEqualTo(ACCEPTING_DIRECT.getId());
        assertThat(apiPreAcceptedLotDto).isEqualTo(
                new ApiPreAcceptedLotDto(
                        sortableLot.getBarcode(),
                        AcceptStatusCode.OK,
                        warehouse.getIncorporation(),
                        sortingCenterTo.getScName(),
                        sortableLot.getLotStatus(),
                        map(ACCEPTING_DIRECT.getId()),
                        0,
                        true,
                        inbound.getToDate().toLocalDate(),
                        inbound.getExternalId()
                )
        );
        assertSuccessOperationLog(PREACCEPT_SUFFIX, AWAITING_DIRECT.getId(), preAcceptedLot, emptyList());
    }

    @DisplayName("Завершение приёмки лота прямого потока")
    @Test
    void finishLotAcceptLotDirect() {
        var lotWithPlaces = createLotWithPlacesDirect(LOT_BARCODE, null);

        lotAcceptService.preAccept(
                new AcceptLotRequestDto(lotWithPlaces.lot().getBarcode()),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        lotAcceptService.finish(null, flowTraceDto(FINISH_SUFFIX));

        var places = placeRepository.findAllById(lotWithPlaces.getPlaceIds());
        assertThat(places).allMatch(it -> it.getStageId().equals(FIRST_ACCEPT_DIRECT.getId()));

        var sortableLot = sortableLotService.findBySortableId(lotWithPlaces.lot().getSortableId()).orElseThrow();
        assertThat(sortableLot.getStageId()).isEqualTo(FINAL_ACCEPT_DIRECT.getId());

        assertSuccessOperationLog(FINISH_SUFFIX, ACCEPTING_DIRECT.getId(), sortableLot, places);
    }

    @DisplayName("Завершение приёмки лота обратного потока")
    @Test
    void finishAcceptLotReturn() {
        var sortableLot = createLot(LOT_BARCODE, null, AWAITING_RETURN.getId());
        var places = testFactory.createForToday(
                order(sortingCenter).externalId("1").places("1-0", "1-1").build()
        ).accept().sort().ship().cancel().makeReturn().getPlaces().values();

        placeRepository.saveAll(
                places.stream()
                        .peek(it -> it.setLot(sortableLot, false, user))
                        .collect(Collectors.toList())
        );

        lotAcceptService.preAccept(
                new AcceptLotRequestDto(sortableLot.getBarcode()),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        lotAcceptService.finish(null, flowTraceDto(FINISH_SUFFIX));

        places = placeNonBlockingQueryService.findPlacesInLot(sortableLot);
        assertThat(places).allMatch(it -> it.getStageId().equals(FIRST_ACCEPT_RETURN.getId()));

        var sortableLotFinished = sortableLotService.findBySortableId(sortableLot.getSortableId()).orElseThrow();
        assertThat(sortableLotFinished.getStageId()).isEqualTo(FINAL_ACCEPT_RETURN.getId());

        assertSuccessOperationLog(FINISH_SUFFIX, ACCEPTING_RETURN.getId(), sortableLotFinished, places);
    }

    @DisplayName("Завершение приёмки для нескольких лотов из разных Поставок")
    @Test
    void finishAcceptForTwoLotsFromDifferentInbound() {
        var inbound1 = testFactory.createInbound(
                TestFactory.CreateInboundParams.builder()
                        .inboundType(InboundType.DS_SC)
                        .sortingCenter(sortingCenter)
                        .fromDate(OffsetDateTime.now(clock))
                        .toDate(OffsetDateTime.now(clock))
                        .inboundStatus(CREATED)
                        .registryMap(Map.of("123-1", List.of()))
                        .build()
        );
        var inbound2 = testFactory.createInbound(
                TestFactory.CreateInboundParams.builder()
                        .inboundType(InboundType.DS_SC)
                        .sortingCenter(sortingCenter)
                        .fromDate(OffsetDateTime.now(clock))
                        .toDate(OffsetDateTime.now(clock))
                        .inboundStatus(CREATED)
                        .registryMap(Map.of("123-2", List.of()))
                        .build()
        );
        var lotWithPlaces1 = createLotWithPlacesDirect(LOT_BARCODE + "-1", inbound1);
        var lot1 = lotWithPlaces1.lot();
        var lotWithPlaces2 = createLotWithPlacesDirect(LOT_BARCODE + "-2", inbound2);
        var lot2 = lotWithPlaces2.lot();

        lotAcceptService.preAccept(new AcceptLotRequestDto(lot1.getBarcode()), flowTraceDto(PREACCEPT_SUFFIX));
        lotAcceptService.preAccept(new AcceptLotRequestDto(lot2.getBarcode()), flowTraceDto(PREACCEPT_SUFFIX));

        lotAcceptService.finish(null, flowTraceDto(FINISH_SUFFIX));

        lot1 = sortableLotService.findBySortableId(lot1.getSortableId()).orElseThrow();
        lot2 = sortableLotService.findBySortableId(lot2.getSortableId()).orElseThrow();

        var places1 = placeRepository.findAllById(lotWithPlaces1.getPlaceIds());
        var places2 = placeRepository.findAllById(lotWithPlaces2.getPlaceIds());

        assertThat(lot1.getStageId()).isEqualTo(FINAL_ACCEPT_DIRECT.getId());
        assertThat(lot2.getStageId()).isEqualTo(FINAL_ACCEPT_DIRECT.getId());
        assertThat(places1).allMatch(it -> it.getStageId().equals(FIRST_ACCEPT_DIRECT.getId()));
        assertThat(places2).allMatch(it -> it.getStageId().equals(FIRST_ACCEPT_DIRECT.getId()));

        assertSuccessOperationLog(FINISH_SUFFIX, ACCEPTING_DIRECT.getId(), lot1, places1);
        assertSuccessOperationLog(FINISH_SUFFIX, ACCEPTING_DIRECT.getId(), lot2, places2);
    }

    @DisplayName("Завершение приёмки кроссдок-лота прямого потока")
    @Test
    void finishCrossDockLotAcceptLotDirect() {
        var sortingCenterTo = testFactory.storedSortingCenter(834563L);
        var courierWithDs = testFactory.magistralCourier(String.valueOf(sortingCenterTo.getId()));
        var cell = testFactory.storedMagistralCell(sortingCenter, courierWithDs.courier().getId());
        var sortableLot = testFactory.storedLot(
                LOT_BARCODE,
                sortingCenter,
                user,
                AWAITING_DIRECT.getId(),
                true,
                null, cell);

        lotAcceptService.preAccept(
                new AcceptLotRequestDto(sortableLot.getBarcode()),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        lotAcceptService.finish(null, flowTraceDto(FINISH_SUFFIX));

        sortableLot = testFactory.getLot(sortableLot.getSortable());

        assertThat(sortableLot.getStageId()).isEqualTo(SORTED_DIRECT.getId());
        assertThat(sortableLot.getLotStatus()).isEqualTo(LotStatus.READY);

        assertSuccessOperationLog(FINISH_SUFFIX, ACCEPTING_DIRECT.getId(), sortableLot, emptyList());
    }

    @DisplayName("Лот в неверном статусе при завершении приемки")
    @Test
    void finishAcceptLotInWrongStage() {
        var lot = createLotWithPlacesDirect(LOT_BARCODE, null).lot();

        lotAcceptService.preAccept(
                new AcceptLotRequestDto(lot.getBarcode()),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        var sortableId = lot.getSortableId();
        // Кто-то как-то поменял статус лота после /preaccept извне флоу
        transactionTemplate.execute(ts -> {
            var sortable = sortableRepository.findById(sortableId).orElseThrow();
            sortable.setStage(StageLoader.getById(SORTED_DIRECT.getId()), user);
            sortableRepository.save(sortable);

            return null;
        });

        assertThrows(ScException.class,
                () -> lotAcceptService.finish(null, flowTraceDto(FINISH_SUFFIX)),
                "Лот в неверном статусе: Подготовлен к отгрузке, прямой поток");

        lot = sortableLotService.findBySortableId(lot.getSortableId()).orElseThrow();
        assertErrorOperationLog(FINISH_SUFFIX, lot, ScErrorCode.LOT_INVALID_STATE);
    }

    @DisplayName("Сканируем лот, потом извне меняем стейдж посылки. Завершаем флоу -- стейдж плейса не изменился")
    @Test
    void finishAcceptPlaceInWrongStage() {
        var sortableLot = createLotWithPlacesDirect(LOT_BARCODE, null).lot();

        lotAcceptService.preAccept(
                new AcceptLotRequestDto(sortableLot.getBarcode()),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        var places = placeNonBlockingQueryService.findPlacesInLot(sortableLot);
        var placeToBeChanged = places.get(0);

        // Кто-то взял и прошелся старой первичной приемкой по посылке
        testFactory.acceptPlace(places.get(0));

        lotAcceptService.finish(null, flowTraceDto(FINISH_SUFFIX));

        var anotherPlaceIds = places.stream()
                .map(Place::getId)
                .filter(it -> !it.equals(placeToBeChanged.getId()))
                .collect(Collectors.toList());

        var placeChanged = placeRepository.findById(placeToBeChanged.getId()).orElseThrow();
        var anotherPlaces = placeRepository.findAllById(anotherPlaceIds);

        assertThat(placeChanged.getStageId()).isEqualTo(FINAL_ACCEPT_DIRECT.getId());
        assertThat(anotherPlaces).allMatch(it -> it.getStageId().equals(FIRST_ACCEPT_DIRECT.getId()));

        sortableLot = sortableLotService.findBySortableId(sortableLot.getSortableId()).orElseThrow();
        assertSuccessOperationLog(FINISH_SUFFIX, ACCEPTING_DIRECT.getId(), sortableLot, anotherPlaces);
    }

    @DisplayName("Откат приемки лота прямого потока")
    @Test
    void revertAcceptLotDirect() {
        var sortableLot = createLotWithPlacesDirect(LOT_BARCODE, null).lot();

        lotAcceptService.preAccept(
                new AcceptLotRequestDto(sortableLot.getBarcode()),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        lotAcceptService.revertAccept(flowTraceDto(REVERT_SUFFIX));

        sortableLot = sortableLotService.findBySortableId(sortableLot.getSortableId()).orElseThrow();
        var places = placeNonBlockingQueryService.findPlacesInLot(sortableLot);
        assertThat(sortableLot.getStageId()).isEqualTo(AWAITING_DIRECT.getId());
        assertThat(places).allMatch(it -> it.getStageId().equals(AWAITING_DIRECT.getId()));

        assertSuccessOperationLog(REVERT_SUFFIX, ACCEPTING_DIRECT.getId(), sortableLot, places);
    }

    @DisplayName("Откат приемки лота обратного потока потока")
    @Test
    void revertAcceptLotReturn() {
        var sortableLot = createLotWithPlacesReturn(LOT_BARCODE, null).lot();

        lotAcceptService.preAccept(
                new AcceptLotRequestDto(sortableLot.getBarcode()),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        lotAcceptService.revertAccept(flowTraceDto(REVERT_SUFFIX));

        sortableLot = sortableLotService.findBySortableId(sortableLot.getSortableId()).orElseThrow();
        var places = placeNonBlockingQueryService.findPlacesInLot(sortableLot);
        assertThat(sortableLot.getStageId()).isEqualTo(AWAITING_RETURN.getId());
        assertThat(places).allMatch(it -> it.getStageId().equals(AWAITING_RETURN.getId()));

        assertSuccessOperationLog(REVERT_SUFFIX, ACCEPTING_RETURN.getId(), sortableLot, places);
    }

    @DisplayName("Откат приемки кросс-док лота")
    @Test
    void revertAcceptCrossDockLot() {
        var sortableLot = testFactory.storedLot(
                LOT_BARCODE,
                sortingCenter,
                user,
                AWAITING_DIRECT.getId(),
                true,
                null,
                null
        );

        lotAcceptService.preAccept(
                new AcceptLotRequestDto(sortableLot.getBarcode()),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        lotAcceptService.revertAccept(flowTraceDto(REVERT_SUFFIX));

        sortableLot = sortableLotService.findBySortableId(sortableLot.getSortableId()).orElseThrow();
        assertThat(sortableLot.getStageId()).isEqualTo(AWAITING_DIRECT.getId());

        assertSuccessOperationLog(REVERT_SUFFIX, ACCEPTING_DIRECT.getId(), sortableLot, emptyList());
    }

    @DisplayName("Откат, когда еще ничего не приняли")
    @Test
    void revertAcceptEmpty() {
        lotAcceptService.revertAccept(flowTraceDto(REVERT_SUFFIX));
        assertThat(operationLogRepository.findAll()).isNotEmpty();
    }

    private FlowTraceDto flowTraceDto(String suffix) {
        return flowTraceDto(suffix, this.user);
    }

    private FlowTraceDto flowTraceDto(String suffix, User user) {
        return new FlowTraceDto(
                zone,
                sortingCenter,
                user,
                process.getSystemName(),
                FlowSystemName.valueOf(flow.getSystemName()),
                OperationSystemName.valueOf(operation.getSystemName()),
                FLOW_TRACE_ID,
                suffix
        );
    }

    private LotWithPlaces createLotWithPlacesDirect(String barcode, @Nullable Inbound inbound) {
        var lot = createLot(barcode, inbound, AWAITING_DIRECT.getId());
        var places = testFactory.createForToday(
                order(sortingCenter)
                        .externalId("order-1-in-lot-" + lot.getBarcode())
                        .places("place-1-0-in-lot-" + lot.getBarcode(), "place-1-1-in-lot-" + lot.getBarcode())
                        .build()
        ).getPlaces().values();

        places = placeRepository.saveAll(
                places.stream()
                        .peek(it -> it.setLot(lot, false, user))
                        .collect(Collectors.toList())
        );

        return new LotWithPlaces(lot, places);
    }

    private LotWithPlaces createDirectLotWithDirectReturnAndCancelledPlaces(String barcode, @Nullable Inbound inbound) {
        var sortableLot = createLot(barcode, inbound, AWAITING_DIRECT.getId());

        var placeDirect = testFactory.createForToday(TestFactory.CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .externalId("o-1")
                        .build())
                .getPlace();
        var placeReturn = testFactory.createForToday(TestFactory.CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .externalId("o-2")
                        .build())
                .cancel().getPlace();
        var placeCancelled = testFactory.createForToday(TestFactory.CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .externalId("o-3")
                        .build())
                .cancel().makeReturn().getPlace();

        var places = placeRepository.saveAll(
                StreamEx.of(placeDirect, placeReturn, placeCancelled)
                        .peek(it -> it.setLot(sortableLot, false, user))
                        .collect(Collectors.toList())
        );

        return new LotWithPlaces(sortableLot, places);
    }

    private LotWithPlaces createLotWithPlacesReturn(String barcode, @Nullable Inbound inbound) {
        var sortableLot = createLot(barcode, inbound, AWAITING_RETURN.getId());
        var places = testFactory.createForToday(
                order(sortingCenter).externalId("1").places("1-0", "1-1").build()
        ).accept().sort().ship().cancel().makeReturn().getPlaces().values();

        places = placeRepository.saveAll(
                places.stream()
                        .peek(it -> it.setLot(sortableLot, false, user))
                        .collect(Collectors.toList())
        );

        return new LotWithPlaces(sortableLot, places);
    }

    private SortableLot createLot(String barcode, @Nullable Inbound inbound, Long stageId) {
        return testFactory.storedLot(
                barcode,
                sortingCenter,
                user,
                stageId,
                false,
                inbound,
                null
        );
    }

    private void assertSuccessOperationLog(String suffix, Long stageBefore, SortableLot lot, Collection<Place> places) {
        var operationLogs = operationLogRepository.findAll();
        var placeOperationLogsExpected = places.stream()
                .map(place -> new OperationLog(
                        sortingCenter,
                        user,
                        zone.getId(),
                        null,
                        process.getId(),
                        flow.getId(),
                        operation.getId(),
                        OperationLogResult.OK,
                        FLOW_TRACE_ID,
                        suffix,
                        null,
                        null,
                        place.getId(),
                        null,
                        SortableType.PLACE.name(),
                        place.getMainPartnerCode(),
                        null,
                        null,
                        stageBefore,
                        lot.getStageId(),
                        null)
                )
                .collect(Collectors.toList());
        var lotOperationLogExpected = new OperationLog(
                sortingCenter,
                user,
                zone.getId(),
                null,
                process.getId(),
                flow.getId(),
                operation.getId(),
                OperationLogResult.OK,
                FLOW_TRACE_ID,
                suffix,
                null,
                null,
                null,
                lot.getSortableId(),
                SortableType.PALLET.name(),
                lot.getBarcode(),
                null,
                null,
                stageBefore,
                lot.getStageId(),
                null
        );

        assertThat(operationLogs)
                .usingElementComparatorIgnoringFields("id", "createdAt", "updatedAt", "fixedAt")
                .containsAll(
                        StreamEx.of(placeOperationLogsExpected)
                                .append(lotOperationLogExpected)
                                .collect(Collectors.toList())
                );
    }

    private void assertErrorOperationLog(String suffix, @Nullable SortableLot lot, ScErrorCode errorCode) {
        var operationLogs = operationLogRepository.findAll();

        assertThat(operationLogs)
                .usingElementComparatorIgnoringFields("id", "createdAt", "updatedAt", "fixedAt")
                .contains(
                        new OperationLog(
                                sortingCenter,
                                user,
                                zone.getId(),
                                null,
                                process.getId(),
                                flow.getId(),
                                operation.getId(),
                                OperationLogResult.ERROR,
                                FLOW_TRACE_ID,
                                suffix,
                                null,
                                errorCode,
                                null,
                                Optional.ofNullable(lot).map(SortableLot::getSortableId).orElse(null),
                                SortableType.PALLET.name(),
                                Optional.ofNullable(lot).map(SortableLot::getBarcode).orElse(null),
                                null,
                                null,
                                Optional.ofNullable(lot).map(SortableLot::getStageId).orElse(null),
                                Optional.ofNullable(lot).map(SortableLot::getStageId).orElse(null),
                                null)
                );
    }

    private record LotWithPlaces(SortableLot lot, Collection<Place> places) {
        Collection<Long> getPlaceIds() {
            return places.stream()
                    .map(Place::getId)
                    .collect(Collectors.toSet());
        }
    }

    private static ApiStageDto map(long stageId) {
        var stage = StageLoader.getById(stageId);

        return new ApiStageDto(stage.getSystemName(), stage.getDisplayName());
    }
}
