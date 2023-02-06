package ru.yandex.market.fintech.banksint.yt;

import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fintech.banksint.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

public class CategoryTreeCacheTest extends FunctionalTest {
    @Autowired
    private CategoryTreeCache categoryTreeCache;

    @Test
    public void testCategoryTreeCache() {
        var categoryTree = categoryTreeCache.get();
        assertThat(categoryTree).isNotNull();
        assertThat(categoryTree.getCategoryMap()).hasSizeGreaterThan(1);
        var rootList = categoryTree.getCategoryMap().values()
                .stream()
                .filter(category -> category.getCategoryPath().isEmpty())
                .collect(Collectors.toList());
        assertThat(rootList).hasSize(1);
        assertThat(rootList.get(0).getCategoryId()).isEqualTo(90401L);
    }
}
