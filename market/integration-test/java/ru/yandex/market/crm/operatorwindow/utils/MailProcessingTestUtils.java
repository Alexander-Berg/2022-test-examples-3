package ru.yandex.market.crm.operatorwindow.utils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.springframework.stereotype.Component;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.GidService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.test.impl.CommentTestUtils;
import ru.yandex.market.jmf.module.def.Contact;
import ru.yandex.market.jmf.module.relation.LinkedRelation;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.utils.Maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.jmf.logic.wf.bcp.WfConstants.SKIP_WF_STATUS_CHANGE_CHECKING_ATTRIBUTE;

@Component
public class MailProcessingTestUtils {

    @Inject
    public GidService gidService;

    @Inject
    protected DbService dbService;

    @Inject
    protected BcpService bcpService;

    @Inject
    protected CommentTestUtils commentTestUtils;

    public String createSubjectWithTicketAndPartnerNumbers(Ticket ticket, Contact partner) {
        return Randoms.string() + ", № OCRM-" + ticket.getId() + "-P" + gidService.parse(partner.getGid()).getId();
    }

    public String createSubjectWithTicketNumber(Ticket ticket) {
        return Randoms.string() + ", № " + ticket.getId();
    }

    public void assertLinkedRelation(Ticket source, Ticket target) {
        assertLinkedRelationCount(source, target, 1);
    }

    public void changeStatus(Ticket ticket, String status) {
        bcpService.edit(
                ticket,
                Map.of(Ticket.STATUS, status),
                Maps.of(SKIP_WF_STATUS_CHANGE_CHECKING_ATTRIBUTE, Boolean.TRUE)
        );
    }

    public List<Comment> getComments(Ticket ticket) {
        return commentTestUtils.getComments(ticket)
                .stream()
                .sorted(Comparator.comparing(Comment::getGid))
                .collect(Collectors.toList());
    }

    public void assertTicketCount(Fqn fqn, int expectedCount) {
        List<Ticket> tickets = dbService.list(Query.of(fqn).withFilters(
                Filters.ne(Ticket.STATUS, Ticket.STATUS_CLOSED)
        ));
        Assertions.assertEquals(expectedCount, tickets.size());
    }

    private void assertLinkedRelationCount(Ticket source, Ticket target, int count) {
        long actualCount = dbService.count(Query.of(LinkedRelation.FQN).withFilters(
                Filters.eq(LinkedRelation.SOURCE, source),
                Filters.eq(LinkedRelation.TARGET, target)
        ));
        assertEquals(count, actualCount);
    }
}
