package ru.yandex.market.robot.db.raw_model.tables;

import com.ninja_squad.dbsetup.operation.Insert;
import ru.yandex.market.robot.shared.clusterizer.CategorySettingsDto;
import ru.yandex.market.robot.shared.clusterizer.TitleParam;

import java.util.stream.Stream;

import static ru.yandex.market.robot.db.raw_model.tables.Columns.ADD_TO_TITLE_ON_DUPLICATE;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.ADD_TO_TITLE_STR;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.ALIASES;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.PARAM_ID;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.POSITION;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.REMOVE_FROM_TITLE;
import static ru.yandex.market.robot.db.raw_model.tables.Columns.XSL_NAME;

/**
 * @author jkt on 20.12.17.
 */
public class CategoryParamsTable {

    public static final String NAME = "category_params";

    public static Stream<Insert> entryFor(CategorySettingsDto categoryData) {
        return categoryData.getParams().stream()
            .map(param -> entryFor(categoryData.getCategoryId(), param));
    }

    public static Insert entryFor(int categoryId, TitleParam parameter) {
        return Insert.into(NAME)
            .row()
            .column(Columns.CATEGORY_ID, categoryId)
            .column(PARAM_ID, parameter.getParamId())
            .column(REMOVE_FROM_TITLE, parameter.isRemoveFromTitle())
            .column(POSITION, parameter.getPosition())
            .column(ADD_TO_TITLE_ON_DUPLICATE, parameter.isAddOnDuplicate())
            .column(ALIASES, parameter.getAliases())
            .column(Columns.PROCESSORS, parameter.getParamProcessors())
            .column(XSL_NAME, parameter.getXslName())
            .column(ADD_TO_TITLE_STR, parameter.getAddToTitle())
            .end()
            .build();
    }
}
