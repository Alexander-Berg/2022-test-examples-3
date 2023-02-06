package ru.yandex.market.rg.asyncreport.unitedcatalog.migration;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.business.migration.BusinessMigration;
import ru.yandex.market.business.migration.BusinessMigrationServiceGrpc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class ExternalSystemMergerStepTest {
    private static final String SERVER_NAME = "externalSystem";

    static DistributedMigrator migrator;
    static Server server;
    static BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase service;

    ExternalSystemMergerStep step;

    @BeforeAll
    static void init() throws IOException {
        service = mock(BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase.class,
                delegatesTo(new BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase() {
                }));

        server = InProcessServerBuilder
                .forName("externalSystem").directExecutor().addService(service)
                .build();

        var grpcClient = BusinessMigrationServiceGrpc.newBlockingStub(
                InProcessChannelBuilder.forName(SERVER_NAME).directExecutor().build());

        migrator = new DistributedMigrator(UnitedCatalogSystem.MASTER_DATA_MANAGEMENT, grpcClient);

        server.start();
    }

    @AfterAll
    static void purge() {
        server.shutdown();
    }

    @BeforeEach
    void setUp() {
        step = new ExternalSystemMergerStep(migrator);
        reset(service);
    }

    @Test
    void empty() {
        step.accept(List.of());
        verifyZeroInteractions(service);
    }

    @Test
    void errorResponse() {
        var offer = UnitedOfferBuilder.offerBuilder();

        doAnswer(delegatesTo(mockService(req ->
                BusinessMigration.MergeOffersResponse.newBuilder()
                        .setSuccess(false)
                        .build()
        ))).when(service).merge(any(), any());
        var items = List.of(
                BusinessMigration.MergeOffersRequestItem.newBuilder()
                        .setSource(offer.build())
                        .setResult(offer.build())
        );
        assertThrows(MigrationException.class, () -> step.accept(items));

        verify(service).merge(any(), any());
    }

    @Test
    void singleItem() {
        prepareAndRunSingleOffer();

        var captor = ArgumentCaptor.forClass(BusinessMigration.MergeOffersRequest.class);
        verify(service).merge(captor.capture(), any());
        var item = captor.getValue().getMergeRequestItem(0);
        assertThat(item.getSource(), // в запросах так же офер целиком
                equalTo(UnitedOfferBuilder.offerBuilder(1, 10, "100").withBarcode("111222333").build()));
        assertThat(item.getResult(),
                equalTo(UnitedOfferBuilder.offerBuilder(1, 10, "100").withBarcode("111222333").build()));
        assertFalse(item.hasTarget(), "hasTarget");
    }

    @Test
    void testOnlyIds() {
        // пишем только id в запросах
        step = new ExternalSystemMergerStep(migrator,
                MigrationConfig.newBuilder().setMergeOnlyIds(true).build());

        prepareAndRunSingleOffer();

        var captor = ArgumentCaptor.forClass(BusinessMigration.MergeOffersRequest.class);
        verify(service).merge(captor.capture(), any());
        var item = captor.getValue().getMergeRequestItem(0);
        assertThat(item.getSource(), // а в запросах НЕ было полей, кроме id
                equalTo(UnitedOfferBuilder.offerBuilder(1, 10, "100").build()));
        assertThat(item.getResult(),
                equalTo(UnitedOfferBuilder.offerBuilder(1, 10, "100").build()));
        assertFalse(item.hasTarget(), "hasTarget");
    }

    void prepareAndRunSingleOffer() {
        var offer = UnitedOfferBuilder.offerBuilder(1, 10, "100").withBarcode("111222333");
        var requestItem = BusinessMigration.MergeOffersRequestItem.newBuilder()
                .setSource(offer.build())
                .setResult(offer.build());
        var responseResult = UnitedOfferBuilder.offerBuilder(1, 10, "100").withName("test");

        doAnswer(delegatesTo(mockService(req ->
                BusinessMigration.MergeOffersResponse.newBuilder()
                        .setSuccess(true)
                        .addMergeResponseItem(BusinessMigration.MergeOffersResponseItem.newBuilder()
                                .setResult(responseResult.build())
                        ).build()
        ))).when(service).merge(any(), any());
        var items = List.of(requestItem);
        step.accept(items);
    }

    BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase mockService(
            Function<BusinessMigration.MergeOffersRequest, BusinessMigration.MergeOffersResponse> fun) {
        return new BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase() {
            @Override
            public void merge(BusinessMigration.MergeOffersRequest request,
                                 StreamObserver<BusinessMigration.MergeOffersResponse> responseObserver) {
                var resp = fun.apply(request);
                responseObserver.onNext(resp);
                responseObserver.onCompleted();
            }
        };
    }

}
