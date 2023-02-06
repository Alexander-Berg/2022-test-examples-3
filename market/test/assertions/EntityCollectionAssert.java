package ru.yandex.market.jmf.entity.test.assertions;

import java.util.Collection;

import org.assertj.core.api.FactoryBasedNavigableIterableAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.api.ObjectAssertFactory;
import org.hamcrest.Matcher;

import ru.yandex.market.jmf.entity.Entity;

public class EntityCollectionAssert<T extends Entity> extends FactoryBasedNavigableIterableAssert<EntityCollectionAssert<T>, Iterable<? extends T>, T, ObjectAssert<T>> {

    public EntityCollectionAssert(Collection<T> actual) {
        super(actual, EntityCollectionAssert.class, new ObjectAssertFactory<>());
    }

    public static <T extends Entity> EntityCollectionAssert<T> assertThat(Collection<T> actual) {
        return new EntityCollectionAssert<>(actual);
    }

    /**
     * Все сущности коллекции содержат все пары {@code expectedKeyValuePairs}
     * @param expectedKeyValuePairs пары (код атрибута -> значение)
     *                              где значением может быть как фактическое значение
     *                              так и {@link Matcher}
     */
    public EntityCollectionAssert<T> allHasAttributes(Object... expectedKeyValuePairs) {
        this.allSatisfy(entity -> EntityAssert.assertThat(entity).hasAttributes(expectedKeyValuePairs));
        return this;
    }


    /**
     * Хотя бы одна сущность коллекции содержит все пары {@code expectedKeyValuePairs}
     * @param expectedKeyValuePairs пары (код атрибута -> значение)
     *                              где значением может быть как фактическое значение
     *                              так и {@link Matcher}
     */
    public EntityCollectionAssert<T> anyHasAttributes(Object... expectedKeyValuePairs) {
        this.anySatisfy(entity -> EntityAssert.assertThat(entity).hasAttributes(expectedKeyValuePairs));
        return this;
    }

    /**
     * Ни одна из сущностей коллекции не содержит все пары {@code expectedKeyValuePairs}
     * @param expectedKeyValuePairs пары (код атрибута -> значение)
     *                              где значением может быть как фактическое значение
     *                              так и {@link Matcher}
     */
    public EntityCollectionAssert<T> noneHasAttributes(Object... expectedKeyValuePairs) {
        this.noneSatisfy(entity -> EntityAssert.assertThat(entity).hasAttributes(expectedKeyValuePairs));
        return this;
    }
}
