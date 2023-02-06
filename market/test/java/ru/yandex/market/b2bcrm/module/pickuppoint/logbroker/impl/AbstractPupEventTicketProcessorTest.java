package ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.impl;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
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
import ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert;
import ru.yandex.market.jmf.logic.def.AttachmentsService;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.InternalComment;
import ru.yandex.market.jmf.module.comment.test.impl.CommentTestUtils;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.jmf.utils.html.Htmls;

@Transactional
@ExtendWith(SpringExtension.class)
abstract class AbstractPupEventTicketProcessorTest<T extends PupEventAwareTicket> {

    protected final PupEvent<?> event;

    protected PupEventProcessor processor;

    @Inject
    protected DbService dbService;

    @Inject
    protected BcpService bcpService;

    @Inject
    protected AttachmentsService attachmentsService;

    @Inject
    protected ServiceTimeTestUtils serviceTimeTestUtils;

    @Inject
    protected TicketTestUtils ticketTestUtils;

    @Inject
    protected CommentTestUtils commentTestUtils;

    @Inject
    protected Htmls htmls;

    protected AbstractPupEventTicketProcessorTest(String eventFilename) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        event = Exceptions.sneakyRethrow(() ->
                objectMapper.readValue(
                        AbstractPupEventTicketProcessorTest.class.getResource(eventFilename),
                        PupEvent.class
                )
        );
    }


    @BeforeEach
    public void setUp() {
        processor = getProcessor();
        Entity st = dbService.getByNaturalId(ServiceTime.FQN, CatalogItem.CODE, "b2b_9_21");
        serviceTimeTestUtils.createPeriod(st, "monday", "09:00", "21:00");
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
                        Comment.BODY, htmls.safeHtml(body),
                        Comment.ENTITY, entity.getGid()
                );
    }

    protected abstract void assertTicketAttributes(T ticket);

    protected abstract T createTicket(String title);

    protected abstract PupEventProcessor getProcessor();

    protected PickupPointOwner createAccount(Long legalPartnerId) {
        return bcpService.create(PickupPointOwner.FQN, Maps.of(
                PickupPointOwner.TITLE, Randoms.string(),
                PickupPointOwner.PUP_ID, legalPartnerId
        ));
    }
}
