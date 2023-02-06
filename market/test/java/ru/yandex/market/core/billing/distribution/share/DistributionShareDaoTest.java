package ru.yandex.market.core.billing.distribution.share;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

/**
 * Тесты для {@link DistributionShareDao}.
 *
 * @author vbudnev
 */
@DbUnitDataSet(before = "db/DistributionShareDaoTest.before.csv")
class DistributionShareDaoTest extends FunctionalTest {

    private static final BigDecimal AMOUNT_10 = BigDecimal.TEN;
    private static final BigDecimal AMOUNT_1 = BigDecimal.ONE;
    private static final BigDecimal TARIFF_RATE_10 = BigDecimal.valueOf(0.1);
    private static final BigDecimal TARIFF_RATE_1 = BigDecimal.valueOf(0.01);
    private static final long CATEGORY_ID_6000 = 6000L;
    private static final LocalDateTime BILLING_TIME = LocalDateTime.of(2018, 1, 1, 20, 10, 10);
    private static final LocalDateTime CREATION_TIME = LocalDateTime.of(2018, 1, 1, 11, 22, 33);
    private static final LocalDateTime APPROVAL_TIME = BILLING_TIME.plusDays(14);
    private static final int COUNT_2 = 2;
    private static final BigDecimal PRICE_5_4 = new BigDecimal("5.4");
    private static final int INITIAL_PRICE_54 = 54;
    private static final long CLID_100 = 100;
    private static final String VID_100 = "vid100";
    private static final long DISTR_TYPE_1 = 1;
    private static final long MSKU_1 = 1;
    @Autowired
    private DistributionShareDao distributionShareDao;

