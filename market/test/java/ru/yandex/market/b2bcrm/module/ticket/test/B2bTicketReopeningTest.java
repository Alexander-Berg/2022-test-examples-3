package ru.yandex.market.b2bcrm.module.ticket.test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import jdk.jfr.Description;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.b2bcrm.module.account.Shop;
import ru.yandex.market.b2bcrm.module.ticket.B2bLeadTicket;
import ru.yandex.market.b2bcrm.module.ticket.B2bTicket;
import ru.yandex.market.b2bcrm.module.ticket.B2bTicketReopeningStartrekQueue;
import ru.yandex.market.b2bcrm.module.ticket.test.config.B2bTicketTests;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityService;
import ru.yandex.market.jmf.entity.HasId;
import ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert;
import ru.yandex.market.jmf.module.comment.test.impl.CommentTestUtils;
import ru.yandex.market.jmf.module.mail.InMailMessage;
import ru.yandex.market.jmf.module.relation.LinkedRelation;
import ru.yandex.market.jmf.module.relation.Relation;
import ru.yandex.market.jmf.module.relation.impl.trigger.RelatedEntityEvent;
import ru.yandex.market.jmf.module.startrek.StartrekIssue;
import ru.yandex.market.jmf.module.startrek.StartrekQueue;
import ru.yandex.market.jmf.module.ticket.MailConnection;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.test.impl.TimerTestUtils;
import ru.yandex.market.jmf.trigger.TriggerConstants;
import ru.yandex.market.jmf.trigger.TriggerService;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.jmf.module.ticket.Ticket.STATUS_PROCESSING;
import static ru.yandex.market.jmf.module.ticket.Ticket.STATUS_REOPENED;

@B2bTicketTests
public class B2bTicketReopeningTest extends AbstractB2bMailProcessingCreationTest {

    private static final String ST_ISSUE_OPENED_STATUS_KEY = "opened";
    private static final String ST_ISSUE_OPENED_STATUS = "Открыт";
    private static final String ST_ISSUE_CLOSED_STATUS_KEY = "closed";
    private static final String ST_ISSUE_CLOSED_STATUS = "Закрыт";
    private static final String QUEUE = "TEST";
    private static final String B2B_CONNECTION = "b2b";
    @Inject
    protected TicketTestUtils ticketTestUtils;
    @Inject
    protected CommentTestUtils commentTestUtils;
    @Inject
    private TimerTestUtils timerTestUtils;
    @Inject
    private BcpService bcpService;
    @Inject
    private DbService dbService;
    @Inject
    private EntityService entityService;
    @Inject
    private TriggerService triggerService;

    @BeforeEach
    void setUp() {
        createB2bTicketRoutingRules();
        mailTestUtils.createMailConnection(B2B_CONNECTION, Map.of(MailConnection.DEFAULT_SERVICE, defaultService));
    }


    @Test
    @Description("""
            Обращения b2b лидов переоткрываются из статуса processing при поступлении ответа
            https://testpalm.yandex-team.ru/testcase/ocrm-1425
            """)
    @DisplayName("Корректная смена статуса на reopened при наличии номера b2bLead тикета в теме письма")
    public void b2bOutgoingTicketMailSubjectInTicketTest() {
        B2bLeadTicket ticket = createProcessingTicket();
        InMailMessage message = mailMessageBuilderService.getMailMessageBuilder(B2B_CONNECTION)
                .setSubject(Randoms.string() + ", № " + ticket.getId())
                .build();
        processMessage(message);

        assertThat(ticket.getStatus()).isEqualTo(STATUS_REOPENED);
    }

    @Test
    @Description("""
            Обращения b2b лидов переоткрываются из статуса processing при поступлении ответа
            https://testpalm.yandex-team.ru/testcase/ocrm-1425
            """)
    @DisplayName("Корректная смена статуса на reopened при наличии replyMessage b2bLead тикета в references письма")
    public void b2bOutgoingTicketMailSubjectReplyInReferencesTest() {
        B2bLeadTicket ticket = createProcessingTicket();
        InMailMessage message = mailMessageBuilderService.getMailMessageBuilder(B2B_CONNECTION)
                .setReferences(ticket.getReplyMessage())
                .build();
        processMessage(message);

        assertThat(ticket.getStatus()).isEqualTo(STATUS_REOPENED);
    }

    @Test
    public void reopenTicketOnStatusChange() {
        B2bTicket ticket = getTicket();
        StartrekQueue startrekQueue = getStartrekQueue();
        StartrekIssue startrekIssue = getStartrekIssue(startrekQueue);
        StartrekIssue startrekIssueOld = entityService.clone(startrekIssue);
        Relation linkedRelation = getLinkedRelation(ticket, startrekIssue);
        createB2bTicketReopeningStartrekQueue(ticket);

        changeStartrekIssueStatus(startrekIssue);

        ticket = executeTriggers(ticket, startrekIssue, startrekIssueOld, linkedRelation);

        Assertions.assertEquals(B2bTicket.STATUS_REOPENED, ticket.getStatus());
        Assertions.assertEquals(1, commentTestUtils.getComments(ticket).size());
    }

