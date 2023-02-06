package ru.yandex.direct.useractionlog.db;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import ru.yandex.direct.useractionlog.model.AutoChangeableSettings;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.useractionlog.db.ReadPpclogApiTable.USER_LOG_DATE_ZONE_ID;

public class ReadActionLogTableTest {

    private static final Long CLIENT_ID = 55555L;

    @Test
    public void lastAutoUpdatedSettingsSqlBuilder_success(){
        var cids = List.of(111L, 222L);
        var settings = List.of(
                new AutoChangeableSettings()
                        .withType("campaigns")
                        .withItem("strategy_data")
                        .withSubitem("sum"),
                new AutoChangeableSettings()
                        .withType("campaigns")
                        .withItem("strategy_data")
                        .withSubitem("avg_bid"),
                new AutoChangeableSettings()
                        .withType("campaigns")
                        .withItem("day_budget")
                        .withSubitem(null)
                );

        var maxTimeDepth = LocalDateTime.now().minusYears(1);
        var repository = new ReadActionLogTable(null, "user_action_log");
        var objectPaths = repository.buildObjectPathsByCids(CLIENT_ID, cids);
        var sql = repository.lastAutoUpdatedSettingsSqlBuilder(settings, objectPaths, maxTimeDepth);
        var actualQuery = sql.generateSql(true);
        var actualBindigs = sql.getBindings();
        var expectedSql = "SELECT `path`, `object_item`, `object_subitem`, argMax(datetime, datetime) as " +
                "`last_update_datetime`, argMax(new_value, datetime) as `last_update_new_value`, argMax(old_value, " +
                "datetime) as `last_update_old_value`\n" +
                "FROM \n" +
                "( SELECT `path`, `object_item`, `object_subitem`, `datetime`, if(object_subitem is null, " +
                "arrayElement(`new_fields`.`value`, new_field_value_index), JSONExtractRaw(arrayElement(`new_fields`" +
                ".`value`, new_field_value_index), object_subitem)) as `new_value`, if(object_subitem is null, " +
                "arrayElement(`old_fields`.`value`, old_field_value_index), JSONExtractRaw(arrayElement(`old_fields`" +
                ".`value`, old_field_value_index), object_subitem)) as `old_value`\n" +
                "FROM \n" +
                "( SELECT `path`, `datetime`, `new_fields`.`value`, `old_fields`.`value`, 'strategy_data' as " +
                "`object_item`, 'sum' as `object_subitem`, arrayFirstIndex(f -> (f in ('strategy_data')), " +
                "`new_fields`.`name`) as `new_field_value_index`, arrayFirstIndex(f -> (f in ('strategy_data')), " +
                "`old_fields`.`name`) as `old_field_value_index`\n" +
                "FROM `user_action_log`\n" +
                "WHERE `datetime` >= ?\n" +
                "AND type = ? AND (arrayExists(f -> (f in (?)), if(`operation` = 'INSERT',  `new_fields`.`name`, " +
                "`old_fields`.`name`)))\n" +
                "AND `method` = ?\n" +
                "AND (`path` LIKE ? OR `path` LIKE ?)\n" +
                "UNION ALL\n" +
                "SELECT `path`, `datetime`, `new_fields`.`value`, `old_fields`.`value`, 'strategy_data' as " +
                "`object_item`, 'avg_bid' as `object_subitem`, arrayFirstIndex(f -> (f in ('strategy_data')), " +
                "`new_fields`.`name`) as `new_field_value_index`, arrayFirstIndex(f -> (f in ('strategy_data')), " +
                "`old_fields`.`name`) as `old_field_value_index`\n" +
                "FROM `user_action_log`\n" +
                "WHERE `datetime` >= ?\n" +
                "AND type = ? AND (arrayExists(f -> (f in (?)), if(`operation` = 'INSERT',  `new_fields`.`name`, " +
                "`old_fields`.`name`)))\n" +
                "AND `method` = ?\n" +
                "AND (`path` LIKE ? OR `path` LIKE ?)\n" +
                "UNION ALL\n" +
                "SELECT `path`, `datetime`, `new_fields`.`value`, `old_fields`.`value`, 'day_budget' as " +
                "`object_item`, null as `object_subitem`, arrayFirstIndex(f -> (f in ('day_budget')), `new_fields`" +
                ".`name`) as `new_field_value_index`, arrayFirstIndex(f -> (f in ('day_budget')), `old_fields`" +
                ".`name`) as `old_field_value_index`\n" +
                "FROM `user_action_log`\n" +
                "WHERE `datetime` >= ?\n" +
                "AND type = ? AND (arrayExists(f -> (f in (?)), if(`operation` = 'INSERT',  `new_fields`.`name`, " +
                "`old_fields`.`name`)))\n" +
                "AND `method` = ?\n" +
                "AND (`path` LIKE ? OR `path` LIKE ?)\n" +
                " )\n" +
                "WHERE new_value != old_value\n" +
                " )\n" +
                "GROUP BY `path`, `object_item`, `object_subitem`";
        assertEquals(expectedSql, actualQuery);
        assertEquals(18, actualBindigs.length);
        assertEquals(maxTimeDepth, convertTimestampToLocalDateTime((Timestamp) actualBindigs[0]));
        assertEquals("campaigns", actualBindigs[1]);
        assertEquals("strategy_data", actualBindigs[2]);
        assertEquals(ReadActionLogTable.AUTO_APPLY_JOB_NAME, actualBindigs[3]);
        assertEquals("client:55555-camp:111-%", actualBindigs[4]);
        assertEquals("client:55555-camp:222-%", actualBindigs[5]);
        assertEquals(maxTimeDepth, convertTimestampToLocalDateTime((Timestamp) actualBindigs[6]));
        assertEquals("campaigns", actualBindigs[7]);
        assertEquals("strategy_data", actualBindigs[8]);
        assertEquals(ReadActionLogTable.AUTO_APPLY_JOB_NAME, actualBindigs[9]);
        assertEquals("client:55555-camp:111-%", actualBindigs[10]);
        assertEquals("client:55555-camp:222-%", actualBindigs[11]);
        assertEquals(maxTimeDepth, convertTimestampToLocalDateTime((Timestamp) actualBindigs[12]));
        assertEquals("campaigns", actualBindigs[13]);
        assertEquals("day_budget", actualBindigs[14]);
        assertEquals(ReadActionLogTable.AUTO_APPLY_JOB_NAME, actualBindigs[15]);
        assertEquals("client:55555-camp:111-%", actualBindigs[16]);
        assertEquals("client:55555-camp:222-%", actualBindigs[17]);
    }