    @DbUnitDataSet(after = "db/DistributionShareDaoTest.after.csv")
    @Test
    void test_persist() {
        distributionShareDao.persist(
                List.of(
                        ShareItemData.builder()
                                .setOrderId(1L)
                                .setFeedId(200L)
                                .setOfferId("offer_id2")
                                .setItemId(2L)
                                .setAmount(AMOUNT_1)
                                .setRawAmount(AMOUNT_10)
                                .setApprovalTime(APPROVAL_TIME)
                                .setBillingTime(BILLING_TIME)
                                .setCategoryId(CATEGORY_ID_6000)
                                .setClid(CLID_100)
                                .setItemsCount(COUNT_2)
                                .setPrice(PRICE_5_4)
                                .setInitialPrice(INITIAL_PRICE_54)
                                .setTariffRate(TARIFF_RATE_10)
                                .setRawTariffRate(TARIFF_RATE_10)
                                .setVid(VID_100)
                                .setDistrType(DISTR_TYPE_1)
                                .setMsku(MSKU_1)
                                .setCreationTime(CREATION_TIME)
                                .setIsFirstOrder(true)
                                .setFraud(false)
                                .setAdditionalInfo(List.of())
                                .setTariffName(DistributionTariffName.CEHAC)
                                .build(),
                        ShareItemData.builder()
                                .setOrderId(2L)
                                .setFeedId(300L)
                                .setOfferId("offer_id3")
                                .setItemId(3L)
                                .setAmount(AMOUNT_10)
                                .setRawAmount(AMOUNT_10)
                                .setApprovalTime(APPROVAL_TIME)
                                .setBillingTime(BILLING_TIME)
                                .setCategoryId(CATEGORY_ID_6000)
                                .setClid(CLID_100)
                                .setItemsCount(COUNT_2)
                                .setPrice(PRICE_5_4)
                                .setInitialPrice(INITIAL_PRICE_54)
                                .setTariffRate(TARIFF_RATE_1)
                                .setRawTariffRate(TARIFF_RATE_1)
                                .setVid(VID_100)
                                .setDistrType(DISTR_TYPE_1)
                                .setMsku(MSKU_1)
                                .setCreationTime(CREATION_TIME)
                                .setIsFirstOrder(false)
                                .setFraud(false)
                                .setAdditionalInfo(List.of(DistributionShareAdditionalInfo.OVER_LIMIT))
                                .setTariffName(DistributionTariffName.DIY)
                                .build(),
                        ShareItemData.builder()
                                .setOrderId(2L)
                                .setFeedId(300L)
                                .setOfferId("offer_id4")
                                .setItemId(4L)
                                .setAmount(AMOUNT_10)
                                .setRawAmount(AMOUNT_10)
                                .setApprovalTime(APPROVAL_TIME)
                                .setBillingTime(BILLING_TIME)
                                .setCategoryId(CATEGORY_ID_6000)
                                .setClid(CLID_100)
                                .setItemsCount(COUNT_2)
                                .setPrice(PRICE_5_4)
                                .setInitialPrice(INITIAL_PRICE_54)
                                .setTariffRate(TARIFF_RATE_10)
                                .setRawTariffRate(TARIFF_RATE_10)
                                .setVid(VID_100)
                                .setDistrType(DISTR_TYPE_1)
                                .setMsku(MSKU_1)
                                .setCreationTime(CREATION_TIME)
                                .setIsFirstOrder(true)
                                .setFraud(true)
                                .setAdditionalInfo(List.of(DistributionShareAdditionalInfo.FRAUD))
                                .setTariffName(DistributionTariffName.FASHION)
                                .build(),
                        ShareItemData.builder()
                                .setOrderId(2L)
                                .setFeedId(300L)
                                .setOfferId("offer_id5")
                                .setItemId(5L)
                                .setAmount(AMOUNT_10)
                                .setRawAmount(AMOUNT_10)
                                .setApprovalTime(APPROVAL_TIME)
                                .setBillingTime(BILLING_TIME)
                                .setCategoryId(CATEGORY_ID_6000)
                                .setClid(CLID_100)
                                .setItemsCount(COUNT_2)
                                .setPrice(PRICE_5_4)
                                .setInitialPrice(INITIAL_PRICE_54)
                                .setTariffRate(TARIFF_RATE_10)
                                .setRawTariffRate(TARIFF_RATE_10)
                                .setVid(VID_100)
                                .setDistrType(DISTR_TYPE_1)
                                .setMsku(MSKU_1)
                                .setCreationTime(CREATION_TIME)
                                .setIsFirstOrder(true)
                                .setFraud(true)
                                .setAdditionalInfo(List.of(
                                        DistributionShareAdditionalInfo.FRAUD,
                                        DistributionShareAdditionalInfo.OVER_LIMIT
                                ))
                                .setTariffName(DistributionTariffName.FMCG)
                                .build(),
                        ShareItemData.builder()
                                .setOrderId(2L)
                                .setFeedId(300L)
                                .setOfferId("offer_id6")
                                .setItemId(6L)
                                .setAmount(AMOUNT_10)
                                .setRawAmount(AMOUNT_10)
                                .setApprovalTime(APPROVAL_TIME)
                                .setBillingTime(BILLING_TIME)
                                .setCategoryId(CATEGORY_ID_6000)
                                .setClid(CLID_100)
                                .setItemsCount(COUNT_2)
                                .setPrice(PRICE_5_4)
                                .setInitialPrice(INITIAL_PRICE_54)
                                .setTariffRate(TARIFF_RATE_10)
                                .setRawTariffRate(TARIFF_RATE_10)
                                .setVid(VID_100)
                                .setDistrType(DISTR_TYPE_1)
                                .setMsku(MSKU_1)
                                .setCreationTime(CREATION_TIME)
                                .setIsFirstOrder(true)
                                .setFraud(true)
                                .setAdditionalInfo(List.of(
                                        DistributionShareAdditionalInfo.FRAUD,
                                        DistributionShareAdditionalInfo.OVER_LIMIT,
                                        DistributionShareAdditionalInfo.UNDER_MINIMUM_PAYOUT_LIMIT
                                ))
                                .setTariffName(DistributionTariffName.KIDS)
                                .build()
                )
        );
    }
}
