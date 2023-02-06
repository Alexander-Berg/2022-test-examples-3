package ru.yandex.market.robot.db.raw_model.tables;

import com.ninja_squad.dbsetup.operation.Insert;

import static ru.yandex.market.robot.db.raw_model.tables.Columns.*;

/**
 * @author jkt on 20.12.17.
 */
public class MatchingLogTable {

    public static final String NAME = "matching_log";

    public static Insert empty() {
        return entries().build();
    }

    public static Insert.Builder entries() {
        return columns(CATEGORY_ID, VENDOR_ID, MODEL_ID, MATCHED_COUNT);
    }

    public static Insert.Builder columns(String... columns) {
        return Insert.into(NAME)
            .columns(columns);
    }
}
