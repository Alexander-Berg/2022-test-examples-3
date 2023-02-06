package ru.yandex.market.mboc.common.msku;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.github.benas.randombeans.FieldDefinition;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.jooq.repo.OffsetFilter;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.BuyPromoPrice;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

/**
 * @author pochemuto@yandex-team.ru
 */
@SuppressWarnings("checkstyle:magicNumber")
public class BuyPromoPriceRepositoryTest extends BaseDbTestClass {
    private static final int SEED = 33;
    private static final EnhancedRandom RANDOM = TestDataUtils.defaultRandomBuilder(SEED)
        .randomize(new FieldDefinition<>("id", Long.class, BuyPromoPrice.class), (Supplier<Long>) () -> null)
        .build();
    private static final int TEST_DATA_COUNT = 100;

    @Autowired
    private BuyPromoPriceRepository buyPromoPriceRepository;

    @Before
    public void setup() {
    }

    @Test
    public void testFilter() {
        List<BuyPromoPrice> notInFilterData =
            RANDOM.objects(BuyPromoPrice.class, TEST_DATA_COUNT)
                .collect(Collectors.toList());
        buyPromoPriceRepository.save(notInFilterData);

        List<BuyPromoPrice> inFilterData =
            RANDOM.objects(BuyPromoPrice.class, 1)
                .peek(p -> p.setSupplierId(1))
                .peek(p -> p.setMarketSkuId(2L))
                .peek(p -> p.setToDate(DateTimeUtils.dateTimeNow().plusDays(1).withNano(0)))
                .collect(Collectors.toList());
        buyPromoPriceRepository.save(inFilterData);

        BuyPromoPriceRepository.Filter filter = new BuyPromoPriceRepository.Filter()
            .setSupplierId(1)
            .setMarketSkuId(2L)
            .setToDateAfter(DateTimeUtils.dateTimeNow().withNano(0));

        List<BuyPromoPrice> found = buyPromoPriceRepository.find(filter);

        Assertions.assertThat(found).usingElementComparatorIgnoringFields("id")
            .containsOnlyElementsOf(inFilterData);

    }

    @Test
    public void testFilterByShopSkuKeys() {
        List<BuyPromoPrice> notInFilterData =
            RANDOM.objects(BuyPromoPrice.class, TEST_DATA_COUNT)
                .collect(Collectors.toList());
        buyPromoPriceRepository.save(notInFilterData);

        List<BuyPromoPrice> inFilterData = RANDOM.objects(BuyPromoPrice.class, TEST_DATA_COUNT)
            .peek(p -> p.setSupplierId(1))
            .peek(p -> p.setToDate(DateTimeUtils.dateTimeNow().plusDays(1).withNano(0)))
            .collect(Collectors.toList());
        buyPromoPriceRepository.save(inFilterData);

        BuyPromoPriceRepository.Filter filter = new BuyPromoPriceRepository.Filter()
            .setMarketSkuKeys(inFilterData.stream().map(p ->
                new BuyPromoPriceRepository.Filter.MarketSkuKey(
                    p.getSupplierId(), p.getMarketSkuId()
                ))
                .collect(Collectors.toSet())
            )
            .setToDateAfter(DateTimeUtils.dateTimeNow().withNano(0));

        List<BuyPromoPrice> found = buyPromoPriceRepository.find(filter);

        Assertions.assertThat(found).usingElementComparatorIgnoringFields("id")
            .containsOnlyElementsOf(inFilterData);
    }

    @Test
    public void testFilterBySupplierId() {
        List<BuyPromoPrice> testDataSupplier1 =
            RANDOM.objects(BuyPromoPrice.class, TEST_DATA_COUNT)
                .peek(p -> p.setSupplierId(1))
                .collect(Collectors.toList());
        buyPromoPriceRepository.save(testDataSupplier1);

        List<BuyPromoPrice> testDataSupplier2 =
            RANDOM.objects(BuyPromoPrice.class, TEST_DATA_COUNT)
                .peek(p -> p.setSupplierId(2))
                .collect(Collectors.toList());
        buyPromoPriceRepository.save(testDataSupplier2);

        BuyPromoPriceRepository.Filter filter = new BuyPromoPriceRepository.Filter()
            .setSupplierId(1);

        List<BuyPromoPrice> found = buyPromoPriceRepository.find(filter);

        Assertions.assertThat(found).usingElementComparatorIgnoringFields("id")
            .containsOnlyElementsOf(testDataSupplier1);
    }

    @Test
    public void testSortByFromDate() {
        List<BuyPromoPrice> testData =
            RANDOM.objects(BuyPromoPrice.class, TEST_DATA_COUNT)
                .collect(Collectors.toList());
        buyPromoPriceRepository.save(testData);

        BuyPromoPriceRepository.Filter filter = new BuyPromoPriceRepository.Filter();

        List<BuyPromoPrice> sorted = buyPromoPriceRepository.find(
            filter, BuyPromoPriceRepository.SortBy.FROM_DATE.asc(), OffsetFilter.all()
        );

        Assertions.assertThat(sorted)
            .isSortedAccordingTo(Comparator.comparing(BuyPromoPrice::getFromDate));
    }
}
