package client;

import api.IdxApi;
import dto.responses.idxapi.OtraceResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import static toolkit.Retrofits.RETROFIT;

@Slf4j
@Resource.Classpath({"delivery/idx.properties"})
public class IdxClient {
    private final IdxApi idxApi;

    @Property("idxapi.host")
    private String idxapiHost;

    public IdxClient() {
        PropertyLoader.newInstance().populate(this);
        idxApi = RETROFIT.getRetrofit(idxapiHost).create(IdxApi.class);
    }

    @SneakyThrows
    public OtraceResponse otrace(String waremd5) {
        log.debug("Calling idxapi to get otrace...");
        Response<OtraceResponse> execute = idxApi.otrace(waremd5).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось выполнить запрос к otrace");
        Assertions.assertNotNull(execute.body(), "Не удалось получить объект OtraceResponse");
        return execute.body();
    }

}
