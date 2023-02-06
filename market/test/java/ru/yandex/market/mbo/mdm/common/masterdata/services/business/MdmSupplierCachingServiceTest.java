package ru.yandex.market.mbo.mdm.common.masterdata.services.business;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmBusinessStage;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierIdGroup;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;

/**
 * @author albina-gima
 * @date 11/3/20
 */
public class MdmSupplierCachingServiceTest extends MdmBaseDbTestClass {
    private static final int BUSINESS_1 = 1;
    private static final int DISABLED_SUPPLIER_1 = 100;
    private static final int ENABLED_SUPPLIER_1 = 101;

    private static final int BUSINESS_2 = 2;
    private static final int DISABLED_SUPPLIER_2 = 102;

    private static final int LONELY_SUPPLIER = 103;

    private static final int WHITE_ENABLED_1 = 104;
    private static final int WHITE_LONELY_1 = 105;
    private static final int WHITE_DISABLED_1 = 106;

    private static final int UNKNOWN_SUPPLIER = 107;

    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;

    private MdmSupplierCachingServiceImpl cache;

    @Before
    public void setup() {
        StorageKeyValueService storageKeyValueService = new StorageKeyValueServiceMock();
        cache = new MdmSupplierCachingServiceImpl(mdmSupplierRepository, storageKeyValueService);
        storageKeyValueService.putValue(MdmProperties.ENABLE_BUSINESS_STATE_UPDATE_IN_MDM_SUPPLIER_CACHE, true);
        var now = Instant.now().minusSeconds(100);
        mdmSupplierRepository.insertBatch(
            supplier(BUSINESS_1, MdmSupplierType.BUSINESS, null, true, now),
            supplier(DISABLED_SUPPLIER_1, MdmSupplierType.THIRD_PARTY, BUSINESS_1, false, now),
            supplier(ENABLED_SUPPLIER_1, MdmSupplierType.THIRD_PARTY, BUSINESS_1, true, now),
            supplier(BUSINESS_2, MdmSupplierType.BUSINESS, null, true, now),
            supplier(DISABLED_SUPPLIER_2, MdmSupplierType.THIRD_PARTY, BUSINESS_2, false, now),
            supplier(LONELY_SUPPLIER, MdmSupplierType.THIRD_PARTY, null, false, now),
            supplier(WHITE_ENABLED_1, MdmSupplierType.MARKET_SHOP, BUSINESS_1, true, now),
            supplier(WHITE_DISABLED_1, MdmSupplierType.MARKET_SHOP, BUSINESS_1, false, now),
            supplier(WHITE_LONELY_1, MdmSupplierType.MARKET_SHOP, null, false, now)
        );
    }

    @Test
    public void testCacheCanStoreValues() {
        cache.refresh();

        MdmSupplierIdGroup cachedIdGroup = cache.getSupplierIdGroupFor(BUSINESS_1).get();

        checkCachedSupplierIdGroup(cachedIdGroup, BUSINESS_1, List.of(ENABLED_SUPPLIER_1));
    }

