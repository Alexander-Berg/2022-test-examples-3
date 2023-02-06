package ru.yandex.market.tsum.clients.conductor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.TestConductor;
import ru.yandex.market.tsum.event.MicroEvent;


/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 17.11.16
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConductor.class})
public class ConductorLogParserTest {
    @Autowired
    private ConductorClient client;

    @Autowired
    private ConductorLogParser parser;

    @Test
    public void parseLogs() throws URISyntaxException, ExecutionException, InterruptedException, IOException,
        ParseException {
        // setup format time for logs
        final SimpleDateFormat conductorApiDateFormat = new SimpleDateFormat(ConductorParser.DATE_FORMAT);
        final SimpleDateFormat conductorAgentLogFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        // logshater stable
        List<ConductorTaskLog> logs = ConductorParser.getLogs(readFile("logs/logshatterStableLog.json"));
        Set<MicroEvent> actualEvents = new HashSet<>(parser.parseLogs(logs));
        Set<MicroEvent> expectedEvents = new HashSet<>();
        MicroEvent.Builder microEventBuilder = MicroEvent.newBuilder();
        microEventBuilder.setProject("cs");

        expectedEvents.addAll(Arrays.asList(
            microEventBuilder
                .setTimeSeconds(toTimeStamp("2016-10-31T23:09:41+03:00", conductorApiDateFormat))
                .setText("Installing on blacksmith01g.market.yandex.net, blacksmith01h.market.yandex.net, " +
                    "blacksmith01e.market.yandex.net")
                .setType("install")
                .addTags("host:blacksmith01g.market.yandex.net")
                .addTags("host:blacksmith01h.market.yandex.net")
                .addTags("host:blacksmith01e.market.yandex.net")
                .addTags("package:yandex-logshatter:2.1-144")
                .build(),
            microEventBuilder
                .setTimeSeconds(toTimeStamp("2016-10-31T23:09:51+03:00", conductorApiDateFormat))
                .setText("Restarting logshatter on blacksmith01h.market.yandex.net,blacksmith01e.market.yandex.net," +
                    "blacksmith01g.market.yandex.net")
                .setType("restart")
                .clearTags()
                .addTags("host:blacksmith01h.market.yandex.net")
                .addTags("host:blacksmith01e.market.yandex.net")
                .addTags("host:blacksmith01g.market.yandex.net")
                .addTags("package:logshatter")
                .build()
        ));
        Assert.assertEquals(expectedEvents, actualEvents);

        // tsum stable
        logs = ConductorParser.getLogs(readFile("logs/tsumStableLog.json"));
        actualEvents = new HashSet<>(parser.parseLogs(logs));
        expectedEvents.clear();
        expectedEvents.addAll(Arrays.asList(
            microEventBuilder
                .setType("restart")
                .setText("Running  /etc/init.d/tsum-api restart")
                .setTimeSeconds(toTimeStamp("2016-11-14T17:50:13", conductorAgentLogFormat))
                .clearTags()
                .addTags("host:blacksmith01e.market.yandex.net")
                .addTags("package:yandex-market-tsum-api:1.0-9")
                .build(),
            microEventBuilder
                .setTimeSeconds(toTimeStamp("2016-11-14T17:49:35", conductorAgentLogFormat))
                .clearTags()
                .addTags("host:blacksmith01h.market.yandex.net")
                .addTags("package:yandex-market-tsum-api:1.0-9")
                .build(),
            microEventBuilder
                .setTimeSeconds(toTimeStamp("2016-11-14T17:48:55", conductorAgentLogFormat))
                .clearTags()
                .addTags("host:blacksmith01g.market.yandex.net")
                .addTags("package:yandex-market-tsum-api:1.0-9")
                .build(),
            microEventBuilder
                .setTimeSeconds(toTimeStamp("2016-11-14T17:48:57", conductorAgentLogFormat))
                .setText("Running /etc/init.d/tsum-ui restart")
                .clearTags()
                .addTags("host:blacksmith01g.market.yandex.net")
                .addTags("package:yandex-market-tsum-ui:1.0-29")
                .build(),
            microEventBuilder
                .setTimeSeconds(toTimeStamp("2016-11-14T17:49:37", conductorAgentLogFormat))
                .clearTags()
                .addTags("host:blacksmith01h.market.yandex.net")
                .addTags("package:yandex-market-tsum-ui:1.0-29")
                .build(),
            microEventBuilder
                .setTimeSeconds(toTimeStamp("2016-11-14T17:50:15", conductorAgentLogFormat))
                .clearTags()
                .addTags("host:blacksmith01e.market.yandex.net")
                .addTags("package:yandex-market-tsum-ui:1.0-29")
                .build(),
            microEventBuilder
                .setTimeSeconds(toTimeStamp("2016-11-14T17:48:53", conductorAgentLogFormat))
                .setText("Running /etc/init.d/tsum-tms restart")
                .clearTags()
                .addTags("host:blacksmith01g.market.yandex.net")
                .addTags("package:yandex-market-tsum-tms:1.0-9")
                .build(),
            microEventBuilder
                .setTimeSeconds(toTimeStamp("2016-11-14T17:50:11", conductorAgentLogFormat))
                .clearTags()
                .addTags("host:blacksmith01e.market.yandex.net")
                .addTags("package:yandex-market-tsum-tms:1.0-9")
                .build(),
            microEventBuilder
                .setTimeSeconds(toTimeStamp("2016-11-14T17:49:33", conductorAgentLogFormat))
                .clearTags()
                .addTags("host:blacksmith01h.market.yandex.net")
                .addTags("package:yandex-market-tsum-tms:1.0-9")
                .build(),
            microEventBuilder
                .setTimeSeconds(toTimeStamp("2016-11-14T17:48:44", conductorAgentLogFormat))
                .setText("'apt-get', 'install', '-o', 'APT::Get::Assume-Yes=False', 'yandex-market-tsum-api=1.0-9', " +
                    "'yandex-market-tsum-ui=1.0-29', 'yandex-market-tsum-tms=1.0-9'")
                .setType("install")
                .clearTags()
                .addTags("host:blacksmith01h.market.yandex.net")
                .addTags("package:yandex-market-tsum-api:1.0-9")
                .addTags("package:yandex-market-tsum-ui:1.0-29")
                .addTags("package:yandex-market-tsum-tms:1.0-9")
                .build(),
            microEventBuilder
                .setTimeSeconds(toTimeStamp("2016-11-14T17:48:01", conductorAgentLogFormat))
                .clearTags()
                .addTags("host:blacksmith01g.market.yandex.net")
                .addTags("package:yandex-market-tsum-api:1.0-9")
                .addTags("package:yandex-market-tsum-ui:1.0-29")
                .addTags("package:yandex-market-tsum-tms:1.0-9")
                .build(),
            microEventBuilder
                //.setTimeSeconds(1479134969)
                .setTimeSeconds(toTimeStamp("2016-11-14T17:49:29", conductorAgentLogFormat))
                .clearTags()
                .addTags("host:blacksmith01e.market.yandex.net")
                .addTags("package:yandex-market-tsum-api:1.0-9")
                .addTags("package:yandex-market-tsum-ui:1.0-29")
                .addTags("package:yandex-market-tsum-tms:1.0-9")
                .build()
        ));
        Assert.assertEquals(expectedEvents, actualEvents);

        // other format
        logs = ConductorParser.getLogs(readFile("logs/otherLog.json"));
        Assert.assertTrue(parser.parseLogs(logs).isEmpty());
    }

    private String readFile(String path) throws IOException {
        return Resources.toString(Resources.getResource(path), Charsets.UTF_8);
    }

    private int toTimeStamp(String dateTime, SimpleDateFormat dateTimeFormat) throws ParseException {
        return (int) TimeUnit.MILLISECONDS.toSeconds(dateTimeFormat.parse(dateTime).getTime());
    }
}
