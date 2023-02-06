package ru.yandex.market.crm.campaign.services.external.sandbox;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.campaign.test.AbstractServiceMediumWithoutYtTest;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.mcrm.http.HttpEnvironment;
import ru.yandex.market.mcrm.http.HttpRequest;
import ru.yandex.market.mcrm.http.ResponseBuilder;

/**
 * @author zloddey
 */
public class SandboxStepClientTest extends AbstractServiceMediumWithoutYtTest {
    @Inject
    private HttpEnvironment httpEnvironment;
    @Inject
    private JsonSerializer serializer;
    @Inject
    private JsonDeserializer deserializer;
    @Inject
    private SandboxStepClient client;

    @Test
    public void extractIdsFromResponse() {
        List<String> eventIds = Collections.singletonList("678943");
        var response = new HashMap<String, List<String>>() {{
            put("ids", eventIds);
        }};
        httpEnvironment.when(HttpRequest.post("https://step-sandbox1.n.yandex.ru/api/v1/events"))
                .then(request -> ResponseBuilder.newBuilder()
                        .body(serializer.writeObjectAsString(response)).build());
        List<String> actualIds = client.sendStepMessages(new StepServiceEvent("", new HashMap<>()));
        Assertions.assertEquals(eventIds, actualIds);
    }

    @Test
    public void passParameters() {
        Map<String, String> parameters = new HashMap<>() {{
            put("cluster", "arnold");
            put("timestamp", "2020-01-25T22:50:00");
        }};
        AtomicReference<StepServiceRequest> requestReference = new AtomicReference<>();
        httpEnvironment.when(HttpRequest.post("https://step-sandbox1.n.yandex.ru/api/v1/events"))
                .then(request -> {
                    requestReference.set(deserializer.readObject(StepServiceRequest.class, request.getBody()));
                    return ResponseBuilder.newBuilder().body("{\"ids\":[\"31337\"]}").build();
                });
        client.sendStepMessages(new StepServiceEvent("cluster_table_publish", parameters));

        StepServiceRequest actualRequest = requestReference.get();
        Assertions.assertEquals(1, actualRequest.getEvents().size());

        StepServiceEvent receivedEvent = actualRequest.getEvents().get(0);
        Assertions.assertEquals("cluster_table_publish", receivedEvent.getName());
        Assertions.assertEquals(parameters, receivedEvent.getParameters());
    }
}
