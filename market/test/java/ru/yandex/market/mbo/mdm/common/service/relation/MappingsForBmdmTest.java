package ru.yandex.market.mbo.mdm.common.service.relation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mdm.http.MdmAttributeValue;

import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.MAPPING_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.MAPPING_MSKU_ID;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.MAPPING_SSKU_ID;

public class MappingsForBmdmTest {

    @Test
    public void shouldConvertMappingToMdmRelation() {
        // given
        ShopSkuKey shopSkuKey = new ShopSkuKey(100, "testSsku");
        SilverSskuKey silverSskuKey = new SilverSskuKey(shopSkuKey, MasterDataSource.DEFAULT_AUTO_SOURCE);
        Long mskuId = 200_000L;
        MappingCacheDao mappingCacheDao = new MappingCacheDao()
            .setShopSkuKey(shopSkuKey)
            .setMskuId(mskuId)
            .setModifiedTimestamp(LocalDateTime.of(2021, 10, 10, 0, 0));
        MappingsForBmdm mappingsForBmdm = new MappingsForBmdm(
            Map.of(mskuId, 1L),
            Map.of(shopSkuKey, List.of(2L)),
            List.of(mappingCacheDao)
        );

        // when
        var mdmRelations = mappingsForBmdm.toMdmRelations();

        // then
        Assertions.assertThat(mdmRelations).hasSize(1);
        var mdmRelation = mdmRelations.iterator().next();
        Assertions.assertThat(mdmRelation.getBaseEntity().getMdmEntityTypeId()).isEqualTo(MAPPING_ID);
        var attributes = mdmRelation.getBaseEntity().getMdmAttributeValuesMap();
        Assertions.assertThat(attributes).containsKey(MAPPING_SSKU_ID);
        Assertions.assertThat(attributes.get(MAPPING_SSKU_ID).getValuesList())
            .extracting(MdmAttributeValue::getReferenceMdmId)
            .containsOnly(2L);

        Assertions.assertThat(attributes).containsKey(MAPPING_MSKU_ID);
        Assertions.assertThat(attributes.get(MAPPING_MSKU_ID).getValuesList())
            .extracting(MdmAttributeValue::getReferenceMdmId)
            .containsOnly(1L);
    }
}
