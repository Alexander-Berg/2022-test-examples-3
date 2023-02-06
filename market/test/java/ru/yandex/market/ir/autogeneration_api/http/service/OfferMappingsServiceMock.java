package ru.yandex.market.ir.autogeneration_api.http.service;

import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.mboc.http.OfferMappings;
import ru.yandex.market.mboc.http.OfferMappings.OfferMappingsUpdate;
import ru.yandex.market.mboc.http.OfferMappings.OfferMappingsUpdate.MappingType;
import ru.yandex.market.mboc.http.OfferMappings.UpdateOfferMappingsRequest;
import ru.yandex.market.mboc.http.OfferMappings.UpdateOfferMappingsResponse;
import ru.yandex.market.mboc.http.OfferMappingsService;

import java.util.HashMap;
import java.util.Map;

public class OfferMappingsServiceMock implements OfferMappingsService {
    private final Map<OfferMappings.OfferID, OfferMappingValue> byKeyIndex = new HashMap<>();


    @Override
    public MonitoringResult ping() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MonitoringResult monitoring() {
        throw new UnsupportedOperationException();
    }


    public void addMapping(int shopId, String shopSku, long categoryId, long modelId) {
        OfferMappings.OfferID offerID = OfferMappings.OfferID.newBuilder()
            .setShopId(shopId)
            .setShopSku(shopSku)
            .build();
        byKeyIndex.put(offerID, new OfferMappingValue(categoryId, modelId));
    }

    @Override
    public UpdateOfferMappingsResponse updateMappings(UpdateOfferMappingsRequest updateOfferMappingsRequest) {
        UpdateOfferMappingsResponse.Builder builder = UpdateOfferMappingsResponse.newBuilder();

        for (OfferMappingsUpdate offerMappingsUpdate : updateOfferMappingsRequest.getUpdatesList()) {
            long categoryId = offerMappingsUpdate.getCategoryUpdatesList().stream()
                .filter(categoryMappingUpdate -> categoryMappingUpdate.getType() == MappingType.APPROVED)
                .mapToLong(OfferMappings.CategoryMappingUpdate::getCategoryId)
                .findFirst()
                .getAsLong();

            long modelId = offerMappingsUpdate.getModelUpdatesList().stream()
                .filter(categoryMappingUpdate -> categoryMappingUpdate.getType() == MappingType.APPROVED)
                .mapToLong(OfferMappings.ModelMappingUpdate::getModelId)
                .findFirst()
                .getAsLong();

            byKeyIndex.put(offerMappingsUpdate.getId(), new OfferMappingValue(categoryId, modelId));

            builder.addResults(OfferMappings.MappingsUpdateResult.newBuilder()
                .setId(offerMappingsUpdate.getId())
                .setStatus(OfferMappings.MappingsUpdateResult.Status.OK)
            );
        }

        return builder.build();
    }

    @Override
    public OfferMappings.GetMappingsResponse getMappings(
        OfferMappings.GetMappingsRequest getMappingsRequest
    ) {
        OfferMappings.GetMappingsResponse.Builder builder = OfferMappings.GetMappingsResponse.newBuilder();
        for (OfferMappings.OfferID offerID : getMappingsRequest.getIdsList()) {
            OfferMappingValue offerMappingValue = byKeyIndex.get(offerID);
            if (offerMappingValue != null) {
                builder.addMappings(OfferMappings.Mappings.newBuilder()
                    .setId(offerID)
                    .setApprovedCategory(OfferMappings.CategoryMapping.newBuilder()
                        .setMappedId(offerMappingValue.categoryId)
                    )
                    .setApprovedModel(OfferMappings.ModelMapping.newBuilder()
                        .setMappedId(offerMappingValue.modelId)
                    )
                );
            }
        }
        return builder.build();
    }

    private final class OfferMappingValue {
        private final long categoryId;
        private final long modelId;

        private OfferMappingValue(long categoryId, long modelId) {
            this.categoryId = categoryId;
            this.modelId = modelId;
        }
    }

}
