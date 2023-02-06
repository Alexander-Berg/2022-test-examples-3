package ru.yandex.market.marketpromo.core.test.utils;


import java.util.List;

import javax.annotation.Nonnull;

import ru.yandex.market.marketpromo.model.Category;

public final class CategoriesTestHelper {

    private CategoriesTestHelper() {
    }

    @Nonnull
    public static List<Category> defaultCategoryList() {
        return List.of(
                new Category(90401L, 0L, true, true, "Все товары",
                        "\tВсе товары\t"),
                new Category(90402L, 90401L, true, true, "Товары для авто- и мототехники",
                        "\tВсе товары\tТовары для авто- и мототехники\t"),
                new Category(90403L, 90402L, true, true, "Автомобильная аудио- и видеотехника",
                        "\tВсе товары\tТовары для авто- и мототехники\tАвтомобильная аудио- и видеотехника\t"),
                new Category(90404L, 90403L, false, true, "Автомагнитолы",
                        "\\tВсе товары\\tТовары для авто- и мототехники\\tАвтомобильная аудио- и " +
                                "видеотехника\\tАвтомагнитолы\\t"),
                new Category(90407L, 90403L, false, true, "Автомобильные антенны",
                        "\\tВсе товары\\tТовары для авто- и мототехники\\tАвтомобильная аудио- и " +
                                "видеотехника\\tАвтомобильные антенны\\t")
        );
    }
}
