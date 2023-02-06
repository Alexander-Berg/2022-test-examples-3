package ru.yandex.market.pricelabs.api.api;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.util.MimeTypeUtils;

import ru.yandex.market.pricelabs.MockMvcProxy;
import ru.yandex.market.pricelabs.api.AbstractApiSpringConfiguration;
import ru.yandex.market.pricelabs.generated.server.pub.model.ScheduleResponse;
import ru.yandex.market.pricelabs.misc.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PublicScheduleApiTest extends AbstractApiSpringConfiguration {

    @Autowired
    private PublicScheduleApi publicApiBean;
    private PublicScheduleApiInterfaces publicApi;

    @Autowired
    @Qualifier("mockWebServerTms")
    private MockWebServer mockWebServerTms;

    @BeforeEach
    void init() {
        publicApi = MockMvcProxy.buildProxy(PublicScheduleApiInterfaces.class, publicApiBean);
    }

    @Test
    void testPost() throws InterruptedException {
        mockWebServerTms.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MimeTypeUtils.APPLICATION_JSON_VALUE + ";charset=utf-8")
                .setBody(Utils.toJsonString(new ScheduleResponse().jobId(4L))));

        var ret = publicApi.scheduleOffersPost(1, List.of(), false, null, null);
        assertEquals(HttpStatus.OK, ret.getStatusCode());

        var request = mockWebServerTms.takeRequest(1, TimeUnit.MILLISECONDS);
        assertNotNull(request);

        var url = request.getRequestUrl();
        assertEquals("1", url.queryParameter("shopId"));
        assertNull(url.queryParameter("feedIdList"));
        assertEquals("false", url.queryParameter("syncContent"));

        assertEquals(4L, Objects.requireNonNull(ret.getBody()).getJobId());
    }

}
