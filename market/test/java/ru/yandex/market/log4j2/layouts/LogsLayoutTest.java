package ru.yandex.market.log4j2.layouts;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent.Builder;
import org.apache.logging.log4j.message.SimpleMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class LogsLayoutTest {
    @Test
    public void base() throws JSONException {
        final LogsLayout layout = LogsLayout.createSimpleLayout(
            "test_project",
            "test_service"
        );
        Log4jLogEvent event = getBaseEvent().build();
        String actual = layout.toSerializable(event);
        JSONObject obj = new JSONObject(actual);
        Assert.assertEquals(obj.getString("time"), "2012-11-02T10:34:02.123Z");
        Assert.assertEquals(obj.getString("project"), "test_project");
        Assert.assertEquals(obj.getString("service"), "test_service");
        Assert.assertFalse(obj.has("version"));
        Assert.assertFalse(obj.has("cluster"));
        Assert.assertFalse(obj.has("env"));
        Assert.assertEquals(obj.getString("message"), "msg");
        Assert.assertEquals(obj.getString("level"), "DEBUG");
        Assert.assertEquals(obj.getString("message"), "msg");
        Assert.assertEquals(obj.getString("component"), "loggerName");
        Assert.assertEquals(obj.getString("thread"), "threadName");
        Assert.assertEquals(obj.getString("hostname"), HostnameUtils.getHostname());
        Assert.assertEquals(obj.getString("dc"), HostnameUtils.getDatacenter());
    }

    @Test
    public void full() throws JSONException {
        final LogsLayout layout = LogsLayout.createLayout(
            "test_project",
            "test_service",
            "test_environment",
            "test_cluster",
            "test_version"
        );
        Log4jLogEvent event = getBaseEvent().build();
        String actual = layout.toSerializable(event);
        JSONObject obj = new JSONObject(actual);

        Assert.assertEquals(obj.getString("time"), "2012-11-02T10:34:02.123Z");
        Assert.assertEquals(obj.getString("project"), "test_project");
        Assert.assertEquals(obj.getString("service"), "test_service");
        Assert.assertEquals(obj.getString("env"), "test_environment");
        Assert.assertEquals(obj.getString("cluster"), "test_cluster");
        Assert.assertEquals(obj.getString("version"), "test_version");
        Assert.assertEquals(obj.getString("message"), "msg");
        Assert.assertEquals(obj.getString("level"), "DEBUG");
        Assert.assertEquals(obj.getString("message"), "msg");
        Assert.assertEquals(obj.getString("component"), "loggerName");
        Assert.assertEquals(obj.getString("thread"), "threadName");
        Assert.assertEquals(obj.getString("hostname"), HostnameUtils.getHostname());
        Assert.assertEquals(obj.getString("dc"), HostnameUtils.getDatacenter());
    }

    private Builder getBaseEvent() {
        return Log4jLogEvent.newBuilder()
            .setLevel(Level.DEBUG)
            .setMessage(new SimpleMessage("msg"))
            .setThreadName("threadName")
            .setLoggerName("loggerName")
            .setTimeMillis(1351852442123L);
    }
}
