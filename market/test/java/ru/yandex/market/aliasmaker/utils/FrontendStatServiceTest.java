package ru.yandex.market.aliasmaker.utils;

import java.time.Instant;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.http.FrontendStat;

/**
 * @author yuramalinov
 * @created 26.12.2019
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class FrontendStatServiceTest {
    @Test
    public void testFrontendStat() {
        FrontendStatService service = new FrontendStatService();
        String message = service.buildMessage(
                FrontendStat.FrontendStatMessage.newBuilder()
                        .setComponent("component_value")
                        .setPage("page_value")
                        .setUrl("url_value")
                        .setUserAgent("user_agent_value")
                        .setUid("uid_value")
                        .setContext("0001178185--5f098bdf54c43b25c30b58c7")
                        .build(),
                FrontendStat.FrontendStatMessage.Event.newBuilder()
                        .setDurationMs(1234)
                        .setName("name_value")
                        .setLevel(FrontendStat.FrontendStatMessage.Level.ERROR)
                        .setMessage("message_value")
                        .addRequests(FrontendStat.FrontendStatMessage.Request.newBuilder()
                                .setReqId("req1")
                                .setDurationMs(1001)
                                .setUrl("/url1")
                                .build())
                        .addRequests(FrontendStat.FrontendStatMessage.Request.newBuilder()
                                .setReqId("req2")
                                .setDurationMs(1002)
                                .setUrl("/url2")
                                .build())
                        .build(),
                Instant.parse("2020-04-17T08:28:09.713Z"));

        Assertions.assertThat(message)
                .isEqualTo(String.join("\t",
                        "tskv",
                        "date=2020-04-17T08:28:09.713Z",
                        "component=component_value",
                        "page=page_value",
                        "url=url_value",
                        "user_agent=user_agent_value",
                        "uid=uid_value",
                        "context=0001178185--5f098bdf54c43b25c30b58c7",
                        "level=ERROR",
                        "name=name_value",
                        "message=message_value",
                        "duration_ms=1234",
                        "requests_url=/url1,/url2",
                        "requests_req_id=req1,req2",
                        "requests_duration_ms=1001,1002"));
    }
}
