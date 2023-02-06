package ru.yandex.market.tpl.integration.tests.context;

import java.util.HashMap;
import java.util.Map;

import static ru.yandex.market.tpl.integration.tests.stress.StressTestsUtil.currentCourierEmail;
import static ru.yandex.market.tpl.integration.tests.stress.StressTestsUtil.isStressTestEnabled;

public class AutoTestContextHolder {
    private static final ThreadLocal<AutoTestContext> autoTestContext = new ThreadLocal<>();
    private static final Map<String, AutoTestContext> stressTestsContexts = new HashMap<>();

    public static AutoTestContext getContext() {
        AutoTestContext context;
        if (isStressTestEnabled() && currentCourierEmail() != null) {
            context = stressTestsContexts.get(currentCourierEmail());
        } else {
            context = autoTestContext.get();
        }
        if (context == null) {
            context = createEmptyContext();
            autoTestContext.set(context);
            if (isStressTestEnabled()) {
                stressTestsContexts.put(currentCourierEmail(), context);
            }
        }
        return context;
    }

    public static void setContext(AutoTestContext context) {
        autoTestContext.set(context);
        if (currentCourierEmail() != null) {
            stressTestsContexts.put(currentCourierEmail(), context);
        }
    }

    public static void clearContext() {
        autoTestContext.remove();
        stressTestsContexts.clear();
    }

    public static AutoTestContext createEmptyContext() {
        return new AutoTestContext();
    }
}
