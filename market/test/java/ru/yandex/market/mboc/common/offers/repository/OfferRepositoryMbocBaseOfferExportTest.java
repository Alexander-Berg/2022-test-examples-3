package ru.yandex.market.mboc.common.offers.repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

public class OfferRepositoryMbocBaseOfferExportTest extends BaseDbTestClass {
    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Consumer<Map<String, ?>> validateColumns = map -> Assertions.assertThat(map)
        .containsOnlyKeys(
            "business_id",
            "shop_sku",
            "offer_destination",
            "mapped_category_id",
            "mapped_category_confidence",
            "mapped_model_id",
            "mapped_model_confidence",
            "approved_sku_mapping_id",
            "approved_sku_mapping_confidence",
            "processing_status",
            "first_supplier_id",
            "supplier_ids");

    @Test
    public void testExportWithServiceOffersView() {
        Supplier businessSupplier = OfferTestUtils.simpleSupplier()
            .setId(1)
            .setType(MbocSupplierType.BUSINESS);
        Supplier bluePartySupplier1 = OfferTestUtils.simpleSupplier()
            .setId(2)
            .setBusinessId(businessSupplier.getId())
            .setType(MbocSupplierType.THIRD_PARTY);
        Supplier bluePartySupplier2 = OfferTestUtils.simpleSupplier()
            .setId(3)
            .setBusinessId(businessSupplier.getId())
            .setType(MbocSupplierType.THIRD_PARTY);
        Supplier whiteSupplier1 = OfferTestUtils.simpleSupplier()
            .setId(4)
            .setBusinessId(businessSupplier.getId())
            .setType(MbocSupplierType.MARKET_SHOP);
        Supplier whiteSupplier2 = OfferTestUtils.simpleSupplier()
            .setId(5)
            .setBusinessId(businessSupplier.getId())
            .setType(MbocSupplierType.MARKET_SHOP);

        supplierRepository.insertBatch(List.of(
            businessSupplier, bluePartySupplier1, bluePartySupplier2, whiteSupplier1, whiteSupplier2));

        Offer offerWhiteBlueBlue = OfferTestUtils.nextOffer()
            .setBusinessId(businessSupplier.getId())
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
            .setMappedCategoryId(99L, Offer.MappingConfidence.CONTENT)
            .setMappedModelId(1111111L, Offer.MappingConfidence.CONTENT)
            .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(1), Offer.MappingConfidence.CONTENT)
            .setServiceOffers(List.of(
                new Offer.ServiceOffer(whiteSupplier1.getId(),
                    whiteSupplier1.getType(), Offer.AcceptanceStatus.OK),
                new Offer.ServiceOffer(bluePartySupplier1.getId(),
                    bluePartySupplier1.getType(), Offer.AcceptanceStatus.OK),
                new Offer.ServiceOffer(bluePartySupplier2.getId(),
                    bluePartySupplier2.getType(), Offer.AcceptanceStatus.OK)
            ));

