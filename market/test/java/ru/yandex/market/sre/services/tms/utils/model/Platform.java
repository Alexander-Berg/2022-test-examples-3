
package ru.yandex.market.sre.services.tms.utils.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "title",
        "query",
        "rawQuery",
        "threshold",
        "higher",
        "grafana",
        "alerting"
})
public class Platform {

    @JsonProperty("title")
    private String title;
    @JsonProperty("query")
    private String query;
    @JsonProperty("rawQuery")
    private String rawQuery;
    @JsonProperty("threshold")
    private Double threshold;
    @JsonProperty("higher")
    private Boolean higher;
    @JsonProperty("grafana")
    private Grafana grafana;
    @JsonProperty("alerting")
    private Alert alerting;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
    }

    @JsonProperty("query")
    public String getQuery() {
        return query;
    }

    @JsonProperty("query")
    public void setQuery(String query) {
        this.query = query;
    }

    @JsonProperty("rawQuery")
    public String getRawQuery() {
        return rawQuery;
    }

    @JsonProperty("rawQuery")
    public void setRawQuery(String rawQuery) {
        this.rawQuery = rawQuery;
    }

    @JsonProperty("threshold")
    public Double getThreshold() {
        return threshold;
    }

    @JsonProperty("threshold")
    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    @JsonProperty("higher")
    public Boolean getHigher() {
        return higher;
    }

    @JsonProperty("higher")
    public void setHigher(Boolean higher) {
        this.higher = higher;
    }

    @JsonProperty("alerting")
    public Alert getAlerting() {
        return alerting;
    }

    @JsonProperty("alerting")
    public void setAlerting(Alert alerting) {
        this.alerting = alerting;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @JsonProperty("grafana")
    public Grafana getGrafana() {
        return grafana;
    }

    @JsonProperty("grafana")
    public void setGrafana(Grafana grafana) {
        this.grafana = grafana;
    }
}
