package ru.yandex.vendor.category;

import org.junit.Test;
import ru.yandex.vendor.util.SqlQuery;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ru.yandex.vendor.category.CategoryService.createGetCategoriesQuery;
import static ru.yandex.vendor.util.Utils.entry;
import static ru.yandex.vendor.util.Utils.map;

public class CategoryServiceTest {

    @Test
    public void get_categories_query_with_no_filters() {
        SqlQuery getCategoriesQuery = createGetCategoriesQuery(new CategoryFilter(), false);
        String expectedSql =
            "SELECT DISTINCT cat.hyper_id id, cat.path path, cat.output_type, cat.grouped, mecp.can_create_sku, " +
                    "cat.accept_partner_models, cat.accept_partner_skus, cat.accept_good_content " +
            "FROM vendors.v_market_category_filtered cat " +
            "LEFT JOIN VENDORS.MODEL_EDITOR_CAT_PARAMS mecp ON cat.HYPER_ID = mecp.CATEGORY_ID " +
            "ORDER BY cat.hyper_id";
        assertEqualsWithSpacesCompressed(expectedSql, getCategoriesQuery.getQuery());
        assertEquals(emptyMap(), getCategoriesQuery.getParameters());
    }

    @Test
    public void get_categories_query_with_all_categories_vendor_without_id_changes_nothing() {
        SqlQuery getCategoriesQuery = createGetCategoriesQuery(new CategoryFilter(), true);
        String expectedSql =
            "SELECT DISTINCT cat.hyper_id id, cat.path path, cat.output_type, cat.grouped, mecp.can_create_sku, " +
                    "cat.accept_partner_models, cat.accept_partner_skus, cat.accept_good_content " +
            "FROM vendors.v_market_category_filtered cat " +
            "LEFT JOIN VENDORS.MODEL_EDITOR_CAT_PARAMS mecp ON cat.HYPER_ID = mecp.CATEGORY_ID " +
            "ORDER BY cat.hyper_id";
        assertEqualsWithSpacesCompressed(expectedSql, getCategoriesQuery.getQuery());
        assertEquals(emptyMap(), getCategoriesQuery.getParameters());
    }

    @Test
    public void get_categories_query_with_text_filter() {
        CategoryFilter filter = new CategoryFilter();
        filter.setTextForSearch("qwe");

        SqlQuery getCategoriesQuery = createGetCategoriesQuery(filter, false);
        String expectedSql =
            "SELECT DISTINCT cat.hyper_id id, cat.path path, cat.output_type, cat.grouped, mecp.can_create_sku, " +
                    "cat.accept_partner_models, cat.accept_partner_skus, cat.accept_good_content " +
            "FROM vendors.v_market_category_filtered cat " +
            "LEFT JOIN VENDORS.MODEL_EDITOR_CAT_PARAMS mecp ON cat.HYPER_ID = mecp.CATEGORY_ID " +
            "WHERE (lower(cat.category) LIKE lower(:textForSearch)) " +
            "ORDER BY cat.hyper_id";
        assertEqualsWithSpacesCompressed(expectedSql, getCategoriesQuery.getQuery());
        assertEquals(singletonMap("textForSearch", "%qwe%"), getCategoriesQuery.getParameters());
    }

    @Test
    public void get_categories_query_with_vendor_filter() {
        CategoryFilter filter = new CategoryFilter();
        filter.setVendorId(42L);

        SqlQuery getCategoriesQuery = createGetCategoriesQuery(filter, false);
        String expectedSql =
            "SELECT DISTINCT cat.hyper_id id, cat.path path, cat.output_type, cat.grouped, mecp.can_create_sku, " +
                    "cat.accept_partner_models, cat.accept_partner_skus, cat.accept_good_content , bcat.popularity " +
            "FROM vendors.v_market_category_filtered cat " +
            "LEFT JOIN VENDORS.MODEL_EDITOR_CAT_PARAMS mecp ON cat.HYPER_ID = mecp.CATEGORY_ID " +
            "JOIN vendors.v_vendor_categories vcat " +
            "  ON cat.path LIKE vcat.category_path||'%' " +
            " AND vcat.vendor_id = :vendorId " +
            "LEFT JOIN vendors.market_brand_category bcat " +
            "  ON bcat.category_id = cat.hyper_id " +
            " AND bcat.brand_id = ( " +
            "    SELECT brand_id FROM vendors.vendor WHERE id = :vendorId " +
            " ) " +
            "ORDER BY bcat.popularity DESC NULLS LAST, cat.hyper_id";
        assertEqualsWithSpacesCompressed(expectedSql, getCategoriesQuery.getQuery());
        assertEquals(singletonMap("vendorId", 42L), getCategoriesQuery.getParameters());
    }

