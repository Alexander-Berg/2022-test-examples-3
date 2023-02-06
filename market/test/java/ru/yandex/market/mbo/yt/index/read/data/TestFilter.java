package ru.yandex.market.mbo.yt.index.read.data;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import ru.yandex.market.mbo.yt.index.CompositeField;
import ru.yandex.market.mbo.yt.index.ExtractableField;
import ru.yandex.market.mbo.yt.index.Field;
import ru.yandex.market.mbo.yt.index.GenericField;
import ru.yandex.market.mbo.yt.index.Operation;
import ru.yandex.market.mbo.yt.index.OperationContainer;
import ru.yandex.market.mbo.yt.index.Value;
import ru.yandex.market.mbo.yt.index.read.SearchFilter;

import static ru.yandex.market.mbo.yt.index.ValueFactory.collectionOfRawValues;
import static ru.yandex.market.mbo.yt.index.ValueFactory.collectionOfValues;
import static ru.yandex.market.mbo.yt.index.ValueFactory.rawValue;
import static ru.yandex.market.mbo.yt.index.ValueFactory.tupleOfValues;

/**
 * @author apluhin
 * @created 7/9/21
 */
public class TestFilter extends SearchFilter {

    public static Field OFFER_ID = new EventIdField();
    public static Field TIMESTAMP = new TimestampField();
    public static Field BUSINESS_ID = new QuotedField();
    public static Field OFFER_AND_TIMESTAMP = new CompositeField(OFFER_ID, TIMESTAMP);

    public TestFilter searchByIds(List<TestModel> ids) {
        updateOperation(
                new OperationContainer(OFFER_ID, collectionOfRawValues(ids), Operation.IN)
        );
        return this;
    }

    public TestFilter searchFrom(Long timestamp) {
        updateOperation(
                new OperationContainer(TIMESTAMP, rawValue(timestamp), Operation.GT)
        );
        return this;
    }

    public TestFilter searchTo(Long timestamp) {
        updateOperation(
                new OperationContainer(TIMESTAMP, rawValue(timestamp), Operation.LT)
        );
        return this;
    }

    public TestFilter searchCompositeIn(List<Pair<TestModel, Long>> idAndTimestampPairs) {
        Value values = collectionOfValues(
                idAndTimestampPairs.stream()
                        .map(it -> tupleOfValues(rawValue(it.getLeft()), rawValue(it.getRight())))
                        .collect(Collectors.toList())
        );
        updateOperation(
                new OperationContainer(
                        OFFER_AND_TIMESTAMP, values, Operation.IN
                )
        );
        return this;
    }

    public TestFilter searchByBusinessId(List<String> ids) {
        updateOperation(
                new OperationContainer(BUSINESS_ID, collectionOfRawValues(ids), Operation.IN)
        );
        return this;
    }

    private static class EventIdField extends GenericField implements ExtractableField {
        @Override
        public String ytColumn() {
            return "offer_id";
        }

        @Override
        public String extractInputValueToYt(Object value) {
            TestModel model = (TestModel) value;
            return String.valueOf(model.getId());
        }

        @Override
        public String extractInputValueToYql(Object value) {
            throw new RuntimeException("Not implemented Saas for field " + name());
        }

        @Override
        public String extractInputValueToSaas(Object value) {
            throw new RuntimeException("Not implemented Saas for field " + name());
        }
    }

    private static class TimestampField extends GenericField {
        @Override
        public String ytColumn() {
            return "timestamp";
        }
    }

    private static class QuotedField extends GenericField {
        @Override
        public String ytColumn() {
            return "business_id";
        }

        @Override
        public boolean ytQuoted() {
            return true;
        }
    }

    public static class TestModel {
        private final long id;

        public TestModel(long id) {
            this.id = id;
        }

        public long getId() {
            return id;
        }
    }
}
