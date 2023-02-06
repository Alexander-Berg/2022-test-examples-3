package ru.yandex.market.abo.core.screenshot;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.ticket.TicketService;
import ru.yandex.market.abo.core.ticket.TicketTagService;
import ru.yandex.market.abo.core.ticket.model.CheckMethod;
import ru.yandex.market.abo.core.ticket.model.Ticket;
import ru.yandex.market.abo.core.ticket.model.TicketTag;
import ru.yandex.market.abo.gen.HypothesisService;
import ru.yandex.market.abo.gen.model.Hypothesis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author kukabara
 */
@Transactional(transactionManager = "jpaPgTransactionManager")
public class ScreenshotRepositoryTest extends EmptyTest {
    public static final long USER_ID = 1L;
    @Autowired
    private ScreenshotRepository screenshotRepository;
    @Autowired
    private HypothesisService hypothesisService;
    @Autowired
    private TicketTagService tagService;
    @Autowired
    private TicketService ticketService;

    @Test
    public void testRepo() throws Exception {
        long entityId = 1;
        int entityTypeId = ScreenshotSource.TICKET_PROBLEM.getId();
        Screenshot save = screenshotRepository.save(new Screenshot(USER_ID, entityId, entityTypeId));
        assertNotNull(save.getId());

        Screenshot foundById = screenshotRepository.findByIdOrNull(save.getId());
        assertEquals(foundById.getEntityId(), save.getEntityId());
        assertEquals(foundById.getEntityTypeId(), save.getEntityTypeId());
        assertEquals(foundById.getId(), save.getId());
    }

    @Test
    public void testFindByEntityTypeIdAndEntityId() throws Exception {
        long entityId = 1;
        int entityTypeId = ScreenshotSource.TICKET_PROBLEM.getId();
        screenshotRepository.save(new Screenshot(USER_ID, entityId, entityTypeId));
        screenshotRepository.save(new Screenshot(USER_ID, entityId, entityTypeId));

        assertEquals(2, screenshotRepository.findByEntityTypeIdAndEntityIdAndStatusId(
                entityTypeId, entityId, ScreenshotStatus.PUBLISHED.getId()).size());
    }

    @Test
    public void testFindByEntityTypeIdAndEntityIdIn() throws Exception {
        int entityTypeId = ScreenshotSource.TICKET_PROBLEM.getId();
        long entityId1 = 1;
        long entityId2 = 2;
        screenshotRepository.save(new Screenshot(USER_ID, entityId1, entityTypeId));
        screenshotRepository.save(new Screenshot(USER_ID, entityId1, entityTypeId));
        screenshotRepository.save(new Screenshot(USER_ID, entityId2, entityTypeId));

        assertEquals(3,
                screenshotRepository.findByEntityTypeIdAndEntityIdIn(entityTypeId, Arrays.asList(entityId1, entityId2)).size()
        );
    }

    @Test
    void testFindScreenshotsWithoutTickets() {
        long entityId = 1234567;
        int entityTypeId = ScreenshotSource.TICKET.getId();
        long createdTicketId = createTicket();

        screenshotRepository.save(new Screenshot(USER_ID, createdTicketId, entityTypeId));
        screenshotRepository.save(new Screenshot(USER_ID, entityId, entityTypeId));

        var screenshotsWithoutTicket = screenshotRepository.findScreenshotsWithoutTickets(
                entityTypeId,
                PageRequest.of(0, 10, Sort.Direction.ASC, "modificationTime")
        );

        assertEquals(1, screenshotsWithoutTicket.size());
        assertNotEquals(createdTicketId, screenshotsWithoutTicket.get(0).getEntityId());
        assertEquals(entityId, screenshotsWithoutTicket.get(0).getEntityId());
    }

    private long createTicket() {
        Hypothesis hypothesis = new Hypothesis(-1L, 0, 97, null, 0, 0, "");
        hypothesisService.createHypothesis(hypothesis);

        TicketTag tag = tagService.createTag(1);
        Ticket t = new Ticket(hypothesis, null, 0, CheckMethod.DEFAULT);
        ticketService.saveTicket(t, tag);

        return t.getId();
    }
}
