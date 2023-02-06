package ru.yandex.autotests.innerpochta.proxy;

import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.hamcrest.Matcher;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;

import java.util.regex.Pattern;

import static ru.yandex.autotests.innerpochta.util.props.HostRootProperties.hostrootProps;

public class LittleHostFilter extends HttpFiltersSourceAdapter {

    private Matcher<String> m;

    private LittleHostFilter(Matcher<String> m) {
        this.m = m;
    }

    public static LittleHostFilter hostFilter(Matcher<String> m) {
        return new LittleHostFilter(m);
    }

    @Override
    public HttpFilters filterRequest(HttpRequest originalRequest) {
        return new HttpFiltersAdapter(originalRequest) {
            @Override
            public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                if (httpObject instanceof HttpRequest) {
                    HttpRequest httpRequest = (HttpRequest) httpObject;
                    String uri = httpRequest.getUri();
                    if (m.matches(httpRequest.headers().get("Host"))) {
                        if (!uri.startsWith("/?retpath")) {
                            if (!uri.contains("monitoring.txt")) {
                                httpRequest.setUri(getReplacedHost(uri));
                            }
                        }
                    }
                }
                return null;
            }
        };
    }

    private String getReplacedHost(String uri) {
        Pattern p = Pattern.compile("(.*/|^)(stand-[^.]*|[a-z-]+[0-9-]+[0-9]+|ub[0-9]{1,2}).*");
        final String LIZA_HOST_FORMAT = "nginx-1.nginx.%s.verstka-qa.mail.stable.qloud-d.yandex.net";
        String newUrl = hostrootProps().testhost();
        java.util.regex.Matcher m = p.matcher(newUrl);
        if (m.find()) {
            newUrl = String.format(LIZA_HOST_FORMAT, m.group(2));
        }
        return uri.replaceFirst(
            "mail.yandex.[^:?/]+",
            newUrl
        );
    }
}
