package ru.yandex.market.loyalty.admin.tms;

import java.time.Duration;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.bolts.collection.Option;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.dao.OncePerPhoneNumRecordDao;
import ru.yandex.market.loyalty.core.model.coupon.OncePerPhoneNumRecord;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.personal.PersonalClient;
import ru.yandex.market.loyalty.core.service.personal.PersonalClientImpl;
import ru.yandex.market.loyalty.core.service.personal.PersonalService;
import ru.yandex.market.loyalty.core.service.personal.model.BulkStoreResponseItemSuccess;
import ru.yandex.market.loyalty.core.service.personal.model.PersonalBulkStoreResponse;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.monitoring.PushMonitor;
import ru.yandex.market.loyalty.test.TestFor;
import ru.yandex.market.loyalty.trace.Tracer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

@TestFor(OneTimeTmsProcessor.class)
public class OneTimeTmsProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private OncePerPhoneNumRecordDao oncePerPhoneNumRecordDao;
    @Autowired
    private PromoService promoService;
    @Autowired
    private Tracer tracer;
    @Autowired
    private PushMonitor pushMonitor;
    @Autowired
    private OneTimeTmsProcessor oneTimeTmsProcessor;

    @Test
    public void shouldFillPhoneIds() {
        final var phone1 = "+79001112201";
        final var phone2 = "+79001112202";
        final var phoneId1 = "phone_id_1";
        final var phoneId2 = "phone_id_2";
        var promoId1 = promoService.addPromo(PromoUtils.Cashback.defaultPercent(3));
        var promoId2 = promoService.addPromo(PromoUtils.Cashback.defaultPercent(5));
        oncePerPhoneNumRecordDao.addRecord(OncePerPhoneNumRecord.builder().setPromoId(promoId1).setPhoneNum(phone1).build());
        oncePerPhoneNumRecordDao.addRecord(OncePerPhoneNumRecord.builder().setPromoId(promoId2).setPhoneNum(phone2).build());
        oncePerPhoneNumRecordDao.addRecord(OncePerPhoneNumRecord.builder().setPromoId(promoId1).setPhoneNum(phone2).build());

        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        final PersonalClient personalClient = new PersonalClientImpl(restTemplate,
                () -> Option.of("service ticket").toOptional(),"http://personal.tst.yandex.net");
        final PersonalService personalService = new PersonalService(tracer, pushMonitor, personalClient);

        Mockito.when(restTemplate.exchange(any(RequestEntity.class), any(Class.class))).thenReturn(ResponseEntity.ok(
                new PersonalBulkStoreResponse(List.of(
                        new BulkStoreResponseItemSuccess(phoneId1, phone1),
                        new BulkStoreResponseItemSuccess(phoneId2, phone2)
                ))
        ));

        final var testingProcessor = new OneTimeTmsProcessor(oncePerPhoneNumRecordDao, personalService, clock);
        testingProcessor.fillPersonalPhoneId(Duration.ofMillis(10_000));

        assertThat(oncePerPhoneNumRecordDao.getPhonesWithoutPersonalId(1_000), Matchers.anEmptyMap());
        assertTrue(oncePerPhoneNumRecordDao.hasRecord(phoneId1, promoId1));
        assertTrue(oncePerPhoneNumRecordDao.hasRecord(phoneId2, promoId2));
        assertTrue(oncePerPhoneNumRecordDao.hasRecord(phoneId2, promoId1));

        // to meet 'not all public methods was call' (dry run)
        this.oneTimeTmsProcessor.fillPersonalPhoneId(Duration.ofMillis(0));
    }
}
