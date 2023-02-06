package client;

import api.Ff4ShopsApi;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.ff4shops.api.model.auth.ClientRole;
import ru.yandex.market.ff4shops.api.model.order.OrderRemovalPermissionsDto;

import static toolkit.Retrofits.RETROFIT;

@Resource.Classpath("delivery/ff4shops.properties")
public class Ff4ShopsApiClient {

    private final Ff4ShopsApi ff4ShopsApi;

    @Property("ff4shopsapi.host")
    private String host;

    public Ff4ShopsApiClient() {
        PropertyLoader.newInstance().populate(this);
        ff4ShopsApi = RETROFIT.getRetrofit(host).create(Ff4ShopsApi.class);

    }

    @SneakyThrows
    public OrderRemovalPermissionsDto getRemovalPermissions(
        long orderId,
        long clientId,
        long shopId,
        ClientRole clientRole
    ) {
        Response<OrderRemovalPermissionsDto> execute = ff4ShopsApi.getRemovalPermissions(
            orderId,
            clientId,
            shopId,
            clientRole
        )
            .execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось получить разметку по id заказа " + orderId);
        Assertions.assertNotNull(execute.body(), "Пустой ответ при получении разметки по id заказа " + orderId);
        return execute.body();
    }
}
