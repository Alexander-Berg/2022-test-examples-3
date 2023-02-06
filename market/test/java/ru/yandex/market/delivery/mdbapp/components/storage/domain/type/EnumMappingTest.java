package ru.yandex.market.delivery.mdbapp.components.storage.domain.type;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
@RequiredArgsConstructor
public class EnumMappingTest {

    private final Class<? extends Enum<?>> enum1;
    private final Class<? extends Enum<?>> enum2;

    @Parameters
    public static Collection<Object[]> testParameters() {
        return List.of(
            new Object[]{
                PossibleOrderChangesMethod.class,
                ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeMethod.class
            },
            new Object[]{
                PossibleOrderChangesType.class,
                ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeType.class
            }
        );
    }

    @Test
    public void testEnumsMapping() {
        assertEquals(
            toStringSet(enum1),
            toStringSet(enum2)
        );
    }

    private static Set<String> toStringSet(Class<? extends Enum<?>> enum1) {
        return Stream.of(enum1.getEnumConstants())
            .map(Enum::name)
            .collect(Collectors.toSet());
    }
}
