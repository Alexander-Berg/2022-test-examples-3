package ru.yandex.travel.allure;

import io.qameta.allure.listener.StepLifecycleListener;
import io.qameta.allure.model.StepResult;

import java.util.Deque;
import java.util.LinkedList;
import java.util.logging.Logger;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

public class AllureStepLogger implements StepLifecycleListener {

    private final Logger logger = Logger.getLogger(AllureStepLogger.class.getName());

    private Deque<String> names = new LinkedList<>();

    public void beforeStepStart(StepResult result) {
        names.push(result.getName());
        logger.info(getOffset() + " [ -> ] " + defaultIfBlank(result.getDescription(), result.getName()));
    }

    public void afterStepStop(StepResult result) {
        logger.info(getOffset() + " [ <- ] Step Finished!");
        names.poll();
    }

    private String getOffset() {
        return new String(new char[names.size() == 0 ? 0 : names.size() - 1]).replaceAll("\0", "   ");
    }


}
