package ru.yandex.market.global.checkout.domain.queue.task.notification;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import retrofit2.Response;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.global.checkout.BaseLocalTest;
import ru.yandex.mj.generated.client.taxi_communications_scenario.api.TaxiCommunicationsScenarioApiClient;
import ru.yandex.mj.generated.client.taxi_communications_scenario.model.ChannelType;
import ru.yandex.mj.generated.client.taxi_communications_scenario.model.Channels;
import ru.yandex.mj.generated.client.taxi_communications_scenario.model.InlineObject;
import ru.yandex.mj.generated.client.taxi_communications_scenario.model.InlineResponse200;
import ru.yandex.mj.generated.client.taxi_communications_scenario.model.Recipient;
import ru.yandex.mj.generated.client.taxi_communications_scenario.model.UcommunicationsPushSettings;

@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CommunicationScenarioLocalTest extends BaseLocalTest {

    private final TaxiCommunicationsScenarioApiClient scenarioApiClient;

    @Test
    public void localTest() throws IOException {
        Channels en = new Channels().locale("en");
        en.setPush(new UcommunicationsPushSettings().intent("global_market_order").channelType(ChannelType.PUSH_GO));
        String idempotency = "global-market-test4";
        HashMap<String, Object> stringStringHashMap = new HashMap<>();
        stringStringHashMap.put("deeplink", "yandextaxi://external?service=market");
        stringStringHashMap.put("title",
                Map.of("keyset", "global_market_notifications",
                        "key", "reward_promocode_received_title",
                        "params", Map.of()));
        stringStringHashMap.put("text",
                Map.of("keyset", "global_market_notifications",
                        "key", "reward_promocode_received_text",
                        "params", Map.of("sum", 228)));


        InlineObject channels = new InlineObject()
                .scenario("global_market_notification")
                .recipient(new Recipient().yandexUid("4092490744"))
                .parameters(stringStringHashMap)
                .channels(en);
        ExecuteCall<InlineResponse200, RetryStrategy> inlineResponse200Call =
                scenarioApiClient.v1StartPost(channels, idempotency);
        Response<InlineResponse200> execute = inlineResponse200Call.scheduleResponse().join();

        System.out.println(execute.code());
        System.out.println(execute.message());
        System.out.println(execute.body().getLaunchId());
    }

}
