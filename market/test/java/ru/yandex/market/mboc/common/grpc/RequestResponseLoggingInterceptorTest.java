package ru.yandex.market.mboc.common.grpc;

import Market.DataCamp.DataCampOfferTechInfo;
import org.junit.Before;
import org.junit.Test;

public class RequestResponseLoggingInterceptorTest {
    private static final Long seconds = 1752738931163698706L;

    private RequestResponseLoggingInterceptor requestResponseLoggingInterceptor;

    @Before
    public void prepare() {
        requestResponseLoggingInterceptor = new RequestResponseLoggingInterceptor();
    }

    @Test
    public void testIncorrectTsNotFailing() {
        var updateBuilder = DataCampOfferTechInfo.UpdateDropInfo.newBuilder();
        updateBuilder.getMetaBuilder().getTimestampBuilder().setSeconds(seconds);
        requestResponseLoggingInterceptor.logTrace("dir", "method", updateBuilder.build());
    }
}
