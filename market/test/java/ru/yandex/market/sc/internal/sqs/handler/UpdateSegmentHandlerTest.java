package ru.yandex.market.sc.internal.sqs.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.dto.PointType;
import ru.yandex.market.logistics.les.tpl.StorageUnitUpdateSegmentResponseEvent;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.stage.Stages;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.ScOrderWithPlace;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.AbstractBaseIntTest;
import ru.yandex.market.sc.internal.test.CargoUnitTestFactory;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;
import ru.yandex.market.sc.internal.util.les.LesModelFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author: dbryndin
 * @date: 5/26/22
 */
@EmbeddedDbIntTest
public class UpdateSegmentHandlerTest extends AbstractBaseIntTest {

    @Autowired
    CargoUnitTestFactory cargoUnitTestFactory;

    private SortingCenter sortingCenter;
    private User user;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, TestFactory.USER_UID_LONG);
    }

    @Test
    @DisplayName("success обновление склада возврата")
    public void successUpdateWH() {
        var wh = testFactory.storedWarehouse("111222");
        var externalOrderId = "o1";
        var placeBarcode = "p2";
        var cargoUnitId = "cu1";
        var lrmOrder = createCargoUnit(externalOrderId, placeBarcode, cargoUnitId, "s1",
                PointType.SORTING_CENTER, Long.parseLong(wh.getYandexId())
        );
        {
            var place = testFactory.cancelOrder(lrmOrder.order())
                    .acceptPlace(lrmOrder.order(), placeBarcode)
                    .sortPlace(lrmOrder.order(), placeBarcode)
                    .getPlace(lrmOrder.place().getId());
            assertThat(place).isNotNull();
            assertThat(place.getSortableStatus()).isEqualTo(SortableStatus.SORTED_RETURN);
            assertThat(place.getWarehouseReturn().getYandexId()).isEqualTo(wh.getYandexId());
        }
        long whReturnId = 99L;
        var cargoUpdateSegment = cargoUnitTestFactory.createCargoUpdateSegment(sortingCenter, cargoUnitId,
                externalOrderId, placeBarcode,
                "s2", LesModelFactory.createPointDto(PointType.SORTING_CENTER, whReturnId,
                        null, "vsc"));

        {
            var place = testFactory.getPlace(lrmOrder.place().getId());
            assertThat(place.getWarehouseReturn().getYandexId()).isEqualTo(String.valueOf(whReturnId));
            assertThat(place.getSortableStatus()).isEqualTo(SortableStatus.ACCEPTED_RETURN);
            assertThat(place.getStageId()).isEqualTo(Stages.AWAITING_SORT_RETURN.getId());
        }
    }

    @Test
    @DisplayName("fail коробка в лоте не можем обновить склад")
    public void failUpdateWHInLot() {
        var wh = testFactory.storedWarehouse("111222");
        var externalOrderId = "o1";
        var placeBarcode = "p2";
        var cargoUnitId = "cu1";
        var lrmOrder = createCargoUnit(externalOrderId, placeBarcode, cargoUnitId, "s1",
                PointType.SORTING_CENTER, Long.parseLong(wh.getYandexId())
        );
        {
            var place = testFactory.cancelOrder(lrmOrder.order())
                    .acceptPlace(lrmOrder.order(), placeBarcode)
                    .sortPlace(lrmOrder.order(), placeBarcode)
                    .getPlace(lrmOrder.place().getId());
            assertThat(place).isNotNull();
            assertThat(place.getSortableStatus()).isEqualTo(SortableStatus.SORTED_RETURN);
            assertThat(place.getWarehouseReturn().getYandexId()).isEqualTo(wh.getYandexId());
        }
        var place = testFactory.getPlace(lrmOrder.place().getId());
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(place)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, place);
        var lot = testFactory.storedLot(sortingCenter, cell, LotStatus.CREATED);

        testFactory.sortToLot(place, lot, user);
        long whReturnId = 99L;
        var response = cargoUnitTestFactory.createCargoUpdateSegment(sortingCenter, cargoUnitId,
                externalOrderId, placeBarcode,
                "s2", LesModelFactory.createPointDto(PointType.SORTING_CENTER, whReturnId,
                        null, "vsc"));
        var result = ((StorageUnitUpdateSegmentResponseEvent) response.response()).getResult();
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors().get(0).getDetails().getActualDestination().getId())
                .isEqualTo(Long.valueOf(wh.getLogisticPointId()));

        {
            var place0 = testFactory.getPlace(lrmOrder.place().getId());
            assertThat(place0.getWarehouseReturn().getYandexId()).isEqualTo(wh.getYandexId());
            assertThat(place0.getSortableStatus()).isEqualTo(SortableStatus.SORTED_RETURN);
        }
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

}
