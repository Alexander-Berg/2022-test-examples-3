package ru.yandex.market.core.operations;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.operations.model.OperationInfo;
import ru.yandex.market.core.operations.model.OperationResult;
import ru.yandex.market.core.operations.model.OperationStatus;
import ru.yandex.market.core.operations.model.OperationType;
import ru.yandex.market.core.operations.model.OperationTypeInfo;
import ru.yandex.market.core.operations.model.OperationUpload;
import ru.yandex.market.core.operations.model.params.BusinessMigrationParams;
import ru.yandex.market.core.operations.model.params.DefaultOperationParams;

/**
 * Тесты для {@link OperationService}.
 */
@DbUnitDataSet(before = "DefaultOperationService.before.csv")
class DefaultOperationServiceFunctionalTest extends FunctionalTest {

    @Autowired
    private OperationService operationService;

    @Test
    @DbUnitDataSet(after = "DefaultOperationService.prepareOperationTest.after.csv")
    public void prepareOperationTest() {
        operationService.prepareOperation(OperationInfo.<DefaultOperationParams>newBuilder()
                .withType(OperationType.DEFAULT_PULL_OPERATION)
                .withStatus(OperationStatus.OK)
                .withPartnerId(777L)
                .withParams(new DefaultOperationParams(777L))
                .withExternalType("external_type")
                .withOriginalOperationId(100L)
                .build());
    }

    @Test
    @DbUnitDataSet(before = "DefaultOperationService.getOperationInfoTest.before.csv")
    public void getOperationInfoTest() {
        Optional<OperationInfo<BusinessMigrationParams>> info = operationService.getOperationInfo(1L);
        Assertions.assertTrue(info.isPresent());
        OperationInfo<BusinessMigrationParams> operationInfo = info.get();
        Assertions.assertEquals(1L, operationInfo.getOperationId());
        Assertions.assertEquals(OperationType.BUSINESS_MIGRATION, operationInfo.getType());
        Assertions.assertEquals(OperationStatus.OK, operationInfo.getStatus());
        Assertions.assertEquals(777L, operationInfo.getPartnerId());
        BusinessMigrationParams operationParams = operationInfo.getParams();
        Assertions.assertEquals(777L, operationParams.getServiceId());
        Assertions.assertEquals("external_type", operationInfo.getExternalType());
        Assertions.assertEquals(100, operationInfo.getOriginalOperationId().get());
        Assertions.assertEquals(LocalDateTime.of(
                2021, 3, 28, 10, 0, 0
        ), operationInfo.getRequestTime());
    }

    @Test
    @DbUnitDataSet(before = "DefaultOperationService.getRunningOperationsTest.before.csv")
    public void getRunningOperationsTest() {
        List<OperationInfo<DefaultOperationParams>> operationInfoList = operationService.getRunningOperations();
        Assertions.assertEquals(2, operationInfoList.size());
        Assertions.assertEquals(List.of(2L, 3L),
                operationInfoList.stream().map(OperationInfo::getOperationId).collect(Collectors.toList()));
    }

    @Test
    @DbUnitDataSet(before = "DefaultOperationService.processOperationResultTest.before.csv",
            after = "DefaultOperationService.processOperationResultTest.after.csv")
    public void processOperationResultTest() {
        operationService.processOperationResult(OperationResult.newBuilder()
                .withOperationId(1L)
                .withOperationUpload(OperationUpload.newBuilder()
                        .withId(0L)
                        .build())
                .withTotalOffers(3)
                .withProcessedOffers(1)
                .withWarningOffers(1)
                .withErrorOffers(1)
                .build());
    }

    @Test
    @DbUnitDataSet(before = "DefaultOperationService.saveErrorOperationResultTest.before.csv",
            after = "DefaultOperationService.saveErrorOperationResultTest.after.csv")
    public void saveErrorOperationResultTest() {
        operationService.saveErrorOperationResult(1L);
    }

    @Test
    @DbUnitDataSet(before = "DefaultOperationService.setExternalRequestId.before.csv",
            after = "DefaultOperationService.setExternalRequestId.after.csv")
    public void setExternalRequestIdTest() {
        operationService.setExternalRequestId(1L, "m12345678");
    }

    @Test
    public void getOperationTypeInfoTest() {
        Optional<OperationTypeInfo> typeInfo = operationService.getOperationTypeInfo(OperationType.DEFAULT_PULL_OPERATION);
        Assertions.assertTrue(typeInfo.isPresent());
        OperationTypeInfo operationTypeInfo = typeInfo.get();
        Assertions.assertEquals(0L, operationTypeInfo.getId());
        Assertions.assertEquals("DEFAULT_PULL_OPERATION", operationTypeInfo.getName());
        Assertions.assertEquals("Стандартная pull операция", operationTypeInfo.getDescription());
        Assertions.assertEquals(3600, operationTypeInfo.getTreshold());
        Assertions.assertFalse(operationTypeInfo.isPush());
        operationService.updateMonitorThreshold(OperationType.DEFAULT_PULL_OPERATION, 4000);
        typeInfo = operationService.getOperationTypeInfo(OperationType.DEFAULT_PULL_OPERATION);
        operationTypeInfo = typeInfo.get();
        Assertions.assertEquals(0L, operationTypeInfo.getId());
        Assertions.assertEquals("DEFAULT_PULL_OPERATION", operationTypeInfo.getName());
        Assertions.assertEquals("Стандартная pull операция", operationTypeInfo.getDescription());
        Assertions.assertEquals(4000, operationTypeInfo.getTreshold());
    }

    @Test
    @DbUnitDataSet(before = "DefaultOperationService.getOperationUploadTest.before.csv")
    public void getOperationUploadTest() {
        Optional<OperationUpload> upload = operationService.getOperationUpload(0L);
        Assertions.assertTrue(upload.isPresent());
        OperationUpload operationUpload = upload.get();
        Assertions.assertEquals(0L, operationUpload.getId());
        Assertions.assertEquals("http://path/to", operationUpload.getUrlToDownload());
        Assertions.assertEquals("filename", operationUpload.getFilename());
        Assertions.assertEquals(777, operationUpload.getPartnerId());
    }

    @Test
    @DbUnitDataSet(before = "DefaultOperationService.addOperationUploadTest.before.csv",
            after = "DefaultOperationService.addOperationUploadTest.after.csv")
    public void addOperationUploadTest() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        operationService.addOperationUpload(1L, OperationUpload.newBuilder()
                .withId(1L)
                .withUrlToDownload("http://from/where")
                .withFilename("filo")
                .withPartnerId(776L)
                .withUploadDate(LocalDateTime.parse("2020-12-01 00:00", fmt))
                .build());
    }

    @Test
    @DbUnitDataSet(before = "DefaultOperationService.lockOperationTest.before.csv")
    public void lockOperationTest() {
        Assertions.assertTrue(operationService.lockOperation(1L));
    }
}
