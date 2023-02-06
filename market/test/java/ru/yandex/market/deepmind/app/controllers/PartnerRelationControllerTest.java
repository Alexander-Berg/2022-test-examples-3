package ru.yandex.market.deepmind.app.controllers;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.app.DeepmindBaseAppDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.PartnerRelationType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.PartnerRelation;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.repository.partner_relations.PartnerRelationRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;

public class PartnerRelationControllerTest extends DeepmindBaseAppDbTestClass {
    @Resource
    private PartnerRelationRepository partnerRelationRepository;
    @Resource
    private SupplierRepository deepmindSupplierRepository;

    private PartnerRelationController controller;
    @Before
    public void setUp() throws Exception {
        controller = new PartnerRelationController(partnerRelationRepository, deepmindSupplierRepository);
    }

    @Test
    public void filterBySupplierIds() {
        deepmindSupplierRepository.save(
            new Supplier().setId(1).setName("1").setCrossdock(true),
            new Supplier().setId(2).setName("2").setDropship(true),
            new Supplier().setId(3).setName("3").setDropship(true)
        );

        partnerRelationRepository.save(
            partnerRelation(1, 1, null, PartnerRelationType.CROSSDOCK),
            partnerRelation(2, 2, 20L, PartnerRelationType.DROPSHIP),
            partnerRelation(3, 3, 30L, PartnerRelationType.DROPSHIP)
        );

        var result = controller.list(new PartnerRelationController.PartnerRelationFilter()
            .setSupplierIds(List.of(1, 2, 100500)));

        Assertions.assertThat(result)
            .extracting(PartnerRelation::getSupplierId)
            .containsExactlyInAnyOrder(1, 2);
    }

    @Test
    public void skipPartnerRelationsIfSuppliersDontMarkProperly() {
        deepmindSupplierRepository.save(
            new Supplier().setId(1).setName("1").setCrossdock(true),
            new Supplier().setId(2).setName("2").setDropship(true),
            new Supplier().setId(3).setName("3")
        );

        partnerRelationRepository.save(
            partnerRelation(1, 1, null, PartnerRelationType.CROSSDOCK),
            partnerRelation(2, 11, 21L, PartnerRelationType.CROSSDOCK),
            partnerRelation(3, 3, 30L, PartnerRelationType.DROPSHIP)
        );

        var result = controller.list(new PartnerRelationController.PartnerRelationFilter()
            .setSupplierIds(List.of(1, 2, 3)));

        Assertions.assertThat(result)
            .usingElementComparatorIgnoringFields("id", "modifiedTs")
            .containsExactlyInAnyOrder(
                partnerRelation(1, 1, null, PartnerRelationType.CROSSDOCK)
            );
    }

    private PartnerRelation partnerRelation(int supplierId, long fromWarehouse, @Nullable Long toWarehouse,
                                            PartnerRelationType type) {
        return new PartnerRelation()
            .setSupplierId(supplierId)
            .setFromWarehouseIds(fromWarehouse)
            .setToWarehouseId(toWarehouse)
            .setRelationType(type);
    }
}
