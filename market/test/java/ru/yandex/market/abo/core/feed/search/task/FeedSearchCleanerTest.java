package ru.yandex.market.abo.core.feed.search.task;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.abo.core.deliverycalculator.db.DeliveryCalculatorInfoSnapshot;
import ru.yandex.market.abo.core.feed.model.DbFeedOfferDelivery;
import ru.yandex.market.abo.core.feed.model.DbFeedOfferDetails;
import ru.yandex.market.abo.core.feed.repo.DbFeedOfferDeliveryRepo;
import ru.yandex.market.abo.core.feed.repo.DbFeedOfferDetailsRepo;
import ru.yandex.market.abo.core.feed.search.task.log.FeedSearchTaskLogRepo;
import ru.yandex.market.abo.core.feed.search.task.model.FeedSearchTask;
import ru.yandex.market.abo.core.feed.search.task.model.FeedSearchTaskLog;
import ru.yandex.market.abo.core.feed.search.task.model.FeedSearchTaskSource;
import ru.yandex.market.abo.core.feed.search.task.model.SearchTaskTarget;
import ru.yandex.market.abo.core.ticket.repository.OfferDeliverySnapshotRepo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author komarovns
 * @date 25.11.2019
 */
class FeedSearchCleanerTest extends EmptyTest {
    @Autowired
    FeedSearchCleaner feedSearchCleaner;
    @Autowired
    FeedSearchTaskLogRepo feedSearchTaskLogRepo;
    @Autowired
    DbFeedOfferDetailsRepo dbFeedOfferDetailsRepo;
    @Autowired
    DbFeedOfferDeliveryRepo dbFeedOfferDeliveryRepo;
    @Autowired
    FeedSearchTaskRepo feedSearchTaskRepo;
    @Autowired
    OfferDeliverySnapshotRepo offerDeliverySnapshotRepo;

    @Test
    void deleteOldTaskTest() {
        var oldOffer = createOfferWithDelivery();
        var oldTask = createTask(FeedSearchCleaner.TASK_LIFETIME_DAYS + 1, oldOffer);
        var oldLog = createLog(oldTask);
        var oldDeliveryCalc = oldTask.getDeliveryCalculatorInfoSnapshot();

        var newOffer = createOfferWithDelivery();
        var newTask = createTask(FeedSearchCleaner.TASK_LIFETIME_DAYS - 1, newOffer);
        var newLog = createLog(newTask);
        var newDeliveryCalc = newTask.getDeliveryCalculatorInfoSnapshot();
        flushAndClear();

        feedSearchCleaner.deleteOldTask();
        flushAndClear();

        assertFalse(dbFeedOfferDetailsRepo.existsById(oldOffer.getId()));
        assertFalse(dbFeedOfferDeliveryRepo.existsById(oldOffer.getDeliveryOptions().get(0).getId()));
        assertFalse(feedSearchTaskRepo.existsById(oldTask.getId()));
        assertFalse(feedSearchTaskLogRepo.existsById(oldLog.getId()));
        assertFalse(dbFeedOfferDeliveryRepo.existsById(oldDeliveryCalc.getId()));

        assertTrue(dbFeedOfferDetailsRepo.existsById(newOffer.getId()));
        assertTrue(dbFeedOfferDeliveryRepo.existsById(newOffer.getDeliveryOptions().get(0).getId()));
        assertTrue(feedSearchTaskRepo.existsById(newTask.getId()));
        assertTrue(feedSearchTaskLogRepo.existsById(newLog.getId()));
        assertTrue(dbFeedOfferDeliveryRepo.existsById(newDeliveryCalc.getId()));
    }

    static DbFeedOfferDetails createOfferWithDelivery() {
        var dbOffer = new DbFeedOfferDetails();
        dbOffer.setFeedId(0);
        dbOffer.setOfferId("");
        dbOffer.setShopCurrency(Currency.RUR);
        dbOffer.setDeliveryOptions(List.of(createDelivery(dbOffer)));
        return dbOffer;
    }

    static DbFeedOfferDelivery createDelivery(DbFeedOfferDetails offer) {
        var delivery = new DbFeedOfferDelivery();
        delivery.setFeedOfferDetails(offer);
        delivery.setPrice(BigDecimal.ZERO);
        return delivery;
    }

    FeedSearchTaskLog createLog(FeedSearchTask task) {
        var log = FeedSearchTaskLog.builder(task, "").build();
        return feedSearchTaskLogRepo.save(log);
    }

    FeedSearchTask createTask(long ageInDays, DbFeedOfferDetails dbOffer) {
        var searchAfter = LocalDateTime.now().minusDays(ageInDays);
        var task = FeedSearchTask.builder()
                .withSearchAfter(searchAfter)
                .withFeedId(0)
                .withShopOfferId("")
                .withSourceId(0)
                .withSourceType(FeedSearchTaskSource.CORE)
                .withTarget(SearchTaskTarget.IDX_API)
                .withShopId(0)
                .withCurrency(dbOffer.getShopCurrency())
                .build();
        task.setOffer(dbOffer);
        var deliveryCalc = new DeliveryCalculatorInfoSnapshot(true, LocalDateTime.now(), LocalDateTime.now(), task);
        task.setDeliveryCalculatorInfoSnapshot(deliveryCalc);
        return feedSearchTaskRepo.save(task);
    }
}
