package ru.yandex.direct.multitype.entity;

import org.jooq.JoinType;
import org.jooq.SelectQuery;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.dbschema.ppc.Tables;
import ru.yandex.direct.dbschema.ppc.tables.Campaigns;
import ru.yandex.direct.dbschema.ppc.tables.CampaignsInternal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JoinCollectorTest {

    private final Campaigns left = Tables.CAMPAIGNS;
    private final CampaignsInternal right = Tables.CAMPAIGNS_INTERNAL;

    @Test
    public void simpleJoin() {
        var collector = new JoinCollector();
        var baseSelect = select();
        var expected = select();
        var joinQuery = join();
        collector.collect(joinQuery);

        var query = collector.addSelectJoins(baseSelect);
        expected.addJoin(joinQuery.getTable(), joinQuery.getType(), joinQuery.getConditions());

        assertEquals(query.getSQL(), expected.getSQL());
    }

    @Test
    public void deduplicateJoins() {
        var collector = new JoinCollector();
        var baseSelect = select();
        var expected = select();
        var joinQuery = join();
        collector.collect(joinQuery);
        collector.collect(joinQuery);

        var query = collector.addSelectJoins(baseSelect);
        expected.addJoin(joinQuery.getTable(), joinQuery.getType(), joinQuery.getConditions());

        assertEquals(query.getSQL(), expected.getSQL());
    }

    @Test
    public void notTouchAliasJoins() {
        var collector = new JoinCollector();
        var baseSelect = select();
        var expected = select();
        var joinQuery = join();
        var rightAlias = right.as("right_alias");
        var aliasJoin = new JoinQuery(rightAlias, JoinType.LEFT_OUTER_JOIN, rightAlias.CID.eq(left.CID));

        collector.collect(joinQuery);
        collector.collect(aliasJoin);

        var query = collector.addSelectJoins(baseSelect);
        expected.addJoin(joinQuery.getTable(), joinQuery.getType(), joinQuery.getConditions());
        expected.addJoin(aliasJoin.getTable(), aliasJoin.getType(), aliasJoin.getConditions());

        assertEquals(query.getSQL(), expected.getSQL());
    }

    private SelectQuery<?> select() {
        return DSL.select(left.CID).from(left).getQuery();
    }

    private JoinQuery join() {
        return new JoinQuery(right, JoinType.LEFT_OUTER_JOIN, right.CID.eq(left.CID));
    }
}
