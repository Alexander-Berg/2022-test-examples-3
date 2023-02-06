package ru.yandex.market.tpl.core.domain.pickup.holiday;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.tpl.core.domain.pickup.generator.PickupPointGenerator.generatePickupPoint;

@RequiredArgsConstructor
class PickupPointHolidayRepositoryTest extends TplAbstractTest {

    public static final long LOGISTIC_POINT_ID = 1L;
    private final PickupPointHolidayRepository pickupPointHolidayRepository;
    private final TransactionTemplate transactionTemplate;

    @DisplayName("Сохранение выходных 😎")
    @Test
    void testSaveAll() {
        List<PickupPointHoliday> pickupPointHolidayList = List.of(
                new PickupPointHoliday(generatePickupPoint(LOGISTIC_POINT_ID), LocalDate.now())
        );

        List<PickupPointHoliday> pickupPointHolidays = pickupPointHolidayRepository.saveAll(pickupPointHolidayList);

        assertEquals(
                pickupPointHolidayList.size(),
                pickupPointHolidays.size()
        );
    }

    @DisplayName("Удаление выходных 🙁")
    @Test
    void testDeleteAll() {
        List<PickupPointHoliday> pickupPointHolidayList = List.of(
                new PickupPointHoliday(generatePickupPoint(LOGISTIC_POINT_ID), LocalDate.now())
        );

        transactionTemplate.execute(t -> {
            List<PickupPointHoliday> pickupPointHolidays =
                    pickupPointHolidayRepository.saveAll(pickupPointHolidayList);
            assertNotNull(pickupPointHolidays);
            pickupPointHolidayRepository.deleteAll(pickupPointHolidays);
            return null;
        });

        List<PickupPointHoliday> holidays = pickupPointHolidayRepository.findAll();
        assertTrue(holidays.isEmpty());
    }

}
