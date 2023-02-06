package ru.yandex.market.sc.core.domain.place.repository;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.order.repository.ScOrderRoutes;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author valter
 */
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PlaceRepositoryTest {

    private final PlaceRepository placeRepository;
    private final TestFactory testFactory;
    private final Clock clock;

    @Test
    void entityGraphHasParentCellId() {
        var saved = testFactory.createOrderForToday(testFactory.storedSortingCenter())
                .accept().sort().sortToLot()
                .getPlace();
        var place = placeRepository.findByIdOrThrow(saved.getId());
        assertThat(place.getParent()).isNotNull();
        assertThat(place.getParent().getCellIdOrNull()).isNotNull();
    }

    @Test
    void save() {
        var expected = testFactory.place();
        var actual = placeRepository.save(expected);
        assertThat(actual).isEqualToIgnoringGivenFields(expected, "id", "createdAt", "updatedAt");
    }

    @Test
    void saveWithoutKorobyte() {
        var expected = testFactory.place(testFactory.storedSortingCenter(), false);
        var actual = placeRepository.save(expected);
        assertThat(actual).isEqualToIgnoringGivenFields(expected, "id", "createdAt", "updatedAt");
    }

    @Test
    void setHistoryWithUser() {
        var sortingCenter = testFactory.storedSortingCenter();
        var user = testFactory.storedUser(sortingCenter, 1L);
        var place = testFactory.place(sortingCenter);
        var cell = testFactory.storedCell(sortingCenter);
        place.setSiteAndStatus(cell, SortableStatus.ARRIVED_DIRECT, PlaceStatus.ACCEPTED, user,
                Instant.now(clock));
        place = placeRepository.save(place);
        assertThat(place.getHistory().stream().map(PlaceHistory::getUser).toList())
                .isEqualTo(Arrays.asList(user, user));
    }


    @Test
    void incomingCourierRoute() {
        var sortingCenter = testFactory.storedSortingCenter();
        var order = testFactory.createOrderForToday(sortingCenter).accept().sort().ship().makeReturn().get();
        var incomingCourierRoute =
                testFactory.findPossibleIncomingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        Place place = testFactory.orderPlace(order);

        ScOrderRoutes actual = placeRepository.findPlaceRoutes(place.getId());
        assertThat(placeRepository.findByIdOrThrow(actual.getId())).isEqualTo(place);

        assertThat(actual.getIncomingCourierRouteId()).isEqualTo(incomingCourierRoute.allowNextRead().getId());
        assertThat(actual.getCourierFromId()).isEqualTo(
                Objects.requireNonNull(incomingCourierRoute.allowNextRead().getCourierFrom()).getId());

    }

    @Test
    void outgoingCourierRoute() {
        var sortingCenter = testFactory.storedSortingCenter();
        var order = testFactory.createOrderForToday(sortingCenter).get();
        var outgoingCourierRoute =
                testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        Place place = testFactory.orderPlace(order);

        ScOrderRoutes actual = placeRepository.findPlaceRoutes(place.getId());
        assertThat(placeRepository.findByIdOrThrow(actual.getId())).isEqualTo(place);
        assertThat(actual.getOutgoingCourierRouteId()).isEqualTo(outgoingCourierRoute.allowNextRead().getId());
        assertThat(actual.getCourierToId()).isEqualTo(
                Objects.requireNonNull(outgoingCourierRoute.allowNextRead().getCourierTo()).getId());
    }

    @Test
    void incomingWarehouseRoute() {
        var sortingCenter = testFactory.storedSortingCenter();
        var order = testFactory.createOrderForToday(sortingCenter).get();
        var incomingWarehouseRoute =
                testFactory.findPossibleIncomingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        Place place = testFactory.orderPlace(order);

        ScOrderRoutes actual = placeRepository.findPlaceRoutes(place.getId());
        assertThat(placeRepository.findByIdOrThrow(actual.getId())).isEqualTo(place);
        assertThat(actual.getIncomingWarehouseRouteId()).isEqualTo(incomingWarehouseRoute.allowNextRead().getId());
        assertThat(actual.getWarehouseFromId()).isEqualTo(
                Objects.requireNonNull(incomingWarehouseRoute.allowNextRead().getWarehouseFrom()).getId());
    }

    @Test
    void outgoingWarehouseRoute() {
        var sortingCenter = testFactory.storedSortingCenter();
        var order = testFactory.createOrderForToday(sortingCenter).accept().sort().ship().makeReturn().get();
        var outgoingWarehouseRoute =
                testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        Place place = testFactory.orderPlace(order);

        ScOrderRoutes actual = placeRepository.findPlaceRoutes(place.getId());
        assertThat(placeRepository.findByIdOrThrow(actual.getId())).isEqualTo(place);
        assertThat(actual.getOutgoingWarehouseRouteId()).isEqualTo(outgoingWarehouseRoute.allowNextRead().getId());
        assertThat(actual.getWarehouseToId()).isEqualTo(
                Objects.requireNonNull(outgoingWarehouseRoute.allowNextRead().getWarehouseTo()).getId());
    }

}
