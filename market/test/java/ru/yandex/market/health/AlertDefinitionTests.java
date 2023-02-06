package ru.yandex.market.health;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import ru.yandex.market.health.context.AlertKind;
import ru.yandex.market.health.context.Environment;
import ru.yandex.market.health.context.ErrorKind;
import ru.yandex.market.health.context.HttpEndPointAlertContext;
import ru.yandex.market.health.context.Period;
import ru.yandex.market.health.context.Quantile;
import ru.yandex.market.health.context.Verb;
import ru.yandex.market.health.factories.AbstractAlertFactory;
import ru.yandex.market.health.model.Alert;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.collection.IsMapContaining.hasEntry;

public class AlertDefinitionTests {

    public static final long MINUTES_31 = 31 * 60 * 1000;
    public static final String CHANNEL = "test-channel";
    public static final String TEST_PROJECT = "test-project";
    public static final String TEST_SERVICE = "test-service";

    @Test
    public void shouldConstructLoyaltyTimingAlertViaDefinitionV2() {
        AbstractAlertFactory alertFactory = new TestFactory(TEST_PROJECT, TEST_SERVICE, MINUTES_31,
                Arrays.asList(CHANNEL));

        HttpEndPointAlertContext context = new HttpEndPointAlertContext(
                "/endpoints",
                Verb.GET,
                "get_endpoints",
                Period.FIVE_MIN,
                AlertKind.TIMING,
                Quantile.Q95,
                null,
                Environment.PRESTABLE,
                100.0,
                10.0,
                0,
                null
        );

        Alert actual = alertFactory.createAlert(context);

        assertThat(actual.getId(), equalTo("a_5m_ch_ps_mon_get_endpoints_get_timings_95"));
        assertThat(actual.getName(), equalTo("Timing five_min_0_95 on GET get_endpoints(prestable)"));
        assertThat(actual.getType().getExpression().getProgram(), equalTo("let raw_data=QUERY_SELECTOR;\n" +
                "let data = tail(replace_nan(raw_data, 0), 12);\n" +
                "let warn_count = count(drop_below(data, 10.00));\n" +
                "let val = max(raw_data);\n" +
                "let crit_val = val > 100.00;\n" +
                "let warn = warn_count > 0;\n" +
                "let warn_to_crit = (0 > 0) && (warn_count > 0);\n" +
                "let crit = crit_val || warn_to_crit;\n" +
                "let description = crit_val ? '0_95 персентиль превысил порог 100ms на ручке /endpoints" +
                "(get_endpoints)" +
                " и составил {{val}}ms' : warn ? '{{warn_count}} значений 0_95 персентиля превысили порог 10ms на" +
                " ручке /endpoints(get_endpoints)' : 'Всё хорошо с ручкой /endpoints(get_endpoints)';\n" +
                "let trafficColor = crit ? 'red': (warn ? 'yellow' : 'green');\n" +
                "alarm_if(crit);\n" +
                "warn_if(warn);"));

        assertThat(actual.getType().getExpression().getCheckExpression(), equalTo("false"));

        assertThat(actual.getPeriodMillis(), equalTo(MINUTES_31));
        assertThat(actual.getNotificationChannels(), equalTo(Arrays.asList(CHANNEL)));
        assertThat(actual.getAnnotations(), allOf(
                hasEntry("host", "test-service"),
                hasEntry("service", "5m_ch_ps_mon_get_endpoints_get_timings_95"),
                hasEntry("environment", "prestable"),
                hasEntry("method", "GET"),
                hasEntry("page", "get_endpoints"),
                hasEntry("traceParameters", "resptime_ms >= 10.0"),
                hasEntry("trafficLight.color", "{{#isNoData}}green{{/isNoData}}{{^isNoData}}{{expression" +
                        ".trafficColor}}{{/isNoData}}"),
                hasEntry("description", "{{expression.description}}"),
                hasEntry("status.code", "{{status.code}}"),
                hasEntry("type", "timings")
        ));
    }

