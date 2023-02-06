package ru.yandex.market.checkout.checkouter.trace;

import java.util.List;
import java.util.Map;

import ru.yandex.market.checkout.carter.InMemoryAppender;

public final class TraceLogHelper {

    private TraceLogHelper() {
    }

    /**
     * Трассировка пишется после того как запрос отдали клиенту, поэтому нужно немного подождать
     */
    public static List<Map<String, String>> awaitTraceLog(InMemoryAppender traceAppender,
                                                          List<Map<String, String>> events) {

        int i = 5;
        while ((events.isEmpty() || events.stream().noneMatch(tskv -> "IN".equals(tskv.get("type")))) && i > 0) {
            events = traceAppender.getTskvMaps();
            i--;

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        if (i == 0) {
            throw new IllegalStateException("No trace log after 5000ms");
        }

        return events;
    }
}
