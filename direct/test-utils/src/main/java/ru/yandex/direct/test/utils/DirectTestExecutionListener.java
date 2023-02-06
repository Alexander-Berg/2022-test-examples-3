package ru.yandex.direct.test.utils;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Это набросок для того, чтобы лучше собирать статистику по тестам в будущем
 */
public class DirectTestExecutionListener implements TestExecutionListener {

    private final Logger logger = LoggerFactory.getLogger(DirectTestExecutionListener.class);

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        logger.info("testPlanExecutionStarted {}", testPlan);
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        logger.info("testPlanExecutionFinished {}", testPlan);
    }

    @Override
    public void dynamicTestRegistered(TestIdentifier testIdentifier) {
        logger.info("dynamicTestRegistered {}", testIdentifier);
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        logger.info("executionSkipped {} {}", testIdentifier, reason);
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        logger.info("executionStarted {}", testIdentifier);
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        logger.info("executionFinished {} {}", testIdentifier, testExecutionResult);
        if(TestExecutionResult.Status.FAILED.equals(testExecutionResult.getStatus())) {
            logger.error("Test failed: {} {}", testIdentifier.getDisplayName(), testExecutionResult);
        }
    }

    @Override
    public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
        logger.info("reportingEntryPublished {} {}", testIdentifier, entry);
    }
}
