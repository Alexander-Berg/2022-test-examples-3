package ru.yandex.market.abo.core.no_placement;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.premod.PremodTicketService;
import ru.yandex.market.abo.core.premod.model.PremodCheckType;
import ru.yandex.market.abo.core.premod.model.PremodTicket;
import ru.yandex.market.abo.core.premod.model.PremodTicketStatus;
import ru.yandex.market.core.abo.AboCutoff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author kukabara
 */
@Transactional(transactionManager = "jpaPgTransactionManager")
public class NoPlacementManagerTest extends EmptyTest {
    private static final long USER_ID = 1L;
    private static final long SHOP_ID = 774L;

    @Autowired
    NoPlacementManager noPlacementManager;
    @Autowired
    PremodTicketService premodTicketService;
    @Autowired
    private NoPlacementRecordRepository noPlacementRecordRepository;

    @Test
    public void testReason() throws Exception {
        NoPlacementReason saved = saveReason();
        assertTrue(saved.getId() > 0);

        Collection<NoPlacementReason> reasons = noPlacementManager.getActiveReasons();
        assertEquals(1, reasons.size());
        assertEquals(saved, reasons.iterator().next());

        String name = "new name";
        saved.setName(name);
        saved.setActive(false);
        NoPlacementReason saved2 = noPlacementManager.addOrModifyReason(saved);
        assertEquals(saved, saved2);
    }

    private NoPlacementReason saveReason() {
        NoPlacementReason reason = new NoPlacementReason();
        reason.setName("предлагает товары, реклама которых запрещена законодательством Российской Федерации");
        reason.setActive(true);
        return noPlacementManager.addOrModifyReason(reason);
    }

    @Test
    public void testRecord() throws Exception {
        NoPlacementReason saved = saveReason();
        AboCutoff cutoffType = AboCutoff.COMMON_QUALITY;
        NoPlacementRecord record = noPlacementManager.addRecord(SHOP_ID, saved, cutoffType, USER_ID);
        assertTrue(record.getId() > 0);

        List<NoPlacementRecord> records = noPlacementRecordRepository.findAll();
        assertFalse(records.isEmpty());
        assertFalse(records.get(0).isClosed());

        noPlacementManager.closeCutoff(SHOP_ID, cutoffType);
        records = noPlacementRecordRepository.findAll();
        assertFalse(records.isEmpty());
        assertTrue(records.get(0).isClosed());
    }

    @Test
    public void testRecordRemove() {
        PremodTicket ticket = new PremodTicket(SHOP_ID, 1, PremodCheckType.VERTICAL_SHARE);
        ticket.setStatus(PremodTicketStatus.IN_PROGRESS);
        premodTicketService.save(ticket);
        NoPlacementReason saved = saveReason();
        AboCutoff cutoffType = AboCutoff.VERTICAL_QUALITY;
        NoPlacementRecord record = noPlacementManager.addRecordForPremoderation(
                SHOP_ID, saved.getId(), cutoffType, USER_ID, ticket.getId()
        );
        assertTrue(record.getId() > 0);

        noPlacementManager.removeRecord(SHOP_ID, ticket.getId());
        assertTrue(noPlacementRecordRepository.findAll().isEmpty());
    }

    @Test
    public void testSpec() throws Exception {
        NoPlacementReason reason1 = saveReason();
        NoPlacementReason reason2 = saveReason();
        noPlacementManager.addRecord(SHOP_ID, reason1, AboCutoff.COMMON_QUALITY, USER_ID);
        noPlacementManager.addRecord(SHOP_ID, reason2, AboCutoff.COMMON_QUALITY, USER_ID);
        noPlacementManager.addRecord(SHOP_ID + 1, reason1, AboCutoff.COMMON_QUALITY, USER_ID);

        PageRequest pageable = PageRequest.of(0, 10);
        NoPlacementRecordRequest request = new NoPlacementRecordRequest();
        Page<NoPlacementRecord> page = noPlacementManager.searchRecords(request, pageable);
        assertEquals(3,page.getTotalElements());

        request.setShopId(SHOP_ID);
        page = noPlacementManager.searchRecords(request, pageable);
        assertEquals(2,page.getTotalElements());

        request.setShopId(null);
        request.setReasonId(reason1.getId());
        page = noPlacementManager.searchRecords(request, pageable);
        assertEquals(2,page.getTotalElements());
    }
}
