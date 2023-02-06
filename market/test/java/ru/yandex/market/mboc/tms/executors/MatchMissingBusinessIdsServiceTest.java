package ru.yandex.market.mboc.tms.executors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.services.offers.processing.MatchMissingBusinessIdsService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

public class MatchMissingBusinessIdsServiceTest extends BaseDbTestClass {
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private TransactionHelper transactionHelper;
    private MatchMissingBusinessIdsService matchMissingBusinessIdsService;
    private final Integer BUSINESS_ID = 1000;
    private final Integer SUPPLIER_1 = 1019351;
    private final Integer SUPPLIER_2 = 10339600;
    private final Integer SUPPLIER_3 = 1019352;
    private final Integer FIRST_PARTY_SUPPLIER = 10339601;

    @Before
    public void setup() {
        matchMissingBusinessIdsService =
            new MatchMissingBusinessIdsService(supplierRepository, transactionHelper);
        supplierRepository.deleteAll();
        var supplier1 = OfferTestUtils.businessSupplier();
        var supplier2 = OfferTestUtils.simpleSupplier().setId(SUPPLIER_1).setMbiBusinessId(BUSINESS_ID);
        var supplier3 = OfferTestUtils.simpleSupplier().setId(SUPPLIER_2);
        var supplier4 = OfferTestUtils.firstPartySupplier().setId(FIRST_PARTY_SUPPLIER).setRealSupplierId("000001");
        var supplier5 = OfferTestUtils.simpleSupplier().setId(SUPPLIER_3).setMbiBusinessId(BUSINESS_ID);
        supplierRepository.insertBatch(supplier1, supplier2, supplier3, supplier4, supplier5);
    }

    @Test
    public void testMissingBusinessIdsProcessing() {
        matchMissingBusinessIdsService.process();

        var firstPartySupplier = supplierRepository.findById(FIRST_PARTY_SUPPLIER);
        Assertions.assertThat(firstPartySupplier).isNotNull();
        Assertions.assertThat(firstPartySupplier.getBusinessId()).isNull();

        var supplierWithBusiness = supplierRepository.findById(SUPPLIER_1);
        Assertions.assertThat(supplierWithBusiness).isNotNull();
        Assertions.assertThat(supplierWithBusiness.getBusinessId()).isNotNull();
        Assertions.assertThat(supplierWithBusiness.getBusinessId()).isEqualTo(BUSINESS_ID);

        var anotherSupplierWithBusiness = supplierRepository.findById(SUPPLIER_3);
        Assertions.assertThat(anotherSupplierWithBusiness).isNotNull();
        Assertions.assertThat(anotherSupplierWithBusiness.getBusinessId()).isNotNull();
        Assertions.assertThat(anotherSupplierWithBusiness.getBusinessId()).isEqualTo(BUSINESS_ID);

        var supplierWithoutBusiness = supplierRepository.findById(SUPPLIER_2);
        Assertions.assertThat(supplierWithoutBusiness).isNotNull();
        Assertions.assertThat(supplierWithoutBusiness.getBusinessId()).isNull();
    }
}
