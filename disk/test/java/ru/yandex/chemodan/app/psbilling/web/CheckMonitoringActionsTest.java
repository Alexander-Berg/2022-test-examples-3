package ru.yandex.chemodan.app.psbilling.web;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.SneakyThrows;
import org.apache.logging.log4j.util.Strings;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;

import ru.yandex.commune.a3.ActionApp;
import ru.yandex.commune.a3.action.ActionDescriptor;
import ru.yandex.misc.log.mlf.Logger;
import ru.yandex.misc.log.mlf.LoggerFactory;


public class CheckMonitoringActionsTest extends PsBillingWebContextTest {
    private static final Logger logger = LoggerFactory.getLogger(CheckMonitoringActionsTest.class);

    private static final String METRIC_EXTRACT_SCRIPT = "../../admin/docker/disk/clusters/disk-ps-billing-web/common" +
            "-settings/usr/bin/ps-billing-nginx-timings.py";

    public static final Pattern HANDLERS_COUNT = Pattern.compile("^handlers_count_codes\\..*_200 1$");


    @Test
    @SneakyThrows
    public void testAllActionsForOne() {
        try (ConfigurableApplicationContext appContext = initContext()) {
            appContext.getBean(ActionApp.class)
                    .getActionDescriptorResolver()
                    .getActions()
                    .map(ActionDescriptor::getName)
                    .map(s -> s.substring(s.indexOf("/")))
                    .sorted()
                    .forEach(this::testEndpoint);
        }
    }

    @SneakyThrows
    private void testEndpoint(String endpoint) {
        logger.info("Test testEndpoint={}", endpoint);
        ProcessBuilder processBuilder = new ProcessBuilder()
                .command("bash",
                        "-c",
                        "echo -e '" + buildLogLine(endpoint) + "' | " + METRIC_EXTRACT_SCRIPT);

        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
           List<String> lines = reader.lines().collect(Collectors.toList());

            long actualSize = lines.stream()
                    .peek(logger::info)
                    .filter(s -> HANDLERS_COUNT.matcher(s).find())
                    .count();

            Assert.assertEquals(
                    "Не сматчилось. Количество метрик для "
                            + endpoint
                            + "\nРезультат скрипта: "
                            + Strings.join(lines, '\n'),
                    1,
                    actualSize);
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Process exit code " + exitCode);
        }
    }

    private String buildLogLine(String endpoint) {
        return String.format("status=200\\trequest_time=0.01\\tprotocol=HTTP/1.1\\trequest=%s\n", endpoint);
    }

}
