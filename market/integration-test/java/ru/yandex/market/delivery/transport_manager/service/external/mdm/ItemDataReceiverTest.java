package ru.yandex.market.delivery.transport_manager.service.external.mdm;

import java.util.Map;
import java.util.Set;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.service.external.mdm.dto.ItemRequest;
import ru.yandex.market.delivery.transport_manager.util.YaGrpcCleanupExtension;
import ru.yandex.market.mdm.http.tm.MdmTmIntegrationServiceGrpc;
import ru.yandex.market.mdm.http.tm.TmEnrichmentData;
import ru.yandex.market.mdm.http.tm.TmEnrichmentResponse;
import ru.yandex.market.mdm.http.tm.TmShopSkuKey;
import ru.yandex.market.mdm.http.tm.TmWeightDimensionsInfo;

public class ItemDataReceiverTest extends AbstractContextualTest {


    public YaGrpcCleanupExtension grpcCleanup = new YaGrpcCleanupExtension();

    private static final Set<ItemRequest> ITEM_REQUEST_3P = Set.of(new ItemRequest(3, "3000"));
    private static final Set<ItemRequest> ITEM_REQUEST_1P = Set.of(new ItemRequest(1, "1000"));

    private final MdmTmIntegrationServiceGrpc.MdmTmIntegrationServiceBlockingStub mdmTmIntegration;
    private final ItemDataReceiver itemDataReceiver;


    public ItemDataReceiverTest() throws Exception {
        var base = implementMockServer();
        var serverName = InProcessServerBuilder.generateName();
        grpcCleanup.register(
            InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(base)
                .build()
                .start()
        );

        ManagedChannel channel = grpcCleanup.register(
            InProcessChannelBuilder.forName(serverName).directExecutor().build());

        this.mdmTmIntegration = MdmTmIntegrationServiceGrpc.newBlockingStub(channel);
        itemDataReceiver = new ItemDataReceiver(
            mdmTmIntegration,
            1
        );
    }

    @Test
    @DisplayName("3p поставщик")
    void sskuRequestConverted3p() {
        var response = itemDataReceiver.searchSskuMasterData(ITEM_REQUEST_3P);
        Assertions.assertThat(response).isEqualTo(Map.of(
            new ItemRequest(3, "3000"),
            TmEnrichmentData.newBuilder()
                .setShopSku("3000")
                .setSupplierId(3)
                .setWeightDimensionsInfo(TmWeightDimensionsInfo.newBuilder().setBoxHeightUm(3000).build())
                .build()
        ));
    }

    @Test
    @DisplayName("1p поставщик, проставляем флаг externalKey")
    void sskuRequestConverted1p() {
        var response = itemDataReceiver.searchSskuMasterData(ITEM_REQUEST_1P);
        Assertions.assertThat(response).isEqualTo(Map.of(
            new ItemRequest(1, "1000"),
            TmEnrichmentData.newBuilder()
                .setShopSku("1000")
                .setSupplierId(1)
                .setWeightDimensionsInfo(TmWeightDimensionsInfo.newBuilder().setBoxHeightUm(1000).build())
                .build()
        ));
    }

    @Test
    void requestsFailed() {
        Assertions.assertThat(itemDataReceiver.searchSskuMasterData(
            Set.of(new ItemRequest(2, "10"))
        )).isEmpty();
    }

    private MdmTmIntegrationServiceGrpc.MdmTmIntegrationServiceImplBase implementMockServer() {
        return new MdmTmIntegrationServiceGrpc.MdmTmIntegrationServiceImplBase() {
            public void search(
                ru.yandex.market.mdm.http.tm.TmEnrichmentRequest request,
                io.grpc.stub.StreamObserver<TmEnrichmentResponse> responseObserver
            ) {
                TmShopSkuKey shopSku = request.getShopSku(0);
                TmEnrichmentData.Builder builder = TmEnrichmentData.newBuilder()
                    .setSupplierId(shopSku.getSupplierId())
                    .setShopSku(shopSku.getShopSku());
                if (!request.getExternalKeyFormat() && shopSku.getSupplierId() == 3) {
                    // мок для запроса 3p поставщика, проверяем совпадение поставщика и флага внешнего ключа
                    responseObserver.onNext(TmEnrichmentResponse.newBuilder()
                        .addEnrichmentData(
                            builder.setWeightDimensionsInfo(
                                TmWeightDimensionsInfo.newBuilder().setBoxHeightUm(3000).build()
                            ).build()).build());
                } else if (request.getExternalKeyFormat() && shopSku.getSupplierId() == 1) {
                    // мок для запроса 1p поставщика, проверяем совпадение поставщика и флага внешнего ключа
                    responseObserver.onNext(TmEnrichmentResponse.newBuilder()
                        .addEnrichmentData(
                            builder.setWeightDimensionsInfo(
                                TmWeightDimensionsInfo.newBuilder().setBoxHeightUm(1000).build()
                            ).build()).build());
                } else {
                    responseObserver.onError(new IllegalArgumentException("Wrong supplier"));
                }
                responseObserver.onCompleted();
            }
        };
    }

}
