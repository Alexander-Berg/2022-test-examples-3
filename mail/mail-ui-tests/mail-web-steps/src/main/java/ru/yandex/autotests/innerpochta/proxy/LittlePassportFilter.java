package ru.yandex.autotests.innerpochta.proxy;

import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.hamcrest.Matcher;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;

import static ru.yandex.autotests.innerpochta.util.props.PassportProperties.passProps;

/**
 * Created by kurau on 13.04.15.
 */
public class LittlePassportFilter extends HttpFiltersSourceAdapter {
    private Matcher m;

    private LittlePassportFilter(Matcher m) {
        this.m = m;
    }

    public static LittlePassportFilter passportFilter(Matcher m) {
        return new LittlePassportFilter(m);
    }

    @Override
    public HttpFilters filterRequest(HttpRequest originalRequest) {
        return new HttpFiltersAdapter(originalRequest) {
            @Override
            public HttpResponse proxyToServerRequest(HttpObject httpObject) {
                if (httpObject instanceof HttpRequest) {
                    HttpRequest httpRequest = (HttpRequest) httpObject;
                    String uri = httpRequest.getUri();
                    if (m.matches(httpRequest.headers().get("Host"))) {
                        if (!uri.startsWith("/?retpath")) {
                            if (!uri.contains("monitoring.txt")) {
                                System.out.println(" DO "  + uri);
                                httpRequest.setUri(uri.replaceFirst("passport.yandex.(ru|(com.tr)|com)",
                                        passProps().pasportHost()));
                                System.out.println(" POSLE "  + httpRequest);
                            }
                        }
                    }
                }
                return null;
            }
        };
    }
}
