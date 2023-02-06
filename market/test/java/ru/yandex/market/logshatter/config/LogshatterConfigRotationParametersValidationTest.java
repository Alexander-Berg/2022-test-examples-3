package ru.yandex.market.logshatter.config;

import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.market.health.configs.logshatter.config.LogShatterConfig;

public class LogshatterConfigRotationParametersValidationTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void verifyExceptionMessageOnInvalidConfigs_DifferentRotationDaysForOneTable() {

        ConfigurationService configurationService = new ConfigurationService();

        String firstConfigName = "testConfig1";
        String secondConfigName = "testConfig2";
        String tableName = "someTableName";

        int firstDataRotationDays = 10;
        int secondDataRotationDays = 20;

        LogShatterConfig testConfig1 = createConfig(firstConfigName, firstDataRotationDays);

        LogShatterConfig testConfig2 = createConfig(secondConfigName, secondDataRotationDays);

        expectedException.expectMessage(
            String.format(
                "Different data rotation days found in configs:\n" +
                    "%s:%s\n," +
                    "%s:%s\n," +
                    "for table '%s'",
                firstConfigName,
                firstDataRotationDays,
                secondConfigName,
                secondDataRotationDays,
                tableName)
        );

        configurationService.validateDataRotationDays(testConfig1, testConfig2, tableName);
    }

    @NotNull
    private LogShatterConfig createConfig(String fileName, int rotationDays) {
        return LogShatterConfig.newBuilder()
            .setConfigId(fileName)
            .setDataRotationDays(rotationDays)
            .build();
    }

}
