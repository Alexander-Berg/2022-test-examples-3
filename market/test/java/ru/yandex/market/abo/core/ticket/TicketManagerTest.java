package ru.yandex.market.abo.core.ticket;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.core.calendar.WorkHour;
import ru.yandex.market.abo.core.calendar.db.CalendarEntry;
import ru.yandex.market.abo.core.calendar.db.CalendarService;
import ru.yandex.market.abo.core.inbox.InboxService;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.problem.model.ProblemStatus;
import ru.yandex.market.abo.core.problem.model.ProblemTypeId;
import ru.yandex.market.abo.core.ticket.model.CheckMethod;
import ru.yandex.market.abo.core.ticket.model.Ticket;
import ru.yandex.market.abo.core.ticket.model.TicketStatus;
import ru.yandex.market.abo.core.ticket.model.TicketTag;
import ru.yandex.market.abo.gen.model.GenId;
import ru.yandex.market.abo.gen.model.Hypothesis;
import ru.yandex.market.abo.util.FakeUsers;
import ru.yandex.market.abo.util.db.DbUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author komarovns
 * @date 20.08.18
 */
class TicketManagerTest extends AbstractCoreHierarchyTest {

    private static final long TEST_SHOP = 1337;

    @Spy
    @Autowired
    TicketService ticketService;
    @Autowired
    InboxService inboxService;

    @Autowired
    @InjectMocks
    TicketManager ticketManager;
    @Mock
    CalendarService calendarService;
    @Mock
    WorkHour workHour;

    private final Map<String, String> tableCountQueries = new HashMap<>();

    @BeforeEach
    void init() {
        openMocks(this);
        when(workHour.add(any(), anyInt())).then(inv -> {
            Object[] args = inv.getArguments();
            return new WorkHour(calendarService).add((Date) args[0], (int) args[1]);
        });
        when(ticketService.loadStraightFromDb(anyLong())).then(arg -> ticketService.loadTicketById((Long) arg.getArguments()[0]));
    }

    @Test
    void testDeleteCancelledTickets() {
        doReturn(new CalendarEntry(null, false, false, "пн")).when(calendarService).get(any(Date.class));

        Timestamp today = new Timestamp(System.currentTimeMillis());
        Timestamp goodOldDays = Timestamp.valueOf("2007-01-01 00:00:00");
        Ticket normalTicket = generateTicketWithDependencies(today, TicketStatus.OPEN);
        Ticket cancelledTicket = generateTicketWithDependencies(goodOldDays, TicketStatus.CANCELED);

        int normalTicketRows = countRowsWithTicket(normalTicket);
        int cancelledTicketRows = countRowsWithTicket(cancelledTicket);
        assertEquals(tableCountQueries.size(), normalTicketRows);
        assertEquals(tableCountQueries.size(), cancelledTicketRows);

        ticketManager.deleteTicketsCancelledByTime();
        normalTicketRows = countRowsWithTicket(normalTicket);
        cancelledTicketRows = countRowsWithTicket(cancelledTicket);
        assertEquals(tableCountQueries.size(), normalTicketRows);
        assertEquals(0, cancelledTicketRows);
    }

    @Test
    void testDeleteCancelledByPinger() {
        long ticketId = createTicket(777, GenId.CONTENT_SIZE_PINGER_GEN);
        Ticket created = ticketService.loadTicketById(ticketId);
        assertNotNull(created);

        created.setStatus(TicketStatus.CANCELED);
        ticketService.saveTicket(created, createTag(new Timestamp(0), 0));
        entityManager.flush();
        entityManager.clear();

        ticketManager.deleteCancelledPingerTickets();
        assertNull(ticketService.loadTicketById(ticketId));
    }

    @Test
    void testDeleteCancelledTicketWithProblem() {
        doReturn(new CalendarEntry(null, false, false, "пн")).when(calendarService).get(any(LocalDateTime.class));

        Ticket ticket = generateTicketWithDependencies(Timestamp.valueOf("2007-01-01 00:00:00"), TicketStatus.CANCELED);
        problemRepo.save(Problem.newBuilder()
                .ticketId(ticket.getId())
                .problemTypeId(ProblemTypeId.HIGHER_PRICE)
                .status(ProblemStatus.NEW)
                .build()
        );
        entityManager.flush();

        ticketManager.deleteTicketsCancelledByTime();
        int count = countRowsWithTicket(ticket);
        assertEquals(tableCountQueries.size(), count);
    }

