package ru.yandex.market.crm.triggers.test;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author apershukov
 */
public class EventSubscription {

    public final String eventType;
    public final String eventName;
    public final String processInstanceId;
    public final LocalDateTime visitTime;

    public EventSubscription(String eventType,
                             String eventName,
                             String processInstanceId,
                             LocalDateTime visitTime) {
        this.eventType = eventType;
        this.eventName = eventName;
        this.processInstanceId = processInstanceId;
        this.visitTime = visitTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EventSubscription that = (EventSubscription) o;
        return Objects.equals(eventType, that.eventType) &&
            Objects.equals(eventName, that.eventName) &&
            Objects.equals(processInstanceId, that.processInstanceId) &&
            Objects.equals(visitTime, that.visitTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventType, eventName, processInstanceId, visitTime);
    }
}
