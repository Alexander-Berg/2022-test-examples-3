package ru.yandex.market.mbo.tms.modeltransfer.processor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.common.ZooKeeper.ZooKeeperService;
import ru.yandex.market.mbo.export.dumpstorage.DumpStorageDashboard;
import ru.yandex.market.mbo.export.dumpstorage.ZkDumpStorageInfoService;
import ru.yandex.market.mbo.gwt.models.MboDumpSessionStatus;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransfer;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStep;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStepInfo;
import ru.yandex.market.mbo.gwt.models.transfer.ResultInfo;
import ru.yandex.market.mbo.gwt.models.transfer.step.TextResult;
import ru.yandex.market.mbo.gwt.models.transfer.step.TicketStepConfig;
import ru.yandex.market.mbo.synchronizer.export.ExportRegistry;
import ru.yandex.market.mbo.tms.modeltransfer.ModelTransferJobContext;
import ru.yandex.market.mbo.tms.modeltransfer.ModelTransferStepInfoBuilder;
import ru.yandex.market.mbo.tms.modeltransfer.ResultInfoBuilder;
import ru.yandex.market.mbo.utils.ZooKeeperServiceMock;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author dmserebr
 * @date 31.10.18
 */
@RunWith(MockitoJUnitRunner.class)
public class FreezeUnfreezeDumpStepProcessorTest {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ZooKeeperService zooKeeperService;
    private ZkDumpStorageInfoService dumpStorageInfoService;

    private FreezeDumpStepProcessor freezeDumpStepProcessor;
    private UnfreezeDumpStepProcessor unfreezeDumpStepProcessor;

    private static final ModelTransferStepInfo FREEZE_DUMP_STEP_INFO = ModelTransferStepInfoBuilder.newBuilder()
        .withStepType(ModelTransferStep.Type.FREEZE_DUMP_FOR_TRANSFER).build();
    private static final ModelTransferStepInfo UNFREEZE_DUMP_STEP_INFO = ModelTransferStepInfoBuilder.newBuilder()
        .withStepType(ModelTransferStep.Type.UNFREEZE_DUMP_FOR_TRANSFER).build();
    private static final ModelTransferStepInfo MOVE_MODELS_STEP_INFO = ModelTransferStepInfoBuilder.newBuilder()
        .withStepType(ModelTransferStep.Type.MOVE_MODELS)
        .withStepIsValidatable(true)
        .build();

    @Before
    public void before() {
        zooKeeperService = new ZooKeeperServiceMock();
        dumpStorageInfoService = new ZkDumpStorageInfoService();
        dumpStorageInfoService.setZooKeeperService(zooKeeperService);

        DumpStorageDashboard dumpStorageDashboard = new DumpStorageDashboard();
        dumpStorageDashboard.setDumpStorageInfoService(dumpStorageInfoService);

        ExportRegistry stuffExportRegistry = new ExportRegistry();
        stuffExportRegistry.setDumpName("stuff");
        stuffExportRegistry.setFolderNameFormat("yyyyMMdd_HHmm");
        ExportRegistry fastExportRegistry = new ExportRegistry();
        fastExportRegistry.setDumpName("fast");
        fastExportRegistry.setFolderNameFormat("yyyyMMdd_HHmm");

        freezeDumpStepProcessor = new FreezeDumpStepProcessor(dumpStorageDashboard,
            stuffExportRegistry, fastExportRegistry);
        unfreezeDumpStepProcessor = new UnfreezeDumpStepProcessor(dumpStorageDashboard,
            stuffExportRegistry, fastExportRegistry);
    }

    @Test
    public void testFreezeUnfreezeWhenOneSessionExistsForEachDump() {
        // Add good dumps to storage
        dumpStorageInfoService.updateSessionInfo("stuff", "20171012_0413", MboDumpSessionStatus.OK);
        dumpStorageInfoService.updateSessionInfo("fast", "20171012_0518", MboDumpSessionStatus.OK);

        List<ModelTransferStepInfo> stepInfos = Arrays.asList(
            FREEZE_DUMP_STEP_INFO, MOVE_MODELS_STEP_INFO, UNFREEZE_DUMP_STEP_INFO);

        TextResult freezeResult = freezeDumpStepProcessor.executeStep(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.IN_PROGRESS).build(),
            getModelTransferJobContext(FREEZE_DUMP_STEP_INFO, stepInfos)
        );

