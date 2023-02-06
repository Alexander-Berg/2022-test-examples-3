package ru.yandex.market.notification.service.common;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

import ru.yandex.market.notification.common.Registry;
import ru.yandex.market.notification.simple.service.common.RegistryImpl;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link RegistryImpl}.
 *
 * @author Vladislav Bauer
 */
public class RegistryImplTest {

    @Test
    public void testRegistry() {
        final Collection<String> elements = Arrays.asList("a", "b", "c");
        final Registry<String> registry = new RegistryImpl<>(elements);

        final Collection<String> registered = registry.getAll();
        assertThat(registered, containsInAnyOrder(elements.toArray(new String[elements.size()])));
    }

}
