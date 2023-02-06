package ru.yandex.market.health;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

import ru.yandex.market.health.context.Environment;
import ru.yandex.market.health.context.Verb;
import ru.yandex.market.health.controller.TestController;
import ru.yandex.market.health.methods.MethodDefinition;
import ru.yandex.market.health.model.Alert;
import ru.yandex.market.health.providers.EndPointsAlertProvider;
import ru.yandex.market.health.providers.SpringRequestMethodProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

@WebMvcTest
@ContextConfiguration(classes = TestController.class)
public class MethodProviderTests {

    public static final long PERIOD_MILLIS = 1000L;
    public static final ImmutableList<String> TELEGRAM = ImmutableList.of("telegram");
    @Autowired
    List<RequestMappingInfoHandlerMapping> handlerMappings;

    @Test
    public void testDefinition() {
        SpringRequestMethodProvider provider = new SpringRequestMethodProvider(handlerMappings);
        List<MethodDefinition> methods = provider.getMethods();
        assertThat(methods,
                allOf(
                        hasItem(
                                allOf(
                                        hasProperty("path", equalTo("/test/{property}/name")),
                                        hasProperty("verb", equalTo(Verb.GET)),
                                        hasProperty("method", hasProperty("name", equalTo("manyRequestMethods"))
                                        )
                                )
                        ),
                        hasItem(
                                allOf(
                                        hasProperty("path", equalTo("/test/{property}/name")),
                                        hasProperty("verb", equalTo(Verb.POST)),
                                        hasProperty("method", hasProperty("name", equalTo("manyRequestMethods"))
                                        )
                                )
                        )

                )
        );
    }

    @Test
    public void integrateAlertTest() {
        EndPointsAlertProvider provider = new EndPointsAlertProvider(
                new TestFactory("test", "service", PERIOD_MILLIS, TELEGRAM),
                new SpringRequestMethodProvider(handlerMappings),
                ImmutableSet.of(Environment.PRODUCTION));

        List<Alert> alerts = provider.getAlerts();
        Map<String, List<Alert>> alertById = alerts.stream()
                .collect(Collectors.groupingBy(Alert::getId));

        for (Alert alert : alerts) {
            System.out.println(alert.getId());
        }
        Alert one = alertById.get("a_1m_ch_p_mon_test_property_name_post_timings_99").get(0);

        assertThat(one.getId(), CoreMatchers.equalTo("a_1m_ch_p_mon_test_property_name_post_timings_99"));
        assertThat(one.getName(), CoreMatchers.equalTo("Timing one_min_0_99 on POST test_property_name(production)"));
        assertThat(one.getType().getExpression().getProgram(), CoreMatchers.equalTo("let raw_data=QUERY_SELECTOR;\n" +
                "let data = tail(replace_nan(raw_data, 0), 12);\n" +
                "let warn_count = count(drop_below(data, 10000.00));\n" +
                "let val = max(raw_data);\n" +
                "let crit_val = false;\n" +
                "let warn = warn_count > 0;\n" +
                "let warn_to_crit = (-1 > 0) && (warn_count > -1);\n" +
                "let crit = crit_val || warn_to_crit;\n" +
                "let description = warn ? '{{warn_count}} " +
                "значений 0_99 персентиля превысили порог 10000ms на ручке POST:/test/{property}/name" +
                "(test_property_name)' : " +
                "'Всё хорошо с ручкой POST:/test/{property}/name(test_property_name)';\n" +
                "let trafficColor = crit ? 'red': (warn ? 'yellow' : 'green');\n" +
                "alarm_if(crit);\n" +
                "warn_if(warn);"));

        assertThat(one.getType().getExpression().getCheckExpression(), CoreMatchers.equalTo("false"));

        assertThat(one.getPeriodMillis(), equalTo(PERIOD_MILLIS));
        assertThat(one.getNotificationChannels(), equalTo(TELEGRAM));
        assertThat(one.getAnnotations(), allOf(
                hasEntry("host", "service"),
                hasEntry("service", "1m_ch_p_mon_test_property_name_post_timings_99"),
                hasEntry("environment", "production"),
                hasEntry("method", "POST"),
                hasEntry("page", "test_property_name"),
                hasEntry("traceParameters", "resptime_ms >= 10000.0"),
                hasEntry("trafficLight.color", "{{#isNoData}}green{{/isNoData}}{{^isNoData}}{{expression" +
                        ".trafficColor}}{{/isNoData}}"),
                hasEntry("description", "{{expression.description}}"),
                hasEntry("status.code", "{{status.code}}"),
                hasEntry("type", "timings")
        ));

    }
}
