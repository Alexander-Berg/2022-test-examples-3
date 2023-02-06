package ru.yandex.market.mbo.mdm.common.masterdata.repository;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.SupplierDqScore;

/**
 * @author dmserebr
 * @date 24/08/2020
 */
@SuppressWarnings("checkstyle:magicNumber")
public class SupplierDqScoreRepositoryImplTest
    extends MdmGenericMapperRepositoryTestBase<SupplierDqScoreRepository, SupplierDqScore, MasterDataSource> {

    @Override
    protected SupplierDqScore randomRecord() {
        return random.nextObject(SupplierDqScore.class);
    }

    @Override
    protected String[] getFieldsToIgnore() {
        return new String[]{"updatedTs"};
    }

    @Override
    protected Function<SupplierDqScore, MasterDataSource> getIdSupplier() {
        return SupplierDqScore::getMasterDataSource;
    }

    @Override
    protected List<BiConsumer<Integer, SupplierDqScore>> getUpdaters() {
        return List.of(
            (i, record) -> record.setDqScore(10),
            (i, record) -> record.setDqScore(-20),
            (i, record) -> record.setDqScore(100),
            (i, record) -> record.setDqScore(10000)
        );
    }
}
