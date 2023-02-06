package ru.yandex.market.tsum.clients.nanny;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;

import javax.annotation.ParametersAreNonnullByDefault;

import nanny.pod_sets_api.PodSetsApi;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.request.netty.HttpClientConfig;
import ru.yandex.market.request.netty.NettyHttpClientContext;

@ParametersAreNonnullByDefault
@Ignore("integration test")
public class NannyYpApiClientTest {
    private static final String NANNY_TOKEN;
    private static final NannyYpApiClient NANNY_YP_API_CLIENT;

    static {
        // https://nanny.yandex-team.ru/ui/#/oauth/
        NANNY_TOKEN = getToken(".nanny/token");
        NettyHttpClientContext nettyContext = new NettyHttpClientContext(new HttpClientConfig());
        NANNY_YP_API_CLIENT = new NannyYpApiClient("https://yp-lite-ui.nanny.yandex-team.ru/api/yplite/pod-sets/",
            NANNY_TOKEN, nettyContext);
    }

    // TODO после MARKETINFRA-6390 объединить с YandexDeployClientTest.getToken
    private static String getToken(String path) {
        try {
            // TODO поменять на Files.readString, когда не надо будет поддерживать Java 8
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
        PodSetsApi.GetPodSetResponse podSet =
            NANNY_YP_API_CLIENT.getPodSet("mt_front-blue-unified--bluemarket-11452pre_d50e2737_sas", "SAS");
        System.out.println(podSet);
    }
}
