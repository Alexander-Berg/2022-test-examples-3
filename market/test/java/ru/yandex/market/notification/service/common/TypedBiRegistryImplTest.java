package ru.yandex.market.notification.service.common;

import org.junit.Test;

import ru.yandex.market.notification.common.TypedBiRegistry;
import ru.yandex.market.notification.exception.RegistryElementNotFoundException;
import ru.yandex.market.notification.simple.service.common.TypedBiRegistryImpl;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit-тесты для {@link TypedBiRegistryImpl}.
 *
 * @author Vladislav Bauer
 */
public class TypedBiRegistryImplTest extends AbstractTypedRegistryTest<TypedBiRegistry<String, String>> {

    @Test
    public void testFindBackward() {
        final TypedBiRegistry<String, String> registry = createRegistry();

        assertThat(registry.findBackward("1").isPresent(), equalTo(true));
        assertThat(registry.findBackward("2").isPresent(), equalTo(true));
        assertThat(registry.findBackward("3").isPresent(), equalTo(true));
        assertThat(registry.findBackward("4").isPresent(), equalTo(false));
    }

    @Test
    public void testGetBackwardPositive() {
        final TypedBiRegistry<String, String> registry = createRegistry();

        assertThat(registry.getBackward("1"), equalTo("a"));
        assertThat(registry.getBackward("2"), equalTo("b"));
        assertThat(registry.getBackward("3"), equalTo("c"));
    }

    @Test(expected = RegistryElementNotFoundException.class)
    public void testGetBackwardNegative() {
        final TypedBiRegistry<String, String> registry = createRegistry();

        fail(registry.getBackward("4"));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected TypedBiRegistry<String, String> createRegistry() {
        return new TypedBiRegistryImpl<>(createData());
    }

}
