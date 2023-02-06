package ru.yandex.market.sc.core.domain.scan;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
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
import ru.yandex.market.sc.core.domain.flow_operation_context.repository.SortableAcceptSimpleFlowOperationContextData;
import ru.yandex.market.sc.core.domain.operation.repository.Operation;
import ru.yandex.market.sc.core.domain.operation.repository.OperationSystemName;
import ru.yandex.market.sc.core.domain.operation_log.model.OperationLogResult;
import ru.yandex.market.sc.core.domain.operation_log.repository.OperationLog;
import ru.yandex.market.sc.core.domain.operation_log.repository.OperationLogRepository;
import ru.yandex.market.sc.core.domain.order.AcceptService;
import ru.yandex.market.sc.core.domain.order.model.AcceptSortableRequestDto;
import ru.yandex.market.sc.core.domain.order.model.AcceptStatusCode;
import ru.yandex.market.sc.core.domain.order.model.ApiPreAcceptedSortableDto;
import ru.yandex.market.sc.core.domain.order.model.ApiSortableDto;
import ru.yandex.market.sc.core.domain.order.model.FlowTraceDto;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.place.model.PlaceId;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.process.repository.Process;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.stage.StageLoader;
import ru.yandex.market.sc.core.domain.stage.Stages;
import ru.yandex.market.sc.core.domain.stage.model.ApiStageDto;
import ru.yandex.market.sc.core.domain.user.model.UserRole;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.model.ApiWarehouseDto;
import ru.yandex.market.sc.core.domain.zone.repository.Zone;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.exception.ScException;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.sc.core.domain.scan.SortableAcceptSimpleService.FINISH_SUFFIX;
import static ru.yandex.market.sc.core.domain.scan.SortableAcceptSimpleService.PREACCEPT_SUFFIX;
import static ru.yandex.market.sc.core.domain.scan.SortableAcceptSimpleService.REVERT_SUFFIX;
import static ru.yandex.market.sc.core.domain.stage.Stages.ACCEPTING_DIRECT;
import static ru.yandex.market.sc.core.domain.stage.Stages.ACCEPTING_RETURN;
import static ru.yandex.market.sc.core.domain.stage.Stages.AWAITING_DIRECT;
import static ru.yandex.market.sc.core.domain.stage.Stages.CANCELLED;
import static ru.yandex.market.sc.core.domain.stage.Stages.FINAL_ACCEPT_DIRECT;
import static ru.yandex.market.sc.core.domain.stage.Stages.FINAL_ACCEPT_RETURN;
import static ru.yandex.market.sc.core.domain.stage.Stages.RETURNED_WITHOUT_ACCEPTANCE;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SortableAcceptSimpleServiceTest {

    private static final String FLOW_TRACE_ID = "74047fa9-21b8-4320-b244-08b228305b13";

    private final SortableAcceptSimpleService sortableAcceptSimpleService;
    private final TestFactory testFactory;
    private final PlaceRepository placeRepository;
    private final OperationLogRepository operationLogRepository;
    private final TransactionTemplate transactionTemplate;
    private final AcceptService acceptService;
    private final FlowOperationContextCommandService flowOperationContextCommandService;

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
        zone = testFactory.storedZone(sortingCenter, "zone-1", Collections.emptyList());
    }

    private void createFlowOperationContext(JsonNode configData) {
        operation = testFactory.storedOperation(
                OperationSystemName.SORTABLE_ACCEPT_SIMPLE.name(),
                OperationSystemName.SORTABLE_ACCEPT_SIMPLE.name());
        flow = testFactory.storedFlow(
                FlowSystemName.MERCHANT_INITIAL_ACCEPTANCE.name(),
                FlowSystemName.MERCHANT_INITIAL_ACCEPTANCE.name(),
                Map.of(operation, configData));
        process = testFactory.storedProcess(
                "process_name",
                "process_name",
                List.of(flow));

        var flowOperation = flow.getFlowOperations().stream().findFirst().orElseThrow();


        flowOperationContextCommandService.save(
                new FlowOperationContext(flowOperation, flow, FLOW_TRACE_ID, false,
                        new SortableAcceptSimpleFlowOperationContextData())
        );
    }

    private void createFlowOperationContextWithDefaultConfig() {
        var configData = testFactory.toJsonNode("{\"validateMerchant\": true, \"processCancelled\": true}");
        createFlowOperationContext(configData);
    }

    @Test
    void acceptByTwoBarcodesTest() {
        createFlowOperationContextWithDefaultConfig();
        var order = testFactory.createForToday(
                order(sortingCenter).externalId("1").places("1-0", "1-1").build()
        ).get();

        var apiSortableDto = sortableAcceptSimpleService.preAccept(
                new AcceptSortableRequestDto(List.of("1", "1-0"), order.getWarehouseFrom().getId(), null),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        var expected = buildOkResponse("1-0", order);
        assertThat(apiSortableDto).isEqualTo(expected);
        var place = placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "1-0").orElseThrow();
        assertThat(place.getStageId()).isEqualTo(ACCEPTING_DIRECT.getId());
        assertSuccessOperationLog(PREACCEPT_SUFFIX, AWAITING_DIRECT.getId(), place);
    }

    @Test
    void acceptByPlaceBarcodeTest() {
        createFlowOperationContextWithDefaultConfig();
        var order = testFactory.createForToday(
                order(sortingCenter).externalId("1").places("1-0", "1-1").build()
        ).get();

        var apiSortableDto = sortableAcceptSimpleService.preAccept(
                new AcceptSortableRequestDto(List.of("1-0"), order.getWarehouseFrom().getId(), null),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        var expected = buildOkResponse("1-0", order);
        assertThat(apiSortableDto).isEqualTo(expected);
        var place = placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "1-0").orElseThrow();
        assertThat(place.getStageId()).isEqualTo(ACCEPTING_DIRECT.getId());
        assertSuccessOperationLog(PREACCEPT_SUFFIX, AWAITING_DIRECT.getId(), place);
    }

    @Test
    @DisplayName("Повторная приемка посылки после реверта")
    void acceptPlaceAfterRevertStage() {
        createFlowOperationContextWithDefaultConfig();
        var order = testFactory.createForToday(
                order(sortingCenter).externalId("1").places("1-1").build()).get();


        sortableAcceptSimpleService.preAccept(
                new AcceptSortableRequestDto(List.of("1-1"), order.getWarehouseFrom().getId(), null),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        sortableAcceptSimpleService.revertAccept(flowTraceDto(REVERT_SUFFIX));

        sortableAcceptSimpleService.preAccept(
                new AcceptSortableRequestDto(List.of("1-1"), order.getWarehouseFrom().getId(), null),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        var apiSortableDto = sortableAcceptSimpleService.preAccept(
                new AcceptSortableRequestDto(List.of("1-1"), order.getWarehouseFrom().getId(), null),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        assertThat(apiSortableDto.acceptStatusCode()).isEqualTo(AcceptStatusCode.ALREADY_ACCEPTED);

        var place = placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "1-1").orElseThrow();
        assertThat(place.getStageId()).isEqualTo(ACCEPTING_DIRECT.getId());
        assertSuccessOperationLog(PREACCEPT_SUFFIX, AWAITING_DIRECT.getId(), place);
    }

    @Test
    void needAnotherBarcodeTest() {
        createFlowOperationContextWithDefaultConfig();
        var order = testFactory.createForToday(
                order(sortingCenter).externalId("1").places("1-0", "1-1").build()
        ).get();

        var apiSortableDto = sortableAcceptSimpleService.preAccept(
                new AcceptSortableRequestDto(List.of("1"), order.getWarehouseFrom().getId(), null),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        assertThat(apiSortableDto).isEqualTo(new ApiSortableDto(AcceptStatusCode.NEED_ANOTHER_BARCODE));
        var place1 = placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "1-0").orElseThrow();
        assertThat(place1.getStageId()).isEqualTo(AWAITING_DIRECT.getId());

        var order2 = testFactory.createForToday(
                order(sortingCenter).externalId("2").places("2-0", "1-1").build()
        ).get();

        apiSortableDto = sortableAcceptSimpleService.preAccept(
                new AcceptSortableRequestDto(List.of("1-1"), order2.getWarehouseFrom().getId(), null),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        assertThat(apiSortableDto).isEqualTo(new ApiSortableDto(AcceptStatusCode.NEED_ANOTHER_BARCODE));
        var place2 = placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "1-1").orElseThrow();
        assertThat(place2.getStageId()).isEqualTo(AWAITING_DIRECT.getId());
        var place3 = placeRepository.findByOrderIdAndMainPartnerCode(order2.getId(), "1-1").orElseThrow();
        assertThat(place3.getStageId()).isEqualTo(AWAITING_DIRECT.getId());

        apiSortableDto = sortableAcceptSimpleService.preAccept(
                new AcceptSortableRequestDto(List.of("1", "1-1"), order.getWarehouseFrom().getId(), null),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        place2 = placeRepository.findById(place2.getId()).orElseThrow();

        assertThat(apiSortableDto.acceptStatusCode()).isEqualTo(AcceptStatusCode.OK);
        assertSuccessOperationLog(PREACCEPT_SUFFIX, AWAITING_DIRECT.getId(), place2);
    }

    @Test
    void acceptBySinglePlaceOrderOrderBarcodeTest() {
        createFlowOperationContextWithDefaultConfig();
        var order = testFactory.createForToday(order(sortingCenter).externalId("1").build()).get();

        var apiSortableDto = sortableAcceptSimpleService.preAccept(
                new AcceptSortableRequestDto(List.of("1"), order.getWarehouseFrom().getId(), null),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        var expected = buildOkResponse("1", order);
        assertThat(apiSortableDto).isEqualTo(expected);
        var place = placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "1").orElseThrow();
        assertThat(place.getStageId()).isEqualTo(ACCEPTING_DIRECT.getId());
        assertSuccessOperationLog(PREACCEPT_SUFFIX, AWAITING_DIRECT.getId(), place);
    }

    @Test
    void acceptBySinglePlaceOrderPlaceBarcodeTest() {
        createFlowOperationContextWithDefaultConfig();
        var order = testFactory.createForToday(
                order(sortingCenter).externalId("1").places("1-0").build()
        ).get();

        var apiSortableDto = sortableAcceptSimpleService.preAccept(
                new AcceptSortableRequestDto(List.of("1"), order.getWarehouseFrom().getId(), null),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        var expected = buildOkResponse("1-0", order);
        assertThat(apiSortableDto).isEqualTo(expected);
        var place = placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "1-0").orElseThrow();
        assertThat(place.getStageId()).isEqualTo(ACCEPTING_DIRECT.getId());
        assertSuccessOperationLog(PREACCEPT_SUFFIX, AWAITING_DIRECT.getId(), place);
    }

    @Test
    void acceptPlaceByPlaceAndOrderBarcode() {
        createFlowOperationContextWithDefaultConfig();
        var order = testFactory.createForToday(
                order(sortingCenter).externalId("1").places("1-0").build()
        ).get();
        testFactory.createForToday(order(sortingCenter).externalId("2").places("1-0").build()).get();

        var apiSortableDto = sortableAcceptSimpleService.preAccept(
                new AcceptSortableRequestDto(List.of("1-0"), order.getWarehouseFrom().getId(), null),
                flowTraceDto(PREACCEPT_SUFFIX)
        );
        assertThat(apiSortableDto.acceptStatusCode()).isEqualTo(AcceptStatusCode.NEED_ANOTHER_BARCODE);

        apiSortableDto = sortableAcceptSimpleService.preAccept(
                new AcceptSortableRequestDto(List.of("1-0", "1"), order.getWarehouseFrom().getId(), null),
                flowTraceDto(PREACCEPT_SUFFIX)
        );
        assertThat(apiSortableDto.acceptStatusCode()).isEqualTo(AcceptStatusCode.OK);
    }

    @Test
    void canNotFindOrderTest() {
        createFlowOperationContextWithDefaultConfig();
        var warehouse = testFactory.storedWarehouse();
        assertThrows(ScException.class, () -> sortableAcceptSimpleService.preAccept(
                        new AcceptSortableRequestDto(List.of("1", "1-0"), warehouse.getId(), null),
                        flowTraceDto(PREACCEPT_SUFFIX)
                )
        );
        assertErrorOperationLog(user, PREACCEPT_SUFFIX, null, ScErrorCode.PLACE_NOT_FOUND);
    }

    @Test
    void wrongWarehouseTest() {
        createFlowOperationContextWithDefaultConfig();
        var order = testFactory.createForToday(
                order(sortingCenter).externalId("1").places("1-0").build()
        ).get();

        var apiSortableDto = sortableAcceptSimpleService.preAccept(
                new AcceptSortableRequestDto(List.of("1-0"), 13703L, null),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        assertThat(apiSortableDto).isEqualTo(
                new ApiSortableDto(
                        "1-0",
                        new ApiWarehouseDto(
                                order.getWarehouseFrom().getId(),
                                order.getWarehouseFrom().getIncorporation()
                        ),
                        AcceptStatusCode.WRONG_WAREHOUSE)
        );
        var place = placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "1-0").orElseThrow();
        assertThat(place.getStageId()).isEqualTo(AWAITING_DIRECT.getId());
        assertErrorOperationLog(user, PREACCEPT_SUFFIX, place, ScErrorCode.WRONG_WAREHOUSE_FOR_PLACE);
    }

    @Test
    void alreadyAcceptedTest() {
        createFlowOperationContextWithDefaultConfig();
        var user2 = testFactory.storedUser(sortingCenter, 100010L, UserRole.STOCKMAN, "kolya@mail", "Коля Коля");

        var order = testFactory.createForToday(
                order(sortingCenter).externalId("1").places("1-0").build()
        ).get();

        sortableAcceptSimpleService.preAccept(
                new AcceptSortableRequestDto(List.of("1-0"), order.getWarehouseFrom().getId(), null),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        var apiSortableDto = sortableAcceptSimpleService.preAccept(
                new AcceptSortableRequestDto(List.of("1-0"), order.getWarehouseFrom().getId(), null),
                new FlowTraceDto(
                        zone,
                        sortingCenter,
                        user2,
                        "process_name",
                        FlowSystemName.MERCHANT_INITIAL_ACCEPTANCE,
                        OperationSystemName.SORTABLE_ACCEPT_SIMPLE,
                        FLOW_TRACE_ID,
                        PREACCEPT_SUFFIX
                )
        );

        assertThat(apiSortableDto).isEqualTo(
                new ApiSortableDto(
                        "1-0",
                        new ApiWarehouseDto(
                                order.getWarehouseFrom().getId(),
                                order.getWarehouseFrom().getIncorporation()
                        ),
                        AcceptStatusCode.ALREADY_ACCEPTED,
                        LocalDateTime.now(clock),
                        user.getName()
                )
        );
        var place = placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "1-0").orElseThrow();
        assertThat(place.getStageId()).isEqualTo(ACCEPTING_DIRECT.getId());
        assertErrorOperationLog(user2, PREACCEPT_SUFFIX, place, ScErrorCode.SORTABLE_ALREADY_ACCEPTED);
    }

    @Test
    void wrongStageTest() {
        createFlowOperationContextWithDefaultConfig();
        var order = testFactory.createForToday(
                order(sortingCenter).externalId("1").places("1-0").build()
        ).get();

        var place = transactionTemplate.execute(ts -> {
            var p = testFactory.orderPlaces(order.getId()).get(0);
            p.setStage(StageLoader.getBySortableStatus(SortableStatus.SORTED_DIRECT), user);
            p = placeRepository.save(p);
            return p;
        });

        assertThrows(ScException.class, () -> sortableAcceptSimpleService.preAccept(
                        new AcceptSortableRequestDto(List.of("1", "1-0"), order.getWarehouseFrom().getId(), null),
                        flowTraceDto(PREACCEPT_SUFFIX)
                ), "Коробка находится в неверном статусе: Подготовлен к отгрузке, прямой поток"
        );

        assertErrorOperationLog(user, PREACCEPT_SUFFIX, place, ScErrorCode.PLACE_IN_WRONG_STATUS);
    }

    @Test
    void acceptSortablesAndFinishTest() {
        createFlowOperationContextWithDefaultConfig();
        var places = testFactory.createForToday(
                order(sortingCenter).externalId("1").places("1-1", "1-2", "1-3").build()
        ).getPlacesList();

        places.forEach(
                place -> sortableAcceptSimpleService.preAccept(
                        new AcceptSortableRequestDto(
                                List.of(place.getMainPartnerCode()),
                                place.getWarehouseFrom().getId(),
                                null
                        ),
                        flowTraceDto(PREACCEPT_SUFFIX)
                )
        );

        sortableAcceptSimpleService.finish(null, flowTraceDto(FINISH_SUFFIX));
        List<Long> placeIds = places.stream().map(Place::getId).toList();
        places = placeRepository.findAllById(placeIds);
        assertThat(places).allMatch(it -> it.getStageId().equals(FINAL_ACCEPT_DIRECT.getId()));
        places.forEach(place -> assertSuccessOperationLog(FINISH_SUFFIX, ACCEPTING_DIRECT.getId(), place));
    }

    @DisplayName("Сканируем плейс во флоу, потом извне меняем его стейдж. Завершаем флоу -- стейдж плейса не изменился")
    @Test
    void acceptSortablesAndFailFinishTest() {
        createFlowOperationContextWithDefaultConfig();
        var places = testFactory.createForToday(
                order(sortingCenter).externalId("1").places("1-1", "1-2", "1-3").build()
        ).getPlacesList();

        places.forEach(
                place -> sortableAcceptSimpleService.preAccept(
                        new AcceptSortableRequestDto(
                                List.of(place.getMainPartnerCode()),
                                place.getWarehouseFrom().getId(),
                                null
                        ),
                        flowTraceDto(PREACCEPT_SUFFIX)
                )
        );

        // Каким-то образом один из заказов переходит в невалидный для приёмки статус
        long changedPlaceId = places.get(0).getId();
        transactionTemplate.execute(ts -> {
            var placeInvalid = placeRepository.findById(changedPlaceId).orElseThrow();
            placeInvalid.setStage(StageLoader.getById(Stages.SHIPPED_DIRECT.getId()), user);
            placeRepository.save(placeInvalid);

            return null;
        });
        List<Long> anotherPlaceIds = places.stream()
                .map(Place::getId)
                .filter(it -> !it.equals(changedPlaceId))
                .collect(Collectors.toList());

        sortableAcceptSimpleService.finish(null, flowTraceDto(FINISH_SUFFIX));

        List<Place> anotherPlaces = placeRepository.findAllById(anotherPlaceIds);
        assertThat(anotherPlaces).allMatch(it -> it.getStageId().equals(FINAL_ACCEPT_DIRECT.getId()));
        anotherPlaces.forEach(place -> assertSuccessOperationLog(FINISH_SUFFIX, ACCEPTING_DIRECT.getId(), place));

        var placeChanged = placeRepository.findById(changedPlaceId).orElseThrow();
        assertThat(placeChanged.getStageId()).isEqualTo(Stages.SHIPPED_DIRECT.getId());
    }

    @Test
    void acceptSortablesAndRevertTest() {
        createFlowOperationContextWithDefaultConfig();
        var places = testFactory.createForToday(
                order(sortingCenter).externalId("1").places("1-1", "1-2", "1-3").build()
        ).getPlacesList();

        places.forEach(
                place -> sortableAcceptSimpleService.preAccept(
                        new AcceptSortableRequestDto(
                                List.of(place.getMainPartnerCode()),
                                place.getWarehouseFrom().getId(),
                                null
                        ),
                        flowTraceDto(PREACCEPT_SUFFIX)
                )
        );

        sortableAcceptSimpleService.revertAccept(flowTraceDto(REVERT_SUFFIX));
        List<Long> placeIds = places.stream().map(Place::getId).collect(Collectors.toList());
        places = placeRepository.findAllById(placeIds);
        assertThat(places).allMatch(it -> it.getStageId().equals(AWAITING_DIRECT.getId()));
        places.forEach(place -> assertSuccessOperationLog(REVERT_SUFFIX, ACCEPTING_DIRECT.getId(), place));
    }

    private ApiSortableDto buildOkResponse(String placeExternalId, ScOrder order) {
        return new ApiSortableDto(
                placeExternalId,
                new ApiWarehouseDto(
                        order.getWarehouseFrom().getId(),
                        order.getWarehouseFrom().getIncorporation()
                ),
                AcceptStatusCode.OK,
                LocalDateTime.now(clock),
                user.getName()
        );
    }

    private void assertSuccessOperationLog(String suffix, Long stageBefore, Place place) {
        var operationLogs = operationLogRepository.findAll();

        assertThat(operationLogs)
                .usingElementComparatorIgnoringFields("id", "createdAt", "updatedAt")
                .contains(
                        new OperationLog(
                                sortingCenter,
                                user,
                                zone.getId(),
                                Instant.now(clock),
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
                                place.getStageId(),
                                null)
                );
    }

    private void assertErrorOperationLog(User user, String suffix, @Nullable Place place, ScErrorCode errorCode) {
        var operationLogs = operationLogRepository.findAll();

        assertThat(operationLogs)
                .usingElementComparatorIgnoringFields("id", "createdAt", "updatedAt")
                .contains(
                        new OperationLog(
                                sortingCenter,
                                user,
                                zone.getId(),
                                Instant.now(clock),
                                process.getId(),
                                flow.getId(),
                                operation.getId(),
                                OperationLogResult.ERROR,
                                FLOW_TRACE_ID,
                                suffix,
                                null,
                                errorCode,
                                Optional.ofNullable(place).map(Place::getId).orElse(null),
                                null,
                                SortableType.PLACE.name(),
                                Optional.ofNullable(place).map(Place::getMainPartnerCode).orElse(null),
                                null,
                                null,
                                Optional.ofNullable(place).map(Place::getStageId).orElse(null),
                                Optional.ofNullable(place).map(Place::getStageId).orElse(null),
                                null)
                );
    }

    @Test
    void preAcceptedSortables() {
        createFlowOperationContextWithDefaultConfig();
        var places = testFactory.createForToday(
                order(sortingCenter).externalId("1").places("1-1", "1-2", "1-3").build()
        ).getPlacesList();

        places.stream()
                .filter(p -> !Objects.equals(p.getMainPartnerCode(), "1-2"))
                .forEach(
                        place -> sortableAcceptSimpleService.preAccept(
                                new AcceptSortableRequestDto(
                                        List.of(place.getMainPartnerCode()),
                                        place.getWarehouseFrom().getId(),
                                        null
                                ),
                                flowTraceDto(PREACCEPT_SUFFIX)
                        )
                );

        var w = Objects.requireNonNull(places.get(0).getWarehouseFrom());
        var result = sortableAcceptSimpleService.getPreAcceptedPlaces(FLOW_TRACE_ID);
        assertThat(result.placeExternalIds()).containsExactlyInAnyOrder("1-1", "1-3");
        assertThat(result.warehouse()).isEqualTo(new ApiWarehouseDto(w.getId(), w.getIncorporation()));
    }

    @Test
    @DisplayName("Частичный реверт стэйджей джобой")
    void callPreAcceptedSortableWhenPartialRevertedStages() {
        createFlowOperationContextWithDefaultConfig();
        var order = testFactory.createForToday(
                order(sortingCenter).externalId("o1").places("p1", "p2", "p3").build()).get();

        var cancelled = testFactory.createForToday(
                order(sortingCenter).externalId("o2").places("p4", "p5", "p6").build()).cancel().get();

        Consumer<Place> preAcceptConsumer = place -> sortableAcceptSimpleService.preAccept(
                new AcceptSortableRequestDto(
                        List.of(place.getMainPartnerCode()),
                        place.getWarehouseFrom().getId(),
                        true
                ),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        StreamEx.of(testFactory.orderPlaces(order)).forEach(preAcceptConsumer);
        StreamEx.of(testFactory.orderPlaces(cancelled)).forEach(preAcceptConsumer);
        transactionTemplate.execute(tx -> {
            acceptService.revertAcceptPlace(PlaceId.of(testFactory.orderPlace(order, "p1")), user);
            acceptService.revertAcceptPlace(PlaceId.of(testFactory.orderPlace(order, "p2")), user);
            acceptService.revertAcceptCancelledPlace(PlaceId.of(testFactory.orderPlace(cancelled, "p5")), user);
            return null;
        });
        var wFrom = Objects.requireNonNull(testFactory.orderPlace(order, "p1").getWarehouseFrom());
        var result = sortableAcceptSimpleService.getPreAcceptedPlaces(FLOW_TRACE_ID);
        assertThat(result.placeExternalIds()).containsExactlyInAnyOrder("p3", "p4", "p6");
        assertThat(result.warehouse()).isEqualTo(new ApiWarehouseDto(wFrom.getId(), wFrom.getIncorporation()));
    }

    @Test
    void preAcceptedSortablesEmpty() {
        createFlowOperationContextWithDefaultConfig();
        var result = sortableAcceptSimpleService.getPreAcceptedPlaces(FLOW_TRACE_ID);
        assertThat(result).isEqualTo(new ApiPreAcceptedSortableDto(Collections.emptyList(), null));
    }

    @Test
    void preAcceptedCanBeReturnedSortable() {
        createFlowOperationContextWithDefaultConfig();
        var order = testFactory.createForToday(
                order(sortingCenter).externalId("1").places("1-1", "1-2", "1-3").build()
        ).cancel().get();

        //сканируют первый раз отмененную посылку
        var apiSortableDto = sortableAcceptSimpleService.preAccept(
                new AcceptSortableRequestDto(
                        List.of("1-2"),
                        order.getWarehouseFrom().getId(),
                        null
                ),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        var expected = new ApiSortableDto(
                "1-2",
                new ApiWarehouseDto(order.getWarehouseFrom().getId(), order.getWarehouseFrom().getIncorporation()),
                AcceptStatusCode.CAN_BE_RETURNED,
                new ApiStageDto(CANCELLED.name(), "Отмена из внешней системы")
        );

        assertThat(apiSortableDto).isEqualTo(expected);
        var place = placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "1-2").orElseThrow();
        assertThat(place.getStageId()).isEqualTo(CANCELLED.getId());
    }

    @Test
    void preAcceptedReturnSortable() {
        createFlowOperationContextWithDefaultConfig();
        var order = testFactory.createForToday(
                order(sortingCenter).externalId("1").places("1-1", "1-2", "1-3").build()
        ).cancel().get();

        //выбрали пункт "принять посылку..."
        var apiSortableDto = sortableAcceptSimpleService.preAccept(
                new AcceptSortableRequestDto(
                        List.of("1-2"),
                        order.getWarehouseFrom().getId(),
                        true
                ),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        var expected = new ApiSortableDto(
                "1-2",
                new ApiWarehouseDto(
                        order.getWarehouseFrom().getId(),
                        order.getWarehouseFrom().getIncorporation()
                ),
                AcceptStatusCode.OK,
                LocalDateTime.now(clock),
                user.getName()
        );

        assertThat(apiSortableDto).isEqualTo(expected);
        var place = placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "1-2").orElseThrow();
        assertThat(place.getStageId()).isEqualTo(ACCEPTING_RETURN.getId());
        assertSuccessOperationLog(PREACCEPT_SUFFIX, CANCELLED.getId(), place);

        //финиш приемки
        sortableAcceptSimpleService.finish(null, flowTraceDto(FINISH_SUFFIX));
        place = placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "1-2").orElseThrow();
        assertThat(place.getStageId()).isEqualTo(FINAL_ACCEPT_RETURN.getId());
        assertSuccessOperationLog(FINISH_SUFFIX, ACCEPTING_RETURN.getId(), place);
    }

    @Test
    void preAcceptedReturnSortableAlreadyAccepted() {
        createFlowOperationContextWithDefaultConfig();
        var order = testFactory.createForToday(
                order(sortingCenter).externalId("1").places("1-1", "1-2", "1-3").build()
        ).cancel().get();

        //выбрали пункт "принять посылку..."
        var apiSortableDto = sortableAcceptSimpleService.preAccept(
                new AcceptSortableRequestDto(
                        List.of("1-2"),
                        order.getWarehouseFrom().getId(),
                        true
                ),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        var expected = new ApiSortableDto(
                "1-2",
                new ApiWarehouseDto(
                        order.getWarehouseFrom().getId(),
                        order.getWarehouseFrom().getIncorporation()
                ),
                AcceptStatusCode.ALREADY_ACCEPTED,
                LocalDateTime.now(clock),
                user.getName()
        );

        apiSortableDto = sortableAcceptSimpleService.preAccept(
                new AcceptSortableRequestDto(
                        List.of("1-2"),
                        order.getWarehouseFrom().getId(),
                        null
                ),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        assertThat(apiSortableDto).isEqualTo(expected);
    }

    @Test
    void returnWithoutAcceptanceSortable() {
        createFlowOperationContextWithDefaultConfig();
        var order = testFactory.createForToday(
                order(sortingCenter).externalId("1").places("1-1", "1-2", "1-3").build()
        ).cancel().get();

        //выбрали пункт "вернуть посылку без приемки"
        var apiSortableDto = sortableAcceptSimpleService.preAccept(
                new AcceptSortableRequestDto(
                        List.of("1-2"),
                        order.getWarehouseFrom().getId(),
                        false
                ),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        var expected = new ApiSortableDto(
                "1-2",
                new ApiWarehouseDto(
                        order.getWarehouseFrom().getId(),
                        order.getWarehouseFrom().getIncorporation()
                ),
                AcceptStatusCode.OK,
                LocalDateTime.now(clock),
                user.getName()
        );

        assertThat(apiSortableDto).isEqualTo(expected);
        var place = placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "1-2").orElseThrow();
        assertThat(place.getStageId()).isEqualTo(RETURNED_WITHOUT_ACCEPTANCE.getId());
        assertSuccessOperationLog(PREACCEPT_SUFFIX, CANCELLED.getId(), place);

        //сканируем еще раз посылку со статусом RETURNED_WITHOUT_ACCEPTANCE
        apiSortableDto = sortableAcceptSimpleService.preAccept(
                new AcceptSortableRequestDto(
                        List.of("1-2"),
                        order.getWarehouseFrom().getId(),
                        null
                ),
                flowTraceDto(PREACCEPT_SUFFIX)
        );

        expected = new ApiSortableDto(
                "1-2",
                new ApiWarehouseDto(order.getWarehouseFrom().getId(), order.getWarehouseFrom().getIncorporation()),
                AcceptStatusCode.CAN_BE_RETURNED,
                new ApiStageDto(Stages.RETURNED_WITHOUT_ACCEPTANCE.name(), "Возвращен без приемки")
        );

        assertThat(apiSortableDto).isEqualTo(expected);
        place = placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "1-2").orElseThrow();
        assertThat(place.getStageId()).isEqualTo(RETURNED_WITHOUT_ACCEPTANCE.getId());
    }

    @Test
    void acceptCreatedAndCancelledSortablesAndFinishTest() {
        createFlowOperationContextWithDefaultConfig();
        var createdPlaces = testFactory.createForToday(
                order(sortingCenter).externalId("1").places("1-1", "1-2", "1-3").build()
        ).getPlacesList();

        var cancelledPlaces = testFactory.createForToday(
                order(sortingCenter).externalId("2").places("2-1", "2-2", "2-3").build()
        ).cancel().getPlacesList();

        createdPlaces.forEach(
                place -> sortableAcceptSimpleService.preAccept(
                        new AcceptSortableRequestDto(
                                List.of(place.getMainPartnerCode()),
                                place.getWarehouseFrom().getId(),
                                null
                        ),
                        flowTraceDto(PREACCEPT_SUFFIX)
                )
        );

        cancelledPlaces.forEach(
                place -> sortableAcceptSimpleService.preAccept(
                        new AcceptSortableRequestDto(
                                List.of(place.getMainPartnerCode()),
                                place.getWarehouseFrom().getId(),
                                true
                        ),
                        flowTraceDto(PREACCEPT_SUFFIX)
                )
        );

        sortableAcceptSimpleService.finish(null, flowTraceDto(FINISH_SUFFIX));

        var createdPlaceIds = createdPlaces.stream().map(Place::getId).toList();
        createdPlaces = placeRepository.findAllById(createdPlaceIds);
        assertThat(createdPlaces).allMatch(it -> it.getStageId().equals(FINAL_ACCEPT_DIRECT.getId()));
        createdPlaces.forEach(place -> assertSuccessOperationLog(FINISH_SUFFIX, ACCEPTING_DIRECT.getId(), place));

        var cancelledPlaceIds = cancelledPlaces.stream().map(Place::getId).toList();
        cancelledPlaces = placeRepository.findAllById(cancelledPlaceIds);
        assertThat(cancelledPlaces).allMatch(it -> it.getStageId().equals(FINAL_ACCEPT_RETURN.getId()));
        cancelledPlaces.forEach(place -> assertSuccessOperationLog(FINISH_SUFFIX, ACCEPTING_RETURN.getId(), place));
    }

    @Test
    void acceptCreatedAndCancelledSortablesAndRevertTest() {
        createFlowOperationContextWithDefaultConfig();
        var createdPlaces = testFactory.createForToday(
                order(sortingCenter).externalId("1").places("1-1", "1-2", "1-3").build()
        ).getPlacesList();

        var cancelledPlaces = testFactory.createForToday(
                order(sortingCenter).externalId("2").places("2-1", "2-2", "2-3").build()
        ).cancel().getPlacesList();

        createdPlaces.forEach(
                place -> sortableAcceptSimpleService.preAccept(
                        new AcceptSortableRequestDto(
                                List.of(place.getMainPartnerCode()),
                                place.getWarehouseFrom().getId(),
                                null
                        ),
                        flowTraceDto(PREACCEPT_SUFFIX)
                )
        );

        cancelledPlaces.forEach(
                place -> sortableAcceptSimpleService.preAccept(
                        new AcceptSortableRequestDto(
                                List.of(place.getMainPartnerCode()),
                                place.getWarehouseFrom().getId(),
                                true
                        ),
                        flowTraceDto(PREACCEPT_SUFFIX)
                )
        );

        sortableAcceptSimpleService.revertAccept(flowTraceDto(REVERT_SUFFIX));

        var createdPlaceIds = createdPlaces.stream().map(Place::getId).toList();
        createdPlaces = placeRepository.findAllById(createdPlaceIds);
        assertThat(createdPlaces).allMatch(it -> it.getStageId().equals(AWAITING_DIRECT.getId()));
        createdPlaces.forEach(place -> assertSuccessOperationLog(REVERT_SUFFIX, ACCEPTING_DIRECT.getId(), place));

        var cancelledPlaceIds = cancelledPlaces.stream().map(Place::getId).toList();
        cancelledPlaces = placeRepository.findAllById(cancelledPlaceIds);
        assertThat(cancelledPlaces).allMatch(it -> it.getStageId().equals(CANCELLED.getId()));
        cancelledPlaces.forEach(place -> assertSuccessOperationLog(REVERT_SUFFIX, ACCEPTING_RETURN.getId(), place));
    }

    @Test
    void preAcceptedCantProcessCancelled() {
        var configData = testFactory.toJsonNode("{\"validateMerchant\": false, \"processCancelled\": false}");
        createFlowOperationContext(configData);
        var order = testFactory.createForToday(
                order(sortingCenter).externalId("1").places("1-1", "1-2", "1-3").build()
        ).cancel().get();

        assertThrows(ScException.class, () -> sortableAcceptSimpleService.preAccept(
                        new AcceptSortableRequestDto(List.of("1", "1-2"), order.getWarehouseFrom().getId(), null),
                        flowTraceDto(PREACCEPT_SUFFIX)
                ), "Коробка находится в неверном статусе: Отменен"
        );
        var place = placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "1-2").orElseThrow();
        assertErrorOperationLog(user, PREACCEPT_SUFFIX, place, ScErrorCode.PLACE_IN_WRONG_STATUS);
        assertThat(place.getStageId()).isEqualTo(CANCELLED.getId());
    }

    private FlowTraceDto flowTraceDto(String suffix) {
        return new FlowTraceDto(
                zone,
                sortingCenter,
                user,
                "process_name",
                FlowSystemName.MERCHANT_INITIAL_ACCEPTANCE,
                OperationSystemName.SORTABLE_ACCEPT_SIMPLE,
                FLOW_TRACE_ID,
                suffix
        );
    }
}
