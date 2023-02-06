package ru.yandex.market.abo.cpa.lms;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 22.04.2021
 */
class EstimatedExpressShipmentTimeTest extends EmptyTest {

    private static final LocalTime SCHEDULE_TIME_FROM = LocalTime.of(10, 0);
    private static final LocalTime SCHEDULE_TIME_TO = LocalTime.of(20, 0);

    private static final int MINUTES_BEFORE_SCHEDULE_TIME_TO_FOR_OFFERS_HIDING = 60;
    private static final int MINUTES_FOR_ORDER_ASSEMBLY = 30;
    private static final int MINUTES_BETWEEN_CREATION_AND_PROCESSING = 2;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void estimatedTime__beforeScheduleTimeFrom() {
        var processingTime = SCHEDULE_TIME_FROM.minusHours(3).atDate(LocalDate.now());
        var creationTime = processingTime.minusMinutes(MINUTES_BETWEEN_CREATION_AND_PROCESSING);

        assertEquals(
                SCHEDULE_TIME_FROM.plusMinutes(MINUTES_FOR_ORDER_ASSEMBLY).atDate(LocalDate.now()),
                estimatedShipmentTime(creationTime, processingTime)
        );
    }

    @Test
    void estimatedTime__beforeOffersHiding() {
        var processingTime = SCHEDULE_TIME_TO
                .minusMinutes(MINUTES_BEFORE_SCHEDULE_TIME_TO_FOR_OFFERS_HIDING + 15)
                .atDate(LocalDate.now());
        var creationTime = processingTime.minusMinutes(MINUTES_BETWEEN_CREATION_AND_PROCESSING);

        assertEquals(
                processingTime.plusMinutes(MINUTES_FOR_ORDER_ASSEMBLY),
                estimatedShipmentTime(creationTime, processingTime)
        );
    }

    @Test
    void estimatedTime__afterOffersHiding() {
        var processingTime = SCHEDULE_TIME_TO
                .minusMinutes(MINUTES_BEFORE_SCHEDULE_TIME_TO_FOR_OFFERS_HIDING - 15)
                .atDate(LocalDate.now());
        var creationTime = processingTime.minusMinutes(MINUTES_BETWEEN_CREATION_AND_PROCESSING);

        assertEquals(
                SCHEDULE_TIME_FROM.atDate(LocalDate.now().plusDays(1)).plusMinutes(MINUTES_FOR_ORDER_ASSEMBLY),
                estimatedShipmentTime(creationTime, processingTime)
        );
    }

    @Test
    void estimatedTime__scheduleNotExists() {
        var processingTime = SCHEDULE_TIME_TO
                .minusMinutes(MINUTES_BEFORE_SCHEDULE_TIME_TO_FOR_OFFERS_HIDING - 15)
                .atDate(LocalDate.now());
        var creationTime = processingTime.minusMinutes(MINUTES_BETWEEN_CREATION_AND_PROCESSING);

        assertEquals(
                processingTime.plusMinutes(MINUTES_FOR_ORDER_ASSEMBLY),
                estimatedShipmentTime(creationTime, processingTime, null)
        );
    }

    private LocalDateTime estimatedShipmentTime(LocalDateTime creationTime, LocalDateTime processingTime) {
        return estimatedShipmentTime(creationTime, processingTime, SCHEDULE_TIME_FROM);
    }

    private LocalDateTime estimatedShipmentTime(LocalDateTime creationTime, LocalDateTime processingTime, LocalTime scheduleTimeFrom) {
        return jdbcTemplate.queryForObject(
                "select estimated_express_shipment_time(?, ?, ?, ?, ?)", LocalDateTime.class,
                creationTime, processingTime, scheduleTimeFrom, SCHEDULE_TIME_TO, scheduleTimeFrom
        );
    }
}
