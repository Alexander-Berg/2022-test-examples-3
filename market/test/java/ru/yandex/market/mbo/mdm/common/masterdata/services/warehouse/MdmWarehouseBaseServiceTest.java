package ru.yandex.market.mbo.mdm.common.masterdata.services.warehouse;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.warehouse.MdmWarehouse;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.warehouse.MdmWarehouseRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MdmWarehouseBaseServiceTest {

    private final MdmWarehouseRepository repository = mock(MdmWarehouseRepository.class);
    private final MdmWarehouseService service = new MdmWarehouseBaseService(repository);

    @Test
    public void testCheckDropship() {
        // given
        MdmWarehouse dropship = new MdmWarehouse().setId("d").setLmsType(PartnerType.DROPSHIP);
        MdmWarehouse ff = new MdmWarehouse().setId("ff").setLmsType(PartnerType.FULFILLMENT);
        when(repository.findByIds(any())).thenReturn(List.of(dropship, ff));

        // then
        assertThat(service.isDropship(dropship.getId())).isTrue();
        assertThat(service.isDropship(ff.getId())).isFalse();
    }

    @Test
    public void testAllDropshipIdsReturned() {
        // given
        MdmWarehouse dropship1 = new MdmWarehouse().setId("d1").setLmsType(PartnerType.DROPSHIP);
        MdmWarehouse dropship2 = new MdmWarehouse().setId("d2").setLmsType(PartnerType.DROPSHIP);
        MdmWarehouse ff = new MdmWarehouse().setId("ff").setLmsType(PartnerType.FULFILLMENT);
        when(repository.findAll()).thenReturn(List.of(dropship1, dropship2, ff));

        // when
        var allDropshipIds = service.allDropshipIds();

        // then
        assertThat(allDropshipIds).containsExactlyInAnyOrder("d1", "d2");
    }
}
