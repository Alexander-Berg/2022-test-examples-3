package ru.yandex.market.logshatter.reader.logbroker;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.health.configs.logshatter.config.LogShatterConfig;
import ru.yandex.market.health.configs.logshatter.config.LogSource;
import ru.yandex.market.logshatter.logs.parser.utils.ParamUtils;
import ru.yandex.market.logshatter.parser.LogParserProvider;
import ru.yandex.market.logshatter.parser.ProtoConverterProvider;
import ru.yandex.market.logshatter.parser.front.errorBooster.universal.ErrorsParser;
import ru.yandex.market.logshatter.reader.logbroker.LbReadingTester.Session.Partition;

import static ru.yandex.market.logshatter.reader.logbroker.LbReadingTester.configThatMatchesEverything;
import static ru.yandex.market.logshatter.reader.logbroker.LbReadingTester.messageData;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * <br>
 * Date: 23.01.2019
 */
public class LogBrokerReaderServiceReadTest {
    @Rule
    public final Timeout timeout = new Timeout(10, TimeUnit.SECONDS);

    private final LbReadingTester tester = new LbReadingTester();

    @Test
    public void noConfigs() {
        Partition partition = tester.givenStartedSessionWithLockedPartition();
        partition.lbSendsData(1, messageData(2));
        tester.verifyInteractions(partition.lbReceivedCommit(1));
    }

    @Test
    public void skippedBecauseOfSeqNo() {
        Partition partition = tester.givenStartedSessionWithLockedPartition(configThatMatchesEverything());

        partition.lbSendsData(1, messageData(10, "a"));

        tester.runParsers();
        tester.runWriters(1);
        tester.verifyInteractions(
            tester.clickHouseReceivedData(),
            partition.mongoReceivedSeqNo("a", 10),
            partition.lbReceivedCommit(1)
        );

        partition.lbSendsData(2, messageData(5, "a"));
        tester.verifyInteractions(
            partition.lbReceivedCommit(1)
        );
    }

    @Test
    public void readBatchWithoutSource() {
        Partition partition = tester.givenStartedSessionWithLockedPartition(configThatMatchesEverything());

        partition.lbSendsData(1, messageData(1, "a", null));

        tester.runParsers();
        tester.runWriters(1);
        tester.verifyInteractions(
            tester.clickHouseReceivedData(),
            partition.mongoReceivedSeqNo("a", 1),
            partition.lbReceivedCommit(1)
        );
    }

    @Test
    public void oneReadBatch_twoMessages_oneConfig() {
        Partition partition = tester.givenStartedSessionWithLockedPartition(configThatMatchesEverything());

        partition.lbSendsData(
            1,
            messageData(2, "a"),
            messageData(3, "b")
        );
        tester.verifyNoInteractions();

        tester.runParsers();
        tester.runParsers();
        tester.verifyNoInteractions();

        tester.runWriters(1);
        tester.verifyInteractions(
            tester.clickHouseReceivedData()
        );

        tester.runWriters(1);
        tester.verifyInteractions(
            tester.clickHouseReceivedData(),
            partition.mongoReceivedSeqNo("a", 2),
            partition.mongoReceivedSeqNo("b", 3),
            partition.lbReceivedCommit(1)
        );
    }

    @Test
    public void oneReadBatch_oneMessage_twoConfigs() {
        Partition partition = tester.givenStartedSessionWithLockedPartition(
            configThatMatchesEverything(), configThatMatchesEverything()
        );

        partition.lbSendsData(
            1,
            messageData(1, "a")
        );
        tester.verifyNoInteractions();

        tester.runParsers();
        tester.runParsers();
        tester.verifyNoInteractions();

        tester.runWriters(1);
        tester.verifyInteractions(
            tester.clickHouseReceivedData()
        );

        tester.runWriters(1);
        tester.verifyInteractions(
            tester.clickHouseReceivedData(),
            partition.mongoReceivedSeqNo("a", 1),
            partition.lbReceivedCommit(1)
        );
    }

