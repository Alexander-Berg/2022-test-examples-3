package ru.yandex.common.framework.db;

import ru.yandex.common.util.db.DbUtil;

/**
 * Date: 25.06.2009
 * Time: 20:25:19
 *
 * @author Antonina Mamaeva mamton@yandex-team.ru
 */
public enum ColumnsOutletInfo {
    ID,
    STATUS;

    public static String getNamesString(String tableName) {
        return DbUtil.convertToColumnsString(ColumnsOutletInfo.values(), tableName);
    }

    public static String[] getNames(String tableName) {
        return DbUtil.convertToColumnNames(ColumnsOutletInfo.values(), tableName);
    }
}