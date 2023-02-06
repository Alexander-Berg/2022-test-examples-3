package ru.yandex.market.mboc.common.masterdata.services.iris.proto;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;

public final class ProtobufRandomizer<E extends Message> implements Randomizer<E> {

    private static final int MIN_LIST_LENGTH = 1;
    private static final int MAX_LIST_LENGTH = 10;
    private static final int MAX_STRING_LENGTH = 10;

    private final ProtobufRandomizerRegistry registry;
    private final EnhancedRandom random;
    private final E instance;

    public ProtobufRandomizer(ProtobufRandomizerRegistry registry, EnhancedRandom random, E instance) {
        this.random = random;
        this.instance = instance;
        this.registry = registry;
    }

    @Override
    public E getRandomValue() {
        E.Builder builder = instance.newBuilderForType();
        for (FieldDescriptor field : instance.getDescriptorForType().getFields()) {
            builder.setField(field, getRandomValue(field));
        }
        //noinspection unchecked
        return (E) builder.build();
    }

    private Object getRandomValue(FieldDescriptor field) {
        if (field.isRepeated()) {
            ProtobufRandomizerRegistry.RepeatedFieldSize repeatedFieldSize =
                (ProtobufRandomizerRegistry.RepeatedFieldSize) registry
                    .getRandomizer(ProtobufRandomizerRegistry.RepeatedFieldSize.class)
                    .getRandomValue();

            List<Object> values = new ArrayList<>();
            for (int i = 0; i < repeatedFieldSize.getSize(); i++) {
                values.add(getRandomSingleValue(field));
            }
            return values;
        } else {
            return getRandomSingleValue(field);
        }
    }

    private Object getRandomSingleValue(FieldDescriptor field) {
        Randomizer<?> randomizer = registry.getRandomizer(field);
        if (randomizer != null) {
            return randomizer.getRandomValue();
        }
        switch (field.getJavaType()) {
            case BOOLEAN:
                return random.nextBoolean();

            case DOUBLE:
                return random.nextDouble();

            case FLOAT:
                return random.nextFloat();

            case INT:
                return random.nextInt();

            case LONG: {
                return random.nextLong();
            }
            case STRING:
                return random.nextObject(String.class);

            case BYTE_STRING:
                return registry.getRandomizer(ByteString.class).getRandomValue();

            case ENUM:
                List<EnumValueDescriptor> values = field.getEnumType().getValues();
                return values.get(random.nextInt(values.size()));

            case MESSAGE:
                return registry.getRandomizer(field.getMessageType()).getRandomValue();

            default:
                return null;
        }
    }

}
