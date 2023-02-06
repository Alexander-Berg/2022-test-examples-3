package ru.yandex.market.mcrp_request.clients;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import feign.Feign;
import feign.Logger;
import feign.httpclient.ApacheHttpClient;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mcrp_request.clients.dispenser.DispenserFeignClient;
import ru.yandex.market.mcrp_request.clients.dispenser.ErrorDecoder;
import ru.yandex.market.mcrp_request.clients.dispenser.QuotaRequest;
import ru.yandex.market.mcrp_request.clients.dispenser.ResourceLB;
import ru.yandex.market.mcrp_request.clients.dispenser.ResourceMDB;
import ru.yandex.market.mcrp_request.clients.dispenser.ResourceMDS;
import ru.yandex.market.mcrp_request.clients.dispenser.ResourceRTC;
import ru.yandex.market.mcrp_request.clients.dispenser.ResourceSaaS;
import ru.yandex.market.mcrp_request.clients.dispenser.ResourceYT;

import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.market.mcrp_request.clients.dispenser.ResourcePreorderReasonType.GROWTH;

public class DispenserFeignClientTest {

    DispenserFeignClient dispenserFeignClient;

    String dispenserToken = "Если вместо мока будет реальный http клиент тут нужно вставить токен";

    @Test
    public void testClient() throws IOException {

        ProtocolVersion http11 = new ProtocolVersion("HTTP", 1, 1);
        BasicHttpResponse response = new BasicHttpResponse(http11, 200, null);
        BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
        basicHttpEntity.setContent(new ByteArrayInputStream("{\"result\":[]}".getBytes()));
        response.setEntity(basicHttpEntity);

        HttpClient httpClient = Mockito.mock(HttpClient.class);
        Mockito.when(httpClient.execute(any())).thenReturn(response);
        Mockito.when(httpClient.execute(any(HttpHost.class), any())).thenReturn(response);
        Mockito.when(httpClient.execute(any(HttpHost.class), any(HttpRequest.class), any(HttpContext.class)))
                .thenReturn(response);
        Mockito.when(httpClient.execute(any(HttpUriRequest.class), any(HttpContext.class))).thenReturn(response);
        Mockito.when(httpClient.execute(any(HttpUriRequest.class), any(ResponseHandler.class))).thenReturn(response);
        Mockito.when(httpClient.execute(any(HttpUriRequest.class), any(ResponseHandler.class), any(HttpContext.class)))
                .thenReturn(response);
        Mockito.when(httpClient.execute(any(), any(), any(), any())).thenReturn(response);

        dispenserFeignClient = Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .errorDecoder(new ErrorDecoder())
                .logger(new Slf4jLogger(DispenserFeignClient.class))
                .logLevel(Logger.Level.FULL)
                .client(new ApacheHttpClient(httpClient
                        //HttpClientBuilder.create().build()
                ))
                .target(DispenserFeignClient.class,
                        "https://dispenser.test.yandex-team.ru"
                        //"http://dispenser.yandex-team.ru"
                );

        QuotaRequest request = new QuotaRequest("market",
                "",
                "RESOURCE_PREORDER",
                "Название заявки тест",
                "Комментарий тест",
                GROWTH,
                new QuotaRequest.Change(
                        ResourceYT.CPU,
                        43,
                        new String[]{"arnold"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceYT.HDD,
                        43,
                        new String[]{"arnold"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceYT.SSD,
                        43,
                        new String[]{"arnold"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceYT.BURST_GUARANTEE_CPU,
                        43,
                        new String[]{"arnold"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceYT.CLUSTER_NETWORK_TRAFFIC,
                        43,
                        new String[]{"markov"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceYT.CPU_CLICKHOUSE,
                        43,
                        new String[]{"arnold"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceYT.CPU_FLOW,
                        43,
                        new String[]{"arnold"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceYT.GPU,
                        43,
                        new String[]{"arnold"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceYT.RPC_PROXY,
                        43,
                        new String[]{"arnold"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceYT.TABLET_CELL_BUNDLE,
                        43,
                        new String[]{"arnold"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceYT.TABLET_STATIC_MEMORY,
                        43,
                        new String[]{"arnold"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceMDB.CPU,
                        43,
                        new String[]{"VLA", "dbaas_pgsql"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceMDB.HDD,
                        43,
                        new String[]{"VLA", "dbaas_clickhouse"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceMDB.SSD,
                        43,
                        new String[]{"VLA", "dbaas_pgsql"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceMDB.RAM,
                        43,
                        new String[]{"VLA", "dbaas_pgsql"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceMDS.MDS,
                        43,
                        new String[]{},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceMDS.AVATARS,
                        43,
                        new String[]{},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceMDS.S3_API,
                        43,
                        new String[]{},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceRTC.CPU,
                        43,
                        new String[]{"VLA"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceRTC.HDD,
                        43,
                        new String[]{"VLA"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceRTC.SSD,
                        43,
                        new String[]{"VLA"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceRTC.RAM,
                        43,
                        new String[]{"VLA"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceLB.DATA_FLOW,
                        43,
                        new String[]{"logbroker_MAN"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceSaaS.CPU,
                        43,
                        new String[]{"VLA"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceSaaS.HDD,
                        43,
                        new String[]{"VLA"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceSaaS.SSD,
                        43,
                        new String[]{"VLA"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceSaaS.RAM,
                        43,
                        new String[]{"VLA"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceSaaS.IO_SSD,
                        43,
                        new String[]{"VLA"},
                        10
                ),
                new QuotaRequest.Change(
                        ResourceSaaS.IO_HDD,
                        43,
                        new String[]{"VLA"},
                        10
                )
        );

        dispenserFeignClient.getOrders(dispenserToken);
        dispenserFeignClient.createQuotaRequests(request, dispenserToken);
    }
}
