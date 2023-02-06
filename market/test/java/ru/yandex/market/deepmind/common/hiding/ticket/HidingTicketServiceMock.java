package ru.yandex.market.deepmind.common.hiding.ticket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.market.deepmind.common.hiding.configuration.CloseTicketConfiguration;
import ru.yandex.market.deepmind.common.hiding.configuration.CreateTicketConfiguration;
import ru.yandex.startrek.client.model.IssueCreate;

public class HidingTicketServiceMock implements HidingTicketService {
    private final Set<String> openTickets = new HashSet<>();
    private final Map<String, IssueCreate> openTicketsMap = new HashMap<>();
    private final Map<String, String> movedTickets = new HashMap<>();
    private int ticketId = 0;

    @Override
    public List<String> findOpenTickets(Collection<String> tickets) {
        return tickets.stream()
            .filter(openTickets::contains)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<String> findNewTicketKey(String oldTicket) {
       return Optional.ofNullable(movedTickets.get(oldTicket));
    }

    @Override
    public List<String> findOpenTicketsByCurrentUser() {
        return openTickets.stream()
            .map(t -> movedTickets.getOrDefault(t, t))
            .collect(Collectors.toList());
    }

    public List<IssueCreate> findOpenTicketsIssueCreateObjects() {
        return new ArrayList<>(openTicketsMap.values());
    }

    @Override
    public Ticket createTicket(CreateTicketConfiguration configuration) {
        var tags = new ArrayList<>(configuration.getCatTeams());
        var issueBuilder = IssueCreate.builder()
            .type(configuration.getTicketType())
            .queue(configuration.getQueue())
            .summary(configuration.getSummary())
            .description(configuration.getDescription())
            .tags(Cf.wrap(tags));
        var key = configuration.getQueue() + "-" + (++ticketId);
        openTickets.add(key);
        openTicketsMap.put(key, issueBuilder.build());
        return new Ticket("", key);
    }

    @Override
    public void setAssigneeAndSubscribers(Ticket ticket, CreateTicketConfiguration configuration) {
        // do nothing
    }

    @Override
    public void closeWithCommentTicket(String ticket, CloseTicketConfiguration configuration) {
        setMaxId(ticket);
        openTickets.remove(ticket);
        openTicketsMap.remove(ticket);
    }

    @Override
    public void closeWithCommentTicket(String ticket, String comment) {
        setMaxId(ticket);
        openTickets.remove(ticket);
        openTicketsMap.remove(ticket);
    }

    @Override
    public boolean isClosed(String ticket) {
        return !openTickets.contains(ticket);
    }

    public void setOpenTickets(String... tickets) {
        for (String ticket : tickets) {
            setMaxId(ticket);
        }

        this.openTickets.addAll(List.of(tickets));
    }

    private void setMaxId(String ticket) {
        var i = ticket.indexOf("-");
        var id = Integer.parseInt(ticket.substring(i + 1));
        setMaxId(id);
    }

    public void setMaxId(int lastId) {
        ticketId = Math.max(ticketId, lastId);
    }

    public void setMovedTickets(Map<String, String> movedTickets) {
        this.movedTickets.putAll(movedTickets);
    }
}
