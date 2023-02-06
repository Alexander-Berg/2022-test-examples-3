package ru.yandex.market.deepmind.common.services.tracker_approver.configurations;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import ru.yandex.market.deepmind.tracker_approver.service.TrackerApproverExecutionContext;
import ru.yandex.startrek.client.StartrekClientBuilder;

import static ru.yandex.market.deepmind.tracker_approver.utils.TrackerQueryBuilder.TIMEZONE;

@SuppressWarnings("unused")
public class AssortCommitteeQueryingLocalTest {
    /**
     * Method to check correctness of query building and parameterizing of Service
     * It can be run locally by attaching personal token and adding @Test annotation
     */
    public void trackerQueryLocalTest() {
        //https://a.yandex-team.ru/svn/trunk/arcadia/tracker/tracker-java-client#configuring
        String token = "";

        var queue = "TESTASSORTCOMMI";
        var from = OffsetDateTime.ofInstant(
            Instant.now().minus(7, ChronoUnit.DAYS),
            ZoneId.of(TIMEZONE)
        );
        var to = OffsetDateTime.ofInstant(
            Instant.now(),
            ZoneId.of(TIMEZONE)
        );

        var executionContext = new TrackerApproverExecutionContext()
            .setThreadCount(1)
            .setMaxRetryCount(5);

        var assortCommitteeConfiguration = new AssortCommitteeTrackerApproverConfiguration(
            queue,
            null,
            executionContext
        );

        var query = assortCommitteeConfiguration.getTrackerQueryBuilder()
            .setUpdatedFrom(from)
            .setUpdatedTo(to)
            .buildQuery();

        System.out.println(query);

        var session = StartrekClientBuilder.newBuilder()
            .uri("https://st-api.yandex-team.ru")
            .maxConnections(10)
            .connectionTimeout(1, TimeUnit.SECONDS)
            .socketTimeout(500, TimeUnit.MILLISECONDS)
            .build(token);

        var issueIterator = session.issues().find(query);

        while (issueIterator.hasNext()) {
            var issue = issueIterator.next();
            System.out.println(issue);
            System.out.println(issue.getStatus());
        }
    }
}
