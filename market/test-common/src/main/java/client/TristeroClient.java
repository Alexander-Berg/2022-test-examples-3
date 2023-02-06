package client;

import java.util.Map;

import api.TristeroApi;
import dto.responses.lavka.TristeroOrderResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import static toolkit.Retrofits.RETROFIT;

@Slf4j
@Resource.Classpath("delivery/lavka.properties")
public class TristeroClient {

    @Property("tristero.host")
    private static String host;
    private final TristeroApi tristeroApi;

    public TristeroClient() {
        PropertyLoader.newInstance().populate(this);
        tristeroApi = RETROFIT.getRetrofit(host).create(TristeroApi.class);
    }

    @SneakyThrows
    public TristeroOrderResponse getLavkaOrderInfo(Long orderId) {
        log.debug("Get order info from lavka...");
        Response<TristeroOrderResponse> bodyResponse = tristeroApi
            .getLavkaOrderInfo(TVM.INSTANCE.getServiceTicket(TVM.TRISTERO), orderId)
            .execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Не удалось получить информацию о заказе в лавке");
        Assertions.assertNotNull(bodyResponse.body(), "Пустой ответ: информация о заказе в лавке");
        return bodyResponse.body();
    }

    @SneakyThrows
    public TristeroOrderResponse setStatus(String parcelId, String state) {
        log.debug("Set parcel state {}", state);
        Response<TristeroOrderResponse> bodyResponse = tristeroApi
            .setStatus(TVM.INSTANCE.getServiceTicket(TVM.TRISTERO), parcelId, Map.of("state", state))
            .execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Не удалось установить статус посылки");
        Assertions.assertNotNull(bodyResponse.body(), "Пустой ответ: изменение стауса посылки");
        return bodyResponse.body();
    }
}
