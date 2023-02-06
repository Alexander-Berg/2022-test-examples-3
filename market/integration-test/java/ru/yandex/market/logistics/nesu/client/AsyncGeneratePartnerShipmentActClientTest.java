package ru.yandex.market.logistics.nesu.client;

import java.util.concurrent.CompletableFuture;

import lombok.SneakyThrows;
import org.asynchttpclient.AsyncHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import retrofit2.Retrofit;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.logistics.nesu.client.async.NesuAsyncClientService;
import ru.yandex.market.logistics.nesu.client.async.NesuAsyncClientServiceImpl;
import ru.yandex.market.logistics.nesu.client.async.NesuRetrofitService;
import ru.yandex.market.request.trace.Module;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

@DisplayName("Генерация АПП в асинхронном клиенте")
public class AsyncGeneratePartnerShipmentActClientTest extends AbstractClientTest {

    @Autowired
    protected AsyncHttpClient nesuAsyncHttpClient;

    @Autowired
    protected Retrofit nesuRetrofit;

    private NesuAsyncClientService nesuAsyncClientService;

    private NesuRetrofitService nesuRetrofitService;

    @BeforeEach
    public void setUpSpy() {
        nesuRetrofitService = spy(new NesuRetrofitService(nesuRetrofit, Module.NESU));
        nesuAsyncClientService = new NesuAsyncClientServiceImpl(nesuRetrofitService);
    }

    @Test
    @SneakyThrows
    @DisplayName("Асинхронный запрос")
    void asyncRequest() {
        byte[] content = {1, 2, 3};
        var result = CompletableFuture.completedFuture(content);

        mockAsyncInvocation(result);

        softly.assertThat(nesuAsyncClientService.generateInboundAct(100, 500, 1000).get()).isEqualTo(result.get());
    }

    private void mockAsyncInvocation(CompletableFuture<byte[]> result) {
        doAnswer(callerInvocation -> {
            var invocationResult = (ExecuteCall) callerInvocation.callRealMethod();
            var spyInvocationResult = spy(invocationResult);
            doAnswer(scheduleInvocation -> {
                scheduleInvocation.callRealMethod();
                return result;
            }).when(spyInvocationResult).schedule();
            return spyInvocationResult;
        }).when(nesuRetrofitService).caller(any());
    }
}
