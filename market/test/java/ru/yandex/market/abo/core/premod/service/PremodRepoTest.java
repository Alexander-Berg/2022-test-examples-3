package ru.yandex.market.abo.core.premod.service;

import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.premod.model.PremodCheckType;
import ru.yandex.market.abo.core.premod.model.PremodFlag;
import ru.yandex.market.abo.core.premod.model.PremodFlagName;
import ru.yandex.market.abo.core.premod.model.PremodItem;
import ru.yandex.market.abo.core.premod.model.PremodItemStatus;
import ru.yandex.market.abo.core.premod.model.PremodItemType;
import ru.yandex.market.abo.core.premod.model.PremodTicket;
import ru.yandex.market.abo.core.premod.model.PremodTicketStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author komarovns
 * @date 30.07.19
 */
class PremodRepoTest extends EmptyTest {
    private static final long SHOP_ID = 774;

    @Autowired
    private PremodRepo.PremodTicketRepo premodTicketRepo;
    @Autowired
    private PremodRepo.PremodItemRepo premodItemRepo;
    @Autowired
    private PremodFlagRepo premodFlagRepo;

    @Test
    void findAllByStatusIdAndCheckTypeIdAndWithoutFlagTest() {
        var ticket = premodTicketRepo.save(new PremodTicket(SHOP_ID, 0, PremodCheckType.CPC_PREMODERATION));
        var ticketList = premodTicketRepo.findAllByStatusIdAndCheckTypeIdAndWithoutFlag(
                ticket.getStatus().getId(), ticket.getTestingType().getId(), PremodFlagName.OFFERS_CREATED
        );
        assertEquals(1, ticketList.size());

//        premodFlagRepo.save(new PremodFlag(PremodFlagName.ANOTHER_FLAG, ticket.getId()));
        ticketList = premodTicketRepo.findAllByStatusIdAndCheckTypeIdAndWithoutFlag(
                ticket.getStatus().getId(), ticket.getTestingType().getId(), PremodFlagName.OFFERS_CREATED
        );
        assertEquals(1, ticketList.size());

        premodFlagRepo.save(new PremodFlag(PremodFlagName.OFFERS_CREATED, ticket.getId()));
        ticketList = premodTicketRepo.findAllByStatusIdAndCheckTypeIdAndWithoutFlag(
                ticket.getStatus().getId(), ticket.getTestingType().getId(), PremodFlagName.OFFERS_CREATED
        );
        assertEquals(0, ticketList.size());
    }

    @Test
    void findLastTicketsByShopIdAndCheckTypeIdInTest() {
        var oldCpc = premodTicketRepo.saveAndFlush(new PremodTicket(SHOP_ID, 0, PremodCheckType.CPC_PREMODERATION)).getId();
        var newCpc = premodTicketRepo.saveAndFlush(new PremodTicket(SHOP_ID, 0, PremodCheckType.CPC_PREMODERATION)).getId();
        var cpa = premodTicketRepo.saveAndFlush(new PremodTicket(SHOP_ID, 0, PremodCheckType.CPA_PREMODERATION)).getId();
        flushAndClear();

        var typeIds = StreamEx.of(PremodCheckType.MARKET_TYPES).map(PremodCheckType::getId).toList();
        var statusIds = StreamEx.of(PremodTicketStatus.values()).map(PremodTicketStatus::getId).toList();
        var tickets = premodTicketRepo.findLastTicketsByShopIdAndCheckTypeIdInAndStatusIdIn(
                SHOP_ID, typeIds, statusIds
        );
        assertEquals(2, tickets.size());
        var ticketIds = StreamEx.of(tickets).map(PremodTicket::getId).toSet();
        assertEquals(Set.of(newCpc, cpa), ticketIds);
    }

    @Test
    void deleteByTicketIdAndTypeIdTest() {
        var t1 = premodTicketRepo.saveAndFlush(new PremodTicket(SHOP_ID, 0, PremodCheckType.CPA_PREMODERATION)).getId();
        var t2 = premodTicketRepo.saveAndFlush(new PremodTicket(SHOP_ID, 0, PremodCheckType.CPA_PREMODERATION)).getId();
        var i1 = premodItemRepo.save(new PremodItem(t1, PremodItemStatus.NEW, PremodItemType.MONITORINGS)).getId();
        var i2 = premodItemRepo.save(new PremodItem(t1, PremodItemStatus.NEW, PremodItemType.AUTOORDER)).getId();
        var i3 = premodItemRepo.save(new PremodItem(t2, PremodItemStatus.NEW, PremodItemType.MONITORINGS)).getId();
        var i4 = premodItemRepo.save(new PremodItem(t2, PremodItemStatus.NEW, PremodItemType.AUTOORDER)).getId();
        flushAndClear();
        premodItemRepo.deleteByTicketIdAndTypeId(t2, PremodItemType.AUTOORDER.getId());
        flushAndClear();
        assertThat(premodItemRepo.findAll())
                .extracting(PremodItem::getId)
                .containsExactlyInAnyOrder(i1, i2, i3);
    }
}
