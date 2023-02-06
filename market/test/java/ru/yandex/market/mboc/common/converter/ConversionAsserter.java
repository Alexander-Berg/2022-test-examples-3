package ru.yandex.market.mboc.common.converter;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.assertj.core.api.SoftAssertions;

/**
 * Тестируем конвертацию pojo в proto и обратно.
 *
 * @author masterj
 */
public class ConversionAsserter<Pojo, Proto extends Message> {
    private final Supplier<Proto> emptyProtoSupplier;
    private final Supplier<Pojo> emptyPojoSupplier;
    private final Supplier<Pojo> randomPojoSupplier;
    private final Function<Pojo, Proto> pojoToProtoF;
    private final Function<Proto, Pojo> protoToPojoF;

    public ConversionAsserter(
        Supplier<Proto> emptyProtoSupplier,
        Supplier<Pojo> emptyPojoSupplier,
        Supplier<Pojo> randomPojoSupplier,
        Function<Pojo, Proto> pojoToProtoF,
        Function<Proto, Pojo> protoToPojoF
    ) {
        this.emptyProtoSupplier = emptyProtoSupplier;
        this.emptyPojoSupplier = emptyPojoSupplier;
        this.randomPojoSupplier = randomPojoSupplier;
        this.pojoToProtoF = pojoToProtoF;
        this.protoToPojoF = protoToPojoF;
    }

    public static void assertThatAllProtoFieldsAreSet(Message message) {
        SoftAssertions.assertSoftly(softAssertions -> assertThatAllProtoFieldsAreSet(softAssertions, message));
    }

    private static void assertThatAllProtoFieldsAreSet(SoftAssertions softly, Message message) {
        Set<String> deprecated = getDeprecatedFields(message);
        List<String> instanceFields = message.getAllFields().keySet().stream()
            .map(Descriptors.FieldDescriptor::getFullName)
            .filter(f -> !deprecated.contains(f))
            .collect(Collectors.toList());
        List<String> classFields = message.getDescriptorForType().getFields().stream()
            .map(Descriptors.FieldDescriptor::getFullName)
            .filter(f -> !deprecated.contains(f))
            .collect(Collectors.toList());
        softly.assertThat(instanceFields)
            .containsExactlyInAnyOrderElementsOf(classFields);
    }

    private static Set<String> getDeprecatedFields(Message message) {
        return message.getDescriptorForType().getFields()
            .stream()
            .filter(f -> f.getOptions().getDeprecated())
            .map(Descriptors.FieldDescriptor::getFullName)
            .collect(Collectors.toSet());
    }

    public static void assertThatNoProtoFieldsAreSet(Message message) {
        SoftAssertions.assertSoftly(softly -> softly.assertThat(message.getAllFields()).isEmpty());
    }

    private static void assertThatNoProtoFieldsAreSet(SoftAssertions softly, Message message) {
        softly.assertThat(message.getAllFields()).isEmpty();
    }

    private void whenConvertsEmptyPojoToProtoShouldSetNoFields(SoftAssertions softly) {
        Pojo emptyPojo = emptyPojoSupplier.get();
        Proto proto = pojoToProtoF.apply(emptyPojo);
        assertThatNoProtoFieldsAreSet(softly, proto);
    }

    private void whenConvertsEmptyProtoToPojoShouldSetNoFields(SoftAssertions softly) {
        Proto emptyProto = emptyProtoSupplier.get();
        assertThatNoProtoFieldsAreSet(softly, emptyProto);
        Pojo pojo = protoToPojoF.apply(emptyProto);
        Proto proto = pojoToProtoF.apply(pojo);
        assertThatNoProtoFieldsAreSet(softly, proto);
    }

    private void whenConvertsRandomPojoToProtoShouldSetAllFields(SoftAssertions softly) {
        Pojo randomPojo = randomPojoSupplier.get();
        Proto proto = pojoToProtoF.apply(randomPojo);
        Set<String> deprecated = getDeprecatedFields(proto);
        List<String> instanceFields = proto.getAllFields().keySet().stream()
            .map(Descriptors.FieldDescriptor::getFullName)
            .filter(f -> !deprecated.contains(f))
            .collect(Collectors.toList());
        List<String> classFields = proto.getDescriptorForType().getFields().stream()
            .map(Descriptors.FieldDescriptor::getFullName)
            .filter(f -> !deprecated.contains(f))
            .collect(Collectors.toList());
        softly.assertThat(instanceFields)
            .as("pojo=%s\nproto=%s", randomPojo, proto)
            .containsExactlyInAnyOrderElementsOf(classFields);
    }

    private void whenConvertsRandomProtoToPojoShouldSetAllFields(SoftAssertions softly) {
        // start with a random proto
        Pojo randomPojo = randomPojoSupplier.get();
        Proto randomProto = pojoToProtoF.apply(randomPojo);

        /** convert it to proto hoping no fields are forgotten in {@link #protoToPojoF} */
        Pojo pojo = protoToPojoF.apply(randomProto);

        // now ensure the hope
        Proto proto = pojoToProtoF.apply(pojo);
        assertThatAllProtoFieldsAreSet(softly, proto);
    }

    public void doAssertions() {
        SoftAssertions.assertSoftly(softly -> {
            this.whenConvertsEmptyPojoToProtoShouldSetNoFields(softly);
            this.whenConvertsEmptyProtoToPojoShouldSetNoFields(softly);
            this.whenConvertsRandomPojoToProtoShouldSetAllFields(softly);
            this.whenConvertsRandomProtoToPojoShouldSetAllFields(softly);
        });
    }
}
