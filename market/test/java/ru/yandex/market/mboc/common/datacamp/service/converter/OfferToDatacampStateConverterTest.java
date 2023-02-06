package ru.yandex.market.mboc.common.datacamp.service.converter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampExplanation;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMarketContent;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampResolution;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.datacamp.DataCampOfferUtil;
import ru.yandex.market.mboc.common.datacamp.OfferBuilder;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.honestmark.AllowedCategoryType;
import ru.yandex.market.mboc.common.honestmark.CategoryRestriction;
import ru.yandex.market.mboc.common.honestmark.EmptyCategoryRestriction;
import ru.yandex.market.mboc.common.honestmark.GroupCategoryRestriction;
import ru.yandex.market.mboc.common.honestmark.SingleCategoryRestriction;
import ru.yandex.market.mboc.common.offers.model.AntiMapping;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingService;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.DescribedCheck;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.DEFAULT_SHOP_SKU;

public class OfferToDatacampStateConverterTest {

    @Test
    public void testEnrichWithState() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create()
            .defaultNoMatchOffer().get().build();
        Offer currentOffer = OfferTestUtils.simpleOffer(OfferTestUtils.blueSupplierUnderBiz1())
            .setId(1)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .setCategoryConfidence(Offer.CategoryConfidence.CONTENT)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
            .setDataCampContentVersion(dcOffer.getStatus().getVersion().getActualContentVersion().getCounter());

        CategoryRestriction restriction = new SingleCategoryRestriction(OfferTestUtils.TEST_CATEGORY_INFO_ID);

        var antiMapping = new AntiMapping()
            .setOfferId(1L)
            .setNotModelId(2L)
            .setNotSkuId(3L)
            .setUpdatedTs(Instant.now());
        var antiMappingProto =
            DataCampOfferMapping.AntiMapping.newBuilder().addNotModelId(2L).addNotSkuId(3L);

