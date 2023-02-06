package ru.yandex.market.api.cpa.yam.dao.filter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import ru.yandex.market.api.cpa.yam.entity.RequestType;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.supplier.prepay.PartnerApplicationKey;
import ru.yandex.market.mbi.util.db.jdbc.TNumberTbl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.ARRAY;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;

/**
 * Unit тесты для {@link PrepayRequestFilter}.
 *
 * @author avetokhin 04/04/17.
 */
class PrepayRequestFilterTest {
    @Test
    void testEmpty() {
        var builder = PrepayRequestFilter.newBuilder();
        assertThatSatisfies(builder, "", Map.of());
    }

    @Test
    void testSql() {
        var builder = PrepayRequestFilter.newBuilder();
        var paramsExpected = new HashMap<String, Object>();

        builder.addRequestId(10L);
        paramsExpected.put("fREQUEST_ID_array_0", 10L);
        assertThatSatisfies(
                builder,
                "f.REQUEST_ID = :fREQUEST_ID_array_0",
                paramsExpected
        );

        builder.addStatus(PartnerApplicationStatus.IN_PROGRESS);
        paramsExpected.put("fSTATUS_array_0", 1);
        assertThatSatisfies(
                builder,
                "(f.REQUEST_ID = :fREQUEST_ID_array_0 AND f.STATUS = :fSTATUS_array_0)",
                paramsExpected
        );

        builder.addDatasources(Arrays.asList(1L, 2L, null));
        paramsExpected.put("fDATASOURCE_ID_array_0", new TNumberTbl(List.of(1L, 2L)));
        assertThatSatisfies(
                builder,
                "((f.REQUEST_ID = :fREQUEST_ID_array_0 AND f.STATUS = :fSTATUS_array_0) AND  (f.DATASOURCE_ID IN (select /*+ cardinality(t 1)*/ value(t) FROM TABLE (cast(:fDATASOURCE_ID_array_0 AS shops_web.t_number_tbl)) t)))",
                paramsExpected
        );

        builder.addPrepayType(PrepayType.YANDEX_MARKET);
        paramsExpected.put("fPREPAY_TYPE", 1);
        assertThatSatisfies(
                builder,
                "(((f.REQUEST_ID = :fREQUEST_ID_array_0 AND f.STATUS = :fSTATUS_array_0) AND  (f.DATASOURCE_ID IN (select /*+ cardinality(t 1)*/ value(t) FROM TABLE (cast(:fDATASOURCE_ID_array_0 AS shops_web.t_number_tbl)) t))) AND f.PREPAY_TYPE = :fPREPAY_TYPE)",
                paramsExpected
        );

        builder.addRequestType(RequestType.MARKETPLACE);
        paramsExpected.put("fREQUEST_TYPE", "MARKETPLACE");
        assertThatSatisfies(
                builder,
                "((((f.REQUEST_ID = :fREQUEST_ID_array_0 AND f.STATUS = :fSTATUS_array_0) AND  (f.DATASOURCE_ID IN (select /*+ cardinality(t 1)*/ value(t) FROM TABLE (cast(:fDATASOURCE_ID_array_0 AS shops_web.t_number_tbl)) t))) AND f.PREPAY_TYPE = :fPREPAY_TYPE) AND f.REQUEST_TYPE = :fREQUEST_TYPE)",
                paramsExpected
        );

    }

    @Test
    void tuple2() {
        var builder = PrepayRequestFilter.newBuilder();

        builder.addStatuses(List.of(PartnerApplicationStatus.IN_PROGRESS));
        assertThatSatisfies(
                builder,
                "f.STATUS = :fSTATUS_array_0",
                Map.of("fSTATUS_array_0", 1)
        );

        builder.addStatuses(List.of(PartnerApplicationStatus.NEED_INFO));
        assertThatSatisfies(
                builder,
                " (f.STATUS IN (select /*+ cardinality(t 1)*/ value(t) FROM TABLE (cast(:fSTATUS_array_0 AS shops_web.t_number_tbl)) t))",
                p -> assertThat(p).extractingByKey("fSTATUS_array_0")
                        .isInstanceOfSatisfying(TNumberTbl.class, tNumberTbl ->
                                assertThat(tNumberTbl.get()).containsExactlyInAnyOrder(1L, 8L))
        );

        builder.addApplicationKeys(List.of(new PartnerApplicationKey(1, 2)));
        assertThatSatisfies(
                builder,
                "( (f.STATUS IN (select /*+ cardinality(t 1)*/ value(t) FROM TABLE (cast(:fSTATUS_array_0 AS shops_web.t_number_tbl)) t)) AND  ( (f.DATASOURCE_ID, f.REQUEST_ID) IN (:applicationKeys)) )",
                p -> assertThat(p).extractingByKey("applicationKeys")
                    .asInstanceOf(LIST).first()
                    .asInstanceOf(ARRAY).containsExactly(2L, 1L)
        );
    }

    static void assertThatSatisfies(PrepayRequestFilter.Builder builder, String sql, Map<String, Object> params) {
        assertThatSatisfies(builder, sql, p -> assertThat(p).isEqualTo(params));
    }

    static void assertThatSatisfies(
            PrepayRequestFilter.Builder builder,
            String sql,
            Consumer<Map<String, Object>> assertOnParams
    ) {
        var filter = builder.build();
        assertThat(filter).satisfies(f -> {
            assertThat(f).hasToString(sql);
            assertOnParams.accept(f.toParameters());
        });
    }
}
