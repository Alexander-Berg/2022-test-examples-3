package ru.yandex.market.robot.db.raw_model.tables;

import com.ninja_squad.dbsetup.operation.Insert;
import ru.yandex.market.robot.shared.raw_model.RawModel;

import java.util.stream.Stream;

import static ru.yandex.market.robot.db.raw_model.tables.Columns.*;

/**
 * @author jkt on 20.12.17.
 */
public class MarketRelationTable {

    public static final String NAME = "market_relation";

    public static Stream<Insert> entryFor(RawModel model) {
        return Stream.of(
            Insert.into(NAME)
                .row()
                .column(MODEL_ID, model.getId())
                .column(VENDOR_ID, model.getMarketRelations().getVendorId())
                .column(VENDOR, model.getMarketRelations().getVendorName())
                .column(MARKET_CATEGORY_ID, model.getMarketCategoryId())
                .column(MAPPED_CATEGORY_ID, model.getMarketRelations().getMappedCategoryId())
                .column(MARKET_CATEGORY_NAME, model.getMarketRelations().getMarketCategoryName())
                .column(STATUS, model.getMarketRelations().getStatus())
                .column(FIRST_VERSION_NUMBER, model.getFirstVersionNumber())
                .column(LAST_VERSION_NUMBER, model.getLastVersionNumber())
                .column(LAST_MODEL_UPDATE_TIME, model.getMarketRelations().getLastModelUpdateTime())
                .column(MARKET_CATEGORY_STATUS, model.getMarketRelations().getMarketCategoryStatus())
                .end()
                .withBinder(new ModelStatusBinder(), STATUS, MARKET_CATEGORY_STATUS)
                .build()
        );
    }
}
