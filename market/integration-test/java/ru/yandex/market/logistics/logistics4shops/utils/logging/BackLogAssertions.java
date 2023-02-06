package ru.yandex.market.logistics.logistics4shops.utils.logging;

import java.util.Objects;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.ArrayUtils;
import org.assertj.core.internal.IgnoringFieldsComparator;

import ru.yandex.market.logistics.logistics4shops.logging.LoggingCode;
import ru.yandex.market.logistics.logistics4shops.logging.LoggingTag;

@ParametersAreNonnullByDefault
public final class BackLogAssertions {

    private BackLogAssertions() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static Predicate<TskvLogRecord<?>> logEqualsTo(TskvLogRecord<?> expected) {
        return expected::equals;
    }

    @Nonnull
    public static Predicate<TskvLogRecord<?>> logEqualsToIgnoringFields(TskvLogRecord<?> expected, String... fields) {
        return actual -> new IgnoringFieldsComparator(fields).compare(actual, expected) == 0;
    }

    @Nonnull
    public static Predicate<TskvLogRecord<?>> logHasTag(LoggingTag tag) {
        return record -> ArrayUtils.contains(record.getTags(), tag.name());
    }

    @Nonnull
    public static Predicate<TskvLogRecord<?>> logHasCode(LoggingCode<?> code) {
        return record -> Objects.equals(code.asEnum().name(), record.getCode());
    }

    @Nonnull
    public static Predicate<TskvLogRecord<?>> logHasLevel(TskvLogRecord.Level level) {
        return record -> record.getLevel() == level;
    }

    @Nonnull
    public static Predicate<TskvLogRecord<?>> logHasFormat(TskvLogRecordFormat<?> format) {
        return record -> Objects.equals(record.getFormat(), format);
    }
}
