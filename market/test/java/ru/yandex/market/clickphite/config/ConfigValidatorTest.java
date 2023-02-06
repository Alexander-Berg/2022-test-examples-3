package ru.yandex.market.clickphite.config;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.clickphite.config.validation.context.ConfigValidationException;
import ru.yandex.market.clickphite.config.validation.context.ConfigValidator;
import ru.yandex.market.health.configs.clickphite.MetricType;
import ru.yandex.market.health.configs.clickphite.config.metric.GraphiteMetricConfig;

import static org.junit.Assert.assertEquals;

public class ConfigValidatorTest {

    @Test
    public void validateQuantiles() {
        GraphiteMetricConfig graphiteMetricConfig = new GraphiteMetricConfig();
        graphiteMetricConfig.setType(MetricType.QUANTILE_TIMING);
        List<String> quantiles = Arrays.asList(
            "0.6", "0.5", "-0.2", "0.7", "0.80", "0.90", "0.95", "0.97", "das", "0.99",
            "0.995", "0.997", "0.998", "0.999", "1", "2"
        );
        List<String> validQuantiles = Arrays.asList(
            "0.5", "0.6", "0.7", "0.80", "0.90", "0.95", "0.97", "0.99", "0.995", "0.997", "0.998", "0.999", "1"
        );
        graphiteMetricConfig.setQuantiles(quantiles);

        ConfigValidator configValidator = new ConfigValidator();
        configValidator.validateQuantiles(graphiteMetricConfig);
        assertEquals(validQuantiles, graphiteMetricConfig.getQuantiles());
    }

    @Test
    public void forbiddenFunctionsTest() {
        ConfigValidator configValidator = new ConfigValidator("quantileExact", "quantileExactWeighted");

        String metricField = "quantileTDigestIf(0.99)(ts - download_ts, type='feed_price' and report_status=0 " +
            "and notLike(report_sub_role, 'blue%') and notLike(report_sub_role, 'red%'))";
        Assert.assertTrue(isValidMetricField(configValidator, metricField));

        metricField = "quantileExactWeightedArray(0.5)(arrayMap(ts -> toUInt32(complete_date) - ts, " +
            "timestamp_seconds_to_line_count_keys), timestamp_seconds_to_line_count_values)";
        Assert.assertTrue(isValidMetricField(configValidator, metricField));

        metricField = "quantile(0.50)(job_running_date - job_queued_date)";
        Assert.assertTrue(isValidMetricField(configValidator, metricField));

        metricField = "quantileExactIf(0.99)(ts - download_ts, type='feed_price' and report_status=0 " +
            "and notLike(report_sub_role, 'blue%') and notLike(report_sub_role, 'red%'))";
        Assert.assertFalse(isValidMetricField(configValidator, metricField));

        metricField = "quantileExact(0.50)(job_running_date - job_queued_date)";
        Assert.assertFalse(isValidMetricField(configValidator, metricField));
    }

    private static boolean isValidMetricField(ConfigValidator configValidator,
                                              String metricField) {
        try {
            configValidator.validateMetricFunctions(metricField);
        } catch (ConfigValidationException e) {
            return false;
        }
        return true;
    }
}
