package ru.yandex.market.request.netty;

import com.google.common.util.concurrent.ListenableFuture;
import io.netty.handler.codec.http.HttpMethod;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import ru.yandex.market.request.netty.retry.RetryAllWithSleepPolicy;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.request.trace.RequestContextHolder;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 14/10/16
 */
public class Main {
    public static void main(String[] args) throws Exception {
        RequestContextHolder.createNewContext();
        HttpClientConfig httpClientConfig = new HttpClientConfig();
        httpClientConfig.setRetryPolicy(new RetryAllWithSleepPolicy(5, 100));
        NettyHttpClient client = new NettyHttpClient(Module.GURU, httpClientConfig);
        RequestBuilder builder = new RequestBuilder(HttpMethod.GET.name()).setUrl("http://httpstat.us/500");


        ListenableFuture<Response> responseFuture = client.executeRequest(builder);
        Response response = responseFuture.get();
        System.gc();
//        RequestBuilder builder = new RequestBuilder().setUrl("http://httpstat.us/500");


//        client.

//        ListenableFuture<Response> responseFuture = client.prepareGet().execute(new NettyAsyncHandler());
//        Response response = responseFuture.get();
//        System.gc();
//
//        response.

//        asyncHttpClient.executeRequest()
//        asyncHttpClient.se
//    asyncHttpClient.


    }
}
