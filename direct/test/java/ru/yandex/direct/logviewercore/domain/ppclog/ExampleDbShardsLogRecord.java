package ru.yandex.direct.logviewercore.domain.ppclog;

import java.sql.Timestamp;

@LogTable(tableName = "dbshards_ids", logName = "example_dbshards_log")
@SuppressWarnings({"MemberName", "checkstyle:visibilitymodifier"})
public class ExampleDbShardsLogRecord extends LogRecord {
    public Timestamp log_time;
    public String source;
    public String host;
    public long reqid;
    public String key;
    public long[] ids;
    public String insert_data;
}
