package ru.yandex.market.tpl.integration.tests.stress;

import java.util.Objects;

import org.slf4j.MDC;

public class StressTestsUtil {
    public static final String STRESS_TEST_ENABLED = "stress.tests.enabled";
    public static final String STRESS_TEST_COURIER = "stress.test.courier";

    public static boolean isStressTestEnabled() {
        return Objects.equals(System.getProperty(STRESS_TEST_ENABLED), "true");
    }

    public static void currentCourierEmail(String email) {
        MDC.put(STRESS_TEST_COURIER, email);
    }

    public static String currentCourierEmail() {
        return MDC.get(STRESS_TEST_COURIER);
    }
}
