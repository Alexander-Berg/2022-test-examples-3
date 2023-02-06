package ru.yandex.market.mboc.common.offers.model;

import java.time.LocalDateTime;

import org.junit.Test;

import ru.yandex.common.util.collections.Either;
import ru.yandex.misc.test.Assert;

public class OfferForServiceTest {
    @Test
    public void withRestoredDeletedApprovedSkuMapping() {
        var mapping1 = new Offer.Mapping(1L, LocalDateTime.now());
        var mapping2 = new Offer.Mapping(1L, LocalDateTime.now());
        var mapping3 = new Offer.Mapping(1L, LocalDateTime.now(), Offer.SkuType.PARTNER);
        var mapping4 = new Offer.Mapping(1L, LocalDateTime.now(), Offer.SkuType.PARTNER10);
        var mapping5 = new Offer.Mapping(1L, LocalDateTime.now(), Offer.SkuType.PARTNER20);
        var mappingDeleted = new Offer.Mapping(0L, LocalDateTime.now());

        var withoutApprovedMapping = offerWithMapping(null, null, true);
        Assert.isTrue(withoutApprovedMapping.withRestoredDeletedApprovedSkuMapping().isLeftNotRight());

        var noDeletedMapping = offerWithMapping(mapping1, null, true);
        Assert.isTrue(noDeletedMapping.withRestoredDeletedApprovedSkuMapping().isLeftNotRight());

        var deletedMapping = offerWithMapping(mappingDeleted, mapping1, true);
        Assert.equals(Either.right(offerWithMapping(mapping1, null, true)),
            deletedMapping.withRestoredDeletedApprovedSkuMapping());
        Assert.isFalse(deletedMapping.withRestoredDeletedApprovedSkuMapping().isLeftNotRight());

        var deletedMappingNotValid = offerWithMapping(mappingDeleted, null, true);
        Assert.isTrue(deletedMappingNotValid.withRestoredDeletedApprovedSkuMapping().isLeftNotRight());

        var notDeletedMapping = offerWithMapping(mapping1, mapping2, true);
        Assert.isTrue(notDeletedMapping.withRestoredDeletedApprovedSkuMapping().isLeftNotRight());

        var deletedMapping2 = offerWithMapping(mappingDeleted, mapping3, true);
        Assert.isTrue(deletedMapping2.withRestoredDeletedApprovedSkuMapping().isLeftNotRight());

        var deletedMapping3 = offerWithMapping(mappingDeleted, mapping4, true);
        Assert.isTrue(deletedMapping2.withRestoredDeletedApprovedSkuMapping().isLeftNotRight());

        var deletedMapping4 = offerWithMapping(mappingDeleted, mapping5, true);
        Assert.isTrue(deletedMapping2.withRestoredDeletedApprovedSkuMapping().isLeftNotRight());

        var deletedMapping5 = offerWithMapping(mappingDeleted, mapping1, false);
        Assert.isTrue(deletedMapping5.withRestoredDeletedApprovedSkuMapping().isLeftNotRight());
        Assert.isTrue(deletedMapping5.withRestoredDeletedApprovedSkuMapping().asLeft()
            .contains("deleted_approved_sku_mapping_confidence"));
    }

    private static final Offer basicOffer = new Offer()
        .setBusinessId(123)
        .setShopSku("test");

    private static OfferForService offerWithMapping(Offer.Mapping approvedMapping, Offer.Mapping deletedApprovedMapping,
                                                    boolean withConfidence) {
        var basic = basicOffer.copy();
        basic.setApprovedSkuMappingInternal(approvedMapping);
        basic.setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        basic.setDeletedApprovedSkuMapping(deletedApprovedMapping);
        if (withConfidence && deletedApprovedMapping != null) {
            basic.setDeletedApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT);
        }
        return new OfferForService(basic, new Offer.ServiceOffer());
    }
}
