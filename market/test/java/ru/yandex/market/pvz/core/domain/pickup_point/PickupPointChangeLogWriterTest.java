package ru.yandex.market.pvz.core.domain.pickup_point;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField;
import ru.yandex.market.pvz.core.domain.pickup_point.changelog.PickupPointChangeLog;
import ru.yandex.market.pvz.core.domain.pickup_point.changelog.PickupPointChangeLogRepository;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.REGIONS_FOR_DEPENDENT_PARAMS;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.REGION_SPECIFIC_MAX_HEIGHT;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.REGION_SPECIFIC_MAX_LENGTH;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.REGION_SPECIFIC_MAX_WIDTH;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.ACTIVE;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.BRANDING_TYPE;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.CAPACITY;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.CARD_ALLOWED;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.CARD_COMPENSATION_RATE;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.CASHBOX_TOKEN;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.CASH_ALLOWED;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.CASH_COMPENSATION_RATE;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.FRIDAY_IS_WORKING;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.FRIDAY_TIME_FROM;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.FRIDAY_TIME_TO;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.HEIGHT;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.INSTRUCTION;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.LENGTH;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.LOCATION;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.MONDAY_IS_WORKING;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.MONDAY_TIME_FROM;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.MONDAY_TIME_TO;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.NAME;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.PARTIAL_RETURN_ALLOWED;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.PHONE;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.PREPAID_ALLOWED;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.RETURN_ALLOWED;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.SATURDAY_IS_WORKING;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.SATURDAY_TIME_FROM;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.SATURDAY_TIME_TO;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.SIDES_SUM;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.STORAGE_PERIOD;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.SUNDAY_IS_WORKING;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.SUNDAY_TIME_FROM;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.SUNDAY_TIME_TO;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.THURSDAY_IS_WORKING;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.THURSDAY_TIME_FROM;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.THURSDAY_TIME_TO;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.TIME_OFFSET;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.TRANSMISSION_REWARD;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.TUESDAY_IS_WORKING;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.TUESDAY_TIME_FROM;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.TUESDAY_TIME_TO;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.WEDNESDAY_IS_WORKING;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.WEDNESDAY_TIME_FROM;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.WEDNESDAY_TIME_TO;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.WEIGHT;
import static ru.yandex.market.pvz.core.domain.pickup_point.changelog.ChangeLogField.WIDTH;
import static ru.yandex.market.pvz.core.test.TestExternalConfiguration.DEFAULT_UID;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.CreatePickupPointBuilder;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleDayTestParams;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleDayTestParams.DEFAULT_IS_WORKING_DAY;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleDayTestParams.DEFAULT_TIME_FROM;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleDayTestParams.DEFAULT_TIME_TO;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleTestParams;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_ACTIVE;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_BRANDING_TYPE;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_CAPACITY;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_CARD_ALLOWED;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_CARD_COMPENSATION_RATE;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_CASH_ALLOWED;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_CASH_COMPENSATION_RATE;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_HEIGHT;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_INSTRUCTION;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_LENGTH;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_MAX_SIDES_SUM;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_OFFSET;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_PARTIAL_RETURN_ALLOWED;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_PHONE;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_PREPAY_ALLOWED;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_RETURN_ALLOWED;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_STORAGE_PERIOD;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_TRANSMISSION_REWARD;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_WEIGHT;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_WIDTH;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PickupPointChangeLogWriterTest {

    private final TestPickupPointFactory pickupPointFactory;
    private final PickupPointChangeLogRepository changeLogRepository;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    @BeforeEach
    void setup() {
        configurationGlobalCommandService.setValue(REGION_SPECIFIC_MAX_LENGTH, PickupPoint.REGION_SPECIFIC_MAX_LENGTH);
        configurationGlobalCommandService.setValue(REGION_SPECIFIC_MAX_WIDTH, PickupPoint.REGION_SPECIFIC_MAX_WIDTH);
        configurationGlobalCommandService.setValue(REGION_SPECIFIC_MAX_HEIGHT, PickupPoint.REGION_SPECIFIC_MAX_HEIGHT);
        configurationGlobalCommandService.setValue(
                REGIONS_FOR_DEPENDENT_PARAMS, PickupPoint.REGIONS_FOR_DEPENDENT_PARAMS);
    }

    @SuppressWarnings("checkstyle:MethodLength")
    @Test
    void saveLogsAfterCreatingNewPickupPoint() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();

        String defaultLocation =
                pickupPointFactory.mapLocation(PickupPointLocationTestParams.builder().build()).getAddress();
        List<PickupPointChangeLogEntry> expected = List.of(
                createLogEntry(NAME, DEFAULT_NAME),
                createLogEntry(LOCATION, defaultLocation),
                createLogEntry(MONDAY_IS_WORKING, DEFAULT_IS_WORKING_DAY),
                createLogEntry(MONDAY_TIME_FROM, DEFAULT_TIME_FROM),
                createLogEntry(MONDAY_TIME_TO, DEFAULT_TIME_TO),
                createLogEntry(TUESDAY_IS_WORKING, DEFAULT_IS_WORKING_DAY),
                createLogEntry(TUESDAY_TIME_FROM, DEFAULT_TIME_FROM),
                createLogEntry(TUESDAY_TIME_TO, DEFAULT_TIME_TO),
                createLogEntry(WEDNESDAY_IS_WORKING, DEFAULT_IS_WORKING_DAY),
                createLogEntry(WEDNESDAY_TIME_FROM, DEFAULT_TIME_FROM),
                createLogEntry(WEDNESDAY_TIME_TO, DEFAULT_TIME_TO),
                createLogEntry(THURSDAY_IS_WORKING, DEFAULT_IS_WORKING_DAY),
                createLogEntry(THURSDAY_TIME_FROM, DEFAULT_TIME_FROM),
                createLogEntry(THURSDAY_TIME_TO, DEFAULT_TIME_TO),
                createLogEntry(FRIDAY_IS_WORKING, DEFAULT_IS_WORKING_DAY),
                createLogEntry(FRIDAY_TIME_FROM, DEFAULT_TIME_FROM),
                createLogEntry(FRIDAY_TIME_TO, DEFAULT_TIME_TO),
                createLogEntry(SATURDAY_IS_WORKING, DEFAULT_IS_WORKING_DAY),
                createLogEntry(SATURDAY_TIME_FROM, DEFAULT_TIME_FROM),
                createLogEntry(SATURDAY_TIME_TO, DEFAULT_TIME_TO),
                createLogEntry(SUNDAY_IS_WORKING, DEFAULT_IS_WORKING_DAY),
                createLogEntry(SUNDAY_TIME_FROM, DEFAULT_TIME_FROM),
                createLogEntry(SUNDAY_TIME_TO, DEFAULT_TIME_TO),
                createLogEntry(PHONE, DEFAULT_PHONE),
                createLogEntry(CASH_ALLOWED, DEFAULT_CASH_ALLOWED),
                createLogEntry(PREPAID_ALLOWED, DEFAULT_PREPAY_ALLOWED),
                createLogEntry(CARD_ALLOWED, DEFAULT_CARD_ALLOWED),
                createLogEntry(INSTRUCTION, DEFAULT_INSTRUCTION),
                createLogEntry(RETURN_ALLOWED, DEFAULT_RETURN_ALLOWED),
                createLogEntry(STORAGE_PERIOD, DEFAULT_STORAGE_PERIOD),
                createLogEntry(LENGTH, formatBigDecimal(DEFAULT_LENGTH)),
                createLogEntry(WIDTH, formatBigDecimal(DEFAULT_WIDTH)),
                createLogEntry(HEIGHT, formatBigDecimal(DEFAULT_HEIGHT)),
                createLogEntry(WEIGHT, formatBigDecimal(DEFAULT_WEIGHT)),
                createLogEntry(SIDES_SUM, formatBigDecimal(DEFAULT_MAX_SIDES_SUM)),
                createLogEntry(TIME_OFFSET, DEFAULT_OFFSET),
                createLogEntry(ACTIVE, DEFAULT_ACTIVE),
                createLogEntry(CAPACITY, DEFAULT_CAPACITY),
                createLogEntry(TRANSMISSION_REWARD, formatBigDecimal(DEFAULT_TRANSMISSION_REWARD)),
                createLogEntry(CASH_COMPENSATION_RATE, formatBigDecimal(DEFAULT_CASH_COMPENSATION_RATE)),
                createLogEntry(CARD_COMPENSATION_RATE, formatBigDecimal(DEFAULT_CARD_COMPENSATION_RATE)),
                createLogEntry(BRANDING_TYPE, DEFAULT_BRANDING_TYPE),
                createLogEntry(PARTIAL_RETURN_ALLOWED, DEFAULT_PARTIAL_RETURN_ALLOWED)
        );

        List<PickupPointChangeLogEntry> actual = changeLogRepository.findByPickupPointId(pickupPoint.getId()).stream()
                .map(PickupPointChangeLogWriterTest::mapLogEntry)
                .collect(Collectors.toList());

        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    private static String formatBigDecimal(BigDecimal defaultLength) {
        return defaultLength.stripTrailingZeros().toPlainString();
    }

    @Test
    void saveLogsAfterUpdatingPickupPoint() {
        String cashboxToken = RandomStringUtils.randomAlphanumeric(10);
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint(
                CreatePickupPointBuilder.builder()
                        .params(PickupPointTestParams.builder()
                                .phone(DEFAULT_PHONE + "(000)")
                                .cashboxToken(cashboxToken)
                                .build())
                        .build()
        );
        changeLogRepository.deleteAll();

        pickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                PickupPointTestParams.builder()
                        .cardAllowed(false)
                        .capacity(300)
                        .phone(DEFAULT_PHONE + "(000)")
                        .instruction(DEFAULT_INSTRUCTION)
                        .storagePeriod(1)
                        .cashboxToken(cashboxToken + "-1")
                        .location(PickupPointLocationTestParams.builder()
                                .metro("Минская")
                                .build())
                        .schedule(PickupPointScheduleTestParams.builder()
                                .scheduleDays(List.of(
                                        PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.MONDAY)
                                                .build(),
                                        PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.TUESDAY)
                                                .build(),
                                        PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.WEDNESDAY)
                                                .build(),
                                        PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.THURSDAY)
                                                .build(),
                                        PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.FRIDAY)
                                                .timeTo(LocalTime.of(18, 0))
                                                .build(),
                                        PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SATURDAY)
                                                .isWorkingDay(false)
                                                .build(),
                                        PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SUNDAY)
                                                .timeFrom(LocalTime.of(7, 30))
                                                .build()
                                ))
                                .build())
                        .build());

        List<PickupPointChangeLogEntry> expected = List.of(
                createLogEntry(CARD_ALLOWED, false),
                createLogEntry(CAPACITY, 300),
                createLogEntry(STORAGE_PERIOD, 1),
                createLogEntry(CASHBOX_TOKEN, cashboxToken + "-1"),
                createLogEntry(FRIDAY_TIME_TO, LocalTime.of(18, 0)),
                createLogEntry(SATURDAY_IS_WORKING, false),
                createLogEntry(SUNDAY_TIME_FROM, LocalTime.of(7, 30))
        );

        List<PickupPointChangeLogEntry> actual = changeLogRepository.findByPickupPointId(pickupPoint.getId()).stream()
                .map(PickupPointChangeLogWriterTest::mapLogEntry)
                .collect(Collectors.toList());

        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    private static PickupPointChangeLogEntry createLogEntry(ChangeLogField field, Object value) {
        return PickupPointChangeLogEntry.builder()
                .field(field)
                .value(value.toString())
                .changerUid(DEFAULT_UID)
                .build();
    }

    private static PickupPointChangeLogEntry mapLogEntry(PickupPointChangeLog pickupPointChangeLog) {
        return PickupPointChangeLogEntry.builder()
                .field(pickupPointChangeLog.getField())
                .value(pickupPointChangeLog.getValue())
                .changerUid(pickupPointChangeLog.getChangerUid())
                .build();
    }

    @Data
    @Builder
    private static class PickupPointChangeLogEntry {
        private ChangeLogField field;
        private String value;
        private Long changerUid;
    }
}