    @Test
    public void protobufToJsonTest() {
        String base64data =
            "CgVzdGFjaxICZGMaBWxldmVsIgVzbG90cyoFcmVxaWQyBm1ldGhvZDoGc291cmNlQgxzb3VyY2VNZXRob2RKCnNvdXJjZVR5cGVQA" +
                "FgAYgRob3N0agRmaWxlcgdtZXNzYWdlegdwcm9qZWN0ggEHc2VydmljZYoBAmlwkgELZXhwZXJpbWVudHOaAQhwbGF0Zm9yba" +
                "IBBWJsb2NrqgEIbGFuZ3VhZ2WyAQZyZWdpb266AQd2ZXJzaW9uwgEJeWFuZGV4dWlkygEDZW520gEJdXNlcmFnZW502AHo8re" +
                "b3y3iAQN1cmzqAQ4KBGtleTESBnZhbHVlMeoBDgoEa2V5MhIGdmFsdWUy8gEEcGFnZQ==";
        byte[] data = Base64.getDecoder().decode(base64data.getBytes(StandardCharsets.US_ASCII));

        Partition partition;
        try {
            partition = tester.givenStartedSessionWithLockedPartition(
                LogShatterConfig.newBuilder()
                    .setConfigId("/1")
                    .setLogPath("**")
                    .setParams(new HashMap<>())
                    .setSources(Collections.singletonList(LogSource.create("logbroker://megamind--error-log")))
                    .setLogHosts("*")
                    .setDataClickHouseTable(new ClickHouseTableDefinitionImpl("db.first", Collections.emptyList(),
                        null))
                    .setParserProvider(new LogParserProvider(ErrorsParser.class.getName(), null, null))
                    .setProtoConverterProvider(new ProtoConverterProvider("ru.yandex.market.logshatter.parser.front" +
                        ".errorBooster.universal.ErrorsProtoConverter"))
            );
        } catch (Exception e) {
            partition = null;
        }

        partition.lbSendsData(
            1,
            messageData(
                1,
                "c",
                CompressionCodec.RAW,
                data
            )
        );

        tester.runParsers();
        tester.runWriters(1);

        tester.verifyInteractions(
            tester.clickHouseReceivedData(),
            partition.mongoReceivedSeqNo("c", 1),
            partition.lbReceivedCommit(1)
        );
    }

    @Test
    public void oneReadBatch_oneMessage_firstConfigWithClusterIdAndSecondWithoutClusterId() {
        Partition partition = tester.givenStartedSessionWithLockedPartition(
            configThatMatchesEverything(), configThatMatchesEverything().setClickHouseClusterId("mdb1")
        );

        partition.lbSendsData(
            1,
            messageData(1, "a")
        );
        tester.verifyInternalAndExternalInteractionsOnReadData(1, 1, 2, 2, 2, 1, 1, 0);

        tester.runInternalAndExternalParsersAsync();
        tester.verifyInternalAndExternalInteractionsOnParseData(2, 2, 1, 1, 1, 1, 1, 1);

        tester.runInternalAndExternalWritersAsync();
        tester.verifyInternalAndExternalInteractionsOnWriteData(1, 1, 1, 1, 3, 3, 2, 1);
    }

    @Test
    public void oneReadBatch_skipMessageBecauseOfLogPathMismatch() {
        Partition partition = tester.givenStartedSessionWithLockedPartition(
            configThatMatchesEverything().setLogPath("some/log.log")
        );

        partition.lbSendsData(
            1,
            messageData(1, "a"),
            messageData(2, "b"),
            messageData(3, "c")
        );
        tester.verifyInternalAndExternalInteractionsOnReadData(0, 0, 0, 0, 0, 0, 0, 1);
    }

    @Test
    public void oneReadBatch_severalMessages_oneConfigWithoutClusterId() {
        Partition partition = tester.givenStartedSessionWithLockedPartition(
            configThatMatchesEverything()
        );

        partition.lbSendsData(
            1,
            messageData(1, "a"),
            messageData(2, "b"),
            messageData(3, "c")
        );

        tester.verifyInternalAndExternalInteractionsOnReadData(1, 0, 1, 4, 0, 3, 0, 0);

        tester.runInternalAndExternalParsersAsync();
        tester.runInternalParserAsync();
        tester.runInternalParserAsync();
        tester.verifyInternalAndExternalInteractionsOnParseData(6, 1, 3, 0, 3, 0, 3, 0);

        tester.runInternalAndExternalWritersAsync();
        tester.verifyInternalAndExternalInteractionsOnWriteData(1, 0, 3, 0, 3, 1, 1, 0);
    }

