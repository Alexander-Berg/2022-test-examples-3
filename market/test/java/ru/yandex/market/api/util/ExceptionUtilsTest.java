package ru.yandex.market.api.util;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.common.ApiException;
import ru.yandex.market.api.common.ExternalServiceErrorException;
import ru.yandex.market.api.integration.UnitTestBase;

/**
 * Created by fettsery on 14.05.19.
 */
public class ExceptionUtilsTest extends UnitTestBase {

    @Test
    public void unwrapRuntimeExceptionWithCause() {
        Throwable e = new RuntimeException(new NullPointerException());

        Throwable unwrapped = ExceptionUtils.unwrap(e);

        Assert.assertEquals(NullPointerException.class, unwrapped.getClass());
    }

    @Test
    public void notUnwrapRuntimeExceptionWithoutCause() {
        Throwable e = new RuntimeException();

        Throwable unwrapped = ExceptionUtils.unwrap(e);

        Assert.assertEquals(RuntimeException.class, unwrapped.getClass());
    }

    @Test
    public void notUnwrapChild() {
        Throwable e = new ExternalServiceErrorException("test", "test", new NullPointerException());

        Throwable unwrapped = ExceptionUtils.unwrap(e);

        Assert.assertEquals(ExternalServiceErrorException.class, unwrapped.getClass());
    }

    @Test
    public void unwrapRecursively() {
        Throwable e = new RuntimeException(new RuntimeException(new NullPointerException()));

        Throwable unwrapped = ExceptionUtils.unwrap(e);

        Assert.assertEquals(NullPointerException.class, unwrapped.getClass());
    }

    @Test
    public void unwrapApiException() {
        Throwable e = new ApiException(new NullPointerException());

        Throwable unwrapped = ExceptionUtils.unwrap(e);

        Assert.assertEquals(NullPointerException.class, unwrapped.getClass());
    }
}