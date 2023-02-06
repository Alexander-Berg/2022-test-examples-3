package ru.yandex.market.mbi.bpmn.process.migration;

import java.util.Map;

import io.grpc.stub.StreamObserver;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.business.migration.BusinessMigration;
import ru.yandex.market.business.migration.BusinessMigrationServiceGrpc;
import ru.yandex.market.core.asyncreport.ReportState;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponse;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponseStatus;
import ru.yandex.market.mbi.api.client.entity.business.CanMigrateVerdictDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerInUnitedCatalogDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerIndexedWithBusinessDTO;
import ru.yandex.market.mbi.bpmn.FunctionalTest;
import ru.yandex.market.mbi.bpmn.model.enums.ProcessType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.reset;
import static ru.yandex.market.mbi.bpmn.util.AsyncReportTestUtil.TEST_REPORT_ID;
import static ru.yandex.market.mbi.bpmn.util.AsyncReportTestUtil.getReportInfoDTO;

public abstract class AbstractMigrationTest extends FunctionalTest {

    protected static final Integer OPERATION_ID = 123456;
    protected static final Integer SRC_BUSINESS_ID = 1111;
    protected static final Integer DST_BUSINES_ID = 2222;
    protected static final Integer SERVICE_ID = 11;


    @Autowired
    @Qualifier("mbocBusinessMigrationService")
    public BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase mboService;

    @Autowired
    @Qualifier("mdmBusinessMigrationService")
    public BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase mdmService;

    @Autowired
    @Qualifier("pppBusinessMigrationService")
    public BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase pppService;


    public static final String REASON = "God knows why!";
    public static final long ENTITY_ID = 11;


    @BeforeEach
    void initClientDefaults() {
        reset(pppService);
        reset(mboService);
        reset(mdmService);

        willAnswer(invocation -> getReportInfoDTO(ENTITY_ID, ReportState.PROCESSING))
                .given(mbiApiClient).requestReportGeneration(any());
        willAnswer(invocation -> getReportInfoDTO(ENTITY_ID, ReportState.DONE))
                .given(mbiApiClient).getReportInfo(eq(TEST_REPORT_ID));
        //MBI: migration possible for shop
        willAnswer(invocation -> CanMigrateVerdictDTO.yes()).given(mbiApiClient)
                .canMigrate(anyLong(), anyLong(), anyLong());
        //MBI: migration possible for supplier
        willAnswer(invocation -> CanMigrateVerdictDTO.yes()).given(mbiApiClient)
                .canMigrateToUCat(anyLong(), anyLong());
        //MBI: lock
        willAnswer(invocation -> successMbiLockResponse()).given(mbiOpenApiClient).lockBusiness(any());
        willAnswer(invocation -> successMbiUnlockResponse()).given(mbiOpenApiClient).unlockBusiness(any());
        //DataCamp: lock
        willAnswer(invocation -> successLockResponse()).given(dataCampShopClient).lock(any());
        willAnswer(invocation -> successUnlockResponse()).given(dataCampShopClient).unlock(any());
        //MDM: lock
        willAnswer(this::successLockAnswer).given(mdmService).lock(any(), any());
        willAnswer(this::successUnlockAnswer).given(mdmService).unlock(any(), any());
        //given
        willAnswer(invocation -> asyncFinishAnswer(invocation, pendingResponse()))
                .willAnswer(invocation -> asyncFinishAnswer(invocation, successResponse()))
                .given(mboService).asyncFinish(any(), any());
        //MBO: lock
        willAnswer(this::successLockAnswer).given(mboService).lock(any(), any());
        willAnswer(this::successUnlockAnswer).given(mboService).unlock(any(), any());
        //PPP: lock
        willAnswer(this::successLockAnswer).given(pppService).lock(any(), any());
        willAnswer(this::successUnlockAnswer).given(pppService).unlock(any(), any());
        //MBI-API mocks
        willAnswer(invocation -> successBusinessIdChange()).given(mbiApiClient).changeBusiness(any());
        willAnswer(invocation -> successEnableUCatStatus()).given(mbiApiClient).enableUnitedCatalog(anyLong(), any());
        //MBI: notify
        willAnswer(invocation -> GenericCallResponse.ok()).given(mbiApiClient).updateOperationStatus(any());

    }

    protected ProcessInstance runMigrationProcessInstance(ProcessType processType) {
        return runWithVariables(processType, getDefaultVariables());
    }

