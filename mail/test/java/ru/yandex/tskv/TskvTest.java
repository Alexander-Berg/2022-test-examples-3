package ru.yandex.tskv;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.http.util.nio.BasicAsyncRequestProducerGenerator;
import ru.yandex.http.util.nio.client.AsyncClient;
import ru.yandex.http.util.nio.client.SharedConnectingIOReactor;
import ru.yandex.io.StringBuilderWriter;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class TskvTest extends TestBase {
    @Test
    public void testWriter() throws Exception {
        TskvFormatConfigBuilder config = new TskvFormatConfigBuilder();
        StringBuilderWriter sbWriter = new StringBuilderWriter();
        TskvWriter writer = new TskvWriter(sbWriter, config);
        writer.write("service", "simple");
        writer.write("value", "Value\tTo\0Escape\n");
        writer.write("a=b", "All\\to escape");
        writer.close();

        String expected = "tskv\tservice=simple\t"
            + "value=Value\\\tTo\\\0Escape\\\n\t"
            + "a\\=b=All\\\\to escape";
        YandexAssert.check(new StringChecker(expected), sbWriter.toString());

        // testing multirecord

        sbWriter = new StringBuilderWriter();
        writer = new TskvWriter(sbWriter, config);
        String key1 = "key1";
        String key2 = "key2";

        writer.write(key1, "value1");
        writer.write(key2, "value2");
        writer.endRecord();
        writer.write(key1, "value3");
        writer.write(key2, "value4");
        writer.close();
        expected = "tskv\tkey1=value1\tkey2=value2\n"
            + "tskv\tkey1=value3\tkey2=value4";
        YandexAssert.check(new StringChecker(expected), sbWriter.toString());

        sbWriter = new StringBuilderWriter();
        writer = new TskvWriter(sbWriter, config);
        writer.write("key10=value10");
        writer.write("key1\t1=value1\n1");
        expected = "tskv\tkey10=value10\tkey1\\\t1=value1\\\n1";
        YandexAssert.check(new StringChecker(expected), sbWriter.toString());
    }

    @Test
    public void testConfig() throws Exception {
        TskvFormatConfigBuilder config = new TskvFormatConfigBuilder();
        config.linePrefix("prefix");
        config.escapingSymbol('/');
        config.fieldSeparator('+');
        config.recordSeparator('#');

        StringBuilderWriter sbWriter = new StringBuilderWriter();
        TskvWriter writer = new TskvWriter(sbWriter, config);
        String serviceName = "service_name";
        writer.write(serviceName, "name");
        writer.write("param_value", "Vvalue\tTo\0Escape\n");
        writer.write("c=d", "Alll\\to escape");
        writer.endRecord();
        writer.write(serviceName, "none");
        writer.flush();
        writer.close();

        String expected = "prefix+service_name=name+"
            + "param_value=Vvalue/\tTo/\0Escape/\n+"
            + "c/=d=Alll/\\to escape#"
            + "prefix+service_name=none";
        YandexAssert.check(new StringChecker(expected), sbWriter.toString());

        config = new TskvFormatConfigBuilder(config);
        config.enableEscaping(false);
        config.enableTableIndex(false);
        config.tableIndexColumn("none_index");

        sbWriter = new StringBuilderWriter();
        writer = new TskvWriter(sbWriter, config);
        writer.write(serviceName, "\tname");
        expected = "prefix+service_name=\tname";
        YandexAssert.check(new StringChecker(expected), sbWriter.toString());

        config = new TskvFormatConfigBuilder();
        config.linePrefix(null);
        sbWriter = new StringBuilderWriter();
        writer = new TskvWriter(sbWriter, config);
        writer.write(serviceName, "1");
        expected = "service_name=1";
        YandexAssert.check(new StringChecker(expected), sbWriter.toString());

        config.linePrefix("");
        sbWriter = new StringBuilderWriter();
        writer = new TskvWriter(sbWriter, config);
        writer.write(serviceName, "2");
        expected = "service_name=2";
        YandexAssert.check(new StringChecker(expected), sbWriter.toString());
    }

    @Test
    public void testRecord() throws Exception {
        TskvString record = new TskvString();
        record.append("record_key_1", "record_value_1");
        record.append("record_key_2", "record_value_2");
        String expected = "tskv\trecord_key_1=record_value_1\t"
            + "record_key_2=record_value_2";
        YandexAssert.check(new StringChecker(expected), record.toString());

        record = new TskvString(new ImmutableTskvFormatConfig(),
                                "prefix_k=prefix_v");
        record.append("record_key_3", "record_value_3");
        expected = "prefix_k=prefix_v\trecord_key_3=record_value_3";
        YandexAssert.check(new StringChecker(expected), record.toString());

        record = new TskvString();
        record.append("pk", "pv");
        record = new TskvString(record.config(), record);
        record.append("record_key_4", "record_value_4");
        expected = "tskv\tpk=pv\trecord_key_4=record_value_4";
        YandexAssert.check(new StringChecker(expected), record.toString());

        record = new TskvString();
        record.append("record_key_8", "record_value_8");
        record.append("record_key_7", "record_value_7");
        expected = "tskv\trecord_key_8=record_value_8\t"
            + "record_key_7=record_value_7";
        YandexAssert.check(new StringChecker(expected), record.toString());

        record = new TskvString();
        try {
            record.append("a", "b", "c");
            Assert.fail("Expecting exception here");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testLogger() throws Exception {
        MockLogger mockLogger = new MockLogger();
        TskvLogger logger = new TskvLogger(mockLogger);
        logger.info(
            "record_key_5",
            "record_value_5",
            "record_key_6",
            "record_value_6");

        Assert.assertEquals(1, mockLogger.output().size());
        Assert.assertEquals("tskv\trecord_key_5=record_value_5\t"
            + "record_key_6=record_value_6", mockLogger.output().get(0));

        mockLogger = new MockLogger();
        logger = new TskvLogger(mockLogger);
        logger.fine("record_key_21", "record_value_21");
        logger.severe("record_key_22", "record_value_22");
        Assert.assertEquals(
            "tskv\trecord_key_21=record_value_21",
            mockLogger.output().get(0));
        Assert.assertEquals(
            "tskv\trecord_key_22=record_value_22",
            mockLogger.output().get(1));

        mockLogger = new MockLogger();
        logger = new TskvLogger(mockLogger);
        logger.info(
            new TskvString().append("record_key_23", "record_value_23"));
        logger.fine(
            new TskvString().append("record_key_24", "record_value_24"));
        Assert.assertEquals(2, mockLogger.output().size());

        Assert.assertEquals(
            "tskv\trecord_key_23=record_value_23",
            mockLogger.output().get(0));
        Assert.assertEquals(
            "tskv\trecord_key_24=record_value_24",
            mockLogger.output().get(1));
    }

    //CSOFF: Magic Number
    @Test
    public void testParser() throws Exception {
        String source = "tskv\tservice1=simple1\t"
            + "value11=Value11\\\tTo\\\0Escape\\\n\t"
            + "a1\\=b1=All1\\\\to escape";
        StringReader reader = new StringReader(source);
        BasicTskvHandler handler = new BasicTskvHandler();
        TskvFormatConfig config = new TskvFormatConfigBuilder();
        BasicTskvParser parser = new BasicTskvParser(handler, config);
        parser.parse(reader);
        Assert.assertEquals(1, handler.records().size());
        Map<String, String> record = handler.records.get(0);
        Assert.assertEquals("simple1", record.get("service1"));
        Assert.assertEquals("Value11\tTo\0Escape\n", record.get("value11"));
        Assert.assertEquals("All1\\to escape", record.get("a1=b1"));

        handler.clear();
        String second = "prefix\tservice2=complex2\n";
        String third = "pref\\=ix4\n";
        String fourth = "service3=complex3\n";
        reader = new StringReader(source + '\n' + second + third + fourth);
        parser.parse(reader);
        Assert.assertEquals(3, handler.records().size());
        record = handler.records.get(1);
        Assert.assertEquals("complex2", record.get("service2"));
        record = handler.records.get(2);
        Assert.assertEquals("complex3", record.get("service3"));
    }

    @Test
    public void testConsumers() throws Exception {
        try (StaticServer server = new StaticServer(Configs.baseConfig());
             SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                 Configs.baseConfig(),
                 Configs.dnsConfig());
             AsyncClient client = new AsyncClient(
                 reactor,
                 Configs.targetConfig()))
        {
            TskvRecord record1 = new TskvRecord();
            record1.put("field1", "value41");
            record1.put("field2", "value72");

            TskvRecord record2 = new TskvRecord();
            record2.put("field3", "value43");
            record2.put("field52", "value52");

            final String uri = "/tskv";
            server.add(
                uri,
                record1.toString() + '\n' + record2.toString());
            reactor.start();
            server.start();
            client.start();

            List<TskvRecord> gotRecords =
                client.execute(
                    server.host(),
                    new BasicAsyncRequestProducerGenerator(uri),
                    TskvAsyncConsumerFactory.OK,
                    EmptyFutureCallback.INSTANCE).get();
            Assert.assertEquals(2, gotRecords.size());
            Assert.assertTrue(
                record1.keySet().containsAll(gotRecords.get(0).keySet()));
            Assert.assertTrue(
                record2.keySet().containsAll(gotRecords.get(1).keySet()));

            Assert.assertEquals(
                record1.toString(),
                gotRecords.get(0).toString());
            Assert.assertEquals(
                record2.toString(),
                gotRecords.get(1).toString());

            final List<TskvRecord> streamRecords = new ArrayList<>();
            client.execute(
                server.host(),
                new BasicAsyncRequestProducerGenerator(uri),
                new TskvStreamAsyncConsumerFactory((r) -> {
                    streamRecords.add(r);
                    return true;
                }),
                EmptyFutureCallback
                    .INSTANCE).get();
            Assert.assertEquals(1, streamRecords.size());
            Assert.assertTrue(
                record1.keySet().containsAll(streamRecords.get(0).keySet()));
            Assert.assertEquals(
                record1.toString(),
                streamRecords.get(0).toString());
        }
    }

    private static class BasicTskvHandler
        extends AbstractTskvHandler<TskvRecord>
    {
        private final List<Map<String, String>> records;

        BasicTskvHandler() {
            this.records = new ArrayList<>();
        }

        @Override
        public boolean onRecord(final TskvRecord record) {
            records.add(record);
            return true;
        }

        List<Map<String, String>> records() {
            return records;
        }

        void clear() {
            records.clear();
        }
    }

    //CSON: Magic Number
}
