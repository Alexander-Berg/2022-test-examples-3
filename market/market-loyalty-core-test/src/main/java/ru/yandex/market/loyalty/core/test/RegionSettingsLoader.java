package ru.yandex.market.loyalty.core.test;

import org.intellij.lang.annotations.Language;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RegionSettingsLoader {

    public static final int YANDEX_PLUS_TEST_THRESHOLD = 500;
    public static final long YANDEX_PLUS_TEST_THRESHOLD_REGION = 11010;
    public static final long YANDEX_PLUS_TEST_THRESHOLD_REGION_WO_DEFAULT_THRESHOLD = 11013;

    @Language("PostgreSQL")
    private static final String INSERT_REGION_THRESHOLDS = "" +
            "INSERT INTO region_settings(region_id, threshold, threshold_enabled, coin_emission_enabled)" +
            "VALUES " +
            "(11457, NULL, FALSE, FALSE)," +
            "(11464, NULL, FALSE, FALSE)," +
            "(11375, NULL, FALSE, FALSE), " +
            "(102444,5000,TRUE, TRUE), " +
            "(59,5000,TRUE, FALSE), " +
            "(52,5000,TRUE, FALSE), " +
            "(73,7000,TRUE, FALSE), " +
            "(225,2499,TRUE, FALSE)," +
            "(11443, NULL, FALSE, FALSE)," +
            "(11398, NULL, FALSE, FALSE)," +
            "(10251, NULL, FALSE, FALSE)," +
            "(10176, NULL, FALSE, FALSE)," +
            "(11403, NULL, FALSE, FALSE)," +
            "(11450, NULL, FALSE, FALSE)," +
            "(11156, NULL, FALSE, FALSE)";

    @Language("PostgreSQL")
    private static final String INSERT_YANDEX_REGION_THRESHOLDS = "" +
            "INSERT INTO region_settings(region_id, threshold, threshold_enabled, yandex_plus_threshold_enabled, " +
            "yandex_plus_threshold,   coin_emission_enabled)" +
            "VALUES " +
            "(" + YANDEX_PLUS_TEST_THRESHOLD_REGION + ", 2499, TRUE, 'ENABLED', " + YANDEX_PLUS_TEST_THRESHOLD + "," +
            "FALSE), " +
            "(" + YANDEX_PLUS_TEST_THRESHOLD_REGION_WO_DEFAULT_THRESHOLD + ", NULL, FALSE, 'ENABLED', " + YANDEX_PLUS_TEST_THRESHOLD + ", FALSE) ";


    @Autowired
    JdbcTemplate jdbcTemplate;

    public void loadRegionThresholdAndCoinEmissionSettings() {
        jdbcTemplate.execute(INSERT_REGION_THRESHOLDS);
        jdbcTemplate.execute(INSERT_YANDEX_REGION_THRESHOLDS);
    }
}
