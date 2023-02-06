package ru.yandex.autotests.reporting.api.beans;

import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Property;
import ru.yandex.qatools.properties.annotations.Resource;

/**
 * Created by kateleb on 15.11.16.
 */
@Resource.Classpath("reporting-api.properties")
public class ReportingApiConfig {

    @Property("reporting.api.base.url")
    private String apiBaseUrl = "http://mstgate01ht.market.yandex.net:8082/reporting/";


    public ReportingApiConfig() {
        PropertyLoader.populate(this);
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }
}
