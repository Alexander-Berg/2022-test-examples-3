package ru.yandex.market.mboc.common.datacamp;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampContentMarketParameterValue;
import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMarketContent;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferPictures;
import Market.UltraControllerServiceData.UltraController;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.services.datacamp.OfferGenerationHelper;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static ru.yandex.market.mboc.common.utils.OfferTestUtils.TEST_CATEGORY_INFO_ID;

@NoArgsConstructor
@AllArgsConstructor
public class OfferBuilder {
    DataCampOffer.Offer.Builder offerBuilder = DataCampOffer.Offer.newBuilder();

    public OfferBuilder withIdentifiers(BusinessSkuKey key) {
        return withIdentifiers(key.getBusinessId(), key.getShopSku());
    }

    public OfferBuilder withIdentifiers(int businessId, String offerId) {
        offerBuilder.getIdentifiersBuilder()
            .setBusinessId(businessId)
            .setOfferId(offerId);
        return this;
    }

    public OfferBuilder withIdentifiers(int businessId, int shopId, String offerId) {
        offerBuilder.getIdentifiersBuilder()
            .setBusinessId(businessId)
            .setShopId(shopId)
            .setOfferId(offerId);
        return this;
    }

    public OfferBuilder withUcMapping(DataCampOfferMapping.Mapping.Builder mapping) {
        offerBuilder.getContentBuilder().getBindingBuilder().setUcMapping(mapping);
        return this;
    }

    public static DataCampOfferMapping.AntiMapping.Builder defaultAntiMapping() {
        return DataCampOfferMapping.AntiMapping.newBuilder();
    }


    public OfferBuilder withPartnerMapping(DataCampOfferMapping.Mapping.Builder mapping) {
        offerBuilder.getContentBuilder().getBindingBuilder().setPartner(mapping);
        return this;
    }

    public OfferBuilder withPartnerTitle(String title) {
        offerBuilder.getContentBuilder().getPartnerBuilder().getActualBuilder().getTitleBuilder().setValue(title);
        return this;
    }

    public OfferBuilder withApprovedMapping(DataCampOfferMapping.Mapping.Builder mapping) {
        offerBuilder.getContentBuilder().getBindingBuilder().setApproved(mapping);
        return this;
    }

    public static DataCampOfferMapping.Mapping.Builder defaultSkuMapping() {
        return mapping(TEST_CATEGORY_INFO_ID, OfferTestUtils.TEST_MODEL_ID, OfferTestUtils.TEST_SKU_ID,
            OfferTestUtils.DEFAULT_CATEGORY_NAME, OfferTestUtils.DEFAULT_MODEL_NAME, OfferTestUtils.DEFAULT_SKU_NAME);
    }

    public static DataCampOfferMapping.Mapping.Builder defaultModelMapping() {
        return mapping(TEST_CATEGORY_INFO_ID, OfferTestUtils.TEST_MODEL_ID, null,
            OfferTestUtils.DEFAULT_CATEGORY_NAME, OfferTestUtils.DEFAULT_MODEL_NAME, null);
    }

    public static DataCampOfferMapping.Mapping.Builder defaultCategoryMapping() {
        return mapping(TEST_CATEGORY_INFO_ID, null, null,
            OfferTestUtils.DEFAULT_CATEGORY_NAME, null, null);
    }

    public OfferBuilder withAntiMapping(DataCampOfferMapping.AntiMapping.Builder mapping) {
        offerBuilder.getContentBuilder().getBindingBuilder().setAntiMappingForUc(mapping);
        return this;
    }

    public static DataCampOfferMapping.Mapping.Builder categoryMapping(Long categoryId) {
        return categoryMapping(categoryId, null);
    }

    public static DataCampOfferMapping.Mapping.Builder modelMapping(Long modelId) {
        return modelMapping(null, modelId, null, null);
    }

    public static DataCampOfferMapping.Mapping.Builder skuMapping(Long skuId) {
        return mapping(null, null, skuId, null, null, null);
    }

    public static DataCampOfferMapping.Mapping.Builder categoryMapping(Long categoryId, String categoryName) {
        return modelMapping(categoryId, null, categoryName, null);
    }

    public static DataCampOfferMapping.Mapping.Builder modelMapping(Long categoryId, Long modelId,
                                                                    String categoryName, String modelName) {
        return mapping(categoryId, modelId, null, categoryName, modelName, null);
    }

    public static DataCampOfferMapping.Mapping.Builder mapping(Long categoryId, Long modelId, Long skuId,
                                                               String categoryName, String modelName, String skuName) {
        DataCampOfferMapping.Mapping.Builder mapping = DataCampOfferMapping.Mapping.newBuilder();
        if (categoryId != null) {
            mapping.setMarketCategoryId(categoryId.intValue());
            if (categoryName != null) {
                mapping.setMarketCategoryName(categoryName);
            }
        }
        if (modelId != null) {
            mapping.setMarketModelId(modelId);
            if (modelName != null) {
                mapping.setMarketModelName(modelName);
            }
        }
        if (skuId != null) {
            mapping.setMarketSkuId(skuId);
            if (skuName != null) {
                mapping.setMarketSkuName(skuName);
            }
        }
        return mapping;
    }

