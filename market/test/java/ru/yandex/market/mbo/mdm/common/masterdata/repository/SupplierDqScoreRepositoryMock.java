package ru.yandex.market.mbo.mdm.common.masterdata.repository;

import ru.yandex.market.mbo.lightmapper.test.GenericMapperRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.SupplierDqScore;

/**
 * @author dmserebr
 * @date 24/08/2020
 */
public class SupplierDqScoreRepositoryMock extends GenericMapperRepositoryMock<SupplierDqScore, MasterDataSource>
    implements SupplierDqScoreRepository {

    public SupplierDqScoreRepositoryMock() {
        super(null, SupplierDqScore::getMasterDataSource);
    }

    @Override
    protected MasterDataSource nextId() {
        throw new UnsupportedOperationException();
    }
}
