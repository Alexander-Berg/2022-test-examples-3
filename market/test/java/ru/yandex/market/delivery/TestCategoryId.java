package ru.yandex.market.delivery;

import javax.annotation.Nullable;

/**
 * @author sergey-fed
 */
public final class TestCategoryId {
    private final @Nullable
    String categoryId;
    private final long feedId;

    TestCategoryId(@Nullable String categoryId, long feedId) {
        this.categoryId = categoryId;
        this.feedId = feedId;
    }

    public @Nullable
    String getCategoryId() {
        return categoryId;
    }

    public long getFeedId() {
        return feedId;
    }
}
