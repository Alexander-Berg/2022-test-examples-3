package ru.yandex.market.markup2.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.RandomStringUtils;
import ru.yandex.market.markup2.workflow.general.AbstractTaskDataItemPayload;
import ru.yandex.market.markup2.workflow.general.IResponseItem;

/**
 * @author york
 * @since 05.07.2017
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class DummyTestTask {

    public static class DummyTaskIdentity {
        final int id;

        @JsonCreator
        public DummyTaskIdentity(@JsonProperty("id") int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof DummyTaskIdentity)) {
                return false;
            }
            DummyTaskIdentity that = (DummyTaskIdentity) o;
            return id == that.id;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    public static class DummyTaskPayload extends AbstractTaskDataItemPayload<DummyTaskIdentity> {
        private final String data;

        public DummyTaskPayload(int id, String data) {
            this(new DummyTaskIdentity(id), data);
        }

        public DummyTaskPayload(DummyTaskIdentity dataIdentifier, String data) {
            super(dataIdentifier);
            this.data = data;
        }

        public String getData() {
            return data;
        }
    }

    public static class DummyTaskResponse implements IResponseItem {
        private final long id;
        private final String result;
        private static final String NA = "N/A";

        @JsonCreator
        public DummyTaskResponse(@JsonProperty("id") long id, @JsonProperty("result") String result) {
            this.id = id;
            this.result = result;
        }
        @Override
        public long getId() {
            return id;
        }

        @JsonIgnore
        @Override
        public boolean hasResult() {
            return !result.equals(NA);
        }

        public String getResult() {
            return result;
        }

        public boolean isResult() {
            return result.length() % 2 == 0;
        }

        public static DummyTaskResponse generateResponse(long reqId, boolean cannot) {
            return new DummyTaskResponse(reqId,
                cannot ? NA : RandomStringUtils.randomAlphabetic(4));
        }
    }

    public static class DummyResponse extends DummyTaskResponse implements IResponseItem {
        public DummyResponse(long id, String result) {
            super(id, result);
        }
    }
}
