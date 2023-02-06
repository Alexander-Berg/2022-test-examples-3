package ru.yandex.market.logistics.logistics4shops.converter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.protobuf.Timestamp;
import com.google.type.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.les.lom.ShopReadyToCreateOrderEvent;
import ru.yandex.market.logistics.logistics4shops.AbstractTest;
import ru.yandex.market.logistics.logistics4shops.event.model.DropshipOrderCreatedPayload;
import ru.yandex.market.logistics.logistics4shops.event.model.ExpressOrderCreatedPayload;

@DisplayName("Конвертация событий LES в пейлоады создания заказа")
class ShopReadyToCreateOrderEventConverterTest extends AbstractTest {
    private final ShopReadyToCreateOrderEventConverter converter = new ShopReadyToCreateOrderEventConverter();

    private static final List<String> EXPRESS_TAGS = List.of("EXPRESS");
    private static final String ORDER_ID = "1";
    private static final Long SHOP_ID = 2L;
    private static final Long PARTNER_ID = 3L;
    private static final LocalDate LES_SHIPMENT_DATE = LocalDate.of(2022, 2, 20);
    private static final Date LB_SHIPMENT_DATE = Date.newBuilder().setYear(2022).setMonth(2).setDay(20).build();
    private static final Instant LES_SHIPMENT_DATE_TIME = Instant.ofEpochSecond(1645358400L);
    private static final Timestamp LB_SHIPMENT_DATE_TIME = Timestamp.newBuilder().setSeconds(1645358400L).build();
    private static final Long SHIPMENT_LOGISTICS_POINT_ID = 12345654321L;

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Конвертация LES события в пэйлоад создания экспресс заказа")
    void eventToExpressPayload(
        String name,
        ShopReadyToCreateOrderEvent event,
        ExpressOrderCreatedPayload expectedPayload
    ) {
        ExpressOrderCreatedPayload payload = converter.toExpressOrderCreatedPayloadOptional(event).orElse(null);
        softly.assertThat(payload).usingRecursiveComparison().isEqualTo(expectedPayload);
    }

