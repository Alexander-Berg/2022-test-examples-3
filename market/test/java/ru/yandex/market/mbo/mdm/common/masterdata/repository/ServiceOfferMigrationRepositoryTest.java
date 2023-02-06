package ru.yandex.market.mbo.mdm.common.masterdata.repository;

import java.time.Instant;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.ServiceOfferMigrationInfo;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class ServiceOfferMigrationRepositoryTest
    extends MdmGenericMapperRepositoryTestBase<ServiceOfferMigrationRepository, ServiceOfferMigrationInfo, ShopSkuKey> {

    @Autowired
    ServiceOfferMigrationRepository serviceOfferMigrationRepository;

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
                record.setProcessed(true);
                record.setProcessedTimestamp(Instant.now());
            },
            (i, record) -> {
                record.setProcessed(true);
                record.setDstBusinessId(777);
                record.setProcessedTimestamp(Instant.now());
            }
        );
    }

    @Test
    public void whenFindBySupplierIdShouldReturnCorrectRows() {
        var row1 = random.nextObject(ServiceOfferMigrationInfo.class);
        var row2 = random.nextObject(ServiceOfferMigrationInfo.class);
        int supplierId = 777;
        row1.setSupplierId(supplierId);
        row2.setSupplierId(supplierId);
        serviceOfferMigrationRepository.insertOrUpdateAll(List.of(row1, row2));
        var result =
            serviceOfferMigrationRepository.findBySupplierIds(List.of(supplierId));

        Assertions.assertThat(result).hasSize(2);
        Assertions.assertThat(result).containsExactlyInAnyOrder(row1, row2);
    }

    @Test
    public void whenFindUnprocessedShouldReturnCorrecrRows() {
        var row1 = random.nextObject(ServiceOfferMigrationInfo.class);
        var row2 = random.nextObject(ServiceOfferMigrationInfo.class);
        var row3 = random.nextObject(ServiceOfferMigrationInfo.class);
        row1.setProcessed(false);
        row2.setProcessed(false);
        row3.setProcessed(true);
        serviceOfferMigrationRepository.insertOrUpdateAll(List.of(row1, row2, row3));

        var result = serviceOfferMigrationRepository.findAllUnprocessed();
        Assertions.assertThat(result).hasSize(2);
        Assertions.assertThat(result).containsExactlyInAnyOrder(row1, row2);
    }

}
