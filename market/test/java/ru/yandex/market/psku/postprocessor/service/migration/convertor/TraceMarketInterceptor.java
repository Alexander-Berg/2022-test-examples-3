package ru.yandex.market.psku.postprocessor.service.migration.convertor;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nur-Magomed Dzhamiev <a href="mailto:n-mago@yandex-team.ru"></a>
 */
class TraceMarketInterceptor implements HttpResponseInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TraceMarketInterceptor.class);

    @Override
    public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
        String requestId = response.getHeaders("x-market-req-id")[0].getValue();
        log.debug("market request id is {}", requestId);
    }
}
