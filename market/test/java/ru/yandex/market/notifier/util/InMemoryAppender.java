package ru.yandex.market.notifier.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import ru.yandex.common.util.collections.Pair;

import static java.util.stream.Collectors.toMap;

public class InMemoryAppender extends AppenderBase<ILoggingEvent> {

    private final List<ILoggingEvent> log = new ArrayList<>();

    private static Map<String, String> tskvToMap(String tskv) {
        return Arrays.stream(tskv.split("\t"))
                .filter(pair -> pair.contains("="))
                .map(pair -> {
                    String[] keyValueArray = pair.split("=", 2);
                    return new Pair<>(
                            keyValueArray[0],
                            keyValueArray[1]
                    );
                }).collect(toMap(Pair::getFirst, Pair::getSecond));
    }

    @Override
    protected void append(ILoggingEvent event) {
        log.add(event);
    }

    public void clear() {
        log.clear();
    }

    public List<ILoggingEvent> getRaw() {
        return log;
    }

    public List<Map<String, String>> getTskvMaps() {
        return new ArrayList<>(log).stream()
                .map(ILoggingEvent::getMessage)
                .map(Object::toString)
                .map(InMemoryAppender::tskvToMap)
                .collect(Collectors.toList());
    }
}
