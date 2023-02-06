package ru.yandex.market.sc.internal.sqs.handler;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistic.api.model.fulfillment.ReturnType;
import ru.yandex.market.logistics.les.dto.PointDto;
import ru.yandex.market.logistics.les.dto.PointType;
import ru.yandex.market.logistics.les.dto.StorageUnitResponseErrorDto;
import ru.yandex.market.logistics.les.dto.StorageUnitResponseErrorType;
import ru.yandex.market.logistics.les.tpl.StorageUnitCreateResponseEvent;
import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.stage.Stages;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.test.ScOrderWithPlace;
import ru.yandex.market.sc.core.test.ScOrderWithPlaces;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.CargoUnitTestFactory;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;
import ru.yandex.market.sc.internal.util.les.LesModelFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@EmbeddedDbIntTest
class CargoUnitCreateHandlerTest {

    @Autowired
    TestFactory testFactory;
    @Autowired
    CargoUnitTestFactory cargoUnitTestFactory;
    @Autowired
    Clock clock;

    @Autowired
    JdbcTemplate jdbcTemplate;
    private SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        testFactory.setConfiguration(ConfigurationProperties.ENABLE_CREATE_CARGO_V2, true);
        sortingCenter = testFactory.storedSortingCenter();
    }


    @Test
    public void lrmCreateWithoutLom() {
        var orderWithPlace = createCargoUnit(
                "o1", "p2", "cu1", "s1",
                PointType.SORTING_CENTER, 987L
        );
        assertThat(orderWithPlace.order().getFfStatus()).isEqualTo(ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(orderWithPlace.order().getSegmentUid()).isEqualTo("s1");
        assertThat(orderWithPlace.order().getCargoUnitId()).isEqualTo("cu1");

        assertThat(orderWithPlace.place().getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);
        assertThat(orderWithPlace.place().getStageId()).isEqualTo(Stages.AWAITING_RETURN.getId());
        assertThat(orderWithPlace.place().getSegmentUid()).isEqualTo("s1");
        assertThat(orderWithPlace.place().getCargoUnitId()).isEqualTo("cu1");
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void lrmCreateAfterLomNotOnSc() {
        var warehouse1 = 123L;
        var lomOrder = createOrder("o1", "p2", warehouse1, ReturnType.DROPOFF);
        assertThat(lomOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_CREATED_FF);
        assertThat(lomOrder.order().getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse1));
        assertThat(lomOrder.order().getWarehouseReturn().getType()).isEqualTo(WarehouseType.DROPOFF);
        assertThat(lomOrder.order().getSegmentUid()).isNull();
        assertThat(lomOrder.order().getCargoUnitId()).isNull();

        assertThat(lomOrder.place("p2").getSortableStatus()).isEqualTo(SortableStatus.AWAITING_DIRECT);
        assertThat(lomOrder.place("p2").getStageId()).isEqualTo(Stages.AWAITING_DIRECT.getId());
        assertThat(lomOrder.place("p2").getWarehouseReturn()).isNotNull();
        assertThat(lomOrder.place("p2").getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse1));
        assertThat(lomOrder.place("p2").getWarehouseReturn().getType()).isEqualTo(WarehouseType.DROPOFF);
        assertThat(lomOrder.place("p2").getSegmentUid()).isNull();
        assertThat(lomOrder.place("p2").getCargoUnitId()).isNull();

        // TODO: ORDER_CANCELLED_FF -> 160
        var warehouse2 = 321L;
        var lrmOrder = createCargoUnit(
                "o1", "p2", "cu1", "s1",
                PointType.SORTING_CENTER, warehouse2
        );
        assertThat(lrmOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_CANCELLED_FF);
        assertThat(lrmOrder.order().getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse2));
        assertThat(lrmOrder.order().getWarehouseReturn().getType()).isEqualTo(WarehouseType.SORTING_CENTER);
        assertThat(lrmOrder.order().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.order().getCargoUnitId()).isEqualTo("cu1");

        assertThat(lrmOrder.place().getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);
        assertThat(lrmOrder.place().getStageId()).isEqualTo(Stages.AWAITING_RETURN.getId());
        assertThat(lrmOrder.place().getWarehouseReturn()).isNotNull();
        assertThat(lrmOrder.place().getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse2));
        assertThat(lrmOrder.place().getWarehouseReturn().getType()).isEqualTo(WarehouseType.SORTING_CENTER);
        assertThat(lrmOrder.place().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.place().getCargoUnitId()).isEqualTo("cu1");
    }

    @Test
    public void lrmCreateAfterLomAccepted() {
        var warehouse1 = 123L;
        var lomOrder = createOrder(
                "o1", "p2", warehouse1, ReturnType.DROPOFF,
                builder -> builder.acceptPlace("p2")
        );
        assertThat(lomOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);

        assertThat(lomOrder.place("p2").getSortableStatus()).isEqualTo(SortableStatus.ARRIVED_DIRECT);
        assertThat(lomOrder.place("p2").getStageId()).isEqualTo(Stages.FINAL_ACCEPT_DIRECT.getId());

        var warehouse2 = 321L;
        var lrmOrder = createCargoUnit(
                "o1", "p2", "cu1", "s1",
                PointType.SORTING_CENTER, warehouse2
        );
        assertThat(lrmOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        assertThat(lrmOrder.order().getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse2));
        assertThat(lrmOrder.order().getWarehouseReturn().getType()).isEqualTo(WarehouseType.SORTING_CENTER);
        assertThat(lrmOrder.order().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.order().getCargoUnitId()).isEqualTo("cu1");

        assertThat(lrmOrder.place().getSortableStatus()).isEqualTo(SortableStatus.ACCEPTED_RETURN);
        assertThat(lrmOrder.place().getStageId()).isEqualTo(Stages.FINAL_ACCEPT_RETURN.getId());
        assertThat(lrmOrder.place().getWarehouseReturn()).isNotNull();
        assertThat(lrmOrder.place().getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse2));
        assertThat(lrmOrder.place().getWarehouseReturn().getType()).isEqualTo(WarehouseType.SORTING_CENTER);
        assertThat(lrmOrder.place().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.place().getCargoUnitId()).isEqualTo("cu1");
    }

    @Test
    public void lrmCreateAfterLomCancelNotOnSc() {
        var warehouse1 = 123L;
        var lomOrder = createOrder(
                "o1", "p2", warehouse1, ReturnType.WAREHOUSE,
                TestFactory.TestOrderBuilder::cancel
        );
        assertThat(lomOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_CANCELLED_FF);

        assertThat(lomOrder.place("p2").getSortableStatus()).isEqualTo(SortableStatus.CANCELLED);
        assertThat(lomOrder.place("p2").getStageId()).isEqualTo(Stages.CANCELLED.getId());

        // TODO: ORDER_CANCELLED_FF -> 160
        var warehouse2 = 321L;
        var lrmOrder = createCargoUnit(
                "o1", "p2", "cu1", "s1",
                PointType.SORTING_CENTER, warehouse2
        );
        assertThat(lrmOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_CANCELLED_FF);
        assertThat(lrmOrder.order().getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse2));
        assertThat(lrmOrder.order().getWarehouseReturn().getType()).isEqualTo(WarehouseType.SORTING_CENTER);
        assertThat(lrmOrder.order().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.order().getCargoUnitId()).isEqualTo("cu1");

        assertThat(lrmOrder.place().getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);
        assertThat(lrmOrder.place().getStageId()).isEqualTo(Stages.AWAITING_RETURN.getId());
        assertThat(lrmOrder.place().getWarehouseReturn()).isNotNull();
        assertThat(lrmOrder.place().getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse2));
        assertThat(lrmOrder.place().getWarehouseReturn().getType()).isEqualTo(WarehouseType.SORTING_CENTER);
        assertThat(lrmOrder.place().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.place().getCargoUnitId()).isEqualTo("cu1");
    }

    @Test
    public void lrmCreateAfterLomCancelAccepted() {
        var warehouse1 = 123L;
        var lomOrder = createOrder(
                "o1", "p2", warehouse1, ReturnType.WAREHOUSE,
                builder -> builder.acceptPlace("p2").cancel()
        );
        assertThat(lomOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);

        assertThat(lomOrder.place("p2").getSortableStatus()).isEqualTo(SortableStatus.ACCEPTED_RETURN);
        assertThat(lomOrder.place("p2").getStageId()).isEqualTo(Stages.FINAL_ACCEPT_RETURN.getId());

        var warehouse2 = 321L;
        var lrmOrder = createCargoUnit(
                "o1", "p2", "cu1", "s1",
                PointType.SORTING_CENTER, warehouse2
        );
        assertThat(lrmOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        assertThat(lrmOrder.order().getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse2));
        assertThat(lrmOrder.order().getWarehouseReturn().getType()).isEqualTo(WarehouseType.SORTING_CENTER);
        assertThat(lrmOrder.order().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.order().getCargoUnitId()).isEqualTo("cu1");

        assertThat(lrmOrder.place().getSortableStatus()).isEqualTo(SortableStatus.ACCEPTED_RETURN);
        assertThat(lrmOrder.place().getStageId()).isEqualTo(Stages.FINAL_ACCEPT_RETURN.getId());
        assertThat(lrmOrder.place().getWarehouseReturn()).isNotNull();
        assertThat(lrmOrder.place().getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse2));
        assertThat(lrmOrder.place().getWarehouseReturn().getType()).isEqualTo(WarehouseType.SORTING_CENTER);
        assertThat(lrmOrder.place().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.place().getCargoUnitId()).isEqualTo("cu1");
    }

    @Test
    public void lrmCreateAfterLomSortedToBuffer() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, true);
        var warehouse1 = 123L;
        var lomOrder = createOrder(
                "o1", "p2", warehouse1, ReturnType.SHOP,
                builder -> builder.acceptPlace("p2").cancel().keepPlaces("p2")
        );
        assertThat(lomOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);

        assertThat(lomOrder.place("p2").getSortableStatus()).isEqualTo(SortableStatus.KEEPED_RETURN);
        assertThat(lomOrder.place("p2").getStageId()).isEqualTo(Stages.KEEPED_RETURN.getId());

        var warehouse2 = 321L;
        var lrmOrder = createCargoUnit(
                "o1", "p2", "cu1", "s1",
                PointType.SORTING_CENTER, warehouse2
        );
        assertThat(lrmOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(lrmOrder.order().getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse2));
        assertThat(lrmOrder.order().getWarehouseReturn().getType()).isEqualTo(WarehouseType.SORTING_CENTER);
        assertThat(lrmOrder.order().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.order().getCargoUnitId()).isEqualTo("cu1");

        assertThat(lrmOrder.place().getSortableStatus()).isEqualTo(SortableStatus.KEEPED_RETURN);
        assertThat(lrmOrder.place().getStageId()).isEqualTo(Stages.KEEPED_RETURN.getId());
        assertThat(lrmOrder.place().getWarehouseReturn()).isNotNull();
        assertThat(lrmOrder.place().getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse2));
        assertThat(lrmOrder.place().getWarehouseReturn().getType()).isEqualTo(WarehouseType.SORTING_CENTER);
        assertThat(lrmOrder.place().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.place().getCargoUnitId()).isEqualTo("cu1");
    }

    @Test
    public void lrmCreateAfterLomSortedDirect() {
        var warehouse1 = 123L;
        var lomOrder = createOrder(
                "o1", "p2", warehouse1, ReturnType.SHOP,
                builder -> builder.updateShipmentDate(LocalDate.now(clock)).acceptPlace("p2").sortPlace("p2")
        );
        assertThat(lomOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);

        assertThat(lomOrder.place("p2").getSortableStatus()).isEqualTo(SortableStatus.SORTED_DIRECT);
        assertThat(lomOrder.place("p2").getStageId()).isEqualTo(Stages.SORTED_DIRECT.getId());

        var warehouse2 = 321L;
        assertThat(createCargoUnitReturnResponse(
                "o1", "p2", "cu1", "s1",
                PointType.SORTING_CENTER, warehouse2
        ).getResult().getErrors()).isEqualTo(getSortedErrors(PointType.SHOP, warehouse1, 431782L));
    }

    @SuppressWarnings("SameParameterValue")
    private List<StorageUnitResponseErrorDto> getSortedErrors(PointType type, long warehouseId,
                                                              @Nullable Long shopId) {
        return List.of(new StorageUnitResponseErrorDto(
                "Can't update segment: invalid status",
                StorageUnitResponseErrorType.IMPOSSIBLE_UPDATE_SEGMENT_ALREADY_ON_SC,
                List.of(new StorageUnitResponseErrorDto.CargoUnit(
                        "cu1",
                        "s1"
                )),
                new StorageUnitResponseErrorDto.Details(new PointDto(
                        PointType.valueOf(type.name()),
                        warehouseId,
                        shopId,
                        "ООО Ромашка-Склад"
                ))
        ));
    }

    @Test
    public void lrmCreateAfterLomSortedToReturn() {
        var warehouse1 = 123L;
        var lomOrder = createOrder(
                "o1", "p2", warehouse1, ReturnType.SHOP,
                builder -> builder.acceptPlace("p2").cancel().sortPlace("p2")
        );
        assertThat(lomOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);

        assertThat(lomOrder.place("p2").getSortableStatus()).isEqualTo(SortableStatus.SORTED_RETURN);
        assertThat(lomOrder.place("p2").getStageId()).isEqualTo(Stages.SORTED_RETURN.getId());

        var warehouse2 = 321L;
        assertThat(createCargoUnitReturnResponse(
                "o1", "p2", "cu1", "s1",
                PointType.SORTING_CENTER, warehouse2
        ).getResult().getErrors()).isEqualTo(getSortedErrors(PointType.SHOP, warehouse1, 431782L));
    }

    @Test
    public void lrmFallbackPointTypeForUtilizatorWarehouseType() {
        var warehouse1 = 123L;
        createOrder(
                "o1", "p2", warehouse1, ReturnType.WAREHOUSE, // тип не обновится
                builder -> builder.acceptPlace("p2").cancel().sortPlace("p2")
        );
        testFactory.updateWarehouseType(String.valueOf(warehouse1), WarehouseType.UTILIZATOR);
        var warehouse2 = 321L;
        assertThat(createCargoUnitReturnResponse(
                "o1", "p2", "cu1", "s1",
                PointType.SORTING_CENTER, warehouse2
        ).getResult().getErrors()).isEqualTo(getSortedErrors(PointType.UTILIZER, warehouse1, null));
    }

    @Test
    public void lrmCreateAfterLomSortedToLot() {
        var warehouse1 = 123L;
        var lomOrder = createOrder(
                "o1", "p2", warehouse1, ReturnType.SHOP,
                builder -> builder.acceptPlace("p2").cancel().sortPlace("p2")
                        .sortPlaceToLot("SC_LOT_l1", SortableType.PALLET, "p2")
        );
        assertThat(lomOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);

        assertThat(lomOrder.place("p2").getSortableStatus()).isEqualTo(SortableStatus.SORTED_RETURN);
        assertThat(lomOrder.place("p2").getStageId()).isEqualTo(Stages.SORTED_RETURN.getId());

        var warehouse2 = 321L;
        assertThat(createCargoUnitReturnResponse(
                "o1", "p2", "cu1", "s1",
                PointType.SORTING_CENTER, warehouse2
        ).getResult().getErrors()).isEqualTo(getSortedErrors(PointType.SHOP, warehouse1, 431782L));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void lrmCreateAfterLomNotOnScSameWarehouse() {
        var warehouse = 123L;
        var lomOrder = createOrder("o1", "p2", warehouse, ReturnType.DROPOFF);
        assertThat(lomOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_CREATED_FF);
        assertThat(lomOrder.order().getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse));
        assertThat(lomOrder.order().getWarehouseReturn().getType()).isEqualTo(WarehouseType.DROPOFF);
        assertThat(lomOrder.order().getSegmentUid()).isNull();
        assertThat(lomOrder.order().getCargoUnitId()).isNull();

        assertThat(lomOrder.place("p2").getSortableStatus()).isEqualTo(SortableStatus.AWAITING_DIRECT);
        assertThat(lomOrder.place("p2").getStageId()).isEqualTo(Stages.AWAITING_DIRECT.getId());
        assertThat(lomOrder.place("p2").getWarehouseReturn()).isNotNull();
        assertThat(lomOrder.place("p2").getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse));
        assertThat(lomOrder.place("p2").getWarehouseReturn().getType()).isEqualTo(WarehouseType.DROPOFF);
        assertThat(lomOrder.place("p2").getSegmentUid()).isNull();
        assertThat(lomOrder.place("p2").getCargoUnitId()).isNull();

        // TODO: ORDER_CANCELLED_FF -> 160
        var lrmOrder = createCargoUnit(
                "o1", "p2", "cu1", "s1",
                PointType.DROPOFF, warehouse
        );
        assertThat(lrmOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_CANCELLED_FF);
        assertThat(lrmOrder.order().getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse));
        assertThat(lrmOrder.order().getWarehouseReturn().getType()).isEqualTo(WarehouseType.DROPOFF);
        assertThat(lrmOrder.order().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.order().getCargoUnitId()).isEqualTo("cu1");

        assertThat(lrmOrder.place().getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);
        assertThat(lrmOrder.place().getStageId()).isEqualTo(Stages.AWAITING_RETURN.getId());
        assertThat(lrmOrder.place().getWarehouseReturn()).isNotNull();
        assertThat(lrmOrder.place().getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse));
        assertThat(lrmOrder.place().getWarehouseReturn().getType()).isEqualTo(WarehouseType.DROPOFF);
        assertThat(lrmOrder.place().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.place().getCargoUnitId()).isEqualTo("cu1");
    }

    @Test
    public void lrmCreateAfterLomAcceptedSameWarehouse() {
        var warehouse = 123L;
        var lomOrder = createOrder(
                "o1", "p2", warehouse, ReturnType.SHOP,
                builder -> builder.acceptPlace("p2")
        );
        assertThat(lomOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);

        assertThat(lomOrder.place("p2").getSortableStatus()).isEqualTo(SortableStatus.ARRIVED_DIRECT);
        assertThat(lomOrder.place("p2").getStageId()).isEqualTo(Stages.FINAL_ACCEPT_DIRECT.getId());

        String warehouseReturnYandexId = lomOrder.order().getWarehouseReturn().getYandexId();
        var lrmOrder = createCargoUnit(
                "o1", "p2", "cu1", "s1",
                PointType.SHOP, Long.parseLong(Objects.requireNonNull(warehouseReturnYandexId))
        );
        assertThat(lrmOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        assertThat(lrmOrder.order().getWarehouseReturn().getYandexId()).isEqualTo(warehouseReturnYandexId);
        assertThat(lrmOrder.order().getWarehouseReturn().getType()).isEqualTo(WarehouseType.SHOP);
        assertThat(lrmOrder.order().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.order().getCargoUnitId()).isEqualTo("cu1");

        assertThat(lrmOrder.place().getSortableStatus()).isEqualTo(SortableStatus.ACCEPTED_RETURN);
        assertThat(lrmOrder.place().getStageId()).isEqualTo(Stages.FINAL_ACCEPT_RETURN.getId());
        assertThat(lrmOrder.place().getWarehouseReturn()).isNotNull();
        assertThat(lrmOrder.place().getWarehouseReturn().getYandexId()).isEqualTo(warehouseReturnYandexId);
        assertThat(lrmOrder.place().getWarehouseReturn().getType()).isEqualTo(WarehouseType.SHOP);
        assertThat(lrmOrder.place().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.place().getCargoUnitId()).isEqualTo("cu1");
    }

    @Test
    public void lrmCreateAfterLomSortedDirectSameWarehouse() {
        var warehouse = 123L;
        var lomOrder = createOrder(
                "o1", "p2", warehouse, ReturnType.DROPOFF,
                builder -> builder.updateShipmentDate(LocalDate.now(clock)).acceptPlace("p2").sortPlace("p2")
        );
        assertThat(lomOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);

        assertThat(lomOrder.place("p2").getSortableStatus()).isEqualTo(SortableStatus.SORTED_DIRECT);
        assertThat(lomOrder.place("p2").getStageId()).isEqualTo(Stages.SORTED_DIRECT.getId());

        List<StorageUnitResponseErrorDto> errors = createCargoUnitReturnResponse("o1", "p2", "cu1", "s1",
                PointType.SORTING_CENTER, Long.parseLong(
                        Objects.requireNonNull(lomOrder.order().getWarehouseReturn().getYandexId())))
                .getResult().getErrors();
        System.out.println(errors);
        assertThat(errors).isEqualTo(getSortedErrors(PointType.DROPOFF, warehouse, null));
    }

    @Test
    public void lrmCreateAfterLomCancelNotOnScSameWarehouse() {
        var warehouse = 123L;
        var lomOrder = createOrder(
                "o1", "p2", warehouse, ReturnType.DROPOFF,
                TestFactory.TestOrderBuilder::cancel
        );
        assertThat(lomOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_CANCELLED_FF);

        assertThat(lomOrder.place("p2").getSortableStatus()).isEqualTo(SortableStatus.CANCELLED);
        assertThat(lomOrder.place("p2").getStageId()).isEqualTo(Stages.CANCELLED.getId());

        // TODO: ORDER_CANCELLED_FF -> 160
        var lrmOrder = createCargoUnit(
                "o1", "p2", "cu1", "s1",
                PointType.DROPOFF, Long.parseLong(
                        Objects.requireNonNull(lomOrder.order().getWarehouseReturn().getYandexId()))
        );
        assertThat(lrmOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_CANCELLED_FF);
        assertThat(lrmOrder.order().getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse));
        assertThat(lrmOrder.order().getWarehouseReturn().getType()).isEqualTo(WarehouseType.DROPOFF);
        assertThat(lrmOrder.order().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.order().getCargoUnitId()).isEqualTo("cu1");

        assertThat(lrmOrder.place().getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);
        assertThat(lrmOrder.place().getStageId()).isEqualTo(Stages.AWAITING_RETURN.getId());
        assertThat(lrmOrder.place().getWarehouseReturn()).isNotNull();
        assertThat(lrmOrder.place().getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse));
        assertThat(lrmOrder.place().getWarehouseReturn().getType()).isEqualTo(WarehouseType.DROPOFF);
        assertThat(lrmOrder.place().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.place().getCargoUnitId()).isEqualTo("cu1");
    }

    @Test
    public void lrmCreateAfterLomCancelAcceptedSameWarehouse() {
        var warehouse = 123L;
        var lomOrder = createOrder(
                "o1", "p2", warehouse, ReturnType.DROPOFF,
                builder -> builder.acceptPlace("p2").cancel()
        );
        assertThat(lomOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);

        assertThat(lomOrder.place("p2").getSortableStatus()).isEqualTo(SortableStatus.ACCEPTED_RETURN);
        assertThat(lomOrder.place("p2").getStageId()).isEqualTo(Stages.FINAL_ACCEPT_RETURN.getId());

        var lrmOrder = createCargoUnit(
                "o1", "p2", "cu1", "s1",
                PointType.DROPOFF, Long.parseLong(
                        Objects.requireNonNull(lomOrder.order().getWarehouseReturn().getYandexId()))
        );
        assertThat(lrmOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        assertThat(lrmOrder.order().getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse));
        assertThat(lrmOrder.order().getWarehouseReturn().getType()).isEqualTo(WarehouseType.DROPOFF);
        assertThat(lrmOrder.order().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.order().getCargoUnitId()).isEqualTo("cu1");

        assertThat(lrmOrder.place().getSortableStatus()).isEqualTo(SortableStatus.ACCEPTED_RETURN);
        assertThat(lrmOrder.place().getStageId()).isEqualTo(Stages.FINAL_ACCEPT_RETURN.getId());
        assertThat(lrmOrder.place().getWarehouseReturn()).isNotNull();
        assertThat(lrmOrder.place().getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse));
        assertThat(lrmOrder.place().getWarehouseReturn().getType()).isEqualTo(WarehouseType.DROPOFF);
        assertThat(lrmOrder.place().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.place().getCargoUnitId()).isEqualTo("cu1");
    }

    @DisplayName("success lom отменил заказ, заказ приняли и отсортировали, lrm начал обновлять грузоместо." +
            "Статус заказа не должен измениться только проставиться сегмент")
    @Test
    public void successAfterShippedLrmUpdateSameWarehouse() {
        var warehouse = 123L;
        var lomOrder = createOrder(
                "o1", "p2", warehouse, ReturnType.DROPOFF,
                builder -> builder.acceptPlace("p2").cancel().acceptPlaces("p2").sortPlaces("p2").ship()
        );
        assertThat(lomOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(lomOrder.order().getSegmentUid()).isNull();

        assertThat(lomOrder.place("p2").getSortableStatus()).isEqualTo(SortableStatus.SHIPPED_RETURN);
        assertThat(lomOrder.place("p2").getStageId()).isEqualTo(Stages.SHIPPED_RETURN.getId());

        var lrmOrder = createCargoUnit(
                "o1", "p2", "cu1", "s1",
                PointType.DROPOFF, Long.parseLong(
                        Objects.requireNonNull(lomOrder.order().getWarehouseReturn().getYandexId()))
        );

        assertThat(lrmOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(lrmOrder.order().getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse));
        assertThat(lrmOrder.order().getWarehouseReturn().getType()).isEqualTo(WarehouseType.DROPOFF);
        assertThat(lrmOrder.order().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.order().getCargoUnitId()).isEqualTo("cu1");

        assertThat(lrmOrder.place().getSortableStatus()).isEqualTo(SortableStatus.SHIPPED_RETURN);
        assertThat(lrmOrder.place().getStageId()).isEqualTo(Stages.SHIPPED_RETURN.getId());
        assertThat(lrmOrder.place().getWarehouseReturn()).isNotNull();
        assertThat(lrmOrder.place().getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse));
        assertThat(lrmOrder.place().getWarehouseReturn().getType()).isEqualTo(WarehouseType.DROPOFF);
        assertThat(lrmOrder.place().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.place().getCargoUnitId()).isEqualTo("cu1");
    }

    @Test
    @DisplayName("success успешная отмена двух посылок в статусах p1 = ARRIVED_DIRECT и p2 = AWAITING_DIRECT; " +
            "у p1 обновляются сегменты у p2 нет, только меняется статус")
    public void successCancelTwoParcel() {
        var warehouse = 123L;
        var lomOrder = createOrder(
                "o1", List.of("p1", "p2"), warehouse, ReturnType.DROPOFF,
                builder -> builder.acceptPlace("p1")
        );

        assertThat(lomOrder.place("p1").getSortableStatus()).isEqualTo(SortableStatus.ARRIVED_DIRECT);
        assertThat(lomOrder.place("p2").getSortableStatus()).isEqualTo(SortableStatus.AWAITING_DIRECT);

        var lrmOrder = createCargoUnit(
                "o1", "p1", "cu1", "s1",
                PointType.DROPOFF, Long.parseLong(
                        Objects.requireNonNull(lomOrder.order().getWarehouseReturn().getYandexId()))
        );

        var o = testFactory.findOrder("o1", sortingCenter);
        var p1 = testFactory.orderPlace(o, "p1");
        var p2 = testFactory.orderPlace(o, "p2");

        assertThat(lrmOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);

        assertThat(p1.getSortableStatus()).isEqualTo(SortableStatus.ACCEPTED_RETURN);
        assertThat(p1.getSegmentUid()).isEqualTo("s1");
        assertThat(p1.getCargoUnitId()).isEqualTo("cu1");

        assertThat(p2.getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);
        assertThat(p2.getSegmentUid()).isNull();
        assertThat(p2.getCargoUnitId()).isNull();
        assertThat(p1.getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse));
        assertThat(p1.getWarehouseReturn().getType()).isEqualTo(WarehouseType.DROPOFF);
    }

    @DisplayName("fail отмена не проходит тк одна из коробок отсортированна, статусы не изменяются ")
    @Test
    public void failCancelSortedPlace() {
        var warehouse = 123L;
        var lomOrder = createOrder("o1", List.of("p1", "p2"), warehouse, ReturnType.DROPOFF,
                builder -> builder.acceptPlace("p1")
        );

        testFactory.acceptPlace(lomOrder.order(), "p1");
        testFactory.acceptPlace(lomOrder.order(), "p2")
                .sortPlace(lomOrder.order(), "p2");

        var o = testFactory.findOrder("o1", sortingCenter);
        var p1 = testFactory.orderPlace(o, "p1");
        var p2 = testFactory.orderPlace(o, "p2");

        assertThat(p1.getSortableStatus()).isEqualTo(SortableStatus.ARRIVED_DIRECT);
        assertThat(p2.getSortableStatus()).isEqualTo(SortableStatus.SORTED_DIRECT);

        var lrmOrder = createCargoUnit(
                "o1", "p1", "cu1", "s1",
                PointType.DROPOFF, Long.parseLong(
                        Objects.requireNonNull(lomOrder.order().getWarehouseReturn().getYandexId()))
        );

         o = testFactory.findOrder("o1", sortingCenter);
         p1 = testFactory.orderPlace(o, "p1");
         p2 = testFactory.orderPlace(o, "p2");

        assertThat(p1.getSortableStatus()).isEqualTo(SortableStatus.ARRIVED_DIRECT);
        assertThat(p1.getSegmentUid()).isNull();
        assertThat(p1.getCargoUnitId()).isNull();

        assertThat(p2.getSortableStatus()).isEqualTo(SortableStatus.SORTED_DIRECT);
        assertThat(p2.getSegmentUid()).isNull();
        assertThat(p2.getCargoUnitId()).isNull();
        assertThat(p1.getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse));
        assertThat(p1.getWarehouseReturn().getType()).isEqualTo(WarehouseType.DROPOFF);
    }

    @Test
    @DisplayName("success успешная отмена двух посылок в статусах p1 = ARRIVED_DIRECT и p2 = SHIPPED_DIRECT; " +
            "у p1 обновляются сегменты у p2 нет, только меняется статус")
    public void successCancelTwoParcel2() {
        var warehouse = 123L;
        var lomOrder = createOrder("o1", List.of("p1", "p2"), warehouse, ReturnType.DROPOFF,
                builder -> builder.acceptPlace("p1")
        );

        testFactory.acceptPlace(lomOrder.order(), "p1");
        testFactory.acceptPlace(lomOrder.order(), "p2")
                .sortPlace(lomOrder.order(), "p2")
                .shipPlace(lomOrder.order(), "p2");

        var o = testFactory.findOrder("o1", sortingCenter);
        var p1 = testFactory.orderPlace(o, "p1");
        var p2 = testFactory.orderPlace(o, "p2");

        assertThat(p1.getSortableStatus()).isEqualTo(SortableStatus.ARRIVED_DIRECT);
        assertThat(p2.getSortableStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);

        var lrmOrder = createCargoUnit(
                "o1", "p1", "cu1", "s1",
                PointType.DROPOFF, Long.parseLong(
                        Objects.requireNonNull(lomOrder.order().getWarehouseReturn().getYandexId()))
        );

         o = testFactory.findOrder("o1", sortingCenter);
         p1 = testFactory.orderPlace(o, "p1");
         p2 = testFactory.orderPlace(o, "p2");

        assertThat(p1.getSortableStatus()).isEqualTo(SortableStatus.ACCEPTED_RETURN);
        assertThat(p1.getSegmentUid()).isEqualTo("s1");
        assertThat(p1.getCargoUnitId()).isEqualTo("cu1");

        assertThat(p2.getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);
        assertThat(p2.getSegmentUid()).isNull();
        assertThat(p2.getCargoUnitId()).isNull();
        assertThat(p1.getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse));
        assertThat(p1.getWarehouseReturn().getType()).isEqualTo(WarehouseType.DROPOFF);
    }

    @Test
    public void lrmCreateAfterLomSortedToBufferSameWarehouse() {

    }

    @Test
    public void lrmCreateAfterLomSortedToReturnSameWarehouse() {
        var warehouse = 123L;
        var lomOrder = createOrder(
                "o1", "p2", warehouse, ReturnType.DROPOFF,
                builder -> builder.acceptPlace("p2").cancel().sortPlace("p2")
        );
        assertThat(lomOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(lomOrder.order().getSegmentUid()).isNull();
        assertThat(lomOrder.order().getCargoUnitId()).isNull();

        assertThat(lomOrder.place("p2").getSortableStatus()).isEqualTo(SortableStatus.SORTED_RETURN);
        assertThat(lomOrder.place("p2").getStageId()).isEqualTo(Stages.SORTED_RETURN.getId());
        assertThat(lomOrder.place("p2").getSegmentUid()).isNull();
        assertThat(lomOrder.place("p2").getCargoUnitId()).isNull();

        var lrmOrder = createCargoUnit(
                "o1", "p2", "cu1", "s1",
                PointType.DROPOFF, Long.parseLong(
                        Objects.requireNonNull(lomOrder.order().getWarehouseReturn().getYandexId()))
        );
        assertThat(lrmOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(lrmOrder.order().getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse));
        assertThat(lrmOrder.order().getWarehouseReturn().getType()).isEqualTo(WarehouseType.DROPOFF);
        assertThat(lrmOrder.order().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.order().getCargoUnitId()).isEqualTo("cu1");

        assertThat(lrmOrder.place().getSortableStatus()).isEqualTo(SortableStatus.SORTED_RETURN);
        assertThat(lrmOrder.place().getStageId()).isEqualTo(Stages.SORTED_RETURN.getId());
        assertThat(lrmOrder.place().getWarehouseReturn()).isNotNull();
        assertThat(lrmOrder.place().getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(warehouse));
        assertThat(lrmOrder.place().getWarehouseReturn().getType()).isEqualTo(WarehouseType.DROPOFF);
        assertThat(lrmOrder.place().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.place().getCargoUnitId()).isEqualTo("cu1");
    }

    @Test
    public void lrmCreateAfterLomSortedToLotSameWarehouse() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, true);
        var warehouse = 123L;
        var lomOrder = createOrder(
                "o1", "p2", warehouse, ReturnType.SHOP,
                builder -> builder.acceptPlace("p2").cancel().keepPlaces("p2")
        );
        assertThat(lomOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);

        assertThat(lomOrder.place("p2").getSortableStatus()).isEqualTo(SortableStatus.KEEPED_RETURN);
        assertThat(lomOrder.place("p2").getStageId()).isEqualTo(Stages.KEEPED_RETURN.getId());
        String warehouseReturnYandexId = lomOrder.order().getWarehouseReturn().getYandexId();

        var lrmOrder = createCargoUnit(
                "o1", "p2", "cu1", "s1",
                PointType.SHOP, Long.parseLong(Objects.requireNonNull(warehouseReturnYandexId))
        );
        assertThat(lrmOrder.order().getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(lrmOrder.order().getWarehouseReturn().getYandexId()).isEqualTo(warehouseReturnYandexId);
        assertThat(lrmOrder.order().getWarehouseReturn().getType()).isEqualTo(WarehouseType.SHOP);
        assertThat(lrmOrder.order().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.order().getCargoUnitId()).isEqualTo("cu1");

        assertThat(lrmOrder.place().getSortableStatus()).isEqualTo(SortableStatus.KEEPED_RETURN);
        assertThat(lrmOrder.place().getStageId()).isEqualTo(Stages.KEEPED_RETURN.getId());
        assertThat(lrmOrder.place().getWarehouseReturn()).isNotNull();
        assertThat(lrmOrder.place().getWarehouseReturn().getYandexId()).isEqualTo(warehouseReturnYandexId);
        assertThat(lrmOrder.place().getWarehouseReturn().getType()).isEqualTo(WarehouseType.SHOP);
        assertThat(lrmOrder.place().getSegmentUid()).isEqualTo("s1");
        assertThat(lrmOrder.place().getCargoUnitId()).isEqualTo("cu1");
    }

    @SuppressWarnings("SameParameterValue")
    private ScOrderWithPlaces createOrder(String externalOrderId, String placeBarcode,
                                          long warehouseReturnYandexId, ReturnType returnType) {
        return createOrder(externalOrderId, placeBarcode, warehouseReturnYandexId, returnType, (b) -> {
        });
    }

    private ScOrderWithPlaces createOrder(String externalOrderId, String placeBarcode,
                                          long warehouseReturnYandexId, ReturnType returnType,
                                          Consumer<TestFactory.TestOrderBuilder> action) {
        var builder = testFactory.create(
                order(sortingCenter)
                        .externalId(externalOrderId)
                        .places(placeBarcode)
                        .warehouseReturnId(String.valueOf(warehouseReturnYandexId))
                        .warehouseReturnType(returnType)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build()
        );
        action.accept(builder);
        return builder.getOrderWithPlaces();
    }
    private ScOrderWithPlaces createOrder(String externalOrderId, List<String> placeBarcodes,
                                          long warehouseReturnYandexId, ReturnType returnType,
                                          Consumer<TestFactory.TestOrderBuilder> action) {
        var builder = testFactory.create(
                order(sortingCenter)
                        .externalId(externalOrderId)
                        .places(placeBarcodes)
                        .warehouseReturnId(String.valueOf(warehouseReturnYandexId))
                        .warehouseReturnType(returnType)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build()
        );
        action.accept(builder);
        return builder.getOrderWithPlaces();
    }

    @SuppressWarnings("SameParameterValue")
    private ScOrderWithPlace createCargoUnit(String externalOrderId, String placeBarcode,
                                             String cargoUnitId, String segmentUid,
                                             PointType pointType,
                                             long warehouseReturnYandexId) {
        return cargoUnitTestFactory.createCargoUnitFromOrder(
                sortingCenter,
                externalOrderId, placeBarcode, cargoUnitId, segmentUid,
                LesModelFactory.createPointDto(
                        pointType, warehouseReturnYandexId,
                        pointType == PointType.SHOP ? warehouseReturnYandexId : null,
                        "wh1"
                )
        ).orderWithPlace();
    }

    @SuppressWarnings("SameParameterValue")
    private StorageUnitCreateResponseEvent createCargoUnitReturnResponse(String externalOrderId, String placeBarcode,
                                                                         String cargoUnitId, String segmentUid,
                                                                         PointType pointType,
                                                                         long warehouseReturnYandexId) {
        return (StorageUnitCreateResponseEvent) cargoUnitTestFactory.createCargoUnitFromOrder(
                sortingCenter,
                externalOrderId, placeBarcode, cargoUnitId, segmentUid,
                LesModelFactory.createPointDto(
                        pointType, warehouseReturnYandexId,
                        pointType == PointType.SHOP ? warehouseReturnYandexId : null,
                        "wh1"
                )
        ).response();
    }

}
