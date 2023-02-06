package ru.yandex.market.logistics.iris.core.index;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.google.common.collect.ImmutableSet;

import ru.yandex.market.logistics.iris.core.index.dummy.TestPredefinedField;
import ru.yandex.market.logistics.iris.core.index.field.Field;
import ru.yandex.market.logistics.iris.core.index.field.FieldValue;
import ru.yandex.market.logistics.iris.core.index.field.PredefinedFieldProvider;
import ru.yandex.market.logistics.iris.core.index.field.PredefinedFields;
import ru.yandex.market.logistics.iris.core.index.implementation.ChangeTrackingReferenceIndexer;
import ru.yandex.market.logistics.iris.core.index.implementation.Reference;
import ru.yandex.market.logistics.iris.core.index.json.deserialization.FieldValueDeserializer;
import ru.yandex.market.logistics.iris.core.index.json.serialization.FieldValueSerializer;
import ru.yandex.market.logistics.iris.core.index.json.serialization.ReferenceSerializer;

public class ReferenceIndexerTestFactory {

    public static PredefinedFieldProvider getProvider() {
        return PredefinedFieldProvider.of(
            ImmutableSet.<Field<?>>builder()
                .addAll(PredefinedFields.getAllPredefinedFields())
                .add(TestPredefinedField.DUMMY)
                .add(TestPredefinedField.YUMMY)
                .add(TestPredefinedField.GUMMY)
                .build()
        );
    }

    public static ChangeTrackingReferenceIndexer getIndexer() {
        return new ChangeTrackingReferenceIndexer(objectMapper(getProvider()));
    }

    private static ObjectMapper objectMapper(PredefinedFieldProvider predefinedFieldProvider) {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();

        module.addSerializer(Reference.class, new ReferenceSerializer());
        module.addSerializer(FieldValue.class, new FieldValueSerializer(mapper));
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_DATE_TIME));

        module.addDeserializer(FieldValue.class, new FieldValueDeserializer(mapper, predefinedFieldProvider));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ISO_DATE_TIME));

        mapper.registerModule(module);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return mapper;
    }
}
