package ru.yandex.market.mbo.mdm.common.masterdata.services.ssku;

import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.warehouse.MdmWarehouse;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.warehouse.MdmWarehouseService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;

public class MskuParamValuesForSskuLoaderImplTest extends MdmBaseDbTestClass {
    @Autowired
    private MdmWarehouseService mdmWarehouseService;
    @Autowired
    private MskuRepository mskuRepository;

    private MskuParamValuesForSskuLoaderImpl mskuParamValuesLoader;

    @Before
    public void setUp() throws Exception {
        this.mskuParamValuesLoader = new MskuParamValuesForSskuLoaderImpl(mskuRepository,
            mdmWarehouseService);
        mdmWarehouseService.addOrUpdateAll(List.of(
            new MdmWarehouse().setId(145L).setLmsType(PartnerType.FULFILLMENT),
            new MdmWarehouse().setId(48046L).setLmsType(PartnerType.DROPSHIP)
        ));
    }

    @Test
    public void testReplaceDropshipSourceId() {
        Set<String> dropshipIds = mdmWarehouseService.allDropshipIds();

        Assertions.assertThat(mskuParamValuesLoader.replaceDropshipSourceId("", dropshipIds))
            .isEqualTo("");
        Assertions.assertThat(mskuParamValuesLoader.replaceDropshipSourceId("test", dropshipIds))
            .isEqualTo("test");
        Assertions.assertThat(
                mskuParamValuesLoader.replaceDropshipSourceId(
                    "msku:617313990 supplier_id:471555 shop_sku:185340 warehouse:145", dropshipIds))
            .isEqualTo("msku:617313990 supplier_id:471555 shop_sku:185340 warehouse:145");
        Assertions.assertThat(
                mskuParamValuesLoader.replaceDropshipSourceId(
                    "msku:1723948674 supplier_id:607364 shop_sku:900000002202 warehouse:48046", dropshipIds))
            .isEqualTo("msku:1723948674 supplier_id:607364 shop_sku:900000002202 supplier:607364");
        Assertions.assertThat(
                mskuParamValuesLoader.replaceDropshipSourceId(
                    "msku:187375203 supplier_id:618886 shop_sku:ZP-40008 supplier:618886", dropshipIds))
            .isEqualTo("msku:187375203 supplier_id:618886 shop_sku:ZP-40008 supplier:618886");
    }
}
