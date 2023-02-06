package ru.yandex.market.request.jetty.trace;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

public class TskvRecordParser {

    private static final String TAB = "\t";

    public static List<Item> parse(String tskvRecord) {
        return Arrays.stream(tskvRecord.split(TAB))
                .filter(x -> !Strings.isNullOrEmpty(x))
                .map(TskvRecordParser::parseRecord)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private static Optional<Item> parseRecord(String value) {
        final int equalSignPos = value.indexOf('=');
        if (equalSignPos >= 0) {
            return Optional.of(new Item(value.substring(0, equalSignPos), value.substring(equalSignPos + 1)));
        }
        return Optional.empty();
    }

    public static class Item {
        private final String key;
        private final String value;

        public Item(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }
}
