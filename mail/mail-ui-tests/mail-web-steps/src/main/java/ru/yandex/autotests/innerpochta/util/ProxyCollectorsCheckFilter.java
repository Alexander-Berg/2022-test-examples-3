package ru.yandex.autotests.innerpochta.util;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.apache.commons.io.IOUtils;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;

import java.io.IOException;

import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.autotests.innerpochta.util.handlers.CollectorsConstants.HANDLER_DO_COLLECTOR_CHECK;


/**
 * Created by mabelpines on 16.05.15.
 */
public class ProxyCollectorsCheckFilter extends HttpFiltersSourceAdapter {
    private static String jsonResourcePath;

    public static ProxyCollectorsCheckFilter proxyCollectorsCheckFilter(String jsonResourcePath) {
        return new ProxyCollectorsCheckFilter(jsonResourcePath);
    }

    private ProxyCollectorsCheckFilter(String jsonResourcePath){
        this.jsonResourcePath = jsonResourcePath;
    }

    @Override
    public HttpFilters filterRequest(HttpRequest originalRequest) {
        return new HttpFiltersAdapter(originalRequest) {
            @Override
            public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                if (httpObject instanceof HttpRequest) {
                    HttpRequest httpRequest = (HttpRequest) httpObject;
                    if ((containsString(HANDLER_DO_COLLECTOR_CHECK)).matches(httpRequest.getUri())) {
                        return collectorCheckSuccess();
                    }
                }
                return null;
            }
        };
    }

    private static HttpResponse collectorCheckSuccess(){
        HttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.copiedBuffer(getJsonRespFromResourses()));
        httpResponse.headers().add("Content-Type", "application/json");
        return httpResponse;
    }

    private static byte[] getJsonRespFromResourses(){
        byte[] resp = new byte[0];
        try {
            resp = IOUtils.toString(ProxyCollectorsCheckFilter.class.getClassLoader()
                    .getResourceAsStream(jsonResourcePath)).getBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resp;
    }
}