        Offer offerBlueWhiteBlue = OfferTestUtils.nextOffer()
            .setBusinessId(businessSupplier.getId())
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
            .setMappedCategoryId(99L, Offer.MappingConfidence.CONTENT)
            .setMappedModelId(1111111L, Offer.MappingConfidence.CONTENT)
            .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(1), Offer.MappingConfidence.CONTENT)
            .setServiceOffers(List.of(
                new Offer.ServiceOffer(bluePartySupplier1.getId(),
                    bluePartySupplier1.getType(), Offer.AcceptanceStatus.OK),
                new Offer.ServiceOffer(whiteSupplier1.getId(),
                    whiteSupplier1.getType(), Offer.AcceptanceStatus.OK),
                new Offer.ServiceOffer(bluePartySupplier2.getId(),
                    bluePartySupplier2.getType(), Offer.AcceptanceStatus.OK)
            ));

        Offer offerWhiteWhite = OfferTestUtils.nextOffer()
            .setBusinessId(businessSupplier.getId())
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
            .setMappedCategoryId(99L, Offer.MappingConfidence.CONTENT)
            .setMappedModelId(1111111L, Offer.MappingConfidence.CONTENT)
            .setCategoryIdForTests(99L, Offer.BindingKind.APPROVED)
            .updateApprovedSkuMapping(OfferTestUtils.mapping(1), Offer.MappingConfidence.CONTENT)
            .setServiceOffers(List.of(
                new Offer.ServiceOffer(whiteSupplier1.getId(),
                    whiteSupplier1.getType(), Offer.AcceptanceStatus.OK),
                new Offer.ServiceOffer(whiteSupplier2.getId(),
                    whiteSupplier2.getType(), Offer.AcceptanceStatus.OK)
            ));

        offerRepository.insertOffers(List.of(
            offerWhiteBlueBlue, offerBlueWhiteBlue, offerWhiteWhite));

        List<Map<String, Object>> offers = jdbcTemplate.queryForList(
            "select * from mbo_category.v_mboc_base_offer order by shop_sku");

        // 3 базовых офера
        Assertions.assertThat(offers).hasSize(3);

        var offersByShopSku = offers.stream()
            .peek(validateColumns)
            .map(SelectedOffer::new)
            .collect(Collectors.groupingBy(offer -> offer.shopSku));

        Assertions.assertThat(offersByShopSku.get(offerWhiteBlueBlue.getShopSku()))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(SelectedOffer.builder()
                .businessId(offerWhiteBlueBlue.getBusinessId())
                .offerDestination(offerWhiteBlueBlue.getOfferDestination().name())
                .mappedCategoryId(offerWhiteBlueBlue.getMappedCategoryId())
                .mappedCategoryConfidence(offerWhiteBlueBlue.getMappedCategoryConfidence().name())
                .mappedModelId(offerWhiteBlueBlue.getMappedModelId())
                .mappedModelConfidence(offerWhiteBlueBlue.getMappedModelConfidence().name())
                .approvedSkuMappingConfidence(offerWhiteBlueBlue.getApprovedSkuMappingConfidence().name())
                .shopSku(offerWhiteBlueBlue.getShopSku())
                .approvedSkuMappingId(offerWhiteBlueBlue.getApprovedSkuMapping().getMappingId())
                .processingStatus(offerWhiteBlueBlue.getProcessingStatus().name())
                // second service offer is picked as first not MARKET_SHOP
                .firstSupplierId(offerWhiteBlueBlue.getServiceOffers().get(1).getSupplierId())
                .supplierIds(offerWhiteBlueBlue.getServiceOffers().stream()
                    .map(Offer.ServiceOffer::getSupplierId)
                    .map(Objects::toString)
                    .collect(Collectors.joining(",")))
                .build());

        Assertions.assertThat(offersByShopSku.get(offerBlueWhiteBlue.getShopSku()))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(SelectedOffer.builder()
                .businessId(offerBlueWhiteBlue.getBusinessId())
                .offerDestination(offerBlueWhiteBlue.getOfferDestination().name())
                .mappedCategoryId(offerBlueWhiteBlue.getMappedCategoryId())
                .mappedCategoryConfidence(offerBlueWhiteBlue.getMappedCategoryConfidence().name())
                .mappedModelId(offerBlueWhiteBlue.getMappedModelId())
                .mappedModelConfidence(offerBlueWhiteBlue.getMappedModelConfidence().name())
                .approvedSkuMappingConfidence(offerBlueWhiteBlue.getApprovedSkuMappingConfidence().name())
                .shopSku(offerBlueWhiteBlue.getShopSku())
                .approvedSkuMappingId(offerBlueWhiteBlue.getApprovedSkuMapping().getMappingId())
                .processingStatus(offerBlueWhiteBlue.getProcessingStatus().name())
                // first service offer is picked as first not MARKET_SHOP
                .firstSupplierId(offerBlueWhiteBlue.getServiceOffers().get(0).getSupplierId())
                .supplierIds(offerBlueWhiteBlue.getServiceOffers().stream()
                    .map(Offer.ServiceOffer::getSupplierId)
                    .map(Objects::toString)
                    .collect(Collectors.joining(",")))
                .build());

        Assertions.assertThat(offersByShopSku.get(offerWhiteWhite.getShopSku()))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(SelectedOffer.builder()
                .businessId(offerWhiteWhite.getBusinessId())
                .offerDestination(offerWhiteWhite.getOfferDestination().name())
                .mappedCategoryId(offerWhiteWhite.getMappedCategoryId())
                .mappedCategoryConfidence(offerWhiteWhite.getMappedCategoryConfidence().name())
                .mappedModelId(offerWhiteWhite.getMappedModelId())
                .mappedModelConfidence(offerWhiteWhite.getMappedModelConfidence().name())
                .approvedSkuMappingConfidence(offerWhiteWhite.getApprovedSkuMappingConfidence().name())
                .shopSku(offerWhiteWhite.getShopSku())
                .approvedSkuMappingId(offerWhiteWhite.getApprovedSkuMapping().getMappingId())
                .processingStatus(offerWhiteWhite.getProcessingStatus().name())
                // first service offer is picked as there are MARKET_SHOP-s only
                .firstSupplierId(offerWhiteWhite.getServiceOffers().get(0).getSupplierId())
                .supplierIds(offerWhiteWhite.getServiceOffers().stream()
                    .map(Offer.ServiceOffer::getSupplierId)
                    .map(Objects::toString)
                    .collect(Collectors.joining(",")))
                .build());
    }

    @Builder
    @AllArgsConstructor
    @ToString
    public static class SelectedOffer {
        Integer businessId;
        String shopSku;
        String offerDestination;
        Long mappedCategoryId;
        String mappedCategoryConfidence;
        Long mappedModelId;
        String mappedModelConfidence;
        Long approvedSkuMappingId;
        String approvedSkuMappingConfidence;
        String processingStatus;
        Integer firstSupplierId;
        String supplierIds;

        public SelectedOffer(Map<String, Object> columns) {
            this.businessId = (Integer) columns.get("business_id");
            this.shopSku = (String) columns.get("shop_sku");
            this.offerDestination = (String) columns.get("offer_destination");
            this.mappedCategoryId = (Long) columns.get("mapped_category_id");
            this.mappedCategoryConfidence = (String) columns.get("mapped_category_confidence");
            this.mappedModelId = (Long) columns.get("mapped_model_id");
            this.mappedModelConfidence = (String ) columns.get("mapped_model_confidence");
            this.approvedSkuMappingId = (Long) columns.get("approved_sku_mapping_id");
            this.approvedSkuMappingConfidence = (String) columns.get("approved_sku_mapping_confidence");
            this.processingStatus = (String) columns.get("processing_status");
            this.firstSupplierId = (Integer) columns.get("first_supplier_id");
            this.supplierIds = (String) columns.get("supplier_ids");
        }
    }
}
