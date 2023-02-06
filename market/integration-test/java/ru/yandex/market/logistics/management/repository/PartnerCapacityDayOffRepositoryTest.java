package ru.yandex.market.logistics.management.repository;

import java.time.LocalDate;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.PartnerCapacity;
import ru.yandex.market.logistics.management.domain.entity.PartnerCapacityDayOff;
import ru.yandex.market.logistics.management.util.CleanDatabase;

@CleanDatabase
class PartnerCapacityDayOffRepositoryTest extends AbstractContextualTest {

    private static final PartnerCapacity CAPACITY = new PartnerCapacity().setId(1L);

    private static final LocalDate APRIL_30 = LocalDate.of(2019, 4, 30);
    private static final LocalDate MAY_1 = LocalDate.of(2019, 5, 1);
    private static final LocalDate MAY_2 = LocalDate.of(2019, 5, 2);
    private static final LocalDate MAY_3 = LocalDate.of(2019, 5, 3);
    private static final LocalDate MAY_4 = LocalDate.of(2019, 5, 4);

    @Autowired
    private PartnerCapacityDayOffRepository partnerCapacityDayOffRepository;

    @Test
    @DatabaseSetup("/data/controller/partnerCapacity/prepare_data.xml")
    void testOnlyOncomingDaysOff() {
        softly.assertThat(partnerCapacityDayOffRepository.findAllByCapacityFromDay(CAPACITY, APRIL_30))
            .extracting(PartnerCapacityDayOff::getDay)
            .containsExactlyInAnyOrder(MAY_1, MAY_2, MAY_3);

        softly.assertThat(partnerCapacityDayOffRepository.findAllByCapacityFromDay(CAPACITY, MAY_1))
            .extracting(PartnerCapacityDayOff::getDay)
            .containsExactlyInAnyOrder(MAY_1, MAY_2, MAY_3);

        softly.assertThat(partnerCapacityDayOffRepository.findAllByCapacityFromDay(CAPACITY, MAY_2))
            .extracting(PartnerCapacityDayOff::getDay)
            .containsExactlyInAnyOrder(MAY_2, MAY_3);

        softly.assertThat(partnerCapacityDayOffRepository.findAllByCapacityFromDay(CAPACITY, MAY_3))
            .extracting(PartnerCapacityDayOff::getDay)
            .containsExactlyInAnyOrder(MAY_3);

        softly.assertThat(partnerCapacityDayOffRepository.findAllByCapacityFromDay(CAPACITY, MAY_4))
            .extracting(PartnerCapacityDayOff::getDay)
            .isEmpty();
    }
}
