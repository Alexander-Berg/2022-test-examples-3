package client;

import api.TaxiApi;
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
@Resource.Classpath("delivery/taxi.properties")
public class TaxiClient {

    private final TaxiApi taxiApi;
    @Property("host")
    private String host;

    public TaxiClient() {
        PropertyLoader.newInstance().populate(this);
        taxiApi = RETROFIT.getRetrofit(host).create(TaxiApi.class);
    }

    @SneakyThrows
    public void transferActivate(String transferId, String requestId) {
        log.debug("Activate transfer");
        Response<ResponseBody> execute = taxiApi.transferActivate(transferId, requestId).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось вызвать курьера такси");
    }
}