        DataCampOffer.Offer result = OfferToDataCampStateConverter.enrichWithState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(OfferTestUtils.defaultCategory())
                .skuNamesMap(Collections.emptyMap())
                .skuCreatedTsMap(Collections.emptyMap())
                .antiMappings(List.of(antiMapping))
                .build(),
            currentOffer,
            dcOffer,
            restriction,
            createConverterConfig(),
            true
        );

        DataCampOfferMeta.UpdateMeta meta = result.getContent().getStatus().getContentSystemStatus().getMeta();
        DataCampOfferMeta.UpdateMeta antiMappingMeta =
            DataCampOfferUtil.createUpdateMeta(antiMapping.getUpdatedTs(), DataCampOfferMeta.DataSource.MARKET_MBO);

        assertThat(result)
            .isEqualTo(OfferBuilder.create()
                .defaultNoMatchOffer()
                .withEmptyProcessingResult(meta)
                .withApprovedMapping(OfferBuilder.defaultCategoryMapping().setMeta(meta))
                .withAntiMapping(antiMappingProto.setMeta(antiMappingMeta))
                .withContentSystemStatus(DataCampContentStatus.ContentSystemStatus.newBuilder()
                    .setStatusContentVersion(DataCampOfferMeta.VersionCounter.newBuilder()
                        .setCounter(currentOffer.getDataCampContentVersion()).build())
                    .setAllowCategorySelection(false)
                    .setAllowModelSelection(true)
                    .setAllowModelCreateUpdate(true)
                    .setModelBarcodeRequired(true)
                    .setMeta(meta)
                    .setCpcState(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_CONTENT)
                    .setCpaState(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_CONTENT)
                    .setOfferAcceptanceStatus(DataCampContentStatus.OfferAcceptanceStatus.ACCEPTANCE_STATUS_OK)
                    .setCategoryRestriction(DataCampContentStatus.CategoryRestriction.newBuilder()
                        .setAllowedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
                        .setType(DataCampContentStatus.CategoryRestriction.AllowedType.SINGLE))
                    .setOfferHasServicePart(true)
                    .setCategoryConfidence(DataCampContentStatus.CategoryConfidence.CATEGORY_CONFIDENCE_CONTENT)
                    .build())
                .get()
                .setResolution(DataCampResolution.Resolution.newBuilder()
                    .addBySource(DataCampResolution.Verdicts.newBuilder().setMeta(meta)))
                .build());
    }

    @Test
    public void testOfferInClassificationHasCpcContentNeedMappingStatus() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create()
            .defaultNoMatchOffer().get().build();
        Offer currentOffer = OfferTestUtils.simpleOffer(OfferTestUtils.whiteSupplierUnderBiz())
            .setId(1)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.SUGGESTED)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_CLASSIFICATION);

        CategoryRestriction restriction = new SingleCategoryRestriction(OfferTestUtils.TEST_CATEGORY_INFO_ID);

        DataCampOffer.Offer result = OfferToDataCampStateConverter.createDataCampOfferState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(OfferTestUtils.defaultCategory())
                .skuNamesMap(Collections.emptyMap())
                .skuCreatedTsMap(Collections.emptyMap())
                .build(),
            dcOffer.getIdentifiers(),
            currentOffer,
            restriction,
            createConverterConfig(),
            true
        );

        DataCampContentStatus.ContentSystemStatus status = result.getContent().getStatus().getContentSystemStatus();
        assertThat(status.getCpcState())
            .isEqualTo(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_MAPPING);
        assertThat(status.getCpaState())
            .isEqualTo(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_UNKNOWN);
        assertThat(status.getOfferAcceptanceStatus())
            .isEqualTo(
                DataCampContentStatus.OfferAcceptanceStatus.forNumber(currentOffer.getAcceptanceStatus().ordinal()));
    }

    @Test
    public void testContentProcessingStatuses() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create()
            .defaultNoMatchOffer().get().build();
        Offer currentOffer = OfferTestUtils.simpleOffer(OfferTestUtils.blueSupplierUnderBiz1())
            .setId(1)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT);

        CategoryRestriction restriction = new SingleCategoryRestriction(OfferTestUtils.TEST_CATEGORY_INFO_ID);

        DataCampOffer.Offer result = OfferToDataCampStateConverter.createDataCampOfferState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(OfferTestUtils.defaultCategory())
                .skuNamesMap(Collections.emptyMap())
                .skuCreatedTsMap(Collections.emptyMap())
                .build(),
            dcOffer.getIdentifiers(),
            currentOffer,
            restriction,
            createConverterConfig(),
            false
        );

        DataCampContentStatus.ContentSystemStatus status = result.getContent().getStatus().getContentSystemStatus();
        assertThat(status.getCpcState()).isEqualTo(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_NEED_CONTENT);
        assertThat(status.getCpaState()).isEqualTo(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_NEED_CONTENT);
        assertThat(status.getOfferAcceptanceStatus())
            .isEqualTo(DataCampContentStatus.OfferAcceptanceStatus.forNumber(currentOffer.getAcceptanceStatus().ordinal()));
        assertThat(status.getActiveErrorList()).isEmpty();

        currentOffer.setContentStatusActiveError(MbocErrors.get().contentProcessingFailed(currentOffer.getShopSku()));

        ConverterConfig converterConfig = createConverterConfig().toBuilder().useVerdictsForErrors(false).build();

        result = OfferToDataCampStateConverter.createDataCampOfferState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(OfferTestUtils.defaultCategory())
                .skuNamesMap(Collections.emptyMap())
                .skuCreatedTsMap(Collections.emptyMap())
                .build(),
            dcOffer.getIdentifiers(),
            currentOffer,
            restriction,
            converterConfig,
            false
        );

        status = result.getContent().getStatus().getContentSystemStatus();
        assertThat(status.getCpcState()).isEqualTo(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_CARD_CREATE_ERROR);
        assertThat(status.getOfferAcceptanceStatus()).isEqualTo(DataCampContentStatus.OfferAcceptanceStatus.forNumber(currentOffer.getAcceptanceStatus().ordinal()));
        assertThat(status.getCpaState()).isEqualTo(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CARD_CREATE_ERROR);

        var expectedError = MbocErrors.get().contentProcessingFailed(DEFAULT_SHOP_SKU);

        assertThat(status.getActiveErrorList()).containsExactly(DataCampExplanation.Explanation.newBuilder()
            .setLevel(DataCampExplanation.Explanation.Level.ERROR)
            .setCode(expectedError.getErrorCode())
            .setNamespace(DataCampConverterService.MBOC_CI_ERROR)
            .setText(expectedError.render())
            .addAllParams(expectedError.getParams().entrySet().stream()
                .map(e -> DataCampExplanation.Explanation.Param.newBuilder()
                    .setName(e.getKey())
                    .setValue(String.valueOf(e.getValue()))
                    .build())
                .collect(Collectors.toList()))
            .setDetails(DataCampOfferUtil.toJson(expectedError))
            .build());

        currentOffer.setContentStatusActiveError(null);
        currentOffer.updateApprovedSkuMapping(new Offer.Mapping(
            1L, LocalDateTime.now()), Offer.MappingConfidence.PARTNER_SELF);

        result = OfferToDataCampStateConverter.createDataCampOfferState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(OfferTestUtils.defaultCategory())
                .skuNamesMap(Collections.emptyMap())
                .skuCreatedTsMap(Collections.emptyMap())
                .build(),
            dcOffer.getIdentifiers(),
            currentOffer,
            restriction,
            createConverterConfig(),
            false
        );

        status = result.getContent().getStatus().getContentSystemStatus();
        assertThat(status.getCpcState()).isEqualTo(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_READY);
        assertThat(status.getCpaState()).isEqualTo(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_READY);
        assertThat(status.getOfferAcceptanceStatus()).isEqualTo(DataCampContentStatus.OfferAcceptanceStatus.forNumber(currentOffer.getAcceptanceStatus().ordinal()));

        currentOffer.setContentStatusActiveError(MbocErrors.get().contentProcessingFailed(currentOffer.getShopSku()));

        result = OfferToDataCampStateConverter.createDataCampOfferState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(OfferTestUtils.defaultCategory())
                .skuNamesMap(Collections.emptyMap())
                .skuCreatedTsMap(Collections.emptyMap())
                .build(),
            dcOffer.getIdentifiers(),
            currentOffer,
            restriction,
            createConverterConfig(),
            false
        );

        status = result.getContent().getStatus().getContentSystemStatus();
        assertThat(status.getCpcState()).isEqualTo(DataCampContentStatus.OfferContentCpcState.CPC_CONTENT_CARD_UPDATE_ERROR);
        assertThat(status.getCpaState()).isEqualTo(DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_CARD_UPDATE_ERROR);
        assertThat(status.getOfferAcceptanceStatus()).isEqualTo(DataCampContentStatus.OfferAcceptanceStatus.forNumber(currentOffer.getAcceptanceStatus().ordinal()));
    }

    @Test
    public void testPartnerMappingStatus() {
        Instant timestamp = DateTimeUtils.instantNow();
        String testMappingName = "some name";
        DataCampOffer.Offer dcOffer = OfferBuilder.create()
            .defaultNoMatchOffer().get().build();
        Offer currentOffer = OfferTestUtils.simpleOffer(OfferTestUtils.blueSupplierUnderBiz1())
            .setId(1)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
            .setSupplierMappingTimestamp(timestamp)
            .setSupplierCategoryMappingStatus(Offer.MappingStatus.NEW)
            .setSupplierModelMappingStatus(Offer.MappingStatus.ACCEPTED)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.REJECTED)
            .updateApprovedSkuMapping(OfferTestUtils
                .mapping(1222, Offer.SkuType.MARKET), Offer.MappingConfidence.CONTENT);

        CategoryRestriction restriction = new SingleCategoryRestriction(OfferTestUtils.TEST_CATEGORY_INFO_ID);

        DataCampOffer.Offer result = OfferToDataCampStateConverter.createDataCampOfferState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(OfferTestUtils.defaultCategory())
                .skuNamesMap(Collections.singletonMap(currentOffer.getApprovedSkuId(), testMappingName))
                .skuCreatedTsMap(Collections.singletonMap(currentOffer.getApprovedSkuId(), timestamp))
                .build(),
            dcOffer.getIdentifiers(),
            currentOffer,
            restriction,
            createConverterConfig(),
            false
        );
        DataCampOfferMapping.Mapping approvedMapping = result.getContentOrBuilder().getBinding().getApproved();
        assertThat(approvedMapping.getMarketSkuName()).isEqualTo(testMappingName);
        assertThat(approvedMapping.getMarketSkuType())
            .isEqualTo(DataCampOfferMapping.Mapping.MarketSkuType.MARKET_SKU_TYPE_MSKU);

        var mskuChangeTsFromMboc = approvedMapping.getMskuChangeTsFromMboc();
        assertThat(mskuChangeTsFromMboc.getSeconds())
            .isEqualTo(currentOffer.getApprovedSkuMapping().getInstant().getEpochSecond());
        assertThat(mskuChangeTsFromMboc.getNanos())
            .isEqualTo(currentOffer.getApprovedSkuMapping().getInstant().getNano());

        DataCampContentStatus.ContentSystemStatus status = result.getContent().getStatus().getContentSystemStatus();
        assertThat(status.getPartnerMappingStatus()).isEqualTo(
            DataCampContentStatus.PartnerMappingStatus.newBuilder()
                .setTimestamp(DataCampOfferUtil.toTimestamp(timestamp))
                .setCategoryMappingState(DataCampContentStatus.PartnerMappingState.MAPPING_STATUS_NEW)
                .setModelMappingState(DataCampContentStatus.PartnerMappingState.MAPPING_STATUS_ACCEPTED)
                .setSkuMappingState(DataCampContentStatus.PartnerMappingState.MAPPING_STATUS_REJECTED)
                .build()
        );
    }

    @Test
    public void testNoApprovedBindingForSuggest() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create()
            .defaultNoMatchOffer().get().build();
        Offer currentOffer = OfferTestUtils.simpleOffer(OfferTestUtils.blueSupplierUnderBiz1())
            .setId(1)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .setSuggestSkuMapping(
                new Offer.Mapping(1222, LocalDateTime.now(), Offer.SkuType.MARKET));

        CategoryRestriction restriction = new SingleCategoryRestriction(OfferTestUtils.TEST_CATEGORY_INFO_ID);

        DataCampOffer.Offer result = OfferToDataCampStateConverter.createDataCampOfferState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(OfferTestUtils.defaultCategory())
                .skuNamesMap(Map.of())
                .skuCreatedTsMap(Map.of())
                .build(),
            dcOffer.getIdentifiers(),
            currentOffer,
            restriction,
            createConverterConfig(),
            false
        );

        assertThat(result.getContentOrBuilder().getBinding().hasApproved()).isTrue();
        assertThat(result.getContentOrBuilder().getBinding().getApproved().hasMarketCategoryId()).isTrue();
        assertThat(result.getContentOrBuilder().getBinding().getApproved().hasMarketSkuId()).isFalse();
    }

    @Test
    public void testMappingForUc() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create()
            .defaultNoMatchOffer().get().build();
        Offer currentOffer = OfferTestUtils.simpleOffer(OfferTestUtils.blueSupplierUnderBiz1())
            .setId(1)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .setMappedModelId(100500L)
            .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
            .setUploadToYtStamp(1L)
            .updateApprovedSkuMapping(OfferTestUtils
                .mapping(1222, Offer.SkuType.MARKET), Offer.MappingConfidence.CONTENT);

        CategoryRestriction restriction = new SingleCategoryRestriction(OfferTestUtils.TEST_CATEGORY_INFO_ID);

        DataCampOffer.Offer result = OfferToDataCampStateConverter.createDataCampOfferState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(OfferTestUtils.defaultCategory())
                .skuNamesMap(Map.of())
                .skuCreatedTsMap(Map.of())
                .build(),
            dcOffer.getIdentifiers(),
            currentOffer,
            restriction,
            createConverterConfig(),
            false
        );

        assertThat(result.getContentOrBuilder().getBinding().hasMappingForUc()).isTrue();
        DataCampOfferMapping.Mapping mappingForUc = result.getContentOrBuilder().getBinding().getMappingForUc();
        assertThat(mappingForUc.hasMarketCategoryId()).isTrue();
        assertThat(mappingForUc.getMarketCategoryId()).isEqualTo(OfferTestUtils.TEST_CATEGORY_INFO_ID);
        assertThat(mappingForUc.hasMarketModelId()).isTrue();
        assertThat(mappingForUc.getMarketModelId()).isEqualTo(100500L);
        assertThat(mappingForUc.hasMarketSkuId()).isTrue();
        assertThat(mappingForUc.getMarketSkuId()).isEqualTo(1222);
    }

    @Test
    public void testFastSkuMarketTypeForFastSkuApprovedMapping() {
        String testMappingName = "some name";
        Instant testCreatedTs = Instant.now();
        DataCampOffer.Offer dcOffer = OfferBuilder.create()
            .defaultNoMatchOffer().get().build();
        Offer currentOffer = OfferTestUtils.simpleOffer(OfferTestUtils.blueSupplierUnderBiz1())
            .setId(1)
            .setContentProcessingStatusInternal(Offer.ContentProcessingStatus.PROCESSED)
            .updateApprovedSkuMapping(OfferTestUtils
                .mapping(1222, Offer.SkuType.FAST_SKU), Offer.MappingConfidence.CONTENT);

        CategoryRestriction restriction = new SingleCategoryRestriction(OfferTestUtils.TEST_CATEGORY_INFO_ID);

        var category = OfferTestUtils.defaultCategory().setAllowFastSkuCreation(true);
        DataCampOffer.Offer result = OfferToDataCampStateConverter.createDataCampOfferState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(category)
                .skuNamesMap(Collections.singletonMap(currentOffer.getApprovedSkuId(), testMappingName))
                .skuCreatedTsMap(Collections.singletonMap(currentOffer.getApprovedSkuId(), testCreatedTs))
                .build(),
            dcOffer.getIdentifiers(),
            currentOffer,
            restriction,
            createConverterConfig(),
            false
        );

        DataCampOfferMapping.Mapping approvedMapping = result.getContentOrBuilder().getBinding().getApproved();
        assertThat(approvedMapping.getMarketSkuName()).isEqualTo(testMappingName);
        assertThat(approvedMapping.getMarketSkuType())
            .isEqualTo(DataCampOfferMapping.Mapping.MarketSkuType.MARKET_SKU_TYPE_FAST);
    }

    @Test
    public void testEmptyApprovedForNoMapping() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create()
            .defaultNoMatchOffer().get().build();
        Offer currentOffer = OfferTestUtils.simpleOffer(OfferTestUtils.blueSupplierUnderBiz1())
            .setId(1)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER);

        CategoryRestriction restriction = new SingleCategoryRestriction(OfferTestUtils.TEST_CATEGORY_INFO_ID);

        DataCampOffer.Offer result = OfferToDataCampStateConverter.createDataCampOfferState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(OfferTestUtils.defaultCategory())
                .skuNamesMap(Map.of())
                .build(),
            dcOffer.getIdentifiers(),
            currentOffer,
            restriction,
            createConverterConfig(),
            false
        );

        assertThat(result.getContentOrBuilder().getBinding().hasApproved()).isTrue();
        assertThat(result.getContentOrBuilder().getBinding().getApproved().hasMarketCategoryId()).isFalse();
        assertThat(result.getContentOrBuilder().getBinding().getApproved().hasMarketSkuId()).isFalse();
        assertThat(result.getContentOrBuilder().getBinding().getApproved().hasMarketModelId()).isFalse();
        assertThat(result.getContentOrBuilder().getBinding().getApproved().hasMeta()).isTrue();
    }

    @Test
    public void testApprovedForFastSkuMapping() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create()
            .defaultNoMatchOffer().get().build();
        Offer currentOffer = OfferTestUtils.simpleOffer(OfferTestUtils.blueSupplierUnderBiz1())
            .setId(1)
            .updateApprovedSkuMapping(OfferTestUtils
                .mapping(100L, Offer.SkuType.FAST_SKU), Offer.MappingConfidence.PARTNER_SELF)
            .setModelId(0L)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .setContentProcessingStatusInternal(Offer.ContentProcessingStatus.PROCESSED);

        CategoryRestriction restriction = new EmptyCategoryRestriction();

        var category = OfferTestUtils.defaultCategory().setAllowFastSkuCreation(true);
        DataCampOffer.Offer result = OfferToDataCampStateConverter.createDataCampOfferState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(category)
                .skuNamesMap(Map.of())
                .skuCreatedTsMap(Map.of())
                .build(),
            dcOffer.getIdentifiers(),
            currentOffer,
            restriction,
            createConverterConfig(),
            false
        );

        assertThat(result.getContentOrBuilder().getBinding().hasApproved()).isTrue();
        assertThat(result.getContentOrBuilder().getBinding().getApproved().hasMarketCategoryId()).isFalse();
        assertThat(result.getContentOrBuilder().getBinding().getApproved().hasMarketSkuId()).isTrue();
        assertThat(result.getContentOrBuilder().getBinding().getApproved().getMarketSkuId()).isEqualTo(100L);
        assertThat(result.getContentOrBuilder().getBinding().getApproved().getMarketModelId()).isEqualTo(100L);
        assertThat(result.getContentOrBuilder().getBinding().getApproved().hasMeta()).isTrue();
    }

    @Test
    public void testApprovedForFastSkuMappingNotAllowed() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create()
            .defaultNoMatchOffer().get().build();
        Offer currentOffer = OfferTestUtils.simpleOffer(OfferTestUtils.blueSupplierUnderBiz1())
            .setId(1)
            .updateApprovedSkuMapping(OfferTestUtils
                .mapping(100L, Offer.SkuType.FAST_SKU), Offer.MappingConfidence.PARTNER_SELF)
            .setModelId(0L)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER);

        CategoryRestriction restriction = new EmptyCategoryRestriction();

        DataCampOffer.Offer result = OfferToDataCampStateConverter.createDataCampOfferState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(OfferTestUtils.defaultCategory().setAllowFastSkuCreation(false))
                .skuNamesMap(Map.of())
                .skuCreatedTsMap(Map.of())
                .build(),
            dcOffer.getIdentifiers(),
            currentOffer,
            restriction,
            createConverterConfig(),
            false
        );

        assertThat(result.getContentOrBuilder().getBinding().hasApproved()).isTrue();
        assertThat(result.getContentOrBuilder().getBinding().getApproved().hasMarketCategoryId()).isFalse();
        assertThat(result.getContentOrBuilder().getBinding().getApproved().hasMarketSkuId()).isFalse();
        assertThat(result.getContentOrBuilder().getBinding().getApproved().hasMeta()).isTrue();
    }

    @Test
    public void testDeletedApprovedMapping() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create()
            .defaultNoMatchOffer().get().build();
        Offer currentOffer = OfferTestUtils.simpleOffer(OfferTestUtils.blueSupplierUnderBiz1())
            .setId(1)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .updateApprovedSkuMapping(Offer.Mapping.fromSku(ModelStorageCachingService.DELETED_MODEL),
                Offer.MappingConfidence.CONTENT);

        CategoryRestriction restriction = new EmptyCategoryRestriction();

        DataCampOffer.Offer result = OfferToDataCampStateConverter.createDataCampOfferState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(OfferTestUtils.defaultCategory())
                .skuNamesMap(Map.of())
                .build(),
            dcOffer.getIdentifiers(),
            currentOffer,
            restriction,
            createConverterConfig(),
            false
        );

        assertThat(result.getContentOrBuilder().getBinding().hasApproved()).isTrue();
        assertThat(result.getContentOrBuilder().getBinding().getApproved().hasMarketCategoryId()).isFalse();
        assertThat(result.getContentOrBuilder().getBinding().getApproved().hasMarketSkuId()).isFalse();
        assertThat(result.getContentOrBuilder().getBinding().getApproved().hasMarketModelId()).isFalse();
        assertThat(result.getContentOrBuilder().getBinding().getApproved().hasMeta()).isTrue();
    }

    @Test
    public void testDeletedApprovedMappingForUc() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create()
            .defaultNoMatchOffer().get().build();
        Offer currentOffer = OfferTestUtils.simpleOffer(OfferTestUtils.blueSupplierUnderBiz1())
            .setId(1)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .setUploadToYtStamp(1234L)
            .updateApprovedSkuMapping(Offer.Mapping.fromSku(ModelStorageCachingService.DELETED_MODEL),
                Offer.MappingConfidence.CONTENT);

        CategoryRestriction restriction = new EmptyCategoryRestriction();

        DataCampOffer.Offer result = OfferToDataCampStateConverter.createDataCampOfferState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(OfferTestUtils.defaultCategory())
                .skuNamesMap(Map.of())
                .build(),
            dcOffer.getIdentifiers(),
            currentOffer,
            restriction,
            createConverterConfig(),
            false
        );

        assertThat(result.getContentOrBuilder().getBinding().hasMappingForUc()).isTrue();
        DataCampOfferMapping.Mapping mappingForUc = result.getContentOrBuilder().getBinding().getMappingForUc();
        assertThat(mappingForUc.hasMarketCategoryId()).isFalse();
        assertThat(mappingForUc.hasMarketModelId()).isFalse();
        assertThat(mappingForUc.hasMarketSkuId()).isFalse();
        assertThat(mappingForUc.hasMeta()).isTrue();
    }

    @Test
    public void testProcessingResponseIsFilledWhenErrorIsPresent() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create()
            .defaultNoMatchOffer().get().build();
        Offer currentOffer = OfferTestUtils.simpleOffer(OfferTestUtils.blueSupplierUnderBiz1())
            .setId(1)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .setSuggestSkuMapping(
                new Offer.Mapping(1222, LocalDateTime.now(), Offer.SkuType.MARKET))
            .setContentStatusActiveError(MbocErrors.get().barcodeRequired(DEFAULT_SHOP_SKU));

        CategoryRestriction restriction = new SingleCategoryRestriction(OfferTestUtils.TEST_CATEGORY_INFO_ID);

        DataCampOfferMarketContent.MarketContentProcessing processingResponse =
            DataCampOfferMarketContent.MarketContentProcessing.newBuilder()
                .setResult(DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_ERROR)
                .addItems(DataCampOfferMarketContent.MarketContentProcessing.Item.newBuilder()
                    .setItemResult(DataCampOfferMarketContent.MarketContentProcessing.ItemResult.ERROR)
                    .setMessage(DataCampExplanation.Explanation.newBuilder()
                        .setCode("some.code")
                        .build())
                    .build())
                .build();

        DataCampOffer.Offer result = OfferToDataCampStateConverter.createDataCampOfferState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(OfferTestUtils.defaultCategory())
                .skuNamesMap(Map.of())
                .skuCreatedTsMap(Map.of())
                .processingResponse(processingResponse)
                .build(),
            dcOffer.getIdentifiers(),
            currentOffer,
            restriction,
            createConverterConfig(),
            false
        );

        assertThat(result.getContent().getPartner().getMarketSpecificContent().getProcessingResponse())
            .extracting(r -> r.toBuilder().clearMeta().build())
            .isEqualTo(processingResponse);
    }

    @Test
    public void testProcessingResponseIsSkippedWhenErrorIsNotPresent() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create()
            .defaultNoMatchOffer().get().build();
        Offer currentOffer = OfferTestUtils.simpleOffer(OfferTestUtils.blueSupplierUnderBiz1())
            .setId(1)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .setSuggestSkuMapping(
                new Offer.Mapping(1222, LocalDateTime.now(), Offer.SkuType.MARKET))
            .setContentStatusActiveError(null);

        CategoryRestriction restriction = new SingleCategoryRestriction(OfferTestUtils.TEST_CATEGORY_INFO_ID);

        DataCampOfferMarketContent.MarketContentProcessing processingResponse =
            DataCampOfferMarketContent.MarketContentProcessing.newBuilder()
                .setResult(DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_ERROR)
                .addItems(DataCampOfferMarketContent.MarketContentProcessing.Item.newBuilder()
                    .setItemResult(DataCampOfferMarketContent.MarketContentProcessing.ItemResult.ERROR)
                    .setMessage(DataCampExplanation.Explanation.newBuilder()
                        .setCode("some.code")
                        .build())
                    .build())
                .build();

        DataCampOffer.Offer result = OfferToDataCampStateConverter.createDataCampOfferState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(OfferTestUtils.defaultCategory())
                .skuNamesMap(Map.of())
                .skuCreatedTsMap(Map.of())
                .processingResponse(processingResponse)
                .build(),
            dcOffer.getIdentifiers(),
            currentOffer,
            restriction,
            createConverterConfig(),
            false
        );

        assertThat(result.getContent().getPartner().getMarketSpecificContent().getProcessingResponse())
            .extracting(r -> r.toBuilder().clearMeta().build())
            .isEqualTo(DataCampOfferMarketContent.MarketContentProcessing.newBuilder().build());
    }

    @Test
    public void testProcessingResponseIsNotOverwrittenWhenErrorIsPresent() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create()
            .defaultNoMatchOffer().get().build();
        Offer currentOffer = OfferTestUtils.simpleOffer(OfferTestUtils.blueSupplierUnderBiz1())
            .setId(1)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .setSuggestSkuMapping(
                new Offer.Mapping(1222, LocalDateTime.now(), Offer.SkuType.MARKET))
            .setContentStatusActiveError(MbocErrors.get().barcodeRequired(DEFAULT_SHOP_SKU));

        CategoryRestriction restriction = new SingleCategoryRestriction(OfferTestUtils.TEST_CATEGORY_INFO_ID);

        DataCampOfferMarketContent.MarketContentProcessing processingResponse = null;

        DataCampOffer.Offer result = OfferToDataCampStateConverter.createDataCampOfferState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(OfferTestUtils.defaultCategory())
                .skuNamesMap(Map.of())
                .skuCreatedTsMap(Map.of())
                .processingResponse(processingResponse)
                .build(),
            dcOffer.getIdentifiers(),
            currentOffer,
            restriction,
            createConverterConfig(),
            false
        );

        assertThat(result.getContent().getPartner().getMarketSpecificContent().hasProcessingResponse())
            .isFalse();
    }

    @Test
    public void testAllowModelCreateUpdateForcedByNeedContentStatus() {
        Supplier supplierNewContent = OfferTestUtils.simpleSupplier()
            .setType(MbocSupplierType.BUSINESS)
            .setNewContentPipeline(true);
        Category categoryGood = new Category()
            .setCategoryId(1L)
            .setHasKnowledge(true)
            .setAcceptContentFromWhiteShops(true);

        Offer offerWithMapping = OfferTestUtils.simpleOffer(supplierNewContent)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setDataCampOffer(true)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(100L), Offer.MappingConfidence.CONTENT);

        Set<Offer.ProcessingStatus> needContentStatuses = Set.of(
            Offer.ProcessingStatus.NEED_CONTENT,
            Offer.ProcessingStatus.CONTENT_PROCESSING
        );
        ConverterConfig converterConfig = createConverterConfig();

        Set<Offer.ProcessingStatus> otherStatuses = Arrays.stream(Offer.ProcessingStatus.values())
            .filter(not(needContentStatuses::contains))
            .collect(Collectors.toSet());

        SoftAssertions.assertSoftly(s ->
            needContentStatuses.forEach(needContentStatus -> {
                Offer offer = offerWithMapping.copy()
                    .updateProcessingStatusIfValid(needContentStatus);
                DescribedCheck allowModelCreateUpdate = OfferToDataCampStateConverter
                    .checkAllowModelCreateUpdate(context(categoryGood, supplierNewContent), offer, converterConfig, false);
                s.assertThat(allowModelCreateUpdate.isTrue())
                    .as("allowModelCreateUpdate should be %b for status %s", true, needContentStatus)
                    .isTrue();
            }));

        SoftAssertions.assertSoftly(s ->
            otherStatuses.forEach(otherStatus -> {
                Offer offer = offerWithMapping.copy()
                    .updateProcessingStatusIfValid(otherStatus);
                DescribedCheck allowModelCreateUpdate = OfferToDataCampStateConverter
                    .checkAllowModelCreateUpdate(context(categoryGood, supplierNewContent), offer, converterConfig, false);
                s.assertThat(allowModelCreateUpdate.isTrue())
                    .as("allowModelCreateUpdate should be %b for status %s", false, otherStatus)
                    .isFalse();
            }));
    }

    @Test
    public void testAllowModelCreateUpdateDescriptions() {
        ConverterConfig converterConfig = createConverterConfig();

        Supplier supplierNotNewContent = OfferTestUtils.simpleSupplier()
            .setType(MbocSupplierType.BUSINESS)
            .setNewContentPipeline(false);
        Supplier supplierNewContent = OfferTestUtils.simpleSupplier()
            .setType(MbocSupplierType.BUSINESS)
            .setNewContentPipeline(true);
        Category categoryNoKnowledge = new Category()
            .setCategoryId(1L)
            .setHasKnowledge(false)
            .setAcceptContentFromWhiteShops(true);
        Category categoryNotAcceptsContent = new Category()
            .setCategoryId(1L)
            .setHasKnowledge(true)
            .setAcceptContentFromWhiteShops(false);
        Category categoryGood = new Category()
            .setCategoryId(1L)
            .setHasKnowledge(true)
            .setAcceptContentFromWhiteShops(true);

        Offer offerBlueWithMapping = OfferTestUtils.simpleOffer(supplierNewContent)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setDataCampOffer(true)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(100L), Offer.MappingConfidence.CONTENT);
        Offer offerBlueWithSuggest = OfferTestUtils.simpleOffer(supplierNewContent)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .setDataCampOffer(true)
            .setSuggestSkuMapping(OfferTestUtils.mapping(100L));
        Offer offerNoCategory = OfferTestUtils.simpleOffer(supplierNewContent)
            .setDataCampOffer(true)
            .setCategoryIdInternal(0L);
        Offer offerWrongStatus = OfferTestUtils.simpleOffer(supplierNewContent)
            .setDataCampOffer(true)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS);
        Offer offerGood = OfferTestUtils.simpleOffer()
            .setDataCampOffer(true)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED);

        List<DescribedCheck> results = List.of(
            OfferToDataCampStateConverter.checkAllowModelCreateUpdate(
                context(categoryGood, supplierNewContent), offerBlueWithMapping, converterConfig, false),
            OfferToDataCampStateConverter.checkAllowModelCreateUpdate(
                context(categoryGood, supplierNewContent), offerBlueWithSuggest, converterConfig, false),
            OfferToDataCampStateConverter.checkAllowModelCreateUpdate(
                context(categoryGood, supplierNewContent), offerNoCategory, converterConfig, false),
            OfferToDataCampStateConverter.checkAllowModelCreateUpdate(
                context(categoryNoKnowledge, supplierNewContent), offerGood, converterConfig, false),
            OfferToDataCampStateConverter.checkAllowModelCreateUpdate(
                context(categoryNotAcceptsContent, supplierNewContent), offerGood, converterConfig, false),
            OfferToDataCampStateConverter.checkAllowModelCreateUpdate(
                context(categoryGood, supplierNotNewContent), offerGood, converterConfig, false),
            OfferToDataCampStateConverter.checkAllowModelCreateUpdate(
                context(categoryGood, supplierNewContent), offerWrongStatus, converterConfig, false)
        );

        Assertions.assertThat(results)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(
                DescribedCheck.fail("для офера задана неизменяемая карточка"),
                DescribedCheck.fail("для офера задана неизменяемая карточка"),
                DescribedCheck.fail("для офера не определена категория"),
                DescribedCheck.fail("в категории нельзя создавать или изменять карточки"),
                DescribedCheck.fail("создание или изменение карточки запрещено"),
                DescribedCheck.fail("создание или изменение карточки запрещено"),
                DescribedCheck.ok()
            );
    }

    @Test
    public void testAllowModelCreateUpdateForFCIsFalseWhenNotGoodContentOffer() {
        Offer offer = new Offer();
        offer.setCategoryIdForTests(111L, Offer.BindingKind.APPROVED);
        //Status when sending offer to GG
        offer.setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS);
        offer.updateApprovedSkuMapping(OfferTestUtils
            .mapping(1222, Offer.SkuType.FAST_SKU), Offer.MappingConfidence.PARTNER_FAST);
        offer.setForceGoodContentStatus(Offer.ForceGoodContentStatus.FORCE_NOT_GOOD_CONTENT);

        Category category = new Category();
        category.setHasKnowledge(true);

        Supplier supplier = new Supplier();
        supplier.setType(MbocSupplierType.BUSINESS);

        ConverterConfig converterConfig = createConverterConfig();

        DescribedCheck describedCheck = OfferToDataCampStateConverter.checkAllowModelCreateUpdate(
            context(category, supplier), offer, converterConfig, false);
        Assertions.assertThat(describedCheck.isFalse()).isTrue();
        Assertions.assertThat(describedCheck.getFailMessage()).isEqualTo("создание или изменение карточки запрещено");
    }

    @Test
    public void testAllowModelCreateUpdateForFCIsTrueWhenGoodContentOffer() {
        Offer offer = new Offer();
        offer.setCategoryIdForTests(111L, Offer.BindingKind.APPROVED);
        //Status when sending offer to GG
        offer.setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS);
        offer.updateApprovedSkuMapping(OfferTestUtils
            .mapping(1L, Offer.SkuType.FAST_SKU), Offer.MappingConfidence.PARTNER_FAST);

        Category category = new Category()
            .setHasKnowledge(true)
            .setAcceptContentFromWhiteShops(true);

        Supplier supplier = new Supplier()
            .setType(MbocSupplierType.BUSINESS)
            .setNewContentPipeline(true);

        ConverterConfig converterConfig = createConverterConfig();

        DescribedCheck describedCheck = OfferToDataCampStateConverter.checkAllowModelCreateUpdate(
            context(category, supplier), offer, converterConfig, false);
        Assertions.assertThat(describedCheck.isFalse()).isTrue();
        Assertions.assertThat(describedCheck.getFailMessage()).isEqualTo("создание или изменение карточки запрещено");
    }

    @Test
    public void testAllowModelCreateUpdateForPartnerSelfIsTrue() {
        Offer offer = new Offer();
        offer.setCategoryIdForTests(111L, Offer.BindingKind.APPROVED);
        //Status when sending offer to GG
        offer.setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS);
        offer.updateApprovedSkuMapping(OfferTestUtils
            .mapping(123L, Offer.SkuType.PARTNER20), Offer.MappingConfidence.PARTNER_SELF);
        offer.setDataCampOffer(true);

        Category category = new Category();
        category.setAcceptGoodContent(false);
        category.setHasKnowledge(true);

        Supplier supplier = new Supplier();
        supplier.setType(MbocSupplierType.BUSINESS);
        supplier.setNewContentPipeline(true);

        ConverterConfig converterConfig = createConverterConfig();

        DescribedCheck describedCheck = OfferToDataCampStateConverter.checkAllowModelCreateUpdate(
            context(category, supplier), offer, converterConfig, false);
        Assertions.assertThat(describedCheck.isTrue()).isTrue();
    }

    @Test
    public void testAllowModelCreateUpdateFalseOnMsku() {
        Offer offer = new Offer();
        offer.setCategoryIdForTests(111L, Offer.BindingKind.APPROVED);
        //Status when sending offer to GG
        offer.setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS);
        offer.updateApprovedSkuMapping(OfferTestUtils
            .mapping(123L, Offer.SkuType.MARKET), Offer.MappingConfidence.PARTNER_SELF);
        offer.setDataCampOffer(true);

        Category category = new Category();
        category.setAcceptGoodContent(false);
        category.setHasKnowledge(true);

        Supplier supplier = new Supplier();
        supplier.setType(MbocSupplierType.BUSINESS);
        supplier.setNewContentPipeline(true);

        ConverterConfig converterConfig = createConverterConfig();

        DescribedCheck describedCheck = OfferToDataCampStateConverter.checkAllowModelCreateUpdate(
            context(category, supplier), offer, converterConfig, false);
        Assertions.assertThat(describedCheck.isTrue()).isFalse();
    }

    @Test
    public void testAllowModelCreateUpdateOnCsku() {
        Offer offer = new Offer();
        offer.setBusinessId(2);
        offer.setCategoryIdForTests(111L, Offer.BindingKind.APPROVED);
        offer.setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS);
        offer.updateApprovedSkuMapping(OfferTestUtils
            .mapping(123L, Offer.SkuType.PARTNER20), Offer.MappingConfidence.CONTENT);
        offer.setDataCampOffer(true);

        Category category = new Category();
        category.setAcceptGoodContent(false);
        category.setHasKnowledge(true);

        Supplier supplier = new Supplier();
        supplier.setType(MbocSupplierType.BUSINESS);
        supplier.setNewContentPipeline(true);

        ConverterConfig converterConfig = ConverterConfig.builder()
            .allowPsku2EditingByAuthor(true)
            .useVerdictsForErrors(true)
            .explanationsNamespace("test")
            .build();

        var contextOk = Context.builder()
            .supplier(supplier)
            .category(category)
            .build();

        DescribedCheck expectedCheckOk = OfferToDataCampStateConverter.checkAllowModelCreateUpdate(
            contextOk, offer, converterConfig, false
        );
        assertTrue(expectedCheckOk.isTrue());

        //but not if category has no knowledge
        category.setHasKnowledge(false);
        DescribedCheck expectedCheckFalse = OfferToDataCampStateConverter.checkAllowModelCreateUpdate(
            contextOk, offer, converterConfig, false
        );
        assertFalse(expectedCheckFalse.isTrue());
    }

    @Test
    public void whenHasContentMappedCategoryThenAllowCategorySelectionIsFalse() {
        var offer = OfferTestUtils.nextOffer()
            .setBindingKind(Offer.BindingKind.APPROVED)
            .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.MappingConfidence.CONTENT);
        CategoryRestriction restriction = new EmptyCategoryRestriction();

        boolean result = OfferToDataCampStateConverter.checkAllowCategorySelection(offer, restriction,
            Context.builder().build(), createConverterConfig());

        assertThat(result).isFalse();
    }

    @Test
    public void whenSingleRestrictionThenAllowCategorySelectionIsFalse() {
        var offer = OfferTestUtils.nextOffer()
            .setBindingKind(Offer.BindingKind.APPROVED);
        CategoryRestriction restriction = new SingleCategoryRestriction(OfferTestUtils.TEST_CATEGORY_INFO_ID);

        boolean result = OfferToDataCampStateConverter.checkAllowCategorySelection(offer, restriction,
            Context.builder().build(), createConverterConfig());

        assertThat(result).isFalse();
    }

    @Test
    public void whenNotSingleRestrictionThenAllowCategorySelectionIsTrue() {
        var offer = OfferTestUtils.nextOffer()
            .setBindingKind(Offer.BindingKind.APPROVED);
        var restrictions = Arrays.stream(AllowedCategoryType.values())
            .filter(x -> x != AllowedCategoryType.SINGLE)
            .map(type -> new EmptyCategoryRestriction() {
                @Override
                public AllowedCategoryType getType() {
                    return type;
                }
            })
            .collect(Collectors.toList());

        SoftAssertions.assertSoftly(softAssertions -> {
            restrictions.forEach(restriction -> {
                boolean result = OfferToDataCampStateConverter.checkAllowCategorySelection(offer, restriction,
                    Context.builder().build(), createConverterConfig());
                softAssertions.assertThat(result)
                    .as("restriction type %s allows category selection", restriction.getType().name())
                    .isTrue();
            });
        });
    }

    @Test
    public void whenNullRestrictionApprovedThenAllowCategorySelectionIsFalse() {
        var offer = OfferTestUtils.nextOffer()
            .setBindingKind(Offer.BindingKind.APPROVED);
        CategoryRestriction restriction = null;

        boolean result = OfferToDataCampStateConverter.checkAllowCategorySelection(offer, restriction,
            Context.builder().build(), createConverterConfig());

        assertThat(result).isFalse();
    }

    @Test
    public void whenNullRestrictionNotApprovedThenAllowCategorySelectionIsTrue() {
        var offers = Arrays.stream(Offer.BindingKind.values())
            .filter(x -> x != Offer.BindingKind.APPROVED)
            .map(bindingKind -> OfferTestUtils.nextOffer()
                .setBindingKind(bindingKind))
            .collect(Collectors.toList());

        CategoryRestriction restriction = null;

        SoftAssertions.assertSoftly(softAssertions -> {
            offers.forEach(offer -> {
                boolean result = OfferToDataCampStateConverter.checkAllowCategorySelection(offer, restriction,
                    Context.builder().build(), createConverterConfig());
                softAssertions.assertThat(result)
                    .as("bindingKind %s allows category selection", offer.getBindingKind())
                    .isTrue();
            });
        });
    }

    @Test
    public void whenNoCategoryStatusThenAllowCategorySelectionIsFalse() {
        var offer = OfferTestUtils.nextOffer()
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NO_CATEGORY);
        CategoryRestriction restriction = new GroupCategoryRestriction(1L, Set.of());

        boolean result = OfferToDataCampStateConverter.checkAllowCategorySelection(offer, restriction,
            Context.builder().build(), createConverterConfig());

        assertThat(result).isFalse();
    }

    @Test
    public void whenHasModelIdThenAllowCategorySelectionIsFalse() {
        var offer = OfferTestUtils.nextOffer()
            .setModelId(1L);
        CategoryRestriction restriction = new GroupCategoryRestriction(1L, Set.of());

        boolean result = OfferToDataCampStateConverter.checkAllowCategorySelection(offer, restriction,
            Context.builder().build(), createConverterConfig());

        assertThat(result).isFalse();
    }

    @Test
    public void whenApprovedPartnerSkuThenAllowCategorySelectionIsTrue() {
        var offers = Stream.of(
            Offer.MappingConfidence.PARTNER_SELF,
            Offer.MappingConfidence.PARTNER_FAST
        ).map(confidence -> OfferTestUtils.nextOffer()
            .setBindingKind(Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(1L), confidence)
        ).collect(Collectors.toList());

        CategoryRestriction restriction = new GroupCategoryRestriction(1L, Set.of());

        SoftAssertions.assertSoftly(softAssertions -> {
            offers.forEach(offer -> {
                boolean result = OfferToDataCampStateConverter.checkAllowCategorySelection(offer, restriction,
                    Context.builder().build(), createConverterConfig());
                softAssertions.assertThat(result)
                    .as("approved %s allows category selection", offer.getApprovedSkuMappingConfidence())
                    .isTrue();
            });
        });
    }

    @Test
    public void whenContentApprovedMappingThenAllowCategorySelectionIsFalse() {
        var offer = OfferTestUtils.nextOffer()
            .setBindingKind(Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(1L), Offer.MappingConfidence.CONTENT);

        CategoryRestriction restriction = new GroupCategoryRestriction(1L, Set.of());

        boolean result = OfferToDataCampStateConverter.checkAllowCategorySelection(offer, restriction,
            Context.builder().build(), createConverterConfig());
        assertThat(result).isFalse();
    }

    @Test
    public void whenHasSuggestMappingThenAllowCategorySelectionIsFalse() {
        var offer = OfferTestUtils.nextOffer()
            .setSuggestSkuMapping(OfferTestUtils.mapping(1L));
        CategoryRestriction restriction = new GroupCategoryRestriction(1L, Set.of());

        boolean result = OfferToDataCampStateConverter.checkAllowCategorySelection(offer, restriction,
            Context.builder().build(), createConverterConfig());

        assertThat(result).isFalse();
    }

    @Test
    public void shouldAddPartnerCategoryClassification() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create()
            .defaultNoMatchOffer().get().build();
        Offer currentOffer = OfferTestUtils.nextOffer()
            .setRecheckCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
            .setRecheckClassificationStatus(Offer.RecheckClassificationStatus.CONFIRMED);

        DataCampOffer.Offer result = OfferToDataCampStateConverter.createDataCampOfferState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(OfferTestUtils.defaultCategory())
                .skuNamesMap(Map.of())
                .skuCreatedTsMap(Map.of())
                .build(),
            dcOffer.getIdentifiers(),
            currentOffer,
            new EmptyCategoryRestriction(),
            createConverterConfig(),
            true
        );

        assertThat(result.getContentOrBuilder().getBinding().hasPartnerCategoryClassification()).isTrue();
        var partnerCategoryClassification =
            result.getContentOrBuilder().getBinding().getPartnerCategoryClassification();

        assertEquals(partnerCategoryClassification.getReclassificationResult().getMarketCategoryId(),
            OfferTestUtils.TEST_CATEGORY_INFO_ID);
        assertEquals(partnerCategoryClassification.getReclassificationResult().getValue(),
            DataCampOfferMapping.ReclassificationResult.Result.STILL_APPROVED);
    }

    @Test
    public void shouldAddPartnerCategoryMappingStatus() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create()
            .defaultNoMatchOffer().get().build();
        Offer currentOffer = OfferTestUtils.nextOffer()
            .setSupplierCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
            .setSupplierCategoryMappingStatus(Offer.MappingStatus.ACCEPTED);

        DataCampOffer.Offer result = OfferToDataCampStateConverter.createDataCampOfferState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(OfferTestUtils.defaultCategory())
                .skuNamesMap(Map.of())
                .skuCreatedTsMap(Map.of())
                .build(),
            dcOffer.getIdentifiers(),
            currentOffer,
            new EmptyCategoryRestriction(),
            createConverterConfig(),
            true
        );

        assertThat(result.getContentOrBuilder().getBinding().hasPartnerCategoryClassification()).isTrue();
        var partnerCategoryClassification =
            result.getContentOrBuilder().getBinding().getPartnerCategoryClassification();

        assertEquals(partnerCategoryClassification.getPartnerMappingStatus().getMarketCategoryId(),
            OfferTestUtils.TEST_CATEGORY_INFO_ID);
        assertEquals(partnerCategoryClassification.getPartnerMappingStatus().getValue(),
            DataCampOfferMapping.PartnerCategoryMappingStatus.Status.ACCEPTED);
    }

    @Test
    public void shouldAddRecheckMappingResultApproved() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create()
            .defaultNoMatchOffer().get().build();
        Offer currentOffer = createRecheckedOffer(
            Offer.RecheckMappingStatus.MAPPING_CONFIRMED,
            Offer.RecheckMappingSource.PARTNER
        );

        DataCampOffer.Offer result = OfferToDataCampStateConverter.createDataCampOfferState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(OfferTestUtils.defaultCategory())
                .skuNamesMap(Map.of())
                .skuCreatedTsMap(Map.of())
                .build(),
            dcOffer.getIdentifiers(),
            currentOffer,
            new EmptyCategoryRestriction(),
            createConverterConfig(),
            false
        );

        assertThat(result.getContentOrBuilder().getBinding().hasPartnerMappingModeration()).isTrue();
        var partnerMappingModeration = result.getContentOrBuilder().getBinding().getPartnerMappingModeration();

        assertEquals(partnerMappingModeration.getRemappingResult().getValue(),
            DataCampOfferMapping.RemappingResult.Result.STILL_APPROVED);
    }

    @Test
    public void shouldAddRecheckMappingResultNotChecked() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create()
            .defaultNoMatchOffer().get().build();
        Offer currentOffer = createRecheckedOffer(
            Offer.RecheckMappingStatus.ON_RECHECK,
            Offer.RecheckMappingSource.PARTNER
        );

        DataCampOffer.Offer result = OfferToDataCampStateConverter.createDataCampOfferState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(OfferTestUtils.defaultCategory())
                .skuNamesMap(Map.of())
                .skuCreatedTsMap(Map.of())
                .build(),
            dcOffer.getIdentifiers(),
            currentOffer,
            new EmptyCategoryRestriction(),
            createConverterConfig(),
            false
        );

        assertThat(result.getContentOrBuilder().getBinding().hasPartnerMappingModeration()).isTrue();
        var partnerMappingModeration = result.getContentOrBuilder().getBinding().getPartnerMappingModeration();

        assertEquals(partnerMappingModeration.getRemappingResult().getValue(),
            DataCampOfferMapping.RemappingResult.Result.NOT_CHECKED);
    }

    @Test
    public void shouldNotAddRecheckMappingResultOnNotRechecked() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create()
            .defaultNoMatchOffer().get().build();
        Offer currentOffer = createRecheckedOffer(
            null,
            null
        );

        DataCampOffer.Offer result = OfferToDataCampStateConverter.createDataCampOfferState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(OfferTestUtils.defaultCategory())
                .skuNamesMap(Map.of())
                .skuCreatedTsMap(Map.of())
                .build(),
            dcOffer.getIdentifiers(),
            currentOffer,
            new EmptyCategoryRestriction(),
            createConverterConfig(),
            false
        );

        assertThat(result.getContentOrBuilder().getBinding().hasPartnerMappingModeration()).isFalse();
    }

    @Test
    public void shouldNotAddRecheckMappingResultOnInternalRecheck() {
        DataCampOffer.Offer dcOffer = OfferBuilder.create()
            .defaultNoMatchOffer().get().build();
        Offer currentOffer = createRecheckedOffer(
            Offer.RecheckMappingStatus.MAPPING_CONFIRMED,
            Offer.RecheckMappingSource.OPERATOR
        );

        DataCampOffer.Offer result = OfferToDataCampStateConverter.createDataCampOfferState(
            Context.builder()
                .supplier(OfferTestUtils.businessSupplier())
                .category(OfferTestUtils.defaultCategory())
                .skuNamesMap(Map.of())
                .skuCreatedTsMap(Map.of())
                .build(),
            dcOffer.getIdentifiers(),
            currentOffer,
            new EmptyCategoryRestriction(),
            createConverterConfig(),
            false
        );

        assertThat(result.getContentOrBuilder().getBinding().hasPartnerMappingModeration()).isFalse();
    }

    @Test
    public void whenModelInMigrationThenAllowCategorySelectionIsFalse() {
        var offers = Stream.of(
            Offer.MappingConfidence.PARTNER_SELF,
            Offer.MappingConfidence.PARTNER_FAST
        ).map(confidence -> OfferTestUtils.nextOffer()
            .setBindingKind(Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(1L), confidence)
        ).collect(Collectors.toList());

        CategoryRestriction restriction = new GroupCategoryRestriction(1L, Set.of());

        SoftAssertions.assertSoftly(softAssertions -> {
            offers.forEach(offer -> {
                boolean result = OfferToDataCampStateConverter.checkAllowCategorySelection(
                    offer,
                    restriction,
                    Context.builder()
                        .isModelInCategoryMigration(true)
                        .build(),
                    createConverterConfig()
                );
                softAssertions.assertThat(result)
                    .as("Category selection for frozen model is forbidden")
                    .isFalse();
            });
        });
    }

    @Test
    public void whenModelInMigrationThenNewAllowCategorySelectionIsFalse() {
        var offers = Stream.of(
            Offer.MappingConfidence.PARTNER_SELF,
            Offer.MappingConfidence.PARTNER_FAST
        ).map(confidence -> OfferTestUtils.nextOffer()
            .setBindingKind(Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(1L), confidence)
        ).collect(Collectors.toList());

        CategoryRestriction restriction = new GroupCategoryRestriction(1L, Set.of());

        SoftAssertions.assertSoftly(softAssertions -> {
            offers.forEach(offer -> {
                boolean result = OfferToDataCampStateConverter.checkAllowCategorySelection(
                    offer,
                    restriction,
                    Context.builder()
                        .isModelInCategoryMigration(true)
                        .build(),
                    createConverterConfig(cfg -> cfg.enableNewAllowCategorySelection(true))
                );
                softAssertions.assertThat(result)
                    .as("Category selection for frozen model is forbidden")
                    .isFalse();
            });
        });
    }

    @Test
    public void whenRecheckClassificationConfirmedThenNewAllowCategorySelectionIsFalse() {
        var offer = OfferTestUtils.nextOffer();
        offer
            .setRecheckCategoryId(offer.getCategoryId())
            .setRecheckClassificationSource(Offer.RecheckClassificationSource.PARTNER)
            .setRecheckClassificationStatus(Offer.RecheckClassificationStatus.CONFIRMED);

        CategoryRestriction restriction = new GroupCategoryRestriction(1L, Set.of());

        boolean result = OfferToDataCampStateConverter.checkAllowCategorySelection(
            offer,
            restriction,
            Context.builder().build(),
            createConverterConfig(cfg -> cfg.enableNewAllowCategorySelection(true))
        );

        assertThat(result)
            .as("Category selection for confirmed category is forbidden")
            .isFalse();
    }

    @Test
    public void whenNotConfirmedSkuMappingThenNewAllowCategorySelectionIsFalse() {
        var offer = OfferTestUtils.nextOffer()
            .updateApprovedSkuMapping(OfferTestUtils
                .mapping(OfferTestUtils.TEST_SKU_ID, Offer.SkuType.MARKET), Offer.MappingConfidence.CONTENT)
            .setPartnerMappingModerationDecision(null);

        CategoryRestriction restriction = new GroupCategoryRestriction(1L, Set.of());

        boolean result = OfferToDataCampStateConverter.checkAllowCategorySelection(
            offer,
            restriction,
            Context.builder().build(),
            createConverterConfig(cfg -> cfg.enableNewAllowCategorySelection(true))
        );

        assertThat(result)
            .as("Category selection for not confirmed sku mapping is forbidden")
            .isFalse();
    }

    @Test
    public void whenNotConfirmedFastSkuMappingThenNewAllowCategorySelectionIsTrue() {
        var offer = OfferTestUtils.nextOffer()
            .updateApprovedSkuMapping(OfferTestUtils
                .mapping(OfferTestUtils.TEST_SKU_ID, Offer.SkuType.FAST_SKU), Offer.MappingConfidence.CONTENT)
            .setPartnerMappingModerationDecision(null);

        CategoryRestriction restriction = new GroupCategoryRestriction(1L, Set.of());

        boolean result = OfferToDataCampStateConverter.checkAllowCategorySelection(
            offer,
            restriction,
            Context.builder().build(),
            createConverterConfig(cfg -> cfg.enableNewAllowCategorySelection(true))
        );

        assertThat(result)
            .as("Category selection for not confirmed sku mapping is allowed")
            .isTrue();
    }

    private ConverterConfig createConverterConfig() {
        return createConverterConfig(__ -> {
        });
    }

    private ConverterConfig createConverterConfig(Consumer<ConverterConfig.ConverterConfigBuilder> configCustomiser) {
        var defaultConfig = ConverterConfig.builder()
            .allowPsku2EditingByAuthor(true)
            .useVerdictsForErrors(true)
            .explanationsNamespace(DataCampConverterService.MBOC_CI_ERROR);
        configCustomiser.accept(defaultConfig);
        return defaultConfig.build();
    }

    private Offer createRecheckedOffer(Offer.RecheckMappingStatus recheckMappingStatus,
                                       Offer.RecheckMappingSource recheckMappingSource) {
        return OfferTestUtils.simpleOffer(OfferTestUtils.blueSupplierUnderBiz1())
            .setId(1)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .setMappedModelId(100500L)
            .setMappedCategoryId(OfferTestUtils.TEST_CATEGORY_INFO_ID)
            .setUploadToYtStamp(1L)
            .updateApprovedSkuMapping(OfferTestUtils
                .mapping(1222, Offer.SkuType.MARKET), Offer.MappingConfidence.CONTENT)
            .setRecheckMappingSource(recheckMappingSource)
            .setRecheckMappingStatus(recheckMappingStatus);
    }

    private Context context(Category category, Supplier supplier) {
        return Context.builder()
            .category(category)
            .supplier(supplier)
            .build();
    }
}
