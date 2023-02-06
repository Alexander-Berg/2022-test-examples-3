package ru.yandex.autotests.innerpochta.proxy;


import io.restassured.internal.http.URIBuilder;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;

import java.net.URISyntaxException;
import java.util.Vector;

import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.hamcrest.Matchers.containsString;

public class HostRootFilter extends HttpFiltersSourceAdapter {

    private String host;
    private Vector<String> stat = new Vector<>();

    private HostRootFilter(String host) {
        this.host = host;
    }

    public static HostRootFilter hostrootFilter(String host) {
        return new HostRootFilter(host);
    }

    @Override
    public HttpFilters filterRequest(HttpRequest originalRequest) {
        return new HttpFiltersAdapter(originalRequest) {
            @Override
            public HttpResponse proxyToServerRequest(HttpObject httpObject) {
                if (httpObject instanceof HttpRequest) {
                    HttpRequest httpRequest = (HttpRequest) httpObject;
                    String uri = httpRequest.getUri();
//                    System.out.println(" >>>> " + httpRequest.headers().get("Host") + " " + uri);
                    if (containsString("mail.yandex").matches(httpRequest.headers().get("Host"))
//                            && containsString("mail.yandex.").matches(uri)
                            ) {
//                        httpRequest

                        try {
//                            httpRequest.setUri(fromUri(URIBuilder.convertToURI(uri)).host(host).build().toString());
//                            System.out.println(" >>>>> URI   " + uri);
//                            System.out.println(" >>>>> HOST  " + URIBuilder.convertToURI(uri));
//                            System.out.println(" >>>>>       " + httpRequest.setUri(fromUri(URIBuilder.convertToURI(uri)).host(host).build().toString()));
                            httpRequest.setUri(fromUri(URIBuilder.convertToURI(uri)).host(host).build().toString());

                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }

                        httpRequest.setUri(uri.replaceFirst("mail.yandex.*/", "verstka11-qa.yandex.ru"));

                    }
                }
                return null;
            }
        };
    }

    public Vector<String> stat() {
        return stat;
    }
}
