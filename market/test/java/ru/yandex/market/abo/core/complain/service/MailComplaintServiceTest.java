package ru.yandex.market.abo.core.complain.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.api.entity.complaint.ComplaintType;
import ru.yandex.market.abo.core.complain.model.Complaint;
import ru.yandex.market.abo.core.hiding.util.HiddenOffersNotifier;
import ru.yandex.market.abo.core.hiding.util.model.CheckStatus;
import ru.yandex.market.abo.core.hiding.util.model.Mailable;
import ru.yandex.market.abo.core.indexer.Generation;
import ru.yandex.market.abo.core.indexer.GenerationService;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.cpa.MbiApiService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 23.11.2015
 */
public class MailComplaintServiceTest extends EmptyTest {

    @Autowired
    private MailComplaintService mailComplaintService;

    private MbiApiService mbiApiService;
    private Complaint complaint;
    private ComplaintService complaintService;
    private GenerationService generationService;
    private Generation generation;

    @BeforeEach
    public void init() {
        mbiApiService = mock(MbiApiService.class);
        complaintService = mock(ComplaintService.class);
        generationService = mock(GenerationService.class);
        generation = mock(Generation.class);
        when(generation.getReleaseDate()).thenReturn(new Timestamp(new Date().getTime()));
        when(generationService.loadPrevReleaseGeneration()).thenReturn(generation);
        when(mbiApiService.sendMessageToShop(anyLong(), anyInt(), anyString())).thenReturn(true);
        HiddenOffersNotifier hiddenOffersNotifier = spy(new HiddenOffersNotifier());
        hiddenOffersNotifier.setMbiApiService(mbiApiService);
        hiddenOffersNotifier.setGenerationService(generationService);
        mailComplaintService.setComplaintService(complaintService);
        mailComplaintService.setHiddenOffersNotifier(hiddenOffersNotifier);
    }

    @Test
    public void testSendComplainMails() {
        // проставилась ли дата отправки
        when(mbiApiService.sendMessageToShop(anyLong(), anyInt(), anyString())).thenReturn(true);
        complaint = initComplaint();
        mailComplaintService.sendComplainMails(Collections.singletonList(complaint));
        assertTrue(!getUpdatedMails().isEmpty());
    }

    @Test
    public void testMbiApiServiceException() {
        // в случае ошибки mbiApiService не проставлять дату отправки (для последующих попыток)
        when(mbiApiService.sendMessageToShop(anyLong(), anyInt(), anyString())).thenReturn(false);
        complaint = initComplaint();
        mailComplaintService.sendComplainMails(Collections.singletonList(complaint));
        assertNull(complaint.getMailSentDate());
    }

    @Test
    public void testAvoidDuplicates() {
        // уже отправленные в прошлые разы повторно не отсылать, если новых нет
        complaint = initComplaint();
        complaint.setMailSentDate(new Date());
        mailComplaintService.sendComplainMails(Collections.singletonList(complaint));
        verifyNoMoreInteractions(mbiApiService);
    }

    @Test
    public void testSendManyComplaints() {
        final int NUM = 300;
        List<Complaint> complaints = new ArrayList<>();
        for (int i = 0; i < NUM; i++) {
            complaints.add(initComplaint());
        }
        when(mbiApiService.sendMessageToShop(anyLong(), anyInt(), anyString())).thenReturn(true);
        mailComplaintService.sendComplainMails(complaints);
        assertTrue(getUpdatedMails().size() == NUM);
    }

    @Test
    public void testDoNotSendAfter1Gen() {
        //если оффер был скрыт поколение назад - письмо не шлем, ждем ещё одно поколение
        Complaint oldComplaint = initComplaint();
        Complaint newComplaint = initComplaint();
        newComplaint.setOfferRemovedDate(new Date());
        List<Complaint> complaints = new ArrayList<>(Arrays.asList(oldComplaint, newComplaint));
        mailComplaintService.sendComplainMails(complaints);
        assertEquals(getUpdatedMails().size(), 1);
    }

    private List<Mailable> getUpdatedMails() {
        ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);
        verify(complaintService, atLeastOnce()).updateMailDates(argument.capture());
        return argument.getValue();
    }

    private static Complaint initComplaint() {
        Complaint complaint = new Complaint();
        complaint.setShopId(0L);
        complaint.setCheckStatus(CheckStatus.OFFER_REMOVED);
        complaint.setOfferRemovedDate(new Date(0));
        complaint.setComplaintType(ComplaintType.OTHER);
        complaint.setReportOffer(initOffer());
        return complaint;
    }

    private static Offer initOffer() {
        Offer offer = new Offer();
        offer.setName("apple iphone 7");
        offer.setDirectUrl("http://apple.com");
        return offer;
    }
}
