package ru.yandex.market.crm.operatorwindow;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.operatorwindow.services.geo.TimeZoneResolver;
import ru.yandex.market.crm.operatorwindow.services.task.calltime.CustomerCallTimeService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.TimingService;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CustomerCallTimeServiceTest extends AbstractModuleOwTest {

    private static final long regionId = 96;
    private static final ZoneId YEKT_ZONE_ID = ZoneId.of("Asia/Yekaterinburg");
    private static final ZonedDateTime testTime = ZonedDateTime.of(
            LocalDate.of(2021, 8, 24),
            LocalTime.of(14, 30),
            YEKT_ZONE_ID
    );

    @Inject
    private DbService dbService;

    @Inject
    private TimingService timingService;

    @Inject
    private ServiceTimeTestUtils serviceTimeTestUtils;

    private CustomerCallTimeService customerCallTimeService;


    private List<Arguments> data() {
        return new TestDataFactory().createTestData();
    }

    @BeforeAll
    public void setUp() {
        TimeZoneResolver timeZoneResolver = mock(TimeZoneResolver.class);
        when(timeZoneResolver.getZoneId(regionId)).thenReturn(Optional.of(YEKT_ZONE_ID));
        customerCallTimeService = spy(new CustomerCallTimeService(timingService, timeZoneResolver, dbService));
        doReturn(testTime).when(customerCallTimeService).getZonedDateTimeNow(any(ZoneId.class));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource(value = "data")
    public void testGetNearestCallTime(@SuppressWarnings("unused") String name,
                                       OffsetDateTime expectedNextAllowedCallTime,
                                       WorkingPeriod firstDayWP,
                                       WorkingPeriod secondDayWP,
                                       WorkingPeriod... exceptionalPeriods) {
        var nearestCallTime = customerCallTimeService.getNearestCallTime(
                createTwoDaysServiceTime(firstDayWP, secondDayWP, exceptionalPeriods),
                regionId
        );

        Assertions.assertTrue(nearestCallTime.isPresent());
        Assertions.assertEquals(expectedNextAllowedCallTime, nearestCallTime.get().nextAllowedCallTime());
    }


    private ServiceTime createTwoDaysServiceTime(WorkingPeriod firstDayWP,
                                                 WorkingPeriod secondDayWP,
                                                 WorkingPeriod... exceptionalPeriods) {
        ServiceTime st = serviceTimeTestUtils.createServiceTime();
        serviceTimeTestUtils.createPeriod(st, firstDayWP.dayOfWeek, firstDayWP.startTime, firstDayWP.finishTime);
        serviceTimeTestUtils.createPeriod(st, secondDayWP.dayOfWeek, secondDayWP.startTime, secondDayWP.finishTime);
        for (WorkingPeriod period : exceptionalPeriods) {
            serviceTimeTestUtils.createException(st, period.dayOfWeek, period.startTime, period.finishTime);
        }

        return st;
    }


    private record WorkingPeriod(String dayOfWeek, String startTime, String finishTime) {
    }

    private static class TestDataFactory {
        private static final String TUESDAY = "tuesday";
        private static final String WEDNESDAY = "wednesday";
        private static final String STANDARD_START_TIME = "09:00";
        private static final String STANDARD_FINISH_TIME = "18:00";
        private static final OffsetDateTime WEDNESDAY_START_TIME = OffsetDateTime
                .of(testTime.toLocalDate().plusDays(1), LocalTime.of(9, 0), testTime.getOffset());
        private static final String TODAY = "2021-08-24";
        private static final String TOMORROW = "2021-08-25";


        public List<Arguments> createTestData() {
            var data = new ArrayList<Arguments>();
            data.addAll(createDataWithoutExceptionalPeriods());
            data.addAll(createDataWithExceptionalPeriods());

            return data;
        }

        private List<Arguments> createDataWithoutExceptionalPeriods() {
            return List.of(
                    Arguments.of(
                            "Звонить можно прямо сейчас",
                            testTime.toOffsetDateTime(),
                            new WorkingPeriod(TUESDAY, STANDARD_START_TIME, STANDARD_FINISH_TIME),
                            new WorkingPeriod(WEDNESDAY, STANDARD_START_TIME, STANDARD_FINISH_TIME),
                            new WorkingPeriod[0]
                    ),
                    Arguments.of(
                            "Сегодняшний рабочий день закончен, звонить можно завтра утром",
                            WEDNESDAY_START_TIME,
                            new WorkingPeriod(TUESDAY, STANDARD_START_TIME, "14:00"),
                            new WorkingPeriod(WEDNESDAY, STANDARD_START_TIME, STANDARD_FINISH_TIME),
                            new WorkingPeriod[0]
                    ),
                    Arguments.of(
                            "Граничный случай: сейчас начало рабочего дня - звонить можно",
                            testTime.toOffsetDateTime(),
                            new WorkingPeriod(TUESDAY, "14:30", STANDARD_FINISH_TIME),
                            new WorkingPeriod(WEDNESDAY, STANDARD_START_TIME, STANDARD_FINISH_TIME),
                            new WorkingPeriod[0]
                    ),
                    Arguments.of(
                            "Граничный случай: сейчас конец рабочего дня - звонить можно",
                            testTime.toOffsetDateTime(),
                            new WorkingPeriod(TUESDAY, STANDARD_START_TIME, "14:30"),
                            new WorkingPeriod(WEDNESDAY, STANDARD_START_TIME, STANDARD_FINISH_TIME),
                            new WorkingPeriod[0]
                    )
            );
        }

        private List<Arguments> createDataWithExceptionalPeriods() {
            OffsetDateTime wednesdayExceptionalStartTime = OffsetDateTime
                    .of(testTime.toLocalDate().plusDays(1), LocalTime.of(13, 0), testTime.getOffset());

            return List.of(
                    Arguments.of(
                            "Обычно по вторникам в 14:30 звонить нельзя, но именно сегодня можно",
                            testTime.toOffsetDateTime(),
                            new WorkingPeriod(TUESDAY, "15:00", "23:00"),
                            new WorkingPeriod(WEDNESDAY, STANDARD_START_TIME, STANDARD_FINISH_TIME),
                            new WorkingPeriod[]{new WorkingPeriod(TODAY, STANDARD_START_TIME, "15:00")}
                    ),
                    Arguments.of(
                            "Обычно по вторникам в 14:30 звонить можно, но именно сегодня нельзя",
                            WEDNESDAY_START_TIME,
                            new WorkingPeriod(TUESDAY, STANDARD_START_TIME, STANDARD_FINISH_TIME),
                            new WorkingPeriod(WEDNESDAY, STANDARD_START_TIME, STANDARD_FINISH_TIME),
                            new WorkingPeriod[]{new WorkingPeriod(TODAY, STANDARD_START_TIME, "14:29")}
                    ),
                    Arguments.of(
                            "Сегодня звонить поздно, но можно завтра по измененному графику",
                            wednesdayExceptionalStartTime,
                            new WorkingPeriod(TUESDAY, STANDARD_START_TIME, "14:29"),
                            new WorkingPeriod(WEDNESDAY, STANDARD_START_TIME, STANDARD_FINISH_TIME),
                            new WorkingPeriod[]{new WorkingPeriod(TOMORROW, "13:00", STANDARD_FINISH_TIME)}
                    )
            );
        }
    }
}
