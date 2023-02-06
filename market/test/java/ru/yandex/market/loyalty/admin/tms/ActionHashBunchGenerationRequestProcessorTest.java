package ru.yandex.market.loyalty.admin.tms;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.service.bunch.BunchRequestService;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.dao.coin.GeneratorType;
import ru.yandex.market.loyalty.core.dao.hash.ActionHashDao;
import ru.yandex.market.loyalty.core.model.coin.BunchGenerationRequest;
import ru.yandex.market.loyalty.core.model.hash.ActionHash;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.mail.Attachment;
import ru.yandex.market.loyalty.core.service.mail.YabacksMailer;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import java.time.Duration;
import java.util.List;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.loyalty.admin.tms.CoinBunchRequestExecutor.MAX_DURATION;
import static ru.yandex.market.loyalty.admin.tms.CoinBunchRequestExecutor.WRITE_CHUNK_SIZE;
import static ru.yandex.market.loyalty.api.model.TableFormat.CSV;
import static ru.yandex.market.loyalty.core.dao.hash.ActionHashDao.ACTION_HASH_TABLE;

/**
 * @author : poluektov
 * date: 2019-09-04.
 */
@TestFor(BunchRequestProcessor.class)
public class ActionHashBunchGenerationRequestProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String MAIL = "krosh@example.com";
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private BunchRequestService bunchRequestService;
    @Autowired
    private BunchRequestProcessor bunchRequestProcessor;
    @Autowired
    private ActionHashDao actionHashDao;
    @Autowired
    private YabacksMailer yabacksMailer;

    private Promo promo;

    @Before
    public void generateDefaultPromo() {
        promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
    }

    @Test
    public void shouldGenerateActionHashes() {
        bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(promo.getId(), "test-hash1", 100, MAIL, CSV, null,
                        GeneratorType.ACTION_HASH));
        bunchRequestProcessor.actionHashBunchRequestProcess(WRITE_CHUNK_SIZE, MAX_DURATION);
        bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(promo.getId(), "test-hash2", 155, MAIL, CSV, null,
                        GeneratorType.ACTION_HASH));
        bunchRequestProcessor.actionHashBunchRequestProcess(WRITE_CHUNK_SIZE, MAX_DURATION);

        int count = actionHashDao.getHashesCount(ACTION_HASH_TABLE.sourceKey.like("%test-hash%"));
        assertEquals(255, count);
        List<ActionHash> generatedHashes = actionHashDao.selectHashes(
                ACTION_HASH_TABLE.id.geTo(1L), 1);
        assertThat(generatedHashes, not(empty()));
        ActionHash hash = generatedHashes.iterator().next();
        assertThat(hash.getCreationTime(), notNullValue());
        assertThat(hash.getUniqueKey(), notNullValue());
        assertThat(hash.getSourceKey(), notNullValue());
        assertThat(hash.getUid(), nullValue());
    }

    @Test
    public void shouldSendStartAndCompleteEmail() {
        bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(promo.getId(), "test-hash", 11, MAIL, CSV, null,
                        GeneratorType.ACTION_HASH));
        verify(yabacksMailer, times(1)).sendMail(anyString(), anyString(), anyString());

        bunchRequestProcessor.actionHashBunchRequestProcess(WRITE_CHUNK_SIZE, MAX_DURATION);
        bunchRequestProcessor.processPreparedRequests(Duration.of(1, MINUTES));
        verify(yabacksMailer, times(1)).sendMail(anyString(), anyString(), anyString(), any(Attachment.class));
    }
}
