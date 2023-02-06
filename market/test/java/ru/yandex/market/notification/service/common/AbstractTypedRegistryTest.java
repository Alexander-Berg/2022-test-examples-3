package ru.yandex.market.notification.service.common;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import ru.yandex.market.notification.common.TypedRegistry;
import ru.yandex.market.notification.exception.RegistryElementNotFoundException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Базовый класс для unit-тестов реестров.
 *
 * @author Vladislav Bauer
 */
public abstract class AbstractTypedRegistryTest<T extends TypedRegistry<String, String>> {

    @Test
    public void testFind() {
        final T typedRegistry = createRegistry();

        assertThat(typedRegistry.find("a").isPresent(), equalTo(true));
        assertThat(typedRegistry.find("b").isPresent(), equalTo(true));
        assertThat(typedRegistry.find("c").isPresent(), equalTo(true));
        assertThat(typedRegistry.find("d").isPresent(), equalTo(false));
    }

    @Test
    public void testGetPositive() {
        final T typedRegistry = createRegistry();

        assertThat(typedRegistry.get("a"), equalTo("1"));
        assertThat(typedRegistry.get("b"), equalTo("2"));
        assertThat(typedRegistry.get("c"), equalTo("3"));
    }

    @Test(expected = RegistryElementNotFoundException.class)
    public void testGetNegative() {
        final T typedRegistry = createRegistry();

        fail(typedRegistry.get("d"));
    }


    protected abstract T createRegistry();

    protected final Map<String, String> createData() {
        final Map<String, String> data = new HashMap<>();
        data.put("a", "1");
        data.put("b", "2");
        data.put("c", "3");
        return data;
    }

}
