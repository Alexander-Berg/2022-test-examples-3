package ru.yandex.market.mbo.mdm.common.masterdata.services.warehouse;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.warehouse.MdmWarehouse;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.warehouse.MdmWarehouseRepository;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;

public class MdmWarehouseCachedServiceTest extends MdmBaseDbTestClass {

    @Autowired
    private MdmWarehouseRepository repository;

    private MdmWarehouseService service;

    @Before
    public void setUp() {
        service = new MdmWarehouseCachedService(new MdmWarehouseBaseService(repository));
    }

    @Test
    public void testCacheWorksProperly() {
        // given
        MdmWarehouse dropship1 = new MdmWarehouse().setId("d1").setLmsType(PartnerType.DROPSHIP);
        MdmWarehouse dropship2 = new MdmWarehouse().setId("d2").setLmsType(PartnerType.DROPSHIP);
        MdmWarehouse ff = new MdmWarehouse().setId("ff").setLmsType(PartnerType.FULFILLMENT);
        service.addOrUpdateAll(List.of(dropship1, dropship2, ff));

        // when
        var allDropshipIds = service.allDropshipIds();

        // then
        assertThat(allDropshipIds).containsExactlyInAnyOrder("d1", "d2");

        // and add one more item to service
        MdmWarehouse dropship3 = new MdmWarehouse().setId("d3").setLmsType(PartnerType.DROPSHIP);
        service.addOrUpdate(dropship3);

        // when
        var updatedAllDropshipIds = service.allDropshipIds();

        // then
        assertThat(updatedAllDropshipIds).containsExactlyInAnyOrder("d1", "d2", "d3");
    }
}
