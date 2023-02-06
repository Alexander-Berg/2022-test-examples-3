package ru.yandex.market.robot.db.raw_model.tables;

import com.ninja_squad.dbsetup.operation.Insert;
import ru.yandex.market.robot.shared.clusterizer.CategorySettings;
import ru.yandex.market.robot.shared.clusterizer.CategorySettingsDto;

import java.util.stream.Stream;

import static ru.yandex.market.robot.db.raw_model.tables.Columns.CHECK_MODELS;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.CLASSIFIER_WORDS;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.CLUSTERIZER_TYPE;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.ENABLED;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.ID;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.STOP_WORDS;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.TYPE_INDEX;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.USE_FORMALIZER;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.VENDOR_CODE_INDEX;

/**
 * @author jkt on 20.12.17.
 */
public class CategoriesTable {

    public static final String NAME = "categories";

    public static Stream<Insert> entryFor(CategorySettingsDto categoryData) {
        return Stream.of(entryFor(categoryData, "NOT_USED_IN_THIS_TEST"));
    }

    public static Insert entryFor(CategorySettingsDto categorySettings, String stopWords) {
        return Insert.into(NAME)
            .row()
            .column(ID, categorySettings.getCategoryId())
            .column(ENABLED, categorySettings.isEnabled())
            .column(CHECK_MODELS, categorySettings.isCheckModels())
            .column(CLASSIFIER_WORDS, categorySettings.getClassifierWords())
            .column(CLUSTERIZER_TYPE, categorySettings.getClusterizerType())
            .column(VENDOR_CODE_INDEX, categorySettings.getVendorCodeIndex())
            .column(TYPE_INDEX, categorySettings.getTypeIndex())
            .column(STOP_WORDS, stopWords)
            .column(USE_FORMALIZER, categorySettings.isUseFormalizer())
            .end()
            .build();
    }
}
