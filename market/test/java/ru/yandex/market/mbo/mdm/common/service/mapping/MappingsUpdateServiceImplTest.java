package ru.yandex.market.mbo.mdm.common.service.mapping;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao.initFrom;

public class MappingsUpdateServiceImplTest extends MdmBaseDbTestClass {

    private static final int CATEGORY_ID = 1;

    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private TransactionHelper transactionHelper;

    private MappingsUpdateService sut;
    private Random random;

    @Before
    public void setup() {
        random = new Random(243225L);
        sut = new MappingsUpdateServiceImpl(mappingsCacheRepository, transactionHelper);
    }

    @Test
    public void shouldUpdateMappings() {
        // given
        ShopSkuKey ssku = testShopSskuKey();
        MappingCacheDao existedMapping = mappingCacheDao(ssku, 15);
        mappingsCacheRepository.insert(existedMapping);
        MappingCacheDao updatedMapping = mappingCacheDao(ssku, 16);
        var update = new UpdatedMappingInfo(updatedMapping);

        // when
        sut.processUpdates(List.of(update));

        // then
        MappingCacheDao savedMapping = mappingsCacheRepository.findById(ssku);
        assertThat(savedMapping).isEqualTo(updatedMapping);
    }

    @Test
    public void shouldCreateMappings() {
        // given
        ShopSkuKey ssku = testShopSskuKey();
        MappingCacheDao newMapping = mappingCacheDao(ssku, 17);
        var update = new UpdatedMappingInfo(newMapping);

        // when
        sut.processUpdates(List.of(update));

        // then
        MappingCacheDao savedMapping = mappingsCacheRepository.findById(ssku);
        assertThat(savedMapping).isEqualTo(newMapping);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void shouldDeleteMappings() {
        // given
        ShopSkuKey ssku = testShopSskuKey();
        MappingCacheDao existedMapping = mappingCacheDao(ssku, 15);
        mappingsCacheRepository.insert(existedMapping);
        MappingCacheDao updatedMapping = mappingCacheDao(ssku, 16);
        var update = new UpdatedMappingInfo(updatedMapping, true);

        // when
        sut.processUpdates(List.of(update));

        // then
        MappingCacheDao savedMapping = mappingsCacheRepository.findById(ssku);
        assertThat(savedMapping).isNull();
    }

    @Test
    public void shouldNotSendToQueueWhenNoNotifiableChanges() {
        // given
        ShopSkuKey ssku = testShopSskuKey();
        MappingCacheDao existedMapping = mappingCacheDao(ssku, 20);
        mappingsCacheRepository.insert(existedMapping);
        MappingCacheDao updatedMapping = initFrom(existedMapping);
        updatedMapping.setEoxTimestamp(Instant.now().plus(10, MINUTES));
        updatedMapping.setVersionTimestamp(Instant.now().plus(10, MINUTES));
        var update = new UpdatedMappingInfo(updatedMapping);

        // when
        sut.processUpdates(List.of(update));

        // then
        MappingCacheDao savedMapping = mappingsCacheRepository.findById(ssku);
        assertThat(savedMapping.getVersionTimestamp()).isEqualTo(updatedMapping.getVersionTimestamp());
    }

    @Test
    public void shouldDeduplicateMappings() {
        // given
        ShopSkuKey ssku1 = testShopSskuKey();
        ShopSkuKey ssku2 = testShopSskuKey();
        MappingCacheDao m11 = mappingCacheDao(ssku1, random.nextLong());
        MappingCacheDao m12 = mappingCacheDao(ssku1, random.nextLong()).setVersionTimestamp(past());
        MappingCacheDao m21 = mappingCacheDao(ssku2, random.nextLong())
            .setMappingKind(MappingCacheDao.MappingKind.SUGGESTED);
        MappingCacheDao m22 = mappingCacheDao(ssku2, random.nextLong()).setVersionTimestamp(past());
        var updates = Stream.of(m11, m12, m21, m22)
            .map(UpdatedMappingInfo::new)
            .collect(Collectors.toList());

        // when
        sut.processUpdates(updates);

        // then
        var savedMappings = mappingsCacheRepository.findAll();
        assertThat(savedMappings).containsExactlyInAnyOrder(m11, m22);
    }

    @Test
    public void shouldNotUpdateMappingsIfTsOlder() {
        // given
        ShopSkuKey ssku = testShopSskuKey();
        MappingCacheDao existedMapping = mappingCacheDao(ssku, 15);
        mappingsCacheRepository.insert(existedMapping);
        MappingCacheDao updatedMapping = mappingCacheDao(ssku, 16).setVersionTimestamp(past());
        var update = new UpdatedMappingInfo(updatedMapping);

        // when
        sut.processUpdates(List.of(update));

        // then
        MappingCacheDao savedMapping = mappingsCacheRepository.findById(ssku);
        assertThat(savedMapping).isEqualTo(existedMapping);
    }

    private ShopSkuKey testShopSskuKey() {
        return new ShopSkuKey(random.nextInt(), "test");
    }

    private static MappingCacheDao mappingCacheDao(ShopSkuKey shopSkuKey, long mskuId) {
        return new MappingCacheDao()
            .setSupplierId(shopSkuKey.getSupplierId())
            .setShopSku(shopSkuKey.getShopSku())
            .setMskuId(mskuId)
            .setCategoryId(CATEGORY_ID)
            .setVersionTimestamp(Instant.now());
    }

    private static Instant past() {
        return Instant.now().minusSeconds(100500L);
    }
}