    @Test
    public void testGetSupplierIdGroupForShouldUpdateCacheIfSupplierBecameBusinessEnabled() {
        cache.refresh();

        //до обновления бизнес-группы в кеше
        MdmSupplierIdGroup supplierIdGroupBefore = cache.getSupplierIdGroupFor(DISABLED_SUPPLIER_1).get();
        checkCachedSupplierIdGroup(supplierIdGroupBefore, null, List.of(DISABLED_SUPPLIER_1));

        boolean isBusinessBefore = cache.isBusiness(DISABLED_SUPPLIER_1);
        Assertions.assertThat(isBusinessBefore).isEqualTo(false);

        MdmBusinessStage businessEnableModeBefore = cache.getBusinessEnableMode(DISABLED_SUPPLIER_1);
        Assertions.assertThat(businessEnableModeBefore).isEqualTo(MdmBusinessStage.BUSINESS_DISABLED);

        //переключаем поставщика на стадию 3 бизнеса (businessEnabled == true)
        mdmSupplierRepository.changeBusinessStage(List.of(DISABLED_SUPPLIER_1), List.of(BUSINESS_1),
            MdmBusinessStage.BUSINESS_ENABLED, BusinessSwitchTransport.MBI_LOGBROKER);

        //при вызове метода кеш должен обновиться для бизнес-группы, к которой относится переданный поставщик
        MdmSupplierIdGroup supplierIdGroupAfter = cache.getSupplierIdGroupFor(DISABLED_SUPPLIER_1).get();
        //проверяем, что в бизнес-группу добавился related supplier
        checkCachedSupplierIdGroup(supplierIdGroupAfter, BUSINESS_1, List.of(DISABLED_SUPPLIER_1, ENABLED_SUPPLIER_1));

        //вызов по любому ид поставщика из бизнес-группы после обновления кеша должен возвращать актуальные данные
        supplierIdGroupAfter = cache.getSupplierIdGroupFor(ENABLED_SUPPLIER_1).get();
        checkCachedSupplierIdGroup(supplierIdGroupAfter, BUSINESS_1, List.of(DISABLED_SUPPLIER_1, ENABLED_SUPPLIER_1));

        boolean isBusinessAfter = cache.isBusiness(DISABLED_SUPPLIER_1);
        Assertions.assertThat(isBusinessAfter).isEqualTo(false);

        MdmBusinessStage businessEnableModeAfter = cache.getBusinessEnableMode(DISABLED_SUPPLIER_1);
        Assertions.assertThat(businessEnableModeAfter).isEqualTo(MdmBusinessStage.BUSINESS_ENABLED);
    }

    @Test
    public void testGetSupplierIdGroupForShouldUpdateCacheIfSupplierWasMarkedAsBusinessDisabled() {
        cache.refresh();

        //до обновления кеша
        MdmSupplierIdGroup supplierIdGroupBefore = cache.getSupplierIdGroupFor(ENABLED_SUPPLIER_1).get();
        checkCachedSupplierIdGroup(supplierIdGroupBefore, BUSINESS_1, List.of(ENABLED_SUPPLIER_1));

        MdmSupplierIdGroup supplierIdGroupBefore2 = cache.getSupplierIdGroupFor(BUSINESS_1).get();
        checkCachedSupplierIdGroup(supplierIdGroupBefore2, BUSINESS_1, List.of(ENABLED_SUPPLIER_1));

        //помечаем поставщика как business_enabled==false
        mdmSupplierRepository.update(supplier(ENABLED_SUPPLIER_1, MdmSupplierType.THIRD_PARTY, BUSINESS_1, false,
            Instant.now()));

        //кеш должен обновиться и удалить этого задизейбленного поставщика
        MdmSupplierIdGroup supplierIdGroupAfter = cache.getSupplierIdGroupFor(ENABLED_SUPPLIER_1).get();
        checkCachedSupplierIdGroup(supplierIdGroupAfter, null, List.of(ENABLED_SUPPLIER_1));

        Assertions.assertThat(cache.getSupplierIdGroupFor(BUSINESS_1)).isEmpty();
    }

    @Test
    public void testAllMethodsWorkCorrectlyForNonBusinessSupplier() {
        MdmBusinessStage businessEnableMode = cache.getBusinessEnableMode(LONELY_SUPPLIER);
        Assertions.assertThat(businessEnableMode).isEqualTo(MdmBusinessStage.NO_BUSINESS);

        MdmSupplierIdGroup supplierIdGroup = cache.getSupplierIdGroupFor(LONELY_SUPPLIER).get();
        checkCachedSupplierIdGroup(supplierIdGroup, null, List.of(LONELY_SUPPLIER));

        Optional<MdmSupplierIdGroup> supplierIdGroupFlatOpt = cache.getFlatRelationsFor(LONELY_SUPPLIER);
        Assertions.assertThat(supplierIdGroupFlatOpt).isPresent();
        MdmSupplierIdGroup supplierIdGroupFlat = supplierIdGroupFlatOpt.get();
        checkCachedSupplierIdGroup(supplierIdGroupFlat, null, List.of(LONELY_SUPPLIER));

        boolean isBusiness = cache.isBusiness(LONELY_SUPPLIER);
        Assertions.assertThat(isBusiness).isEqualTo(false);
    }

