package ru.yandex.direct.grid.core.entity.campaign.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.TimeTargetStatus;
import ru.yandex.direct.core.entity.campaign.model.TimeTargetStatusInfo;
import ru.yandex.direct.core.entity.campaign.service.TimeTargetStatusService;
import ru.yandex.direct.core.entity.timetarget.model.GeoTimezone;
import ru.yandex.direct.core.entity.timetarget.model.HolidayItem;
import ru.yandex.direct.core.entity.timetarget.repository.ProductionCalendarRepository;
import ru.yandex.direct.core.entity.timetarget.service.ProductionCalendarProviderService;
import ru.yandex.direct.libs.timetarget.HoursCoef;
import ru.yandex.direct.libs.timetarget.TimeTarget;
import ru.yandex.direct.libs.timetarget.WeekdayType;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class TimeTargetStatusServiceTest {
    // Используем временную зону Берлина, т.к. там есть первод часов
    private static final GeoTimezone BERLIN_TIMEZONE = new GeoTimezone()
            .withRegionId(96L)
            .withTimezoneId(56L)
            .withTimezone(ZoneId.of("Europe/Berlin"));

    private static final int YEAR = 2017;
    private static final Instant INSTANT = LocalDate.parse("2017-10-11").atStartOfDay().toInstant(ZoneOffset.UTC);
    private static final long REGION_ID = BERLIN_TIMEZONE.getRegionId();
    private static final LocalDate WORKING_SUNDAY = LocalDate.of(YEAR, 3, 12);
    private static final LocalDate HOLIDAY_MONDAY = LocalDate.of(YEAR, 3, 13);

    private static final long HOLIDAY_EPOCH_SECOND = 1489413600;
    //Круглосуточный таймтаргентинг без галочки "Учитывать праздничные дни"
    private static final String TIME_TARGET_NOCTIDIAL_WITHOUT_CONSIDER_HOLIDAY_RAW =
            "1ABCDEFGHIJKLMNOPQRSTUVWX2ABCDEFGHIJKLMNOPQRSTUVWX3ABCDEFGHIJKLMNOPQRSTUVWX4ABCDEFGHIJKLMNOPQRSTUVWX5ABCDEFGHIJKLMNOPQRSTUVWX6ABCDEFGHIJKLMNOPQRSTUVWX7ABCDEFGHIJKLMNOPQRSTUVWX9";

    private static final TimeTarget TIME_TARGET_1AM = createTimeTarget1am();
    private static final TimeTarget TIME_TARGET_2AM = createTimeTarget2am();
    private static final TimeTarget TIME_TARGET_WORKING_HOLIDAY = createTimeTargetWorkingHoliday();

    //Круглосуточный таймтаргентинг без галочки "Учитывать праздничные дни"
    private static final TimeTarget TIME_TARGET_NOCTIDIAL_WITHOUT_CONSIDER_HOLIDAY =
            createTimeTargetNoctidialWithoutConsiderHoliday();
    //Круглосуточный таймтаргентинг с проставленной галочкой "Учитывать праздничные дни": с 18:00 по 19:00
    private static final TimeTarget TIME_TARGET_NOCTIDIAL_WITH_CUSTOM_HOLIDAY =
            createTimeTargetNoctidialWithCustomHolidayTimeTarget();

    private TimeTargetStatusService timeTargetStatusService;

    @Parameterized.Parameter
    public TimeTarget timeTarget;

    @Parameterized.Parameter(1)
    public Instant instant;

    @Parameterized.Parameter(2)
    public TimeTargetStatus expectedStatus;

    @Parameterized.Parameter(3)
    public BigDecimal expectedCoef;

    @Parameterized.Parameter(4)
    public OffsetDateTime expectedActivationTime;

    @Parameterized.Parameter(5)
    public String testDescription;

    @Parameterized.Parameters(name = "description = {5}, instant = {1}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {null, INSTANT, TimeTargetStatus.ACTIVE, null, null, "timeTarget = null"},
                {TIME_TARGET_1AM, Instant.ofEpochSecond(1490487300), TimeTargetStatus.ACTIVE, new BigDecimal("0.5"),
                        null, "26 марта 2017 1:15 в Берлине"},
                {TIME_TARGET_1AM, Instant.ofEpochSecond(1490483700), TimeTargetStatus.TODAY, new BigDecimal("0.5"),
                        OffsetDateTime.parse("2017-03-26T01:00:00+01:00"), "26 марта 2017 0:15 в Берлине"},
                {TIME_TARGET_2AM, Instant.ofEpochSecond(1490487300), TimeTargetStatus.TOMORROW, new BigDecimal("2.0"),
                        OffsetDateTime.parse("2017-03-27T02:00:00+02:00"),
                        "26 марта времени 2:00 не существует, так что следующий возможный вариант - это 2:00 понедельника"},
                {TIME_TARGET_2AM, Instant.ofEpochSecond(1509232500), TimeTargetStatus.TODAY, new BigDecimal("1.0"),
                        OffsetDateTime.parse("2017-10-29T02:00:00+02:00"), "29 октября время 2:00 существует"},
                {TIME_TARGET_2AM, Instant.ofEpochSecond(1509236100), TimeTargetStatus.ACTIVE, new BigDecimal("1.0"),
                        null, "29 октября 2017 2:15 в Берлине"},
                {TIME_TARGET_2AM, Instant.ofEpochSecond(1509236100).plusSeconds(3600), TimeTargetStatus.ACTIVE,
                        new BigDecimal("1.0"), null,
                        "Через 1 час снова будет 2:15 и временной таргетинг всё ещё активен"},
                {TIME_TARGET_2AM, Instant.ofEpochSecond(1490313600), TimeTargetStatus.THIS_WEEK,
                        new BigDecimal("2.0"), OffsetDateTime.parse("2017-03-27T02:00:00+02:00"),
                        "24 марта 2017 1:00 в Берлине"},
                {TIME_TARGET_WORKING_HOLIDAY, Instant.ofEpochSecond(1489273200), TimeTargetStatus.ACTIVE,
                        new BigDecimal("1.0"), null,
                        "12 марта 2017 0:00 в Берлине (рабочий выходной)"},

                {TIME_TARGET_WORKING_HOLIDAY, Instant.ofEpochSecond(1488841200), TimeTargetStatus.THIS_WEEK,
                        new BigDecimal("1.0"), OffsetDateTime.parse("2017-03-12T00:00:00+01:00"),
                        "1 марта 2017 0:00 в Берлине"},
                {TIME_TARGET_WORKING_HOLIDAY, Instant.ofEpochSecond(1485907200), TimeTargetStatus.THIS_WEEK,
                        new BigDecimal("1.0"), OffsetDateTime.parse("2017-02-06T00:00:00+01:00"),
                        "1 февраля 2017 0:00 в Берлине"},

                {TIME_TARGET_NOCTIDIAL_WITHOUT_CONSIDER_HOLIDAY, Instant.ofEpochSecond(HOLIDAY_EPOCH_SECOND),
                        TimeTargetStatus.ACTIVE, BigDecimal.valueOf(1.0), null,
                        "13 марта 2017 15:00 в Берлине - Праздник. Показываем по праздникам"},
                {TIME_TARGET_NOCTIDIAL_WITH_CUSTOM_HOLIDAY, Instant.ofEpochSecond(HOLIDAY_EPOCH_SECOND),
                        TimeTargetStatus.TODAY, BigDecimal.valueOf(1.4),
                        OffsetDateTime.parse("2017-03-13T18:00:00+01:00"),
                        "13 марта 2017 15:00 в Берлине - Праздник. По праздникам показывать с 18:00"},
        });
    }

    private static TimeTarget createTimeTarget1am() {
        TimeTarget timeTarget = new TimeTarget();
        timeTarget.setWeekdayCoef(WeekdayType.SUNDAY, new HoursCoef().withHourCoef(1, 50));
        return timeTarget;
    }

    private static TimeTarget createTimeTarget2am() {
        TimeTarget timeTarget = new TimeTarget();
        timeTarget.setWeekdayCoef(WeekdayType.SUNDAY, new HoursCoef().withHourCoef(2, 100));
        timeTarget.setWeekdayCoef(WeekdayType.MONDAY, new HoursCoef().withHourCoef(2, 200));
        return timeTarget;
    }

    private static TimeTarget createTimeTargetWorkingHoliday() {
        TimeTarget timeTarget = new TimeTarget();
        timeTarget.setWeekdayCoef(WeekdayType.MONDAY, new HoursCoef()
                .withHourCoef(0, 100)
                .withHourCoef(1, 100));
        timeTarget.setWeekdayCoef(WeekdayType.WORKING_WEEKEND, new HoursCoef());
        return timeTarget;
    }

    private static TimeTarget createTimeTargetNoctidialWithoutConsiderHoliday() {
        return TimeTarget.parseRawString(TIME_TARGET_NOCTIDIAL_WITHOUT_CONSIDER_HOLIDAY_RAW);
    }

    private static TimeTarget createTimeTargetNoctidialWithCustomHolidayTimeTarget() {
        TimeTarget timeTarget = createTimeTargetNoctidialWithoutConsiderHoliday();
        timeTarget.setWeekdayCoef(WeekdayType.HOLIDAY, new HoursCoef().withHourCoef(18, 140));
        return timeTarget;
    }

    @Before
    public void setup() {
        ProductionCalendarRepository productionCalendarRepository = mock(ProductionCalendarRepository.class);
        when(productionCalendarRepository.getHolidaysByYear(YEAR))
                .thenReturn(Arrays.asList(
                        new HolidayItem(REGION_ID, WORKING_SUNDAY, HolidayItem.Type.WORKDAY),
                        new HolidayItem(REGION_ID, HOLIDAY_MONDAY, HolidayItem.Type.HOLIDAY)
                ));
        ProductionCalendarProviderService productionCalendarProviderService =
                new ProductionCalendarProviderService(productionCalendarRepository);
        timeTargetStatusService = new TimeTargetStatusService(productionCalendarProviderService);
    }

    @Test
    public void testStatusCalc() {
        TimeTargetStatusInfo status = timeTargetStatusService.getTimeTargetStatus(timeTarget, BERLIN_TIMEZONE, instant);

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(status.getStatus())
                .isEqualTo(expectedStatus);

        if (expectedCoef != null) {
            soft.assertThat(status.getCoef())
                    .isEqualTo(expectedCoef);
        } else {
            soft.assertThat(status.getCoef())
                    .isNull();
        }
        if (expectedActivationTime != null) {
            soft.assertThat(status.getActivationTime())
                    .isEqualTo(expectedActivationTime);
        } else {
            soft.assertThat(status.getActivationTime())
                    .isNull();
        }

        soft.assertAll();
    }
}