    @Test
    public void shouldConstructLoyaltyErrorPercentAlertViaDefinitionV2() {
        AbstractAlertFactory alertFactory = new TestFactory(TEST_PROJECT, TEST_SERVICE, MINUTES_31,
                Arrays.asList(CHANNEL));

        HttpEndPointAlertContext context = new HttpEndPointAlertContext(
                "/endpoints",
                Verb.POST,
                "post_endpoints",
                Period.FIVE_MIN,
                AlertKind.ERRORS,
                null,
                ErrorKind.HTTP_5XX,
                Environment.TESTING,
                0.1,
                0.0,
                0,
                null
        );

        Alert actual = alertFactory.createAlert(context);

        assertThat(actual.getId(), equalTo("a_5m_ch_t_mon_post_endpoints_post_5X-percent"));
        assertThat(actual.getName(), equalTo("Errors percent FIVE_MIN_5xx on POST post_endpoints(testing)"));
        assertThat(actual.getType().getExpression().getProgram(), equalTo("let raw_data=QUERY_SELECTOR;\n" +
                "let data = tail(replace_nan(raw_data, 0), 12);\n" +
                "let warn_count = count(drop_below(data, 0.00));\n" +
                "let val = max(raw_data);\n" +
                "let crit_val = val > 0.10;\n" +
                "let warn = warn_count > 0;\n" +
                "let warn_to_crit = (0 > 0) && (warn_count > 0);\n" +
                "let crit = crit_val || warn_to_crit;\n" +
                "let description = crit_val ? 'Количество 5xx ошибок превысило порог 0.10% на ручке /endpoints" +
                "(post_endpoints) и составило {{val}}%' : (warn_to_crit ? 'Количество 5xx ошибок превысило порог 0" +
                ".00% на ручке /endpoints(post_endpoints) 0 раз' : (warn ? 'Количество 5xx ошибок превысило порог 0" +
                ".00% на ручке /endpoints(post_endpoints) {{warn_count}} раз': ('Всё хорошо с ручкой /endpoints" +
                "(post_endpoints)')));\n" +
                "let trafficColor = crit ? 'red': (warn ? 'yellow' : 'green');\n" +
                "alarm_if(crit);\n" +
                "warn_if(warn);"));

        assertThat(actual.getType().getExpression().getCheckExpression(), equalTo("false"));

        assertThat(actual.getPeriodMillis(), equalTo(MINUTES_31));
        assertThat(actual.getNotificationChannels(), equalTo(Arrays.asList(CHANNEL)));
        assertThat(actual.getAnnotations(), allOf(
                hasEntry("host", "test-service"),
                hasEntry("service", "5m_ch_t_mon_post_endpoints_post_5X-percent"),
                hasEntry("environment", "testing"),
                hasEntry("method", "POST"),
                hasEntry("page", "post_endpoints"),
                hasEntry("traceParameters", "http_code >= 500"),
                hasEntry("trafficLight.color", "{{#isNoData}}green{{/isNoData}}{{^isNoData}}{{expression" +
                        ".trafficColor}}{{/isNoData}}"),
                hasEntry("description", "{{expression.description}}"),
                hasEntry("status.code", "{{status.code}}"),
                hasEntry("type", "5xx-percent")
        ));
    }

    @Test
    public void shouldHaveValidAlertIdLength() {
        AbstractAlertFactory alertFactory = new TestFactory(TEST_PROJECT, TEST_SERVICE, MINUTES_31,
                Arrays.asList(CHANNEL));

        HttpEndPointAlertContext context = new HttpEndPointAlertContext(
                "/orders/{orderId}/status/delivered-with-real-delivery-date",
                Verb.POST,
                "orders_orderId_status_delivered-with-real-delivery-date",
                Period.ONE_MIN,
                AlertKind.TIMING,
                Quantile.Q99,
                null,
                Environment.PRODUCTION,
                0.0,
                10000.0,
                -1,
                null
        );

        Alert actual = alertFactory.createAlert(context);

        assertThat(actual.getId().length(), lessThanOrEqualTo(AbstractAlertFactory.MAX_ID_LENGTH));
        assertThat(actual.getId(), equalTo("a_1m_ch_p_mon_orders_status_delivered-with-real-_post_timings_99"));
    }
}