    public static DataCampOfferPictures.OfferPictures pictures(String... urls) {
        return pictures(
            Arrays.stream(urls)
                .map(url -> Pair.of(url, DataCampOfferPictures.MarketPicture.Status.AVAILABLE))
                .collect(Collectors.toList())
        );
    }

    public static DataCampOfferPictures.OfferPictures pictures(
        List<Pair<String, DataCampOfferPictures.MarketPicture.Status>> pictures) {
        DataCampOfferPictures.PartnerPictures.Builder builder = DataCampOfferPictures.PartnerPictures.newBuilder();
        long i = 0;
        for (var pic : pictures) {
            var key = "sourcePic" + i;
            builder.putActual(key, OfferGenerationHelper.createPicture(pic.first, pic.second));
            builder.getOriginalBuilder().addSource(DataCampOfferPictures.SourcePicture.newBuilder()
                .setUrl(key));
            i++;
        }
        return DataCampOfferPictures.OfferPictures.newBuilder()
            .setPartner(builder)
            .build();
    }


    public OfferBuilder withDefaultProcessedSpecification() {
        DataCampOfferContent.ProcessedSpecification.Builder processed =
            offerBuilder.getContentBuilder().getPartnerBuilder().getActualBuilder();
        processed.getTitleBuilder().setValue(OfferTestUtils.DEFAULT_TITLE);
        processed.getDescriptionBuilder().setValue(OfferTestUtils.DEFAULT_DESCRIPTION);
        processed.getCategoryBuilder().setName(OfferTestUtils.DEFAULT_CATEGORY);
        processed.getUrlBuilder().setValue(OfferTestUtils.DEFAULT_URL);
        processed.getBarcodeBuilder().addValue(OfferTestUtils.DEFAULT_BARCODE);
        processed.getVendorCodeBuilder().setValue(OfferTestUtils.DEFAULT_VENDORCODE);
        processed.setOfferParams(DataCampOfferContent.ProductYmlParams.newBuilder()
            .addParam(DataCampOfferContent.OfferYmlParam.newBuilder()
                .setName("PARAM")
                .setValue("21.1")
                .build())
            .addParam(DataCampOfferContent.OfferYmlParam.newBuilder()
                .setName("OTHER_PARAM")
                .setValue("hello there"))
            .build());
        return this;
    }

    public OfferBuilder withDefaultProcessedSpecification(
        Consumer<DataCampOfferContent.ProcessedSpecification.Builder> customizer
    ) {
        withDefaultProcessedSpecification();
        customizer.accept(offerBuilder.getContentBuilder().getPartnerBuilder().getActualBuilder());
        return this;
    }

    public OfferBuilder withDefaultMarketContent(Consumer<DataCampOfferContent.MarketContent.Builder> customizer) {
        withDefaultMarketContent();
        customizer.accept(offerBuilder.getContentBuilder().getMarketBuilder());
        return this;
    }

    public OfferBuilder withDefaultMarketContent() {
        DataCampOfferContent.MarketContent.Builder market =
            offerBuilder.getContentBuilder().getMarketBuilder();
        market.setVendorId(OfferTestUtils.TEST_VENDOR_ID);
        market.setVendorName(OfferTestUtils.DEFAULT_VENDOR_NAME);
        market.getIrDataBuilder()
            .setClassifierCategoryId((int) TEST_CATEGORY_INFO_ID)
            .setClassifierConfidentTopPercision(0.5)
            .setEnrichType(UltraController.EnrichedOffer.EnrichType.ET_MAIN);
        return this;
    }

    public OfferBuilder withDefaultMarketSpecificContent() {
        offerBuilder.getContentBuilder().getPartnerBuilder().getMarketSpecificContentBuilder()
            .getParameterValuesBuilder().addParameterValues(
                DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                    .setParamId(OfferTestUtils.PARAM_ID)
                    .setValue(
                        DataCampContentMarketParameterValue.MarketValue.newBuilder()
                            .setStrValue("test")
                            .setValueType(DataCampContentMarketParameterValue.MarketValueType.STRING)
                            .build()
                    )
                    .build()
            );
        return this;
    }

    public OfferBuilder withPictures(String... url) {
        offerBuilder.setPictures(pictures(url));
        return this;
    }

    public OfferBuilder withPictures(List<Pair<String, DataCampOfferPictures.MarketPicture.Status>> pics) {
        offerBuilder.setPictures(pictures(pics));
        return this;
    }

    public OfferBuilder withDefaultPicture() {
        return withPictures(OfferTestUtils.DEFAULT_PIC_URL);
    }