    @Test
    public void doNotReopenTicketOnStatusChange() {
        B2bTicket ticket = getTicket();
        StartrekQueue startrekQueue = getStartrekQueue();
        StartrekIssue startrekIssue = getStartrekIssueWithDoNotReopenTag(startrekQueue);
        StartrekIssue startrekIssueOld = entityService.clone(startrekIssue);
        Relation linkedRelation = getLinkedRelation(ticket, startrekIssue);
        createB2bTicketReopeningStartrekQueue(ticket);

        changeStartrekIssueStatus(startrekIssue);

        ticket = executeTriggers(ticket, startrekIssue, startrekIssueOld, linkedRelation);

        Assertions.assertEquals(B2bTicket.STATUS_CLOSED_WAIT_CONTIGUOUS, ticket.getStatus());
        Assertions.assertTrue(commentTestUtils.getComments(ticket).isEmpty());
    }

    @Test
    public void testReopenByIdInMailSubject() {
        B2bTicket ticket = getTicket();
        InMailMessage message = mailMessageBuilderService.getMailMessageBuilder(B2B_CONNECTION)
                .setSubject(Randoms.string() + ", № " + ticket.getId())
                .build();
        processMessage(message);
        Assertions.assertEquals(B2bTicket.STATUS_REOPENED, ticket.getStatus());
    }

    @Test
    public void testReopenByIdSuitNumberInMailSubject() {
        long suitNumber = Randoms.positiveLongValue();
        B2bTicket ticket = bcpService.edit(getTicket(), B2bTicket.SUITE_TICKET_NUMBER, suitNumber);
        InMailMessage message = mailMessageBuilderService.getMailMessageBuilder(B2B_CONNECTION)
                .setSubject(Randoms.string() + " [case:" + suitNumber + "]")
                .build();
        processMessage(message);
        Assertions.assertEquals(B2bTicket.STATUS_REOPENED, ticket.getStatus());
    }

    @Test
    public void testReopenMultipleByEmailReferences() {
        List<String> messageIds = Stream.generate(Randoms::string).limit(2).collect(Collectors.toList());
        List<B2bTicket> tickets = messageIds.stream()
                .map(reference -> bcpService.<B2bTicket>edit(getTicket(), Ticket.REPLY_MESSAGE, reference))
                .collect(Collectors.toList());
        String references = String.join(" ", messageIds) + " unknownReference";
        InMailMessage message = mailMessageBuilderService.getMailMessageBuilder(B2B_CONNECTION)
                .setReferences(references)
                .build();
        processMessage(message);
        EntityCollectionAssert.assertThat(tickets)
                .allHasAttributes(Ticket.STATUS, B2bTicket.STATUS_REOPENED);
    }

    @Test
    @Description("""
            B2b обращение переоткрывается, пробыв в статусе 'Ожидаем смежников' 24 часа.
            Необходимым условием является нахождение обращения в нужной очереди.
            """)
    @DisplayName("После нахождения сутки в статусе 'Ожидаем смежников' тикет переоткрыт")
    public void testReopenWhenClosedWaitContiguousForDay() {
        // Создаем тикеты
        B2bTicket ticket1 = getTicket();
        B2bTicket ticket2 = getTicket();
        // Создаем очереди
        var serviceWithSuitableCode = ticketTestUtils.createService(
                Map.of(Service.CODE, "PVZSupportRequestsFromCC"));
        var serviceWithRandomCode = ticketTestUtils.createService();
        ServiceTime serviceTime = serviceTimeTestUtils.createServiceTime8x7();
        bcpService.edit(serviceWithSuitableCode, Map.of(Service.SERVICE_TIME, serviceTime));
        bcpService.edit(serviceWithRandomCode, Map.of(Service.SERVICE_TIME, serviceTime));

        // Помещаем тикеты в очереди
        bcpService.edit(ticket1, Map.of(Ticket.SERVICE, serviceWithSuitableCode));
        bcpService.edit(ticket2, Map.of(Ticket.SERVICE, serviceWithRandomCode));

        // Вызываем срабатывание таймеров
        timerTestUtils.simulateTimerExpiration(
                ticket1.getGid(),
                B2bTicket.ALLOWANCE_REOPEN_TIMER_FROM_CLOSED_WAIT_CONTIGUOUS
        );
        timerTestUtils.simulateTimerExpiration(
                ticket2.getGid(),
                B2bTicket.ALLOWANCE_REOPEN_TIMER_FROM_CLOSED_WAIT_CONTIGUOUS
        );

        // Проверяем, что переоткрылся лишь тикет в нужной очереди
        Assertions.assertEquals(STATUS_REOPENED, ticket1.getStatus());
        Assertions.assertEquals(B2bTicket.STATUS_CLOSED_WAIT_CONTIGUOUS, ticket2.getStatus());
    }

