package ru.yandex.market.mbo.tms.billing;

import java.time.Instant;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

public class BillingSessionMarksMock implements BillingSessionMarks {

    private TreeSet<Instant> sessionStarts = new TreeSet<>();
    private TreeSet<Instant> sessionUpdates = new TreeSet<>();
    private TreeMap<Instant, Boolean> sessionEnds = new TreeMap<>();

    public void storeSessionStart(Instant start) {
        sessionStarts.add(start);
    }

    @Override
    public void storeSessionStart() {
        Instant start = Instant.now();
        storeSessionStart(start);
    }

    public void storeSessionEnd(Instant end, boolean success) {
        sessionEnds.put(end, success);
    }

    @Override
    public void storeSessionEnd(boolean success) {
        Instant end = Instant.now();
        storeSessionEnd(end, success);
    }

    @Override
    public Instant loadSessionStart() {
        return sessionStarts.isEmpty() ? null : sessionStarts.last();
    }

    @Override
    public Optional<SessionResult> loadSessionEnd() {
        if (sessionEnds.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SessionResult(sessionEnds.lastKey(), sessionEnds.lastEntry().getValue()));
    }

    public void storeSessionUpdate(Instant time) {
        sessionUpdates.add(time);
    }

    @Override
    public void storeSessionUpdate() {
        storeSessionUpdate(Instant.now());
    }

    @Override
    public Instant loadSessionUpdate() {
        return sessionUpdates.isEmpty() ? null : sessionUpdates.last();
    }

    @Override
    public Optional<String> loadLastIncompleteSessionHostname() {
        return Optional.empty();
    }
}
