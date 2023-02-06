package ru.yandex.market.global.checkout.domain.actualize.actualizer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.TestTimeUtil;
import ru.yandex.market.global.checkout.domain.actualize.ActualizationError;
import ru.yandex.market.global.checkout.factory.TestOrderFactory.CreateOrderActualizationBuilder;
import ru.yandex.market.global.common.test.TestClock;
import ru.yandex.market.global.db.jooq.enums.EOrderDeliverySchedulingType;
import ru.yandex.mj.generated.server.model.ScheduleItemDto;

import static ru.yandex.market.global.checkout.factory.TestOrderFactory.buildOrderActualization;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ShopScheduleActualizerTest extends BaseFunctionalTest {

    private static final RecursiveComparisonConfiguration RECURSIVE_COMPARISON_CONFIGURATION =
            RecursiveComparisonConfiguration.builder()
                    .withIgnoreAllExpectedNullFields(true)
                    .build();

    private final TestClock clock;
    private final ShopScheduleActualizer shopScheduleActualizer;

    @BeforeEach
    void setup() {
        clock.setTime(Instant.parse("2022-02-07T12:00:00.00Z")); //MON
    }

    @Test
    void testClosedProduceError() {
        assertHasScheduleError(CreateOrderActualizationBuilder.builder()
                .setupShop(s -> s.schedule(List.of(new ScheduleItemDto()
                        .open(true)
                        .day("FRI")
                        .startAt("00:00:00")
                        .endAt("23:59:59")
                )))
                .setupDelivery(d -> d.setDeliverySchedulingType(EOrderDeliverySchedulingType.NOW))
                .build());
    }

    @Test
    void testOpenedProduceNoErrors() {
        assertNoErrors(CreateOrderActualizationBuilder.builder()
                .setupShop(s -> s.schedule(List.of(new ScheduleItemDto()
                        .open(true)
                        .day("MON")
                        .startAt("00:00:00")
                        .endAt("23:59:59")
                )))
                .setupDelivery(d -> d.setDeliverySchedulingType(EOrderDeliverySchedulingType.NOW))
                .build());
    }

    @Test
    void testRequestedDeliveryTimesWorksProperly() {
        OffsetDateTime dt = TestTimeUtil.getTimeInDefaultTZ(
                LocalDateTime.of(2022, 2, 8, 0, 0));
        ScheduleItemDto scheduleItem = new ScheduleItemDto()
                .open(true)
                .day("TUE")
                .startAt("12:00:00")
                .endAt("16:00:00");

        assertHasScheduleError(withScheduleItemAndTime(atHour(dt, 11), scheduleItem));
        assertHasScheduleError(withScheduleItemAndTime(atHour(dt, 12), scheduleItem));
        assertNoErrors(withScheduleItemAndTime(atHour(dt, 13), scheduleItem));
        assertNoErrors(withScheduleItemAndTime(atHour(dt, 14), scheduleItem));
        assertNoErrors(withScheduleItemAndTime(atHour(dt, 15), scheduleItem));
        assertHasScheduleError(withScheduleItemAndTime(atHour(dt, 16), scheduleItem));
        assertHasScheduleError(withScheduleItemAndTime(atHour(dt, 17), scheduleItem));
    }

    private OffsetDateTime atHour(OffsetDateTime base, int hour) {
        return OffsetDateTime.of(
                base.getYear(),
                base.getMonthValue(),
                base.getDayOfMonth(),
                hour,
                0, 0, 0,
                base.getOffset()
        );
    }

    private CreateOrderActualizationBuilder withScheduleItemAndTime(OffsetDateTime time, ScheduleItemDto scheduleItem) {
        return CreateOrderActualizationBuilder.builder()
                .setupShop(s -> s.schedule(List.of(scheduleItem)))
                .setupDelivery(d -> d
                        .setDeliverySchedulingType(EOrderDeliverySchedulingType.TO_REQUESTED_TIME)
                        .setRequestedDeliveryTime(time))
                .build();
    }

    private void assertNoErrors(CreateOrderActualizationBuilder actualizationBuilder) {
        var actualization = shopScheduleActualizer.actualize(buildOrderActualization(actualizationBuilder));
        Assertions.assertThat(actualization.getErrors()).isEmpty();
    }

    private void assertHasScheduleError(CreateOrderActualizationBuilder actualizationBuilder) {
        var actualization = shopScheduleActualizer.actualize(buildOrderActualization(actualizationBuilder));
        Assertions.assertThat(actualization.getErrors())
                .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                .contains(new ActualizationError()
                        .setCode(ActualizationError.Code.SHOP_SCHEDULE_MISMATCH)
                );
    }

}
