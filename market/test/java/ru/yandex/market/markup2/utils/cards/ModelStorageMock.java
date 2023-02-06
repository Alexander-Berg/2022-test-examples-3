package ru.yandex.market.markup2.utils.cards;

import ru.yandex.market.markup2.utils.ParameterTestUtils;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorageServiceStub;

import java.util.List;
import java.util.stream.Collectors;

public class ModelStorageMock extends ModelStorageServiceStub {
    private List<InStorageCard> guruCards;
    private List<InStorageCard> clusterCards;
    //To find offers for cluster
    private List<Offer> allOffers;
    private int requestCounter;

    public ModelStorageMock(List<InStorageCard> guruCards,
                            List<InStorageCard> clusterCards,
                            List<Offer> allOffers) {
        this.guruCards = guruCards;
        this.clusterCards = clusterCards;
        this.allOffers = allOffers;
    }

    public int getRequestCounter() {
        return requestCounter;
    }

    public ModelStorage.GetModelsResponse getModels(ModelStorage.GetModelsRequest getModelsRequest) {
        return processRequest(getModelsRequest.getModelIdsList());
    }

    @Override
    public ModelStorage.GetModelsResponse findModels(ModelStorage.FindModelsRequest findModelsRequest) {
        return processRequest(findModelsRequest.getModelIdsList());
    }

    private ModelStorage.GetModelsResponse processRequest(List<Long> modelIds) {
        requestCounter++;
        ModelStorage.GetModelsResponse.Builder responseBuilder = ModelStorage.GetModelsResponse.newBuilder();

        guruCards.stream()
            .filter(card -> modelIds.contains(card.getId()))
            .forEach(card -> responseBuilder.addModels(buildModel(card, ModelStorage.ModelType.GURU)));

        clusterCards.stream()
            .filter(cluster -> modelIds.contains(cluster.getId()))
            .forEach(cluster -> {
                List<String> offerIds = allOffers.stream()
                    .filter(offer -> offer.getClusterId() == cluster.getId())
                    .map(Offer::getId)
                    .collect(Collectors.toList());

                ModelStorage.Model.Builder modelBuilder = buildModel(cluster, ModelStorage.ModelType.CLUSTER);
                modelBuilder.addAllClusterizerOfferIds(offerIds);

                responseBuilder.addModels(modelBuilder);
            });

        return responseBuilder.build();
    }

    private ModelStorage.Model.Builder buildModel(InStorageCard card, ModelStorage.ModelType modelType) {
        return buildModel(card, ModelStorage.Model.newBuilder(), modelType);
    }

    private ModelStorage.Model.Builder buildModel(InStorageCard card,
                                                  ModelStorage.Model.Builder modelBuilder,
                                                  ModelStorage.ModelType modelType) {
        modelBuilder.setId(card.getId());
        modelBuilder.addTitles(0, ModelStorage.LocalizedString.newBuilder().setValue(card.getTitle()));
        modelBuilder.addDescriptions(0, ParameterTestUtils.createLocalizedString(card.getDescription()));
        modelBuilder.addPictures(0, ModelStorage.Picture.newBuilder().setUrl(card.getImageUrl()));
        modelBuilder.setCurrentType(modelType.toString());
        modelBuilder.setPublished(card.isMboPublished());
        modelBuilder.setDeleted(false);

        return modelBuilder;
    }
}
