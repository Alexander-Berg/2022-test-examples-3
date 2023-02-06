package ru.yandex.market.model;

import java.util.Collections;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.partner.PartnerService;
import ru.yandex.market.core.partner.model.ModelManagementAccess;

@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "ModelManagementCommandTest.csv")
class ModelManagementCommandTest extends FunctionalTest {
    private static final TestTerminal TEST_TERMINAL = new TestTerminal();
    @Autowired
    private PartnerService partnerService;

    private ModelManagementCommand modelManagement;

    @BeforeEach
    void setUp() {
        modelManagement = new ModelManagementCommand(partnerService);
    }

    @Test
    void testAddNew() {
        Assertions.assertFalse(partnerService.isModelManager(PartnerId.datasourceId(776), ModelManagementAccess.MANUAL));
        runCommand("add", "776");
        Assertions.assertTrue(partnerService.isModelManager(PartnerId.datasourceId(776), ModelManagementAccess.MANUAL));
    }

    @Test
    void testAddExisting() {
        Assertions.assertTrue(partnerService.isModelManager(PartnerId.datasourceId(774), ModelManagementAccess.MANUAL));
        runCommand("add", "774");
        Assertions.assertTrue(partnerService.isModelManager(PartnerId.datasourceId(774), ModelManagementAccess.MANUAL));
    }

    @Test
    void testRemoveExisting() {
        Assertions.assertTrue(partnerService.isModelManager(PartnerId.datasourceId(775), ModelManagementAccess.MANUAL));
        runCommand("remove", "775");
        Assertions.assertFalse(partnerService.isModelManager(PartnerId.datasourceId(775), ModelManagementAccess.MANUAL));
    }

    @Test
    void testRemoveMissing() {
        Assertions.assertFalse(partnerService.isModelManager(PartnerId.datasourceId(777), ModelManagementAccess.MANUAL));
        runCommand("remove", "777");
        Assertions.assertFalse(partnerService.isModelManager(PartnerId.datasourceId(777), ModelManagementAccess.MANUAL));
    }

    @Test
    void testAddNewManual() {
        Assertions.assertFalse(partnerService.isModelManager(PartnerId.datasourceId(776), ModelManagementAccess.MANUAL));
        runCommand("add", "manual", "776");
        Assertions.assertTrue(partnerService.isModelManager(PartnerId.datasourceId(776), ModelManagementAccess.MANUAL));
    }

    @Test
    void testAddExistingManual() {
        Assertions.assertTrue(partnerService.isModelManager(PartnerId.datasourceId(774), ModelManagementAccess.MANUAL));
        runCommand("add", "manual", "774");
        Assertions.assertTrue(partnerService.isModelManager(PartnerId.datasourceId(774), ModelManagementAccess.MANUAL));
    }

    @Test
    void testRemoveExistingManual() {
        Assertions.assertTrue(partnerService.isModelManager(PartnerId.datasourceId(775), ModelManagementAccess.MANUAL));
        runCommand("remove", "manual", "775");
        Assertions.assertFalse(partnerService.isModelManager(PartnerId.datasourceId(775), ModelManagementAccess.MANUAL));
    }

    @Test
    void testRemoveMissingManual() {
        Assertions.assertFalse(partnerService.isModelManager(PartnerId.datasourceId(777), ModelManagementAccess.MANUAL));
        runCommand("remove", "manual", "777");
        Assertions.assertFalse(partnerService.isModelManager(PartnerId.datasourceId(777), ModelManagementAccess.MANUAL));
    }

    @Test
    void testAddNewBatch() {
        Assertions.assertTrue(partnerService.isModelManager(PartnerId.datasourceId(773), ModelManagementAccess.BATCH));
        runCommand("add", "batch", "773");
        Assertions.assertTrue(partnerService.isModelManager(PartnerId.datasourceId(773), ModelManagementAccess.BATCH));
    }

    @Test
    void testAddExistingBatch() {
        Assertions.assertTrue(partnerService.isModelManager(PartnerId.datasourceId(771), ModelManagementAccess.BATCH));
        runCommand("add", "batch", "771");
        Assertions.assertTrue(partnerService.isModelManager(PartnerId.datasourceId(771), ModelManagementAccess.BATCH));
    }

    @Test
    void testRemoveExistingBatch() {
        Assertions.assertTrue(partnerService.isModelManager(PartnerId.datasourceId(772), ModelManagementAccess.BATCH));
        runCommand("remove", "batch", "772");
        Assertions.assertTrue(partnerService.isModelManager(PartnerId.datasourceId(772), ModelManagementAccess.BATCH));
    }

    @Test
    void testRemoveMissingBatch() {
        Assertions.assertTrue(partnerService.isModelManager(PartnerId.datasourceId(770), ModelManagementAccess.BATCH));
        runCommand("remove", "batch", "770");
        Assertions.assertTrue(partnerService.isModelManager(PartnerId.datasourceId(770), ModelManagementAccess.BATCH));
    }

    private void runCommand(String... arguments) {
        CommandInvocation commandInvocation =
                new CommandInvocation("model-management", arguments, Collections.emptyMap());
        modelManagement.executeCommand(commandInvocation, TEST_TERMINAL);
    }

    private static class TestTerminal extends Terminal {
        public TestTerminal() {
            super(System.in, System.out);
        }

        @Override
        protected void onStart() {
        }

        @Override
        protected void onClose() {
        }
    }
}
