package ru.yandex.market.core.category.matcher;

import org.hamcrest.Matcher;

import ru.yandex.market.core.category.model.Category;
import ru.yandex.market.mbi.util.MbiMatchers;

public final class CategoryMatcher {

    private CategoryMatcher() {
        throw new UnsupportedOperationException("Could not initiate util class");
    }

    public static Matcher<Category> hasCategory(String expectedValue) {
        return MbiMatchers.<Category>newAllOfBuilder()
                .add(Category::getCategory, expectedValue, "category")
                .build();
    }

    public static Matcher<Category> hasHyperId(Long expectedValue) {
        return MbiMatchers.<Category>newAllOfBuilder()
                .add(Category::getHyperId, expectedValue, "hyperId")
                .build();
    }

    public static Matcher<Category> hasParentId(Long expectedValue) {
        return MbiMatchers.<Category>newAllOfBuilder()
                .add(Category::getParentId, expectedValue, "parentId")
                .build();
    }
}
