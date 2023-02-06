package client;

import java.util.List;
import java.util.Map;

import api.BlueFApi;
import dto.responses.bluefapi.ResolveLink;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import static toolkit.Retrofits.RETROFIT;

@Slf4j
@Resource.Classpath("delivery/bluefapi.properties")
public class BlueFApiClient {

    private final BlueFApi blueFApi;

    @Property("bluefapi.host")
    private String host;

    public BlueFApiClient() {
        PropertyLoader.newInstance().populate(this);
        blueFApi = RETROFIT.getRetrofit(host).create(BlueFApi.class);
    }

    @SneakyThrows
    public ResolveLink resolveOnDemandLink(String externalId) {
        log.debug("Resolving link");
        Response<ResolveLink> execute = blueFApi.resolveOnDemandLink("resolveOnDemandLink", Map.of(
            "params", List.of(
                Map.of(
                    "isGoInstalled", false,
                    "id", externalId
                )
            )
        )).execute();
        Assertions.assertTrue(
            execute.isSuccessful(),
            "Неуспешный запрос резолва ссылки для вызова курьера с externalId = " + externalId
        );
        Assertions.assertNotNull(
            execute.body(),
            "Не удалось резолвить ссылку для вызова курьера externalId=" + externalId
        );
        return execute.body();
    }
}
