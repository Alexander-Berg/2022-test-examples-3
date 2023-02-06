package ru.yandex.market.pricelabs.tms.programs;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.pricelabs.model.AutostrategyState;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.recommendation.PriceRecommendation;
import ru.yandex.market.pricelabs.model.types.ShopType;
import ru.yandex.market.pricelabs.processing.ShopArg;
import ru.yandex.market.pricelabs.processing.monetization.model.ActionEnum;
import ru.yandex.market.pricelabs.processing.monetization.model.AdvCampaign;
import ru.yandex.market.pricelabs.tms.processing.TaskInfo;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class PriceRecommendationsProcessingTest extends AbstractProgramsTmsTest {

    @DisplayName("Проверяет, что во время большого цикла, подтягиваются рекомендации по цене и кол-во проблемных " +
            "офферов проставляется в таблицах кампаний и АС")
    @Test
    void testPriceRecommendationTakenIntoAccountWhileFullLoop() {

        int campaignId1 = 1001;
        int campaignId2 = 1002;

        String offerId1 = "111";
        String offerId2 = "112";
        String offerId3 = "113";
        String offerId4 = "114";

        Instant now = Instant.now();
        executors.priceRecommendations().clearTargetTable();
        executors.priceRecommendations().insert(List.of(
                new PriceRecommendation(SHOP_ID, offerId1, 0, 0.06, 0.11, 0.21, now, now),
                new PriceRecommendation(SHOP_ID, offerId2, 0, 0.07, 0.12, 0.22, now, now),
                new PriceRecommendation(SHOP_ID, offerId4, 0, 0.09, 0.14, 0.24, now, now),
                new PriceRecommendation(SHOP_ID, "444444", 1, 0.1, 0.15, 0.25, now, now)
        ));

        // 4 оффера
        List<Offer> offers = getOffersWhite1To1CategoryMapping();

        executor.insert(offers, o -> o.setPrice(10000));

        // 2 стандартных АС с фильтрами настроенными на все офферы
        filterExectuor.insert(List.of(
                getFilter(23, List.of(offerId1, offerId2, offerId3, offerId4)),
                getFilter(24, List.of(offerId1, offerId2, offerId3, offerId4))
        ));
        int autoId1 = 100; // вот эта выбирается
        int autoId2 = 200;
        autostrategyExecutor.insert(List.of(
                getAutostrategy(autoId1, SHOP_ID, " пользовательская1", 2, 23),
                getAutostrategy(autoId2, SHOP_ID, " пользовательская2", 1, 24)
        ));

        // две активных кампании монетизации
        AdvCampaign advCampaign = advCampaign(SHOP_ID, campaignId1, 50, true);
        AdvCampaign advCampaign1 = advCampaign(SHOP_ID, campaignId2, 30, true);
        AdvCampaign advCampaign2 = advCampaign(SHOP_ID, 3000, 40, false);
        advCampaignYtScenarioExecutor.insert(List.of(advCampaign, advCampaign1, advCampaign2));
        advCampaignHistoryYtScenarioExecutor.insert(List.of(
                advCampaignHistory(advCampaign, ActionEnum.CREATED),
                advCampaignHistory(advCampaign1, ActionEnum.CREATED),
                advCampaignHistory(advCampaign2, ActionEnum.REMOVED)
        ));

        // ставки только на офферы в этих кампаниях
        advOfferBidsYtScenarioExecutor.insert(List.of(
                advOfferBid(SHOP_ID, campaignId1, offerId1, 100),
                advOfferBid(SHOP_ID, campaignId1, offerId2, 200),
                advOfferBid(SHOP_ID, campaignId2, offerId1, 300),
                advOfferBid(SHOP_ID, 111111, offerId1, 400),
                advOfferBid(SHOP_ID, 222222, offerId1, 500)
        ));

        testControls.resetShops();
        var newShop = initShop(SHOP_ID, s -> s.setFeeds(Set.of((long) FEED_ID)));
        testControls.saveShop(newShop);

        processorRouter.syncAutostrategiesWhite(
                new ShopArg()
                        .setShopId(SHOP_ID)
                        .setType(ShopType.DSBS),
                new TaskInfo(getInstant(), taskWriter)
        );

        printTables();

        List<AdvCampaign> campaigns =
                advCampaignYtScenarioExecutor.selectTargetRows()
                        .stream()
                        .sorted(Comparator.comparingLong(AdvCampaign::getId)).collect(Collectors.toList());

        for (AdvCampaign cmp : campaigns) {
            if (cmp.getId() == campaignId1) {
                Assertions.assertEquals(2, cmp.getRec_count());
            } else {
                Assertions.assertEquals(0, cmp.getRec_count());
            }
        }

        List<AutostrategyState> states = executors.autostrategiesStateWhite().selectTargetRows();

        for (AutostrategyState state : states) {
            if (state.getAutostrategy_id() == autoId1) {
                Assertions.assertEquals(1, state.getRes_count());
            } else {
                Assertions.assertEquals(0, state.getRes_count());
            }
        }

    }
}
