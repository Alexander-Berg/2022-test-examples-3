package ru.yandex.market.api.integration;

import org.junit.Test;
import ru.yandex.market.api.abtest.ClientActiveExperiment;
import ru.yandex.market.api.common.client.KnownMobileClientVersionInfo;
import ru.yandex.market.api.common.client.SemanticVersion;
import ru.yandex.market.api.internal.common.DeviceType;
import ru.yandex.market.api.internal.common.Platform;
import ru.yandex.market.api.model.ModelService;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.version.Version;
import ru.yandex.market.api.util.concurrent.ApiFutures;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReportClientTest extends BaseTest {

    private static final long MODEL_ID = 13485518;

    @Inject
    private ReportTestClient reportTestClient;

    @Inject
    private ModelService modelService;

    @Test
    public void noExperiments_waitNoTestBucketParameter() {
        List<ClientActiveExperiment> abExperiments1 = Collections.emptyList();
        setupMobileClient(abExperiments1);

        reportTestClient.search("modelinfo",
            x -> x.withoutParam("test-buckets"),
            "modelinfo_13485518.json");

        Futures.waitAndGet(modelService.getModel(MODEL_ID, Collections.emptyList(), genericParams));
    }

    @Test
    public void testIdIsPassed() {

        List<ClientActiveExperiment> abExperiments = new ArrayList<>();
        abExperiments.add(new ClientActiveExperiment("12", "34", "alias1"));
        abExperiments.add(new ClientActiveExperiment("56", "78", "alias2"));

        setupMobileClient(abExperiments);

        reportTestClient.search("modelinfo",
            x -> x.param("test-buckets", "12,0,34;56,0,78"),
            "modelinfo_13485518.json");

        Futures.waitAndGet(modelService.getModel(MODEL_ID, Collections.emptyList(), genericParams));
    }

    private void setupMobileClient(List<ClientActiveExperiment> abExperiments) {
        ContextHolder.update(context -> {
            context.setVersion(Version.V2_0_1);
            Client client = new Client();
            client.setType(Client.Type.MOBILE);
            context.setClient(client);

            context.setClientVersionInfo(new KnownMobileClientVersionInfo(
                Platform.ANDROID,
                DeviceType.SMARTPHONE,
                new SemanticVersion(3, 8, 1),
                "3.81",
                abExperiments));
        });
    }
}
