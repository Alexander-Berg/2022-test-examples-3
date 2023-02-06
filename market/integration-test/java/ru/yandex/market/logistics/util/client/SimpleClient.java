package ru.yandex.market.logistics.util.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.model.CreateTestResourceRequest;
import ru.yandex.market.logistics.util.client.model.ResourceId;
import ru.yandex.market.logistics.util.client.model.TestResourceResponse;

/**
 * Тестовый клиент для проверки.
 *
 * @author avetokhin 2019-03-12.
 */
public class SimpleClient {

    private final HttpTemplate httpTemplate;

    public SimpleClient(final HttpTemplate httpTemplate) {
        this.httpTemplate = httpTemplate;
    }

    /**
     * POST метод с входным и выходным телом.
     */
    public TestResourceResponse createResource(final CreateTestResourceRequest request) {
        return httpTemplate.executePost(request, TestResourceResponse.class, "test", "create");
    }

    /**
     * GET метод с выходным телом.
     */
    public TestResourceResponse getResource(long id) {
        final Map<String, Set<String>> paramMap = new HashMap<>();
        paramMap.put("id", Collections.singleton(String.valueOf(id)));

        return httpTemplate.executeGet(TestResourceResponse.class, paramMap, "test", "get");
    }

    /**
     * POST метод с входным телом.
     */
    public void syncResource(final ResourceId resourceId) {
        httpTemplate.executePost(resourceId, "test", "sync");
    }

    /**
     * POST метод с входным телом и заданным контентом.
     */
    public void syncText(final String text) {
        HttpRequest<String> request = new HttpRequest<String>(HttpMethod.POST)
            .setContentType("text/plain")
            .setPath("test", "text")
            .setBody(text);
        httpTemplate.execute(request);
    }

    /**
     * GET метод с входным телом и заданным контентом.
     */
    public void getText() {
        HttpRequest<String> request = new HttpRequest<String>(HttpMethod.GET)
            .setContentType("text/plain")
            .setPath("test", "text2")
            .addHeader("Accept", "text/plain")
            .addHeader("Accept", "application/json");
        httpTemplate.execute(request, String.class);
    }

    /**
     * GET метод с пустым path;
     */
    public void getRoot() {
        HttpRequest<String> request = new HttpRequest<String>(HttpMethod.GET)
            .setContentType(null);
        httpTemplate.execute(request, String.class);
    }

}
