package ru.yandex.market.crm.operatorwindow.socialmessaging;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import ru.yandex.market.crm.operatorwindow.AbstractModuleOwTest;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.angry.InSmmObject;
import ru.yandex.market.jmf.module.angry.OutSmmObject;
import ru.yandex.market.jmf.module.angry.SmmAccount;
import ru.yandex.market.jmf.module.angry.SmmObject;
import ru.yandex.market.jmf.module.angry.SocialMessagingComment;
import ru.yandex.market.jmf.module.angry.SocialMessagingInComment;
import ru.yandex.market.jmf.module.angry.SocialMessagingOutComment;
import ru.yandex.market.jmf.module.angry.Ticket;
import ru.yandex.market.jmf.module.angry.controller.v1.model.AccountObject;
import ru.yandex.market.jmf.module.angry.controller.v1.model.Provider;
import ru.yandex.market.jmf.module.angry.impl.AngrySpaceServiceImpl;
import ru.yandex.market.jmf.module.angry.impl.MockAngrySpaceClient;
import ru.yandex.market.jmf.module.angry.impl.SocialMessagingTestUtils;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.CommentsService;
import ru.yandex.market.jmf.module.comment.test.impl.CommentTestUtils;
import ru.yandex.market.jmf.module.ticket.Brand;
import ru.yandex.market.jmf.module.ticket.TicketCategory;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.trigger.impl.TriggerServiceImpl;
import ru.yandex.market.jmf.utils.Maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
public abstract class AbstractSocialMessagingTest extends AbstractModuleOwTest {

    protected static final String STATUS_ESCALATED = "escalated";
    protected static final String STATUS_DEESCALATED = "deescalated";

    protected static final String SOCIAL_MESSAGING_FIRST_LINE = "socialMessagingFirstLine";
    protected static final String SOCIAL_MESSAGING_SECOND_LINE = "socialMessagingSecondLine";

    protected static final long FACEBOOK_ACCOUNT_ID = 40;
    protected static final long OK_ACCOUNT_ID = 50;
    protected static final long VK_ACCOUNT_ID = 60;

    protected static final String TEXT_FIELD_NAME = "text";

    protected Brand beruBrand;
    protected TicketCategory testBeruTicketCategory;

    @Inject
    protected CommentTestUtils commentTestUtils;
    @Inject
    protected EntityStorageService storage;
    @Inject
    protected SocialMessagingTestUtils utils;
    @Inject
    protected MockAngrySpaceClient mockAngrySpaceClient;
    @Inject
    protected BcpService bcpService;
    @Inject
    protected AngrySpaceServiceImpl angrySpaceService;
    @Inject
    protected TriggerServiceImpl triggerService;
    @Inject
    protected TicketTestUtils ticketTestUtils;
    @Inject
    protected CommentsService commentsService;


    @BeforeEach
    void setUp() {
        mockAngrySpaceClient.clear();

        var accountObjectFacebook = new AccountObject(FACEBOOK_ACCOUNT_ID, Provider.FACEBOOK, null,
                "facebookAccount", "facebookAccount", null);
        utils.createSmmAccount(accountObjectFacebook, mockAngrySpaceClient);

        var accountObjectVk = new AccountObject(VK_ACCOUNT_ID, Provider.VK, null,
                "vkAccount", "vkAccount", null);
        utils.createSmmAccount(accountObjectVk, mockAngrySpaceClient);

        var accountObjectOk = new AccountObject(OK_ACCOUNT_ID, Provider.OK, null,
                "okAccount", "okAccount", null);
        utils.createSmmAccount(accountObjectOk, mockAngrySpaceClient);
        beruBrand = storage.getByNaturalId(Brand.FQN, "beru");
        testBeruTicketCategory = ticketTestUtils.createTicketCategory(beruBrand);
    }

    protected List<Comment> getComments(Ticket ticket) {
        return commentTestUtils.getComments(ticket);
    }

    protected Ticket getTicket(SmmObject smmObject) {
        return storage.get(smmObject.getEntity());
    }

    protected long getTicketsCount() {
        return storage.count(Query.of(Ticket.FQN));
    }

    protected SocialMessagingInComment verifyInboundComment(SmmObject smmObject, String expectedBody) {
        var comment = verifyInboundComment(smmObject);

        assertEquals(expectedBody, comment.getBody());
        return comment;
    }

    private SocialMessagingInComment verifyInboundComment(SmmObject smmObject) {
        SocialMessagingComment comment = utils.getComment(smmObject);
        assertTrue(comment.getMetaclass().equalsOrDescendantOf(SocialMessagingInComment.FQN));
        var smmInComment = (SocialMessagingInComment) comment;
        assertEquals(smmObject.getContent().getAuthor().getName(), smmInComment.getUserName());
        return smmInComment;
    }

