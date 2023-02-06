package ru.yandex.iex.proxy;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

public class OnlineHandler implements HttpRequestHandler {
    private final boolean online;

    public OnlineHandler(final boolean online) {
        this.online = online;
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
    {
        final String responseHeader;
        if (online) {
            responseHeader = "Online";
        } else {
            responseHeader = "Offline";
        }
        response.setHeader("Status", responseHeader);
    }
}
