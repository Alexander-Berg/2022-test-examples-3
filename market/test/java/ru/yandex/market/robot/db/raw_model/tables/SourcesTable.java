package ru.yandex.market.robot.db.raw_model.tables;

import com.ninja_squad.dbsetup.operation.Insert;
import ru.yandex.market.robot.shared.clusterizer.CategorySettingsDto;
import ru.yandex.market.robot.shared.clusterizer.SourceSettings;

import java.util.stream.Stream;

import static ru.yandex.market.robot.db.raw_model.tables.Columns.CALCULATE_METRICS;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.CATEGORY_ID;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.ENABLED;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.PARTNER_SHOP_ID;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.REMOVE_CATEGORY_NAME;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.REMOVE_UPPER_CASE;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.SOURCE_ID;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.SPLIT_NUMBERS_LETTERS;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.STOP_WORDS;

/**
 * @author jkt on 20.12.17.
 */
public class SourcesTable {

    public static final String NAME = "sources";

    public static Stream<Insert> entryFor(CategorySettingsDto categoryData) {
        return categoryData.getSources().values().stream()
            .map(source -> entryFor(categoryData.getCategoryId(), source));

    }

    public static Insert entryFor(int categoryId, SourceSettings source) {
        return Insert.into(NAME)
            .row()
            .column(CATEGORY_ID, categoryId)
            .column(SOURCE_ID, source.getSourceId())
            .column(REMOVE_UPPER_CASE, source.isRemoveUpperCase())
            .column(REMOVE_CATEGORY_NAME, source.isRemoveCategoryTokens())
            .column(ENABLED, source.isEnabled())
            .column(STOP_WORDS, source.getStopWords())
            .column(SPLIT_NUMBERS_LETTERS, source.isSplitNumbersLetters())
            .column(CALCULATE_METRICS, source.isCalculateMetrics())
            .column(PARTNER_SHOP_ID, source.getPartnerShopId())
            .end()
            .build();

    }
}
