package ru.yandex.market.mboc.common.services.business;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryImpl;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryImpl;

/**
 * @author shadoff
 */
public class BusinessSupplierServiceDbTest extends BusinessSupplierServiceTest {

    @Autowired
    private SupplierRepositoryImpl supplierRepository;

    @Autowired
    private OfferRepositoryImpl offerRepository;

    @Override
    protected OfferRepository getOfferRepository() {
        return offerRepository;
    }

    @Override
    protected SupplierRepository getSupplierRepository() {
        return supplierRepository;
    }
}
