package ru.yandex.market.logistics.logistics4shops.utils;

import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.protobuf.Timestamp;
import lombok.experimental.UtilityClass;
import org.assertj.core.api.ProxyableObjectAssert;
import org.assertj.core.api.RecursiveComparisonAssert;

@UtilityClass
@ParametersAreNonnullByDefault
public class ProtobufAssertionsUtils {
    @Nonnull
    public <T> RecursiveComparisonAssert<?> prepareProtobufAssertion(ProxyableObjectAssert<T> assertion) {
        return assertion.usingRecursiveComparison().ignoringFieldsMatchingRegexes(".*memoized.*");
    }

    @Nonnull
    public Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).build();
    }
}
