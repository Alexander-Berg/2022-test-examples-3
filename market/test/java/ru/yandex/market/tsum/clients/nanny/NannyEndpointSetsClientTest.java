package ru.yandex.market.tsum.clients.nanny;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;

import javax.annotation.ParametersAreNonnullByDefault;

import nanny.endpoint_sets_api.EndpointSetsApi;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.request.netty.HttpClientConfig;
import ru.yandex.market.request.netty.NettyHttpClientContext;

@ParametersAreNonnullByDefault
@Ignore("integration test")
public class NannyEndpointSetsClientTest {
    private static final String NANNY_TOKEN;
    private static final NannyEndpointSetsClient NANNY_ENDPOINT_SETS_CLIENT;

    static {
        // https://nanny.yandex-team.ru/ui/#/oauth/
        NANNY_TOKEN = getToken(".nanny/token");
        NettyHttpClientContext nettyContext = new NettyHttpClientContext(new HttpClientConfig());
        NANNY_ENDPOINT_SETS_CLIENT = new NannyEndpointSetsClient(
            "https://yp-lite-ui.nanny.yandex-team.ru/api/yplite/endpoint-sets/",
            NANNY_TOKEN, nettyContext);
    }

    // TODO см NannyYpApiClientTest
    private static String getToken(String path) {
        try {
            // TODO см NannyYpApiClientTest
            //noinspection ReadWriteStringCanBeUsed
            return new String(
                Files.readAllBytes(FileSystems.getDefault().getPath(System.getenv("HOME"), path)),
                StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getPodSet() {
        EndpointSetsApi.ListEndpointSetsResponse endpointSets =
            NANNY_ENDPOINT_SETS_CLIENT.listEndpointSets(
                "testing_market_template_service_for_java_iva",
                "SAS"
            );
        System.out.println(endpointSets);
    }
}
