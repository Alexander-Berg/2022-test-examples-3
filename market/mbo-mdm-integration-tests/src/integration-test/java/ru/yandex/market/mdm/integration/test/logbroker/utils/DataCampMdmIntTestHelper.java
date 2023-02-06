package ru.yandex.market.mdm.integration.test.logbroker.utils;

import java.util.List;

import Market.DataCamp.API.DatacampMessageOuterClass;
import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampUnitedOffer;
import com.google.protobuf.Timestamp;

import ru.yandex.market.mbo.mdm.common.infrastructure.MdmLogbrokerEvent;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;

@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class DataCampMdmIntTestHelper {

    public static MdmLogbrokerEvent<DatacampMessageOuterClass.DatacampMessage> message(
        DataCampUnitedOffer.UnitedOffer... messages) {
        return new MdmLogbrokerEvent<>(DatacampMessageOuterClass.DatacampMessage.newBuilder()
            .addAllUnitedOffers(
                List.of(DataCampUnitedOffer.UnitedOffersBatch.newBuilder()
                    .addAllOffer(List.of(messages))
                    .build()))
            .build());
    }

    public static DataCampUnitedOffer.UnitedOffer unitedOffer(int mappingCategoryId, int businessId, String shopSku,
                                                              long mskuId) {
        DataCampUnitedOffer.UnitedOffer unitedOffer = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(basic(mappingCategoryId, businessId, shopSku, mskuId))
            .build();
        return unitedOffer;
    }

    public static DataCampOffer.Offer basic(int mappingCategoryId, int businessId, String shopSku, long mskuId) {
        return DataCampOffer.Offer.newBuilder()
            .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setBusinessId(businessId)
                .setOfferId(shopSku)
                .build())
            .setContent(offerContent(mappingCategoryId, mskuId))
            .build();
    }

    public static DataCampOfferContent.OfferContent offerContent(int mappingCategoryId, long mskuId) {
        return DataCampOfferContent.OfferContent.newBuilder()
            .setBinding(mapping(mappingCategoryId, mskuId))
            .setStatus(status())
            .build();
    }

    public static DataCampOfferMapping.ContentBinding mapping(int mappingCategoryId, long mskuId) {
        return DataCampOfferMapping.ContentBinding.newBuilder()
            .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                .setMarketSkuId(mskuId)
                .setMarketCategoryId(mappingCategoryId)
                .setMeta(meta()))
            .build();
    }

    public static DataCampContentStatus.ContentStatus status() {
        return DataCampContentStatus.ContentStatus.newBuilder()
            .setContentSystemStatus(DataCampContentStatus.ContentSystemStatus.newBuilder()
                .setMeta(meta())
                .setSkuMappingConfidence(DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT))
            .build();
    }

    public static DataCampOfferMeta.UpdateMeta meta() {
        return DataCampOfferMeta.UpdateMeta.newBuilder()
            .setTimestamp(Timestamp.newBuilder()
                .setSeconds(100)
                .setNanos(90))
            .build();
    }

    public static MappingCacheDao mappingCacheDao(int supplierId, String shopSku, int categoryId, long mskuId) {
        return new MappingCacheDao()
            .setCategoryId(categoryId)
            .setMskuId(mskuId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setMskuId(mskuId);
    }
}