    @Test
    public void get_categories_query_with_all_categories_vendor_filter() {
        CategoryFilter filter = new CategoryFilter();
        filter.setVendorId(42L);

        SqlQuery getCategoriesQuery = createGetCategoriesQuery(filter, true);
        String expectedSql =
            "SELECT DISTINCT cat.hyper_id id, cat.path path, cat.output_type, cat.grouped, mecp.can_create_sku, " +
                    "cat.accept_partner_models, cat.accept_partner_skus, cat.accept_good_content , bcat.popularity " +
            "FROM vendors.v_market_category_filtered cat " +
            "LEFT JOIN VENDORS.MODEL_EDITOR_CAT_PARAMS mecp ON cat.HYPER_ID = mecp.CATEGORY_ID " +
            "LEFT JOIN vendors.market_brand_category bcat " +
            "  ON bcat.category_id = cat.hyper_id " +
            " AND bcat.brand_id = ( " +
            "    SELECT brand_id FROM vendors.vendor WHERE id = :vendorId " +
            " ) " +
            "ORDER BY bcat.popularity DESC NULLS LAST, cat.hyper_id";
        assertEqualsWithSpacesCompressed(expectedSql, getCategoriesQuery.getQuery());
        assertEquals(singletonMap("vendorId", 42L), getCategoriesQuery.getParameters());
    }

    @Test
    public void get_categories_query_with_leaf_type_filter() {
        CategoryFilter filter = new CategoryFilter();
        filter.setLeaf(true);

        SqlQuery getCategoriesQuery = createGetCategoriesQuery(filter, false);
        String expectedSql =
                "SELECT DISTINCT cat.hyper_id id, cat.path path, cat.output_type, cat.grouped, mecp.can_create_sku, " +
                        "cat.accept_partner_models, cat.accept_partner_skus, cat.accept_good_content " +
                "FROM vendors.v_market_category_filtered cat " +
                "LEFT JOIN VENDORS.MODEL_EDITOR_CAT_PARAMS mecp ON cat.HYPER_ID = mecp.CATEGORY_ID " +
                "WHERE (cat.is_leaf = :leaf) " +
                "ORDER BY cat.hyper_id";
        assertEqualsWithSpacesCompressed(expectedSql, getCategoriesQuery.getQuery());
        assertEquals(singletonMap("leaf", true), getCategoriesQuery.getParameters());
    }

    @Test
    public void get_categories_query_with_group_type_filter() {
        CategoryFilter filter = new CategoryFilter();
        filter.setLeaf(false);

        SqlQuery getCategoriesQuery = createGetCategoriesQuery(filter, false);
        String expectedSql =
                "SELECT DISTINCT cat.hyper_id id, cat.path path, cat.output_type, cat.grouped, mecp.can_create_sku, " +
                        "cat.accept_partner_models, cat.accept_partner_skus, cat.accept_good_content " +
                "FROM vendors.v_market_category_filtered cat " +
                "LEFT JOIN VENDORS.MODEL_EDITOR_CAT_PARAMS mecp ON cat.HYPER_ID = mecp.CATEGORY_ID " +
                "WHERE (cat.is_leaf = :leaf) " +
                "ORDER BY cat.hyper_id";
        assertEqualsWithSpacesCompressed(expectedSql, getCategoriesQuery.getQuery());
        assertEquals(singletonMap("leaf", false), getCategoriesQuery.getParameters());
    }

    @Test
    public void get_categories_query_with_vendor_and_text_and_leaf_filter() {
        CategoryFilter filter = new CategoryFilter();
        filter.setVendorId(42L);
        filter.setTextForSearch("rty");
        filter.setLeaf(true);

        SqlQuery getCategoriesQuery = createGetCategoriesQuery(filter, false);
        String expectedSql =
            "SELECT DISTINCT cat.hyper_id id, cat.path path, cat.output_type, cat.grouped, mecp.can_create_sku, " +
                    "cat.accept_partner_models, cat.accept_partner_skus, cat.accept_good_content , bcat.popularity " +
            "FROM vendors.v_market_category_filtered cat " +
            "LEFT JOIN VENDORS.MODEL_EDITOR_CAT_PARAMS mecp ON cat.HYPER_ID = mecp.CATEGORY_ID " +
            "JOIN vendors.v_vendor_categories vcat " +
            "  ON cat.path LIKE vcat.category_path||'%' " +
            " AND vcat.vendor_id = :vendorId " +
            "LEFT JOIN vendors.market_brand_category bcat " +
            "  ON bcat.category_id = cat.hyper_id " +
            " AND bcat.brand_id = ( " +
            "    SELECT brand_id FROM vendors.vendor WHERE id = :vendorId " +
            " ) " +
            "WHERE ((cat.is_leaf = :leaf) AND (lower(cat.category) LIKE lower(:textForSearch))) " +
            "ORDER BY bcat.popularity DESC NULLS LAST, cat.hyper_id";
        assertEqualsWithSpacesCompressed(expectedSql, getCategoriesQuery.getQuery());
        assertEquals(
            map(entry("vendorId", 42L), entry("textForSearch", "%rty%"), entry("leaf", true)),
            getCategoriesQuery.getParameters());
    }

