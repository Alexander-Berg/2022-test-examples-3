package ru.yandex.calendar.logic.notification.xiva;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Repetition;
import ru.yandex.calendar.logic.event.CreateInfo;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.repetition.EventAndRepetition;

class EventTestConverter {
    public EventData convert(CreateInfo createInfo) {
        EventData eventData = new EventData();
        eventData.setEvent(createInfo.getEvent().copy());

        Option<Repetition> repetition = createInfo.getRepetitionInfo().getRepetition();
        if (repetition.isPresent()) {
            eventData.setRepetition(repetition.get().copy());
        }

        return eventData;
    }

    public EventData convert(EventAndRepetition event) {
        EventData eventData = new EventData();
        eventData.setEvent(event.getEvent().copy());

        Option<Repetition> repetition = event.getRepetitionInfo().getRepetition();
        if (repetition.isPresent()) {
            eventData.setRepetition(repetition.get().copy());
        }

        return eventData;
    }
}
