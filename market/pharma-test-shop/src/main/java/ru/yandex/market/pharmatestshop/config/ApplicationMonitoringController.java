package ru.yandex.market.pharmatestshop.config;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringController;

public class ApplicationMonitoringController extends MonitoringController {

    private static final int WAIT_BEFORE_CLOSE_CONTEXT_SECONDS = 5;

    private final ApplicationContext context;
    private volatile boolean isApplicationFinishing = false;

    public ApplicationMonitoringController(ComplexMonitoring generalMonitoring,
                                           ComplexMonitoring pingMonitoring,
                                           ApplicationContext context) {
        super(generalMonitoring, pingMonitoring);
        this.context = context;
    }

    @Override
    public void close(HttpServletRequest request) {
        super.close(request);
        finishContext(context, WAIT_BEFORE_CLOSE_CONTEXT_SECONDS);
    }

    @VisibleForTesting
    void finishContext(ApplicationContext context, int waitSeconds) {

        if (isApplicationFinishing) {
            return;
        }
        isApplicationFinishing = true;

        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        ((ConfigurableApplicationContext) context).close();
                    }
                },
                TimeUnit.SECONDS.toMillis(waitSeconds)
        );
    }
}