    @Test
    public void get_categories_query_with_leaf_type_and_accept_partner_models_filter() {
        CategoryFilter filter = new CategoryFilter();
        filter.setLeaf(false);
        filter.setAcceptPartnerModels(true);

        SqlQuery getCategoriesQuery = createGetCategoriesQuery(filter, false);
        String expectedSql =
                "SELECT DISTINCT cat.hyper_id id, cat.path path, cat.output_type, cat.grouped, mecp.can_create_sku, " +
                        "cat.accept_partner_models, cat.accept_partner_skus, cat.accept_good_content " +
                        "FROM vendors.v_market_category_filtered cat " +
                        "LEFT JOIN VENDORS.MODEL_EDITOR_CAT_PARAMS mecp ON cat.HYPER_ID = mecp.CATEGORY_ID " +
                        "WHERE ((cat.is_leaf = :leaf) AND (cat.accept_partner_models = :acceptPartnerModels)) " +
                        "ORDER BY cat.hyper_id";
        assertEqualsWithSpacesCompressed(expectedSql, getCategoriesQuery.getQuery());
        assertEquals(false, getCategoriesQuery.getParameters().get("leaf"));
        assertEquals(true, getCategoriesQuery.getParameters().get("acceptPartnerModels"));
    }

    @Test
    public void get_categories_query_with_leaf_type_and_accept_partner_skus_filter() {
        CategoryFilter filter = new CategoryFilter();
        filter.setLeaf(true);
        filter.setAcceptPartnerSkus(false);

        SqlQuery getCategoriesQuery = createGetCategoriesQuery(filter, false);
        String expectedSql =
                "SELECT DISTINCT cat.hyper_id id, cat.path path, cat.output_type, cat.grouped, mecp.can_create_sku, " +
                        "cat.accept_partner_models, cat.accept_partner_skus, cat.accept_good_content " +
                        "FROM vendors.v_market_category_filtered cat " +
                        "LEFT JOIN VENDORS.MODEL_EDITOR_CAT_PARAMS mecp ON cat.HYPER_ID = mecp.CATEGORY_ID " +
                        "WHERE ((cat.is_leaf = :leaf) AND (cat.accept_partner_skus = :acceptPartnerSkus)) " +
                        "ORDER BY cat.hyper_id";
        assertEqualsWithSpacesCompressed(expectedSql, getCategoriesQuery.getQuery());
        assertEquals(true, getCategoriesQuery.getParameters().get("leaf"));
        assertEquals(false, getCategoriesQuery.getParameters().get("acceptPartnerSkus"));
    }

    @Test
    public void get_categories_query_with_accept_good_content_filter() {
        CategoryFilter filter = new CategoryFilter();
        filter.setAcceptGoodContent(true);

        SqlQuery getCategoriesQuery = createGetCategoriesQuery(filter, false);
        String expectedSql =
                "SELECT DISTINCT cat.hyper_id id, cat.path path, cat.output_type, cat.grouped, mecp.can_create_sku," +
                        " cat.accept_partner_models, cat.accept_partner_skus, cat.accept_good_content " +
                        "FROM vendors.v_market_category_filtered cat " +
                        "LEFT JOIN VENDORS.MODEL_EDITOR_CAT_PARAMS mecp ON cat.HYPER_ID = mecp.CATEGORY_ID " +
                        "WHERE (cat.accept_good_content = :acceptGoodContent) " +
                        "ORDER BY cat.hyper_id";
        assertEqualsWithSpacesCompressed(expectedSql, getCategoriesQuery.getQuery());
        assertEquals(true, getCategoriesQuery.getParameters().get("acceptGoodContent"));
    }

    @Test
    public void get_categories_query_with_not_accept_good_content() {
        CategoryFilter filter = new CategoryFilter();
        filter.setAcceptGoodContent(false);

        SqlQuery getCategoriesQuery = createGetCategoriesQuery(filter, false);
        String expectedSql =
                "SELECT DISTINCT cat.hyper_id id, cat.path path, cat.output_type, cat.grouped, mecp.can_create_sku," +
                        " cat.accept_partner_models, cat.accept_partner_skus, cat.accept_good_content " +
                        "FROM vendors.v_market_category_filtered cat " +
                        "LEFT JOIN VENDORS.MODEL_EDITOR_CAT_PARAMS mecp ON cat.HYPER_ID = mecp.CATEGORY_ID " +
                        "ORDER BY cat.hyper_id";
        assertEqualsWithSpacesCompressed(expectedSql, getCategoriesQuery.getQuery());
        assertNull(getCategoriesQuery.getParameters().get("acceptGoodContent"));
    }

    void assertEqualsWithSpacesCompressed(String expected, String actual) {
        expected = expected.replaceAll("\\s+", " ").trim();
        actual = actual.replaceAll("\\s+", " ").trim();
        assertEquals(expected, actual);
    }
}
