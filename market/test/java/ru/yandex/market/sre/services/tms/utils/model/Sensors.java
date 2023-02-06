
package ru.yandex.market.sre.services.tms.utils.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "White Desktop",
        "White Touch",
        "Blue Desktop",
        "Blue Touch"
})
public class Sensors {

    @JsonProperty("White Desktop")
    private List<Platform> whiteDesktop = null;
    @JsonProperty("White Touch")
    private List<Platform> whiteTouch = null;
    @JsonProperty("Blue Desktop")
    private List<Platform> blueDesktop = null;
    @JsonProperty("Blue Touch")
    private List<Platform> blueTouch = null;
    @JsonProperty("Blue FAPI")
    private List<Platform> blueFAPI = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("White Desktop")
    public List<Platform> getWhiteDesktop() {
        return whiteDesktop;
    }

    @JsonProperty("White Desktop")
    public void setWhiteDesktop(List<Platform> whiteDesktop) {
        this.whiteDesktop = whiteDesktop;
    }

    @JsonProperty("White Touch")
    public List<Platform> getWhiteTouch() {
        return whiteTouch;
    }

    @JsonProperty("White Touch")
    public void setWhiteTouch(List<Platform> whiteTouch) {
        this.whiteTouch = whiteTouch;
    }

    @JsonProperty("Blue Desktop")
    public List<Platform> getBlueDesktop() {
        return blueDesktop;
    }

    @JsonProperty("Blue Desktop")
    public void setBlueDesktop(List<Platform> blueDesktop) {
        this.blueDesktop = blueDesktop;
    }

    @JsonProperty("Blue Touch")
    public List<Platform> getBlueTouch() {
        return blueTouch;
    }

    @JsonProperty("Blue Touch")
    public void setBlueTouch(List<Platform> blueTouch) {
        this.blueTouch = blueTouch;
    }

    @JsonProperty("Blue FAPI")
    public List<Platform> getBlueFAPI() {
        return blueFAPI;
    }

    @JsonProperty("Blue FAPI")
    public void setBlueFAPI(List<Platform> blueFAPI) {
        this.blueFAPI = blueFAPI;
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
