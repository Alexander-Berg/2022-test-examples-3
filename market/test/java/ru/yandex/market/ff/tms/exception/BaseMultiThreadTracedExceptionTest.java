package ru.yandex.market.ff.tms.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.request.trace.RequestContext;


class BaseMultiThreadTracedExceptionTest {

    @Test
    public void errorMessageEmptyRequestContext() {
        RequestContext requestContext = new RequestContext("");
        Exception e = new BaseMultiThreadTracedException(requestContext, new Exception("some message"));

        Assertions.assertEquals("some message", e.getMessage());
    }

    @Test
    public void errorMessageNullRequestContext() {
        Exception e = new BaseMultiThreadTracedException(null, new Exception("some message"));

        Assertions.assertEquals("some message", e.getMessage());
    }

    @Test
    public void errorMessageOnePartRequestContext() {
        RequestContext requestContext = new RequestContext("1");

        Exception e = new BaseMultiThreadTracedException(requestContext, new Exception("some message"));

        Assertions.assertEquals("some message", e.getMessage());
    }

    @Test
    public void errorMessageTwoPartRequestContext() {
        RequestContext requestContext = new RequestContext("123/35467");

        Exception e = new BaseMultiThreadTracedException(requestContext, new Exception("some message"));

        Assertions.assertEquals("some message", e.getMessage());
    }

    @Test
    public void errorMessageCorrectRequestContextWithShopRequestId() {
        RequestContext requestContext = new RequestContext("123/35467/777");

        Exception e = new BaseMultiThreadTracedException(requestContext, new Exception("some message"));

        Assertions.assertEquals("../../777 some message", e.getMessage());
    }

    @Test
    public void errorMessageCorrectRequestContextWithShopRequestIdAndSubRequest() {
        RequestContext requestContext = new RequestContext("123/35467/777/999");

        Exception e = new BaseMultiThreadTracedException(requestContext, new Exception("some message"));

        Assertions.assertEquals("../../777/999 some message", e.getMessage());
    }
}