        // Check that freeze is successful
        Assert.assertEquals(ResultInfo.Status.COMPLETED, freezeResult.getResultInfo().getStatus());
        Assert.assertEquals("Замораживание выгрузок выполнено успешно. " +
            "Замороженный stuff: 20171012_0413, замороженный fast: 20171012_0518", freezeResult.getText());

        // Check that correct dumps are frozen
        Assert.assertEquals("20171012_0413", dumpStorageInfoService.getLockedDumpSessionId("stuff"));
        Assert.assertEquals("20171012_0518", dumpStorageInfoService.getLockedDumpSessionId("fast"));

        // Move the models
        MOVE_MODELS_STEP_INFO.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
            .completed(dateFromString("2017-10-13 12:18"))
            .build()
        );
        MOVE_MODELS_STEP_INFO.getValidationResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
                .completed(dateFromString("2017-10-13 13:25"))
                .build()
        );

        // Add more good dumps to storage (which have started after move model step is completed)
        dumpStorageInfoService.updateSessionInfo("stuff", "20171013_1518", MboDumpSessionStatus.OK);
        dumpStorageInfoService.updateSessionInfo("fast", "20171013_1436", MboDumpSessionStatus.OK);

        TextResult unfreezeResult = unfreezeDumpStepProcessor.executeStep(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.IN_PROGRESS).build(),
            getModelTransferJobContext(UNFREEZE_DUMP_STEP_INFO, stepInfos, MOVE_MODELS_STEP_INFO)
        );

        // Check that unfreeze is successful
        Assert.assertEquals(ResultInfo.Status.COMPLETED, unfreezeResult.getResultInfo().getStatus());
        Assert.assertEquals("Размораживание выгрузок выполнено успешно. " +
            "Актуальный stuff: 20171013_1518, актуальный fast: 20171013_1436", unfreezeResult.getText());

        // Check that no dumps are frozen
        Assert.assertEquals("", dumpStorageInfoService.getLockedDumpSessionId("stuff"));
        Assert.assertEquals("", dumpStorageInfoService.getLockedDumpSessionId("fast"));

    }

    @Test
    public void testFreezeFailsIfNoSessionsExist() {
        List<ModelTransferStepInfo> stepInfos = Arrays.asList(
            FREEZE_DUMP_STEP_INFO, MOVE_MODELS_STEP_INFO, UNFREEZE_DUMP_STEP_INFO);

        TextResult freezeResult = freezeDumpStepProcessor.executeStep(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.IN_PROGRESS).build(),
            getModelTransferJobContext(FREEZE_DUMP_STEP_INFO, stepInfos)
        );

        // Check that freeze is unsuccessful
        Assert.assertEquals(ResultInfo.Status.FAILED, freezeResult.getResultInfo().getStatus());
        Assert.assertEquals("Не удалось заморозить выгрузки: fast (не найдено подходящей сессии)," +
                " stuff (не найдено подходящей сессии)",
            freezeResult.getText());

        // Check that no dumps are frozen
        Assert.assertEquals("", dumpStorageInfoService.getLockedDumpSessionId("stuff"));
        Assert.assertEquals("", dumpStorageInfoService.getLockedDumpSessionId("fast"));
    }

    @Test
    public void testFreezeFailsIfNoGoodSessionsForOneOfDumpsExist() {
        // Add dumps to storage
        dumpStorageInfoService.updateSessionInfo("stuff", "20171007_1253", MboDumpSessionStatus.FAILED);
        dumpStorageInfoService.updateSessionInfo("stuff", "20171012_0223", MboDumpSessionStatus.FAILED);
        dumpStorageInfoService.updateSessionInfo("stuff", "20171012_0415", MboDumpSessionStatus.STARTED);
        dumpStorageInfoService.updateSessionInfo("fast", "20171012_0518", MboDumpSessionStatus.OK);

        List<ModelTransferStepInfo> stepInfos = Arrays.asList(
            FREEZE_DUMP_STEP_INFO, MOVE_MODELS_STEP_INFO, UNFREEZE_DUMP_STEP_INFO);

        TextResult freezeResult = freezeDumpStepProcessor.executeStep(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.IN_PROGRESS).build(),
            getModelTransferJobContext(FREEZE_DUMP_STEP_INFO, stepInfos)
        );

        // Check that freeze is unsuccessful
        Assert.assertEquals(ResultInfo.Status.FAILED, freezeResult.getResultInfo().getStatus());
        Assert.assertEquals("Не удалось заморозить выгрузки: stuff (не найдено подходящей сессии)",
            freezeResult.getText());

        // Check that both dumps are not frozen
        Assert.assertEquals("", dumpStorageInfoService.getLockedDumpSessionId("stuff"));
        Assert.assertEquals("", dumpStorageInfoService.getLockedDumpSessionId("fast"));
    }

    @Test
    public void testFreezeTakesCorrectSession() {
        // Add multiple dumps to storage
        dumpStorageInfoService.updateSessionInfo("stuff", "20171007_1253", MboDumpSessionStatus.FAILED);
        dumpStorageInfoService.updateSessionInfo("stuff", "20171010_1501", MboDumpSessionStatus.OK);
        dumpStorageInfoService.updateSessionInfo("stuff", "20171012_0118", MboDumpSessionStatus.OK);
        dumpStorageInfoService.updateSessionInfo("stuff", "20171012_0223", MboDumpSessionStatus.FAILED);
        dumpStorageInfoService.updateSessionInfo("stuff", "20171012_0415", MboDumpSessionStatus.STARTED);
        dumpStorageInfoService.updateSessionInfo("fast", "20171010_1210", MboDumpSessionStatus.FAILED);
        dumpStorageInfoService.updateSessionInfo("fast", "20171011_1018", MboDumpSessionStatus.OK);
        dumpStorageInfoService.updateSessionInfo("fast", "20171011_1422", MboDumpSessionStatus.OK);
        dumpStorageInfoService.updateSessionInfo("fast", "20171012_0518", MboDumpSessionStatus.STARTED);

        List<ModelTransferStepInfo> stepInfos = Arrays.asList(FREEZE_DUMP_STEP_INFO);

        TextResult freezeResult = freezeDumpStepProcessor.executeStep(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.IN_PROGRESS).build(),
            getModelTransferJobContext(FREEZE_DUMP_STEP_INFO, stepInfos)
        );

        // Check that freeze is successful
        Assert.assertEquals(ResultInfo.Status.COMPLETED, freezeResult.getResultInfo().getStatus());
        Assert.assertEquals("Замораживание выгрузок выполнено успешно. " +
            "Замороженный stuff: 20171012_0118, замороженный fast: 20171011_1422", freezeResult.getText());

        // Check that correct dumps are frozen
        Assert.assertEquals("20171012_0118", dumpStorageInfoService.getLockedDumpSessionId("stuff"));
        Assert.assertEquals("20171011_1422", dumpStorageInfoService.getLockedDumpSessionId("fast"));
    }

    @Test
    public void testUnfreezeFailsWhenMoveModelsNotExecuted() {
        // Add good dumps to storage
        dumpStorageInfoService.updateSessionInfo("stuff", "20171012_0413", MboDumpSessionStatus.OK);
        dumpStorageInfoService.updateSessionInfo("fast", "20171012_0518", MboDumpSessionStatus.OK);

        List<ModelTransferStepInfo> stepInfos = Arrays.asList(
            FREEZE_DUMP_STEP_INFO, MOVE_MODELS_STEP_INFO, UNFREEZE_DUMP_STEP_INFO);

        TextResult freezeResult = freezeDumpStepProcessor.executeStep(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.IN_PROGRESS).build(),
            getModelTransferJobContext(FREEZE_DUMP_STEP_INFO, stepInfos)
        );

        // Check that freeze is successful
        Assert.assertEquals(ResultInfo.Status.COMPLETED, freezeResult.getResultInfo().getStatus());
        Assert.assertEquals("Замораживание выгрузок выполнено успешно. " +
            "Замороженный stuff: 20171012_0413, замороженный fast: 20171012_0518", freezeResult.getText());

        // Check that both dumps are frozen
        Assert.assertEquals("20171012_0413", dumpStorageInfoService.getLockedDumpSessionId("stuff"));
        Assert.assertEquals("20171012_0518", dumpStorageInfoService.getLockedDumpSessionId("fast"));

        TextResult unfreezeResult = unfreezeDumpStepProcessor.executeStep(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.IN_PROGRESS).build(),
            getModelTransferJobContext(UNFREEZE_DUMP_STEP_INFO, stepInfos, MOVE_MODELS_STEP_INFO)
        );

        // Check that unfreeze fails
        Assert.assertEquals(ResultInfo.Status.FAILED, unfreezeResult.getResultInfo().getStatus());
        Assert.assertEquals("Не удалось разморозить выгрузки - не завершены шаги: Перенос моделей",
            unfreezeResult.getText());

        // Check that both dumps are still frozen
        Assert.assertEquals("20171012_0413", dumpStorageInfoService.getLockedDumpSessionId("stuff"));
        Assert.assertEquals("20171012_0518", dumpStorageInfoService.getLockedDumpSessionId("fast"));
    }

    @Test
    public void testUnfreezeFailsWhenMoveModelsExecutedButNotValidated() {
        // Add good dumps to storage
        dumpStorageInfoService.updateSessionInfo("stuff", "20171012_0413", MboDumpSessionStatus.OK);
        dumpStorageInfoService.updateSessionInfo("fast", "20171012_0518", MboDumpSessionStatus.OK);

        List<ModelTransferStepInfo> stepInfos = Arrays.asList(
            FREEZE_DUMP_STEP_INFO, MOVE_MODELS_STEP_INFO, UNFREEZE_DUMP_STEP_INFO);

        TextResult freezeResult = freezeDumpStepProcessor.executeStep(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.IN_PROGRESS).build(),
            getModelTransferJobContext(FREEZE_DUMP_STEP_INFO, stepInfos)
        );

        // Check that freeze is successful
        Assert.assertEquals(ResultInfo.Status.COMPLETED, freezeResult.getResultInfo().getStatus());
        Assert.assertEquals("Замораживание выгрузок выполнено успешно. " +
            "Замороженный stuff: 20171012_0413, замороженный fast: 20171012_0518", freezeResult.getText());

        // Move the models
        MOVE_MODELS_STEP_INFO.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
                .completed(dateFromString("2017-10-13 12:18"))
                .build()
        );
        MOVE_MODELS_STEP_INFO.getValidationResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.IN_PROGRESS)
                .build()
        );

        TextResult unfreezeResult = unfreezeDumpStepProcessor.executeStep(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.IN_PROGRESS).build(),
            getModelTransferJobContext(UNFREEZE_DUMP_STEP_INFO, stepInfos, MOVE_MODELS_STEP_INFO)
        );

        // Check that unfreeze fails
        Assert.assertEquals(ResultInfo.Status.FAILED, unfreezeResult.getResultInfo().getStatus());
        Assert.assertEquals("Не удалось разморозить выгрузки - не завершены шаги: Перенос моделей",
            unfreezeResult.getText());

        // Check that both dumps are still frozen
        Assert.assertEquals("20171012_0413", dumpStorageInfoService.getLockedDumpSessionId("stuff"));
        Assert.assertEquals("20171012_0518", dumpStorageInfoService.getLockedDumpSessionId("fast"));
    }

    @Test
    public void testUnfreezeFailsWhenNoFreshDumpsPresent() {
        // Add good dumps to storage
        dumpStorageInfoService.updateSessionInfo("stuff", "20171012_0413", MboDumpSessionStatus.OK);
        dumpStorageInfoService.updateSessionInfo("fast", "20171012_0518", MboDumpSessionStatus.OK);

        List<ModelTransferStepInfo> stepInfos = Arrays.asList(
            FREEZE_DUMP_STEP_INFO, MOVE_MODELS_STEP_INFO, UNFREEZE_DUMP_STEP_INFO);

        TextResult freezeResult = freezeDumpStepProcessor.executeStep(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.IN_PROGRESS).build(),
            getModelTransferJobContext(FREEZE_DUMP_STEP_INFO, stepInfos)
        );

        // Check that freeze is successful
        Assert.assertEquals(ResultInfo.Status.COMPLETED, freezeResult.getResultInfo().getStatus());
        Assert.assertEquals("Замораживание выгрузок выполнено успешно. " +
            "Замороженный stuff: 20171012_0413, замороженный fast: 20171012_0518", freezeResult.getText());

        // Move the models
        MOVE_MODELS_STEP_INFO.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
                .completed(dateFromString("2017-10-13 12:18"))
                .build()
        );
        MOVE_MODELS_STEP_INFO.getValidationResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
                .completed(dateFromString("2017-10-13 13:25"))
                .build()
        );

        TextResult unfreezeResult = unfreezeDumpStepProcessor.executeStep(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.IN_PROGRESS).build(),
            getModelTransferJobContext(UNFREEZE_DUMP_STEP_INFO, stepInfos, MOVE_MODELS_STEP_INFO)
        );

        // Check that unfreeze failed
        Assert.assertEquals(ResultInfo.Status.FAILED, unfreezeResult.getResultInfo().getStatus());
        Assert.assertEquals("Не удалось разморозить выгрузки: не найдены подходящие сессии для stuff, fast",
            unfreezeResult.getText());

        // Check that both dumps are still frozen
        Assert.assertEquals("20171012_0413", dumpStorageInfoService.getLockedDumpSessionId("stuff"));
        Assert.assertEquals("20171012_0518", dumpStorageInfoService.getLockedDumpSessionId("fast"));
    }

    @Test
    public void testUnfreezeFailsWhenNoFreshDumpPresentForStuff() {
        // Add good dumps to storage
        dumpStorageInfoService.updateSessionInfo("stuff", "20171012_0413", MboDumpSessionStatus.OK);
        dumpStorageInfoService.updateSessionInfo("fast", "20171012_0518", MboDumpSessionStatus.OK);

        List<ModelTransferStepInfo> stepInfos = Arrays.asList(
            FREEZE_DUMP_STEP_INFO, MOVE_MODELS_STEP_INFO, UNFREEZE_DUMP_STEP_INFO);

        TextResult freezeResult = freezeDumpStepProcessor.executeStep(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.IN_PROGRESS).build(),
            getModelTransferJobContext(FREEZE_DUMP_STEP_INFO, stepInfos)
        );

        // Check that freeze is successful
        Assert.assertEquals(ResultInfo.Status.COMPLETED, freezeResult.getResultInfo().getStatus());
        Assert.assertEquals("Замораживание выгрузок выполнено успешно. " +
            "Замороженный stuff: 20171012_0413, замороженный fast: 20171012_0518", freezeResult.getText());

        // Move the models
        MOVE_MODELS_STEP_INFO.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
                .completed(dateFromString("2017-10-13 12:18"))
                .build()
        );
        MOVE_MODELS_STEP_INFO.getValidationResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
                .completed(dateFromString("2017-10-13 13:25"))
                .build()
        );

        // Add some dumps to storage (for stuff, the dumps are older than move models step or not ok)
        dumpStorageInfoService.updateSessionInfo("stuff", "20171013_1322", MboDumpSessionStatus.OK);
        dumpStorageInfoService.updateSessionInfo("stuff", "20171013_1518", MboDumpSessionStatus.FAILED);
        dumpStorageInfoService.updateSessionInfo("stuff", "20171013_1610", MboDumpSessionStatus.FAILED);
        dumpStorageInfoService.updateSessionInfo("stuff", "20171013_1632", MboDumpSessionStatus.STARTED);
        dumpStorageInfoService.updateSessionInfo("fast", "20171013_1301", MboDumpSessionStatus.OK);
        dumpStorageInfoService.updateSessionInfo("fast", "20171013_1436", MboDumpSessionStatus.OK);
        dumpStorageInfoService.updateSessionInfo("fast", "20171013_1512", MboDumpSessionStatus.OK);
        dumpStorageInfoService.updateSessionInfo("fast", "20171013_1549", MboDumpSessionStatus.FAILED);

        TextResult unfreezeResult = unfreezeDumpStepProcessor.executeStep(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.IN_PROGRESS).build(),
            getModelTransferJobContext(UNFREEZE_DUMP_STEP_INFO, stepInfos, MOVE_MODELS_STEP_INFO)
        );

        // Check that unfreeze failed
        Assert.assertEquals(ResultInfo.Status.FAILED, unfreezeResult.getResultInfo().getStatus());
        Assert.assertEquals("Не удалось разморозить выгрузки: не найдены подходящие сессии для stuff",
            unfreezeResult.getText());

        // Check that both dumps are still frozen
        Assert.assertEquals("20171012_0413", dumpStorageInfoService.getLockedDumpSessionId("stuff"));
        Assert.assertEquals("20171012_0518", dumpStorageInfoService.getLockedDumpSessionId("fast"));
    }


    private ModelTransferJobContext<TicketStepConfig> getModelTransferJobContext(ModelTransferStepInfo currentInfo,
                                                               List<ModelTransferStepInfo> allInfos,
                                                               ModelTransferStepInfo... dependencyStepInfos) {
        ModelTransfer modelTransfer = new ModelTransfer();
        return new ModelTransferJobContext<>(modelTransfer, currentInfo, allInfos, new TicketStepConfig(),
            Arrays.asList(dependencyStepInfos));
    }

    private static Date dateFromString(String string) {
        return Date.from(LocalDateTime.parse(string, DATE_TIME_FORMATTER).toInstant(OffsetDateTime.now().getOffset()));
    }
}
