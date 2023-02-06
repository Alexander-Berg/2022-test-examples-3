package ru.yandex.market.supercontroller.mbologs;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Assert;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YtTables;

/**
 * @author amaslak
 */
public class YtMock {

    public static final String GD_DIFF_SOURCE_TABLE =
        "//tmp/mbo-fake/sc_generation_diff/20171119_2019_20171120_0600/mbo_offers_mr";

    public static final String OP_SOURCE_TABLE = "//tmp/mbo-fake/20171123_2032/params_mr";
    public static final String OP_DASHBOARD_TABLE_NAME = "20171123_2032/params_mr";

    public static final String GD_SOURCE_TABLE = "//tmp/mbo-fake/20121011_1000/mbo_offers_mr";
    public static final String GD_DASHBOARD_TABLE_NAME = "20121011_1000/mbo_offers_mr";

    public static final String GL_SOURCE_TABLE = "//tmp/mbo-fake/offer_stat_20121011_1000";
    public static final String GL_DASHBOARD_TABLE_NAME = "offer_stat_20121011_1000";

    public static final List<String> GL_SESSIONS = Arrays.asList("20121011_1000");

    public static final int ROW_COUNT = 20;

    private YtMock() {
    }

    public static Yt mockYt() {
        Yt yt = Mockito.mock(Yt.class);

        Cypress cypress = mockCypress();
        Mockito.when(yt.cypress()).thenReturn(cypress);

        YtTables ytTables = mockYtTables();
        Mockito.when(yt.tables()).thenReturn(ytTables);
        return yt;
    }

    public static Cypress mockCypress() {
        Cypress cypress = Mockito.mock(Cypress.class);

        Mockito.doAnswer(invocation -> {
            YPath path = invocation.getArgument(0);
            if (path.toString().endsWith("/@row_count")) {
                return YTree.integerNode(ROW_COUNT);
            }
            return null;
        }).when(cypress).get(Mockito.any(YPath.class));

        Mockito.doReturn(YTree.builder()
            .beginAttributes().key("row_count").value(ROW_COUNT).endAttributes()
            .entity().build()
        ).when(cypress).get(Mockito.any(), Mockito.anyBoolean(), Mockito.any(), Mockito.eq(Cf.set("row_count")));
        return cypress;
    }

    /**
     * @noinspection unchecked
     */
    public static YtTables mockYtTables() {
        YtTables ytTables = Mockito.mock(YtTables.class);

        // mock reads as if table empty
        Mockito.doAnswer(invocation -> {
            YPath path = invocation.getArgument(0);
            String tableName = path.toTree().stringValue();
            Assert.assertTrue(
                GD_SOURCE_TABLE.equalsIgnoreCase(tableName)
                    || GD_DIFF_SOURCE_TABLE.equalsIgnoreCase(tableName)
                    || GL_SOURCE_TABLE.equalsIgnoreCase(tableName)
                    || OP_SOURCE_TABLE.equalsIgnoreCase(tableName)
            );
            return null;
        }).when(ytTables)
            .read(
                Mockito.isA(YPath.class),
                Mockito.isA(YTableEntryType.class),
                Mockito.isA(Consumer.class)
            );
        return ytTables;
    }

}
