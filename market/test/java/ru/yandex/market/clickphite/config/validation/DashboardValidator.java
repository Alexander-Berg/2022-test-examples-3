package ru.yandex.market.clickphite.config.validation;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.yandex.market.clickphite.config.validation.context.ConfigValidationException;
import ru.yandex.market.health.configs.clickphite.config.dashboard.DashboardConfig;
import ru.yandex.market.health.configs.clickphite.config.metric.GraphiteMetricConfig;
import ru.yandex.market.health.configs.clickphite.config.metric.MetricField;
import ru.yandex.market.health.configs.clickphite.config.metric.MetricSplit;

public class DashboardValidator {

    private DashboardValidator() {
    }

    public static void validateDashboards(GraphiteMetricConfig metricConfig) throws ConfigValidationException {
        validateSplits(metricConfig);
    }

    private static void validateSplits(GraphiteMetricConfig metricConfig) throws ConfigValidationException {
        final Set<String> metricFields = Stream.concat(
            metricConfig.getSplits().stream().map(MetricSplit::getName),
            metricConfig.getFields().stream().map(MetricField::getName)
        ).collect(Collectors.toSet());

        for (DashboardConfig dashboardConfig : metricConfig.getDashboards()) {
            for (String sortField : dashboardConfig.getSort()) {
                try {
                    MetricFieldValidator.validateFieldExpression(sortField, metricFields);
                } catch (IllegalStateException e) {
                    throw new ConfigValidationException(
                        "Incorrect sort field '" + sortField + "' in dashboard with id = " + dashboardConfig.getId());
                }
            }
        }
    }
}
