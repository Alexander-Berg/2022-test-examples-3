package ru.yandex.market.olap2.util;

import java.util.Arrays;
import java.util.HashSet;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class MetadataMonitoringsUtilTest {

    @Test
    public void testSqlForSla() {
        String sql = MetadataMonitoringsUtil.getSqlForSLa(
                new HashSet<>(Arrays.asList("cube_new_order_dict",
                        "cube_order_dict", "cube_order_item_dict",
                        "fact_order_dict_aggr", "fact_inventory_trans_purchases",
                        "cube_stock_movement", "fact_ue_partitioned", "cube_end2end_analytics_white")),
                "//home/market/production/mstat/analyst/regular/cubes_vertica");

        log.info("sql:\n{}", sql);
    }

    @Test
    public void testSqlForMassFail() {
        String sql = MetadataMonitoringsUtil.getSqlForMassFail(
                "//home/market/production/mstat/analyst/regular/cubes_vertica", "clickhouse", 4);

        log.info("sql:\n{}", sql);
    }
}
