package ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.impl;

import java.io.IOException;
import java.io.UncheckedIOException;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.b2bcrm.module.pickuppoint.PickupPointOwner;
import ru.yandex.market.b2bcrm.module.pickuppoint.PupEventAwareTicket;
import ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.PupEventProcessor;
import ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.model.PupEvent;
import ru.yandex.market.crm.util.Exceptions;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.catalog.items.CatalogItem;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.query.SortingOrder;
import ru.yandex.market.jmf.entity.test.assertions.EntityAssert;
import ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.InternalComment;
import ru.yandex.market.jmf.module.comment.test.impl.CommentTestUtils;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;
import ru.yandex.market.jmf.utils.Maps;

@Transactional
@ExtendWith(SpringExtension.class)
abstract class AbstractPupEventProcessorTest<T extends PupEventAwareTicket> {

    protected final PupEvent<?> event;

    protected PupEventProcessor processor;

    @Inject
    protected DbService dbService;

    @Inject
    protected BcpService bcpService;

    @Inject
    protected ServiceTimeTestUtils serviceTimeTestUtils;

    @Inject
    protected TicketTestUtils ticketTestUtils;

    @Inject
    protected CommentTestUtils commentTestUtils;

    protected AbstractPupEventProcessorTest(String eventFilename) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        event = Exceptions.sneakyRethrow(() ->
                objectMapper.readValue(
                        AbstractPupEventProcessorTest.class.getResource(eventFilename),
                        PupEvent.class
                )
        );
    }


    @BeforeEach
    public void setUp() {
        processor = createProcessor();
        Entity st = dbService.getByNaturalId(ServiceTime.FQN, CatalogItem.CODE, "b2b_9_21");
        serviceTimeTestUtils.createPeriod(st, "monday", "09:00", "21:00");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReopenResolvedTicket() {
        T resolvedTicket = (T) ticketTestUtils.editTicketStatus(
                createTicket("Решенный"),
                Ticket.STATUS_RESOLVED
        );
        processor.process(event);
        assertTicketAttributes(resolvedTicket);
        EntityAssert.assertThat(resolvedTicket)
                .hasAttributes(PupEventAwareTicket.STATUS, Ticket.STATUS_REOPENED);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReopenClosedTicket() {
        T closed = (T) ticketTestUtils.editTicketStatus(
                createTicket("Закрытый"),
                Ticket.STATUS_CLOSED
        );
        processor.process(event);
        assertTicketAttributes(closed);
        EntityAssert.assertThat(closed)
                .hasAttributes(PupEventAwareTicket.STATUS, Ticket.STATUS_REOPENED);
    }

    protected T getLastTicket() {
        Query query = Query.of(PupEventAwareTicket.FQN)
                .withSortingOrder(SortingOrder.desc(PupEventAwareTicket.CREATION_TIME));
        return dbService.<T>list(query).get(0);
    }


    protected void checkCommentAdded(Entity entity, String body) {
        EntityCollectionAssert.assertThat(commentTestUtils.getCommentsOfType(entity, InternalComment.class))
                .hasSize(1)
                .allHasAttributes(
                        Comment.BODY, body,
                        Comment.ENTITY, entity.getGid()
                );
    }

    protected abstract void assertTicketAttributes(T ticket);

    protected abstract T createTicket(String title);

    protected abstract PupEventProcessor createProcessor();

    protected PickupPointOwner createAccount(Long legalPartnerId) {
        return bcpService.create(PickupPointOwner.FQN, Maps.of(
                PickupPointOwner.TITLE, Randoms.string(),
                PickupPointOwner.PUP_ID, legalPartnerId
        ));
    }

    @SuppressWarnings("ConstantConditions")
    static String getStringResource(String filename) {
        try {
            return new String(AbstractPupEventProcessorTest.class.getResourceAsStream(filename).readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
