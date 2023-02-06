package ru.yandex.market.mboc.app.proto;

import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.http.MboCategoryOfferChanges.GetBaseOffersRequest;
import ru.yandex.market.mboc.http.MboCategoryOfferChanges.LogisticSchema;
import ru.yandex.market.mboc.http.SupplierOffer.SupplierType;

public class GetBaseOffersTest extends BaseOfferChangesServiceTest {

    @Test
    public void getOffer3P() {
        var resp = mboCategoryOfferChangesService.getBaseOffers(GetBaseOffersRequest.newBuilder()
            .setBusinessId(60)
            .addShopSkus("sku4")
            .build());
        var offersList = resp.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(60, "sku4", 404040, 60)
            );
    }

    @Test
    public void getOffer1P() {
        var resp = mboCategoryOfferChangesService.getBaseOffers(GetBaseOffersRequest.newBuilder()
            .setBusinessId(465852)
            .addShopSkus("000042.sku5")
            .addShopSkus("000042.sku6")
            .build());
        var offersList = resp.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(465852, "000042.sku5", 505050, 465852),
                offer(465852, "000042.sku6", 100000, 465852)
            );
    }

    @Test
    public void getBizOffer() {
        var resp = mboCategoryOfferChangesService.getBaseOffers(GetBaseOffersRequest.newBuilder()
            .setBusinessId(100)
            .addShopSkus("sku100")
            .build());
        var offersList = resp.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(100, "sku100", 10, 101, 102, 103)
            );
    }

    @Test
    public void getOfferWithoutMappingWontReturnOffer() {
        var sku4 = offerRepository.findOfferByBusinessSkuKey(new BusinessSkuKey(60, "sku4"));
        offerRepository.updateOffers(sku4.setApprovedSkuMappingInternal(null));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        var resp = mboCategoryOfferChangesService.getBaseOffers(GetBaseOffersRequest.newBuilder()
            .setBusinessId(60).addShopSkus("sku4").build());
        var offersList = resp.getOffersList();

        Assertions.assertThat(offersList).isEmpty();
    }

    @Test
    public void getOfferWithDeletedMappingWillReturnOffer() {
        var sequenceId = offerUpdateSequenceService.getLastModifiedSequenceId();

        var sku4 = offerRepository.findOfferByBusinessSkuKey(new BusinessSkuKey(60, "sku4"));
        offerRepository.updateOffers(sku4.setApprovedSkuMappingInternal(new Offer.Mapping(0, LocalDateTime.now())));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        var resp = mboCategoryOfferChangesService.getBaseOffers(GetBaseOffersRequest.newBuilder()
            .setBusinessId(60).addShopSkus("sku4").build());
        var offersList = resp.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(60, "sku4", 0, 60)
            );
    }

    @Test
    public void getDeletedOfferWontReturnOffer() {
        offerRepository.deleteInTest(60, "sku4");
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        var resp = mboCategoryOfferChangesService.getBaseOffers(GetBaseOffersRequest.newBuilder()
            .setBusinessId(60).addShopSkus("sku4").build());
        var offersList = resp.getOffersList();
        Assertions.assertThat(offersList).isEmpty();
    }

    @Test
    public void getOffer3PWithFilterByFBY() {
        var resp = mboCategoryOfferChangesService.getBaseOffers(GetBaseOffersRequest.newBuilder()
            .setBusinessId(60).addShopSkus("sku4")
            .addLogisticSchemas(LogisticSchema.FBY)
            .build());
        var offersList = resp.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(60, "sku4", 404040, 60)
            );
    }

    @Test
    public void getOffer1PWithFilterByFBY() {
        var resp = mboCategoryOfferChangesService.getBaseOffers(GetBaseOffersRequest.newBuilder()
            .setBusinessId(465852).addShopSkus("000042.sku5").addShopSkus("000042.sku6")
            .addLogisticSchemas(LogisticSchema.FBY)
            .build());
        var offersList = resp.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                // при FBY real_supplier всегда должны возвращаться, внезависимости от того, если у них флаг или нет
                offer(465852, "000042.sku5", 505050, 465852),
                offer(465852, "000042.sku6", 100000, 465852)
            );
    }

    @Test
    public void getOfferBizIdWithFilterByFBY() {
        var resp = mboCategoryOfferChangesService.getBaseOffers(GetBaseOffersRequest.newBuilder()
            .setBusinessId(100).addShopSkus("sku100")
            .addLogisticSchemas(LogisticSchema.FBY)
            .build());
        var offersList = resp.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(100, "sku100", 10, 101, 103)
            );
    }

    @Test
    public void getOffer3PWithFilterByThirdParty() {
        var resp = mboCategoryOfferChangesService.getBaseOffers(GetBaseOffersRequest.newBuilder()
            .setBusinessId(60).addShopSkus("sku4")
            .addSupplierTypes(SupplierType.TYPE_THIRD_PARTY)
            .build());
        var offersList = resp.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(60, "sku4", 404040, 60)
            );
    }

    @Test
    public void getOffer3PWithFilterByRealSupplier() {
        var resp = mboCategoryOfferChangesService.getBaseOffers(GetBaseOffersRequest.newBuilder()
            .setBusinessId(60).addShopSkus("sku4")
            .addSupplierTypes(SupplierType.TYPE_REAL_SUPPLIER)
            .build());
        var offersList = resp.getOffersList();

        Assertions.assertThat(offersList).isEmpty();
    }

    @Test
    public void getBizOfferWithoutServiceOffers() {
        var sku100 = offerRepository.findOfferByBusinessSkuKey(100, "sku100");
        sku100.getServiceOffersSuppliers().forEach(sku100::removeServiceOfferIfExistsForTests);
        offerRepository.updateOffers(sku100);

        var resp = mboCategoryOfferChangesService.getBaseOffers(GetBaseOffersRequest.newBuilder()
            .setBusinessId(100).addShopSkus("sku100")
            .build());
        var offersList = resp.getOffersList();

        Assertions.assertThat(offersList).isEmpty();
    }

    @Test
    public void getBizOfferWithoutServiceOffersWhichMatchedByFilter() {
        var sku100 = offerRepository.findOfferByBusinessSkuKey(100, "sku100");
        sku100.removeServiceOfferIfExistsForTests(101, 103);
        offerRepository.updateOffers(sku100);

        var resp = mboCategoryOfferChangesService.getBaseOffers(GetBaseOffersRequest.newBuilder()
            .setBusinessId(100).addShopSkus("sku100")
            .addLogisticSchemas(LogisticSchema.FBY)
            .build());
        var offersList = resp.getOffersList();

        Assertions.assertThat(offersList).isEmpty();
    }
}