    @Test
    public void lastAutoUpdatedEnabledTimeSqlBuilder_success() {
        var cids = List.of(111L, 222L);
        var settings = List.of(
                new AutoChangeableSettings()
                        .withType("campaigns")
                        .withItem("strategy_data")
                        .withSubitem("sum")
                        .withRecommendationOptionName("price_recommendations_management_enabled"),
                new AutoChangeableSettings()
                        .withType("campaigns")
                        .withItem("strategy_data")
                        .withSubitem("avg_bid")
                        .withRecommendationOptionName("price_recommendations_management_enabled"),
                new AutoChangeableSettings()
                        .withType("campaigns")
                        .withItem("day_budget")
                        .withSubitem(null)
                        .withRecommendationOptionName("recommendations_management_enabled")
        );

        var maxTimeDepth = LocalDateTime.now().minusYears(1);
        var repository = new ReadActionLogTable(null, "user_action_log");
        var objectPaths = repository.buildObjectPathsByCids(CLIENT_ID, cids);
        var sql = repository.recommendationManagementHistorySqlBuilder(settings, objectPaths, maxTimeDepth);
        var actualQuery = sql.generateSql(true);
        var actualBindigs = sql.getBindings();
        var expectedQuery = "SELECT `path`, `rec_opt_name`, argMax(datetime, datetime) as `last_update_datetime`, " +
                "new_value as `last_update_new_value`\n" +
                "FROM \n" +
                "( SELECT `path`, `datetime`, 'price_recommendations_management_enabled' as `rec_opt_name`, toString" +
                "(has(splitByChar(',' , arrayElement(`new_fields`.`value`, arrayFirstIndex(f -> (f in ('opts')), " +
                "`new_fields`.`name`))), 'price_recommendations_management_enabled')) as `new_value`\n" +
                "FROM `user_action_log`\n" +
                "WHERE `datetime` >= ?\n" +
                "AND type = ? AND arrayExists(f -> (f in ('opts')), `new_fields`.`name`)\n" +
                "AND (`path` LIKE ? OR `path` LIKE ?)\n" +
                "UNION ALL\n" +
                "SELECT `path`, `datetime`, 'recommendations_management_enabled' as `rec_opt_name`, toString(has" +
                "(splitByChar(',' , arrayElement(`new_fields`.`value`, arrayFirstIndex(f -> (f in ('opts')), " +
                "`new_fields`.`name`))), 'recommendations_management_enabled')) as `new_value`\n" +
                "FROM `user_action_log`\n" +
                "WHERE `datetime` >= ?\n" +
                "AND type = ? AND arrayExists(f -> (f in ('opts')), `new_fields`.`name`)\n" +
                "AND (`path` LIKE ? OR `path` LIKE ?)\n" +
                " )\n" +
                "GROUP BY `path`, `rec_opt_name`, `last_update_new_value`";
        assertEquals(expectedQuery, actualQuery);
        assertEquals(8, actualBindigs.length);
        assertEquals(maxTimeDepth, convertTimestampToLocalDateTime((Timestamp) actualBindigs[0]));
        assertEquals("campaigns", actualBindigs[1]);
        assertEquals("client:55555-camp:111-%", actualBindigs[2]);
        assertEquals("client:55555-camp:222-%", actualBindigs[3]);
        assertEquals(maxTimeDepth, convertTimestampToLocalDateTime((Timestamp) actualBindigs[4]));
        assertEquals("campaigns", actualBindigs[5]);
        assertEquals("client:55555-camp:111-%", actualBindigs[6]);
        assertEquals("client:55555-camp:222-%", actualBindigs[7]);
    }

    private LocalDateTime convertTimestampToLocalDateTime(Timestamp timestamp){
        return timestamp.toInstant().atZone(USER_LOG_DATE_ZONE_ID).toLocalDateTime();
    }
}
