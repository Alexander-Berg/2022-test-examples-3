package client;

import java.util.List;
import java.util.UUID;

import api.LavkaApi;
import dto.requests.lavka.LavkaCreateOrderRequest;
import dto.responses.lavka.TristeroOrderResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import static toolkit.Retrofits.RETROFIT;

@Slf4j
@Resource.Classpath("delivery/lavka.properties")
public class LavkaClient {

    @Property("lavka.host")
    private static String host;
    private final LavkaApi lavkaApi;

    public LavkaClient() {
        PropertyLoader.newInstance().populate(this);
        lavkaApi = RETROFIT.getRetrofit(host).create(LavkaApi.class);
    }

    @SneakyThrows
    public ResponseBody makeOrder(
        Long uid,
        String personalPhoneId,
        TristeroOrderResponse orderInfo,
        List<Double> location
    ) {
        log.debug("Make order from lavka...");
        Response<ResponseBody> bodyResponse = lavkaApi.makeOrder(
            TVM.INSTANCE.getServiceTicket(TVM.LAVKA_ADMIN),
            UUID.randomUUID().toString(),
            new LavkaCreateOrderRequest(
                uid.toString(),
                personalPhoneId,
                new LavkaCreateOrderRequest.Position(
                    orderInfo.getCustomerAddress(),
                    location
                ),
                "RU",
                List.of(
                    new LavkaCreateOrderRequest.LavkaItem(
                        orderInfo.getItems().get(0).getWmsId() + ":st-pa",
                        "1")
                )
            )
        ).execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Не удалось сформировать заказе в лавке");
        Assertions.assertNotNull(bodyResponse.body(), "Пустой ответ: формирования заказе в лавке");
        return bodyResponse.body();
    }
}
