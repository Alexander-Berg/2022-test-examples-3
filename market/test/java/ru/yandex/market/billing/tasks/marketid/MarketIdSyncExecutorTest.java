package ru.yandex.market.billing.tasks.marketid;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DataSetType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.id.ContactModificationResponse;
import ru.yandex.market.id.GetByRegistrationNumberResponse;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.id.UpdateLegalInfoRequest;
import ru.yandex.market.id.UpdateLegalInfoResponse;
import ru.yandex.market.marketid.MarketIdSyncExecutor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MarketIdSyncExecutorTest extends FunctionalTest {

    @Autowired
    private MarketIdSyncExecutor marketIdSyncExecutor;

    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    @Test
    @DbUnitDataSet(
            type = DataSetType.SINGLE_CSV,
            before = "MarketIdSyncExecutorTest.before.csv",
            after = "MarketIdSyncExecutorTest.after.csv"
    )
    void testMarketIdSynchronization() {
        int[] marketId = {2000002};
        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(marketId[0]).build());
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).confirmLegalInfo(any(), any());
        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(marketId[0]).build());
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getOrCreateMarketId(any(), any());
        doAnswer(invocation -> {
            StreamObserver<ContactModificationResponse> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(
                    ContactModificationResponse
                            .newBuilder()
                            .setSuccess(true)
                            .setMessage("OK")
                            .build());
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).syncContactsRequest(any(), any());
        //link marketId
        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(marketId[0]++).build());
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).linkMarketIdRequest(any(), any());
        //update legal info
        doAnswer(invocation -> {
            UpdateLegalInfoRequest request = invocation.getArgument(0);
            StreamObserver<UpdateLegalInfoResponse> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(UpdateLegalInfoResponse.newBuilder().setSuccess(request.hasTimestamp()).build());
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).updateLegalInfo(any(), any());
        //get by registration number
        doAnswer(invocation -> {
            StreamObserver<GetByRegistrationNumberResponse> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(
                    GetByRegistrationNumberResponse
                            .newBuilder()
                            .setSuccess(false)
                            .build());
            marketAccountStreamObserver.onCompleted();
            return null;
        })
                .doAnswer(invocation -> {
                    StreamObserver<GetByRegistrationNumberResponse> marketAccountStreamObserver = invocation.getArgument(1);
                    marketAccountStreamObserver.onNext(
                            GetByRegistrationNumberResponse
                                    .newBuilder()
                                    .setSuccess(true)
                                    .setMarketId(2000002)
                                    .build());
                    marketAccountStreamObserver.onCompleted();
                    return null;
                })
                .when(marketIdServiceImplBase).getByRegistrationNumber(any(), any());
        marketIdSyncExecutor.doJob(Mockito.mock(JobExecutionContext.class));
        verify(marketIdServiceImplBase, times(1)).getOrCreateMarketId(any(), any());
        verify(marketIdServiceImplBase, times(3)).linkMarketIdRequest(any(), any());
        verify(marketIdServiceImplBase, times(4)).updateLegalInfo(any(), any());
        verify(marketIdServiceImplBase, times(5)).confirmLegalInfo(any(), any());
        verify(marketIdServiceImplBase, times(4)).syncContactsRequest(any(), any());
    }
}