    protected ProcessInstance runWithVariables(ProcessType processType, Map<String, Object> variables) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        return runtimeService.startProcessInstanceByKey(
                processType.getId(),
                TEST_BUSINESS_KEY,
                variables
        );
    }

    private Map<String, Object> getDefaultVariables() {
        return Map.of(
                "operationId", OPERATION_ID,
                "srcBusinessId", SRC_BUSINESS_ID,
                "dstBusinessId", DST_BUSINES_ID,
                "serviceId", SERVICE_ID
        );
    }


    public ru.yandex.market.mbi.open.api.client.model.GenericCallResponse successMbiLockResponse() {
        return new ru.yandex.market.mbi.open.api.client.model.GenericCallResponse()
                .status(ru.yandex.market.mbi.open.api.client.model.GenericCallResponseStatus.OK)
                .message("");
    }

    public ru.yandex.market.mbi.open.api.client.model.GenericCallResponse successMbiUnlockResponse() {
        return new ru.yandex.market.mbi.open.api.client.model.GenericCallResponse()
                .status(ru.yandex.market.mbi.open.api.client.model.GenericCallResponseStatus.OK)
                .message("");
    }

    public ru.yandex.market.mbi.open.api.client.model.GenericCallResponse failMbiLockResponse() {
        return new ru.yandex.market.mbi.open.api.client.model.GenericCallResponse()
                .status(ru.yandex.market.mbi.open.api.client.model.GenericCallResponseStatus.ERROR)
                .message(REASON);
    }

    public GenericCallResponse successBusinessIdChange() {
        return GenericCallResponse.ok();
    }

    public GenericCallResponse failBusinessIdChange() {
        return new GenericCallResponse(GenericCallResponseStatus.ERROR, "Test error");
    }

    public GenericCallResponse successEnableUCatStatus() {
        return new GenericCallResponse(GenericCallResponseStatus.OK, "Признак Единого каталога установлен.");
    }

    public GenericCallResponse failEnableUCatStatus() {
        return new GenericCallResponse(GenericCallResponseStatus.ERROR, "Exception.");
    }

    public PartnerIndexedWithBusinessDTO successMbiCheckPartnerBusinessIdResponse(long partnerId, long businessId) {
        return PartnerIndexedWithBusinessDTO.yes(partnerId, businessId);
    }

    public PartnerIndexedWithBusinessDTO failMbiCheckPartnerBusinessIdResponse(long partnerId, long businessId) {
        return PartnerIndexedWithBusinessDTO.no(partnerId, businessId);
    }

    public PartnerInUnitedCatalogDTO successIsSupplierIndexedWithUCatResponse(long supplier) {
        return PartnerInUnitedCatalogDTO.yes(supplier);
    }

    public PartnerInUnitedCatalogDTO failIsSupplierIndexedWithUCatResponse(long supplier) {
        return PartnerInUnitedCatalogDTO.no(supplier);
    }

    public BusinessMigration.LockBusinessResponse successLockResponse() {
        return BusinessMigration.LockBusinessResponse.newBuilder()
                .setStatus(BusinessMigration.Status.SUCCESS)
                .build();
    }

    public BusinessMigration.UnlockBusinessResponse successUnlockResponse() {
        return BusinessMigration.UnlockBusinessResponse.newBuilder()
                .setStatus(BusinessMigration.Status.SUCCESS)
                .build();
    }

    public BusinessMigration.LockBusinessResponse failLockResponse() {
        return BusinessMigration.LockBusinessResponse.newBuilder()
                .setStatus(BusinessMigration.Status.FAIL)
                .setMessage("ErrorMessage")
                .build();
    }

    public BusinessMigration.LockBusinessResponse pendingLockResponse() {
        return BusinessMigration.LockBusinessResponse.newBuilder()
                .setStatus(BusinessMigration.Status.IN_PROGRESS)
                .setMessage("ErrorMessage")
                .build();
    }

    public Object successLockAnswer(InvocationOnMock invocation) {
        StreamObserver<BusinessMigration.LockBusinessResponse> businessLockStreamObserver =
                invocation.getArgument(1);
        businessLockStreamObserver.onNext(successLockResponse());
        businessLockStreamObserver.onCompleted();
        return null;
    }

    public Object successUnlockAnswer(InvocationOnMock invocation) {
        StreamObserver<BusinessMigration.UnlockBusinessResponse> businessUnlockStreamObserver =
                invocation.getArgument(1);
        businessUnlockStreamObserver.onNext(successUnlockResponse());
        businessUnlockStreamObserver.onCompleted();
        return null;
    }

    public Object failLockAnswer(InvocationOnMock invocation) {
        StreamObserver<BusinessMigration.LockBusinessResponse> businessLockStreamObserver =
                invocation.getArgument(1);
        businessLockStreamObserver.onNext(failLockResponse());
        businessLockStreamObserver.onCompleted();
        return null;
    }

    public Object pendingLockAnswer(InvocationOnMock invocation) {
        StreamObserver<BusinessMigration.LockBusinessResponse> businessLockStreamObserver =
                invocation.getArgument(1);
        businessLockStreamObserver.onNext(pendingLockResponse());
        businessLockStreamObserver.onCompleted();
        return null;
    }

    public Object asyncFinishAnswer(InvocationOnMock invocation,
                                    BusinessMigration.AsyncFinishBusinessResponse response) {
        StreamObserver<BusinessMigration.AsyncFinishBusinessResponse> asyncFinishBusinessResponseStreamObserver =
                invocation.getArgument(1);
        asyncFinishBusinessResponseStreamObserver.onNext(response);
        asyncFinishBusinessResponseStreamObserver.onCompleted();
        return null;
    }

    public BusinessMigration.AsyncFinishBusinessResponse successResponse() {
        return BusinessMigration.AsyncFinishBusinessResponse.newBuilder()
                .setStatus(BusinessMigration.Status.SUCCESS)
                .build();
    }

    public BusinessMigration.AsyncFinishBusinessResponse pendingResponse() {
        return BusinessMigration.AsyncFinishBusinessResponse.newBuilder()
                .setStatus(BusinessMigration.Status.IN_PROGRESS)
                .build();
    }
}
