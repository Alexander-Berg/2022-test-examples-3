package ru.yandex.market.clickphite.config;

import org.junit.Test;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 25.07.2018
 */
public class SolomonSensorsValidationTest {

    FailureValidationTestTool tool = new FailureValidationTestTool("solomon_sensors_validation_test/");

    @Test
    public void noLabelsOrLabelsArray() {
        tool.shouldFail("noLabelsOrLabelsArray");
    }

    @Test
    public void noRequiredLabels() {
        tool.shouldFail("noLabelProject");
        tool.shouldFail("noLabelCluster");
        tool.shouldFail("noLabelService");
        tool.shouldFail("noLabelSensor");
    }

    @Test
    public void noSplitExists() {
        tool.shouldFail("noSplitExists");
        tool.shouldFail("noSplitExistsArray");
    }

}
