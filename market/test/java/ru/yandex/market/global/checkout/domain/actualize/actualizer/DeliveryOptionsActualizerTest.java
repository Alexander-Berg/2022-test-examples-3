package ru.yandex.market.global.checkout.domain.actualize.actualizer;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.TestTimeUtil;
import ru.yandex.market.global.checkout.domain.actualize.OrderActualization;
import ru.yandex.market.global.checkout.factory.TestOrderFactory.CreateOrderActualizationBuilder;
import ru.yandex.market.global.common.test.TestClock;
import ru.yandex.mj.generated.server.model.OrderDeliveryIntervalDto;
import ru.yandex.mj.generated.server.model.ScheduleItemDto;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.global.checkout.factory.TestOrderFactory.buildOrderActualization;
import static ru.yandex.mj.generated.server.model.OrderDeliverySchedulingType.NOW;
import static ru.yandex.mj.generated.server.model.OrderDeliverySchedulingType.TO_REQUESTED_TIME;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class DeliveryOptionsActualizerTest extends BaseFunctionalTest {

    private final TestClock clock;
    private final DeliveryOptionsActualizer actualizer;

    @Test
    public void testInstantDeliveryAvailable() {
        clock.setTime(today(12).toInstant());

        OrderActualization actualization = actualizer.actualize(createActualization());

        assertThat(actualization.getDeliveryOptionsDto().getSchedulingTypes())
                .containsExactlyInAnyOrder(NOW, TO_REQUESTED_TIME);
    }

    @Test
    public void testInstantDeliveryNotAvailable() {
        clock.setTime(today(20).plusMinutes(1).toInstant());

        OrderActualization actualization = actualizer.actualize(createActualization());

        assertThat(actualization.getDeliveryOptionsDto().getSchedulingTypes())
                .containsExactlyInAnyOrder(TO_REQUESTED_TIME);
    }

    @Test
    public void testDeliveryIntervalsDayStart() {
        clock.setTime(today(0).toInstant());

        OrderActualization actualization = actualizer.actualize(createActualization());

        assertThat(actualization.getDeliveryOptionsDto().getSchedulingTypes()).contains(TO_REQUESTED_TIME);
        assertThat(actualization.getDeliveryOptionsDto().getPossibleDeliveryTimes()).containsExactly(
                interval(today(13)),
                interval(today(14)),
                interval(today(15)),
                interval(today(16)),
                interval(today(17)),

                interval(tomorrow(13)),
                interval(tomorrow(14)),
                interval(tomorrow(15)),
                interval(tomorrow(16)),
                interval(tomorrow(17))
        );
    }

    @Test
    public void testDeliveryIntervalsDayMiddle() {
        clock.setTime(today(13).plusMinutes(20).toInstant());

        OrderActualization actualization = actualizer.actualize(createActualization());

        assertThat(actualization.getDeliveryOptionsDto().getSchedulingTypes()).contains(TO_REQUESTED_TIME);
        assertThat(actualization.getDeliveryOptionsDto().getPossibleDeliveryTimes()).containsExactly(
                interval(today(15)),
                interval(today(16)),
                interval(today(17)),

                interval(tomorrow(13)),
                interval(tomorrow(14)),
                interval(tomorrow(15)),
                interval(tomorrow(16)),
                interval(tomorrow(17))
        );
    }

    @Test
    public void testDeliveryIntervalsDayEnd() {
        clock.setTime(today(19).toInstant());

        OrderActualization actualization = actualizer.actualize(createActualization());

        assertThat(actualization.getDeliveryOptionsDto().getSchedulingTypes()).contains(TO_REQUESTED_TIME);
        assertThat(actualization.getDeliveryOptionsDto().getPossibleDeliveryTimes()).containsExactly(
                interval(tomorrow(13)),
                interval(tomorrow(14)),
                interval(tomorrow(15)),
                interval(tomorrow(16)),
                interval(tomorrow(17))
        );
    }

    @Test
    public void testDeliveryIntervalsPrevDayEnd() {
        clock.setTime(today(19).minusDays(1).toInstant());

        OrderActualization actualization = actualizer.actualize(createActualization());

        assertThat(actualization.getDeliveryOptionsDto().getSchedulingTypes()).contains(TO_REQUESTED_TIME);
        assertThat(actualization.getDeliveryOptionsDto().getPossibleDeliveryTimes()).containsExactly(
                interval(today(13)),
                interval(today(14)),
                interval(today(15)),
                interval(today(16)),
                interval(today(17))
        );
    }

    @Test
    public void testDeliveryIntervalsDayMiddleBeforeShabat() {
        clock.setTime(shabat(13).plusMinutes(20).minusDays(1).toInstant());

        OrderActualization actualization = actualizer.actualize(createActualization());

        assertThat(actualization.getDeliveryOptionsDto().getSchedulingTypes()).contains(TO_REQUESTED_TIME);
        assertThat(actualization.getDeliveryOptionsDto().getPossibleDeliveryTimes()).containsExactly(
                interval(shabat(15).minusDays(1)),
                interval(shabat(16).minusDays(1)),
                interval(shabat(17).minusDays(1)),

                interval(shabat(13).plusDays(1)),
                interval(shabat(14).plusDays(1)),
                interval(shabat(15).plusDays(1)),
                interval(shabat(16).plusDays(1)),
                interval(shabat(17).plusDays(1))
        );
    }

    @Test
    public void testDeliveryIntervalsMidShabat() {
        clock.setTime(shabat(13).plusMinutes(20).toInstant());

        OrderActualization actualization = actualizer.actualize(createActualization());

        assertThat(actualization.getDeliveryOptionsDto().getSchedulingTypes()).contains(TO_REQUESTED_TIME);
        assertThat(actualization.getDeliveryOptionsDto().getPossibleDeliveryTimes()).containsExactly(
                interval(shabat(13).plusDays(1)),
                interval(shabat(14).plusDays(1)),
                interval(shabat(15).plusDays(1)),
                interval(shabat(16).plusDays(1)),
                interval(shabat(17).plusDays(1))
        );
    }

    private OrderActualization createActualization() {
        return buildOrderActualization(CreateOrderActualizationBuilder.builder()
                .setupShop(s -> s.schedule(buildShabatAwareSchedule()))
                .build());
    }

    private OffsetDateTime today(int hour) {
        return atDay(7, hour); //MON
    }

    private OffsetDateTime tomorrow(int hour) {
        return atDay(8, hour); //TUE
    }

    private OffsetDateTime shabat(int hour) {
        return atDay(11, hour); //FRI
    }

    private OffsetDateTime atDay(int day, int hour) {
        return TestTimeUtil.getTimeInDefaultTZ(LocalDateTime.of(2022, 2, day, hour, 0));
    }

    private OrderDeliveryIntervalDto interval(OffsetDateTime dateTime) {
        return new OrderDeliveryIntervalDto().at(dateTime);
    }

    private List<ScheduleItemDto> buildShabatAwareSchedule() {
        return List.of(
                new ScheduleItemDto()
                        .open(true)
                        .day("MON")
                        .startAt("12:00:00")
                        .endAt("18:00:00"),
                new ScheduleItemDto()
                        .open(true)
                        .day("TUE")
                        .startAt("12:00:00")
                        .endAt("18:00:00"),
                new ScheduleItemDto()
                        .open(true)
                        .day("WED")
                        .startAt("12:00:00")
                        .endAt("18:00:00"),
                new ScheduleItemDto()
                        .open(true)
                        .day("THU")
                        .startAt("12:00:00")
                        .endAt("18:00:00"),
                new ScheduleItemDto()
                        .open(true)
                        .day("SAT")
                        .startAt("12:00:00")
                        .endAt("18:00:00"),
                new ScheduleItemDto()
                        .open(true)
                        .day("SUN")
                        .startAt("12:00:00")
                        .endAt("18:00:00")
        );
    }

}
