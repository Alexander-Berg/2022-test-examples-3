package ru.yandex.mail.diffusion.generator;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.mail.diffusion.IncrementalObject;
import ru.yandex.mail.diffusion.IncrementalObject.NonIncremental;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MetaInfoCollectorTest {
    private static List<Class> parameterizedWith(Class... classes) {
        return List.of(classes);
    }

    private static List<Class> isNotGeneric() {
        return List.of();
    }

    private static FieldInfo field(String name, String getterName, Class type, List<Class> genericElements) {
        return new FieldInfo(name, getterName, type, genericElements);
    }

    @Test
    @DisplayName("Confirm MetaInfoCollector collects class meta information correctly")
    void testMetaInfoCollection() {
        val collector = new MetaInfoCollector();
        val metaInfo = collector.collect(IncrementalObject.class);

        assertThat(metaInfo.getType()).isSameAs(IncrementalObject.class);
        assertThat(metaInfo.getFields())
            .containsExactly(
                field("bool", "isBool", boolean.class, isNotGeneric()),
                field("string", "getString", String.class, isNotGeneric()),
                field("integer", "getInteger", int.class, isNotGeneric()),
                field("boxedLong", "getBoxedLong", Long.class, isNotGeneric()),
                field("set", "getSet", Set.class, parameterizedWith(String.class)),
                field("byteSet", "getByteSet", Set.class, parameterizedWith(Byte.class)),
                field("pojo", "getPojo", NonIncremental.class, isNotGeneric()),
                field("optionalLong", "getOptionalLong", OptionalLong.class, isNotGeneric()),
                field("optionalShort", "getOptionalShort", Optional.class, parameterizedWith(Short.class)),
                field("enumeration", "getEnumeration", IncrementalObject.Enumeration.class, isNotGeneric())
            );
    }
}
