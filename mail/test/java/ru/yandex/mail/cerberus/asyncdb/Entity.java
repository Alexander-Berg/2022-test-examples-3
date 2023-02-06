package ru.yandex.mail.cerberus.asyncdb;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;
import org.jdbi.v3.core.mapper.Nested;
import ru.yandex.mail.cerberus.asyncdb.annotations.Id;
import ru.yandex.mail.cerberus.asyncdb.annotations.Json;
import ru.yandex.mail.cerberus.asyncdb.annotations.JsonB;
import ru.yandex.mail.cerberus.asyncdb.annotations.Serial;
import ru.yandex.mail.micronaut.common.value.LongValueType;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.OptionalInt;

@With
@Value
public class Entity {
    @Value
    @AllArgsConstructor(onConstructor_= @JsonCreator)
    public static class Data {
        int version;
        String text;
    }

    @Value
    public static class TypeSafeId implements LongValueType {
        long value;

        @Override
        public String toString() {
            return Long.toString(value);
        }
    }

    @Id @Serial TypeSafeId id;
    String name;
    OptionalInt age;
    @Json Optional<Data> jsonData;
    @JsonB Optional<Data> jsonBinaryData;
    @JsonB Optional<String> genericJsonBinaryData;
    @Nested("nested") @Nullable NestedData nested;
}
