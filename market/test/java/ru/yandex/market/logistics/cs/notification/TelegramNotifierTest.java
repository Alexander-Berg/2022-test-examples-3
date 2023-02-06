package ru.yandex.market.logistics.cs.notification;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.cs.dbqueue.notifications.telegram.TelegramNotificationsProducer;
import ru.yandex.market.logistics.cs.domain.dto.CapacityWarnDto;
import ru.yandex.market.logistics.cs.domain.dto.DayOffDto;
import ru.yandex.market.logistics.cs.domain.dto.NotifyMessageDto;
import ru.yandex.market.logistics.cs.domain.dto.PartnerDto;
import ru.yandex.market.logistics.cs.domain.entity.CapacityCounterNotification;
import ru.yandex.market.logistics.cs.domain.entity.CapacityValueCounter;
import ru.yandex.market.logistics.cs.domain.enumeration.CapacityValueWarnPercent;
import ru.yandex.market.logistics.cs.domain.enumeration.CounterOverflowReason;
import ru.yandex.market.logistics.cs.domain.enumeration.NotificationType;
import ru.yandex.market.logistics.cs.domain.enumeration.TelegramChannel;
import ru.yandex.market.logistics.cs.notifications.TelegramNotifier;
import ru.yandex.market.logistics.cs.notifications.counter.ICapacityCounterNotifiable;
import ru.yandex.market.logistics.cs.notifications.dayoff.IDayOffNotifiable;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Проверка отправления нотификаций")
public class TelegramNotifierTest {

    private static final String TRUE_VALUE = "1";
    private static final String FALSE_VALUE = "0";
    private static final String MESSAGE_TEXT = "some text";

    private final TelegramNotificationsProducer producer = mock(TelegramNotificationsProducer.class);
    private final TelegramNotifier notifier = new TelegramNotifier(producer);

    @BeforeEach
    void setUp() {
        doNothing().when(producer).enqueue(any(), any());
    }

    @AfterEach
    void after() {
        verifyNoMoreInteractions(producer);
    }

    @DisplayName("Отправление нотификаций о дейоффе")
    @ParameterizedTest(name = "[" + INDEX_PLACEHOLDER + "] {0}")
    @MethodSource("data")
    void dayOffNotify(
        @SuppressWarnings("unused") String displayName,
        PartnerDto dto,
        @Nullable TelegramChannel channel
    ) {
        IDayOffNotifiable notifiable = new IDayOffNotifiable() {
            @Override
            public boolean match(@Nonnull CapacityValueCounter counter) {
                return true;
            }

            @Override
            @Nonnull
            public String getMessageText(@Nonnull NotifyMessageDto dto) {
                return MESSAGE_TEXT;
            }

            @Override
            public NotificationType getNotificationType() {
                return NotificationType.DAY_OFF_SET;
            }

            @Override
            public boolean validatePartnerType(@Nonnull PartnerDto partner) {
                return true;
            }
        };

        notifier.sendNotification(DayOffDto.builder().partner(dto).build(), notifiable);
        if (channel != null) {
            verify(producer).enqueue(channel, MESSAGE_TEXT);
        }
    }

    @DisplayName("Отправление нотификаций о переливе")
    @ParameterizedTest(name = "[" + INDEX_PLACEHOLDER + "] {0}")
    @MethodSource("overflowData")
    void overflowNotify(
        @SuppressWarnings("unused") String displayName,
        PartnerDto dto,
        CounterOverflowReason reason,
        List<TelegramChannel> channels
    ) {
        notifier.sendNotification(
            CapacityWarnDto.builder().partner(dto).eventReason(reason).build(),
            getOverflowNotifiable(NotificationType.COUNTER_OVERFLOW)
        );

        for (TelegramChannel channel : channels) {
            verify(producer).enqueue(channel, MESSAGE_TEXT);
        }
    }

