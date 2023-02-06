package ru.yandex.market.abo.core.complain.service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.api.entity.complaint.ComplaintPlatform;
import ru.yandex.market.abo.api.entity.complaint.ComplaintType;
import ru.yandex.market.abo.core.complain.model.Complaint;
import ru.yandex.market.abo.core.hiding.util.model.CheckStatus;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.checkout.checkouter.order.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 02.11.2015
 */
class ComplaintServiceTest extends EmptyTest {
    private static final long SHOP_ID = 774L;
    @Autowired
    private ComplaintService complaintService;
    @PersistenceContext
    private EntityManager entityManager;
    @Mock
    private Offer offer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(offer.getClassifierMagicId()).thenReturn("cmid");
    }

    @Test
    void testUpdateComplaints() {
        Complaint complaint = storeComplaint();
        assertNull(complaint.getCmId());
        Date removedDate = new Date();
        complaint.setCheckStatus(CheckStatus.OFFER_REMOVED);
        complaint.setOfferRemovedDate(removedDate);
        complaint.setReportOffer(offer);
        complaintService.updateTillNextGen(Collections.singletonList(complaint));
        entityManager.flush();
        entityManager.clear();

        List<Complaint> complaints = complaintService.findComplaints(CheckStatus.OFFER_REMOVED, false);
        assertEquals(1, complaints.size());
        Complaint dbComplaint = complaints.get(0);
        assertNotNull(dbComplaint);
        assertNotNull(dbComplaint.getCmId());
        assertEquals(dbComplaint.getCheckStatus(), complaint.getCheckStatus());
        assertEquals(dbComplaint.getComplaintType(), complaint.getComplaintType());
        assertTrue(removedDate.getTime() - dbComplaint.getOfferRemovedDate().getTime() < 1000);
    }

    @Test
    void testStoreSeveral() {
        IntStream.rangeClosed(0, 1).forEach(i -> storeComplaint());
        List<Complaint> complaints = complaintService.findComplaints(CheckStatus.NEW, false);
        assertEquals(2, complaints.size());
        complaints.forEach(c -> {
            assertEquals(CheckStatus.NEW, c.getCheckStatus());
            assertNotNull(c.getCreateTime());
        });
    }

    private Complaint storeComplaint() {
        return complaintService.storeComplaint(SHOP_ID, 1L, "wareMd5", null, 2L, "offer_id",
                ComplaintType.PRICE, ComplaintPlatform.DESKTOP, "text", true, 213L, null, Color.GREEN, null);
    }
}
