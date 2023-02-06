package ru.yandex.market.mbi.tariffs.matcher;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.Matcher;

import ru.yandex.market.mbi.tariffs.model.Category;
import ru.yandex.market.mbi.util.MbiMatchers;

import static org.hamcrest.Matchers.allOf;

/**
 * Матчер для {@link ru.yandex.market.mbi.tariffs.model.Category}
 */
@ParametersAreNonnullByDefault
public class CategoryMatcher {

    public static Matcher<Category> hasAllFields(long hyperId, @Nullable Long parentId, String name) {
        return allOf(
                hasHyperId(hyperId),
                hasParentId(parentId),
                hasName(name)
        );
    }

    public static Matcher<Category> hasHyperId(long expectedValue) {
        return MbiMatchers.<Category>newAllOfBuilder()
            .add(Category::getHyperId, expectedValue, "hyperId")
            .build();
    }

    public static Matcher<Category> hasParentId(@Nullable Long expectedValue) {
        return MbiMatchers.<Category>newAllOfBuilder()
            .add(Category::getParentId, expectedValue, "parentId")
            .build();
    }

    public static Matcher<Category> hasName(String expectedValue) {
        return MbiMatchers.<Category>newAllOfBuilder()
            .add(Category::getName, expectedValue, "name")
            .build();
    }
}
