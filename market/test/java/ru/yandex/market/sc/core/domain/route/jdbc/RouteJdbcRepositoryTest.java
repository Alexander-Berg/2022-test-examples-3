package ru.yandex.market.sc.core.domain.route.jdbc;

import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author mors741
 */
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class RouteJdbcRepositoryTest {

    private final TestFactory testFactory;
    private final RouteJdbcRepository routeJdbcRepository;
    private SortingCenter sortingCenter;
    @MockBean
    Clock clock;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setupMockClock(clock);
    }

    @Test
    @DisplayName("Проверяем, что при бронировании ячейки на уже занятую дату мы тихо игнорируем занятые даты")
    // Это нужно для сортировки до 12 https://st.yandex-team.ru/OPSPROJECT-132
    void testIntersectedRouteCellBooking() {
        LocalDate today = LocalDate.now(clock);

        Courier courier1 = testFactory.storedCourier(641L);
        Courier courier2 = testFactory.storedCourier(310L);
        Cell cell = testFactory.storedCell(sortingCenter, "cell-2-courier", CellType.COURIER);

        // создали 2 разных маршрута
        Route route1 = testFactory.storedOutgoingCourierRoute(sortingCenter, cell, courier1, today, today)
                .allowReading();
        Route route2 = testFactory.storedOutgoingCourierRoute(
                sortingCenter, cell, courier2, today.plusDays(1), today.plusDays(1)).allowReading();

        // первому привязали ячейку на сегодня
        routeJdbcRepository.setCellId(route1.getId(), cell.getId(), today, today);
        // второму пытаемся привязать эту же ячейку на сегодня и на завтра
        routeJdbcRepository.setCellId(route2.getId(), cell.getId(), today, today.plusDays(1));

        Route actualRoute1 = testFactory.getRoute(route1.getId());
        Route actualRoute2 = testFactory.getRoute(route2.getId());

        // проверяем, что setCellId() на занятую ячейку на сегодня не упал
        // и что бронь на сегодня осталась за первым маршрутом, а не перетёрлась вторым
        assertThat(actualRoute1.getCells(today)).hasSize(1);
        assertThat(actualRoute2.getCells(today)).isEmpty();
        assertThat(actualRoute2.getCells(today.plusDays(1))).hasSize(1);
    }
}
