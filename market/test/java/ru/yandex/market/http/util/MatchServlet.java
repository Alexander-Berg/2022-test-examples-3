package ru.yandex.market.http.util;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ru.yandex.market.api.util.ConcurrentUtils;

/**
 * @author dimkarp93
 */
public class MatchServlet extends HttpServlet {
    private final AtomicInteger counter = new AtomicInteger(0);
    private final IntFunction<HttpConfig> httpConfigGen;

    public MatchServlet(IntFunction<HttpConfig> httpConfigGen) {
        this.httpConfigGen = httpConfigGen;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpConfig httpConfig = httpConfigGen.apply(counter.incrementAndGet());

        if (httpConfig.timeout > 0) {
            ConcurrentUtils.sleep(httpConfig.timeout);
        }

        for (Map.Entry<Predicate<HttpServletRequest>,
                BiConsumer<HttpServletRequest, HttpServletResponse>> p: httpConfig.processors.entrySet()) {
            if (p.getKey().test(req)) {
                p.getValue().accept(req, resp);
                return;
            }
        }

        httpConfig.defaultProcessor.accept(req, resp);
    }
}
