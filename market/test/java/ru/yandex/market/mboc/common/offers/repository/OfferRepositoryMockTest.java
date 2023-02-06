package ru.yandex.market.mboc.common.offers.repository;

import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryMock;

/**
 * @author s-ermakov
 */
public class OfferRepositoryMockTest extends OfferRepositoryTest {

    private OfferRepositoryMock offerRepositoryMock = new OfferRepositoryMock();

    private SupplierRepository supplierRepository = new SupplierRepositoryMock();

    @Override
    protected OfferRepository getRepository() {
        return offerRepositoryMock;
    }

    @Override
    protected SupplierRepository getSupplierRepository() {
        return supplierRepository;
    }
}
