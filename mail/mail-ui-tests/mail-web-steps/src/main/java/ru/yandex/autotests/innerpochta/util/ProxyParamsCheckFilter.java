package ru.yandex.autotests.innerpochta.util;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import ru.yandex.autotests.innerpochta.rules.ProxyServerRule;

/**
 * Created by mabelpines on 01.06.15.
 */
public class ProxyParamsCheckFilter extends HttpFiltersSourceAdapter {
    private String[] handlersName;

    public static ProxyParamsCheckFilter proxyParamsCheckFilter(String... handlersName) {
        return new ProxyParamsCheckFilter(handlersName);
    }

    public ProxyParamsCheckFilter(String[] handlersName) { this.handlersName = handlersName; }

    @Override
    public int getMaximumRequestBufferSizeInBytes() {
        return 5000000;
    }

    @Override
    public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
        return new HttpFiltersAdapter(originalRequest) {
            @Override
            public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                if (httpObject instanceof FullHttpRequest) {
                    FullHttpRequest httpRequest = (FullHttpRequest) httpObject;
                    saveReqParams(httpRequest);
                }
                return null;
            }
        };
    }

    private void saveReqParams(FullHttpRequest httpRequest) {
        for (String handler : handlersName) {
            if (httpRequest.uri().contains(buildHandlerUrl(handler))) {
                ProxyServerRule.setRequestPostBody(getRequestBody(httpRequest));
            }
        }
    }

    private String buildHandlerUrl(String handlersName) {
        return new StringBuilder("?_m=").append(handlersName).toString();
    }

    private String getRequestBody(HttpObject httpObject) {
        HttpContent cont = (HttpContent) httpObject;
        ByteBuf buffer = cont.content();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buffer.capacity(); i++) {
            sb.append((char) buffer.getByte(i));
        }
//        buffer.release();
        return sb.toString();
    }
}
