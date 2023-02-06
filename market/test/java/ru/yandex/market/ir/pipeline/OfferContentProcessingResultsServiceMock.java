package ru.yandex.market.ir.pipeline;

import Market.DataCamp.DataCampOfferContent;
import io.grpc.stub.StreamObserver;
import org.jetbrains.annotations.NotNull;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mboc.http.OfferContentProcessingResults;
import ru.yandex.market.mboc.http.OfferContentProcessingResultsServiceGrpc;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class OfferContentProcessingResultsServiceMock extends OfferContentProcessingResultsServiceGrpc.OfferContentProcessingResultsServiceImplBase {
    private final Map<Pair<Integer, String>, DataCampOfferContent.OfferContent> offers = new HashMap<>();

    /**
     * @return a map (businessId, shopSku) -> offerContent of all offers that have been saved in this service
     */
    public Map<Pair<Integer, String>, DataCampOfferContent.OfferContent> getOffers() {
        return offers;
    }

    @Override
    public void updateDataCampContentProcessingTasks(OfferContentProcessingResults.UpdateContentProcessingTasksRequest request, StreamObserver<OfferContentProcessingResults.UpdateContentProcessingTasksResponse> responseObserver) {
        for (OfferContentProcessingResults.UpdateContentProcessingTasksRequest.BusinessIdsProcessingTask processingTask : request.getContentProcessingTaskList()) {
            DataCampOfferContent.OfferContent content = processingTask.getContentProcessing().getContent();
            offers.put(Pair.of(processingTask.getBusinessId(), processingTask.getShopSku()), content);
        }

        OfferContentProcessingResults.UpdateContentProcessingTasksResponse response = createAllOkResponse(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @NotNull
    private OfferContentProcessingResults.UpdateContentProcessingTasksResponse createAllOkResponse(OfferContentProcessingResults.UpdateContentProcessingTasksRequest request) {

        OfferContentProcessingResults.UpdateContentProcessingTasksResponse.Builder responseBuilder = OfferContentProcessingResults.UpdateContentProcessingTasksResponse.newBuilder();

        responseBuilder.addAllStatusPerBusinessId(
                request.getContentProcessingTaskList().stream()
                        .map(task -> task.getShopSku())
                        .distinct()
                        .map(shopSku -> OfferContentProcessingResults.UpdateContentProcessingTasksResponse.OfferStatus.newBuilder()
                                .setShopSku(shopSku)
                                .setStatus(OfferContentProcessingResults.UpdateContentProcessingTasksResponse.OfferStatus.Status.OK)
                                .build()
                        )
                        .collect(Collectors.toList())
        );
        OfferContentProcessingResults.UpdateContentProcessingTasksResponse response = responseBuilder.build();
        return response;
    }
}
