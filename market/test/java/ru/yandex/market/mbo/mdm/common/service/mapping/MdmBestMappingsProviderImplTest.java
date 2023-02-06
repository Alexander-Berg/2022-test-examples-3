package ru.yandex.market.mbo.mdm.common.service.mapping;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ExpectedMappingQuality;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class MdmBestMappingsProviderImplTest extends MdmBaseDbTestClass {
    @Autowired
    private MdmBestMappingsProvider mdmBestMappingsProvider;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void whenLoadSskuMappingsChooseBestCategoryId() {
        // given
        ShopSkuKey requestedKey = new ShopSkuKey(2, "два");
        ShopSkuKey newerSibling = new ShopSkuKey(3, "три");
        long mskuId = 120;

        MappingCacheDao requestedKeyOriginalMapping = new MappingCacheDao()
            .setShopSkuKey(requestedKey)
            .setMskuId(mskuId)
            .setCategoryId(110)
            .setVersionTimestamp(Instant.parse("2007-12-03T10:15:30.00Z"));
        MappingCacheDao newerSiblingMapping = new MappingCacheDao()
            .setShopSkuKey(newerSibling)
            .setMskuId(mskuId)
            .setCategoryId(120)
            .setVersionTimestamp(Instant.parse("2017-12-03T10:15:30.00Z"));
        mappingsCacheRepository.insertOrUpdateAll(List.of(
            requestedKeyOriginalMapping,
            newerSiblingMapping
        ));

        // when
        List<MappingCacheDao> mappings = mdmBestMappingsProvider.loadBestMappingsForSskus(List.of(requestedKey),
            ExpectedMappingQuality.ANY);

        // then
        Assertions.assertThat(mappings).hasSize(1);
        Assertions.assertThat(mappings.iterator().next())
            .isEqualTo(MappingCacheDao.initFrom(requestedKeyOriginalMapping)
                .setCategoryId(newerSiblingMapping.getCategoryId()));
    }

    @Test
    public void whenLoadMappingsNotFixCategoryIdIfMskuIdIsZero() {
        // given
        ShopSkuKey requestedKey = new ShopSkuKey(2, "два");
        ShopSkuKey newerSibling = new ShopSkuKey(3, "три");
        long mskuId = 0;

        MappingCacheDao requestedKeyOriginalMapping = new MappingCacheDao()
            .setShopSkuKey(requestedKey)
            .setMskuId(mskuId)
            .setCategoryId(110)
            .setVersionTimestamp(Instant.parse("2007-12-03T10:15:30.00Z"));
        MappingCacheDao newerSiblingMapping = new MappingCacheDao()
            .setShopSkuKey(newerSibling)
            .setMskuId(mskuId)
            .setCategoryId(120)
            .setVersionTimestamp(Instant.parse("2017-12-03T10:15:30.00Z"));
        mappingsCacheRepository.insertOrUpdateAll(List.of(
            requestedKeyOriginalMapping,
            newerSiblingMapping
        ));

        // when
        List<MappingCacheDao> mappings = mdmBestMappingsProvider.loadBestMappingsForSskus(List.of(requestedKey),
            ExpectedMappingQuality.ANY);

        // then
        Assertions.assertThat(mappings).hasSize(1);
        Assertions.assertThat(mappings.iterator().next()).isEqualTo(requestedKeyOriginalMapping);
    }

    @Test
    public void whenLoadMskuMappingsChooseBestCategoryId() {
        // given
        ShopSkuKey sskuKey1 = new ShopSkuKey(2, "два");
        ShopSkuKey sskuKey2 = new ShopSkuKey(3, "три");
        long mskuId = 120;

        // чтобы пройти фильтрацию белых поставщиков
        Stream.of(sskuKey1, sskuKey2)
            .map(ShopSkuKey::getSupplierId)
            .map(id -> new MdmSupplier().setId(id).setType(MdmSupplierType.THIRD_PARTY))
            .forEach(mdmSupplierRepository::insertOrUpdate);

        MappingCacheDao mapping1 = new MappingCacheDao()
            .setShopSkuKey(sskuKey1)
            .setMskuId(mskuId)
            .setCategoryId(110)
            .setVersionTimestamp(Instant.parse("2007-12-03T10:15:30.00Z"));
        MappingCacheDao mapping2 = new MappingCacheDao()
            .setShopSkuKey(sskuKey2)
            .setMskuId(mskuId)
            .setCategoryId(120)
            .setVersionTimestamp(Instant.parse("2017-12-03T10:15:30.00Z"));
        mappingsCacheRepository.insertOrUpdateAll(List.of(
            mapping1,
            mapping2
        ));

        // when
        List<MappingCacheDao> mappings = mdmBestMappingsProvider.loadBestMappingsForMskus(List.of(mskuId),
            ExpectedMappingQuality.ANY);

        // then
        Assertions.assertThat(mappings).containsExactlyInAnyOrder(
            MappingCacheDao.initFrom(mapping1).setCategoryId(mapping2.getCategoryId()),
            mapping2
        );
    }

    @Test
    public void loadNoMappingsForZeroMsku() {
        // given
        ShopSkuKey sskuKey1 = new ShopSkuKey(2, "два");
        ShopSkuKey sskuKey2 = new ShopSkuKey(3, "три");
        long mskuId = 0;

        MappingCacheDao mapping1 = new MappingCacheDao()
            .setShopSkuKey(sskuKey1)
            .setMskuId(mskuId)
            .setCategoryId(110)
            .setVersionTimestamp(Instant.parse("2007-12-03T10:15:30.00Z"));
        MappingCacheDao mapping2 = new MappingCacheDao()
            .setShopSkuKey(sskuKey2)
            .setMskuId(mskuId)
            .setCategoryId(120)
            .setVersionTimestamp(Instant.parse("2017-12-03T10:15:30.00Z"));
        mappingsCacheRepository.insertOrUpdateAll(List.of(
            mapping1,
            mapping2
        ));

        // when
        List<MappingCacheDao> mappings = mdmBestMappingsProvider.loadBestMappingsForMskus(List.of(mskuId),
            ExpectedMappingQuality.ANY);

        // then
        Assertions.assertThat(mappings).isEmpty();
    }

    @Test
    public void whenLoadMskuMappingsLoadForAllGroup() {
        // given
        int businessId = 12;
        int serviceId = 13;
        mdmSupplierRepository.insertBatch(
            new MdmSupplier()
                .setId(businessId)
                .setType(MdmSupplierType.BUSINESS),
            new MdmSupplier()
                .setId(serviceId)
                .setType(MdmSupplierType.THIRD_PARTY)
                .setBusinessId(businessId)
                .setBusinessEnabled(true)
        );

        String shopSku = "U-238";
        ShopSkuKey serviceKey = new ShopSkuKey(serviceId, shopSku);
        ShopSkuKey businessKey = new ShopSkuKey(businessId, shopSku);
        sskuExistenceRepository.markExistence(serviceKey, true);

        long mskuId = 123;
        int categoryId = 124;

        MappingCacheDao mappingByService = new MappingCacheDao()
            .setShopSkuKey(serviceKey)
            .setMskuId(mskuId)
            .setCategoryId(categoryId);
        mappingsCacheRepository.insertOrUpdateAll(List.of(mappingByService));
        // and no mapping for business

        // when
        List<MappingCacheDao> mappings = mdmBestMappingsProvider.loadBestMappingsForMskus(List.of(mskuId),
            ExpectedMappingQuality.ANY);

        // then
        Assertions.assertThat(mappings).containsExactlyInAnyOrder(
            mappingByService,
            MappingCacheDao.initFrom(mappingByService).setShopSkuKey(businessKey)
        );
    }

    @Test
    public void whenLoadMskuMappingsChooseBestInGroup() {
        // given
        int businessId = 12;
        int serviceId = 13;
        mdmSupplierRepository.insertBatch(
            new MdmSupplier()
                .setId(businessId)
                .setType(MdmSupplierType.BUSINESS),
            new MdmSupplier()
                .setId(serviceId)
                .setType(MdmSupplierType.THIRD_PARTY)
                .setBusinessId(businessId)
                .setBusinessEnabled(true)
        );

        String shopSku = "U-238";
        ShopSkuKey serviceKey = new ShopSkuKey(serviceId, shopSku);
        ShopSkuKey businessKey = new ShopSkuKey(businessId, shopSku);
        sskuExistenceRepository.markExistence(serviceKey, true);

        long mskuId = 123;
        long anotherMskuId = 125;
        int categoryId = 124;

        MappingCacheDao mappingByService = new MappingCacheDao()
            .setShopSkuKey(serviceKey)
            .setMskuId(mskuId)
            .setCategoryId(categoryId)
            .setVersionTimestamp(Instant.parse("2027-12-03T10:15:30.00Z")); // best
        MappingCacheDao mappingByBusiness = new MappingCacheDao()
            .setShopSkuKey(businessKey)
            .setMskuId(anotherMskuId)
            .setCategoryId(categoryId)
            .setVersionTimestamp(Instant.parse("2017-12-03T10:15:30.00Z"));
        mappingsCacheRepository.insertOrUpdateAll(List.of(mappingByService, mappingByBusiness));

        // when
        List<MappingCacheDao> mappings = mdmBestMappingsProvider.loadBestMappingsForMskus(List.of(mskuId),
            ExpectedMappingQuality.ANY);

        // then
        Assertions.assertThat(mappings).containsExactlyInAnyOrder(
            mappingByService,
            MappingCacheDao.initFrom(mappingByService).setShopSkuKey(businessKey)
        );
    }

    @Test
    public void whenLoadMskuMappingsSkipIfBestInGroupConnectedToAnotherMsku() {
        // given
        int businessId = 12;
        int serviceId = 13;
        mdmSupplierRepository.insertBatch(
            new MdmSupplier()
                .setId(businessId)
                .setType(MdmSupplierType.BUSINESS),
            new MdmSupplier()
                .setId(serviceId)
                .setType(MdmSupplierType.THIRD_PARTY)
                .setBusinessId(businessId)
                .setBusinessEnabled(true)
        );

        String shopSku = "U-238";
        ShopSkuKey serviceKey = new ShopSkuKey(serviceId, shopSku);
        ShopSkuKey businessKey = new ShopSkuKey(businessId, shopSku);
        sskuExistenceRepository.markExistence(serviceKey, true);

        long mskuId = 123;
        long anotherMskuId = 125;
        int categoryId = 124;

        MappingCacheDao mappingByService = new MappingCacheDao()
            .setShopSkuKey(serviceKey)
            .setMskuId(mskuId)
            .setCategoryId(categoryId)
            .setVersionTimestamp(Instant.parse("2007-12-03T10:15:30.00Z"));
        MappingCacheDao mappingByBusiness = new MappingCacheDao()
            .setShopSkuKey(businessKey)
            .setMskuId(anotherMskuId)
            .setCategoryId(categoryId)
            .setVersionTimestamp(Instant.parse("2017-12-03T10:15:30.00Z"));
        mappingsCacheRepository.insertOrUpdateAll(List.of(mappingByService, mappingByBusiness));

        // when
        List<MappingCacheDao> mappings = mdmBestMappingsProvider.loadBestMappingsForMskus(List.of(mskuId),
            ExpectedMappingQuality.ANY);

        // then
        Assertions.assertThat(mappings).isEmpty();
    }
}

