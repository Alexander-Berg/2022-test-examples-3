package ru.yandex.market.billing.pp.storage;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * Тесты для {@link PpTreeTest}.
 *
 * @author vbudnev
 */
class PpTreeTest {

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
            .create();

    /**
     * Проверяем, что на основе входной мапы, корректно строятся деревья с ПП.
     */
    @Test
    void test_appendPpAtPath() throws JSONException, IOException {
        String actualJsonStr = gson.toJson(prepRootJson().getTreeObject());
        String expectedStr = IOUtils.toString(
                this.getClass().getClassLoader().getResourceAsStream("pp_tree.json"),
                Charset.forName("UTF-8")
        );

        JSONAssert.assertEquals(expectedStr, actualJsonStr, JSONCompareMode.STRICT);
    }

    /**
     * Ошибка, если при получении ПП задан несуществующий путь.
     */
    @Test
    void test_getPpForPath_when_invalidPath_then_should_throw() {
        PpHierarchyTree ppTree = prepRootJson();
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> ppTree.getPpByPath("desktop/pictures/_no_such_edge_"));

        Assertions.assertEquals(
                "Specified path \"desktop/pictures/_no_such_edge_\" not found on tree. " +
                        "Failed edge name=\"_no_such_edge_\"",
                ex.getMessage()
        );
    }

    /**
     * Получение ПП для поддрева заданного узла.
     */
    @Test
    void test_getPpForPath() {
        PpHierarchyTree ppTree = prepRootJson();
        Collection<Integer> actual = ppTree.getPpByPath("touch/market/page/cart/");
        assertThat(actual, containsInAnyOrder(275, 999, 295, 293, 245));
    }

    /**
     * Проверяем построение всех путей в древе.
     */
    @Test
    void test_collectAllNestedNodePath() {
        PpHierarchyTree ppTree = prepRootJson();
        Collection<String> actualPaths = ppTree.getAllPaths();

        List<String> expectedPathList = ImmutableList.<String>builder()
                .add("desktop")
                .add("desktop/pictures")
                .add("desktop/pictures/default")
                .add("desktop/search")
                .add("desktop/search/incut")
                .add("desktop/search/incut/like_card")
                .add("mobile_hybrid_android")
                .add("mobile_hybrid_android/market")
                .add("mobile_hybrid_android/market/card")
                .add("mobile_hybrid_android/market/card/model")
                .add("mobile_hybrid_android/market/card/model/default_offer")
                .add("mobile_hybrid_android/market/card/model/default_offer/default")
                .add("old_mobile")
                .add("old_mobile/market")
                .add("old_mobile/market/card")
                .add("old_mobile/market/card/model")
                .add("old_mobile/market/card/model/map")
                .add("old_mobile/market/card/model/map/old")
                .add("old_mobile/market/card/model/prices")
                .add("old_mobile/market/card/model/prices/old")
                .add("old_mobile/market/search")
                .add("old_mobile/market/search/main")
                .add("old_mobile/market/search/main/old")
                .add("touch")
                .add("touch/market")
                .add("touch/market/main_page")
                .add("touch/market/main_page/recommendations")
                .add("touch/market/main_page/recommendations/default")
                .add("touch/market/page")
                .add("touch/market/page/brand")
                .add("touch/market/page/brand/default")
                .add("touch/market/page/buy_list")
                .add("touch/market/page/buy_list/default")
                .add("touch/market/page/cart")
                .add("touch/market/page/cart/accessories")
                .add("touch/market/page/cart/accessories/default")
                .add("touch/market/page/cart/recommendations")
                .add("touch/market/page/cart/recommendations/in_same_shop")
                .add("touch/market/page/cart/recommendations/same_goods")
                .add("touch/market/page/cart/recommendations/same_goods/some_nested_tage")
                .add("touch/market/page/recommendations")
                .add("touch/market/page/recommendations/global_department")
                .add("touch/market/page/recommendations/landing")
                .add("touch/market/page/shop")
                .add("touch/market/page/shop/main")
                .add("touch/market/page/shop/main/default")
                .add("touch/market/search")
                .add("touch/market/search/main")
                .add("touch/market/search/main/default")
                .add("touch/market/search/main/sort")
                .add("touch/market/search/map")
                .add("touch/market/search/map/default")
                .add("touch/market/search/recommendations")
                .add("touch/market/search/recommendations/default")
                .build();

        assertThat(actualPaths, contains(expectedPathList.toArray()));
    }

    private PpHierarchyTree prepRootJson() {
        Map<Integer, String> data = prepareTestData();
        PpHierarchyTree ppTree = new PpHierarchyTree();
        for (Map.Entry<Integer, String> pathEntry : data.entrySet()) {
            ppTree.appendPpAtPath(pathEntry.getKey(), pathEntry.getValue());
        }
        return ppTree;
    }

    private Map<Integer, String> prepareTestData() {
        return ImmutableMap.<Integer, String>builder()
                .put(41, "desktop/pictures/default")
                .put(404, "desktop/search/incut/like_card")
                .put(730, "mobile_hybrid_android/market/card/model/default_offer/default")
                .put(39, "old_mobile/market/card/model/map/old")
                .put(37, "old_mobile/market/card/model/prices/old")
                .put(38, "old_mobile/market/search/main/old")
                .put(279, "touch/market/main_page/recommendations/default")
                .put(660, "touch/market/page/brand/default")
                .put(157, "touch/market/page/buy_list/default")
                .put(245, "touch/market/page/cart/accessories/default")
                .put(275, "touch/market/page/cart/recommendations/in_same_shop")
                .put(295, "touch/market/page/cart/recommendations/same_goods")
                .put(293, "touch/market/page/cart/recommendations/same_goods/some_nested_tage")
                .put(999, "touch/market/page/cart")
                .put(284, "touch/market/page/recommendations/global_department")
                .put(280, "touch/market/page/recommendations/landing")
                .put(622, "touch/market/page/shop/main/default")
                .put(48, "touch/market/search/main/default")
                .put(628, "touch/market/search/main/sort")
                .put(625, "touch/market/search/map/default")
                .put(270, "touch/market/search/recommendations/default")
                .build();
    }
}
