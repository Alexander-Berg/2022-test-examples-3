package ru.yandex.market.logistics.logistics4shops.utils.logging;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.text.StringEscapeUtils;

@ParametersAreNonnullByDefault
public final class TskvLogRecordFormat<T> {
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private static final Map<String, TskvLogRecordFormat<?>> FORMAT_MAP = new HashMap<>();

    public static final TskvLogRecordFormat<String> PLAIN
        = new TskvLogRecordFormat<>(String.class, "plain");
    public static final TskvLogRecordFormat<ExceptionPayload> JSON_EXCEPTION
        = new TskvLogRecordFormat<>(ExceptionPayload.class, "json-exception");

    private final Class<T> payloadClass;
    private final String name;

    private TskvLogRecordFormat(Class<T> payloadClass, String name) {
        this.payloadClass = payloadClass;
        this.name = name;
        FORMAT_MAP.put(name, this);
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Nonnull
    @SneakyThrows
    public T parsePayload(String payload) {
        return this.payloadClass.equals(String.class)
            ? (T) payload
            : MAPPER.readValue(StringEscapeUtils.unescapeJson(payload), this.payloadClass);
    }

    @Nonnull
    public static TskvLogRecordFormat<?> getByFormat(String format) {
        return FORMAT_MAP.getOrDefault(format, PLAIN);
    }

    @Value
    @Builder
    @Jacksonized
    public static class ExceptionPayload {
        String eventMessage;
        String exceptionMessage;
        // Omit stacktrace as no one will assert it

        @Nonnull
        public static ExceptionPayload of(String eventMessage, String exceptionMessage) {
            return ExceptionPayload.builder()
                .eventMessage(eventMessage)
                .exceptionMessage(exceptionMessage)
                .build();
        }
    }
}
