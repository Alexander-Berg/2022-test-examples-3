package step;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import client.TristeroClient;
import dto.requests.checkouter.Address;
import dto.requests.checkouter.CreateOrderParameters;
import dto.requests.checkouter.OrderComment;
import dto.requests.checkouter.RearrFactor;
import dto.requests.report.OfferItem;
import dto.responses.lavka.LavkaItem;
import dto.responses.lavka.LavkaParcelState;
import dto.responses.lavka.TristeroOrderResponse;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import toolkit.Retrier;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;

@Slf4j
@Resource.Classpath({"delivery/checkouter.properties", "delivery/delivery.properties", "delivery/report.properties"})
public class TristeroOrderSteps {

    private static final TristeroClient TRISTERO_CLIENT = new TristeroClient();
    private static final CheckouterSteps CHECKOUTER_STEPS = new CheckouterSteps();
    @Property("checkouter.regionId")
    private long regionId;
    @Property("delivery.ondemandDS")
    private long lavkaServiceId;

    public TristeroOrderSteps() {
        PropertyLoader.newInstance().populate(this);
    }

    @Step("Получаем в лавке orderInfo")
    public TristeroOrderResponse getOrderInfo(long orderId) {
        return Retrier.retry(() -> TRISTERO_CLIENT.getLavkaOrderInfo(orderId));
    }

    @Step("Создаем заказ в лавке")
    public Order createLavkaOrder(List<OfferItem> lavkaItem, Address address) {
        CreateOrderParameters params = CreateOrderParameters
            .newBuilder(regionId, lavkaItem, DeliveryType.DELIVERY)
            .address(address)
            .paymentType(PaymentType.PREPAID)
            .paymentMethod(PaymentMethod.YANDEX)
            .experiment(EnumSet.of(RearrFactor.LAVKA, RearrFactor.COMBINATORONDEMAND))
            .forceDeliveryId(lavkaServiceId)
            .comment(OrderComment.FIND_COURIER_FASTER)
            .build();
        Order order = CHECKOUTER_STEPS.createOrder(params);
        CHECKOUTER_STEPS.payOrder(order);
        return order;
    }

    @Step("Получаем в лавке id посылки по индексу")
    public List<String> getParcelIds(long orderId) {
        return Retrier.retry(() ->
            TRISTERO_CLIENT.getLavkaOrderInfo(orderId)
                .getItems().stream()
                .map(LavkaItem::getId)
                .collect(Collectors.toList())
        );
    }

    @Step("Получаем в лавке id посылки по штрихкоду {barcode}")
    public String getParcelIdByBarcode(long orderId, String barcode) {
        return Retrier.retry(() -> TRISTERO_CLIENT.getLavkaOrderInfo(orderId).getItems()
            .stream()
            .filter(item -> item.getBarcode().equals(barcode))
            .findAny()
            .orElseThrow(() -> new AssertionError("Не нашли итем с баркодом " + barcode))
            .getId());
    }

    @Step("Меняем статус посылок в лавке на {status}")
    public void setStatus(long orderId, LavkaParcelState status) {
        for (String parcelId : getParcelIds(orderId)) {
            setStatus(parcelId, status);
        }
    }

    @Step("Меняем статус посылки в лавке на {status}")
    public void setStatus(String parcelId, LavkaParcelState status) {
        Retrier.retry(() -> TRISTERO_CLIENT.setStatus(parcelId, status.getValue()));
    }

    @Step("Проверяем, что заказ разобьется на 2 коробки в Лавке")
    public void verifyLavkaOrderHasTwoParcels(Long orderId) {
        Retrier.retry(() ->
            Assertions.assertEquals(2, TRISTERO_CLIENT.getLavkaOrderInfo(orderId).getItems().size(),
                "Заказ в лавке не разбился на 2 посылки"));
    }

}