    @Test
    public void oneReadBatch_skipMessageIgnoreLogPaths() {
        Partition partition = tester.givenStartedSessionWithLockedPartition(
            configThatMatchesEverything()
                .setIgnoreLogPaths(Arrays.asList(
                    "some/ignore-*-log.log",
                    "some/ignore2-*-log.log"
                ))
        );

        partition.lbSendsData(
            1,
            messageData(1, "a", "some/ignore-test-log.log")
        );
        tester.verifyInteractions(partition.lbReceivedCommit(1));

        partition.lbSendsData(
            2,
            messageData(2, "a", "some/ignore2-test-log.log")
        );
        tester.verifyInteractions(partition.lbReceivedCommit(2));

        partition.lbSendsData(
            3,
            messageData(3, "a", "some/read-test-log.log")
        );
        tester.runParsers();
        tester.runWriters(1);
        tester.verifyInteractions(
            tester.clickHouseReceivedData(),
            partition.mongoReceivedSeqNo("a", 3),
            partition.lbReceivedCommit(3)
        );
    }

    @Test
    public void whenMultiLineEnabledShouldParseWholeMessage() {
        String message = "[2022-05-12 17:15:47,192] INFO  [ForkJoinPool.commonPool-worker-5] " +
            "1652364947076/6bf0d0191421219f2eb4b531d1de0500/7/2/1 line1\nline2\nline3";
        byte[] data = message.getBytes(StandardCharsets.UTF_8);

        ArrayList<String> parserInput = new ArrayList<>();

        Partition partition;
        try {
            partition = tester.givenStartedSessionWithLockedPartition(
                LogShatterConfig.newBuilder()
                    .setConfigId("/1")
                    .setLogPath("**")
                    .setParams(new HashMap<>() {{
                        put(ParamUtils.MULTI_LINE_PARAMETER, "true");
                    }})
                    .setSources(Collections.singletonList(LogSource.create("logbroker://megamind--error-log")))
                    .setLogHosts("*")
                    .setDataClickHouseTable(new ClickHouseTableDefinitionImpl("db.first", Collections.emptyList(),
                        null))
                    .setParserProvider(tester.parserCachingInputTo(parserInput))
            );
        } catch (Exception e) {
            partition = null;
        }

        partition.lbSendsData(
            1,
            messageData(
                1,
                "c",
                CompressionCodec.RAW,
                data
            )
        );

        tester.runParsers();
        tester.runWriters(1);

        tester.verifyInteractions(
            tester.clickHouseReceivedData(),
            partition.mongoReceivedSeqNo("c", 1),
            partition.lbReceivedCommit(1)
        );

        Assertions.assertThat(parserInput.get(0)).isEqualTo(message);
    }

    @Test
    public void whenMultiLineParameterNotSetShouldSplitMessage() {
        String message = "[2022-05-12 17:15:47,192] INFO line1\nline2\nline3";
        byte[] data = message.getBytes(StandardCharsets.UTF_8);

        ArrayList<String> parserInput = new ArrayList<>();

        Partition partition;
        try {
            partition = tester.givenStartedSessionWithLockedPartition(
                LogShatterConfig.newBuilder()
                    .setConfigId("/1")
                    .setLogPath("**")
                    .setParams(new HashMap<>())
                    .setSources(Collections.singletonList(LogSource.create("logbroker://megamind--error-log")))
                    .setLogHosts("*")
                    .setDataClickHouseTable(new ClickHouseTableDefinitionImpl("db.first", Collections.emptyList(),
                        null))
                    .setParserProvider(tester.parserCachingInputTo(parserInput))
            );
        } catch (Exception e) {
            partition = null;
        }

        partition.lbSendsData(
            1,
            messageData(
                1,
                "c",
                CompressionCodec.RAW,
                data
            )
        );

        tester.runParsers();
        tester.runWriters(1);

        tester.verifyInteractions(
            tester.clickHouseReceivedData(),
            partition.mongoReceivedSeqNo("c", 1),
            partition.lbReceivedCommit(1)
        );

        Assertions.assertThat(parserInput).contains(
            "[2022-05-12 17:15:47,192] INFO line1",
            "line2",
            "line3"
        );
    }
}
