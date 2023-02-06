package ru.yandex.market.logshatter.parser.delivery;

import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

public class DeliveryQueueTaskParserTest {
    static LogParserChecker checker = new LogParserChecker(new DeliveryQueueTaskParser());

    @Test
    public void testParser() throws Exception {
        checker.check(
            "[2016-05-18 03:35:18] Sys.Queue.Task:INFO: {\"now\":\"2016-08-05 16:26:07\"," +
                "\"functionName\":\"order.create\",\"command\":\"MS\\\\Command\\\\Order\\\\OrderToDeliveryCommand\"," +
                "\"status\":\"ERROR\",\"setTime\":\"2016-08-05 19:25:23\",\"startTime\":\"2016-08-05 19:25:59\"," +
                "\"endTime\":\"2016-08-05 19:26:07\",\"memoryPeakUsage\":\"35651584\"} []",
            new Date(1470414367000L),
            "order.create",
            "MS\\Command\\Order\\OrderToDeliveryCommand",
            "ERROR",
            new Date(1470414323000L),
            new Date(1470414359000L),
            new Date(1470414367000L),
            35651584,
            0,
            0
        );

        checker.check(
            "[2016-08-05 16:29:36] Sys.Queue.Task:INFO:  {\"now\":\"2016-08-05 16:29:36\"," +
                "\"functionName\":\"user.mailchimp\",\"command\":\"MS\\\\Command\\\\MailchimpSubscribeCommand\"," +
                "\"status\":\"READY\",\"setTime\":\"2016-08-05 19:28:08\",\"startTime\":\"2016-08-05 19:29:35\"," +
                "\"endTime\":\"2016-08-05 19:29:36\",\"memoryPeakUsage\":\"14417920\", \"workersCount\":\"777\", " +
                "\"duration\":3176} []",
            new Date(1470414576000L),
            "user.mailchimp",
            "MS\\Command\\MailchimpSubscribeCommand",
            "READY",
            new Date(1470414488000L),
            new Date(1470414575000L),
            new Date(1470414576000L),
            14417920,
            777,
            3176
        );
    }
}
