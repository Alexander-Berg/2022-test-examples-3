package ru.yandex.market.crm.campaign.test.utils;

import java.util.UUID;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.mcrm.http.HttpEnvironment;
import ru.yandex.market.mcrm.http.ResponseBuilder;
import ru.yandex.market.mcrm.utils.test.StatefulHelper;
import ru.yandex.market.tsum.event.Event;
import ru.yandex.market.tsum.event.EventId;

import static ru.yandex.market.mcrm.http.HttpRequest.get;
import static ru.yandex.market.mcrm.http.HttpRequest.post;

/**
 * @author apershukov
 */
@Component
public class TimelineApiHelper implements StatefulHelper {

    private static final String BASE_URL = "https://tsum-api.market.yandex.net:4203/events";

    private final HttpEnvironment httpEnvironment;
    private final JsonSerializer jsonSerializer;

    public TimelineApiHelper(HttpEnvironment httpEnvironment, JsonSerializer jsonSerializer) {
        this.httpEnvironment = httpEnvironment;
        this.jsonSerializer = jsonSerializer;
    }

    @Override
    public void setUp() {
        httpEnvironment.when(post(BASE_URL + "/addEvent"))
                .then(request -> {
                    EventId eventId = EventId.newBuilder()
                            .setId(UUID.randomUUID().toString())
                            .build();

                    return ResponseBuilder.newBuilder()
                            .body(jsonSerializer.writeObjectAsBytes(eventId))
                            .build();
                });

        httpEnvironment.when(get(BASE_URL + "/getEvent"))
                .then(request -> {
                    Event event = Event.getDefaultInstance();
                    return ResponseBuilder.newBuilder()
                            .body(jsonSerializer.writeObjectAsBytes(event))
                            .build();
                });

        httpEnvironment.when(post(BASE_URL + "/upsertEvent"))
                .then(request -> ResponseBuilder.newBuilder().build());
    }

    @Override
    public void tearDown() {
    }
}
