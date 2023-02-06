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
        "iframe",
        "fullscreen"
})
public class Grafana {

    @JsonProperty("iframe")
    private String iframe;
    @JsonProperty("fullscreen")
    private String fullscreen;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("iframe")
    public String getIframe() {
        return iframe;
    }

    @JsonProperty("iframe")
    public void setIframe(String iframe) {
        this.iframe = iframe;
    }

    @JsonProperty("fullscreen")
    public String getFullscreen() {
        return fullscreen;
    }

    @JsonProperty("fullscreen")
    public void setFullscreen(String fullscreen) {
        this.fullscreen = fullscreen;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
