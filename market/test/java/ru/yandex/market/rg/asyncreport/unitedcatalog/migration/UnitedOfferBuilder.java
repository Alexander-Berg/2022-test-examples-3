package ru.yandex.market.rg.asyncreport.unitedcatalog.migration;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampExplanation;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferPrice;
import Market.DataCamp.DataCampOfferPromos;
import Market.DataCamp.DataCampOfferStatus;
import Market.DataCamp.DataCampOfferStockInfo;
import Market.DataCamp.DataCampPromo;
import Market.DataCamp.DataCampResolution;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.DataCampValidationResult;
import Market.UltraControllerServiceData.UltraController.EnrichedOffer.EnrichType;
import com.google.protobuf.Timestamp;

import ru.yandex.market.core.tanker.model.TankerKeySets;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.rg.asyncreport.unitedcatalog.migration.conflict.ConflictUtils;

import static ru.yandex.market.rg.asyncreport.unitedcatalog.migration.conflict.ConflictUtils.param;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class UnitedOfferBuilder {

    static Clock clock = Clock.systemDefaultZone();
    private final DataCampUnitedOffer.UnitedOffer.Builder offer;

    private UnitedOfferBuilder(DataCampUnitedOffer.UnitedOffer.Builder offer) {
        this.offer = offer;
    }

    public static UnitedOfferBuilder offerBuilder(DataCampUnitedOffer.UnitedOffer offer) {
        return new UnitedOfferBuilder(offer.toBuilder());
    }

    public static UnitedOfferBuilder offerBuilder() {
        return new UnitedOfferBuilder(DataCampUnitedOffer.UnitedOffer.newBuilder());
    }

    public static UnitedOfferBuilder offerBuilder(Integer businessId, Integer serviceId, String offerId,
                                                  Integer warehouseId, boolean isUnitedCatalog, boolean emptyBasicOffer,
                                                  DataCampOfferMeta.OfferMeta offerMeta) {
        var offer = DataCampUnitedOffer.UnitedOffer.newBuilder();
        if (!emptyBasicOffer) {
            offer.setBasic(DataCampOffer.Offer.newBuilder()
                    .setIdentifiers(
                            DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                    .setBusinessId(businessId)
                                    .setOfferId(offerId)
                    )
            );
        }
        if (serviceId != null) {
            var offerIdentifiersBuilder = DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setShopId(serviceId);
            if (offerId != null) {
                offerIdentifiersBuilder.setOfferId(offerId);
            }
            if (businessId != null) {
                offerIdentifiersBuilder.setBusinessId(businessId);
            }
            if (warehouseId != null) {
                offerIdentifiersBuilder.setWarehouseId(warehouseId);
            }
            var serviceOfferBuilder = DataCampOffer.Offer.newBuilder()
                    .setIdentifiers(offerIdentifiersBuilder);
            if (offerMeta != null) {
                serviceOfferBuilder.setMeta(offerMeta);
            }
            if (isUnitedCatalog) {
                serviceOfferBuilder.setStatus(
                        DataCampOfferStatus.OfferStatus.newBuilder()
                                .setUnitedCatalog(
                                        DataCampOfferMeta.Flag.newBuilder()
                                                .setFlag(isUnitedCatalog).build()
                                ).build()
                );
            }
            offer.putService(serviceId, serviceOfferBuilder.build())
                    .build();
        }
        return new UnitedOfferBuilder(offer);
    }

    public static UnitedOfferBuilder offerBuilder(int businessId, Integer serviceId, String offerId,
                                                  boolean isUnitedCatalog) {
       return offerBuilder(businessId, serviceId, offerId, null, isUnitedCatalog, false, null);
    }

    public static UnitedOfferBuilder offerBuilder(int businessId, Integer serviceId, String offerId) {
        return offerBuilder(businessId, serviceId, offerId, false);
    }

    public UnitedOfferBuilder withMapping(long skuMapping) {
        offer.getBasicBuilder()
                .getContentBuilder()
                .getBindingBuilder()
                .getApprovedBuilder()
                .setMarketSkuId(skuMapping);
        return this;
    }

    public UnitedOfferBuilder withUcMapping(long skuMapping) {
        offer.getBasicBuilder()
                .getContentBuilder()
                .getBindingBuilder()
                .getUcMappingBuilder()
                .setMarketSkuId(skuMapping);
        return this;
    }

    public UnitedOfferBuilder withSelectiveMeta() {
        offer.getBasicBuilder()
                .setMeta(DataCampOfferMeta.OfferMeta.newBuilder()
                        .setScope(DataCampOfferMeta.OfferScope.SELECTIVE)
                        .build());
        return this;
    }

    public UnitedOfferBuilder withUcatFlag() {
        offer.getBasicBuilder()
                .setStatus(DataCampOfferStatus.OfferStatus.newBuilder()
                        .setUnitedCatalog(DataCampOfferMeta.Flag.newBuilder()
                                .setFlag(true)
                                .setMeta(meta())
                                .build())
                        .build());
        return this;
    }

    public UnitedOfferBuilder withActualCategory(int categoryId) {
        int businessId = offer.getBasicOrBuilder().getIdentifiersOrBuilder().getBusinessId();
        offer.getBasicBuilder()
                .getContentBuilder()
                .getPartnerBuilder()
                .getActualBuilder()
                .getCategoryBuilder()
                .setBusinessId(businessId)
                .setId(categoryId);
        return this;
    }

    public UnitedOfferBuilder withActualService(int partnerId,
                                                int warehouseId,
                                                boolean unitedCatalog,
                                                DataCampOfferMeta.OfferMeta offerMeta) {
        var warehouseOffer = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(offer.getBasic().getIdentifiers())
                .setStatus(
                        DataCampOfferStatus.OfferStatus.newBuilder()
                                .setUnitedCatalog(
                                        DataCampOfferMeta.Flag.newBuilder()
                                                .setFlag(unitedCatalog)
                                                .build()
                                ).build())
                .setStockInfo(DataCampOfferStockInfo.OfferStockInfo.newBuilder()
                        .setPartnerStocks(
                                DataCampOfferStockInfo.OfferStocks.newBuilder()
                                        .setCount(5)
                                        .setMeta(
                                                DataCampOfferMeta.UpdateMeta.newBuilder()
                                                        .setSource(DataCampOfferMeta.DataSource.MARKET_MBI_MIGRATOR)
                                                        .setTimestamp(Timestamp.newBuilder()
                                                                .setNanos(100)
                                                                .setSeconds(101)
                                                                .build())
                                                        .build()
                                        ).build()
                        ).build());
        if (offerMeta != null) {
            warehouseOffer.setMeta(offerMeta);
        }
        offer.putActual(partnerId, DataCampUnitedOffer.ActualOffers.newBuilder()
                .putWarehouse(warehouseId, warehouseOffer.build())
                .build());
        return this;
    }

    public UnitedOfferBuilder withOriginalCategory(int categoryId) {
        int businessId = offer.getBasicOrBuilder().getIdentifiersOrBuilder().getBusinessId();
        offer.getBasicBuilder()
                .getContentBuilder()
                .getPartnerBuilder()
                .getOriginalBuilder()
                .getCategoryBuilder()
                .setBusinessId(businessId)
                .setId(categoryId);
        return this;
    }

    public UnitedOfferBuilder withName(String name) {
        offer.getBasicBuilder()
                .getContentBuilder()
                .getPartnerBuilder()
                .getOriginalBuilder()
                .setName(DataCampOfferMeta.StringValue.newBuilder().setValue(name));
        return this;
    }

    public UnitedOfferBuilder withContentSystemStatus(DataCampContentStatus.ContentSystemStatus status) {
        offer.getBasicBuilder()
                .getContentBuilder()
                .getStatusBuilder()
                .setContentSystemStatus(status)
                .build();
        return this;
    }

    public UnitedOfferBuilder withBasicPrice(int partnerId) {
        offer.putService(partnerId, DataCampOffer.Offer.newBuilder()
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                        .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                .setVat(1)
                                .build())
                        .build())
                .build());
        return this;
    }

    public UnitedOfferBuilder withEnrichedOffer(long msku, EnrichType eType) {
        offer.getBasicBuilder().getContentBuilder().getMarketBuilder().setEnrichedOffer(
                Market.UltraControllerServiceData.UltraController.EnrichedOffer.newBuilder()
                        .setEnrichType(eType)
                        .setMarketSkuId(msku)
        );
        return this;
    }

    public UnitedOfferBuilder withBarcode(String... barcode) {
        offer.getBasicBuilder()
                .getContentBuilder()
                .getPartnerBuilder()
                .getOriginalBuilder()
                .getBarcodeBuilder()
                .addAllValue(Arrays.asList(barcode));
        return this;
    }

    public UnitedOfferBuilder withVendorCode(String vendor, String vendorCode) {
        offer.getBasicBuilder()
                .getContentBuilder()
                .getPartnerBuilder()
                .getOriginalBuilder()
                .getVendorBuilder()
                .setValue(vendor);
        offer.getBasicBuilder()
                .getContentBuilder()
                .getPartnerBuilder()
                .getOriginalBuilder()
                .getVendorCodeBuilder()
                .setValue(vendorCode);
        return this;
    }

    public UnitedOfferBuilder withDisabled(int serviceId, boolean disabled) {
        var serviceOffer = offer.getServiceMap().get(serviceId).toBuilder();
        serviceOffer.getStatusBuilder().addDisabled(DataCampOfferMeta.Flag.newBuilder()
                .setFlag(disabled)
                .setMeta(meta())
                .build());
        offer.putService(serviceId, serviceOffer.build()).build();
        return this;
    }

    public UnitedOfferBuilder withBasicDisabled(boolean disabled) {
        var offer = this.offer.getBasicBuilder();
        offer.getStatusBuilder().addDisabled(DataCampOfferMeta.Flag.newBuilder()
                .setFlag(disabled)
                .setMeta(meta())
                .build());
        return this;
    }

    public UnitedOfferBuilder withVerdict(int serviceId, String... params) {
        var service = offer.getServiceMap().get(serviceId).toBuilder();
        makeVerdict(service, params);
        offer.putService(serviceId, service.build());
        return this;
    }

    public UnitedOfferBuilder withBasicVerdict(String... params) {
        var offer = this.offer.getBasicBuilder();
        makeVerdict(offer, params);
        return this;
    }

    public UnitedOfferBuilder withMasterData(long height, long width, long length, long weight) {
        offer.getBasicBuilder().getContentBuilder().getMasterDataBuilder()
                .setDimensions(DataCampOfferContent.PreciseDimensions.newBuilder()
                        .setHeightMkm(height * 1000)
                        .setWidthMkm(width * 1000)
                        .setLengthMkm(length * 1000))
                .setWeightGross(DataCampOfferContent.PreciseWeight.newBuilder().setGrams(weight));
        return this;
    }

    public UnitedOfferBuilder withOriginalSpec(long height, long width, long length, long weight) {
        offer.getBasicBuilder().getContentBuilder().getPartnerBuilder()
                .getOriginalBuilder()
                .setDimensions(DataCampOfferContent.PreciseDimensions.newBuilder()
                        .setHeightMkm(height * 1000)
                        .setWidthMkm(width * 1000)
                        .setLengthMkm(length * 1000))
                .setWeight(DataCampOfferContent.PreciseWeight.newBuilder().setGrams(weight));
        return this;
    }

    public UnitedOfferBuilder withPromo(DataCampPromo.PromoType promoType, String promoId) {
        var updatedService = offer.getServiceMap().entrySet().stream().map(service -> {
           var serviceWithPromo = service.getValue().toBuilder()
                   .setPromos(generatePromo(promoType, promoId)).build();
           return Map.entry(service.getKey(), serviceWithPromo);
        });
        offer.clearService()
             .putAllService(updatedService.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        return this;
    }

    private DataCampOfferPromos.OfferPromos generatePromo(DataCampPromo.PromoType promoType, String promoId) {
        var promoBuilder = DataCampOfferPromos.OfferPromos.newBuilder();

        var promo = DataCampOfferPromos.Promo.newBuilder().setId(promoId);
        var promoList = DataCampOfferPromos.Promos.newBuilder();
        promoList.addPromos(promo);
        switch (promoType) {
            case MARKET_PROMOCODE:
                promoBuilder.setPartnerPromos(promoList);
                break;
            case PARTNER_STANDART_CASHBACK:
                promoBuilder.setPartnerCashbackPromos(promoList);
                break;
            default:
                throw new IllegalArgumentException("Not supported promo type");
        }
        return promoBuilder.build();
    }

    private void makeVerdict(DataCampOffer.Offer.Builder offer, String... params) {
        offer.getResolutionBuilder()
                .addBySource(DataCampResolution.Verdicts.newBuilder()
                        .setMeta(meta())
                        .addVerdict(DataCampResolution.Verdict.newBuilder()
                                .addResults(DataCampValidationResult.ValidationResult.newBuilder()
                                        .setIsBanned(true)
                                        .setIsValid(false)
                                        .addApplications(DataCampValidationResult.Feature.CPA)
                                        .addApplications(DataCampValidationResult.Feature.CPC)
                                        .addReasons(DataCampValidationResult.Reason.CONFLICT)
                                        .addMessages(DataCampExplanation.Explanation.newBuilder()
                                                .setLevel(DataCampExplanation.Explanation.Level.ERROR)
                                                .setNamespace(TankerKeySets.SHARED_HIDDEN_OFFERS_SUBREASONS_CODES)
                                                .setCode(ConflictUtils.MIGRATION_CONFLICT_CODE)
                                                .setText(ConflictUtils.COMMON_EXPLANATION)
                                                .addAllParams(mismatch(params))
                                        )
                                )
                        ));
    }

    private DataCampOfferMeta.UpdateMeta meta() {
        return DataCampOfferMeta.UpdateMeta.newBuilder()
                .setTimestamp(DateTimes.toTimestamp(clock.instant()))
                .setSource(DataCampOfferMeta.DataSource.MARKET_MBI_MIGRATOR)
                .build();
    }


    public static List<DataCampExplanation.Explanation.Param> mismatch(String... params) {
        List<DataCampExplanation.Explanation.Param> result = new ArrayList<>(params.length / 2);
        for (int i = 0; i < params.length - 1; i += 2) {
            String key = params[i];
            String value = params[i + 1];
            result.add(param(key, value).build());
        }
        return result;
    }

    public DataCampUnitedOffer.UnitedOffer build() {
        return offer.build();
    }
}
