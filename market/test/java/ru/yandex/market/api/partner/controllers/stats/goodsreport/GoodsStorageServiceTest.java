package ru.yandex.market.api.partner.controllers.stats.goodsreport;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.controllers.stats.goodsreport.model.IncludedStorageDTO;
import ru.yandex.market.api.partner.controllers.stats.goodsreport.model.IncludedStorageTypeDTO;
import ru.yandex.market.api.partner.controllers.stats.goodsreport.model.StorageDTO;
import ru.yandex.market.api.partner.controllers.stats.goodsreport.model.StorageTypeDTO;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.fulfillment.billing.storage.dao.SkuStorageInfoDao;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для {@link GoodsStorageService}.
 */
@DbUnitDataSet(before = "GoodsReportStorageServiceTest.before.csv")
class GoodsStorageServiceTest extends FunctionalTest {

    private static final long SUPPLIER_ID = 150L;

    private static final Set<String> SHOP_SKUS = Set.of("VP59136-3SK", "other_sku", "SPG333-111SK");

    @Autowired
    private SkuStorageInfoDao skuStorageInfoDao;

    private GoodsStorageService goodsStorageService;

    @BeforeEach
    void setUp() {
        goodsStorageService = new GoodsStorageService(skuStorageInfoDao, getTestClock());
    }

    @Test
    void getStorageInfo() {
        Map<String, List<StorageDTO>> storageTypes = goodsStorageService.getStorageTypes(SUPPLIER_ID, SHOP_SKUS);
        assertThat(storageTypes.keySet()).hasSize(3);
        assertThat(storageTypes.get("VP59136-3SK")).hasSize(2);

        List<StorageDTO> allTypesList = storageTypes.get("VP59136-3SK");

        assertThat(allTypesList.get(0).getStorageTypeDTO()).isEqualTo(StorageTypeDTO.FREE);
        assertThat(allTypesList.get(0).getCount()).isEqualTo(35);

        assertThat(allTypesList.get(1).getStorageTypeDTO()).isEqualTo(StorageTypeDTO.PAID);
        assertThat(allTypesList.get(1).getCount()).isEqualTo(136);

        List<IncludedStorageDTO> freeInclusions = allTypesList.get(0).getIncludedTypes();

        assertThat(freeInclusions).isNotNull();
        assertThat(freeInclusions.get(0).getIncludedStorageTypeDTO()).isEqualTo(IncludedStorageTypeDTO.FREE_EXPIRE);
        assertThat(freeInclusions.get(0).getCount()).isEqualTo(26);

        List<IncludedStorageDTO> paidInclusions = allTypesList.get(1).getIncludedTypes();

        assertThat(paidInclusions).isNotNull();

        assertThat(paidInclusions.get(0).getIncludedStorageTypeDTO()).isEqualTo(IncludedStorageTypeDTO.PAID_EXPIRE);
        assertThat(paidInclusions.get(0).getCount()).isEqualTo(97);

        assertThat(paidInclusions.get(1).getIncludedStorageTypeDTO()).isEqualTo(IncludedStorageTypeDTO.PAID_EXTRA);
        assertThat(paidInclusions.get(1).getCount()).isEqualTo(21);

        assertThat(storageTypes.get("other_sku")).isEmpty();

        assertThat(storageTypes.get("SPG333-111SK")).hasSize(2);
        List<StorageDTO> twoTypesList = storageTypes.get("SPG333-111SK");
        assertThat(twoTypesList.get(0).getStorageTypeDTO()).isEqualTo(StorageTypeDTO.FREE);
        assertThat(twoTypesList.get(0).getCount()).isEqualTo(10);

        assertThat(twoTypesList.get(1).getStorageTypeDTO()).isEqualTo(StorageTypeDTO.PAID);
        assertThat(twoTypesList.get(1).getCount()).isEqualTo(50);

        assertThat(twoTypesList.get(0).getIncludedTypes()).isNull();
        assertThat(twoTypesList.get(1).getIncludedTypes()).isNull();

    }

    private Clock getTestClock() {
        Instant instant = LocalDateTime.of(LocalDate.of(2020, Month.JUNE, 2), LocalTime.NOON)
                .atOffset(ZoneOffset.UTC)
                .toInstant();
        return Clock.fixed(instant, ZoneOffset.UTC);
    }
}
