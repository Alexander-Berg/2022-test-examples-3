package ru.yandex.market;

import org.springframework.beans.factory.annotation.Autowired;

public abstract class JettyFunctionalTest extends FunctionalTest {

    protected String baseUrl;

    protected String resourceUrl(String resourcePath) {
        if (!resourcePath.startsWith("/")) {
            resourcePath = String.format("/%s", resourcePath);
        }
        return String.format("%s%s", baseUrl, resourcePath);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    @Autowired
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
