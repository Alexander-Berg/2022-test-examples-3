package ru.yandex.market.vendor.controllers;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

class RegionControllerTest extends AbstractVendorPartnerFunctionalTest {

    private static final String INSERT_REGION = "" +
            "INSERT INTO MARKET_VENDORS.REGIONS_LR(ID,RU_NAME,TYPE,PARENT_ID,PARENT_RU_NAME,PARENTS,CHILDREN," +
            "PATH_RU_NAME)" +
            "VALUES (:id, :ruName, :type, :parentId, :parentRuName, :parents, :children, :pathRuName)";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    RegionControllerTest(NamedParameterJdbcTemplate marketVendorsClickHouseNamedJdbcTemplate) {
        this.namedParameterJdbcTemplate = marketVendorsClickHouseNamedJdbcTemplate;
    }

    @BeforeEach
    void insetRegions() {
        SqlParameterSource[] rows = {
                new MapSqlParameterSource()
                        .addValue("id", 225)
                        .addValue("ruName", "Россия")
                        .addValue("type", 3)
                        .addValue("parentId", 10001)
                        .addValue("parentRuName", "Евразия")
                        .addValue("parents", new Integer[]{10000, 10001})
                        .addValue("children", new Integer[]{3, 17, 26, 40, 52, 59, 73, 381, 382, 102444})
                        .addValue("pathRuName", new String[]{"Россия", "Евразия", "Земля"}),
                new MapSqlParameterSource()
                        .addValue("id", 149)
                        .addValue("ruName", "Беларусь")
                        .addValue("type", 3)
                        .addValue("parentId", 166)
                        .addValue("parentRuName", "СНГ")
                        .addValue("parents", new Integer[]{10000, 10001, 166})
                        .addValue("children", new Integer[]{})
                        .addValue("pathRuName", new String[]{"Беларусь", "Евразия", "Земля"}),
                new MapSqlParameterSource()
                        .addValue("id", 3)
                        .addValue("ruName", "Центральный федеральный округ")
                        .addValue("type", 4)
                        .addValue("parentId", 225)
                        .addValue("parentRuName", "Россия")
                        .addValue("parents", new Integer[]{10000, 10001, 225})
                        .addValue("children", new Integer[]{1})
                        .addValue("pathRuName", new String[]{"Центральный федеральный округ", "Россия", "Евразия",
                        "Земля"}),
                new MapSqlParameterSource()
                        .addValue("id", 17)
                        .addValue("ruName", "Северо-Западный федеральный округ")
                        .addValue("type", 4)
                        .addValue("parentId", 225)
                        .addValue("parentRuName", "Россия")
                        .addValue("parents", new Integer[]{10000, 10001, 225})
                        .addValue("children", new Integer[]{})
                        .addValue("pathRuName", new String[]{"Северо-Западный федеральный округ", "Россия", "Евразия"
                        , "Земля"}),
                new MapSqlParameterSource()
                        .addValue("id", 1)
                        .addValue("ruName", "Москва и Московская область")
                        .addValue("type", 5)
                        .addValue("parentId", 3)
                        .addValue("parentRuName", "Центральный федеральный округ")
                        .addValue("parents", new Integer[]{10000, 10001, 225, 3})
                        .addValue("children", new Integer[]{})
                        .addValue("pathRuName", new String[]{"Москва и Московская область", "Центральный федеральный " +
                        "округ", "Россия", "Евразия", "Земля"})
        };
        namedParameterJdbcTemplate.batchUpdate(INSERT_REGION, rows);
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/RegionControllerTest/testGetRegionTrees/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/RegionControllerTest/testGetRegionTrees/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testGetRegionTrees() {
        String actual = FunctionalTestHelper.get(baseUrl + "/regions?uid=1&regionId=225&regionId" +
                "=149");
        String expected = getStringResource("/testGetRegionTrees/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/RegionControllerTest/testGetRegions/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/RegionControllerTest/testGetRegions/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testGetRegions() {
        String actual = FunctionalTestHelper.get(baseUrl + "/vendors/101/regions?uid=1&regionId=225");
        String expected = getStringResource("/testGetRegions/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }
}
