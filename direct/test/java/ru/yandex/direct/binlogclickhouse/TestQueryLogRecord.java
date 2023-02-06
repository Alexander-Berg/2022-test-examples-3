package ru.yandex.direct.binlogclickhouse;

import java.util.Objects;

import ru.yandex.direct.binlogclickhouse.schema.QueryLogRecord;
import ru.yandex.direct.tracing.data.DirectTraceInfo;

public class TestQueryLogRecord extends QueryLogRecord {
    public TestQueryLogRecord(DirectTraceInfo directTraceInfo, String source, int querySeqNum, String query) {
        super(directTraceInfo, source, "XXX-YYY:123", "XXX-YYY", 123L, querySeqNum, null, query);
    }

    @Override
    public boolean equals(Object o) {
        QueryLogRecord that = (QueryLogRecord) o;
        return (
                Objects.equals(getDirectTraceInfo(), that.getDirectTraceInfo())
                        && Objects.equals(getSource(), that.getSource())
                        && Objects.equals(getQuerySeqNum(), that.getQuerySeqNum())
                        && Objects.equals(getQuery(), that.getQuery())
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getDirectTraceInfo(),
                getSource(),
                getQuerySeqNum(),
                getQuery()
        );
    }
}
