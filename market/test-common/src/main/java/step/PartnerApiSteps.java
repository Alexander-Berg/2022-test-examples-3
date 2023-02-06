package step;

import java.util.List;
import java.util.Map;

import client.PartnerClient;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import toolkit.Retrier;

import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;

public class PartnerApiSteps {

    private final PartnerClient partnerClient;

    public PartnerApiSteps(long yandexuid, long campaignId) {
        partnerClient = new PartnerClient(yandexuid, campaignId);
    }

    @Step("Упаковка дропшип заказа")
    public void packOrder(Order order) {
        List<Parcel> parcels = order.getDelivery().getParcels();
        Assertions.assertFalse(parcels.isEmpty(), "У заказа нет посылок " + order.getId());
        Map<Long, OrderItem> itemsMapById = order.getItemsMapById();
        Assertions.assertFalse(itemsMapById.isEmpty(), "У заказа нет итемов " + order.getId());
        Retrier.retry(() -> partnerClient.packOrder(
            order.getId(),
            parcels.get(0).getId(),
            itemsMapById.entrySet().iterator().next().getValue().getId())
        );
    }

}
