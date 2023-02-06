package ru.yandex.logger;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.parser.config.IniConfig;

public class TestJsonLogger {
    private static final String UTF8 = "UTF-8";

    @Test
    public void testMultipleFiles() throws Exception {
        final String config =
            "default.format=%{remote_addr} %{user} %{status}\n"
                + "yt.output-format=json\n"
                + "yt.format=%{session_id} %{env.service.SERVICE}";

        PrintStream err = System.err;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            System.setErr(new PrintStream(out, true, UTF8));
            LoggerConfigBuilder configBuilder =
                new LoggerConfigBuilder(
                    new IniConfig(
                        new StringReader(config)));
            HandlersManager manager = new HandlersManager();
            Logger logger = configBuilder.build().build(manager);
            LogRecord record = new LogRecord(Level.ALL, "MyMessage");
            Object[] params = new Object[]{new SimpleLokupable()};
            record.setParameters(params);
            logger.log(record);
            synchronized (out) {
                Assert.assertEquals(
                    "127.0.0.1 - -\n"
                        + "{\"session_id\":\"ABIRVALG\"}\n",
                    out.toString(UTF8));
            }
        } finally {
            System.setErr(err);
        }
    }

    @Test
    public void testFormat() throws Exception {
        LoggerFileConfigBuilder fileConfig =
            new LoggerFileConfigBuilder(AccessLoggerConfigDefaults.INSTANCE);
        fileConfig.outputFormat(FormatParser.JSON);
        fileConfig.logFormat(
            "%{sdafa} %{env.service-name.NANNY_SERVICE_ID} %{request} "
                + "%{remote_addr} %{user} %{status} "
                + "%{response_length} %{request_time} %{session_id}");
        LoggerConfigBuilder configBuilder = new LoggerConfigBuilder();
        configBuilder.add(fileConfig);

        ImmutableLoggerConfig config = configBuilder.build();
        LogRecord record = new LogRecord(Level.ALL, "Message");
        Object[] params = new Object[]{new SimpleLokupable()};
        record.setParameters(params);

        PrintStream err = System.err;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            System.setErr(new PrintStream(out, true, UTF8));
            HandlersManager manager = new HandlersManager();
            Logger logger = config.build(manager);
            logger.log(record);
            synchronized (out) {
                Assert.assertEquals(
                    "{\"remote_addr\":\"127.0.0.1\","
                        + "\"response_length\":\"10050\","
                        + "\"request_time\":\"243532\","
                        + "\"session_id\":\"ABIRVALG\"}\n",
                    out.toString(UTF8));
            }
        } finally {
            System.setErr(err);
        }
    }

    private static final class SimpleLokupable implements Lookup<String> {
        @Override
        public String lookup(final String name) {
            String result = null;
            switch (name) {
                case "response_length":
                    result = "10050";
                    break;
                case "session_id":
                    result = "ABIRVALG";
                    break;
                case "request_time":
                    result = "243532";
                    break;
                case "remote_addr":
                    result = "127.0.0.1";
                    break;
                default:
                    break;
            }

            return result;
        }
    }
}
