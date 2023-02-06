package ru.yandex.market.abo.cpa.lms.repo;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.cpa.lms.model.LMSIntake;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 28.04.2021
 */
class LMSIntakeRepoTest extends EmptyTest {

    private static final long RELATION_ID = 2142136L;
    private static final List<Integer> INTAKE_DAYS = List.of(1, 2, 3, 4, 5);

    private static final LocalTime INTAKE_TIME_FROM = LocalTime.of(10, 0);
    private static final LocalTime INTAKE_TIME_TO = LocalTime.of(20, 0);

    @Autowired
    private LMSIntakeRepo lmsIntakeRepo;

    @BeforeEach
    void init() {
        INTAKE_DAYS.forEach(day -> {
                    ScheduleDayResponse schedule = new ScheduleDayResponse(
                            (long) day, day, INTAKE_TIME_FROM, INTAKE_TIME_TO
                    );
                    lmsIntakeRepo.save(new LMSIntake(RELATION_ID, schedule));
                }
        );
        flushAndClear();
    }

    @Test
    void findByRelationTest() {
        var savedIntakes = lmsIntakeRepo.findAllByLmsIntakeKeyPartnerRelationId(RELATION_ID);
        assertEquals(INTAKE_DAYS.size(), savedIntakes.size());
        savedIntakes.forEach(intake -> {
                    assertEquals(INTAKE_TIME_FROM, intake.getTimeFrom());
                    assertEquals(INTAKE_TIME_TO, intake.getTimeTo());
                }
        );
    }
}
