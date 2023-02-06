package ru.yandex.market.mboc.app.proto;

import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.http.MboCategoryOfferChanges;
import ru.yandex.market.mboc.http.MboCategoryOfferChanges.FindOffersByBusinessIdRequest;
import ru.yandex.market.mboc.http.SupplierOffer;

public class FindOffersByBizIdTest extends BaseOfferChangesServiceTest {

    @Test
    public void getOffer3P() {
        var resp = mboCategoryOfferChangesService.findOffersByBusinessId(FindOffersByBusinessIdRequest.newBuilder()
            .setBusinessId(60)
            .build());
        var offersList = resp.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(60, "sku4", 404040, 60)
            );
        Assertions.assertThat(resp.getLastShopSku()).isEqualTo("sku4");

        // get next batch
        resp = mboCategoryOfferChangesService.findOffersByBusinessId(FindOffersByBusinessIdRequest.newBuilder()
            .setBusinessId(60)
            .setLastShopSku("sku4")
            .build());
        Assertions.assertThat(resp.getOffersList()).isEmpty();
    }

    @Test
    public void getOffer1P() {
        var resp = mboCategoryOfferChangesService.findOffersByBusinessId(FindOffersByBusinessIdRequest.newBuilder()
            .setBusinessId(465852)
            .build());
        var offersList = resp.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(465852, "000004.sku1003", 505050, 465852),
                offer(465852, "000042.sku5", 505050, 465852),
                offer(465852, "000042.sku6", 100000, 465852),
                offer(465852, "001234.sku1002", 505050, 465852)
            );
        Assertions.assertThat(resp.getLastShopSku()).isEqualTo("001234.sku1002");
    }

    @Test
    public void getOffer1PWithLastSsku() {
        var resp = mboCategoryOfferChangesService.findOffersByBusinessId(FindOffersByBusinessIdRequest.newBuilder()
            .setBusinessId(465852)
            .setLastShopSku("000042.sku5")
            .build());
        var offersList = resp.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(465852, "000042.sku6", 100000, 465852),
                offer(465852, "001234.sku1002", 505050, 465852)
            );
        Assertions.assertThat(resp.getLastShopSku()).isEqualTo("001234.sku1002");
    }

    @Test
    public void getOffer1PWithLastSskuAndLimit() {
        var resp = mboCategoryOfferChangesService.findOffersByBusinessId(FindOffersByBusinessIdRequest.newBuilder()
            .setBusinessId(465852)
            .setLastShopSku("000004.sku1003")
            .setLimit(2)
            .build());
        var offersList = resp.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(465852, "000042.sku5", 505050, 465852),
                offer(465852, "000042.sku6", 100000, 465852)
            );
        Assertions.assertThat(resp.getLastShopSku()).isEqualTo("000042.sku6");
    }

    @Test
    public void getOfferBizId() {
        var resp = mboCategoryOfferChangesService.findOffersByBusinessId(FindOffersByBusinessIdRequest.newBuilder()
            .setBusinessId(100)
            .build());
        var offersList = resp.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(100, "sku100", 10, 101,  102, 103)
            );
        Assertions.assertThat(resp.getLastShopSku()).isEqualTo("sku100");
    }

    @Test
    public void getOffer1PWithDeletedMappingWillReturnOffer() {
        var sku4 = offerRepository.findOfferByBusinessSkuKey(new BusinessSkuKey(77, "sku5"));
        offerRepository.updateOffers(sku4.setApprovedSkuMappingInternal(new Offer.Mapping(0, LocalDateTime.now())));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        var resp = mboCategoryOfferChangesService.findOffersByBusinessId(FindOffersByBusinessIdRequest.newBuilder()
            .setBusinessId(465852)
            .build());
        var offersList = resp.getOffersList();

        Assertions.assertThat(offersList)
            .usingComparatorForElementFieldsWithNames(getSupplierIdComparator(), "serviceOffersList")
            .usingElementComparatorOnFields(FIELDS_TO_COMPARE)
            .containsExactlyInAnyOrder(
                offer(465852, "000004.sku1003", 505050, 465852),
                offer(465852, "000042.sku5", 0, 465852),
                offer(465852, "000042.sku6", 100000, 465852),
                offer(465852, "001234.sku1002", 505050, 465852)
            );
        Assertions.assertThat(resp.getLastShopSku()).isEqualTo("001234.sku1002");
    }

    @Test
    public void getOffer3PWithDeletedMappingWillReturnOffer() {
        var sku4 = offerRepository.findOfferByBusinessSkuKey(new BusinessSkuKey(60, "sku4"));
        offerRepository.updateOffers(sku4.setApprovedSkuMappingInternal(new Offer.Mapping(0, LocalDateTime.now())));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        var resp = mboCategoryOfferChangesService.findOffersByBusinessId(FindOffersByBusinessIdRequest.newBuilder()
            .setBusinessId(60)
            .build());
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

        var resp = mboCategoryOfferChangesService.findOffersByBusinessId(FindOffersByBusinessIdRequest.newBuilder()
            .setBusinessId(60)
            .build());
        var offersList = resp.getOffersList();

        Assertions.assertThat(offersList).isEmpty();
    }

    @Test
    public void getOfferBizIdWithFilterByFBY() {
        var resp = mboCategoryOfferChangesService.findOffersByBusinessId(FindOffersByBusinessIdRequest.newBuilder()
            .setBusinessId(100)
            .addLogisticSchemas(MboCategoryOfferChanges.LogisticSchema.FBY)
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
        var resp = mboCategoryOfferChangesService.findOffersByBusinessId(FindOffersByBusinessIdRequest.newBuilder()
            .setBusinessId(60)
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_THIRD_PARTY)
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
        var resp = mboCategoryOfferChangesService.findOffersByBusinessId(FindOffersByBusinessIdRequest.newBuilder()
            .setBusinessId(60)
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER)
            .build());
        var offersList = resp.getOffersList();

        Assertions.assertThat(offersList).isEmpty();
    }

    @Test
    public void getBizOfferWithoutServiceOffers() {
        var sku100 = offerRepository.findOfferByBusinessSkuKey(100, "sku100");
        sku100.getServiceOffersSuppliers().forEach(sku100::removeServiceOfferIfExistsForTests);
        offerRepository.updateOffers(sku100);

        var resp = mboCategoryOfferChangesService.findOffersByBusinessId(FindOffersByBusinessIdRequest.newBuilder()
            .setBusinessId(100)
            .build());
        var offersList = resp.getOffersList();

        Assertions.assertThat(offersList).isEmpty();
    }

    @Test
    public void getBizOfferWithoutServiceOffersWhichMatchedByFilter() {
        var sku100 = offerRepository.findOfferByBusinessSkuKey(100, "sku100");
        sku100.removeServiceOfferIfExistsForTests(101, 103);
        offerRepository.updateOffers(sku100);

        var resp = mboCategoryOfferChangesService.findOffersByBusinessId(FindOffersByBusinessIdRequest.newBuilder()
            .setBusinessId(100)
            .addLogisticSchemas(MboCategoryOfferChanges.LogisticSchema.FBY)
            .build());
        var offersList = resp.getOffersList();

        Assertions.assertThat(offersList).isEmpty();
    }
}
