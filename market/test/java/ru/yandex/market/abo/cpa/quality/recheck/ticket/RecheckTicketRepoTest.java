package ru.yandex.market.abo.cpa.quality.recheck.ticket;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.cpa.quality.recheck.repo.RecheckTicketRepo;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecheckTicketRepoTest extends EmptyTest {
    private static final long SHOP_ID = -1L;

    @Autowired
    private RecheckTicketRepo recheckTicketRepo;

    private List<Long> ids;

    @BeforeEach
    void setUp() {
        ids = recheckTicketRepo.saveAll(
                Stream.of(
                        createTicket(RecheckTicketType.REGION_GROUP_MODERATION, RecheckTicketStatus.OPEN), // 0
                        createTicket(RecheckTicketType.REGION_GROUP_MODERATION, RecheckTicketStatus.OPEN), // 1
                        createTicket(RecheckTicketType.REGION_GROUP_MODERATION, RecheckTicketStatus.OPEN), // 2
                        createTicket(RecheckTicketType.REGION_GROUP_MODERATION, RecheckTicketStatus.FAIL), // 3
                        createTicket(RecheckTicketType.LITE_TICKET_COMMON, RecheckTicketStatus.OPEN),      // 4
                        createTicket(RecheckTicketType.MASS_FOUND, RecheckTicketStatus.OPEN)         // 5
                ).collect(Collectors.toList())
        ).stream().map(RecheckTicket::getId).collect(Collectors.toList());
    }

    @Test
    void testFindAllByTypeIdAndStatusId() {
        List<RecheckTicket> loaded = recheckTicketRepo.findByTypeIdAndStatusIdInAndCreationTimeBefore(
                RecheckTicketType.REGION_GROUP_MODERATION.getId(),
                List.of(RecheckTicketStatus.OPEN.getId()),
                new Date()
        );

        Set<Long> expectedIds = IntStream.range(0, 3).mapToObj(ids::get).collect(Collectors.toSet());
        assertEquals(expectedIds, loadedIds(loaded));
    }

    private static Set<Long> loadedIds(List<RecheckTicket> loaded) {
        return loaded.stream().map(RecheckTicket::getId).collect(Collectors.toSet());
    }

    private static RecheckTicket createTicket(RecheckTicketType type, RecheckTicketStatus status) {
        RecheckTicket recheckTicket = new RecheckTicket(SHOP_ID, type, "");
        recheckTicket.setStatus(status);
        return recheckTicket;
    }
}