    @Test
    public void testAllMethodsForWhiteLonelySupplier() {
        MdmBusinessStage businessEnableMode = cache.getBusinessEnableMode(WHITE_LONELY_1);
        Assertions.assertThat(businessEnableMode).isEqualTo(MdmBusinessStage.NO_DATA);

        Optional<MdmSupplierIdGroup> supplierIdGroup = cache.getSupplierIdGroupFor(WHITE_LONELY_1);
        Assertions.assertThat(supplierIdGroup).isEmpty();

        Optional<MdmSupplierIdGroup> supplierIdGroupFlat = cache.getFlatRelationsFor(WHITE_LONELY_1);
        Assertions.assertThat(supplierIdGroupFlat).isEmpty();

        boolean isBusiness = cache.isBusiness(WHITE_LONELY_1);
        Assertions.assertThat(isBusiness).isEqualTo(false);
    }

    @Test
    public void testAllMethodsForWhiteInGroupSupplier() {
        MdmBusinessStage businessEnableMode = cache.getBusinessEnableMode(WHITE_ENABLED_1);
        Assertions.assertThat(businessEnableMode).isEqualTo(MdmBusinessStage.NO_DATA);

        Optional<MdmSupplierIdGroup> supplierIdGroup = cache.getSupplierIdGroupFor(WHITE_ENABLED_1);
        Assertions.assertThat(supplierIdGroup).isEmpty();

        Optional<MdmSupplierIdGroup> supplierIdGroupFlat = cache.getFlatRelationsFor(WHITE_ENABLED_1);
        Assertions.assertThat(supplierIdGroupFlat).isEmpty();

        boolean isBusiness = cache.isBusiness(WHITE_ENABLED_1);
        Assertions.assertThat(isBusiness).isEqualTo(false);
    }

    @Test
    public void testAllMethodsForUnknownSupplier() {
        MdmBusinessStage businessEnableMode = cache.getBusinessEnableMode(UNKNOWN_SUPPLIER);
        Assertions.assertThat(businessEnableMode).isEqualTo(MdmBusinessStage.NO_DATA);

        Optional<MdmSupplierIdGroup> supplierIdGroup = cache.getSupplierIdGroupFor(UNKNOWN_SUPPLIER);
        Assertions.assertThat(supplierIdGroup).isEmpty();

        Optional<MdmSupplierIdGroup> supplierIdGroupFlat = cache.getFlatRelationsFor(UNKNOWN_SUPPLIER);
        Assertions.assertThat(supplierIdGroupFlat).isEmpty();

        boolean isBusiness = cache.isBusiness(UNKNOWN_SUPPLIER);
        Assertions.assertThat(isBusiness).isEqualTo(false);
    }

    private static MdmSupplier supplier(int id, MdmSupplierType type, Integer business,
                                        boolean businessEnabled, Instant businessStateUpdatedTs) {
        MdmSupplier s = new MdmSupplier();
        s.setId(id);
        s.setType(type);
        if (business != null) {
            s.setBusinessId(business);
        }
        s.setBusinessEnabled(businessEnabled);
        s.setBusinessStateUpdatedTs(businessStateUpdatedTs);
        return s;
    }

    private static void checkCachedSupplierIdGroup(MdmSupplierIdGroup cachedSupplierIdGroup,
                                                   Integer businessId,
                                                   List<Integer> expectedRelatedSupplierIds) {
        Assertions.assertThat(cachedSupplierIdGroup).isNotNull();
        Assertions.assertThat(cachedSupplierIdGroup.getBusinessId()).isEqualTo(businessId);
        Assertions.assertThat(cachedSupplierIdGroup.getRelatedSupplierIds().size())
            .isEqualTo(expectedRelatedSupplierIds.size());
        Assertions.assertThat(cachedSupplierIdGroup.getRelatedSupplierIds())
            .containsAll(expectedRelatedSupplierIds);
    }

}
