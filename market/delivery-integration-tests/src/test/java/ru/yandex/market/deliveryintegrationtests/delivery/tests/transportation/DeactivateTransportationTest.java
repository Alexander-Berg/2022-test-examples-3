package ru.yandex.market.deliveryintegrationtests.delivery.tests.transportation;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import toolkit.Delayer;

import ru.yandex.market.delivery.transport_manager.model.enums.TransportationStatus;

@Slf4j
@DisplayName("TM Test")
@Epic("TM")

public class DeactivateTransportationTest extends AbstractTransportationTest {

    /**
     * Константы с набором данных ниже нужны только в этом тесте, уносить их наружу нет смысла
     **/
    private static final long OUTBOUND_PARTNER_ID = 48104;
    private static final long INBOUND_PARTNER_ID = 100136;
    private static final Long PARTNER_RELATION_ID = 605L;
    private static final Long SCHEDULE_ID = 14099542L;
    private final static String TIME_INTERVAL = "08:00-20:00";
    static final int dayAfterTomorrow = LocalDate.now().plusDays(2).getDayOfWeek().getValue();

    static Long transportationId;
    static Long scheduleDayId;

    @BeforeEach
    public void setUp() {
        transportationId = TM_STEPS.getTransportationIdForDay(
                OUTBOUND_PARTNER_ID,
                INBOUND_PARTNER_ID,
                LocalDate.now().plusDays(2),
                TransportationStatus.SCHEDULED
        );
    }

    @Step("Добавляем день в расписание для активации перемещения")
    private static void addDayToSchedule() {
        LMS_STEPS.createScheduleDay("IMPORT", PARTNER_RELATION_ID, dayAfterTomorrow, TIME_INTERVAL);
        Delayer.delay(6, TimeUnit.MINUTES);
        TM_STEPS.refreshTransportation();
    }

    @Step("Удаляем день из расписания для деактивации перемещения")
    private static void deleteDayFromSchedule() {
        scheduleDayId = LMS_STEPS.getScheduleDay(dayAfterTomorrow, SCHEDULE_ID);
        LMS_STEPS.deleteScheduleDay(scheduleDayId);
        Delayer.delay(6, TimeUnit.MINUTES);
        TM_STEPS.refreshTransportation();
    }

    @Test
    @DisplayName("ТМ: Деактивация и активация перемещения на послезавтра")
    void deactivateTransportationTest() {
        log.info("Starting Deactivate Transportation test...");
        if (!TM_STEPS.getTransportationIsActive(transportationId)) {
            addDayToSchedule();
        }
        if (LMS_STEPS.getScheduleDay(dayAfterTomorrow, SCHEDULE_ID).equals(null)
                && TM_STEPS.getTransportationIsActive(transportationId)
        ) {
            LMS_STEPS.createScheduleDay("IMPORT", PARTNER_RELATION_ID, dayAfterTomorrow, TIME_INTERVAL);
        }
        TM_STEPS.verifyTransportationIsActive(transportationId, true);
        scheduleDayId = LMS_STEPS.getScheduleDay(dayAfterTomorrow, SCHEDULE_ID);
        deleteDayFromSchedule();
        TM_STEPS.verifyTransportationIsActive(transportationId, false);
        addDayToSchedule();
        TM_STEPS.verifyTransportationIsActive(transportationId, true);
    }
}
