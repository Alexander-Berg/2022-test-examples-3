package ru.yandex.market.pers.pay.tms;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.pay.PersPayTest;
import ru.yandex.market.pers.pay.model.PersPayEntity;
import ru.yandex.market.pers.pay.model.PersPayEntityType;
import ru.yandex.market.pers.pay.model.PersPayUser;
import ru.yandex.market.pers.pay.model.PersPayUserType;
import ru.yandex.market.pers.pay.model.SkippedOfferReason;
import ru.yandex.market.pers.pay.model.SkippedOffers;
import ru.yandex.market.pers.pay.service.AsyncPaymentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 19.10.2021
 */
public class SkipOffersLogJobTest extends PersPayTest {

    @Autowired
    private SkipOffersLogJob job;

    @Autowired
    private AsyncPaymentService asyncPaymentService;

    @Test
    public void testQueue() {
        assertTrue(asyncPaymentService.isEnableAsync());

        // fill with N-1 items
        asyncPaymentService.getQueue().addAll(IntStream
            .range(0, AsyncPaymentService.TOO_LONG_QUEUE)
            .mapToObj(x -> (Runnable) () -> {
            })
            .collect(Collectors.toList())
        );

        // still ok
        job.skipOffersLogQueueCheck();
        assertTrue(asyncPaymentService.isEnableAsync());

        // add one more
        asyncPaymentService.getQueue().add(() -> {
        });

        // stop processing
        job.skipOffersLogQueueCheck();
        assertFalse(asyncPaymentService.isEnableAsync());

        // remove N/2+1 - still disabled since N/2 are left
        IntStream.range(0, AsyncPaymentService.TOO_LONG_QUEUE / 2 + 1)
            .forEach(x -> asyncPaymentService.getQueue().poll());

        job.skipOffersLogQueueCheck();
        assertFalse(asyncPaymentService.isEnableAsync());

        // remove one more - activate queue
        asyncPaymentService.getQueue().poll();
        job.skipOffersLogQueueCheck();
        assertTrue(asyncPaymentService.isEnableAsync());
    }

    @Test
    public void testQueueCleanup() {
        SkippedOffers offers = new SkippedOffers(new PersPayUser(PersPayUserType.UID, 123));
        offers.skipEntity(new PersPayEntity(PersPayEntityType.MODEL_GRADE, 1), SkippedOfferReason.OFFER_CNT_LIMIT);
        offers.skipEntity(new PersPayEntity(PersPayEntityType.MODEL_GRADE, 2), SkippedOfferReason.OFFER_CNT_LIMIT);

        asyncPaymentService.logNoOfferReasonsSync(offers);

        jdbcTemplate.update(
            "update pay.payment_offer_skip_log set upd_time = now() - make_interval(days:= ? - 1) " +
                "where entity_id = ?",
            AsyncPaymentService.KEEP_SKIP_LOG_DAYS,
            "1"
        );
        jdbcTemplate.update(
            "update pay.payment_offer_skip_log set upd_time = now() - make_interval(days:= ? + 1) " +
                "where entity_id = ?",
            AsyncPaymentService.KEEP_SKIP_LOG_DAYS,
            "2"
        );

        job.skipOffersLogQueueCleanup();

        assertEquals("0-123-1-1",
            jdbcTemplate.queryForObject("select pay_key from pay.payment_offer_skip_log", String.class));
    }
}
