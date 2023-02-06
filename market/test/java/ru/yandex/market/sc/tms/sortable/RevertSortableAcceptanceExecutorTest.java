package ru.yandex.market.sc.tms.sortable;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.sc.core.domain.flow.repository.Flow;
import ru.yandex.market.sc.core.domain.flow.repository.FlowSystemName;
import ru.yandex.market.sc.core.domain.flow_operation_context.FlowOperationContextCommandService;
import ru.yandex.market.sc.core.domain.flow_operation_context.repository.FlowOperationContext;
import ru.yandex.market.sc.core.domain.flow_operation_context.repository.FlowOperationContextData;
import ru.yandex.market.sc.core.domain.flow_operation_context.repository.LotAcceptOperationContextData;
import ru.yandex.market.sc.core.domain.flow_operation_context.repository.SortableAcceptSimpleFlowOperationContextData;
import ru.yandex.market.sc.core.domain.operation.repository.Operation;
import ru.yandex.market.sc.core.domain.operation.repository.OperationSystemName;
import ru.yandex.market.sc.core.domain.order.model.AcceptLotRequestDto;
import ru.yandex.market.sc.core.domain.order.model.AcceptSortableRequestDto;
import ru.yandex.market.sc.core.domain.order.model.FlowTraceDto;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.process.repository.Process;
import ru.yandex.market.sc.core.domain.scan.LotAcceptService;
import ru.yandex.market.sc.core.domain.scan.SortableAcceptSimpleService;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.zone.repository.Zone;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.tms.domain.sortable.RevertSortableAcceptanceExecutor;
import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.domain.stage.Stages.ACCEPTING_DIRECT;
import static ru.yandex.market.sc.core.domain.stage.Stages.ACCEPTING_RETURN;
import static ru.yandex.market.sc.core.domain.stage.Stages.AWAITING_DIRECT;
import static ru.yandex.market.sc.core.domain.stage.Stages.CANCELLED;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTmsTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RevertSortableAcceptanceExecutorTest {

    private static final String FLOW_TRACE_ID = "74047fa9-21b8-4320-b244-08b228305b13";

    private final RevertSortableAcceptanceExecutor revertSortableAcceptanceExecutor;
    private final TestFactory testFactory;
    private final SortableAcceptSimpleService sortableAcceptSimpleService;
    private final LotAcceptService lotAcceptService;
    private final SortableLotService sortableLotService;
    private final PlaceRepository placeRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final FlowOperationContextCommandService flowOperationContextCommandService;

    private SortingCenter sortingCenter;
    private User user;
    private Zone zone;
    private Process process;
    private Flow flow;
    private Operation operation;
    private FlowTraceDto flowTraceDto;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, 123L);
        zone = testFactory.storedZone(sortingCenter, "zone-1", Collections.emptyList());
    }

    private void init(FlowSystemName flowName, OperationSystemName operationName, FlowOperationContextData context) {
        var configData = testFactory.toJsonNode("{\"validateMerchant\": true, \"processCancelled\": true}");
        operation = testFactory.storedOperation(operationName.name(), operationName.name());
        flow = testFactory.storedFlow(flowName.name(), flowName.name(), Map.of(operation, configData));
        process = testFactory.storedProcess("process_name", "process_name", List.of(flow));
        flowTraceDto = new FlowTraceDto(
                zone,
                sortingCenter,
                user,
                "process_name",
                flowName,
                operationName,
                FLOW_TRACE_ID,
                "suffix"
        );

        var flowOperation = flow.getFlowOperations().stream().findFirst().orElseThrow();

        flowOperationContextCommandService.save(
                new FlowOperationContext(flowOperation, flow, FLOW_TRACE_ID, false, context)
        );
    }

    @Test
    void testRevertPlaceAcceptance() {
        init(FlowSystemName.MERCHANT_INITIAL_ACCEPTANCE, OperationSystemName.SORTABLE_ACCEPT_SIMPLE,
                new SortableAcceptSimpleFlowOperationContextData());
        var firstPartPlaces = testFactory.createForToday(
                order(sortingCenter).externalId("1").places("1-1", "1-2", "1-3").build()
        ).getPlacesList();

        var firstPartCancelledPlaces = testFactory.createForToday(
                order(sortingCenter).externalId("3").places("3-1", "3-2", "3-3").build()
        ).cancel().getPlacesList();

        Stream.concat(firstPartPlaces.stream(), firstPartCancelledPlaces.stream()).forEach(
                place -> sortableAcceptSimpleService.preAccept(
                        new AcceptSortableRequestDto(
                                List.of(place.getMainPartnerCode()),
                                place.getWarehouseFrom().getId(),
                                true
                        ),
                        flowTraceDto
                )
        );

        List<Long> firstPlaceIds = firstPartPlaces.stream().map(Place::getId).toList();

        //приняли первую партию плейсов
        var firstPartPlaceIds = firstPartPlaces.stream().map(Place::getId).collect(Collectors.toList());
        firstPartPlaces = placeRepository.findAllById(firstPartPlaceIds);
        assertThat(firstPartPlaces).allMatch(it -> it.getStageId().equals(ACCEPTING_DIRECT.getId()));

        var firstPartCancelledPlaceIds =
                firstPartCancelledPlaces.stream().map(Place::getId).collect(Collectors.toList());
        firstPartCancelledPlaces = placeRepository.findAllById(firstPartCancelledPlaceIds);
        assertThat(firstPartCancelledPlaces).allMatch(it -> it.getStageId().equals(ACCEPTING_RETURN.getId()));

        //прошло 12 часов
        changePlaceHistoryTime(firstPlaceIds);
        changePlaceHistoryTime(firstPartCancelledPlaceIds);

        var secondPartPlaces = testFactory.createForToday(
                order(sortingCenter).externalId("2").places("2-1", "2-2", "2-3").build()
        ).getPlacesList();
        var secondPartCancelledPlaces = testFactory.createForToday(
                order(sortingCenter).externalId("4").places("4-1", "4-2", "4-3").build()
        ).cancel().getPlacesList();

        Stream.concat(secondPartPlaces.stream(), secondPartCancelledPlaces.stream()).forEach(
                place -> sortableAcceptSimpleService.preAccept(
                        new AcceptSortableRequestDto(
                                List.of(place.getMainPartnerCode()),
                                place.getWarehouseFrom().getId(),
                                true
                        ),
                        flowTraceDto
                )
        );

        //приняли вторую часть плейсов
        var secondPartPlaceIds = secondPartPlaces.stream().map(Place::getId).collect(Collectors.toList());
        secondPartPlaces = placeRepository.findAllById(secondPartPlaceIds);
        assertThat(secondPartPlaces).allMatch(it -> it.getStageId().equals(ACCEPTING_DIRECT.getId()));

        var secondPartCancelledPlaceIds =
                secondPartCancelledPlaces.stream().map(Place::getId).collect(Collectors.toList());
        secondPartCancelledPlaces = placeRepository.findAllById(secondPartCancelledPlaceIds);
        assertThat(secondPartCancelledPlaces).allMatch(it -> it.getStageId().equals(ACCEPTING_RETURN.getId()));

        //прошло 12 часов
        changePlaceHistoryTime(firstPlaceIds);
        changePlaceHistoryTime(secondPartPlaceIds);
        changePlaceHistoryTime(firstPartCancelledPlaceIds);
        changePlaceHistoryTime(secondPartCancelledPlaceIds);

        //сработала джоба
        revertSortableAcceptanceExecutor.revertAll(Instant.now());

        //должны откатиться только первая часть плейсов
        firstPartPlaces = placeRepository.findAllById(firstPartPlaceIds);
        assertThat(firstPartPlaces).allMatch(it -> it.getStageId().equals(AWAITING_DIRECT.getId()));

        firstPartCancelledPlaces = placeRepository.findAllById(firstPartCancelledPlaceIds);
        assertThat(firstPartCancelledPlaces).allMatch(it -> it.getStageId().equals(CANCELLED.getId()));

        secondPartPlaces = placeRepository.findAllById(secondPartPlaceIds);
        assertThat(secondPartPlaces).allMatch(it -> it.getStageId().equals(ACCEPTING_DIRECT.getId()));

        secondPartCancelledPlaces = placeRepository.findAllById(secondPartCancelledPlaceIds);
        assertThat(secondPartCancelledPlaces).allMatch(it -> it.getStageId().equals(ACCEPTING_RETURN.getId()));

        //сработала джоба
        revertSortableAcceptanceExecutor.revertAll(Instant.now());

        // ничего не поменялось
        firstPartPlaces = placeRepository.findAllById(firstPartPlaceIds);
        assertThat(firstPartPlaces).allMatch(it -> it.getStageId().equals(AWAITING_DIRECT.getId()));

        firstPartCancelledPlaces = placeRepository.findAllById(firstPartCancelledPlaceIds);
        assertThat(firstPartCancelledPlaces).allMatch(it -> it.getStageId().equals(CANCELLED.getId()));

        secondPartPlaces = placeRepository.findAllById(secondPartPlaceIds);
        assertThat(secondPartPlaces).allMatch(it -> it.getStageId().equals(ACCEPTING_DIRECT.getId()));

        secondPartCancelledPlaces = placeRepository.findAllById(secondPartCancelledPlaceIds);
        assertThat(secondPartCancelledPlaces).allMatch(it -> it.getStageId().equals(ACCEPTING_RETURN.getId()));

        //прошло 12 часов
        changePlaceHistoryTime(firstPlaceIds);
        changePlaceHistoryTime(secondPartPlaceIds);
        changePlaceHistoryTime(firstPartCancelledPlaceIds);
        changePlaceHistoryTime(secondPartCancelledPlaceIds);

        //сработала джоба
        revertSortableAcceptanceExecutor.revertAll(Instant.now());

        //вторая часть тоже откатилась
        secondPartPlaces = placeRepository.findAllById(secondPartPlaceIds);
        assertThat(secondPartPlaces).allMatch(it -> it.getStageId().equals(AWAITING_DIRECT.getId()));
        secondPartCancelledPlaces = placeRepository.findAllById(secondPartCancelledPlaceIds);
        assertThat(secondPartCancelledPlaces).allMatch(it -> it.getStageId().equals(CANCELLED.getId()));
    }


    @Test
    void testRevertLotAcceptance() {
        init(FlowSystemName.LOT_INITIAL_ACCEPTANCE, OperationSystemName.LOT_ACCEPT,
                new LotAcceptOperationContextData());
        var firstPartLot =
                testFactory.storedLot("SC_LOT_007", sortingCenter, user, AWAITING_DIRECT.getId(), true, null, null);

        lotAcceptService.preAccept(new AcceptLotRequestDto(firstPartLot.getBarcode()), flowTraceDto);

        firstPartLot = sortableLotService.findBySortableId(firstPartLot.getSortableId()).orElseThrow();
        assertThat(firstPartLot.getStageId()).isEqualTo(ACCEPTING_DIRECT.getId());

        //прошло 12 часов
        changeSortableHistoryTime(List.of(firstPartLot.getSortableId()));

        var secondPartLot =
                testFactory.storedLot("SC_LOT_008", sortingCenter, user, AWAITING_DIRECT.getId(), true, null, null);

        lotAcceptService.preAccept(new AcceptLotRequestDto(secondPartLot.getBarcode()), flowTraceDto);

        //приняли вторую часть лотов
        secondPartLot = sortableLotService.findBySortableId(secondPartLot.getSortableId()).orElseThrow();
        assertThat(secondPartLot.getStageId()).isEqualTo(ACCEPTING_DIRECT.getId());

        //прошло 12 часов
        changeSortableHistoryTime(List.of(firstPartLot.getSortableId(), secondPartLot.getSortableId()));

        //сработала джоба
        revertSortableAcceptanceExecutor.revertAll(Instant.now());

        //должны откатиться только первая часть плейсов
        firstPartLot = sortableLotService.findBySortableId(firstPartLot.getSortableId()).orElseThrow();
        assertThat(firstPartLot.getStageId()).isEqualTo(AWAITING_DIRECT.getId());

        secondPartLot = sortableLotService.findBySortableId(secondPartLot.getSortableId()).orElseThrow();
        assertThat(secondPartLot.getStageId()).isEqualTo(ACCEPTING_DIRECT.getId());

        //сработала джоба
        revertSortableAcceptanceExecutor.revertAll(Instant.now());

        // ничего не поменялось
        firstPartLot = sortableLotService.findBySortableId(firstPartLot.getSortableId()).orElseThrow();
        assertThat(firstPartLot.getStageId()).isEqualTo(AWAITING_DIRECT.getId());

        secondPartLot = sortableLotService.findBySortableId(secondPartLot.getSortableId()).orElseThrow();
        assertThat(secondPartLot.getStageId()).isEqualTo(ACCEPTING_DIRECT.getId());

        //прошло 12 часов
        changeSortableHistoryTime(List.of(firstPartLot.getSortableId(), secondPartLot.getSortableId()));

        //сработала джоба
        revertSortableAcceptanceExecutor.revertAll(Instant.now());

        //вторая часть тоже откатилась
        secondPartLot = sortableLotService.findBySortableId(secondPartLot.getSortableId()).orElseThrow();
        assertThat(secondPartLot.getStageId()).isEqualTo(AWAITING_DIRECT.getId());
    }

    private void changePlaceHistoryTime(List<Long> placeIds) {
        jdbcTemplate.update("UPDATE place " +
                        "SET created_at = created_at - interval '12 hours', " +
                        "updated_at = updated_at - interval '12 hours' " +
                        "WHERE id in (:placeIds)",
                Map.of("placeIds", placeIds));
        jdbcTemplate.update("UPDATE place_history " +
                        "SET created_at = created_at - interval '12 hours', " +
                        "updated_at = updated_at - interval '12 hours' " +
                        "WHERE place_id in (:placeIds)",
                Map.of("placeIds", placeIds));
    }

    private void changeSortableHistoryTime(List<Long> sortableIds) {
        jdbcTemplate.update("UPDATE sortable " +
                        "SET created_at = created_at - interval '12 hours', " +
                        "updated_at = updated_at - interval '12 hours' " +
                        "WHERE id in (:sortableIds)",
                Map.of("sortableIds", sortableIds));
        jdbcTemplate.update("UPDATE sortable_history " +
                        "SET created_at = created_at - interval '12 hours' " +
                        "WHERE sortable_id in (:sortableIds)",
                Map.of("sortableIds", sortableIds));
    }
}
