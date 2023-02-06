package ru.yandex.reminders.logic.callmeback;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class DictItems {
    @JsonProperty("callmeback-api")
    private PortDesc callmeback;
    private PortDesc reminders;
}
