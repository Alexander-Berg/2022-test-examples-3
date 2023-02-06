package ru.yandex.market.logistics.logistics4go.enums;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.logistics.logistics4go.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4go.client.model.CancellationRequestReason;
import ru.yandex.market.logistics.logistics4go.client.model.ChangeRequestReason;
import ru.yandex.market.logistics.logistics4go.client.model.ChangeRequestType;
import ru.yandex.market.logistics.logistics4go.client.model.DeliveryType;
import ru.yandex.market.logistics.logistics4go.client.model.PartnerType;
import ru.yandex.market.logistics.logistics4go.client.model.PaymentMethod;
import ru.yandex.market.logistics.logistics4go.client.model.ShipmentType;
import ru.yandex.market.logistics.logistics4go.client.model.VatRate;
import ru.yandex.market.logistics.logistics4go.client.model.WaybillSegmentStatus;
import ru.yandex.market.logistics.logistics4go.client.model.WaybillSegmentType;
import ru.yandex.market.logistics.logistics4go.converter.EnumConverter;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderReason;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestReason;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.lom.model.enums.VatType;

@DisplayName("Корректность конвертации енумок из LOM в енумки апи L4G")
class LomEnumConversionTest extends AbstractIntegrationTest {

    private static final List<Pair<Class<?>, Class<?>>> API_TO_LOM_ENUM_MAPPING = List.of(
        Pair.of(DeliveryType.class, ru.yandex.market.logistics.lom.model.enums.DeliveryType.class),
        Pair.of(PaymentMethod.class, ru.yandex.market.logistics.lom.model.enums.PaymentMethod.class),
        Pair.of(ShipmentType.class, ru.yandex.market.logistics.lom.model.enums.ShipmentType.class),
        Pair.of(VatRate.class, VatType.class),
        Pair.of(ChangeRequestReason.class, ChangeOrderRequestReason.class),
        Pair.of(ChangeRequestType.class, ChangeOrderRequestType.class),
        Pair.of(PartnerType.class, ru.yandex.market.logistics.lom.model.enums.PartnerType.class),
        Pair.of(WaybillSegmentType.class, SegmentType.class),
        Pair.of(WaybillSegmentStatus.class, SegmentStatus.class)
    );

    /*
     * Сюда нужно добавлять енумки L4G и значения из LOM, которые ей BY DESIGN не покрываются.
     */
    private static final Map<Class<?>, Set<String>> API_ENUM_VALUE_BLACK_LIST_BY_TYPE = Map.of(
        DeliveryType.class, Set.of("POST", "MOVEMENT"),
        ShipmentType.class, Set.of("WITHDRAW"),
        ChangeRequestReason.class, Set.of(
            "DELIVERY_DATE_UPDATED_BY_ROUTE_RECALCULATION",
            "PRE_DELIVERY_ROUTE_RECALCULATION",
            "DELIVERY_DATE_UPDATED_BY_USER",
            "SHIPPING_DELAYED",
            "CALL_COURIER_BY_USER",
            "DELIVERY_SERVICE_PROBLEM",
            "PROCESSING_DELAYED_BY_PARTNER",
            "LAST_MILE_CHANGED_BY_USER",
            "DIMENSIONS_EXCEEDED_LOCKER"
        ),
        ChangeRequestType.class, Set.of(
            "ORDER_ITEM_IS_NOT_SUPPLIED",
            "ORDER_CHANGED_BY_PARTNER",
            "ITEM_NOT_FOUND",
            "DELIVERY_OPTION",
            "UPDATE_TRANSFER_CODES",
            "UPDATE_COURIER",
            "CHANGE_TO_ON_DEMAND",
            "RECALCULATE_ROUTE_DATES",
            "LAST_MILE",
            "CHANGE_LAST_MILE_TO_COURIER",
            "CHANGE_LAST_MILE_TO_PICKUP",
            "UPDATE_PLACES",
            "CHANGE_LAST_MILE_FROM_PICKUP_TO_PICKUP",
            "UNKNOWN"
        ),
        PartnerType.class, Set.of(
            "FULFILLMENT",
            "OWN_DELIVERY",
            "DROPSHIP",
            "SUPPLIER",
            "DROPSHIP_BY_SELLER"
        ),
        WaybillSegmentType.class, Set.of(
            "SUPPLIER",
            "FULFILLMENT"
        )
    );

    private final EnumConverter enumConverter = new EnumConverter();

