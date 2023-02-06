package ru.yandex.market.mbi.affiliate.promo.common;

import org.junit.Test;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RetrofitRetryingCallbackTest {

    @SuppressWarnings("unchecked")
    @Test
    public void test() {
        Call<String> callA = mock(Call.class, RETURNS_DEEP_STUBS);
        Call<String> callB = mock(Call.class, RETURNS_DEEP_STUBS);
        doAnswer(invocation -> {
            var callback = (Callback<String>) invocation.getArgument(0);
            callback.onFailure(callA, new Exception("A failed"));
            return null;
        }).when(callA).enqueue(any());
        doAnswer(invocation -> {
            var callback = (Callback<String>) invocation.getArgument(0);
            callback.onResponse(callB, Response.success("b!"));
            return null;
        }).when(callB).enqueue(any());

        var callbackA = new RetrofitRetryingCallback<>("A", v -> callA, 3);
        callA.enqueue(callbackA);
        var callbackB = new RetrofitRetryingCallback<>("B", v -> callB, 3);
        callB.enqueue(callbackB);

        verify(callA, times(4)).enqueue(any());
        verify(callB, times(1)).enqueue(any());
    }
}
