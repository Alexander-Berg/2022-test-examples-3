package ru.yandex.direct.logviewercore.domain.ppclog;

import java.sql.Timestamp;

@LogTable(tableName = "example_log",
        logName = "example_log",
        desc = "Requests to Direct.API",
        encodingBroken = true
)
@SuppressWarnings({"MemberName", "checkstyle:visibilitymodifier"})
public class ExampleLogRecord extends LogRecord {
    public Timestamp log_time;
    @LogField(desc = "uid or login of operator")
    public long uid;
    public long[] cid;
    public float runtime;
    public int http_status;
    @LogField(selective = true)
    public long reqid;
    public String host;
    public String token;
}
