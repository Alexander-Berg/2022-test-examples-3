package ru.yandex.market.logshatter.parser.ridetech;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.logshatter_for_logs.Level;

@SuppressWarnings("LineLength")
public class BackendPy3LogParserTest {

    LogParserChecker checker = new LogParserChecker(new BackendPy3LogParser());

    @Test
    public void parse() throws Exception {
        String line1 = "tskv\ttimestamp=2022-04-26 13:49:13,091\tmodule=taxi.clients.crons\tlevel=INFO\t" +
            "link=e57cb12e643848e59d6e7546fe3d4b8c\t_type=log\thost=dg3lhmct7fvrvzxa.vla.yp-c.yandex.net\t" +
            "command=clowny_alert_manager-crontasks-start_applying_configs_in_queue\t" +
            "text=taxi-crons [POST](/v1/task/clowny_alert_manager-crontasks-start_applying_configs_in_queue/lock/" +
            "aquire/) request completed in 0.006604433059692383 seconds";

        checker.setHost("dg3lhmct7fvrvzxa.vla.yp-c.yandex.net");
        checker.setParam("dg3lhmct7fvrvzxa.vla.yp-c.yandex.net",
            "{'namespace': 'taxi', 'project': 'taxi-devops', 'service': 'clowny-alert-manager', 'env': 'stable'}");
        checker.setParam("module-ignore-set", "taxi.logs.utils::taxi.util.aiohttp_kit.middleware");
        checker.setLogBrokerTopic("rt3.sas--taxi--taxi-clowny-alert-manager-yandex-taxi-clowny-alert-manager-cron-log");

        checker.check(
            line1,
            1650970153,
            LocalDateTime.parse("2022-04-26T13:49:13.091"),
            "taxi_taxi-devops", // project
            "clowny-alert-manager", // service
            "taxi-crons [POST](/v1/task/clowny_alert_manager-crontasks-start_applying_configs_in_queue/lock/aquire/) " +
                "request completed in 0.006604433059692383 seconds", // message
            "stable", // env
            "", // cluster
            Level.INFO, // level
            "dg3lhmct7fvrvzxa.vla.yp-c.yandex.net", // hostname
            "", // version
            "dc", // dc
            "e57cb12e643848e59d6e7546fe3d4b8c", // request_id
            "", // trace_id
            "", // span_id
            "taxi.clients.crons", // component
            UUID.fromString("804f9283-b826-36a5-97e7-824f4f27f687"), // record_id
            "", // validation_err
            "{\"_type\":\"log\",\"command\":\"clowny_alert_manager-crontasks-start_applying_configs_in_queue\"}" // rest
        );

        String line2 = "tskv\ttimestamp=2022-04-26 14:01:31,772\tmodule=generated.clients.clownductor\tlevel=INFO\t" +
            "link=2b72034bd2ed4916b4ea955f7db93952\t_type=log\thost=dg3lhmct7fvrvzxa.vla.yp-c.yandex.net\t" +
            "text=POST request http://clownductor.taxi.yandex.net/v2/branches/ attempt 1/3 success with status code " +
            "200";

        checker.check(
            line2,
            1650970891,
            LocalDateTime.parse("2022-04-26T14:01:31.772"),
            "taxi_taxi-devops", // project
            "clowny-alert-manager", // service
            "POST request http://clownductor.taxi.yandex.net/v2/branches/ attempt 1/3 success with status code 200",
            // message
            "stable", // env
            "", // cluster
            Level.INFO, // level
            "dg3lhmct7fvrvzxa.vla.yp-c.yandex.net", // hostname
            "", // version
            "dc", // dc
            "2b72034bd2ed4916b4ea955f7db93952", // request_id
            "", // trace_id
            "", // span_id
            "generated.clients.clownductor", // component
            UUID.fromString("64fe1e9c-d4c6-3e09-94d2-9b3ec61ae058"), // record_id
            "", // validation_err
            "{\"_type\":\"log\"}" // rest
        );

        String line3 = "tskv\ttimestamp=2022-04-26 14:01:31,772\tmodule=taxi.logs.utils\tlevel=INFO\t" +
            "link=2b72034bd2ed4916b4ea955f7db93952\t_type=log\thost=dg3lhmct7fvrvzxa.vla.yp-c.yandex.net\t" +
            "text=POST request http://clownductor.taxi.yandex.net/v2/branches/ attempt 1/3 success with status code " +
            "200";
        checker.checkEmpty(line3);
    }
}
