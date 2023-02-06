package ru.yandex.market.robot.db.raw_model.tables;

import com.ninja_squad.dbsetup.operation.Insert;

import static ru.yandex.market.robot.db.raw_model.tables.Columns.CREATED_ROW_DATE;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.FIRST_VERSION_NUMBER;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.LAST_MODEL_UPDATE_TIME;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.LAST_VERSION_NUMBER;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.MARKET_CATEGORY_ID;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.MARKET_CATEGORY_STATUS;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.MODEL_ID;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.STATUS;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.VENDOR_ID;

public class MarketRelationHistoryTable {
    public static final String NAME = "market_relation_history";

    public static Insert.Builder entries() {
        return columns(MARKET_CATEGORY_ID, VENDOR_ID, CREATED_ROW_DATE, MODEL_ID, STATUS, FIRST_VERSION_NUMBER,
            LAST_VERSION_NUMBER, MARKET_CATEGORY_STATUS, LAST_MODEL_UPDATE_TIME);
    }

    public static Insert.Builder columns(String... columns) {
        return Insert.into(NAME)
            .columns(columns)
            .withBinder(new ModelStatusBinder(), STATUS, MARKET_CATEGORY_STATUS);
    }
}
