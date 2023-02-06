package ru.yandex.market.test.scenario.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.test.scenario.AbstractTestScenarioStep;
import ru.yandex.market.test.scenario.TestScenarioContext;
import ru.yandex.market.test.util.RestUtils;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 18.05.16
 */
public abstract class HttpRequestStep<T> extends AbstractTestScenarioStep {
    private static final Logger log = LoggerFactory.getLogger(HttpRequestStep.class);

    private String url;
    private HttpMethod method;
    private Class<T> responseType;

    public HttpRequestStep() {
        super();
    }

    public HttpRequestStep(String name, String url, HttpMethod method, Class<T> responseType) {
        super(name);
        this.url = url;
        this.method = method;
        this.responseType = responseType;
    }

    public HttpRequestStep(String name, String description, String url, HttpMethod method, Class<T> responseType) {
        super(name, description);
        this.url = url;
        this.method = method;
        this.responseType = responseType;
    }

    @Override
    public boolean make(TestScenarioContext context) {
        RestTemplate template = RestUtils.buildDefaultRestTemplate();
        log.debug("Calling " + method.name() + " method on " + url);
        ResponseEntity<T> responseEntity = template.exchange(url, method, request(context), responseType);
        return validateResponse(context, responseEntity);
    }

    protected abstract HttpEntity request(TestScenarioContext context);

    protected abstract boolean validateResponse(TestScenarioContext context, ResponseEntity<T> responseEntity);

    public void setUrl(String url) {
        this.url = url;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public void setResponseType(Class<T> responseType) {
        this.responseType = responseType;
    }
}
