package ru.yandex.market.markup2.utils.cards;

import ru.yandex.market.markup2.utils.ParameterTestUtils;
import ru.yandex.market.mbo.export.CategoryModelsServiceStub;
import ru.yandex.market.mbo.export.MboExport;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.List;
import java.util.stream.Collectors;

public class CategoryModelsMock extends CategoryModelsServiceStub {
    private List<InStorageCard> guruCards;
    private List<InStorageCard> clusterCards;
    private List<Offer> allOffers;
    private int requestCounter;

    public CategoryModelsMock(List<InStorageCard> guruCards, List<InStorageCard> clusterCards, List<Offer> allOffers) {
        this.guruCards = guruCards;
        this.clusterCards = clusterCards;
        this.allOffers = allOffers;
    }

    @Override
    public MboExport.GetCategoryModelsResponse getModels(MboExport.GetCategoryModelsRequest request) {
        requestCounter++;
        MboExport.GetCategoryModelsResponse.Builder responseBuilder = MboExport.GetCategoryModelsResponse.newBuilder();

        List<Long> modelIds = request.getModelIdList();
        guruCards.stream()
            .filter(card -> modelIds.contains(card.getId()))
            .forEach(card -> responseBuilder.addModels(buildModel(card, ModelStorage.Model.newBuilder(),
                ModelStorage.ModelType.GURU)));

        clusterCards.stream()
            .filter(cluster -> modelIds.contains(cluster.getId()))
            .forEach(cluster -> {
                ModelStorage.Model.Builder modelBuilder = buildModel(cluster, ModelStorage.Model.newBuilder(),
                    ModelStorage.ModelType.CLUSTER);
                List<String> offerIds = allOffers.stream()
                    .filter(offer -> offer.getClusterId() == cluster.getId())
                    .map(Offer::getId)
                    .collect(Collectors.toList());
                modelBuilder.addAllClusterizerOfferIds(offerIds);

                responseBuilder.addModels(modelBuilder);
            });

        return responseBuilder.build();
    }

    private ModelStorage.Model.Builder buildModel(InStorageCard card,
                                                  ModelStorage.Model.Builder modelBuilder,
                                                  ModelStorage.ModelType modelType) {
        modelBuilder.setId(card.getId());
        modelBuilder.addDescriptions(0, ParameterTestUtils.createLocalizedString(card.getDescription()));
        modelBuilder.addTitles(0, ModelStorage.LocalizedString.newBuilder().setValue(card.getTitle()));
        modelBuilder.addPictures(0, ModelStorage.Picture.newBuilder().setUrl(card.getImageUrl()));
        modelBuilder.setCurrentType(modelType.toString());
        modelBuilder.setPublished(card.isMboPublished());

        return modelBuilder;
    }

    public int getRequestCounter() {
        return requestCounter;
    }
}
