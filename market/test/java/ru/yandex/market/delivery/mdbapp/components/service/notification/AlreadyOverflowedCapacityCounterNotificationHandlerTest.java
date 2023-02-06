package ru.yandex.market.delivery.mdbapp.components.service.notification;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.common.util.region.RegionService;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.CapacityCounter;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.CapacityCounterNotification;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.PartnerCapacity;
import ru.yandex.market.logistics.management.client.LMSClient;

@ExtendWith(SpringExtension.class)
public class AlreadyOverflowedCapacityCounterNotificationHandlerTest {
    @MockBean
    LMSClient lmsClient;
    @MockBean
    RegionService regionService;
    @MockBean
    ApplicationEventPublisher eventPublisher;
    AlreadyOverflowedCapacityCounterNotificationHandler handler;

    private static Stream<Arguments> testData() {
        return Stream.of(
            Arguments.of(0L, 0L, 0L, false, false),
            // превышение на 180 парселов, но уведомление не отправлялось, результат нет
            Arguments.of(20L, 0L, 200L, false, false),
            // превышение на 200 парселов, но уведомление не отправлялось, результат нет
            Arguments.of(0L, 0L, 200L, false, false),
            Arguments.of(20L, 0L, 20L, true, false),
            Arguments.of(10L, 10L, 11L, false, false),
            Arguments.of(10L, 10L, 20L, false, false),
            // превышение на 10 парселов, уведомление отправлялось, но отправлено и текущее равны, результат нет
            Arguments.of(10L, 20L, 20L, true, false),
            Arguments.of(20L, 20L, 20L, true, false),
            // капасити = 10, отправлено уже 10, всего парселов 20, уведомелние отправлялось, результат ДА
            Arguments.of(10L, 10L, 20L, true, true),
            // парселов больше остальных цифр и уведомелние отправлялось, результат ДА
            Arguments.of(0L, 0L, 20L, true, true),
            Arguments.of(0L, 10L, 20L, true, true),
            Arguments.of(10L, 0L, 20L, true, true)
        );
    }

    @BeforeEach
    void setUp() {
        handler = new AlreadyOverflowedCapacityCounterNotificationHandler(
            lmsClient,
            regionService,
            eventPublisher
        );
    }

    @ParameterizedTest
    @MethodSource("testData")
    void canHandle(
        long capacityValue,
        long lastSentCount,
        long parcelCount,
        boolean overflowPercentNotified,
        boolean result
    ) {
        Assertions.assertEquals(
            handler.canHandle(getCounter(capacityValue, lastSentCount, parcelCount, overflowPercentNotified)),
            result
        );
    }

    CapacityCounter getCounter(
        long capacityValue,
        long lastSentCount,
        long parcelCount,
        boolean overflowPercentNotified
    ) {
        var pc = new PartnerCapacity();
        pc.setCapacityId(123L);
        pc.setValue(capacityValue);

        var n = new CapacityCounterNotification();
        n.setLastSentCount(lastSentCount);
        n.setIs100PercentNotificationSend(overflowPercentNotified);

        var c = new CapacityCounter();
        c.setMaxAllowedParcelCount(0L);
        c.setParcelCount(parcelCount);
        c.setPartnerCapacity(pc);
        c.setCapacityCounterNotification(n);

        n.setCapacityCounter(c);
        n.setCapacityCounterId(c.getId());

        pc.addCapacityCounter(c);
        return c;
    }
}
