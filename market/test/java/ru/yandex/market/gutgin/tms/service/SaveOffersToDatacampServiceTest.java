package ru.yandex.market.gutgin.tms.service;

import Market.DataCamp.DataCampOffer;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logbroker.LogbrokerInteractionException;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.partner.content.common.db.dao.XlsDataCampDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.XlsLogbrokerStatus;

import java.sql.Timestamp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.XLS_DATACAMP_OFFER;

@SuppressWarnings("checkstyle:magicnumber")
public class SaveOffersToDatacampServiceTest extends BaseXlsDatacampServiceTest {

    private XlsDataCampDao dataCampOfferDao;
    private LogbrokerService logbrokerService;
    private XlsDataCampLogbrokerSender logbrokerSender;
    private SaveOffersToDatacampService saveOffersToDatacampService;

    @Before
    public void init() {
        logbrokerService = mock(LogbrokerService.class);
        logbrokerSender = new XlsDataCampLogbrokerSender(logbrokerService);
        dataCampOfferDao = new XlsDataCampDao(configuration);
        saveOffersToDatacampService = new SaveOffersToDatacampService(logbrokerSender, dataCampOfferDao);
        prepareDb();
    }

    @Test
    public void shouldProcessNewOffersSuccessfully() {
        dsl().insertInto(XLS_DATACAMP_OFFER,
            XLS_DATACAMP_OFFER.ID,
            XLS_DATACAMP_OFFER.GC_RAW_SKU_ID,
            XLS_DATACAMP_OFFER.UPDATE_DATE,
            XLS_DATACAMP_OFFER.CREATE_DATE,
            XLS_DATACAMP_OFFER.STATUS,
            XLS_DATACAMP_OFFER.DATACAMP_OFFER
        )
            .values(1L, 1L, new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()),
                XlsLogbrokerStatus.NEW, DataCampOffer.Offer.newBuilder().build())
            .values(2L, 1L, new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()),
                XlsLogbrokerStatus.NEW, DataCampOffer.Offer.newBuilder().build())
            .execute();

        boolean sendingResult = saveOffersToDatacampService.sendToDataCamp(1L);

        int success = dsl().selectFrom(XLS_DATACAMP_OFFER)
            .where(XLS_DATACAMP_OFFER.STATUS.eq(XlsLogbrokerStatus.SUCCESS))
            .execute();
        int notSuccess = dsl().selectFrom(XLS_DATACAMP_OFFER)
            .where(XLS_DATACAMP_OFFER.STATUS.ne(XlsLogbrokerStatus.SUCCESS))
            .execute();

        verify(logbrokerService, times(1)).publishEvent(any());
        assertTrue("Result should be true", sendingResult);
        assertEquals("Success offers wrong value", 2, success);
        assertEquals("Not success offers wrong value", 0, notSuccess);
    }

    @Test
    public void shouldNotProcessOffersInCaseLogbrokerError() {
        doThrow(new LogbrokerInteractionException(""))
            .when(logbrokerService).publishEvent(any());

        dsl().insertInto(XLS_DATACAMP_OFFER,
            XLS_DATACAMP_OFFER.ID,
            XLS_DATACAMP_OFFER.GC_RAW_SKU_ID,
            XLS_DATACAMP_OFFER.UPDATE_DATE,
            XLS_DATACAMP_OFFER.CREATE_DATE,
            XLS_DATACAMP_OFFER.STATUS,
            XLS_DATACAMP_OFFER.DATACAMP_OFFER
        )
            .values(1L, 1L, new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()),
                XlsLogbrokerStatus.NEW, DataCampOffer.Offer.newBuilder().build())
            .values(2L, 1L, new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()),
                XlsLogbrokerStatus.NEW, DataCampOffer.Offer.newBuilder().build())
            .execute();

        boolean sendingResult = saveOffersToDatacampService.sendToDataCamp(1L);

        int success = dsl().selectFrom(XLS_DATACAMP_OFFER)
            .where(XLS_DATACAMP_OFFER.STATUS.eq(XlsLogbrokerStatus.SUCCESS))
            .execute();
        int failed = dsl().selectFrom(XLS_DATACAMP_OFFER)
            .where(XLS_DATACAMP_OFFER.STATUS.eq(XlsLogbrokerStatus.FAIL))
            .execute();

        verify(logbrokerService, times(1)).publishEvent(any());
        assertFalse("Result should be false", sendingResult);
        assertEquals("Success offers wrong value", 0, success);
        assertEquals("Failed offers wrong value", 2, failed);
    }

    @Test
    public void shouldOtherOffersRemainsNew() {
        dsl().insertInto(XLS_DATACAMP_OFFER,
            XLS_DATACAMP_OFFER.ID,
            XLS_DATACAMP_OFFER.GC_RAW_SKU_ID,
            XLS_DATACAMP_OFFER.UPDATE_DATE,
            XLS_DATACAMP_OFFER.CREATE_DATE,
            XLS_DATACAMP_OFFER.STATUS,
            XLS_DATACAMP_OFFER.DATACAMP_OFFER
        )
            .values(1L, 1L, new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()),
                XlsLogbrokerStatus.NEW, DataCampOffer.Offer.newBuilder().build())
            .values(2L, 1L, new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()),
                XlsLogbrokerStatus.NEW, DataCampOffer.Offer.newBuilder().build())
            .values(3L, 1L, new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()),
                XlsLogbrokerStatus.NEW, DataCampOffer.Offer.newBuilder().build())
            .values(4L, 2L, new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()),
                XlsLogbrokerStatus.NEW, DataCampOffer.Offer.newBuilder().build())
            .values(5L, 2L, new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()),
                XlsLogbrokerStatus.NEW, DataCampOffer.Offer.newBuilder().build())
            .values(6L, 3L, new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()),
                XlsLogbrokerStatus.NEW, DataCampOffer.Offer.newBuilder().build())
            .execute();

        boolean sendingResult = saveOffersToDatacampService.sendToDataCamp(2L);

        int successCurrent = dsl().selectFrom(XLS_DATACAMP_OFFER)
            .where()
            .and(XLS_DATACAMP_OFFER.GC_RAW_SKU_ID.eq(2L))
            .and(XLS_DATACAMP_OFFER.STATUS.eq(XlsLogbrokerStatus.SUCCESS))
            .execute();
        int notSuccessCurrent = dsl().selectFrom(XLS_DATACAMP_OFFER)
            .where()
            .and(XLS_DATACAMP_OFFER.GC_RAW_SKU_ID.eq(2L))
            .and(XLS_DATACAMP_OFFER.STATUS.ne(XlsLogbrokerStatus.SUCCESS))
            .execute();

        int othersRemainsNew = dsl().selectFrom(XLS_DATACAMP_OFFER)
            .where()
            .and(XLS_DATACAMP_OFFER.GC_RAW_SKU_ID.ne(2L))
            .and(XLS_DATACAMP_OFFER.STATUS.eq(XlsLogbrokerStatus.NEW))
            .execute();

        verify(logbrokerService, times(1)).publishEvent(any());
        assertTrue("Result should be true", sendingResult);
        assertEquals("Success offers wrong value", 2, successCurrent);
        assertEquals("Not success offers wrong value", 0, notSuccessCurrent);
        assertEquals("Others new offers wrong value", 4, othersRemainsNew);
    }

    @Test
    public void shouldOkIfOffersEmpty() {
        boolean sendingResult = saveOffersToDatacampService.sendToDataCamp(2L);
        verify(logbrokerService, times(0)).publishEvent(any());
        assertTrue("Result should be true", sendingResult);
    }
}
