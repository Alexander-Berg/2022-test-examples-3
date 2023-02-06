package ru.yandex.market.crm.operatorwindow;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import jdk.jfr.Description;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.operatorwindow.integration.Brands;
import ru.yandex.market.crm.operatorwindow.jmf.entity.BeruLogisticSupportRuleOnEdit;
import ru.yandex.market.crm.operatorwindow.jmf.entity.LogisticSupportRule;
import ru.yandex.market.crm.operatorwindow.jmf.entity.LogisticSupportTicket;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.InternalComment;
import ru.yandex.market.jmf.module.comment.test.impl.CommentTestUtils;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;
import ru.yandex.market.jmf.utils.Maps;

@Transactional
public class LogisticRulesTest extends AbstractModuleOwTest {

    private static final Fqn LOGISTIC_SUPPORT_TICKET_FQN = LogisticSupportTicket.FQN;

    @Inject
    private BcpService bcpService;
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private ServiceTimeTestUtils serviceTimeTestUtils;
    @Inject
    private CommentTestUtils commentTestUtils;

    @Test
    @Description("""
             Проверка смены статуса и добавления комментария при срабатывании logisticSupportRules
             тест-кейсы
             - https://testpalm2.yandex-team.ru/testcase/ocrm-993
             - https://testpalm2.yandex-team.ru/testcase/ocrm-992
            """)
    public void onChangeLogisticSupportRulesTransfersTicketToCorrespondingStatusAndAddComment() {
        final Team team = ticketTestUtils.createTeam();
        final Service service = ticketTestUtils.createService(Maps.of(
                Service.SERVICE_TIME, serviceTimeTestUtils.createServiceTime24x7(),
                Service.RESPONSIBLE_TEAM, team,
                Service.BRAND, Brands.LOGISTIC_SUPPORT
        ));

        Ticket ticket = ticketTestUtils.createTicket(LOGISTIC_SUPPORT_TICKET_FQN,
                team,
                service,
                Maps.of(
                        Ticket.STATUS, Ticket.STATUS_REGISTERED,
                        "@comment", Maps.of(
                                Comment.METACLASS, InternalComment.FQN,
                                Comment.BODY, Randoms.string()
                        )
                )
        );
        Assertions.assertEquals(Ticket.STATUS_REGISTERED, ticket.getStatus());

        //Если описание обращения содержит "hello", то статус обращения меняется на "PROCESSING"
        createLogisticSupportRule(
                Set.of("hello"),
                Ticket.STATUS_PROCESSING,
                null
        );
        //Если описание обращения содержит "hello", то добавляется комментарий к обращению
        LogisticSupportRule firstLogisticSupportRule = createLogisticSupportRule(
                Set.of("hello"),
                null,
                Randoms.string()
        );

        //правила срабатывают на изменение обращения
        bcpService.edit(ticket, Maps.of(
                Ticket.DESCRIPTION, "Hello world"
        ));

        Comment comment = commentTestUtils.getLastComment(ticket, Comment.FQN);

        Assertions.assertEquals(Ticket.STATUS_PROCESSING, ticket.getStatus());
        Assertions.assertEquals(InternalComment.FQN, comment.getFqn());
        Assertions.assertEquals(
                firstLogisticSupportRule.getAttribute(LogisticSupportRule.COMMENT_BODY),
                comment.getTextBody()
        );
    }

    private LogisticSupportRule createLogisticSupportRule(
            Set<String> description,
            String targetTicketStatus,
            String commentBody
    ) {
        Map<String, Object> attributes = Maps.of(
                LogisticSupportRule.TITLE, "Some rule",
                LogisticSupportRule.DESCRIPTION, description,
                LogisticSupportRule.TICKET_STATUS, targetTicketStatus,
                LogisticSupportRule.COMMENT_BODY, commentBody
        );
        return bcpService.create(BeruLogisticSupportRuleOnEdit.FQN, attributes);
    }
}
