package ru.yandex.market.mbo.core.kdepot.impl.search;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.core.kdepot.api.KnownEntityTypes;
import ru.yandex.market.mbo.core.kdepot.api.KnownLinkTypes;
import ru.yandex.market.mbo.core.kdepot.api.SearchCondition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author mar-tim  mar-tim@yandex-team.ru
 */
@SuppressWarnings("checkstyle:magicNumber")
public class SearchJoinConditionTest {

    @Test
    public void testBuildSql() {
        SearchJoinCondition searchJoinCondition = new SearchJoinCondition();

        String sql = searchJoinCondition.getConditionSql("");
        Assert.assertEquals(" select b.ENTITY_ID from entity b  where  1 = 1 ", sql);
    }

    @Test
    public void testBuildSqlTypeCondition() {
        SearchJoinCondition searchJoinCondition = new SearchJoinCondition();
        searchJoinCondition.add(new TypeCondition(KnownEntityTypes.MARKET_MODEL));

        String sql = searchJoinCondition.getConditionSql("");
        Assert.assertEquals(" select b.ENTITY_ID from entity b  where  1 = 1 " +
            " AND b.entity_type_id in (10002)", sql);
    }

    @Test
    public void testBuildSqlTypeConditionAttr() {
        SearchJoinCondition searchJoinCondition = new SearchJoinCondition();
        searchJoinCondition.add(new TypeCondition(KnownEntityTypes.MARKET_MODEL));

        searchJoinCondition.add(new AttributeSubstringCondition("name", "AMW-710D-2A"));

        String sql = searchJoinCondition.getConditionSql("");
        String constSql = " select b.ENTITY_ID from entity b " +
            " join ENTITY_ATTR e0 on e0.entity_id = b.entity_id and e0.attr_name = 'name'" +
            " AND LOWER(e0.attr_as_str) LIKE LOWER('%AMW-710D-2A%')  where  1 = 1  AND b.entity_type_id in (10002)";
        Assert.assertEquals(constSql, sql);
    }


    @Test
    public void testBuildSqlTypeConditionAttrLink() {
        SearchJoinCondition searchJoinCondition = new SearchJoinCondition();
        searchJoinCondition.add(new TypeCondition(KnownEntityTypes.MARKET_MODEL));

        searchJoinCondition.add(new ToLinkCondition(2544908, KnownLinkTypes.MARKET_GURU_INHERITANCE));
        searchJoinCondition.add(new AttributeSubstringCondition("name", "AMW-710D-2A"));

        String sql = searchJoinCondition.getConditionSql("");
        String constSql = " select b.ENTITY_ID from entity b " +
            " join LINK l0 on b.ENTITY_ID = l0.from_id and l0.to_id = 2544908 AND l0.link_type_id = 20050 " +
            " join ENTITY_ATTR e1 on e1.entity_id = b.entity_id and e1.attr_name = 'name'" +
            " AND LOWER(e1.attr_as_str) LIKE LOWER('%AMW-710D-2A%')  where  1 = 1  AND b.entity_type_id in (10002)";
        Assert.assertEquals(constSql, sql);
    }

    @Test
    public void testBuildSqlTypeConditionManyAttr() {
        SearchJoinCondition searchJoinCondition = new SearchJoinCondition();
        searchJoinCondition.add(new TypeCondition(KnownEntityTypes.PROPERTY_TEMPLATE_TYPE_ID));

        List<SearchCondition> c = new ArrayList<>();
        c.add(new AttributeCondition("entity_type_id", "10002"));
        c.add(new AttributeCondition("hidden", "FALSE"));
        c.add(new AttributeCondition("type", "boolean"));
        c.add(new AttributeCondition("necessary", "FALSE"));
        c.add(new AttributeSubstringCondition("name", "bluetooth"));
        c.add(new LinksPathCondition(
            111,
            Arrays.asList(
                PathElement.createPathElement(KnownLinkTypes.MARKET_GURU_INHERITANCE, false),
                PathElement.createPathElement(KnownLinkTypes.MARKET_GURU_INHERITANCE, false)
            )
        ));
        searchJoinCondition.addAll(c);

        String sql = searchJoinCondition.getConditionSql("");
        String constSql = " select b.ENTITY_ID from entity b " +
            " join ENTITY_ATTR e0 on e0.entity_id = b.entity_id" +
            " and e0.attr_name = 'entity_type_id' AND e0.attr_as_str = '10002' " +
            " join ENTITY_ATTR e1 on e1.entity_id = b.entity_id and e1.attr_name = 'hidden'" +
            " AND e1.attr_as_str = 'FALSE'  join ENTITY_ATTR e2 on e2.entity_id = b.entity_id" +
            " and e2.attr_name = 'type' AND e2.attr_as_str = 'boolean' " +
            " join ENTITY_ATTR e3 on e3.entity_id = b.entity_id and e3.attr_name = 'necessary'" +
            " AND e3.attr_as_str = 'FALSE'  join ENTITY_ATTR e4 on e4.entity_id = b.entity_id" +
            " and e4.attr_name = 'name' AND LOWER(e4.attr_as_str) LIKE LOWER('%bluetooth%')" +
            " join link k1 on k1.from_id = b.entity_id and k1.link_type_id = 20050 join link k0" +
            " on k0.from_id = k1.to_id and k0.link_type_id = 20050  and k0.to_id = 111 where " +
            " 1 = 1  AND b.entity_type_id in (101)";
        Assert.assertEquals(constSql, sql);
    }

