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
import ru.yandex.market.core.asyncreport.model.unitedcatalog.CopyOffersParams;

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
class ExternalSystemPopulateStepTest {
    private static final String SERVER_NAME = "externalSystem";

    static DistributedMigrator migrator;
    static Server server;
    static BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase service;

    ExternalSystemPopulateStep step;

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
        step = new ExternalSystemPopulateStep(migrator, new CopyOffersParams());
        reset(service);
    }

    @Test
    void empty() {
        step.accept(List.of());
        verifyZeroInteractions(service);
    }

    @Test
    void emptyResponse() {
        var offer = UnitedOfferBuilder.offerBuilder();

        doAnswer(delegatesTo(mockService(req ->
                BusinessMigration.PopulateOffersResponse.newBuilder()
                        .setSuccess(true)
                        .build()
        ))).when(service).populate(any(), any());
        var items = List.of(
                BusinessMigration.MergeOffersRequestItem.newBuilder()
                        .setSource(offer.build())
                        .setResult(offer.build())
        );
        step.accept(items);

        verify(service).populate(any(), any());
        assertThat(items.get(0).getResult(), equalTo(items.get(0).getSource()));
    }

    @Test
    void errorResponse() {
        var offer = UnitedOfferBuilder.offerBuilder();

        doAnswer(delegatesTo(mockService(req ->
                BusinessMigration.PopulateOffersResponse.newBuilder()
                        .setSuccess(false)
                        .build()
        ))).when(service).populate(any(), any());
        var items = List.of(
                BusinessMigration.MergeOffersRequestItem.newBuilder()
                        .setSource(offer.build())
                        .setResult(offer.build())
        );
        assertThrows(MigrationException.class, () -> step.accept(items));

        verify(service).populate(any(), any());
    }

    @Test
    void singleItem() {
        var items = prepareAndRunSingleOffer();

        var captor = ArgumentCaptor.forClass(BusinessMigration.PopulateOffersRequest.class);
        verify(service).populate(captor.capture(), any());
        assertThat(items.get(0).getResult(),
                equalTo(UnitedOfferBuilder.offerBuilder(1, 10, "100")
                        .withName("test")
                        .withBarcode("111222333")
                        .build())); // ответ содержит все исходные поля + обогащение
        assertThat(captor.getValue().getItem(0).getStored(), // в запросах так же офер целиком
                equalTo(UnitedOfferBuilder.offerBuilder(1, 10, "100").withBarcode("111222333").build()));
        assertThat(captor.getValue().getItem(0).getResult(),
                equalTo(UnitedOfferBuilder.offerBuilder(1, 10, "100").withBarcode("111222333").build()));
    }

    @Test
    void twoItems() {
        var offer1 = UnitedOfferBuilder.offerBuilder(1, 10, "100");
        var offer2 = UnitedOfferBuilder.offerBuilder(1, 10, "200");
        var responseResult1 = UnitedOfferBuilder.offerBuilder(1, 10, "100").withName("test1");
        var responseResult2 = UnitedOfferBuilder.offerBuilder(1, 10, "200").withName("test2");

        doAnswer(delegatesTo(mockService(req ->
                BusinessMigration.PopulateOffersResponse.newBuilder()
                        .setSuccess(true)
                        .addItem(BusinessMigration.PopulateOffersResponseItem.newBuilder()
                                .setResult(responseResult2.build())) // специально в другом порядке
                        .addItem(BusinessMigration.PopulateOffersResponseItem.newBuilder()
                                .setResult(responseResult1.build()))
                        .build()
        ))).when(service).populate(any(), any());
        var items = List.of(
                BusinessMigration.MergeOffersRequestItem.newBuilder()
                        .setSource(offer1.build())
                        .setResult(offer1.build()),
                BusinessMigration.MergeOffersRequestItem.newBuilder()
                        .setSource(offer2.build())
                        .setResult(offer2.build())
        );
        step.accept(items);

        verify(service).populate(any(), any());
        assertThat(items.get(0).getResult(),
                equalTo(responseResult1.build()));
        assertThat(items.get(1).getResult(),
                equalTo(responseResult2.build()));
    }

    @Test
    void absentOffersInResponse() {
        var offer1 = UnitedOfferBuilder.offerBuilder(1, 10, "100");
        var offer2 = UnitedOfferBuilder.offerBuilder(1, 10, "200");
        var responseResult1 = UnitedOfferBuilder.offerBuilder(1, 10, "100").withName("test1");

        doAnswer(delegatesTo(mockService(req ->
                BusinessMigration.PopulateOffersResponse.newBuilder()
                        .setSuccess(true)
                        .addItem(BusinessMigration.PopulateOffersResponseItem.newBuilder()
                                .setResult(responseResult1.build()))
                        .build()
        ))).when(service).populate(any(), any());
        var items = List.of(
                BusinessMigration.MergeOffersRequestItem.newBuilder()
                        .setSource(offer1.build())
                        .setResult(offer1.build()),
                BusinessMigration.MergeOffersRequestItem.newBuilder()
                        .setSource(offer2.build())
                        .setResult(offer2.build())
        );
        step.accept(items);

        verify(service).populate(any(), any());
        assertThat(items.get(0).getResult(),
                equalTo(responseResult1.build()));
        assertFalse(//второго айтема не было в ответе, значит name не заполнился
                items.get(1).getResult().getBasic().getContent().getPartner().getOriginal().hasName(),
                "item[1].hasName");
    }

    @Test
    void testOnlyIds() {
        // пишем только id в запросах
        step = new ExternalSystemPopulateStep(migrator, new CopyOffersParams(),
                MigrationConfig.newBuilder().setPopulateOnlyIds(true).build());

        var items = prepareAndRunSingleOffer();

        var captor = ArgumentCaptor.forClass(BusinessMigration.PopulateOffersRequest.class);
        verify(service).populate(captor.capture(), any());
        assertThat(items.get(0).getResult(),
                equalTo(UnitedOfferBuilder.offerBuilder(1, 10, "100")
                        .withName("test")
                        .withBarcode("111222333").build())); // в ответе все поля, что были + обогащение
        assertThat(captor.getValue().getItem(0).getStored(), // а в запросах НЕ было полей, кроме id
                equalTo(UnitedOfferBuilder.offerBuilder(1, 10, "100").build()));
        assertThat(captor.getValue().getItem(0).getResult(),
                equalTo(UnitedOfferBuilder.offerBuilder(1, 10, "100").build()));
    }

    List<BusinessMigration.MergeOffersRequestItem.Builder> prepareAndRunSingleOffer() {
        var offer = UnitedOfferBuilder.offerBuilder(1, 10, "100").withBarcode("111222333");
        var requestItem = BusinessMigration.MergeOffersRequestItem.newBuilder()
                .setSource(offer.build())
                .setResult(offer.build());
        var responseResult = UnitedOfferBuilder.offerBuilder(1, 10, "100").withName("test");

        doAnswer(delegatesTo(mockService(req ->
                BusinessMigration.PopulateOffersResponse.newBuilder()
                        .setSuccess(true)
                        .addItem(BusinessMigration.PopulateOffersResponseItem.newBuilder()
                                .setResult(responseResult.build())
                        ).build()
        ))).when(service).populate(any(), any());
        var items = List.of(requestItem);
        step.accept(items);
        return items;
    }

    BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase mockService(
            Function<BusinessMigration.PopulateOffersRequest, BusinessMigration.PopulateOffersResponse> fun) {
        return new BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase() {
            @Override
            public void populate(BusinessMigration.PopulateOffersRequest request,
                                 StreamObserver<BusinessMigration.PopulateOffersResponse> responseObserver) {
                var resp = fun.apply(request);
                responseObserver.onNext(resp);
                responseObserver.onCompleted();
            }
        };
    }

}
