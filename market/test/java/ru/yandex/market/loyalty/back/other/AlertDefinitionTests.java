package ru.yandex.market.loyalty.back.other;

import java.util.Arrays;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import ru.yandex.market.health.context.AlertKind;
import ru.yandex.market.health.context.Environment;
import ru.yandex.market.health.context.ErrorKind;
import ru.yandex.market.health.context.HttpEndPointAlertContext;
import ru.yandex.market.health.context.Period;
import ru.yandex.market.health.context.Quantile;
import ru.yandex.market.health.factories.AbstractAlertFactory;
import ru.yandex.market.health.model.Alert;
import ru.yandex.market.loyalty.health.factories.LoyaltyAlertFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class AlertDefinitionTests {

    public static final long MINUTES_17 = 17 * 60 * 1000;
    public static final String CHANNEL = "market-loyalty-juggler-channel";

    @Test
    public void shouldConstructLoyaltyTimingAlertViaDefinitionV2() {
        AbstractAlertFactory alertFactory = new LoyaltyAlertFactory("market-loyalty", "back", MINUTES_17,
                Arrays.asList(CHANNEL));

        HttpEndPointAlertContext context = new HttpEndPointAlertContext(
                "POST:/loyalty-programs/add",
                null,
                "add_program_using_post",
                Period.FIVE_MIN,
                AlertKind.TIMING,
                Quantile.Q99,
                null,
                Environment.PRODUCTION,
                250.0,
                0.0,
                3,
                null
        );

        Alert actual = alertFactory.createAlert(context);

        assertThat(actual.getId(), equalTo("a_add_program_using_post-5m-timing-99-p-c"));
        assertThat(actual.getName(), equalTo("Timing five_min_0_99 on add_program_using_post(production)"));
        assertThat(actual.getType().getExpression().getProgram(), equalTo("let raw_data={cluster='stable', " +
                "service='back', sensor='timing-quantile-0_99', page_id='add_program_using_post', " +
                "project='market-loyalty', period='five_min', environment='PRODUCTION'};\n" +
                "let data = replace_nan(raw_data, 0);\n" +
                "let val = max(raw_data);\n" +
                "let alert = val > 250.00;\n" +
                "let description = alert ? '0_99 персентиль превысил порог 250ms на ручке POST:/loyalty-programs/add" +
                "(add_program_using_post) и составил {{val}}ms': 'Все хорошо с ручкой POST:/loyalty-programs/add" +
                "(add_program_using_post)';\n" +
                "let trafficColor = alert ? 'red': 'green';"));

        assertThat(actual.getType().getExpression().getCheckExpression(), equalTo("alert"));

        assertThat(actual.getPeriodMillis(), equalTo(MINUTES_17));
        assertThat(actual.getNotificationChannels(), equalTo(Arrays.asList(CHANNEL)));
        assertThat(actual.getAnnotations(), equalTo(
                ImmutableMap.of(
                        "host", "market_loyalty-production-timing-crit",
                        "description", "{{expression.description}}",
                        "service", "-add_program_using_post-5m-timing-99-p-c",
                        "trafficLight.color", "{{expression.trafficColor}}",
                        "environment", "production"
                )
        ));
    }

    @Test
    public void shouldConstructLoyaltyErrorPercentAlertViaDefinitionV2() {
        AbstractAlertFactory alertFactory = new LoyaltyAlertFactory("market-loyalty", "back", MINUTES_17,
                Arrays.asList(CHANNEL));

        HttpEndPointAlertContext context = new HttpEndPointAlertContext(
                "POST:/top500/activate",
                null,
                "activate_code_using_post",
                Period.FIVE_MIN,
                AlertKind.ERRORS,
                null,
                ErrorKind.HTTP_5XX,
                Environment.PRODUCTION,
                0.1,
                0.0,
                0,
                null
        );

        Alert actual = alertFactory.createAlert(context);

        assertThat(actual.getId(), equalTo("a_activate_code_using_post-5m-errors-5X-percent-p-c"));
        assertThat(actual.getName(), equalTo("Errors percent FIVE_MIN_5xx on activate_code_using_post(production)"));
        assertThat(actual.getType().getExpression().getProgram(), equalTo("let raw_data={cluster='stable', " +
                "service='back', sensor='errors-5xx-percent', page_id='activate_code_using_post', " +
                "project='market-loyalty', period='five_min', environment='PRODUCTION'};\n" +
                "let data = replace_nan(raw_data, 0);\n" +
                "let val = max(raw_data);\n" +
                "let alert = val > 0.10;\n" +
                "let description = alert ? 'Количество 5xx ошибок превысило порог 0.10% на ручке " +
                "POST:/top500/activate(activate_code_using_post) и составило {{val}}%': 'Все хорошо с ручкой " +
                "POST:/top500/activate(activate_code_using_post)';\n" +
                "let trafficColor = alert ? 'red': 'green';"));

        assertThat(actual.getType().getExpression().getCheckExpression(), equalTo("alert"));

        assertThat(actual.getPeriodMillis(), equalTo(MINUTES_17));
        assertThat(actual.getNotificationChannels(), equalTo(Arrays.asList(CHANNEL)));
        assertThat(actual.getAnnotations(), equalTo(
                ImmutableMap.of(
                        "host", "market_loyalty-production-5xx-crit",
                        "description", "{{expression.description}}",
                        "service", "-activate_code_using_post-5m-errors-5X-percent-p-c",
                        "trafficLight.color", "{{expression.trafficColor}}",
                        "environment", "production"
                )
        ));
    }
}
