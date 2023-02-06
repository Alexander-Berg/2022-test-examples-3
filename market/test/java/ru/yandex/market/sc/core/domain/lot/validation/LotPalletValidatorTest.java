package ru.yandex.market.sc.core.domain.lot.validation;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.route_so.Routable;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.exception.ScException;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;

import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LotPalletValidatorTest {

    private final TestFactory testFactory;
    private final LotPalletValidator lotPalletValidator;
    private final JdbcTemplate jdbcTemplate;

    SortingCenter sortingCenter;
    Cell parentCell;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter(13L);
        parentCell = testFactory.storedCell(sortingCenter, "r1", CellType.RETURN);
    }

    @Test
    @DisplayName("[Одноместный заказ] Лот связан с другой ячейкой")
    void validateOrderCanBeSortedToLotWhenIncorrectParentCell() {
        Place p1 = testFactory.createForToday(order(sortingCenter, "o1").build())
                .accept()
                .sort()
                .getPlace();
        Cell c2 = testFactory.storedCell(sortingCenter, "c2", CellType.COURIER);
        SortableLot l1 = testFactory.storedLot(sortingCenter, c2, LotStatus.PROCESSING);
        Route route = testFactory.findOutgoingCourierRoute(p1).orElseThrow();
        Routable routable = testFactory.getRoutable(route);
//        Routable routable = testFactory.chooseRoutable(route, testFactory.getRouteSo(route));

        Assertions.assertThatThrownBy(() -> lotPalletValidator.validate(p1, l1, routable))
                .isInstanceOf(ScException.class)
                .hasFieldOrPropertyWithValue("code", ScErrorCode.LOT_PARENT_CELL_FROM_ANOTHER_ROUTE.name());
    }

    @Test
    @DisplayName("[Одноместный заказ] Лот в некорректном состоянии")
    void validateOrderCanBeSortedToLotWhenLotIncorrectState() {
        Place p1 = testFactory.createForToday(order(sortingCenter, "o1").build())
                .accept()
                .sort()
                .getPlace();
        SortableLot l1 = testFactory.storedLot(sortingCenter, p1.getCell(), LotStatus.READY);
        Route route = testFactory.findOutgoingCourierRoute(p1).orElseThrow();
        Routable routable = testFactory.getRoutable(route);
//        Routable routable = testFactory.chooseRoutable(route, testFactory.getRouteSo(route));

        Assertions.assertThatThrownBy(() -> lotPalletValidator.validate(p1, l1, routable))
                .isInstanceOf(ScException.class)
                .hasFieldOrPropertyWithValue("code", ScErrorCode.LOT_INVALID_STATE.name());
    }

    @Test
    @DisplayName("[Одноместный заказ] Посылка в некорректном состоянии")
    void validatePlaceCantBeSortedToLotWhenPlaceIncorrectState() {
        Place p1 = testFactory.createForToday(order(sortingCenter, "o1").build())
                .accept()
                .sort()
                .getPlace();
        SortableLot l1 = testFactory.storedLot(sortingCenter, p1.getCell(), LotStatus.PROCESSING);
        Route route = testFactory.findOutgoingCourierRoute(p1).orElseThrow();
        SortableStatus oldStatus = p1.getSortableStatus();
        jdbcTemplate.update("UPDATE place SET sortable_status = ? WHERE id = ?", SortableStatus.AWAITING_DIRECT.name(),
                p1.getId());

        Place finalP1 = testFactory.updated(p1);
        Routable routable = testFactory.getRoutable(route);
//        Routable routable = testFactory.chooseRoutable(route, testFactory.getRouteSo(route));

        Assertions.assertThatThrownBy(() -> lotPalletValidator.validate(finalP1, l1, routable))
                .isInstanceOf(TplInvalidActionException.class)
                .hasMessage("Can't sort place %s to lot %s: place in status %s.", finalP1, l1,
                        SortableStatus.AWAITING_DIRECT.name());

        //Возвращаем все как было, чтобы SortableFlowSwitcher смог проверить маршруты и ячейки
        jdbcTemplate.update("UPDATE place SET sortable_status = ? WHERE id = ?", oldStatus.name(),
                p1.getId());

    }

    @Test
    @DisplayName("[Многоместный заказ] Лот связан с другой ячейкой")
    void validatePlaceCanBeSortedToLotWhenIncorrectParentCell() {
        ScOrder o1 = testFactory.createForToday(order(sortingCenter, "o1").places("p1", "p2").build())
                .acceptPlaces("p1", "p2").sortPlaces("p1", "p2").get();
        Cell c2 = testFactory.storedCell(sortingCenter, "c2", CellType.COURIER);
        SortableLot l1 = testFactory.storedLot(sortingCenter, c2, LotStatus.PROCESSING);
        Route route = testFactory.findOutgoingCourierRoute(o1).orElseThrow();
        Place p1 = testFactory.orderPlace(o1, "p1");
        Routable routable = testFactory.getRoutable(route);
//        Routable routable = testFactory.chooseRoutable(route, testFactory.getRouteSo(route));

        Assertions.assertThatThrownBy(() -> lotPalletValidator.validate(p1, l1, routable))
                .isInstanceOf(ScException.class)
                .hasFieldOrPropertyWithValue("code", ScErrorCode.LOT_PARENT_CELL_FROM_ANOTHER_ROUTE.name());
    }

    @Test
    @DisplayName("[Многоместный заказ] Лот в некорректном состоянии")
    void validatePlaceCanBeSortedToLotWhenLotIncorrectState() {
        ScOrder o1 = testFactory.createForToday(order(sortingCenter, "o1").places("p1", "p2").build())
                .acceptPlaces("p1", "p2").sortPlaces("p1", "p2").get();
        Place p1 = testFactory.orderPlace(o1, "p1");
        SortableLot l1 = testFactory.storedLot(sortingCenter, p1.getCell(), LotStatus.READY);
        Route route = testFactory.findOutgoingCourierRoute(p1).orElseThrow();
        Routable routable = testFactory.getRoutable(route);
//        Routable routable = testFactory.chooseRoutable(route, testFactory.getRouteSo(route));

        Assertions.assertThatThrownBy(() -> lotPalletValidator.validate(p1, l1, routable))
                .isInstanceOf(ScException.class)
                .hasFieldOrPropertyWithValue("code", ScErrorCode.LOT_INVALID_STATE.name());
    }

    @Test
    @DisplayName("[Многоместный заказ] Заказ в некорректном состоянии")
    void validatePlaceCanBeSortedToLotWhenPlaceIncorrectState() {
        ScOrder o1 = testFactory.createForToday(order(sortingCenter, "o1").places("p1", "p2").build())
                .acceptPlaces("p1", "p2").sortPlaces("p1", "p2").get();
        Place p1 = testFactory.orderPlace(o1, "p1");
        SortableLot l1 = testFactory.storedLot(sortingCenter, p1.getCell(), LotStatus.PROCESSING);
        Route route = testFactory.findOutgoingCourierRoute(p1).orElseThrow();
        SortableStatus oldStatus = p1.getSortableStatus();
        String status = SortableStatus.SHIPPED_RETURN.name();
        jdbcTemplate.update("UPDATE place SET sortable_status = ? WHERE id = ?", status, p1.getId());
        Routable routable = testFactory.getRoutable(route);
//        Routable routable = testFactory.chooseRoutable(route, testFactory.getRouteSo(route));

        Place actualP1 = testFactory.updated(p1);
        Assertions.assertThatThrownBy(() -> lotPalletValidator.validate(actualP1, l1, routable))
                .isInstanceOf(TplInvalidActionException.class)
                .hasMessage("Can't sort place %s to lot %s: place in status %s.", actualP1, l1,
                        status);

        //Возвращаем все как было, чтобы SortableFlowSwitcher смог проверить маршруты и ячейки
        jdbcTemplate.update("UPDATE place SET sortable_status = ? WHERE id = ?", oldStatus.name(),
                p1.getId());

    }
}
