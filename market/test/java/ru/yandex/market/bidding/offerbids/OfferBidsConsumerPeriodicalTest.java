package ru.yandex.market.bidding.offerbids;

import java.io.File;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.kikimr.persqueue.consumer.StreamConsumer;
import ru.yandex.market.bidding.service.AdminService;
import ru.yandex.market.bidding.service.ExchangeService;
import ru.yandex.market.failover.FailoverTestUtils;
import ru.yandex.market.failover.Periodical;
import ru.yandex.market.failover.selfcheck.SelfcheckHeartbeatService;
import ru.yandex.market.logbroker.db.LogbrokerMonitorExceptionsService;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.bidding.ExchangeProtos.Parcel;
import static ru.yandex.market.bidding.offerbids.OfferBidsTestUtils.createParcelWithManyBids;

/**
 * Тесты для {@link OfferBidsConsumerPeriodical}
 */
@ExtendWith(MockitoExtension.class)
public class OfferBidsConsumerPeriodicalTest {

    private static final String storageBasePath = System.getProperty("java.io.tmpdir");

    private OfferBidsConsumerPeriodical offerBidsConsumerPeriodical;

    @Mock
    private ExchangeService exchangeService;

    @Mock
    private AdminService adminService;

    @BeforeEach
    void beforeEach() throws NoSuchFieldException, IllegalAccessException {
        when(adminService.isProcessOfferBidsReply()).thenReturn(true);
        when(adminService.getOfferBidsReplyBidsCount()).thenReturn(10);

        OfferBidsConsumerCreator creator = mock(OfferBidsConsumerCreator.class);
        when(creator.create()).thenReturn(mock(StreamConsumer.class));

        offerBidsConsumerPeriodical = spy(new OfferBidsConsumerPeriodical(
                creator,
                mock(TransactionTemplate.class),
                mock(LogbrokerMonitorExceptionsService.class),
                exchangeService,
                adminService,
                storageBasePath
        ));

        FailoverTestUtils.setPrivate(offerBidsConsumerPeriodical, "failoverThreadPriority", 8);
        FailoverTestUtils.setPrivate(offerBidsConsumerPeriodical, "applicationContext", mock(AbstractApplicationContext.class));
        FailoverTestUtils.setPrivate(offerBidsConsumerPeriodical, "selfcheckHeartbeatService", mock(SelfcheckHeartbeatService.class));

        offerBidsConsumerPeriodical.start();
        startOfferBidsPeriodical();
    }

    @AfterEach
    void afterEach() {
        if (offerBidsConsumerPeriodical != null) {
            offerBidsConsumerPeriodical.stop();
        }
    }

    @DisplayName("Тест на то,что данные собираются в один парсель и отправляются в биддинг")
    @Test
    void testSendParcelToBiddingEngine() throws ExchangeService.BadDataException {
        var parcel3bids = createParcelWithManyBids(3, 1L);
        var parcel8bids = createParcelWithManyBids(8, 1L);
        var parcel9bids = createParcelWithManyBids(9, 1L);

        offerBidsConsumerPeriodical.saveParcel(parcel3bids);
        offerBidsConsumerPeriodical.saveParcel(parcel8bids);
        offerBidsConsumerPeriodical.saveParcel(parcel9bids);

        var captor = ArgumentCaptor.forClass(Parcel.class);

        //в bidding engine уедет всего 1 большой парсель
        // т.к. 2 парсель не успел еще накопится ( там всего 9 бидов, а порог = 10) 
        verify(exchangeService).parcelReply(anyInt(), captor.capture());

        var bids = captor.getValue().getBidsList();
        assertThat(bids, hasSize(11)); // 3 + 8
        assertThat(bids, is(
                Parcel.newBuilder()
                        .mergeFrom(parcel3bids)
                        .mergeFrom(parcel8bids)
                        .getBidsList()
        ));
    }

    @DisplayName("Тест на то, что остаточные данные уедут в bidding engine после остановки консумера")
    @Test
    void testSendParcelToBiddingEngineAfterStop() throws ExchangeService.BadDataException {
        var parcel3bids = createParcelWithManyBids(3, 1L);
        offerBidsConsumerPeriodical.saveParcel(parcel3bids);

        //ничего не отправилось, т.к. порог не превышен
        verify(exchangeService, never()).parcelReply(anyInt(), ArgumentMatchers.any(Parcel.class));

        offerBidsConsumerPeriodical.stop();

        var captor = ArgumentCaptor.forClass(Parcel.class);
        verify(exchangeService, only()).parcelReply(anyInt(), captor.capture());
        assertThat(captor.getValue().getBidsList(), is(parcel3bids.getBidsList()));
    }

    @DisplayName("Тест на то, что если ничего не пришло - то мы не отправим пустой парсель в bidding engine")
    @Test
    void testNoSendEmptyParcelToBiddingEngine() throws ExchangeService.BadDataException {
        var emptyParcel = createParcelWithManyBids(0, 1L);
        offerBidsConsumerPeriodical.saveParcel(emptyParcel);

        //ничего не отправилось, т.к. парсель пустой
        verify(exchangeService, never()).parcelReply(anyInt(), ArgumentMatchers.any(Parcel.class));

        offerBidsConsumerPeriodical.stop();

        //ничего не отправилось даже после остановки - сработало условие
        verify(exchangeService, never()).parcelReply(anyInt(), ArgumentMatchers.any(Parcel.class));
    }

    @DisplayName("Тест на сохранение \"плохого\" парселя в файл")
    @Test
    void testSaveBadParcelToFile() throws Exception {
        var badParcel = createParcelWithManyBids(10, 1L);
        var filePath = offerBidsConsumerPeriodical.saveParcelOnFS(badParcel);

        var parcelFile = new File(filePath);
        try {
            assertTrue(parcelFile.exists());

            final byte[] readBytes = FileUtils.readFileToByteArray(parcelFile);
            var parcelFromFile = Parcel.parseFrom(readBytes);
            assertThat(parcelFromFile, equalTo(badParcel));
        } finally {
            FileUtils.deleteQuietly(parcelFile);
        }
    }

    private void startOfferBidsPeriodical() {
        try {
            var method = Periodical.class.getDeclaredMethod("startTimer");
            method.setAccessible(true);
            method.invoke(offerBidsConsumerPeriodical);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
