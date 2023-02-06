package ru.yandex.market.mbo.core.kdepot.impl.search;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.core.kdepot.api.KnownLinkTypes;

import java.util.Arrays;

import static java.util.Arrays.asList;
import static ru.yandex.market.mbo.core.kdepot.api.KnownLinkTypes.MARKET_GURU_INHERITANCE;
import static ru.yandex.market.mbo.core.kdepot.impl.search.PathElement.createPathElement;

/**
 * @author Dmitry Tsyganov dtsyganov@yandex-team.ru
 */
@SuppressWarnings("checkstyle:magicNumber")
public class LinksPathConditionTest {

    @Test
    public void testBuildSqlDirect() {
        String sql = LinksPathCondition.buildSql(1,
            asList(
                createPathElement(MARKET_GURU_INHERITANCE, true),
                createPathElement(MARKET_GURU_INHERITANCE, true)
            )
        );
        Assert.assertEquals(
            " e.entity_id in (\n" +
                " select k2.to_id from link k2\n" +
                " join link k1 on k2.from_id = k1.to_id and k1.link_type_id = 20050\n" +
                " where k2.link_type_id = 20050 and k1.from_id = 1)", sql);
    }

    @Test
    public void testBuildSqlReverse() {
        String sql = LinksPathCondition.buildSql(1,
            asList(
                createPathElement(MARKET_GURU_INHERITANCE, false),
                createPathElement(MARKET_GURU_INHERITANCE, false)
            )
        );
        Assert.assertEquals(
            " e.entity_id in (\n" +
                " select k2.from_id from link k2\n" +
                " join link k1 on k2.to_id = k1.from_id and k1.link_type_id = 20050\n" +
                " where k2.link_type_id = 20050 and k1.to_id = 1)", sql);
    }

    @Test
    public void testq() {
        String sql = LinksPathCondition.buildSql(
            5158921,
            Arrays.asList(
                PathElement.createPathElement(KnownLinkTypes.TOVAR_TREE_HISTORY_TO_CATEGORY, true),
                PathElement.createPathElement(KnownLinkTypes.TOVAR_TREE_CATEGORY_CATEGORY_LINK_TYPE_ID, true),
                PathElement.createPathElement(KnownLinkTypes.TOVAR_TREE_GURU, true)
            ));
        Assert.assertEquals(
            " e.entity_id in (\n" +
            " select k3.to_id from link k3\n" +
            " join link k2 on k3.from_id = k2.to_id and k2.link_type_id = 100\n" +
            " join link k1 on k2.from_id = k1.to_id and k1.link_type_id = 101\n" +
            " where k3.link_type_id = 105 and k1.from_id = 5158921)", sql);
    }
}
