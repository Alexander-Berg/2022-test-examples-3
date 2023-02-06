
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
        "windowTime",
        "warnTime",
        "critTime",
        "goodTime",
        "eventType",
        "ticket"
})
public class Alert {

    @JsonProperty("windowTime")
    private String windowTime;
    @JsonProperty("warnTime")
    private String warnTime;
    @JsonProperty("critTime")
    private String critTime;
    @JsonProperty("goodTime")
    private String goodTime;
    @JsonProperty("eventType")
    private String eventType;
    @JsonProperty("ticket")
    private TicketInfo ticket;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("windowTime")
    public String getWindowTime() {
        return windowTime;
    }

    @JsonProperty("windowTime")
    public void setWindowTime(String windowTime) {
        this.windowTime = windowTime;
    }

    @JsonProperty("warnTime")
    public String getWarnTime() {
        return warnTime;
    }

    @JsonProperty("warnTime")
    public void setWarnTime(String warnTime) {
        this.warnTime = warnTime;
    }

    @JsonProperty("critTime")
    public String getCritTime() {
        return critTime;
    }

    @JsonProperty("critTime")
    public void setCritTime(String critTime) {
        this.critTime = critTime;
    }

    @JsonProperty("goodTime")
    public String getGoodTime() {
        return goodTime;
    }

    @JsonProperty("goodTime")
    public void setGoodTime(String goodTime) {
        this.goodTime = goodTime;
    }

    @JsonProperty("eventType")
    public String getEventType() {
        return eventType;
    }

    @JsonProperty("eventType")
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @JsonProperty("ticket")
    public TicketInfo getTicket() {
        return ticket;
    }

    @JsonProperty("ticket")
    public void setTicket(TicketInfo ticket) {
        this.ticket = ticket;
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