    @ParameterizedTest
    @MethodSource("cancelTicketTestMethodSource")
    void cancelTicketTest(TicketStatus status, boolean inInbox) {
        var ticket = generateTicketWithDependencies(new Timestamp(System.currentTimeMillis()), status);
        if (inInbox) {
            inboxService.putTicketToInbox(ticket.getId(), -1);
        }
        var cancelled = ticketManager.cancelTicket(ticket, RND.nextLong(), true);
        assertEquals(!inInbox && status == TicketStatus.NEW, cancelled);
    }

    private static Stream<Arguments> cancelTicketTestMethodSource() {
        return StreamEx.of(TicketStatus.NEW, TicketStatus.OPEN, TicketStatus.FINISHED)
                .cross(true, false)
                .mapKeyValue(Arguments::of);
    }

    private Ticket generateTicketWithDependencies(Timestamp timestamp, TicketStatus status) {
        Hypothesis hypothesis = new Hypothesis(TEST_SHOP, 0, 0, "", 1, 1, "");
        hypothesisService.createHypothesis(hypothesis);

        TicketTag modificationTag = createTag(timestamp, FakeUsers.TICKET_CANCELLATOR_BY_TIME.getId());

        Offer offer = createOffer();
        offerStateManager.rememberCurrentOfferState(offer, modificationTag.getId());

        Ticket ticket = new Ticket(hypothesis, offer.getId(), 213, CheckMethod.BASKET);
        ticket.setCreationTime(DateUtils.addDays(timestamp, -1));
        ticket.setStatus(status);
        ticketService.saveTicket(ticket, modificationTag);

        entityManager.flush();
        return ticket;
    }

    TicketTag createTag(Date timestamp, long yaUid) {
        long id = DbUtils.getNextSequenceValuePg(jdbcTemplate, "s_core_tag");
        TicketTag tag = new TicketTag(yaUid);
        tag.setTime(timestamp);
        tag.setId(id);
        jdbcTemplate.update("INSERT INTO core_tag VALUES (?, ?, ?)", tag.getId(), tag.getTime(), tag.getYaUid());
        return tag;
    }

    private Offer createOffer() {
        Offer offer = new Offer();
        offer.setShopId(TEST_SHOP);
        offer.setName("name");
        offer.setShopOfferId("asdf");
        offer.setFeedId(1235L);
        offer.setFeedCategoryId("category");
        offer.setPriceCurrency(ru.yandex.common.util.currency.Currency.RUR);
        offer.setPrice(new BigDecimal(12345d));
        offer.setFeeShow("feeShow");
        offer.setPriorityRegionId(213L);
        offer.setOnStock(true);
        offer.setBaseGeneration("generation");
        offerDbService.storeOffer(offer);
        return offer;
    }

    private int countRowsWithTicket(Ticket ticket) {
        long tag = jdbcTemplate.queryForList("" +
                        "SELECT modification_tag_id " +
                        "FROM core_ticket " +
                        "WHERE hyp_id = ?",
                Long.class,
                ticket.getId()).stream().findFirst().orElse(-1L);

        tableCountQueries.clear();
        tableCountQueries.put("hypothesis", "id = " + ticket.getId());
        tableCountQueries.put("core_ticket", "hyp_id = " + ticket.getId());
        tableCountQueries.put("core_ticket_history", "hyp_id = " + ticket.getId());
        tableCountQueries.put("core_tag", "id = " + tag);
        tableCountQueries.put("core_offer", "id = " + ticket.getOfferId());
        tableCountQueries.put("core_offer_state", "offer_id = " + ticket.getOfferId());

        return tableCountQueries.entrySet().stream()
                .map(e -> "SELECT count(*) FROM " + e.getKey() + " WHERE " + e.getValue())
                .mapToInt(query -> jdbcTemplate.queryForObject(query, int.class))
                .sum();
    }
}
