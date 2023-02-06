package ru.yandex.market.hc.utils;

import NAppHostHttp.Http;
import org.junit.Test;

import ru.yandex.market.hc.util.ApphostUtils;

import static org.junit.Assert.assertEquals;

/**
 * Created by aproskriakov on 1/12/22
 */
public class ApphostUtilsTest {

    @Test
    public void testGetHostAndEndpoint() {
        String expectedResult = "mstat-antifraud-orders.vs.market.yandex.net/antifraud/detect";
        Http.THttpRequest httpRequest = Http.THttpRequest.newBuilder()
                .addHeaders(Http.THeader.newBuilder()
                        .setName("Host")
                        .setValue("mstat-antifraud-orders.vs.market.yandex.net")
                        .build())
                .setPath("/antifraud/detect")
                .build();

        String res = ApphostUtils.getHostAndEndpoint(httpRequest);

        assertEquals(res, expectedResult);
    }
}
