package ru.yandex.market.crm.campaign.http.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

import javax.inject.Inject;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.core.domain.categories.Category;
import ru.yandex.market.crm.core.domain.categories.CategoryCacheData;
import ru.yandex.market.crm.core.services.categories.CategoryImageLinkDAO;
import ru.yandex.market.crm.core.test.TestingCategorySupplier;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author apershukov
 */
@Disabled
public class CategoryControllerTest extends AbstractControllerMediumTest {

    @Inject
    private TestingCategorySupplier categorySupplier;
    @Inject
    private CategoryImageLinkDAO categoryImageLinkDAO;

    private static void category(int hid, int parent, String name, Int2ObjectMap<Category> categories) {
        categories.put(hid, new Category(hid, name, name, parent));
    }

    @BeforeEach
    public void setUp() {
        Int2ObjectMap<Category> categories = new Int2ObjectOpenHashMap<>();

        category(1, 0, "Электроника", categories);
        category(11, 1, "Мобильные телефоны", categories);
        category(111, 11, "Смартфоны", categories);
        category(12, 1, "Телевизоры", categories);
        category(13, 1, "Бытовая техника", categories);
        category(131, 13, "Электронные часы", categories);

        category(2, 0, "Компьютеры", categories);
        category(21, 2, "Ноутбуки", categories);
        category(22, 2, "Планшеты", categories);
        category(23, 2, "Мониторы", categories);

        categorySupplier.setData(new CategoryCacheData(categories));

        prepareImages(11, 5);
        prepareImages(111, 2);
        prepareImages(131, 1);
    }

    @Test
    public void testGetTreeWithoutFiltration() throws Exception {
        testGetTree(null, "full-category-tree.json");
    }

    @Test
    public void testFilterSingleRootNode() throws Exception {
        testGetTree("электро", "root-node-found.json");
    }

    @Test
    public void testFilterLeafNode() throws Exception {
        testGetTree("смарт", "single-leaf-node-found.json");
    }

    @Test
    public void testFilterIsNotCaseSensitive() throws Exception {
        testGetTree("СМАРТ", "single-leaf-node-found.json");
    }

    @Test
    public void testSearchByHid() throws Exception {
        testGetTree("111", "single-leaf-node-found.json");
    }

    @Test
    public void testGetCategory() throws Exception {
        String response = mockMvc.perform(get("/api/categories/{hid}", 11))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JSONAssert.assertEquals(getResource("requested-category.json"), response, true);
    }

    @Test
    public void testGetNotExistingCategory() throws Exception {
        mockMvc.perform(get("/api/categories/{hid}", 999))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    private void testGetTree(String namePart, String expectedResponsePath) throws Exception {
        String response = mockMvc.perform(get("/api/categories/tree")
                .param("name_part", namePart))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JSONAssert.assertEquals(getResource(expectedResponsePath), response, true);
    }

    private String getResource(String path) throws IOException {
        return IOUtils.toString(
                getClass().getResourceAsStream(path),
                StandardCharsets.UTF_8
        );
    }

    private void prepareImages(int hid, int count) {
        for (int i = 0; i < count; ++i) {
            categoryImageLinkDAO.createOrUpdateCategoryImageLink(
                    hid,
                    "http://image/" + UUID.randomUUID().toString(),
                    "image alt",
                    Collections.singleton("S")
            );
        }
    }
}
