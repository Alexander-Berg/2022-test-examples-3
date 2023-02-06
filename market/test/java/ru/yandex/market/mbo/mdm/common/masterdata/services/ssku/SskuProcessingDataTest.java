package ru.yandex.market.mbo.mdm.common.masterdata.services.ssku;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.VghValidationRequirements;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.RslMarkups;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.MdmSskuKeyGroup;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.ExistingGoldenItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingData;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class SskuProcessingDataTest {
    private static int supplierIdSeq = 1;
    private static final String ORPHAN_SKU_1 = "orphan1";
    private static final String ORPHAN_SKU_2 = "orphan2";
    private static final String SKU_A = "a";
    private static final String SKU_B = "b";

    private static final ShopSkuKey ORPHAN_KEY1 = new ShopSkuKey(supplierIdSeq++, ORPHAN_SKU_1);
    private static final ShopSkuKey ORPHAN_KEY2 = new ShopSkuKey(supplierIdSeq++, ORPHAN_SKU_2);
    private static final ShopSkuKey BUSINESS_KEY_A = new ShopSkuKey(supplierIdSeq++, SKU_A);
    private static final ShopSkuKey SERVICE_A1 = new ShopSkuKey(supplierIdSeq++, SKU_A);
    private static final ShopSkuKey SERVICE_A2 = new ShopSkuKey(supplierIdSeq++, SKU_A);
    private static final ShopSkuKey BUSINESS_KEY_B = new ShopSkuKey(supplierIdSeq++, SKU_B);
    private static final ShopSkuKey SERVICE_B1 = new ShopSkuKey(supplierIdSeq++, SKU_B);
    private static final ShopSkuKey SERVICE_B2 = new ShopSkuKey(supplierIdSeq++, SKU_B);

    @Test
    public void testRetainWithoutDiscardedKeepsAll() {
        var data = generateDataWithTestContent(Set.of());
        Assertions.assertThat(data).isEqualTo(data.retainActual());
    }

    @Test
    public void testDiscardedServiceKeysDropEntireGroups() {
        var data = generateDataWithTestContent(Set.of(
            ORPHAN_KEY1,
            SERVICE_B1
        )).retainActual();

        Assertions.assertThat(data.getKeysToProcess()).containsExactlyInAnyOrder(
            ORPHAN_KEY2,
            SERVICE_A1
        );
        Assertions.assertThat(data.getAllServiceKeys()).containsExactlyInAnyOrder(
            ORPHAN_KEY2,
            SERVICE_A1, SERVICE_A2
        );
        Assertions.assertThat(data.getKeyGroups()).containsExactlyInAnyOrder(
            MdmSskuKeyGroup.createNoBusinessGroup(ORPHAN_KEY2),
            MdmSskuKeyGroup.createBusinessGroup(BUSINESS_KEY_A, List.of(SERVICE_A1, SERVICE_A2))
        );
        Assertions.assertThat(data.getKeyGroupsByAllKeys().keySet()).containsExactlyInAnyOrder(
            ORPHAN_KEY2,
            BUSINESS_KEY_A, SERVICE_A1, SERVICE_A2
        );
        Assertions.assertThat(data.getFromIrisItems().keySet()).containsExactlyInAnyOrder(
            ORPHAN_KEY2,
            SERVICE_A2 // A1 не было
        );
        Assertions.assertThat(data.getExistingGoldenItems().keySet()).containsExactlyInAnyOrder(
            ORPHAN_KEY2,
            SERVICE_A2 // A1 не было
        );
        Assertions.assertThat(data.getMappings().keySet()).containsExactlyInAnyOrder(
            ORPHAN_KEY2,
            SERVICE_A2 // A1 не было
        );
        Assertions.assertThat(data.getMasterData().keySet()).containsExactlyInAnyOrder(
            ORPHAN_KEY2,
            SERVICE_A2 // A1 не было
        );
    }

    @Test
    public void testDiscardedBusinessKeysDropEntireGroups() {
        var data = generateDataWithTestContent(Set.of(
            ORPHAN_KEY2,
            BUSINESS_KEY_A
        )).retainActual();

        Assertions.assertThat(data.getKeysToProcess()).containsExactlyInAnyOrder(
            ORPHAN_KEY1,
            BUSINESS_KEY_B, SERVICE_B2
        );
        Assertions.assertThat(data.getAllServiceKeys()).containsExactlyInAnyOrder(
            ORPHAN_KEY1,
            SERVICE_B1, SERVICE_B2
        );
        Assertions.assertThat(data.getKeyGroups()).containsExactlyInAnyOrder(
            MdmSskuKeyGroup.createNoBusinessGroup(ORPHAN_KEY1),
            MdmSskuKeyGroup.createBusinessGroup(BUSINESS_KEY_B, List.of(SERVICE_B1, SERVICE_B2))
        );
        Assertions.assertThat(data.getKeyGroupsByAllKeys().keySet()).containsExactlyInAnyOrder(
            ORPHAN_KEY1,
            BUSINESS_KEY_B, SERVICE_B1, SERVICE_B2
        );
        Assertions.assertThat(data.getFromIrisItems().keySet()).containsExactlyInAnyOrder(
            ORPHAN_KEY1,
            BUSINESS_KEY_B
        );
        Assertions.assertThat(data.getExistingGoldenItems().keySet()).containsExactlyInAnyOrder(
            ORPHAN_KEY1,
            BUSINESS_KEY_B
        );
        Assertions.assertThat(data.getMappings().keySet()).containsExactlyInAnyOrder(
            ORPHAN_KEY1,
            BUSINESS_KEY_B
        );
        Assertions.assertThat(data.getMasterData().keySet()).containsExactlyInAnyOrder(
            ORPHAN_KEY1,
            BUSINESS_KEY_B
        );
    }

    private SskuProcessingData generateDataWithTestContent(Set<ShopSkuKey> discarded) {
        Set<ShopSkuKey> initialKeys = Set.of(
            ORPHAN_KEY1,
            ORPHAN_KEY2,
            SERVICE_A1,
            BUSINESS_KEY_B, SERVICE_B2
        );
        Set<ShopSkuKey> allServiceKeys = Set.of(
            ORPHAN_KEY1,
            ORPHAN_KEY2,
            SERVICE_A1, SERVICE_A2,
            SERVICE_B1, SERVICE_B2
        );
        List<MdmSskuKeyGroup> keyGroups = List.of(
            MdmSskuKeyGroup.createNoBusinessGroup(ORPHAN_KEY1),
            MdmSskuKeyGroup.createNoBusinessGroup(ORPHAN_KEY2),
            MdmSskuKeyGroup.createBusinessGroup(BUSINESS_KEY_A, List.of(SERVICE_A1, SERVICE_A2)),
            MdmSskuKeyGroup.createBusinessGroup(BUSINESS_KEY_B, List.of(SERVICE_B1, SERVICE_B2))
        );
        Map<ShopSkuKey, MdmSskuKeyGroup> groupsByKeys = mapify(keyGroups);
        Map<ShopSkuKey, MappingCacheDao> mappings = Map.of(
            ORPHAN_KEY1, new MappingCacheDao().setMskuId(2134324523L),
            ORPHAN_KEY2, new MappingCacheDao().setMskuId(9004302L),
            SERVICE_A2, new MappingCacheDao().setMskuId(78888593L),
            BUSINESS_KEY_B, new MappingCacheDao().setMskuId(546589993L)
        );
        Map<ShopSkuKey, List<FromIrisItemWrapper>> irisItems = Map.of(
            ORPHAN_KEY1, List.of(),
            ORPHAN_KEY2, List.of(),
            SERVICE_A2, List.of(),
            BUSINESS_KEY_B, List.of()
        );
        Map<ShopSkuKey, List<SilverCommonSsku>> silverItems = Map.of(
            ORPHAN_KEY1, List.of(),
            ORPHAN_KEY2, List.of(),
            SERVICE_A2, List.of(),
            BUSINESS_KEY_B, List.of()
        );
        Map<ShopSkuKey, ExistingGoldenItemWrapper> goldenItems = Map.of(
            ORPHAN_KEY1, gold(),
            ORPHAN_KEY2, gold(),
            SERVICE_A2, gold(),
            BUSINESS_KEY_B, gold()
        );
        Map<ShopSkuKey, MasterData> masterData = Map.of(
            ORPHAN_KEY1, new MasterData(),
            ORPHAN_KEY2, new MasterData(),
            SERVICE_A2, new MasterData(),
            BUSINESS_KEY_B, new MasterData()
        );

        RslMarkups rslMarkups = new RslMarkups();
        return new SskuProcessingData(
            initialKeys,
            allServiceKeys,
            keyGroups,
            groupsByKeys,
            mappings,
            irisItems,
            silverItems,
            goldenItems,
            Map.of(),
            Map.of(),
            Map.of(),
            masterData,
            VghValidationRequirements.NO_REQUIREMENTS,
            discarded,
            rslMarkups,
            Map.of(),
            Map.of(),
            Map.of(),
            Set.of(),
            Map.of(),
            Map.of(),
            Map.of());
    }

    private Map<ShopSkuKey, MdmSskuKeyGroup> mapify(List<MdmSskuKeyGroup> keyGroups) {
        Map<ShopSkuKey, MdmSskuKeyGroup> result = new HashMap<>();
        for (MdmSskuKeyGroup group : keyGroups) {
            group.getAll().forEach(k -> result.put(k, group));
        }
        return result;
    }

    private ExistingGoldenItemWrapper gold() {
        return new ExistingGoldenItemWrapper(new ReferenceItemWrapper(), new ReferenceItemWrapper());
    }
}
