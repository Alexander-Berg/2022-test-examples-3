package ru.yandex.market.abo.core.problem.pinger;

import java.util.Date;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;

import ru.yandex.EmptyTestWithTransactionTemplate;
import ru.yandex.market.abo.core.message.Messages;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.offer.report.ReportOfferService;
import ru.yandex.market.abo.core.offer.report.ShopSwitchedOffException;
import ru.yandex.market.abo.core.pinger.ShopUrlService;
import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.problem.model.ProblemStatus;
import ru.yandex.market.abo.core.ticket.OfferDbService;
import ru.yandex.market.abo.core.ticket.ProblemManager;
import ru.yandex.market.abo.core.ticket.flag.TicketFlagName;
import ru.yandex.market.abo.core.ticket.flag.TicketFlagService;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.abo.test.TestHelper;
import ru.yandex.market.abo.util.monitoring.SharedMonitoringUnit;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 14/02/19.
 */
class PingerProblemRecheckerTest extends EmptyTestWithTransactionTemplate {
    private static final Long PROBLEM_ID = 1L;
    private static final Long HYP_ID = 22L;
    private static final Long SHOP_ID = 33L;
    private static final String URL = "https://foo.org";
    @InjectMocks
    PingerProblemRechecker pingerProblemRechecker;
    @Mock
    ProblemManager problemManager;
    @Mock
    PingerProblemService pingerProblemService;
    @Mock
    TicketFlagService ticketFlagService;
    @Mock
    ReportOfferService offerService;
    @Mock
    OfferDbService offerDbService;
    @Mock
    MbiApiService mbiApiService;
    @Mock
    SharedMonitoringUnit problemCreationMonitoring;
    @Mock
    ShopUrlService shopUrlService;
    @Mock
    Problem problem;
    @Mock
    PingStatus pingStatus;
    @Mock
    Offer offer;
    @Mock
    ExecutorService pool;

    @BeforeEach
    void setUp() throws ShopSwitchedOffException {
        MockitoAnnotations.openMocks(this);
        when(problem.getId()).thenReturn(PROBLEM_ID);
        when(problem.getTicketId()).thenReturn(HYP_ID);

        when(problem.getCreationTime()).thenReturn(new Date());
        when(problemManager.load(any(), any())).thenReturn(new PageImpl<>(singletonList(problem)));
        when(ticketFlagService.hasTicketFlag(HYP_ID, TicketFlagName.URL_PROBLEM_MAIL_SENT)).thenReturn(false);

        when(pingerProblemService.lastProblemPingStatuses(any())).thenReturn(singletonList(pingStatus));
        when(pingStatus.getProblemId()).thenReturn(PROBLEM_ID);
        when(pingStatus.isUrlAvailable()).thenReturn(true);
        when(pingStatus.getUrl()).thenReturn(URL);
        when(pingStatus.getShopId()).thenReturn(SHOP_ID);

        when(offerService.findFirstWithParams(any())).thenReturn(offer);
        when(offerDbService.loadOfferByHypId(HYP_ID)).thenReturn(offer);
        when(shopUrlService.getShopDown()).thenReturn(emptyList());

        TestHelper.mockExecutorService(pool);
    }

    @Test
    void urlAvailable() {
        pingerProblemRechecker.recheckProblems();
        verify(problemManager).logApproveAndSave(eq(problem), eq(ProblemStatus.DISAPPROVED), any());
        verifyNoMoreInteractions(offerService, mbiApiService);
    }

    @Test
    void stillNotAvailableAndUrlIsActual() throws ShopSwitchedOffException {
        when(pingStatus.isUrlAvailable()).thenReturn(false);
        when(offer.getUrl()).thenReturn(URL);
        pingerProblemRechecker.recheckProblems();
        verify(problemManager, never()).logApproveAndSave(eq(problem), eq(ProblemStatus.DISAPPROVED), any());
        verify(offerService).findFirstWithParams(any());
    }

