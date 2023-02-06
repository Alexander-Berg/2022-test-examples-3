package ru.yandex.autotests.market.partner.backend.tests.auction.category;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.partner.backend.steps.auction.category.AuctionCategorySteps;
import ru.yandex.autotests.market.partner.backend.tests.auction.AuctionPiConfig;
import ru.yandex.autotests.market.partner.backend.tests.auction.AuctionTestSynchronisation;
import ru.yandex.autotests.market.partner.backend.tests.auction.WikiAuctionPiConfig;
import ru.yandex.autotests.market.partner.backend.util.query.MarketPaymentRequestError;
import ru.yandex.autotests.market.partner.backend.util.query.auction.bids.BidReq;
import ru.yandex.autotests.market.partner.backend.util.query.auction.category.CategoryReq;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.hazelcast.LockRule;

import static ru.yandex.autotests.market.partner.backend.steps.auction.category.AuctionCategorySteps.CATEGORY_ALL_ID;

/**
 * @author vbudnev
 */
@Aqua.Test
@Feature("Страница управления ставками в ПИ")
@Stories("Категорийные ставки")
@Features("Категорийные ставки")
@Title("Управление категорийными ставками")
public class AuctionCategoryBidsManagementTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final static AuctionPiConfig config = new WikiAuctionPiConfig();
    @ClassRule
    public final static LockRule lockRule = AuctionTestSynchronisation.getCampaignBasedLockRule(config);

    /**
     * NOTE: Устанавливаем значения используя float, а получаем в виде целых.
     */
    @Title("Установка категорийных значений")
    @Test
    public void test_categoryBids_set() {
        AuctionCategorySteps.setCategoryBids(
                config.getUserId(),
                config.getCampaignId(),
                CATEGORY_ALL_ID,
                CategoryReq.BID_2_22,
                CategoryReq.CBID_3_33,
                CategoryReq.FEE_4_44
        );

        AuctionCategorySteps.verifyCategoryBids(
                config.getUserId(),
                config.getCampaignId(),
                CATEGORY_ALL_ID,
                BidReq.BID_222,
                BidReq.BID_333,
                BidReq.NOT_SET
        );
    }

    /**
     * При сбросе категорийных ставок, сам блок в ответе с категорийными ставками остается, просто значения у него пустые.
     */
    @Title("Сброс категорийных значений")
    @Test
    public void test_categoryBids_clear() {
        AuctionCategorySteps.setCategoryBids(
                config.getUserId(),
                config.getCampaignId(),
                CATEGORY_ALL_ID,
                CategoryReq.BID_0_01,
                CategoryReq.BID_0_01,
                CategoryReq.BID_0_01
        );

        AuctionCategorySteps.verifyCategoryBids(
                config.getUserId(),
                config.getCampaignId(),
                CATEGORY_ALL_ID,
                BidReq.BID_1,
                BidReq.BID_1,
                BidReq.NOT_SET
        );

        AuctionCategorySteps.clearCategoryBids(config.getUserId(), config.getCampaignId(), CATEGORY_ALL_ID);

        AuctionCategorySteps.verifyCategoryBids(
                config.getUserId(),
                config.getCampaignId(),
                CATEGORY_ALL_ID,
                BidReq.NOT_SET,
                BidReq.NOT_SET,
                BidReq.NOT_SET
        );
    }

    @Title("Сброс категорийных значений установкой в 0")
    @Test
    public void test_categoryBids_resetViaExplicit0() {
        AuctionCategorySteps.setCategoryBids(
                config.getUserId(),
                config.getCampaignId(),
                CATEGORY_ALL_ID,
                CategoryReq.BID_0_01,
                CategoryReq.BID_0_01,
                CategoryReq.BID_0_01
        );

        AuctionCategorySteps.verifyCategoryBids(
                config.getUserId(),
                config.getCampaignId(),
                CATEGORY_ALL_ID,
                BidReq.BID_1,
                BidReq.BID_1,
                BidReq.NOT_SET
        );

        AuctionCategorySteps.setCategoryBids(
                config.getUserId(),
                config.getCampaignId(),
                CATEGORY_ALL_ID,
                CategoryReq.BID_0,
                CategoryReq.BID_0,
                CategoryReq.BID_0
        );

        AuctionCategorySteps.verifyCategoryBids(
                config.getUserId(),
                config.getCampaignId(),
                CATEGORY_ALL_ID,
                BidReq.NOT_SET,
                BidReq.NOT_SET,
                BidReq.NOT_SET
        );
    }

    @Title("bid-компонента в 90 уе не принимается")
    @Test
    public void test_categoryBids_set_should_returnError_when_bidEquals90ue() {
        //does not look good in allure
        thrown.expect(MarketPaymentRequestError.class);
        thrown.expectMessage("invalid-bid-value");

        AuctionCategorySteps.setCategoryBids(
                config.getUserId(),
                config.getCampaignId(),
                CATEGORY_ALL_ID,
                CategoryReq.BID_90_00,
                CategoryReq.BID_0,
                CategoryReq.BID_0
        );
    }

}