    @Test
    public void testLinkPathCondition() {
        SearchJoinCondition searchJoinCondition = new SearchJoinCondition();
        searchJoinCondition.add(new LinksPathCondition(
            5158921,
            Arrays.asList(
                PathElement.createPathElement(KnownLinkTypes.TOVAR_TREE_HISTORY_TO_CATEGORY, true),
                PathElement.createPathElement(KnownLinkTypes.TOVAR_TREE_CATEGORY_CATEGORY_LINK_TYPE_ID, true)
            )));

        String sql = searchJoinCondition.getConditionSql("");
        Assert.assertEquals(" select b.ENTITY_ID from entity b " +
            "join link k1 on k1.to_id = b.entity_id and k1.link_type_id = 100 " +
            "join link k0 on k0.to_id = k1.from_id and k0.link_type_id = 101 " +
            " and k0.from_id = 5158921 where  1 = 1 ", sql);
    }

    @Test
    public void testLinkPathConditionModel() {
        SearchJoinCondition searchJoinCondition = new SearchJoinCondition();
        searchJoinCondition.add(new LinksPathCondition(
            4648551,
            Arrays.asList(
                PathElement.createPathElement(KnownLinkTypes.MARKET_GURU_INHERITANCE, true),
                PathElement.createPathElement(KnownLinkTypes.MARKET_GURU_INHERITANCE, true)
            )
        ));

        String sql = searchJoinCondition.getConditionSql("");
        Assert.assertEquals(" select b.ENTITY_ID from entity b" +
            " join link k1 on k1.to_id = b.entity_id and k1.link_type_id = 20050" +
            " join link k0 on k0.to_id = k1.from_id and k0.link_type_id = 20050 " +
            " and k0.from_id = 4648551 where  1 = 1 ", sql);
    }

    @Test
    public void testFieldsOrderWithoutServiceField() {
        SearchJoinCondition joinCondition = new SearchJoinCondition();
        joinCondition.add(new TypeCondition(KnownEntityTypes.PROPERTY_TEMPLATE_TYPE_ID));
        joinCondition.addAll(getConditions());

        joinCondition.add(new AttributeSubstringCondition("entity_type_id",
            String.valueOf(KnownEntityTypes.MARKET_MODEL)));
        joinCondition.add(new AttributeSubstringCondition("view_type", "741147"));

        String sql = SearchHelper.buildEntitiesSql(
            Collections.singletonList(new SetCondition(joinCondition)),
            Collections.emptyList()
        );
        Assert.assertEquals("SELECT * FROM" +
            " (SELECT       e.entity_id,       e.attr_name,       e.attr_npp,       e.attr_as_str,       " +
            "b.entity_type_id,       e.attr_index,       b.modified_ts  FROM       entity_attr e       " +
            "INNER JOIN entity b ON e.entity_id = b.entity_id  WHERE       1=1 " +
            "AND  e.entity_id IN " +
            "( select b.ENTITY_ID from entity b " +
            " join ENTITY_ATTR e1 on e1.entity_id = b.entity_id and e1.attr_name = 'entity_type_id'" +
            " AND LOWER(e1.attr_as_str) LIKE LOWER('%10002%')  join ENTITY_ATTR e2 on e2.entity_id = b.entity_id" +
            " and e2.attr_name = 'view_type' AND LOWER(e2.attr_as_str) LIKE LOWER('%741147%')" +
            "  where  1 = 1  AND b.entity_type_id in (101) AND  b.entity_id NOT IN" +
            " ( select bb.ENTITY_ID from entity bb  join ENTITY_ATTR e0 on e0.entity_id = bb.entity_id" +
            " and e0.attr_name = 'is_service' AND e0.attr_as_str = 'TRUE'  where  1 = 1 " +
            " AND bb.entity_type_id in (101)) ) ) ORDER BY entity_id, attr_index ASC", sql);
    }

    protected List<SearchCondition> getConditions() {
        SearchJoinCondition joinCondition = new SearchJoinCondition();
        joinCondition.add(new TypeCondition(KnownEntityTypes.PROPERTY_TEMPLATE_TYPE_ID));
        joinCondition.add(new AttributeCondition("is_service", "TRUE"));
        SearchCondition sc = new SetCondition(joinCondition, SetCondition.SetOperator.NOT_IN);
        return Collections.singletonList(sc);
    }
}
