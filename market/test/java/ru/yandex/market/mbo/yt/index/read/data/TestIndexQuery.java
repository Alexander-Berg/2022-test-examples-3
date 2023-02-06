package ru.yandex.market.mbo.yt.index.read.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.market.mbo.yt.index.Field;
import ru.yandex.market.mbo.yt.index.Operation;
import ru.yandex.market.mbo.yt.index.OperationContainer;
import ru.yandex.market.mbo.yt.index.read.SearchFilter;
import ru.yandex.market.mbo.yt.index.read.YtIndexQuery;

import static ru.yandex.market.mbo.yt.index.Operation.GT;
import static ru.yandex.market.mbo.yt.index.Operation.IN;
import static ru.yandex.market.mbo.yt.index.Operation.LT;
import static ru.yandex.market.mbo.yt.index.read.data.TestFilter.BUSINESS_ID;
import static ru.yandex.market.mbo.yt.index.read.data.TestFilter.OFFER_AND_TIMESTAMP;
import static ru.yandex.market.mbo.yt.index.read.data.TestFilter.OFFER_ID;
import static ru.yandex.market.mbo.yt.index.read.data.TestFilter.TIMESTAMP;

/**
 * @author apluhin
 * @created 7/9/21
 */
public class TestIndexQuery extends YtIndexQuery {

    private static final List<Field> SORT_FIELDS =
            Arrays.asList(OFFER_ID);

    public TestIndexQuery(SearchFilter filter) {
        super(filter);
    }

    @Override
    public String query(Boolean forSelect) {
        String s = build(OFFER_ID, IN)
                + and(TIMESTAMP, GT)
                + and(TIMESTAMP, LT)
                + and(OFFER_AND_TIMESTAMP, IN)
                + and(BUSINESS_ID, IN);

        if (forSelect) {
            s += offsetLimitOrderBy();
        }
        return fixFirstOperator(s);
    }

    public static Boolean isSupportFilter(SearchFilter filter) {
        Optional<OperationContainer> operation = filter.getOperation(OFFER_ID, IN);
        boolean usesMainIndexedField = operation.isPresent();
        boolean filterResult = filter.correctFilter(availableFiltersFilters());
        return usesMainIndexedField && filterResult && baseSupport(filter, SORT_FIELDS);
    }

    private static List<Tuple2<Field, Operation>> availableFiltersFilters() {
        List<Tuple2<Field, Operation>> ops = new ArrayList<>();
        ops.add(Tuple2.tuple(OFFER_ID, IN));
        ops.add(Tuple2.tuple(TIMESTAMP, GT));
        ops.add(Tuple2.tuple(TIMESTAMP, LT));
        return ops;
    }

}
