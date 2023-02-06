package ru.yandex.common.framework.db;

import junit.framework.TestCase;

/**
 * Date: 29.06.2009
 * Time: 17:23:59
 *
 * @author Antonina Mamaeva mamton@yandex-team.ru
 */
public class SqlAwareTest extends TestCase {
    private static final String TABLE_OUTLET_INFO = "info";
    private static final String TABLE_MODERATION = "mod";

    public void testJoinSqlAware() {
        JoinSqlAware awareInfo = new JoinSqlAware(TABLE_OUTLET_INFO, TABLE_MODERATION, ColumnsOutletInfo.ID.name(),
                ColumnsModeration.OUTLET_ID.name(), "=", ColumnsOutletInfo.getNames(null),
                ColumnsModeration.getNames(null));
        assertEquals("info join mod on info.ID=mod.OUTLET_ID", awareInfo.getTableName());
        assertEquals("info.ID, info.STATUS, mod.OUTLET_ID, mod.EDITED_OUTLET_ID", awareInfo.getColumnNames());
    }

    public void testOneColumnJoin() {
        JoinSqlAware awareInfo = new JoinSqlAware(TABLE_OUTLET_INFO, TABLE_MODERATION, ColumnsOutletInfo.ID.name(),
                ColumnsModeration.OUTLET_ID.name(), "=", new String[]{"ID"}, null);
        assertEquals("info join mod on info.ID=mod.OUTLET_ID", awareInfo.getTableName());
        assertEquals("info.ID", awareInfo.getColumnNames());
        awareInfo = new JoinSqlAware(TABLE_OUTLET_INFO, TABLE_MODERATION, ColumnsOutletInfo.ID.name(),
                ColumnsModeration.OUTLET_ID.name(), "=", null, new String[]{"ID"});
        assertEquals("info join mod on info.ID=mod.OUTLET_ID", awareInfo.getTableName());
        assertEquals("mod.ID", awareInfo.getColumnNames());
    }

    public void testSubColumnsSqlAware() {
        JoinSqlAware awareInfo = new JoinSqlAware(TABLE_OUTLET_INFO, TABLE_MODERATION, ColumnsOutletInfo.ID.name(),
                ColumnsModeration.OUTLET_ID.name(), "=", ColumnsOutletInfo.getNames(null),
                ColumnsModeration.getNames(null));
        SubColumnsSqlAware subColumnsSqlAware =
                new SubColumnsSqlAware(awareInfo, new String[]{ColumnsOutletInfo.ID.toString()});
        assertEquals("info join mod on info.ID=mod.OUTLET_ID", awareInfo.getTableName());
        assertEquals("info join mod on info.ID=mod.OUTLET_ID", subColumnsSqlAware.getTableName());
        assertEquals("info.ID, info.STATUS, mod.OUTLET_ID, mod.EDITED_OUTLET_ID", awareInfo.getColumnNames());
        assertEquals("ID", subColumnsSqlAware.getColumnNames());
        try {
            subColumnsSqlAware = new SubColumnsSqlAware(awareInfo, new String[]{"ccc"});
            fail();
        } catch (IllegalStateException ex) {
        }
    }
}
