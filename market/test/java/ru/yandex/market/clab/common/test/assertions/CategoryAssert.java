package ru.yandex.market.clab.common.test.assertions;

import org.assertj.core.api.AbstractObjectAssert;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Category;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author anmalysh
 * @since 1/21/2019
 */
public class CategoryAssert extends AbstractObjectAssert<CategoryAssert, Category> {

    public CategoryAssert(Category actual) {
        super(actual, CategoryAssert.class);
    }

    public static CategoryAssert assertThatCategory(Category actual) {
        return new CategoryAssert(actual);
    }

    public void isValueEqualTo(Category other) {
        isNotNull();
        Category thisCopy = new Category(actual)
            .setId(null)
            .setModifiedDate(null);
        Category otherCopy = new Category(other)
            .setId(null)
            .setModifiedDate(null);
        assertThat(thisCopy).isEqualTo(otherCopy);
    }
}
