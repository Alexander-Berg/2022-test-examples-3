package ru.yandex.market.logistics.lom.admin.converter;

import java.time.Clock;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.front.library.dto.ReferenceObject;
import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.enums.OrderStatus;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.entity.enums.PlatformClient;


@DisplayName("Конвертация заказа в зависимости от того заказ с Яндекс Доставки и дропшиповый ли он")
class AdminReferenceObjectConverterTest extends AbstractTest {

    private AdminReferenceObjectConverter converter = new AdminReferenceObjectConverter();

    @Test
    void daasOrderTest() {
        Order order = order();
        ReferenceObject referenceObject = converter.nesuSender(order);
        softly.assertThat(referenceObject.getSlug()).isEqualTo("nesu/senders");
        softly.assertThat(referenceObject.getId()).isNotNull();
    }

    @Test
    void dropshipOrderTest() {
        Order order = order()
            .setPlatformClient(PlatformClient.BERU)
            .setWaybill(List.of(
                new WaybillSegment()
                    .setId(1L)
                    .setPartnerType(PartnerType.DROPSHIP)
        ));
        ReferenceObject referenceObject = converter.nesuSender(order);
        softly.assertThat(referenceObject.getSlug()).isEqualTo("nesu/shops");
        softly.assertThat(referenceObject.getId()).isNotNull();
    }

    @Test
    void noDropshipNoDaasOrderTest() {
        Order order = order()
            .setPlatformClient(PlatformClient.BERU)
            .setWaybill(List.of(
                new WaybillSegment()
                    .setId(1L)
                    .setPartnerType(PartnerType.FULFILLMENT)
        ));
        ReferenceObject referenceObject = converter.nesuSender(order);
        softly.assertThat(referenceObject.getSlug()).isEqualTo(null);
        softly.assertThat(referenceObject.getId()).isNull();
    }

    @Nonnull
    private Order order() {
        return new Order()
            .setStatus(OrderStatus.PROCESSING, Clock.systemUTC())
            .setPlatformClient(PlatformClient.YANDEX_DELIVERY)
            .setId(1L);
    }
}
