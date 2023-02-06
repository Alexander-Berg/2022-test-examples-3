package ru.yandex.market.mbo.mdm.common.masterdata.repository;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import ru.yandex.market.mbo.mdm.common.masterdata.model.ServiceOfferMigrationInfo;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

/**
 * @author dmserebr
 * @date 06/04/2021
 */
public class ServiceOfferMigrationRepositoryImplTest
    extends MdmGenericMapperRepositoryTestBase<ServiceOfferMigrationRepository,
    ServiceOfferMigrationInfo, ShopSkuKey> {

    @Override
    protected ServiceOfferMigrationInfo randomRecord() {
        return random.nextObject(ServiceOfferMigrationInfo.class);
    }

    @Override
    protected Function<ServiceOfferMigrationInfo, ShopSkuKey> getIdSupplier() {
        return ServiceOfferMigrationInfo::getShopSkuKey;
    }

    @Override
    protected List<BiConsumer<Integer, ServiceOfferMigrationInfo>> getUpdaters() {
        return List.of(
            (i, record) -> {
                record.setSrcBusinessId(123);
                record.setDstBusinessId(234);
            },
            (i, record) -> {
                record.setSrcBusinessId(234);
                record.setDstBusinessId(345);
            }
        );
    }

    @Override
    protected String[] getFieldsToIgnore() {
        return new String[] { };
    }
}
