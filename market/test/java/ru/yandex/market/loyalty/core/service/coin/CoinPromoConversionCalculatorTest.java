package ru.yandex.market.loyalty.core.service.coin;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.dao.ConversionStatistics;
import ru.yandex.market.loyalty.core.dao.DataVersionDao;
import ru.yandex.market.loyalty.core.dao.MskuAttributesDao;
import ru.yandex.market.loyalty.core.exception.StatisticsNotCompleteException;
import ru.yandex.market.loyalty.core.exception.TableNotLoadedYetException;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.DataVersion;
import ru.yandex.market.loyalty.core.model.MskuAttributesRecord;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinType;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.FIRST_CHILD_CATEGORY_ID;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.PARENT_CATEGORY_ID;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.SECOND_CHILD_CATEGORY_ID;

public class CoinPromoConversionCalculatorTest extends MarketLoyaltyCoreMockedDbTestBase {
    public static final long FIRST_HID = 16089018;
    public static final long SECOND_HID = 12312131;
    public static final long THIRD_HID = 879465459;
    @Autowired
    private CoinPromoConversionCalculator coinPromoConversionCalculator;
    @Autowired
    private MskuAttributesDao mskuAttributesDao;
    @Autowired
    private DataVersionDao dataVersionDao;

    @Test
    public void testModel1() {
        double v = coinPromoConversionCalculator.calculateEstimatedConversion(
                CoreCoinType.FIXED,
                BigDecimal.valueOf(100),
                BigDecimal.ZERO,
                null,
                BigDecimal.valueOf(31), new ConversionStatistics(0.1d, 0.1d, 3000d)
        );

        assertEquals(0.133360972327002, v, 0.01);
    }

    @Test
    public void testModel2() {
        double v = coinPromoConversionCalculator.calculateEstimatedConversion(
                CoreCoinType.FIXED,
                BigDecimal.valueOf(300),
                BigDecimal.ZERO,
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(31), new ConversionStatistics(0.1d, 0.01d, 3000d)
        );

        assertEquals(0.0696608500671727d, v, 0.01);
    }

    @Test
    public void testModel3() {
        double v = coinPromoConversionCalculator.calculateEstimatedConversion(
                CoreCoinType.PERCENT,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                null,
                BigDecimal.valueOf(31), new ConversionStatistics(0.001d, 0.1d, 3000d)
        );

        assertEquals(0.0178157292936469d, v, 0.01);
    }

    @Test
    public void testModel4() {
        double v = coinPromoConversionCalculator.calculateEstimatedConversion(
                CoreCoinType.PERCENT,
                BigDecimal.valueOf(25),
                BigDecimal.ZERO,
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(31), new ConversionStatistics(0.0001d, 0.0001d, 3000d)
        );

        assertEquals(0.760106907696935f, v, 0.01);
    }

    @Test
    public void testModel5() {
        double v = coinPromoConversionCalculator.calculateEstimatedConversion(
                CoreCoinType.FREE_DELIVERY,
                null,
                BigDecimal.ZERO,
                null,
                BigDecimal.valueOf(31), new ConversionStatistics(0d, 0.8d, 2500d)
        );

        assertEquals(0.153817019726567f, v, 0.01);
    }

    @Test
    public void testTraverseCategoryTree() {
        Set<Long> leafHids = coinPromoConversionCalculator.getAllHids(Collections.singleton(PARENT_CATEGORY_ID));
        assertEquals(
                Stream.of(
                        PARENT_CATEGORY_ID, FIRST_CHILD_CATEGORY_ID, SECOND_CHILD_CATEGORY_ID
                ).map(Integer::longValue).collect(Collectors.toSet()),
                leafHids
        );
    }

