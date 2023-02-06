package ru.yandex.market.loyalty.admin.tms;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.service.bunch.BunchRequestService;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.dao.coin.GeneratorType;
import ru.yandex.market.loyalty.core.dao.coupon.CouponDao;
import ru.yandex.market.loyalty.core.model.coin.BunchGenerationRequest;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.mail.Attachment;
import ru.yandex.market.loyalty.core.service.mail.YabacksMailer;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import java.time.Duration;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.loyalty.admin.tms.CoinBunchRequestExecutor.MAX_DURATION;
import static ru.yandex.market.loyalty.admin.tms.CoinBunchRequestExecutor.WRITE_CHUNK_SIZE;
import static ru.yandex.market.loyalty.api.model.TableFormat.CSV;

/**
 * @author : poluektov
 * date: 2019-08-27.
 */
@TestFor(BunchRequestProcessor.class)
public class CouponBunchGenerationRequestProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String MAIL = "krosh@example.com";
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private BunchRequestService bunchRequestService;
    @Autowired
    private BunchRequestProcessor bunchRequestProcessor;
    @Autowired
    private CouponDao couponDao;
    @Autowired
    private YabacksMailer yabacksMailer;

    private Promo promo;

    @Before
    public void generateDefaultPromo() {
        promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
    }

    @Test
    public void shouldGenerateCoupons() {
        bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(promo.getId(), "test-coupon1", 100, MAIL, CSV, null,
                        GeneratorType.COUPON));
        bunchRequestProcessor.couponBunchRequestProcess(WRITE_CHUNK_SIZE, MAX_DURATION);
        bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(promo.getId(), "test-coupon2", 155, MAIL, CSV, null,
                        GeneratorType.COUPON));
        bunchRequestProcessor.couponBunchRequestProcess(WRITE_CHUNK_SIZE, MAX_DURATION);

        int count = couponDao.getCouponsCountBySourceKey("%test-coupon%");
        assertEquals(255, count);
    }

    @Test
    public void shouldSendStartAndCompleteEmail() {
        bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(promo.getId(), "coin", 11, MAIL, CSV, null, GeneratorType.COUPON));
        verify(yabacksMailer, times(1)).sendMail(anyString(), anyString(), anyString());

        bunchRequestProcessor.couponBunchRequestProcess(WRITE_CHUNK_SIZE, MAX_DURATION);
        bunchRequestProcessor.processPreparedRequests(Duration.of(1, MINUTES));
        verify(yabacksMailer, times(1)).sendMail(anyString(), anyString(), anyString(), any(Attachment.class));
    }

}
