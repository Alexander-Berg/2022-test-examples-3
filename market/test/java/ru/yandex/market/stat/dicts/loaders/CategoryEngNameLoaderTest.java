package ru.yandex.market.stat.dicts.loaders;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * @author nettoyeur
 * @since 01.08.2017
 */
public class CategoryEngNameLoaderTest {
    @Test
    public void load() throws Exception {
        List<CategoriesLoader.CategoryEngName> engNames = CategoriesLoader.loadCategoriesEngNames();
        assertThat(engNames, hasSize(4380));
        assertThat(engNames, hasItem(
            CategoriesLoader.CategoryEngName.builder()
                .hyperId(12333825L)
                .name("Сварочное оборудование")
                .nameEn("Welding equipment")
                .context("Все товары/Дом и дача/Строительство и ремонт/Электрика/Сварочное оборудование")
                .build()
        ));
    }
}