    @Test
    public void testConversionStatisticsNominalLessThanAverageOfferPrice() throws TableNotLoadedYetException,
            StatisticsNotCompleteException {
        long dataVersionNum = dataVersionDao.createDataVersionNum();
        dataVersionDao.saveDataVersion(DataVersion.MSKU_ATTRIBUTES, dataVersionNum);
        mskuAttributesDao.saveMskuAttributesRecords(
                dataVersionNum,
                ImmutableSet.of(
                        new MskuAttributesRecord(
                                FIRST_HID, 0L, CoreMarketPlatform.BLUE, 1L, BigDecimal.valueOf(100),
                                BigDecimal.valueOf(100),
                                "" + FIRST_HID + "_1"
                        ),
                        new MskuAttributesRecord(
                                FIRST_HID, 0L, CoreMarketPlatform.BLUE, 1L, BigDecimal.valueOf(100),
                                BigDecimal.valueOf(100),
                                "" + FIRST_HID + "_2"
                        ),
                        new MskuAttributesRecord(
                                FIRST_HID, 0L, CoreMarketPlatform.BLUE, 1L, BigDecimal.valueOf(100),
                                BigDecimal.valueOf(100),
                                "" + FIRST_HID + "_3"
                        ),
                        new MskuAttributesRecord(
                                SECOND_HID, 0L, CoreMarketPlatform.BLUE, 1L, BigDecimal.valueOf(100),
                                BigDecimal.valueOf(100),
                                "" + SECOND_HID + "_1"
                        ),
                        new MskuAttributesRecord(
                                SECOND_HID, 0L, CoreMarketPlatform.BLUE, 1L, BigDecimal.valueOf(100),
                                BigDecimal.valueOf(100),
                                "" + SECOND_HID + "_2"
                        ),
                        new MskuAttributesRecord(
                                SECOND_HID, 0L, CoreMarketPlatform.BLUE, 1L, BigDecimal.valueOf(100),
                                BigDecimal.valueOf(100),
                                "" + SECOND_HID + "_3"
                        ),
                        new MskuAttributesRecord(
                                THIRD_HID, 0L, CoreMarketPlatform.BLUE, 1L, BigDecimal.valueOf(100),
                                BigDecimal.valueOf(100),
                                "" + THIRD_HID + "_1"
                        ),
                        new MskuAttributesRecord(
                                THIRD_HID, 0L, CoreMarketPlatform.BLUE, 1L, BigDecimal.valueOf(100),
                                BigDecimal.valueOf(100),
                                "" + THIRD_HID + "_2"
                        ),
                        new MskuAttributesRecord(
                                THIRD_HID, 0L, CoreMarketPlatform.BLUE, 1L, BigDecimal.valueOf(100),
                                BigDecimal.valueOf(100),
                                "" + THIRD_HID + "_3"
                        )
                )
        );


        ConversionStatistics cs = coinPromoConversionCalculator.calculateConversionStatistics(
                BigDecimal.TEN,
                ImmutableSet.of((int) FIRST_HID),
                ImmutableSet.of(),
                ImmutableSet.of()
        );

        assertThat(cs, allOf(
                hasProperty("coverage", equalTo(0.34)),
                hasProperty("relativeValue", equalTo(0.1))
        ));
    }

    @Test(expected = StatisticsNotCompleteException.class)
    public void testConversionStatisticsWhenNoOffersFound() throws TableNotLoadedYetException,
            StatisticsNotCompleteException {
        long dataVersionNum = dataVersionDao.createDataVersionNum();
        dataVersionDao.saveDataVersion(DataVersion.MSKU_ATTRIBUTES, dataVersionNum);

        coinPromoConversionCalculator.calculateConversionStatistics(
                BigDecimal.TEN,
                ImmutableSet.of((int) FIRST_HID),
                ImmutableSet.of(),
                ImmutableSet.of()
        );
    }

    @Test
    public void testConversionStatisticsNominalGreaterAverageOfferPrice() throws TableNotLoadedYetException,
            StatisticsNotCompleteException {
        long dataVersionNum = dataVersionDao.createDataVersionNum();
        dataVersionDao.saveDataVersion(DataVersion.MSKU_ATTRIBUTES, dataVersionNum);
        mskuAttributesDao.saveMskuAttributesRecords(
                dataVersionNum,
                ImmutableSet.of(
                        new MskuAttributesRecord(
                                FIRST_HID, 0L, CoreMarketPlatform.BLUE, 1L, BigDecimal.valueOf(100),
                                BigDecimal.valueOf(100),
                                "" + FIRST_HID + "_1"
                        ),
                        new MskuAttributesRecord(
                                FIRST_HID, 0L, CoreMarketPlatform.BLUE, 1L, BigDecimal.valueOf(100),
                                BigDecimal.valueOf(100),
                                "" + FIRST_HID + "_2"
                        ),
                        new MskuAttributesRecord(
                                FIRST_HID, 0L, CoreMarketPlatform.BLUE, 1L, BigDecimal.valueOf(100), BigDecimal.valueOf(100),
                                "" + FIRST_HID + "_3"
                        )
                )
        );


        ConversionStatistics cs = coinPromoConversionCalculator.calculateConversionStatistics(
                BigDecimal.valueOf(400),
                ImmutableSet.of((int) FIRST_HID),
                ImmutableSet.of(),
                ImmutableSet.of()
        );

        assertThat(cs, allOf(
                hasProperty("coverage", equalTo(1.0)),
                hasProperty("relativeValue", equalTo(1.0))
        ));
    }
}
