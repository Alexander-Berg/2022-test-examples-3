package ru.yandex.market.mboc.common.services.mstat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.MstatOfferStateRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

/**
 * @author apluhin
 * @created 2/18/21
 */
public class MstatOfferStateServiceTest extends BaseDbTestClass {

    private MstatOfferStateService service;
    @Autowired
    private MstatOfferStateRepository mstatOfferStateRepository;

    private Supplier supplier1;
    private Supplier supplier2;
    private Offer.ServiceOffer serviceOffer1;
    private Offer.ServiceOffer serviceOffer2;

    @Before
    public void setUp() throws Exception {
        service = new MstatOfferStateService(mstatOfferStateRepository);
        supplier1 = new Supplier(1, "Test supplier", null, null);
        supplier2 = new Supplier(2, "Test supplier 2", null, null);
        serviceOffer1 = new Offer.ServiceOffer(supplier1.getId(),
            MbocSupplierType.REAL_SUPPLIER,
            Offer.AcceptanceStatus.NEW);
        serviceOffer2 = new Offer.ServiceOffer(supplier2.getId(),
            MbocSupplierType.REAL_SUPPLIER,
            Offer.AcceptanceStatus.NEW);
    }

    @Test
    public void testCorrectSaveState() {
        String readSupplierName = "some_test";
        supplier1.setRealSupplierId(readSupplierName);
        Offer offer = OfferTestUtils.simpleOffer();
        offer.setServiceOffers(serviceOffer1);
        service.insertOrUpdateStatesForOffer(
            Collections.singleton(offer),
            Map.of(supplier1.getId(), supplier1)
        );
        MstatOfferState offerState = mstatOfferStateRepository.findAll().get(0);
        baseCheck(offer, offerState);
        Assertions.assertThat(offerState.getServiceOfferRealSupplierIds()).isEqualTo(Map.of(1, readSupplierName));
    }

    @Test
    public void testCorrectSaveWithoutRealSupplierId() {
        supplier1.setRealSupplierId(null);
        Offer offer = OfferTestUtils.simpleOffer();
        offer.setServiceOffers(serviceOffer1);
        service.insertOrUpdateStatesForOffer(
            Collections.singleton(offer),
            Map.of(supplier1.getId(), supplier1)
        );
        MstatOfferState offerState = mstatOfferStateRepository.findAll().get(0);
        baseCheck(offer, offerState);
        Assertions.assertThat(offerState.getServiceOfferRealSupplierIds()).isEqualTo(Map.of(1, ""));
    }

    @Test
    public void testCorrectSaveWithoutSupplier() {
        Offer offer = OfferTestUtils.simpleOffer();
        offer.setServiceOffers(serviceOffer1);
        service.insertOrUpdateStatesForOffer(
            Collections.singleton(offer),
            Collections.emptyMap()
        );
        MstatOfferState offerState = mstatOfferStateRepository.findAll().get(0);
        baseCheck(offer, offerState);
        Assertions.assertThat(offerState.getServiceOfferRealSupplierIds()).isEqualTo(Map.of(1, ""));
    }

    @Test
    public void testDeleteState() {
        Offer offer1 = OfferTestUtils.simpleOffer(100);
        Offer offer2 = OfferTestUtils.simpleOffer(101);
        service.insertOrUpdateStatesForOffer(Arrays.asList(offer1, offer2), Collections.emptyMap());
        Assertions.assertThat(mstatOfferStateRepository.findAll().size()).isEqualTo(2);
        service.deleteState(MstatOfferState.builder().offerId(offer1.getId()).build());
        List<MstatOfferState> all = mstatOfferStateRepository.findAll();
        Assertions.assertThat(all.size()).isEqualTo(1);
        baseCheck(offer2, all.get(0));
    }

    @Test
    public void testUpdateState() {
        String readSupplierName = "some_test";
        supplier1.setRealSupplierId(readSupplierName);
        Offer offer = OfferTestUtils.simpleOffer(34);
        offer.setServiceOffers(serviceOffer1);
        service.insertOrUpdateStatesForOffer(Collections.singleton(offer), Map.of(supplier1.getId(), supplier1));
        MstatOfferState offerState = mstatOfferStateRepository.findAll().get(0);
        baseCheck(offer, offerState);
        Assertions.assertThat(offerState.getServiceOfferRealSupplierIds()).isEqualTo(Map.of(1, readSupplierName));

        serviceOffer1.setSupplierType(MbocSupplierType.REAL_SUPPLIER);

        offer.setServiceOffers(List.of(serviceOffer1, serviceOffer2));
        offer.setShopSku("new");
        offer.setBusinessId(10001);

        service.insertOrUpdateStatesForOffer(Collections.singleton(offer), Map.of(supplier1.getId(), supplier1));

        MstatOfferState expected = MstatOfferState.copyToBuilder(offerState)
            .businessId(10001)
            .serviceOffers(List.of(serviceOffer1, serviceOffer2))
            .shopSku("new")
            .serviceOfferRealSupplierIds(
                Map.of(supplier1.getId(), readSupplierName,
                    supplier2.getId(), "")
            )
            .build();

        List<MstatOfferState> all = mstatOfferStateRepository.findAll();
        Assertions.assertThat(all.size()).isEqualTo(1);

        Assertions.assertThat(all.get(0)).isEqualTo(expected);
    }

    @Test
    public void testSearchStateByOfferId() {
        Offer offer1 = OfferTestUtils.simpleOffer(100);
        Offer offer2 = OfferTestUtils.simpleOffer(101);
        Offer offer3 = OfferTestUtils.simpleOffer(103);
        service.insertOrUpdateStatesForOffer(Arrays.asList(offer1, offer2, offer3), Collections.emptyMap());
        List<MstatOfferState> stateByOfferId = service.findStateByOfferId(Arrays.asList(100L, 101L));
        Assertions.assertThat(stateByOfferId.size()).isEqualTo(2);
    }

    private void baseCheck(Offer offer, MstatOfferState offerState) {
        Assertions.assertThat(offerState.getOfferId()).isEqualTo(offer.getId());
        Assertions.assertThat(offerState.getBusinessId()).isEqualTo(offer.getBusinessId());
        Assertions.assertThat(offerState.getShopSku()).isEqualTo(offer.getShopSku());
        Assertions.assertThat(offerState.getServiceOffers()).isEqualTo(offer.getServiceOffers());
    }

}
