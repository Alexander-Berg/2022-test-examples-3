package ru.yandex.market.psku.postprocessor.bazinga.deduplication;

import java.io.IOException;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.market.markup3.api.Markup3Api;
import ru.yandex.market.markup3.api.Markup3ApiTaskServiceGrpc;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterContent;
import ru.yandex.utils.Pair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class TolokaSenderTest {

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Test
    public void addTaskToSend() throws IOException {
        Markup3Api.CreateTasksResponse responseItems = Markup3Api.CreateTasksResponse.newBuilder()
                .addResponseItems(Markup3Api.CreateTaskResponseItem.newBuilder()
                        .setResult(Markup3Api.CreateTaskResponseItem.CreateTaskResult.OK)
                        .build())
                .build();

        Markup3ApiTaskServiceGrpc.Markup3ApiTaskServiceImplBase serviceImpl =
                new Markup3ApiTaskServiceGrpc.Markup3ApiTaskServiceImplBase() {
                    public void createTask(Markup3Api.CreateTasksRequest request,
                                           io.grpc.stub.StreamObserver<Markup3Api.CreateTasksResponse> responseObserver) {
                        responseObserver.onNext(responseItems);
                        responseObserver.onCompleted();
                    }
                };
        String serverName = InProcessServerBuilder.generateName();
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(serviceImpl).build().start());
        ManagedChannel channel = grpcCleanup.register(
                InProcessChannelBuilder.forName(serverName).directExecutor().build());
        Markup3ApiTaskServiceGrpc.Markup3ApiTaskServiceBlockingStub api =
                Markup3ApiTaskServiceGrpc.newBlockingStub(channel);

        TolokaSender tolokaSender = new TolokaSender(api, 3);

        Pair<ClusterContent, SupplierOffer.Offer> el1 = createTaskElement(1L, 10);
        Pair<ClusterContent, SupplierOffer.Offer> el2 = createTaskElement(2L, 20);
        Pair<ClusterContent, SupplierOffer.Offer> el3 = createTaskElement(3L, 30);

        TolokaSender.TolokaTask task1 = tolokaSender.addTaskToSend(el1);
        assertThat(task1).isNull();
        TolokaSender.TolokaTask task2 = tolokaSender.addTaskToSend(el2);
        assertThat(task2).isNull();
        TolokaSender.TolokaTask task3 = tolokaSender.addTaskToSend(el3);
        assertThat(task3).isNotNull();
        assertThat(task3).extracting(TolokaSender.TolokaTask::getTaskId).isEqualTo(1L);
        assertThat(task3.getTaskContent()).hasSize(3);
    }

    @NotNull
    private Pair<ClusterContent, SupplierOffer.Offer> createTaskElement(long targetSkuId, int offerid) {
        ClusterContent clusterContent = new ClusterContent();
        clusterContent.setId(targetSkuId);
        clusterContent.setTargetSkuId(targetSkuId);
        SupplierOffer.Offer offer = SupplierOffer.Offer.newBuilder()
                .setInternalOfferId(offerid)
                .build();
        return Pair.makePair(clusterContent, offer);
    }
}