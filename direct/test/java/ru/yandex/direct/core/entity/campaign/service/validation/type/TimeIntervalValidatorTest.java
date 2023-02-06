package ru.yandex.direct.core.entity.campaign.service.validation.type;

import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.campaign.repository.CampaignMappings;
import ru.yandex.direct.core.entity.time.model.TimeInterval;
import ru.yandex.direct.validation.defect.NumberDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.type.TimeIntervalValidator.MAX_HOUR;
import static ru.yandex.direct.core.entity.campaign.service.validation.type.TimeIntervalValidator.MAX_MINUTE;
import static ru.yandex.direct.core.entity.campaign.service.validation.type.TimeIntervalValidator.MIN_HOUR;
import static ru.yandex.direct.core.entity.campaign.service.validation.type.TimeIntervalValidator.MIN_MINUTE;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidFormat;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class TimeIntervalValidatorTest {

    @SuppressWarnings("unused")
    private static Object[] parametrizedTestData() {
        return new Object[][]{
                {"0:0:0:0", null, null},
                {"00:00:00:00", null, null},
                {"00:00:00:15", null, null},
                {"00:00:00:30", null, null},
                {"00:00:00:45", null, null},

                {"00:00:01:00", null, null},
                {"00:15:01:15", null, null},
                {"00:30:01:30", null, null},
                {"00:45:01:45", null, null},

                {"00:00:24:00", null, null},

                {"09:00:09:00", null, null},
                {"24:00:24:00", null, null},

                // промежуток на границе суток - валидный кейс
                {"18:00:10:00", null, null},

                //invalid cases:
                //endMinute
                {"00:00:00:01", path(field(TimeInterval.END_MINUTE.name())), invalidFormat()},
                {"00:00:00:-1", path(field(TimeInterval.END_MINUTE.name())),
                        NumberDefects.inInterval(MIN_MINUTE, MAX_MINUTE)},
                {"00:00:00:60", path(field(TimeInterval.END_MINUTE.name())),
                        NumberDefects.inInterval(MIN_MINUTE, MAX_MINUTE)},
                {"0:0:24:15", path(field(TimeInterval.END_MINUTE.name())), invalidFormat()},

                //startMinute
                {"00:02:20:00", path(field(TimeInterval.START_MINUTE.name())), invalidFormat()},
                {"00:-2:20:00", path(field(TimeInterval.START_MINUTE.name())),
                        NumberDefects.inInterval(MIN_MINUTE, MAX_MINUTE)},
                {"00:60:20:00", path(field(TimeInterval.START_MINUTE.name())),
                        NumberDefects.inInterval(MIN_MINUTE, MAX_MINUTE)},
                {"24:15:20:00", path(field(TimeInterval.START_MINUTE.name())), invalidFormat()},

                //endHour
                {"00:00:-1:00", path(field(TimeInterval.END_HOUR.name())), NumberDefects.inInterval(MIN_HOUR, MAX_HOUR)},
                {"00:00:25:00", path(field(TimeInterval.END_HOUR.name())), NumberDefects.inInterval(MIN_HOUR, MAX_HOUR)},

                //startHour
                {"-1:00:21:15", path(field(TimeInterval.START_HOUR.name())), NumberDefects.inInterval(MIN_HOUR, MAX_HOUR)},
                {"25:00:21:15", path(field(TimeInterval.START_HOUR.name())), NumberDefects.inInterval(MIN_HOUR, MAX_HOUR)},
        };
    }

    @SuppressWarnings("unused")
    private static Object[] parametrizedTestData_forRequiredFields() {
        Consumer<TimeInterval> doNothing = (interval) -> {
        };

        return new Object[][]{
                {"valid time interval", doNothing, null, null},

                {"startHour is null", (Consumer<TimeInterval>) (interval) -> interval.setStartHour(null),
                        path(field(TimeInterval.START_HOUR.name())), notNull()},
                {"startMinute is null", (Consumer<TimeInterval>) (interval) -> interval.setStartMinute(null),
                        path(field(TimeInterval.START_MINUTE.name())), notNull()},
                {"endHour is null", (Consumer<TimeInterval>) (interval) -> interval.setEndHour(null),
                        path(field(TimeInterval.END_HOUR.name())), notNull()},
                {"endMinute is null", (Consumer<TimeInterval>) (interval) -> interval.setEndMinute(null),
                        path(field(TimeInterval.END_MINUTE.name())), notNull()},

                {"endHour = 24 and endMinute is null",
                        (Consumer<TimeInterval>) (interval) -> interval
                                .withEndHour(MAX_HOUR)
                                .withEndMinute(null),
                        path(field(TimeInterval.END_MINUTE.name())), notNull()},
        };
    }

    private TimeIntervalValidator validator;

    @Before
    public void initTestData() {
        validator = new TimeIntervalValidator();
    }


    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("timeInterval = {0}, expectedPath = {1}, expectedDefect = {2}")
    public void checkTimeIntervalValidator(String stringTimeInterval,
                                           @Nullable Path expectedPath,
                                           @Nullable Defect expectedDefect) {
        TimeInterval timeInterval = CampaignMappings.smsTimeFromDb(stringTimeInterval);

        checkValidator(timeInterval, expectedPath, expectedDefect);
    }

    @Test
    @Parameters(method = "parametrizedTestData_forRequiredFields")
    @TestCaseName("description = {0}, expectedPath = {2}, expectedDefect = {3}")
    public void checkTimeIntervalValidator_forRequiredFields(@SuppressWarnings("unused") String testDescription,
                                                             Consumer<TimeInterval> timeIntervalConsumer,
                                                             @Nullable Path expectedPath,
                                                             @Nullable Defect expectedDefect) {
        TimeInterval timeInterval = new TimeInterval()
                .withStartHour(10).withStartMinute(0)
                .withEndHour(20).withEndMinute(15);
        timeIntervalConsumer.accept(timeInterval);

        checkValidator(timeInterval, expectedPath, expectedDefect);
    }

    private void checkValidator(TimeInterval timeInterval,
                                @Nullable Path expectedPath, @Nullable Defect expectedDefect) {
        ValidationResult<TimeInterval, Defect> result = validator.apply(timeInterval);

        if (expectedDefect == null) {
            assertThat(result).
                    is(matchedBy(hasNoDefectsDefinitions()));
        } else {
            assertThat(result).
                    is(matchedBy(hasDefectWithDefinition(validationError(expectedPath, expectedDefect))));
        }
    }

}
