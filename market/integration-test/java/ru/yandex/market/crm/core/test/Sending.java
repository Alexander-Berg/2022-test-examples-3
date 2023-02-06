package ru.yandex.market.crm.core.test;

import com.fasterxml.jackson.annotation.JsonProperty;

import ru.yandex.market.crm.platform.commons.SendingType;

/**
 * @author apershukov
 */
public class Sending {

    public static Sending periodicSending(String key) {
        return new Sending(key, SendingType.PERIODIC_SENDING);
    }

    public static Sending promo(String id) {
        return new Sending(id, SendingType.PROMO);
    }

    public static Sending trigger(String key) {
        return new Sending(key, SendingType.TRIGGER);
    }

    @JsonProperty("id")
    private final String id;

    @JsonProperty("type")
    private final SendingType type;

    private Sending(String id, SendingType type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public SendingType getType() {
        return type;
    }
}
