package ru.yandex.market.robot.db.raw_model.tables;

import com.ninja_squad.dbsetup.operation.Insert;

import static ru.yandex.market.robot.db.raw_model.tables.Columns.*;

/**
 * @author jkt on 20.12.17.
 */
public class SessionLogTable {

    public static final String NAME = "session_log";

    public static Insert empty() {
        return entries().build();
    }

    public static Insert.Builder entries() {
        return columns(CATEGORY_ID, VENDOR_ID, OFFERS_COUNT);
    }

    public static Insert.Builder columns(String... columns) {
        return Insert.into(NAME)
            .columns(columns);
    }
}
