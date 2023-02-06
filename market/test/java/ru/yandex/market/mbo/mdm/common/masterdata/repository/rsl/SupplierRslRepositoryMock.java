package ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl;

import java.util.Collection;
import java.util.List;

import ru.yandex.market.mbo.lightmapper.test.GenericMapperRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.SupplierRsl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.SupplierRslParam;
import ru.yandex.market.mbo.mdm.common.rsl.RslType;

public class SupplierRslRepositoryMock extends GenericMapperRepositoryMock<SupplierRsl, SupplierRsl.Key>
    implements SupplierRslRepository {

    public SupplierRslRepositoryMock() {
        super(null, SupplierRsl::getKey);
    }

    @Override
    protected SupplierRsl.Key nextId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<SupplierRsl> findBySupplierRslType(List<RslType> types) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<SupplierRsl> findBySupplierRslParams(Collection<SupplierRslParam> params) {
        throw new UnsupportedOperationException();
    }
}
