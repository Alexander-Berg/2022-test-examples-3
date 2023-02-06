package client;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import api.L4SApi;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.logistics4shops.client.model.CreateExcludeOrderFromShipmentRequest;
import ru.yandex.market.logistics4shops.client.model.ExcludeOrderRequestListDto;
import ru.yandex.market.logistics4shops.client.model.Outbound;
import ru.yandex.market.logistics4shops.client.model.OutboundsListDto;
import ru.yandex.market.logistics4shops.client.model.OutboundsSearchRequest;

import static toolkit.Retrofits.RETROFIT;

@Resource.Classpath("delivery/l4s.properties")
public class L4SClient {
    private final L4SApi l4SApi;
    @Property("l4s.host")
    private String host;

    public L4SClient() {
        PropertyLoader.newInstance().populate(this);
        l4SApi = RETROFIT.getRetrofit(host).create(L4SApi.class);
    }

    @SneakyThrows
    public void excludeOrderFromShipment(Long orderId, Long shipmentId) {
        Response<ExcludeOrderRequestListDto> response = l4SApi.excludeOrdersFromShipment(
                TVM.INSTANCE.getServiceTicket(TVM.L4S),
                shipmentId,
                new CreateExcludeOrderFromShipmentRequest()
                    .orderIds(List.of(orderId))
            )
            .execute();
        Assertions.assertTrue(
            response.isSuccessful(),
            "Запрос создания заявки на исключение заказа из отгрузки в L4S неуспешен по заказу " + orderId
        );
        Assertions.assertNotNull(
            response.body(),
            "Пустое тело ответа создания заявки на исключение заказа из отгрузки в L4S по заказу " + orderId
        );
    }

    @Nonnull
    @SneakyThrows
    public List<Outbound> searchOutbounds(List<String> outboundYandexIds) {
        Response<OutboundsListDto> response = l4SApi.searchOutbounds(
                TVM.INSTANCE.getServiceTicket(TVM.L4S),
                new OutboundsSearchRequest().yandexIds(outboundYandexIds)
            )
            .execute();
        Assertions.assertTrue(
            response.isSuccessful(),
            "Запрос поиска отгрузок в L4S неуспешен: " + String.join(",", outboundYandexIds)
        );
        Assertions.assertNotNull(
            response.body(),
            "Пустое тело ответа поиска отгрузок в L4S: " + String.join(",", outboundYandexIds)
        );

        return Objects.requireNonNull(response.body().getOutbounds());
    }
}