    protected SocialMessagingInComment verifyInboundComment(SmmObject smmObject, String... expectedBodyParts) {
        var comment = verifyInboundComment(smmObject);

        for (String expectedBodyPart : expectedBodyParts) {
            assertTrue(comment.getBody().contains(expectedBodyPart));
        }

        return comment;
    }

    protected void assertTicketCount(int count) {
        assertEquals(count, ticketTestUtils.getAllActiveTickets(Ticket.FQN).size());
    }

    protected <T extends SmmObject> T verifyCreateSmmObject(Supplier<T> smmObjectCreator, SmmAccount expectedAccount) {
        int createdSmmObjectsCountBefore = utils.getCreatedSmmObjects(SmmObject.FQN).size();
        T createdObject = smmObjectCreator.get();
        // Правильность заполнения smmObject производится в AngrySpaceServiceTest, тут проверим только основное
        assertEquals(expectedAccount, createdObject.getAccount());
        assertEquals(createdSmmObjectsCountBefore + 1,
                utils.getCreatedSmmObjects(SmmObject.FQN).size());
        return createdObject;
    }

    protected <T extends SmmObject> T verifyCommentCreatedInExistedTicketByReceivedObject(Ticket existedTicket,
                                                                                          Supplier<T> itemReceiver) {
        long ticketsCountBefore = getTicketsCount();
        T receivedObject = itemReceiver.get();
        assertEquals(ticketsCountBefore, getTicketsCount(), "Не должно создаться новых обращений");
        assertEquals(existedTicket, getTicket(receivedObject), "Новый комментарий должен попасть в существующее " +
                "обращение");
        return receivedObject;
    }

    /**
     * Содержит проверку из тест-кейса https://testpalm.yandex-team.ru/testcase/ocrm-1485
     */
    protected <T extends SmmObject> T verifyTicketCreatedByReceivedSmmObject(Supplier<T> smmObjectReceiver,
                                                                             String expectedService) {
        T receivedObject = smmObjectReceiver.get();

        Ticket createdTicket = storage.get(receivedObject.getEntity());
        var comments = commentTestUtils.getComments(createdTicket);
        assertEquals(1, comments.size());
        assertEquals(receivedObject.getContent().getCreatedAt(), comments.get(0).getCreationTime().toInstant());
        assertEquals(receivedObject.getContent().getUrl(), createdTicket.getSourceLink().getHref());
        assertEquals(expectedService, createdTicket.getService().getCode());

        return receivedObject;
    }

    /**
     * Не содержит настройку мока, только отправляет комментарий, в тестах следует использовать методы из
     * специфических методов
     * {@link AbstractSocialMessagingReplyTest#createCommentAndVerifySentSmmItem} или
     * {@link AbstractSocialMessagingChatTest#createCommentAndVerifySentSmmMessage}
     */
    protected <T extends SmmObject> T createCommentAndVerifySentSmmObject(Ticket ticket,
                                                                          JsonNode contentToSend) {
        int inCountBefore = utils.getCreatedSmmObjects(InSmmObject.FQN).size();
        int outCountBefore = utils.getCreatedSmmObjects(OutSmmObject.FQN).size();
        int commentsCountBefore = getComments(ticket).size();

        String commentBody = contentToSend.get(TEXT_FIELD_NAME).asText();

        var comment = (SocialMessagingOutComment) commentsService.create(
                ticket,
                commentBody,
                SocialMessagingOutComment.FQN,
                Map.of());

        TransactionSynchronizationUtils.triggerBeforeCommit(false);
        TransactionSynchronizationUtils.triggerAfterCompletion(0);

        SmmObject smmObject = comment.getSmmObject();
        assertEquals(commentBody, smmObject.getContent().getText());

        assertNotNull(smmObject);
        assertEquals(ticket.getGid(), comment.getSmmObject().getEntity());
        assertEquals(inCountBefore, utils.getCreatedSmmObjects(InSmmObject.FQN).size(),
                "Количество входящих объектов не должно измениться, должны проигнорировать отправку копии");
        assertEquals(outCountBefore + 1, utils.getCreatedSmmObjects(OutSmmObject.FQN).size(),
                "Должны создать новый исходящий объект smm по комментарию");
        assertEquals(commentsCountBefore + 1, getComments(ticket).size());

        return (T) comment.getSmmObject();
    }

    private void deleteAll(Fqn fqn) {
        var objects = storage.list(Query.of(fqn));

        for (Entity object : objects) {
            bcpService.delete(object);
        }
    }

    private void dropValuesInColumn(Fqn fqn, String attribute) {
        var objects = storage.list(Query.of(fqn));

        for (Entity object : objects) {
            bcpService.edit(object, Maps.of(attribute, null));
        }
    }
}
