package client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import api.LrmApi;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import delivery.client.lrm.client.model.CreateReturnRequest;
import delivery.client.lrm.client.model.CreateReturnResponse;
import delivery.client.lrm.client.model.ReturnBoxRequest;
import delivery.client.lrm.client.model.ReturnCourier;
import delivery.client.lrm.client.model.ReturnItem;
import delivery.client.lrm.client.model.ReturnSource;
import delivery.client.lrm.client.model.SearchReturn;
import delivery.client.lrm.client.model.SearchReturnsRequest;
import delivery.client.lrm.client.model.SearchReturnsResponse;
import lombok.SneakyThrows;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;

import static toolkit.Retrofits.RETROFIT;

@Resource.Classpath("delivery/lrm.properties")
public class LrmClient {

    private final LrmApi lrmApi;
    @Property("lrm.host")
    private String host;

    public LrmClient() {
        PropertyLoader.newInstance().populate(this);
        lrmApi = RETROFIT.getRetrofit(host).create(LrmApi.class);
    }

    @SneakyThrows
    public CreateReturnResponse createReturn(
        Order order,
        ReturnSource source,
        String offerId,
        Long scLogisticPointId
    ) {
        String boxExternalId = "box-" + order.getId();
        Delivery delivery = order.getDelivery();
        Long logisticPointId;
        if (scLogisticPointId != null) {
            logisticPointId = scLogisticPointId;
        } else if (delivery.getOutletId() != null) {
            logisticPointId = delivery.getOutletId();
        } else {
            JsonNode route = delivery.getParcels()
                .stream()
                .map(Parcel::getRoute)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Нет рута в чекаутере по заказу " + order.getId()));

            logisticPointId = Optional.ofNullable(route.findValue("logistic_point_id"))
                .map(JsonNode::longValue)
                .orElse(null);
        }

        Assertions.assertNotNull(
            logisticPointId,
            "Нет logistic_point_id в чекаутере"
        );

        OrderItem orderItem = order.getItems()
            .stream()
            .filter(item -> item.getOfferId().equals(offerId))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Нет айтемов в заказе " + order.getId()));
        ArrayNode instances = orderItem.getInstances();

        Assertions.assertNotNull(instances, "Нет instances (uit-ов) у товара в заказе " + order.getId());

        JsonNode cis = instances.findValue("CIS");
        Map<String, String> itemInstances = new HashMap<>();
        itemInstances.put("UIT", instances.findValue("UIT").textValue());
        if (cis != null) {
            itemInstances.put("CIS", cis.textValue());
        }
        CreateReturnRequest lrmRequest = new CreateReturnRequest()
            .orderExternalId(order.getId().toString())
            .boxes(List.of(new ReturnBoxRequest().externalId(boxExternalId)))
            .courier(
                new ReturnCourier()
                    .carNumber("A123BC178")
                    .uid("testuid")
                    .name("Тестовый Курьер")
            )
            .logisticPointFromId(logisticPointId)
            .externalId(order.getId().toString())
            .source(source)
            .items(List.of(
                new ReturnItem()
                    .boxExternalId(boxExternalId)
                    .instances(itemInstances)
                    .vendorCode(orderItem.getShopSku())
                    .supplierId(orderItem.getSupplierId())
            ));
        Response<CreateReturnResponse> bodyResponse = lrmApi.returns(
                TVM.INSTANCE.getServiceTicket(TVM.LRM),
                lrmRequest
            )
            .execute();
        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "Запрос создания возврата в LRM неуспешен по заказу " + order.getId()
        );
        Assertions.assertNotNull(
            bodyResponse.body(),
            "Пустое тело ответа создания возврата в LRM по заказу " + order.getId()
        );
        return bodyResponse.body();
    }

    @SneakyThrows
    public SearchReturn searchReturn(String boxExternalId) {
        SearchReturnsRequest request = new SearchReturnsRequest();
        request.setBoxExternalIds(List.of(boxExternalId));
        Response<SearchReturnsResponse> bodyResponse = lrmApi.searchReturns(
                TVM.INSTANCE.getServiceTicket(TVM.LRM),
                request
            )
            .execute();
        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "Запрос поиска возвратов в LRM неуспешен по коробке " + boxExternalId
        );
        Assertions.assertNotNull(
            bodyResponse.body(),
            "Пустое тело ответа поиска возвратов в LRM по коробке " + boxExternalId
        );
        Assertions.assertTrue(
            bodyResponse.body().getReturns() != null && bodyResponse.body().getReturns().size() > 0,
            "Не найдено возвратов в LRM по коробке " + boxExternalId
        );
        return bodyResponse.body().getReturns().get(0);
    }

    @SneakyThrows
    public void commitReturn(Long returnId) {
        Response<ResponseBody> bodyResponse = lrmApi.commitReturns(
                TVM.INSTANCE.getServiceTicket(TVM.LRM),
                returnId
            )
            .execute();
        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "Ошибка при коммите возврата " + returnId
        );
        Assertions.assertNotNull(
            bodyResponse.body(),
            "Пустое тело ответа при коммите возврата " + returnId
        );
    }
}