    private static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(
                "Нотификация дейоффа Дропшипа",
                dtoBuilder(PartnerType.DROPSHIP).build(),
                TelegramChannel.DROPSHIP
            ),
            Arguments.of(
                "Нотификация дейоффа СЦ",
                dtoBuilder(PartnerType.SORTING_CENTER).build(),
                TelegramChannel.SORTING_CENTER
            ),
            Arguments.of(
                "Нотификация дейоффа СД",
                dtoBuilder(PartnerType.DELIVERY).build(),
                TelegramChannel.DELIVERY
            ),
            Arguments.of(
                "Нотификация дейоффа ФФ",
                dtoBuilder(PartnerType.FULFILLMENT).build(),
                TelegramChannel.WAREHOUSE
            ),
            Arguments.of(
                "Нотификация дейоффа МК с сабтипом Маркет Курьер",
                dtoBuilder(PartnerType.DELIVERY).partnerSubtype(subtypeResponse(2L)).build(),
                TelegramChannel.MARKET_COURIER
            ),
            Arguments.of(
                "Нотификация дейоффа МК с сабтипом Маркет Свои ПВЗ",
                dtoBuilder(PartnerType.DELIVERY).partnerSubtype(subtypeResponse(3L)).build(),
                TelegramChannel.MARKET_COURIER
            ),
            Arguments.of(
                "Нотификация дейоффа МК с сабтипом Маркет Локеры",
                dtoBuilder(PartnerType.DELIVERY).partnerSubtype(subtypeResponse(5L)).build(),
                TelegramChannel.MARKET_COURIER
            ),
            Arguments.of(
                "Нотификация дейоффа МК с сабтипом Такси-Лавка",
                dtoBuilder(PartnerType.DELIVERY).partnerSubtype(subtypeResponse(8L)).build(),
                TelegramChannel.MARKET_COURIER
            ),
            Arguments.of(
                "Нотификация дейоффа Дропоффа",
                dtoBuilder(PartnerType.SORTING_CENTER)
                    .externalParams(List.of(new PartnerExternalParam(
                        PartnerExternalParamType.IS_DROPOFF.name(),
                        "",
                        TRUE_VALUE
                    )))
                    .build(),
                TelegramChannel.DROPOFF
            ),
            Arguments.of(
                "Нотификация дейоффа Дропоффа",
                dtoBuilder(PartnerType.DELIVERY)
                    .externalParams(List.of(new PartnerExternalParam(
                        PartnerExternalParamType.IS_DROPOFF.name(),
                        "",
                        TRUE_VALUE
                    )))
                    .build(),
                TelegramChannel.DROPOFF
            ),
            Arguments.of(
                "Нотификация дейоффа SUPPLIER",
                dtoBuilder(PartnerType.SUPPLIER).build(),
                TelegramChannel.SUPPLIER
            ),
            Arguments.of(
                "Нет нотификаций для типа DISTRIBUTION_CENTER",
                dtoBuilder(PartnerType.DISTRIBUTION_CENTER).build(),
                null
            ),
            Arguments.of(
                "Нет нотификаций для типа DROPSHIP_BY_SELLER",
                dtoBuilder(PartnerType.DROPSHIP_BY_SELLER).build(),
                null
            ),
            Arguments.of(
                "Нет нотификаций для типа XDOC",
                dtoBuilder(PartnerType.XDOC).build(),
                null
            ),
            Arguments.of(
                "Нет нотификаций для типа OWN_DELIVERY",
                dtoBuilder(PartnerType.OWN_DELIVERY).build(),
                null
            ),
            Arguments.of(
                "Нет нотификаций для типа FIRST_PARTY_SUPPLIER",
                dtoBuilder(PartnerType.FIRST_PARTY_SUPPLIER).build(),
                null
            ),
            Arguments.of(
                "Нет нотификаций для типа LINEHAUL",
                dtoBuilder(PartnerType.LINEHAUL).build(),
                null
            ),
            Arguments.of(
                "Нотификация дейоффа СД (не Дропоффа)",
                dtoBuilder(PartnerType.DELIVERY)
                    .externalParams(List.of())
                    .build(),
                TelegramChannel.DELIVERY
            ),
            Arguments.of(
                "Нотификация дейоффа СД (не Дропоффа)",
                dtoBuilder(PartnerType.DELIVERY)
                    .externalParams(List.of(new PartnerExternalParam(
                        PartnerExternalParamType.IS_DROPOFF.name(),
                        "",
                        FALSE_VALUE
                    )))
                    .build(),
                TelegramChannel.DELIVERY
            ),
            Arguments.of(
                "Нотификация дейоффа СЦ (не Дропоффа)",
                dtoBuilder(PartnerType.SORTING_CENTER)
                    .externalParams(List.of())
                    .build(),
                TelegramChannel.SORTING_CENTER
            ),
            Arguments.of(
                "Нотификация дейоффа СЦ (не Дропоффа)",
                dtoBuilder(PartnerType.SORTING_CENTER)
                    .externalParams(List.of(new PartnerExternalParam(
                        PartnerExternalParamType.IS_DROPOFF.name(),
                        "",
                        FALSE_VALUE
                    )))
                    .build(),
                TelegramChannel.SORTING_CENTER
            )
        );
    }

    private static Stream<Arguments> overflowData() {
        return Stream.of(
            Arguments.of(
                "Нотификация перелива Дропшипа",
                dtoBuilder(PartnerType.DROPSHIP).build(),
                CounterOverflowReason.NEW_ORDER,
                List.of(TelegramChannel.CAPACITY_OVERFLOW, TelegramChannel.DROPSHIP)
            ),
            Arguments.of(
                "Нет нотификации перелива Дропшипа из-за ПДД в канал о переливах",
                dtoBuilder(PartnerType.DROPSHIP).build(),
                CounterOverflowReason.ORDER_ROUTE_RECALCULATED,
                List.of(TelegramChannel.DROPSHIP)
            )
        );
    }

    private static PartnerSubtypeResponse subtypeResponse(long subtype) {
        return PartnerSubtypeResponse.newBuilder().id(subtype).build();
    }

    private static PartnerDto.PartnerDtoBuilder dtoBuilder(PartnerType type) {
        return PartnerDto.builder().partnerType(type).partnerSubtype(subtypeResponse(-1));
    }

    private ICapacityCounterNotifiable getOverflowNotifiable(NotificationType type) {
        return new ICapacityCounterNotifiable() {

            @Override
            public boolean match(
                CapacityValueCounter capacityValueCounter,
                Optional<CapacityCounterNotification> notificationDto
            ) {
                return true;
            }

            @NotNull
            @Override
            public CapacityValueWarnPercent getCapacityValueWarnPercent() {
                return CapacityValueWarnPercent.OVERFLOW;
            }

            @Override
            public void markNotificationAsSend(CapacityCounterNotification capacityCounterNotification) {

            }

            @Override
            public NotificationType getNotificationType() {
                return type;
            }

            @NotNull
            @Override
            public String getMessageText(NotifyMessageDto dto) {
                return MESSAGE_TEXT;
            }

            @Override
            public boolean validatePartnerType(PartnerDto partner) {
                return true;
            }
        };
    }
}
