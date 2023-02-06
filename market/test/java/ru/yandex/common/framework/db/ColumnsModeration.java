package ru.yandex.common.framework.db;

import ru.yandex.common.util.db.DbUtil;

/**
 * Date: 25.06.2009
 * Time: 20:25:48
 *
 * @author Antonina Mamaeva mamton@yandex-team.ru
 */ //  ---------------------------------------------------------------------------------------------------------------
public enum ColumnsModeration {
    OUTLET_ID,
    EDITED_OUTLET_ID;

    public static String getNamesString(String tableName) {
        return DbUtil.convertToColumnsString(ColumnsModeration.values(), tableName);
    }

    public static String[] getNames(String tableName) {
        return DbUtil.convertToColumnNames(ColumnsModeration.values(), tableName);
    }
}