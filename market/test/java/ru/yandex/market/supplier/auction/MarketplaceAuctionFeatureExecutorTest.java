package ru.yandex.market.supplier.auction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.verifyNoInteractions;

/**
 * В контексте джобы нулевой баланс считается отрицательным. Отрицательный баланс может быть достигнут в течении дня,
 * но в конце дня мы его прощаем и ставим 0 {@link MarketplaceAuctionFeatureExecutor}.
 */
public class MarketplaceAuctionFeatureExecutorTest extends FunctionalTest {

    @Autowired
    MarketplaceAuctionFeatureExecutor marketplaceAuctionFeatureExecutor;

    @AfterEach
    void tearDown() {
        verifyNoInteractions(partnerNotificationClient);
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "MarketplaceAuctionFeatureExecutorTest.supplier.csv",
                    "MarketplaceAuctionFeatureExecutorTest.negativeBalance.csv",
            },
            after = {
                    "MarketplaceAuctionFeatureExecutorTest.whenNegativeBalanceNoFeature.after.csv",
                    "MarketplaceAuctionFeatureExecutorTest.featureSuccess.csv",
            }
    )
    void whenNegativeBalanceNoFeature() {
        marketplaceAuctionFeatureExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "MarketplaceAuctionFeatureExecutorTest.supplier_netting.csv",
                    "MarketplaceAuctionFeatureExecutorTest.negativeBalance_netting.csv",
            },
            after = {
                    "MarketplaceAuctionFeatureExecutorTest.whenNegativeBalanceChangedToSuccessForNetting.after.csv",
            }
    )
    void whenNegativeBalanceChangedToSuccessForNetting() {
        marketplaceAuctionFeatureExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "MarketplaceAuctionFeatureExecutorTest.supplier.csv",
                    "MarketplaceAuctionFeatureExecutorTest.positiveBalance.csv",
            },
            after = {
                    "MarketplaceAuctionFeatureExecutorTest.whenPositiveBalanceNoFeature.after.csv",
                    "MarketplaceAuctionFeatureExecutorTest.featureSuccess.csv",
            }
    )
    void whenPositiveBalanceNoFeature() {
        marketplaceAuctionFeatureExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "MarketplaceAuctionFeatureExecutorTest.supplier.csv",
                    "MarketplaceAuctionFeatureExecutorTest.featureSuccess.csv",
                    "MarketplaceAuctionFeatureExecutorTest.positiveBalance.csv",
            },
            after = {
                    "MarketplaceAuctionFeatureExecutorTest.whenPositiveBalanceFeatureSuccessTest.after.csv",
                    "MarketplaceAuctionFeatureExecutorTest.featureSuccess.csv",
            }
    )
    void whenPositiveBalanceFeatureSuccessTest() {
        marketplaceAuctionFeatureExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "MarketplaceAuctionFeatureExecutorTest.supplier.csv",
                    "MarketplaceAuctionFeatureExecutorTest.featureSuccess.csv",
                    "MarketplaceAuctionFeatureExecutorTest.negativeBalance.csv",
            },
            after = {
                    "MarketplaceAuctionFeatureExecutorTest.whenNegativeBalanceFeatureSuccessTest.after.csv",
                    "MarketplaceAuctionFeatureExecutorTest.featureSuccess.csv",
            }
    )
    void whenNegativeBalanceFeatureSuccessTest() {
        marketplaceAuctionFeatureExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "MarketplaceAuctionFeatureExecutorTest.supplier.csv",
                    "MarketplaceAuctionFeatureExecutorTest.featureSuccess.csv",
                    "MarketplaceAuctionFeatureExecutorTest.negativeBalance.csv",
                    "MarketplaceAuctionFeatureExecutorTest.netting_transition.csv"
            },
            after = {
                    "MarketplaceAuctionFeatureExecutorTest.featureSuccess.csv",
            }
    )
    void whenNewPartnerNegativeBalanceFeatureSuccessTest() {
        marketplaceAuctionFeatureExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "MarketplaceAuctionFeatureExecutorTest.supplier.csv",
                    "MarketplaceAuctionFeatureExecutorTest.featureFail.csv",
                    "MarketplaceAuctionFeatureExecutorTest.positiveBalance.csv",
            },
            after = {
                    "MarketplaceAuctionFeatureExecutorTest.whenPositiveBalanceFeatureFailTest.after.csv",
                    "MarketplaceAuctionFeatureExecutorTest.featureSuccess.csv",
            }
    )
    void whenPositiveBalanceFeatureFailTest() {
        marketplaceAuctionFeatureExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "MarketplaceAuctionFeatureExecutorTest.dbsShop.csv",
                    "MarketplaceAuctionFeatureExecutorTest.featureFail.csv",
                    "MarketplaceAuctionFeatureExecutorTest.positiveBalance.csv",
            },
            after = {
                    "MarketplaceAuctionFeatureExecutorTest.whenPositiveBalanceFeatureFailTest.after.csv",
                    "MarketplaceAuctionFeatureExecutorTest.featureSuccess.csv",
            }
    )
    void whenPositiveBalanceFeatureFailDbsTest() {
        marketplaceAuctionFeatureExecutor.doJob(null);
    }
}