    public OfferBuilder withGroup(Integer groupId, String name) {
        DataCampOfferContent.OriginalSpecification.Builder builder =
            offerBuilder.getContentBuilder().getPartnerBuilder().getOriginalBuilder();
        builder.getGroupIdBuilder().setValue(groupId);
        builder.getGroupNameBuilder().setValue(name);
        return this;
    }

    public OfferBuilder withNoGroup() {
        DataCampOfferContent.OriginalSpecification.Builder builder =
            offerBuilder.getContentBuilder().getPartnerBuilder().getOriginalBuilder();
        builder.clearGroupId();
        builder.clearGroupName();
        return this;
    }

    public OfferBuilder withProcessingResult(DataCampOfferMarketContent.MarketContentProcessing processingResult) {
        offerBuilder.getContentBuilder()
            .getPartnerBuilder()
            .getMarketSpecificContentBuilder()
            .setProcessingResponse(processingResult);
        return this;
    }

    public OfferBuilder withEmptyProcessingResult(DataCampOfferMeta.UpdateMeta meta) {
        offerBuilder.getContentBuilder()
            .getPartnerBuilder()
            .getMarketSpecificContentBuilder()
            .setProcessingResponse(
                DataCampOfferMarketContent.MarketContentProcessing.newBuilder()
                    .setMeta(meta)
                    .build()
            );
        return this;
    }

    public OfferBuilder withVersion(Long version) {
        offerBuilder.getStatusBuilder().getVersionBuilder().getActualContentVersionBuilder().setCounter(version);
        return this;
    }

    public OfferBuilder withDefaultVersion() {
        return withVersion(1L);
    }

    public OfferBuilder withServiceOfferState(DataCampContentStatus.OfferContentCpaState state) {
        offerBuilder.getContentBuilder().getStatusBuilder().getContentSystemStatusBuilder()
            .setMeta(DataCampOfferUtil.createUpdateMeta(Instant.now(), DataCampOfferMeta.DataSource.MARKET_MBO))
            .setServiceOfferState(state);
        return this;
    }

    public OfferBuilder withContentSystemStatus(DataCampContentStatus.ContentSystemStatus status) {
        offerBuilder.getContentBuilder().getStatusBuilder().setContentSystemStatus(status);
        return this;
    }

    public OfferBuilder withDefaultGroup() {
        return withGroup(OfferTestUtils.GROUP_ID, OfferTestUtils.DEFAULT_GROUP_NAME);
    }


    public OfferBuilder withConfidentParamList(List<UltraController.FormalizedParamPosition> formalizedParamPositions) {
        offerBuilder.getContentBuilder().getMarketBuilder().getEnrichedOfferBuilder()
            .addAllConfidentParamsForPsku(formalizedParamPositions);
        return this;
    }

    public OfferBuilder withDisabledFlag(boolean isDisabled) {
        offerBuilder.getStatusBuilder()
            .clearDisabled()
            .addDisabled(DataCampOfferMeta.Flag.newBuilder()
                .setMeta(DataCampOfferUtil.createUpdateMeta(Instant.now(), DataCampOfferMeta.DataSource.MARKET_MBO))
                .setFlag(isDisabled));
        return this;
    }

    public static OfferBuilder create() {
        return new OfferBuilder();
    }

    public static OfferBuilder create(DataCampOffer.Offer offer) {
        return new OfferBuilder(offer.toBuilder());
    }

    public DataCampOffer.Offer.Builder get() {
        return offerBuilder;
    }

    public DataCampOffer.Offer build() {
        return get().build();
    }

    public DataCampOffer.Offer.Builder getAndClean() {
        DataCampOffer.Offer.Builder toReturn = offerBuilder;
        offerBuilder = DataCampOffer.Offer.newBuilder();
        return toReturn;
    }

    public OfferBuilder defaultNoMatchOffer() {
        return withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.DEFAULT_SHOP_SKU)
            .withDefaultVersion()
            .withDefaultPicture()
            .withDefaultGroup()
            .withDefaultProcessedSpecification()
            .withDefaultMarketSpecificContent()
            .withUcMapping(defaultCategoryMapping());
    }

    public OfferBuilder defaultMatchedOffer() {
        return withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.DEFAULT_SHOP_SKU)
            .withDefaultVersion()
            .withDefaultPicture()
            .withDefaultGroup()
            .withDefaultProcessedSpecification()
            .withDefaultMarketSpecificContent()
            .withUcMapping(defaultModelMapping());
    }

    public OfferBuilder defaultSkutchedOffer() {
        return withIdentifiers(OfferTestUtils.BIZ_ID_SUPPLIER, OfferTestUtils.DEFAULT_SHOP_SKU)
            .withDefaultVersion()
            .withDefaultPicture()
            .withDefaultGroup()
            .withDefaultProcessedSpecification()
            .withDefaultMarketSpecificContent()
            .withUcMapping(defaultSkuMapping());
    }
}
