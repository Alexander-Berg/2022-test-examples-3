package ru.yandex.market.sc.core.domain.route.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbTest
public class InAdvanceReserveRepositoryTest {


    @Autowired
    InAdvanceReserveRepository inAdvanceReserveRepository;
    @Autowired
    TestFactory testFactory;

    @Test
    void save() {
        var sortingCenter = testFactory.storedSortingCenter();
        var cell = testFactory.storedCell(sortingCenter);
        var deliveryService = testFactory.storedDeliveryService();
        var expected = new InAdvanceReserve(cell.getId(),
                deliveryService.getYandexId(),
                sortingCenter, false);
        assertThat(inAdvanceReserveRepository.save(expected)).isEqualTo(expected);
    }

    @Test
    void findBySortingCenterAndStatus() {
        var sortingCenter = testFactory.storedSortingCenter();
        var cell = testFactory.storedCell(sortingCenter);
        var deliveryService = testFactory.storedDeliveryService();
        var expected = new InAdvanceReserve(cell.getId(),
                deliveryService.getYandexId(),
                sortingCenter,  false);
        inAdvanceReserveRepository.save(expected);
        var reserveList = inAdvanceReserveRepository
                .findAllBySortingCenterAndReserveStatusAndRouteSoFlow(
                        sortingCenter, ReserveStatus.READY, false);
        assertThat(reserveList).hasSize(1);
    }

}
