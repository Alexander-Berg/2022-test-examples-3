package ru.yandex.market.jmf.entity.test.assertions;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.utils.Maps;

public class EntityAttributeMatcher<T extends Entity> extends BaseMatcher<T> {

    private final Object[] expectedKeyValuePairs;

    private EntityAssert<Entity> entityAssert;

    private final boolean detailed;

    public EntityAttributeMatcher(Object[] expectedKeyValuePairs, boolean detailed) {
        this.expectedKeyValuePairs = expectedKeyValuePairs;
        this.detailed = detailed;
    }

    public static <T extends Entity> EntityAttributeMatcher<T> havingAttributes(Object... keyValuePairs) {
        return new EntityAttributeMatcher<>(keyValuePairs, false);
    }

    public static <T extends Entity> EntityAttributeMatcher<T> havingAttributesDetailed(Object... keyValuePairs) {
        return new EntityAttributeMatcher<>(keyValuePairs, true);
    }

    @Override
    public boolean matches(Object item) {
        try {
            entityAssert = EntityAssert.assertThat((Entity) item, detailed);
            entityAssert.hasAttributes(expectedKeyValuePairs);
            return true;
        } catch (AssertionError e) {
            return false;
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(String.format("entity having attributes (%s)", Maps.of(expectedKeyValuePairs)));
    }

    @Override
    public void describeMismatch(Object item, Description description) {
        description.appendText(String.format("actually was (%s)", entityAssert.getMismatchValues()));

    }
}