    @DisplayName("Тест на конвертацию енумок LOM в енумки L4G")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("argumentsGeneral")
    <T extends Enum<T>, U extends Enum<U>> void tryConvert(
        String caseName,
        Class<T> l4gEnum,
        Class<U> lomEnum
    ) {
        Set<String> excludedNames = Optional.ofNullable(API_ENUM_VALUE_BLACK_LIST_BY_TYPE.get(l4gEnum))
            .orElse(Set.of());

        StreamEx.of(lomEnum.getEnumConstants())
            .filter(value -> !excludedNames.contains(value.name()))
            .mapToEntry(Enum::name, value -> enumConverter.convert(value, l4gEnum))
            .forKeyValue(
                (key, value) -> softly.assertThat(value)
                    .withFailMessage(String.format(
                        "Failed to convert value %s from LOM enum %s to API enum",
                        key,
                        lomEnum.getSimpleName()
                    ))
                    .isNotNull()
            );
    }

    @Nonnull
    static Stream<Arguments> argumentsGeneral() {
        return API_TO_LOM_ENUM_MAPPING.stream()
            .map(
                mapping -> Arguments.of(
                    mapping.getSecond().getSimpleName(),
                    mapping.getFirst(),
                    mapping.getSecond()
                )
            );
    }

    @DisplayName("Тест на конвертацию причин отмены из LOM в L4G")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("argumentsCancellationReason")
    void cancellationReason(
        CancellationOrderReason<?> lomReason,
        CancellationRequestReason expectedL4gReason
    ) {
        softly
            .assertThat(enumConverter.convert(lomReason.getName(), CancellationRequestReason.class))
            .isEqualTo(expectedL4gReason);
    }

    @Nonnull
    static Stream<Arguments> argumentsCancellationReason() {
        return Stream.of(
            Arguments.of(
                CancellationOrderReason.DELIVERY_SERVICE_LOST,
                CancellationRequestReason.DELIVERY_SERVICE_LOST
            ),
            Arguments.of(
                CancellationOrderReason.SHOP_CANCELLED,
                CancellationRequestReason.SHOP_CANCELLED
            ),
            Arguments.of(
                CancellationOrderReason.DELIVERY_SERVICE_UNDELIVERED,
                CancellationRequestReason.DELIVERY_SERVICE_UNDELIVERED
            ),
            Arguments.of(
                CancellationOrderReason.MISSING_ITEM,
                null
            ),
            Arguments.of(
                CancellationOrderReason.USER_CHANGED_MIND,
                null
            ),
            Arguments.of(
                CancellationOrderReason.USER_REFUSED_DELIVERY,
                null
            ),
            Arguments.of(
                CancellationOrderReason.USER_REFUSED_PRODUCT,
                null
            ),
            Arguments.of(
                CancellationOrderReason.SHOP_FAILED,
                null
            ),
            Arguments.of(
                CancellationOrderReason.USER_REFUSED_QUALITY,
                null
            ),
            Arguments.of(
                CancellationOrderReason.REPLACING_ORDER,
                null
            ),
            Arguments.of(
                CancellationOrderReason.PROCESSING_EXPIRED,
                null
            ),
            Arguments.of(
                CancellationOrderReason.PENDING_EXPIRED,
                null
            ),
            Arguments.of(
                CancellationOrderReason.USER_FRAUD,
                null
            ),
            Arguments.of(
                CancellationOrderReason.USER_PLACED_OTHER_ORDER,
                null
            ),
            Arguments.of(
                CancellationOrderReason.USER_BOUGHT_CHEAPER,
                null
            ),
            Arguments.of(
                CancellationOrderReason.BROKEN_ITEM,
                null
            ),
            Arguments.of(
                CancellationOrderReason.WRONG_ITEM,
                null
            ),
            Arguments.of(
                CancellationOrderReason.PICKUP_EXPIRED,
                null
            ),
            Arguments.of(
                CancellationOrderReason.DELIVERY_PROBLEMS,
                null
            ),
            Arguments.of(
                CancellationOrderReason.LATE_CONTACT,
                null
            ),
            Arguments.of(
                CancellationOrderReason.CUSTOM,
                null
            ),
            Arguments.of(
                CancellationOrderReason.DELIVERY_SERVICE_FAILED,
                null
            ),
            Arguments.of(
                CancellationOrderReason.WAREHOUSE_FAILED_TO_SHIP,
                null
            ),
            Arguments.of(
                CancellationOrderReason.DELIVERY_SERVICE_NOT_RECEIVED,
                null
            ),
            Arguments.of(
                CancellationOrderReason.SHIPPED_TO_WRONG_DELIVERY_SERVICE,
                null
            ),
            Arguments.of(
                CancellationOrderReason.COURIER_NOT_FOUND,
                null
            ),
            Arguments.of(
                CancellationOrderReason.COURIER_NOT_DELIVER_ORDER,
                null
            ),
            Arguments.of(
                CancellationOrderReason.COURIER_RETURNS_ORDER,
                null
            ),
            Arguments.of(
                CancellationOrderReason.COURIER_RETURNED_ORDER,
                null
            ),
            Arguments.of(
                CancellationOrderReason.SERVICE_FAULT,
                null
            ),
            Arguments.of(
                CancellationOrderReason.UNKNOWN,
                CancellationRequestReason.UNKNOWN
            )
        );
    }
}