    @Test
    void notAvailableFor24Hrs() {
        when(problem.getCreationTime()).thenReturn(DateUtils.addHours(new Date(), -25));
        when(pingStatus.isUrlAvailable()).thenReturn(false);

        pingerProblemRechecker.recheckProblems();
        verify(problemManager, never()).logApproveAndSave(eq(problem), eq(ProblemStatus.DISAPPROVED), any());
        verify(mbiApiService).sendMessageToShop(eq(SHOP_ID), eq(Messages.MBI.BAD_URL), any());
        verify(ticketFlagService).addTicketFlag(HYP_ID, TicketFlagName.URL_PROBLEM_MAIL_SENT);
        verifyNoMoreInteractions(offerService);
    }

    @Test
    void notAvailableAndMailSent() {
        when(problem.getCreationTime()).thenReturn(DateUtils.addHours(new Date(), -25));
        when(pingStatus.isUrlAvailable()).thenReturn(false);
        when(ticketFlagService.hasTicketFlag(HYP_ID, TicketFlagName.URL_PROBLEM_MAIL_SENT)).thenReturn(true);

        pingerProblemRechecker.recheckProblems();
        verify(problemManager, never()).logApproveAndSave(eq(problem), eq(ProblemStatus.DISAPPROVED), any());
        verify(ticketFlagService, never()).addTicketFlag(HYP_ID, TicketFlagName.URL_PROBLEM_MAIL_SENT);
        verifyNoMoreInteractions(offerService, mbiApiService);
    }

    @ParameterizedTest
    @CsvSource({"https://foo.org, true", "http://foo.org, true", "foo.org, true", "https://bar.org, false"})
    void urlActual(String url, boolean actual) {
        when(offer.getUrl()).thenReturn(url);
        assertEquals(actual, pingerProblemRechecker.isUrlActual(pingStatus));
    }

    @Test
    void noOffer() throws ShopSwitchedOffException {
        when(offerService.findFirstWithParams(any())).thenReturn(null);
        assertFalse(pingerProblemRechecker.isUrlActual(pingStatus));
    }

    @Test
    void shopIsOff() throws ShopSwitchedOffException {
        when(offerService.findFirstWithParams(any())).thenThrow(new ShopSwitchedOffException());
        assertFalse(pingerProblemRechecker.isUrlActual(pingStatus));
    }

    @Test
    void getXmlBody() {
        String offerName = "bestOffer";
        int httpStatus = 400;
        String comment = PingerContentSizeProblemCreator.publicComment(httpStatus);
        when(offer.getName()).thenReturn(offerName);

        String xmlBody = pingerProblemRechecker.getXmlBody(HYP_ID, httpStatus);
        assertEquals(
                "<abo-info><offer-name>" + offerName + "</offer-name><comment>" + comment + "</comment></abo-info>",
                xmlBody.trim().replaceAll("\n", "")
        );
    }

    @Test
    void cannotSendEmail() {
        when(problem.getCreationTime()).thenReturn(DateUtils.addHours(new Date(), -25));
        when(pingStatus.isUrlAvailable()).thenReturn(false);
        when(offerDbService.loadOfferByHypId(anyLong())).thenReturn(null);

        pingerProblemRechecker.recheckProblems();
        verify(problemCreationMonitoring).critical(any(), any());
    }

    @Test
    void shopIsDown() {
        when(shopUrlService.getShopDown()).thenReturn(singletonList(SHOP_ID));
        when(pingStatus.isUrlAvailable()).thenReturn(false);
        when(problem.getCreationTime()).thenReturn(DateUtils.addHours(new Date(), -25));

        pingerProblemRechecker.recheckProblems();
        verify(problemManager).logApproveAndSave(eq(problem), eq(ProblemStatus.DISAPPROVED), any());
        verifyNoMoreInteractions(offerService, mbiApiService);
    }
}
