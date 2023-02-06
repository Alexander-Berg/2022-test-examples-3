package ru.yandex.autotests.market.bidding.failover.tests;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.bidding.rules.BiddingFailoverTestsRule;
import ru.yandex.autotests.market.bidding.steps.BiddingNodeSteps;
import ru.yandex.autotests.market.bidding.steps.BiddingSteps;
import ru.yandex.autotests.market.bidding.steps.BidsSenderSteps;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.Collection;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.market.bidding.client.config.BiddingProperties.PROPS;
import static ru.yandex.autotests.market.bidding.steps.BidsSenderSteps.waitFor;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.ShopDescription.READ_WRITE_WITH_OFFER_IDS;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.getShopId;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.getShopIdWikiPageUrl;

/**
 * User: alkedr
 * Date: 15.10.2014
 */
@Aqua.Test(title = "Переключение лидера под нагрузкой")
@Features("Переключение лидера под нагрузкой")
@Issue("TESTMARKET-1570")
@RunWith(Parameterized.class)
public class LeaderSwitchOnBalancerUnderLoadTest {
    private static final int THREADS_COUNT = 5;

    @Parameter private final String shopIdWikiPage = getShopIdWikiPageUrl();
    @Parameter private final long shopId = getShopId(READ_WRITE_WITH_OFFER_IDS);

    @Rule
    public final BiddingFailoverTestsRule bidding = new BiddingFailoverTestsRule();
    private final BidsSenderSteps bidsSender = new BidsSenderSteps(PROPS.getBalancerHost(), PROPS.getBalancerPort(), shopId, THREADS_COUNT);

    private final LeaderSwitcher leaderSwitcher;

    public LeaderSwitchOnBalancerUnderLoadTest(String testCaseName, LeaderSwitcher leaderSwitcher) {
        this.leaderSwitcher = leaderSwitcher;
    }

    @Parameterized.Parameters(name = "способ переключения: {0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "остановка лидера",
                        new LeaderSwitcher() {
                            @Override
                            public void switchLeader(BiddingSteps bidding) {
                                BiddingNodeSteps oldLeader = bidding.leader();
                                oldLeader.admin.stop();
                                bidding.waitForSomeNodeToBecomeLeader();
                                bidding.currentLeaderShouldBeDifferentFrom(oldLeader);
                            }
                        }
                },
                {
                        "дёргание ручки /leader-election-stop",
                        new LeaderSwitcher() {
                            @Override
                            public void switchLeader(BiddingSteps bidding) {
                                BiddingNodeSteps oldLeader = bidding.leader();
                                oldLeader.failover.stopLeaderElection();
                                oldLeader.waitForLeaderFlagToBe(false);
                                bidding.waitForSomeNodeToBecomeLeader();
                                bidding.currentLeaderShouldBeDifferentFrom(oldLeader);
                            }
                        }
                },
                {
                        "закрытие порта базы данных",
                        new LeaderSwitcher() {
                            @Override
                            public void switchLeader(BiddingSteps bidding) {
                                BiddingNodeSteps oldLeader = bidding.leader();
                                oldLeader.admin.closeDatabasePort();
                                oldLeader.waitForLeaderFlagToBe(false);
                                bidding.waitForSomeNodeToBecomeLeader();
                                bidding.currentLeaderShouldBeDifferentFrom(oldLeader);
                            }
                        }
                },
        });
    }

    @Test
    public void leaderSwitchUnderLoad() {
        bidding.database.offerBids(shopId).clear();

        waitFor(1, MINUTES);  // ждём пока балансер разбирается кто лидер

        bidsSender.startSendingBids();
        waitFor(5, SECONDS);

        leaderSwitcher.switchLeader(bidding);

        waitFor(5, SECONDS);
        bidsSender.stopSendingBids();

        assumeThat("Ни один запрос не прошёл успешно", bidsSender.getOfferIdsOfSuccesfulRequests(), not(empty()));

        bidding.database.offerBids(shopId).shouldContainOfferIds(bidsSender.getOfferIdsOfSuccesfulRequests());
    }


    private interface LeaderSwitcher {
        void switchLeader(BiddingSteps bidding);
    }
}
