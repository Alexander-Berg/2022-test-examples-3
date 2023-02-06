package ru.yandex.market.crm.operatorwindow;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.operatorwindow.jmf.entity.MarketTicket;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.automation.test.utils.AutomationRuleTestUtils;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.InternalComment;
import ru.yandex.market.jmf.module.comment.UserComment;
import ru.yandex.market.jmf.module.mail.MailConnection;
import ru.yandex.market.jmf.module.mail.test.impl.MailMessageBuilderService;
import ru.yandex.market.jmf.module.ou.impl.OuTestUtils;
import ru.yandex.market.jmf.module.ticket.MailTicketCommentType;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.TicketContactInComment;
import ru.yandex.market.jmf.module.ticket.TicketDeduplicationAlgorithm;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.utils.Maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.jmf.module.ticket.MailConnection.COMMENT_TYPE;
import static ru.yandex.market.jmf.module.ticket.MailConnection.DEDUPLICATION_ALGORITHM;
import static ru.yandex.market.jmf.module.ticket.MailConnection.TICKET_TYPE;
import static ru.yandex.market.jmf.module.ticket.MailConnection.USE_DEFAULT_SCRIPT;

@Transactional
public class MailAutomationRulesTest extends AbstractMailProcessingTest {

    private static final String MARKET_MAIL_CONNECTION = "market";
    private static final String BERU_MAIL_CONNECTION = "beru";
    private static final String MAIL_AUTOMATION_RULE_GROUP = "mail";
    private static final String MAIL_AUTOMATION_TEST_SERVICE = "automationRuleTestService";
    private static final String MAIL_AUTOMATION_TEST_VIP_SERVICE = "automationRuleTestVipService";
    private static final String MARKET_QUESTION_SERVICE = "marketQuestion";

    @Inject
    protected AutomationRuleTestUtils automationRuleTestUtils;
    @Inject
    private OuTestUtils ouTestUtils;

    private MailConnection marketConnection;
    private MailConnection beruConnection;

    private static Stream<Arguments> processCommonMailCommentTypeData() {
        return Stream.of(
                Arguments.of(MailTicketCommentType.USER_COMMENT_TYPE, UserComment.FQN),
                Arguments.of(MailTicketCommentType.INTERNAL_COMMENT_TYPE, InternalComment.FQN),
                Arguments.of(MailTicketCommentType.TICKET_CONTACT_IN_COMMENT_TYPE, TicketContactInComment.FQN),
                Arguments.of(null, UserComment.FQN)
        );
    }

    @BeforeEach
    public void prepareData() {
        ouTestUtils.createOu();
        marketConnection = mailTestUtils.createMailConnection(MARKET_MAIL_CONNECTION);
        beruConnection = mailTestUtils.createMailConnection(BERU_MAIL_CONNECTION);
        ServiceTime st = serviceTimeTestUtils.createServiceTime24x7();
        ticketTestUtils.setServiceTime24x7("marketQuestion");
        ticketTestUtils.createService(Map.of(
                Service.CODE, MAIL_AUTOMATION_TEST_SERVICE,
                Service.SERVICE_TIME, st
        ));
        ticketTestUtils.createService(Map.of(
                Service.CODE, MAIL_AUTOMATION_TEST_VIP_SERVICE,
                Service.SERVICE_TIME, st
        ));
    }

    @Test
    public void positiveBranchRule() {
        createRule(marketConnection, "/automation_rules/mailRule.json");
        String sender = Randoms.email();
        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder = getMailMessageBuilder(MARKET_MAIL_CONNECTION);
        processMessage(mailMessageBuilder
                .setFrom(sender)
                .setSubject("Очень СРОЧНЫЙ вопрос")
                .build()
        );
        MarketTicket ticket = getSingleOpenedMarketTicket();
        assertEquals(MAIL_AUTOMATION_TEST_VIP_SERVICE, ticket.getService().getCode());
    }

    @Test
    public void connectionWithoutRules() {
        createRule(beruConnection, "/automation_rules/mailRule.json");
        String sender = Randoms.email();
        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder = getMailMessageBuilder(MARKET_MAIL_CONNECTION);
        processMessage(mailMessageBuilder
                .setFrom(sender)
                .setSubject("Очень СРОЧНЫЙ вопрос")
                .build()
        );
        MarketTicket ticket = getSingleOpenedMarketTicket();
        assertEquals(MARKET_QUESTION_SERVICE, ticket.getService().getCode());
    }

    /**
     * Проверка типа добавляемого комментария при обработке письма общим скриптом обработки почты:
     * processCommonInMailMessage.groovy
     * <p>
     * Подготовка:
     * - создаем подключение mailConnection
     * - Тип обращения: ticket$market
     * - Алгоритм дедубликации: Общий
     * - Использовать скрипт по умолчанию: да
     * - Тип комментария: <code>commentType</code>
     * - создаем правило автоматизации в mailConnection: заполнение очереди у обращения значением marketQuestion
     * <p>
     * Действия:
     * Отправлям письмо на mailConnection
     * <p>
     * Проверки:
     * - создано одно обращение типа ticket$market
     * - у обращения один комментарий
     * - тип комментария <code>expectedCommentType</code>
     *
     * @param commentType         тип коментария, указаннный в подключении
     * @param expectedCommentType ожидаемый тип добавленного комментария
     */
    @MethodSource("processCommonMailCommentTypeData")
    @ParameterizedTest(name = "comment type: {0}")
    public void processCommonMailWithUserComment(String commentType, Fqn expectedCommentType) {
        String connectionCode = Randoms.string();
        MailConnection mailConnection = mailTestUtils.createMailConnection(connectionCode, Maps.of(
                TICKET_TYPE, MarketTicket.FQN,
                DEDUPLICATION_ALGORITHM, TicketDeduplicationAlgorithm.COMMON_ALGORITHM,
                COMMENT_TYPE, commentType,
                USE_DEFAULT_SCRIPT, true
        ));
        createRule(mailConnection, "/automation_rules/setServiceRule.json");
        MailMessageBuilderService.MailMessageBuilder mailMessageBuilder = getMailMessageBuilder(connectionCode);
        processMessage(mailMessageBuilder.build());

        MarketTicket ticket = getSingleOpenedMarketTicket();
        assertEquals(MARKET_QUESTION_SERVICE, ticket.getService().getCode());
        assertSingleComment(ticket, expectedCommentType);
    }

    private MarketTicket getSingleOpenedMarketTicket() {
        return ticketTestUtils.getSingleOpenedTicket(MarketTicket.FQN);
    }

    private void createRule(Entity entity, String configPath) {
        automationRuleTestUtils.createApprovedEventRule(
                entity,
                configPath,
                MAIL_AUTOMATION_RULE_GROUP,
                Set.of(),
                Set.of(ouTestUtils.getAnyCreatedOu())
        );
    }

    private void assertSingleComment(Ticket ticket, Fqn fqn) {
        List<Comment> comments = dbService.list(Query.of(Comment.FQN)
                .withFilters(Filters.eq(Comment.ENTITY, ticket)));

        Assertions.assertEquals(1, comments.size());

        Comment comment = comments.get(0);
        Assertions.assertEquals(fqn, comment.getFqn());
    }
}
