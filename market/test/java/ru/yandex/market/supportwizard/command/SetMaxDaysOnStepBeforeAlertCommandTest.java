package ru.yandex.market.supportwizard.command;

import java.io.PrintWriter;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.supportwizard.base.supplier.SupplierOnboardingStepType;
import ru.yandex.market.supportwizard.config.BaseFunctionalTest;
import ru.yandex.market.supportwizard.service.alert.OnboardingAlertConfigurationCache;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SetMaxDaysOnStepBeforeAlertCommandTest extends BaseFunctionalTest {

    @Autowired
    private SetMaxDaysOnStepBeforeAlertCommand tested;
    @Autowired
    private Terminal terminal;
    @Autowired
    private OnboardingAlertConfigurationCache alertConfigurationCache;

    @BeforeEach
    public void setUpConfigurations() {
        Mockito.when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
        Mockito.when(terminal.areYouSure()).thenReturn(true);
    }

    @Test
    @DbUnitDataSet(before = "setMaxDaysOnStepBeforeAlertCommand.before.csv",
            after = "setMaxDaysOnStepBeforeAlertCommand.after.csv")
    void testExecuteCommand() {
        CommandInvocation commandInvocation1 = new CommandInvocation("set-max-days-on-step-before-alert",
                new String[]{"REGISTRATION", "3"},
                Collections.emptyMap());
        CommandInvocation commandInvocation2 = new CommandInvocation("set-max-days-on-step-before-alert",
                new String[]{"REQUEST_PROCESSING", "4"},
                Collections.emptyMap());

        asList(commandInvocation1, commandInvocation2)
                .forEach(inv -> tested.executeCommand(inv, terminal));

        assertEquals(3, alertConfigurationCache.getMaxHoursOnStep(SupplierOnboardingStepType.REGISTRATION));
        assertEquals(4, alertConfigurationCache.getMaxHoursOnStep(SupplierOnboardingStepType.REQUEST_PROCESSING));
    }
}