    private void changeStartrekIssueStatus(StartrekIssue startrekIssue) {
        bcpService.edit(startrekIssue,
                Map.of(
                        StartrekIssue.STATUS_KEY, ST_ISSUE_CLOSED_STATUS_KEY,
                        StartrekIssue.STATUS, ST_ISSUE_CLOSED_STATUS
                )
        );
    }

    @NotNull
    private B2bLeadTicket createProcessingTicket() {
        B2bLeadTicket ticket = b2bTicketTestUtils.createB2bLead(Collections.emptyMap());
        ticketTestUtils.editTicketStatus(ticket, STATUS_PROCESSING);
        return ticket;
    }

    private B2bTicket getTicket() {
        return ticketTestUtils.createTicket(B2bTicket.FQN, Map.of(
                B2bTicket.STATUS, B2bTicket.STATUS_CLOSED_WAIT_CONTIGUOUS,
                B2bTicket.CATEGORIES, List.of(createCategory(ticketTestUtils.createBrand())),
                B2bTicket.PARTNER, getShop(),
                Ticket.CHANNEL, "mail"
                )
        );
    }

    private void createB2bTicketReopeningStartrekQueue(B2bTicket ticket) {
        bcpService.create(B2bTicketReopeningStartrekQueue.FQN, Map.of(
                B2bTicketReopeningStartrekQueue.CODE, QUEUE,
                B2bTicketReopeningStartrekQueue.TITLE, QUEUE,
                B2bTicketReopeningStartrekQueue.STARTREK_TICKET_STATUSES_LIST, List.of(ST_ISSUE_OPENED_STATUS,
                        ST_ISSUE_CLOSED_STATUS),
                B2bTicketReopeningStartrekQueue.SERVICES, List.of(ticket.getService()))
        );
    }

    @NotNull
    private B2bTicket executeTriggers(B2bTicket ticket, StartrekIssue startrekIssue, StartrekIssue startrekIssueOld,
                                      Relation linkedRelation) {
        triggerService.execute(new RelatedEntityEvent(
                ticket.getMetaclass(),
                startrekIssue.getMetaclass(),
                TriggerConstants.EDIT,
                startrekIssue,
                startrekIssueOld,
                ticket,
                linkedRelation)
        );
        ticket = dbService.get(ticket.getGid());
        triggerService.execute(new RelatedEntityEvent(
                ticket.getMetaclass(),
                startrekIssue.getMetaclass(),
                TriggerConstants.EDIT,
                startrekIssue,
                startrekIssue,
                ticket,
                linkedRelation)
        );
        return dbService.get(ticket.getGid());
    }

    @NotNull
    private StartrekIssue getStartrekIssueWithDoNotReopenTag(StartrekQueue startrekQueue) {
        return bcpService.create(StartrekIssue.FQN, Map.of(
                HasId.ID, "abc123",
                StartrekIssue.QUEUE, startrekQueue,
                StartrekIssue.STARTREK_ID, "TEST-123",
                StartrekIssue.CREATED_AT, LocalDateTime.now(),
                StartrekIssue.STATUS_KEY, ST_ISSUE_OPENED_STATUS_KEY,
                StartrekIssue.STATUS, ST_ISSUE_OPENED_STATUS,
                StartrekIssue.TAGS, List.of("ignore_status_change")
                )
        );
    }

    @NotNull
    private Relation getLinkedRelation(B2bTicket ticket, StartrekIssue startrekIssue) {
        return bcpService.create(LinkedRelation.FQN, Map.of(
                LinkedRelation.SOURCE, ticket,
                LinkedRelation.TARGET, startrekIssue));
    }

    @NotNull
    private StartrekIssue getStartrekIssue(StartrekQueue startrekQueue) {
        return bcpService.create(StartrekIssue.FQN, Map.of(
                HasId.ID, "abc123",
                StartrekIssue.QUEUE, startrekQueue,
                StartrekIssue.STARTREK_ID, "TEST-123",
                StartrekIssue.CREATED_AT, LocalDateTime.now(),
                StartrekIssue.STATUS_KEY, ST_ISSUE_OPENED_STATUS_KEY,
                StartrekIssue.STATUS, ST_ISSUE_OPENED_STATUS
                )
        );
    }

    @NotNull
    private StartrekQueue getStartrekQueue() {
        return bcpService.create(StartrekQueue.FQN, Map.of(
                StartrekQueue.CODE, QUEUE,
                StartrekQueue.TITLE, QUEUE
                )
        );
    }

    @NotNull
    private Entity getShop() {
        return bcpService.create(Shop.FQN, Map.of(
                Shop.TITLE, "Test Shop " + Randoms.stringNumber(),
                Shop.SHOP_ID, Randoms.stringNumber(),
                Shop.EMAILS, Collections.singletonList("test1@ya.ru"),
                Shop.CAMPAIGN_ID, Randoms.stringNumber()
        ));
    }

    @Test
    @Disabled("Доработки будут в рамках https://st.yandex-team.ru/OCRM-6696")
    public void reopenTicketOnAddComment() {
        Assertions.assertTrue(true);
    }
}
