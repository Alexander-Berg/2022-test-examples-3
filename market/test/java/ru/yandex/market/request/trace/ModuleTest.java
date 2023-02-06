package ru.yandex.market.request.trace;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Тесты для {@link Module}
 */
public class ModuleTest {

    @Test
    public void testIdUniqueness() {
        Map<String, List<Module>> byId = Arrays.stream(Module.values())
                .collect(Collectors.groupingBy(Module::toString));

        Set<String> duplicates = byId
                .entrySet().stream().filter(e -> e.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        assertTrue(String.format("Found duplicated module ID(s): %s", duplicates), duplicates.isEmpty());
    }

}
