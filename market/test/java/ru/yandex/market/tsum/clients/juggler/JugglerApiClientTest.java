package ru.yandex.market.tsum.clients.juggler;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Andrey Trubachev <a href="mailto:d3rp@yandex-team.ru"></a>
 * @date 04/07/2017
 */
@Ignore
public class JugglerApiClientTest {

    private final JugglerApiClient jugglerClient = new JugglerApiClient(
        "http://juggler-api.search.yandex.net:8998/",
        "https://juggler.yandex-team.ru/",
        // https://oauth.yandex-team.ru/authorize?response_type=token&client_id=cd178dcdc31a4ed79f42467f2d89b0d0
        "TOKEN",
        null
    );

    @Test
    public void getEvents() throws Exception {
        EventRequest request = EventRequest.newBuilder()
            .addHost("market_front_desktop")
            .addService("ping")
            .build();

        List<JugglerEvent> events = jugglerClient.getEvents(
            request
        );

        JugglerEvent expectedEvent = new JugglerEvent(
            "market_front_desktop",
            "ping",
            new JugglerEvent.Status(EventStatus.OK, new Date()),
            new Date(),
            null
        );
        assertEquals(expectedEvent, events.get(0));
    }

    /*
        > curl -s -H 'Content-Type: application/json' -X POST --data \
        '{"filters": [{"rule_id": "5c87cddb66a34600769c928f"}]}' \
        'http://juggler-api.search.yandex.net/api/notify_rules/get_notify_rules?do=1' | jq .
        {
          "rules": [
            {
              "hints": [],
              "owners": [
                "@svc_marketito_administration"
              ],
              "description": "https://st.yandex-team.ru/CSADMIN-27628",
              "match_raw_events": false,
              "template_name": "phone_escalation",
              "check_kwargs": {},
              "template_kwargs": {
                "logins": [
                  "d3rp"
                ],
                "delay": 15,
                "time_start": "11:00",
                "time_end": "11:00"
              },
              "creation_time": 1552403931.628577,
              "rule_id": "5c87cddb66a34600769c928f",
              "selector": "namespace=market.sre & tag=market_sre_disaster"
            }
          ],
          "total": 1
        }
        >
     */
    @Test
    public void updateNotificationRule() throws Exception {
        ArrayList<String> logins = new ArrayList<>();
        logins.add("d3rp");

        jugglerClient.updateNotificationRule("5c8d1c18dcc66c006f37faec", logins);
    }

    @Test
    public void getLoginsFromNotificationRule() throws Exception {
        List<String> logins = jugglerClient.getLoginsFromNotificationRule("5c8d1c18dcc66c006f37faec");
        assertEquals("d3rp", logins.get(0));
    }

}
