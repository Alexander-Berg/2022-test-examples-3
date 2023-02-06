package ru.yandex.market.mboc.common.services.business;

import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryMock;

/**
 * @author shadoff
 */
public class BusinessSupplierServiceMockTest extends BusinessSupplierServiceTest {

    private OfferRepositoryMock offerRepositoryMock = new OfferRepositoryMock();

    private SupplierRepository supplierRepository = new SupplierRepositoryMock();

    @Override
    protected OfferRepository getOfferRepository() {
        return offerRepositoryMock;
    }

    @Override
    protected SupplierRepository getSupplierRepository() {
        return supplierRepository;
    }
}
