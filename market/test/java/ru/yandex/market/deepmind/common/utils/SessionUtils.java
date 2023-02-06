package ru.yandex.market.deepmind.common.utils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import ru.yandex.market.deepmind.common.services.tracker_strategy.TicketResolution;
import ru.yandex.market.deepmind.common.services.tracker_strategy.TicketStatus;
import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.excel.ExcelFileConverter;
import ru.yandex.market.tracker.tracker.MockSession;
import ru.yandex.startrek.client.model.Comment;
import ru.yandex.startrek.client.model.IssueUpdate;
import ru.yandex.startrek.client.model.UserRef;

public class SessionUtils {

    private SessionUtils() {
    }

    public static void close(MockSession session, String ticket, TicketResolution resolution) {
        var status = session.issues().get(ticket).getStatus();
        var transition = session.transitions().getAll(ticket).stream()
            .filter(v -> v.getId().equals("close"))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Failed to find transition 'close' " +
                "from status '" + status + "'"));
        session.transitions().execute(ticket, transition,
            IssueUpdate.resolution(resolution.getResolutionAlias()).build());
    }

    public static void close(MockSession session, String ticket) {
        var status = session.issues().get(ticket).getStatus();
        var transition = session.transitions().getAll(ticket).stream()
            .filter(v -> v.getId().equals("close"))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Failed to find transition 'close' " +
                "from status '" + status + "'"));
        session.transitions().execute(ticket, transition);
    }

    public static void check(MockSession session, String ticket) {
        var status = session.issues().get(ticket).getStatus();
        var transition = session.transitions().getAll(ticket).stream()
            .filter(v -> v.getId().equals("check"))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Failed to find transition 'check' " +
                "from status '" + status + "'"));
        session.transitions().execute(ticket, transition);
    }

    public static void autoRule(MockSession session, String ticket) {
        var status = session.issues().get(ticket).getStatus();
        var transition = session.transitions().getAll(ticket).stream()
            .filter(v -> v.getId().equals(TicketStatus.AUTO_RULES.getStatusAliases().get(0)))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Failed to find transition 'check' " +
                "from status '" + status + "'"));
        session.transitions().execute(ticket, transition);
    }

    public static void awaitsActivation(MockSession session, String ticket) {
        var status = session.issues().get(ticket).getStatus();
        var transition = session.transitions().getAll(ticket).stream()
            .filter(v -> v.getId().equals("awaitingForActivation"))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Failed to find transition 'awaitingForActivation' " +
                "from status '" + status + "'"));
        session.transitions().execute(ticket, transition);
    }

    public static void addExcelAttachment(MockSession session, String issue, String name, Instant createdAt,
                                          ExcelFile excelFile, UserRef createdBy) {
        var stream = ExcelFileConverter.convert(excelFile);
        session.attachments().add(issue, name, stream, createdAt, createdBy);
    }

    public static String getLastComment(MockSession session, String ticket) {
        var list = getComments(session, ticket);
        if (list.isEmpty()) {
            throw new IllegalStateException("No comments in ticket: " + ticket);
        }
        return list.get(list.size() - 1).getText().orElse("");
    }

    public static List<Comment> getComments(MockSession session, String issueKey) {
        var iterator = session.comments().getAll(issueKey);
        var list = new ArrayList<Comment>();
        iterator.forEachRemaining(list::add);
        return list;
    }
}
