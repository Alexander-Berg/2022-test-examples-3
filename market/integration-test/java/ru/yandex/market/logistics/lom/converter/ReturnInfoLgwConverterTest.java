package ru.yandex.market.logistics.lom.converter;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.common.model.fulfillment.PartnerInfo;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ReturnInfo;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ReturnType;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.converter.lgw.fulfillment.ReturnInfoLgwConverter;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.PartnerSettings;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.embedded.Credentials;
import ru.yandex.market.logistics.lom.entity.embedded.Sender;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.entity.enums.PlatformClient;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;
import ru.yandex.market.logistics.lom.entity.enums.ShipmentType;

@DisplayName("Конвертация информации о возврате заказа")
public class ReturnInfoLgwConverterTest extends AbstractContextualTest {
    private static final long SENDER_ID = 200;
    private static final String INCORPORATION = "Test incorporation";

    @Autowired
    private ReturnInfoLgwConverter converter;

    @Test
    @DisplayName("Возврат в магазин для ЯДо")
    void returnToYadoSender() {
        softly.assertThat(
                converter.toExternal(new WaybillSegment(), createOrder(PlatformClient.YANDEX_DELIVERY))
            )
            .isEqualTo(
                new ReturnInfo(
                    createPartnerInfo(String.valueOf(SENDER_ID), INCORPORATION),
                    null,
                    ReturnType.SHOP
                )
            );
    }

    @Test
    @DisplayName("Заказ YANDEX_GO, возврат c СЦ/дропоффа в магазин")
    void yandexGoOrderReturnToSender() {
        WaybillSegment yandexGoSegment = new WaybillSegment()
            .setSegmentType(SegmentType.NO_OPERATION);
        WaybillSegment sortingCenterSegment = new WaybillSegment()
            .setSegmentType(SegmentType.SORTING_CENTER)
            .setReturnWaybillSegment(yandexGoSegment);
        Order order = createOrder(PlatformClient.YANDEX_GO);
        order.setWaybill(List.of(yandexGoSegment, sortingCenterSegment));

        ReturnInfo expectedReturnInfo = new ReturnInfo(
            createPartnerInfo(String.valueOf(SENDER_ID), INCORPORATION),
            null,
            ReturnType.SHOP
        );
        softly.assertThat(converter.toExternal(sortingCenterSegment, order)).isEqualTo(expectedReturnInfo);
    }

    @Test
    @DisplayName("Заказ YANDEX_GO, возврат c СЦ в предыдущий СЦ/дропофф")
    void yandexGoOrderReturnToSortingCenter() {
        long partnerId = 100;
        String incorporation = "incorporation";

        WaybillSegment yandexGoSegment = new WaybillSegment()
            .setSegmentType(SegmentType.NO_OPERATION);
        WaybillSegment dropOffSegment = new WaybillSegment()
            .setSegmentType(SegmentType.SORTING_CENTER)
            .setReturnWaybillSegment(yandexGoSegment)
            .setPartnerId(partnerId)
            .setPartnerInfo(
                new WaybillSegment.PartnerInfo()
                    .setCredentials(new Credentials().setIncorporation(incorporation))
            );
        WaybillSegment sortingCenterSegment = new WaybillSegment()
            .setSegmentType(SegmentType.SORTING_CENTER)
            .setReturnWaybillSegment(dropOffSegment);
        Order order = createOrder(PlatformClient.YANDEX_GO);
        order.setWaybill(List.of(yandexGoSegment, dropOffSegment, sortingCenterSegment));

        ReturnInfo expectedReturnInfo = new ReturnInfo(
            createPartnerInfo(String.valueOf(partnerId), incorporation),
            null,
            ReturnType.WAREHOUSE
        );
        softly.assertThat(converter.toExternal(sortingCenterSegment, order)).isEqualTo(expectedReturnInfo);
    }

    @Test
    @DisplayName("Отсутствует return сегмент")
    void noReturnSegment() {
        softly.assertThat(
                converter.toExternal(
                    createDirectSegment(null),
                    createOrder(PlatformClient.BERU)
                )
            )
            .isNull();
    }

    @Test
    @DisplayName("Забирает дропшип")
    void returnSegmentIsDropshipWithImport() {
        softly.assertThat(
                converter.toExternal(
                    createDirectSegment(
                        createReturnSegment(PartnerType.DROPSHIP, SegmentType.FULFILLMENT, null)
                    ),
                    createOrder(PlatformClient.BERU)
                )
            )
            .isEqualTo(
                new ReturnInfo(
                    createPartnerInfo("1", INCORPORATION),
                    createPartnerInfo("1", INCORPORATION),
                    ReturnType.SHOP
                )
            );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Различные типы возвратных сегментов")
    void differentReturnSegments(String description, WaybillSegment returnSegment, ReturnType returnType) {
        softly.assertThat(
                converter.toExternal(
                    createDirectSegment(returnSegment),
                    createOrder(PlatformClient.BERU)
                )
            )
            .isEqualTo(
                new ReturnInfo(
                    createPartnerInfo("1", INCORPORATION),
                    null,
                    returnType
                )
            );
    }

    private static Stream<Arguments> differentReturnSegments() {
        return Stream.of(
            Arguments.of(
                "Возвращаем в дропофф",
                createReturnSegment(PartnerType.DELIVERY, SegmentType.SORTING_CENTER, null),
                ReturnType.DROPOFF
            ),
            Arguments.of(
                "Возвращаем в СЦ",
                createReturnSegment(PartnerType.SORTING_CENTER, SegmentType.SORTING_CENTER, null),
                ReturnType.WAREHOUSE
            ),
            Arguments.of(
                "Возвращаем в ФФ",
                createReturnSegment(PartnerType.FULFILLMENT, SegmentType.FULFILLMENT, null),
                ReturnType.WAREHOUSE
            )
        );
    }

    @Nonnull
    private static PartnerInfo createPartnerInfo(String partnerId, String incorporation) {
        return new PartnerInfo(partnerId, incorporation);
    }

    @Nonnull
    private static WaybillSegment createDirectSegment(@Nullable WaybillSegment returnWaybillSegment) {
        return new WaybillSegment().setReturnWaybillSegment(returnWaybillSegment);
    }

    @Nonnull
    private static Order createOrder(PlatformClient platformClient) {
        return new Order()
            .setSender(new Sender().setId(SENDER_ID).setName(INCORPORATION))
            .setCredentials(new Credentials())
            .setPlatformClient(platformClient);
    }

    @Nonnull
    private static WaybillSegment createReturnSegment(
        PartnerType partnerType,
        SegmentType segmentType,
        @Nullable ShipmentType shipmentType
    ) {
        return new WaybillSegment()
            .setPartnerId(1L)
            .setPartnerType(partnerType)
            .setSegmentType(segmentType)
            .setWaybillShipment(new WaybillSegment.WaybillShipment().setType(shipmentType))
            .setPartnerInfo(
                new WaybillSegment.PartnerInfo().setCredentials(
                    new Credentials().setIncorporation(INCORPORATION)
                )
            )
            .setPartnerSettings(PartnerSettings.builder().dropoff(false).build());
    }
}
