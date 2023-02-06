package ru.yandex.direct.binlogclickhouse;

import java.util.Objects;

import ru.yandex.direct.binlogclickhouse.schema.DbChangeLogRecord;
import ru.yandex.direct.binlogclickhouse.schema.FieldValueList;
import ru.yandex.direct.binlogclickhouse.schema.Operation;
import ru.yandex.direct.tracing.data.DirectTraceInfo;

public class TestDbChangeLogRecord extends DbChangeLogRecord {
    @SuppressWarnings("checkstyle:parameternumber")
    public TestDbChangeLogRecord(DirectTraceInfo directTraceInfo, String source, String db, String table,
                                 Operation operation, int querySeqNum, int changeSeqNum,
                                 FieldValueList primaryKey, FieldValueList row) {
        super(directTraceInfo, source, db, table, operation, "XXX-YYY:2342", "XXX-YYY",
                2342L, querySeqNum, changeSeqNum, primaryKey, null, row);
    }

    @Override
    public boolean equals(Object o) {
        DbChangeLogRecord that = (DbChangeLogRecord) o;
        return (
                Objects.equals(getDirectTraceInfo(), that.getDirectTraceInfo())
                        && Objects.equals(getDb(), that.getDb())
                        && Objects.equals(getSource(), that.getSource())
                        && Objects.equals(getTable(), that.getTable())
                        && Objects.equals(getOperation(), that.getOperation())
                        && Objects.equals(getQuerySeqNum(), that.getQuerySeqNum())
                        && Objects.equals(getChangeSeqNum(), that.getChangeSeqNum())
                        && Objects.equals(getPrimaryKey(), that.getPrimaryKey())
                        && Objects.equals(getRow(), that.getRow())
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getDirectTraceInfo(),
                getDb(),
                getSource(),
                getTable(),
                getOperation(),
                getQuerySeqNum(),
                getChangeSeqNum(),
                getPrimaryKey(),
                getRow()
        );
    }
}
