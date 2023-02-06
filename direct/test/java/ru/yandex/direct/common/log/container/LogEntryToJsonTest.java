package ru.yandex.direct.common.log.container;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;

import ru.yandex.direct.utils.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class LogEntryToJsonTest {
    private static final TypeReference<HashMap<String, String>> MAP_TYPE_REFERENCE =
            new TypeReference<HashMap<String, String>>() {
            };

    @Test
    @SuppressWarnings("unchecked")
    public void logEntryToJson_convertedCorrectly_whenUidSet() throws Exception {
        LogEntry<String> logEntry = new LogEntry<String>()
                .withUid(0L)
                .withMethod("methodName")
                .withService("serviceName")
                .withIp(InetAddress.getByName("127.0.0.1"))
                .withReqId(123)
                .withLogHostname("logHostName")
                .withLogTime("logTime")
                .withLogType(new LogType("testType"))
                .withData("stringPayload");


        Map<String, String> actual = JsonUtils.fromJson(JsonUtils.toJson(logEntry), MAP_TYPE_REFERENCE);

        assertThat(actual).containsOnly(
                entry("uid", "0"),
                entry("method", "methodName"),
                entry("service", "serviceName"),
                entry("ip", "127.0.0.1"),
                entry("reqid", "123"),
                entry("log_hostname", "logHostName"),
                entry("log_time", "logTime"),
                entry("log_type", "testType"),
                entry("data", "stringPayload")
        );
    }

    @Test
    public void logEntryToJson_convertedCorrectly_whenUidIsNull() throws Exception {
        LogEntry<String> logEntry = new LogEntry<String>()
                .withUid(null)
                .withMethod("methodName")
                .withService("serviceName")
                .withIp(InetAddress.getByName("127.0.0.1"))
                .withReqId(123)
                .withLogHostname("logHostName")
                .withLogTime("logTime")
                .withLogType(new LogType("testType"))
                .withData("stringPayload");

        Map<String, String> actual = JsonUtils.fromJson(JsonUtils.toJson(logEntry), MAP_TYPE_REFERENCE);

        assertThat(actual).containsOnly(
                entry("method", "methodName"),
                entry("service", "serviceName"),
                entry("ip", "127.0.0.1"),
                entry("reqid", "123"),
                entry("log_hostname", "logHostName"),
                entry("log_time", "logTime"),
                entry("log_type", "testType"),
                entry("data", "stringPayload")
        );
    }
}
