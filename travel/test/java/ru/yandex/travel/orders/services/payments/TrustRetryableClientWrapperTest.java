package ru.yandex.travel.orders.services.payments;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.http.apiclient.HttpApiRetryableException;
import ru.yandex.travel.orders.services.payments.model.TrustPaymentMethodsResponse;
import ru.yandex.travel.workflow.exceptions.RetryableException;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TrustRetryableClientWrapperTest {
    private TrustClient delegate;
    private TrustClient wrapper;

    @Before
    public void setUp() {
        delegate = mock(TrustClient.class);
        wrapper = new TrustRetryableClientWrapper(delegate);
    }

    @Test(expected = RetryableException.class)
    public void httpApiRetryableExceptionIsWrapped() {
        when(delegate.getPaymentMethods(any())).thenThrow(new HttpApiRetryableException("error!"));
        wrapper.getPaymentMethods(null);
    }

    @Test(expected = ArithmeticException.class)
    public void otherErrorsAreRethrown() {
        when(delegate.getPaymentMethods(any())).thenThrow(new ArithmeticException());
        wrapper.getPaymentMethods(null);
    }

    @Test(expected = RetryableException.class)
    public void asyncMethodsAlsoWrapHttpApiRetryableException() throws Throwable {
        when(delegate.getPaymentMethodsAsync(any())).thenReturn(
                CompletableFuture.failedFuture(new HttpApiRetryableException("404")));
        CompletableFuture<TrustPaymentMethodsResponse> result = wrapper.getPaymentMethodsAsync(null);
        assertTrue(result.isCompletedExceptionally());
        try {
            result.get();
            throw new AssertionError("expected Execution exception!");
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test
    public void passesParametersCorrectly() {
        TrustPaymentMethodsResponse rsp = TrustPaymentMethodsResponse.builder().build();
        TrustUserInfo req = new TrustUserInfo("uid", "ip");
        when(delegate.getPaymentMethods(req)).thenReturn(rsp);
        assertSame(rsp, wrapper.getPaymentMethods(req));
    }

}