    @Nonnull
    private static Stream<Arguments> eventToExpressPayload() {
        return Stream.of(
            Arguments.of(
                "Успех: все поля заполнены",
                new ShopReadyToCreateOrderEvent(
                    ORDER_ID,
                    SHOP_ID,
                    PARTNER_ID,
                    LES_SHIPMENT_DATE,
                    LES_SHIPMENT_DATE_TIME,
                    EXPRESS_TAGS,
                    SHIPMENT_LOGISTICS_POINT_ID
                ),
                expressPayload()
            ),
            Arguments.of(
                "Неуспех: идентификатор заказа отсутствует",
                new ShopReadyToCreateOrderEvent(
                    null,
                    SHOP_ID,
                    PARTNER_ID,
                    LES_SHIPMENT_DATE,
                    LES_SHIPMENT_DATE_TIME,
                    EXPRESS_TAGS,
                    null
                ),
                null
            ),
            Arguments.of(
                "Неуспех: идентификатор магазина отсутствует",
                new ShopReadyToCreateOrderEvent(
                    ORDER_ID,
                    null,
                    PARTNER_ID,
                    LES_SHIPMENT_DATE,
                    LES_SHIPMENT_DATE_TIME,
                    EXPRESS_TAGS,
                    null
                ),
                null
            ),
            Arguments.of(
                "Успех: идентификатор партнера отсутствует",
                new ShopReadyToCreateOrderEvent(
                    ORDER_ID,
                    SHOP_ID,
                    null,
                    LES_SHIPMENT_DATE,
                    LES_SHIPMENT_DATE_TIME,
                    EXPRESS_TAGS,
                    null
                ),
                expressPayload()
            ),
            Arguments.of(
                "Успех: дата отгрузки отсутствует",
                new ShopReadyToCreateOrderEvent(
                    ORDER_ID,
                    SHOP_ID,
                    PARTNER_ID,
                    null,
                    LES_SHIPMENT_DATE_TIME,
                    EXPRESS_TAGS,
                    null
                ),
                expressPayload()
            ),
            Arguments.of(
                "Неуспех: дедлайн сборки отсутствует",
                new ShopReadyToCreateOrderEvent(
                    ORDER_ID,
                    SHOP_ID,
                    PARTNER_ID,
                    LES_SHIPMENT_DATE,
                    null,
                    EXPRESS_TAGS,
                    null
                ),
                null
            ),
            Arguments.of(
                "Успех: пустой список тегов",
                new ShopReadyToCreateOrderEvent(
                    ORDER_ID,
                    SHOP_ID,
                    PARTNER_ID,
                    LES_SHIPMENT_DATE,
                    LES_SHIPMENT_DATE_TIME,
                    List.of(),
                    null
                ),
                expressPayload()
            ),
            Arguments.of(
                "Успех: список тегов отсутствует",
                new ShopReadyToCreateOrderEvent(
                    ORDER_ID,
                    SHOP_ID,
                    PARTNER_ID,
                    LES_SHIPMENT_DATE,
                    LES_SHIPMENT_DATE_TIME,
                    null,
                    null
                ),
                expressPayload()
            ),
            Arguments.of(
                "Успех: идентификатор точки отгрузки отсутствует",
                new ShopReadyToCreateOrderEvent(
                    ORDER_ID,
                    SHOP_ID,
                    PARTNER_ID,
                    LES_SHIPMENT_DATE,
                    LES_SHIPMENT_DATE_TIME,
                    EXPRESS_TAGS,
                    null
                ),
                expressPayload()
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Конвертация LES события в пэйлоад создания дропшип заказа")
    void eventToDropshipPayload(
        String name,
        ShopReadyToCreateOrderEvent event,
        DropshipOrderCreatedPayload expectedPayload
    ) {
        DropshipOrderCreatedPayload payload = converter.toDropshipOrderCreatedPayloadOptional(event).orElse(null);
        softly.assertThat(payload).usingRecursiveComparison().isEqualTo(expectedPayload);
    }

    @Nonnull
    private static Stream<Arguments> eventToDropshipPayload() {
        return Stream.of(
            Arguments.of(
                "Успех: все поля заполнены",
                new ShopReadyToCreateOrderEvent(
                    ORDER_ID,
                    SHOP_ID,
                    PARTNER_ID,
                    LES_SHIPMENT_DATE,
                    LES_SHIPMENT_DATE_TIME,
                    EXPRESS_TAGS,
                    SHIPMENT_LOGISTICS_POINT_ID
                ),
                dropshipPayload()
            ),
            Arguments.of(
                "Неуспех: идентификатор заказа отсутствует",
                new ShopReadyToCreateOrderEvent(
                    null,
                    SHOP_ID,
                    PARTNER_ID,
                    LES_SHIPMENT_DATE,
                    LES_SHIPMENT_DATE_TIME,
                    EXPRESS_TAGS,
                    null
                ),
                null
            ),
            Arguments.of(
                "Неуспех: идентификатор магазина отсутствует",
                new ShopReadyToCreateOrderEvent(
                    ORDER_ID,
                    null,
                    PARTNER_ID,
                    LES_SHIPMENT_DATE,
                    LES_SHIPMENT_DATE_TIME,
                    EXPRESS_TAGS,
                    null
                ),
                null
            ),
            Arguments.of(
                "Успех: идентификатор партнера отсутствует",
                new ShopReadyToCreateOrderEvent(
                    ORDER_ID,
                    SHOP_ID,
                    null,
                    LES_SHIPMENT_DATE,
                    LES_SHIPMENT_DATE_TIME,
                    EXPRESS_TAGS,
                    null
                ),
                dropshipPayload()
            ),
            Arguments.of(
                "Неуспех: дата отгрузки отсутствует",
                new ShopReadyToCreateOrderEvent(
                    ORDER_ID,
                    SHOP_ID,
                    PARTNER_ID,
                    null,
                    LES_SHIPMENT_DATE_TIME,
                    EXPRESS_TAGS,
                    null
                ),
                null
            ),
            Arguments.of(
                "Успех: дедлайн сборки отсутствует",
                new ShopReadyToCreateOrderEvent(
                    ORDER_ID,
                    SHOP_ID,
                    PARTNER_ID,
                    LES_SHIPMENT_DATE,
                    null,
                    EXPRESS_TAGS,
                    null
                ),
                dropshipPayload()
            ),
            Arguments.of(
                "Успех: пустой список тегов",
                new ShopReadyToCreateOrderEvent(
                    ORDER_ID,
                    SHOP_ID,
                    PARTNER_ID,
                    LES_SHIPMENT_DATE,
                    LES_SHIPMENT_DATE_TIME,
                    List.of(),
                    null
                ),
                dropshipPayload()
            ),
            Arguments.of(
                "Успех: список тегов отсутствует",
                new ShopReadyToCreateOrderEvent(
                    ORDER_ID,
                    SHOP_ID,
                    PARTNER_ID,
                    LES_SHIPMENT_DATE,
                    LES_SHIPMENT_DATE_TIME,
                    null,
                    null
                ),
                dropshipPayload()
            ),
            Arguments.of(
                "Успех: идентификатор точки отгрузки отсутствует",
                new ShopReadyToCreateOrderEvent(
                    ORDER_ID,
                    SHOP_ID,
                    PARTNER_ID,
                    LES_SHIPMENT_DATE,
                    LES_SHIPMENT_DATE_TIME,
                    EXPRESS_TAGS,
                    null
                ),
                dropshipPayload()
            )
        );
    }

    @Nonnull
    private static ExpressOrderCreatedPayload expressPayload() {
        return ExpressOrderCreatedPayload.newBuilder()
            .setOrderId(Long.parseLong(ORDER_ID))
            .setShopId(SHOP_ID)
            .setPackagingDeadline(LB_SHIPMENT_DATE_TIME)
            .build();
    }

    @Nonnull
    private static DropshipOrderCreatedPayload dropshipPayload() {
        return DropshipOrderCreatedPayload.newBuilder()
            .setOrderId(Long.parseLong(ORDER_ID))
            .setShopId(SHOP_ID)
            .setShipmentDate(LB_SHIPMENT_DATE)
            .build();
    }
}
