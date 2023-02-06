package ru.yandex.direct.juggler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.yandex.direct.utils.MonotonicClock;

public class StubJugglerClient implements JugglerClient {
    private List<JugglerEvent> sentEvents;
    private int eventsCount;
    private Duration sendDelay;
    private String rejectingDescription;
    private MonotonicClock clock;

    public StubJugglerClient(Duration sendDelay, String rejectingDescription, MonotonicClock clock) {
        this.sentEvents = new ArrayList<>();
        this.eventsCount = 0;
        this.sendDelay = sendDelay;
        this.rejectingDescription = rejectingDescription;
        this.clock = clock;
    }

    public List<JugglerEvent> getSentEvents() {
        return sentEvents;
    }

    @Override
    public void sendEvents(List<JugglerEvent> events) {
        synchronized (this) {
            eventsCount += events.size();
            notifyAll();
        }

        clock.sleepUninterruptibly(sendDelay);
        List<JugglerResponse.FailedEvent> failed = new ArrayList<>();

        synchronized (this) {
            for (JugglerEvent event : events) {
                if (event.getDescription().equals(rejectingDescription)) {
                    failed.add(new JugglerResponse.FailedEvent(
                            new JugglerResponse.EventResponse("failed: " + rejectingDescription, 500),
                            event
                    ));
                } else {
                    sentEvents.add(event);
                }
            }
        }

        if (!failed.isEmpty()) {
            throw new JugglerClient.FailedEventsException(failed);
        }
    }

    @Override
    public List<JugglerChecksStateItem> getChecksStateItems(JugglerChecksStateFilter filter) {
        return Collections.emptyList();
    }

    public synchronized void waitForEventsCount(int count) throws InterruptedException {
        while (eventsCount < count) {
            wait();
        }
    }

    public void close() {
    }
}
