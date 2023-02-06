package client;

import api.MdbApi;
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
@Resource.Classpath("delivery/mdb.properties")
public class MdbClient {

    private final MdbApi mdbApi;

    @Property("mdb.host")
    private String host;

    public MdbClient() {
        PropertyLoader.newInstance().populate(this);
        mdbApi = RETROFIT.getRetrofit(host).create(MdbApi.class);
    }

    @SneakyThrows
    public ResponseBody processIntakeShipments() {
        log.debug("Processing intake shipments...");

        //Надо выполнить на каждом из хостов. Так как работает только балансер, пробуем повторить N раз.
        Response<ResponseBody> bodyResponse = null;
        for (int i = 0; i < 10; i++) {
            bodyResponse = mdbApi.processIntakeShipments().execute();
            Assertions.assertTrue(bodyResponse.isSuccessful(), "Не удалось");
            Assertions.assertNotNull(bodyResponse.body(), "Пустой ответ");
        }
        return bodyResponse.body();
    }

    @SneakyThrows
    public ResponseBody shipmentOrder(Long orderId) {
        log.debug("Getting shipmentOrder data from MDB...");
        Response<ResponseBody> bodyResponse = mdbApi.shipmentOrder(orderId).execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Не удалось");
        Assertions.assertNotNull(bodyResponse.body(), "Пустой ответ");
        return bodyResponse.body();
    }

    @SneakyThrows
    public ResponseBody registerSearch(Long registerId) {
        log.debug("Getting register data from MDB...");
        Response<ResponseBody> bodyResponse = mdbApi.registerSearch(registerId).execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Не удалось");
        Assertions.assertNotNull(bodyResponse.body(), "Пустой ответ");
        return bodyResponse.body();
    }

    @SneakyThrows
    public ResponseBody registerFromSC(Long from, Long to, Integer hour) {
        log.debug("Getting register data from MDB...");
        Response<ResponseBody> bodyResponse = mdbApi.registerFromSC(from, to, hour).execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Не удалось");
        Assertions.assertNotNull(bodyResponse.body(), "Пустой ответ");
        return bodyResponse.body();
    }

}
