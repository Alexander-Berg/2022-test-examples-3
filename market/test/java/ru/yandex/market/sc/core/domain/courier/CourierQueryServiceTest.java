package ru.yandex.market.sc.core.domain.courier;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.sc.core.domain.courier.model.ApiCourierDto;
import ru.yandex.market.sc.core.domain.courier.model.PartnerCourierDto;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author hardlight
 */
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CourierQueryServiceTest {
    private final TestFactory testFactory;
    private final CourierQueryService courierQueryService;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    void findCouriersByNameContaining() {
        testFactory.storedCourier(1L, "Ваня");
        testFactory.storedCourier(2L, "Петя");
        testFactory.storedCourier(3L, "Миша");
        testFactory.storedCourier(4L, "Маша");
        testFactory.storedCourier(5L, "Шарманка");

        var courierNames = courierQueryService.findCouriersByNameContaining("", PageRequest.of(0, 2))
                .stream()
                .map(ApiCourierDto::getName)
                .toList();
        assertThat(courierNames).containsExactly("Ваня", "Маша");
        courierNames = courierQueryService.findCouriersByNameContaining("", PageRequest.of(1, 2))
                .stream()
                .map(ApiCourierDto::getName)
                .toList();
        assertThat(courierNames).containsExactly("Миша", "Петя");

        courierNames = courierQueryService.findCouriersByNameContaining(" шА ", Pageable.unpaged())
                .stream()
                .map(ApiCourierDto::getName)
                .toList();
        assertThat(courierNames).containsExactly("Маша", "Миша", "Шарманка");
    }

    @Test
    void getListOfMidMilesCouriers() {
        testFactory.storedCourier(1L);
        var midMileCourier = testFactory.storedCourier(2L, 1L);
        testFactory.createOrderForToday(sortingCenter)
                .updateCourier(midMileCourier);

        var courierList = courierQueryService.getMidMilesCouriers(sortingCenter);
        assertThat(courierList.size()).isEqualTo(1);
        assertThat(courierList.stream().map(PartnerCourierDto::getId))
                .containsOnly(midMileCourier.getId());
    }

}
