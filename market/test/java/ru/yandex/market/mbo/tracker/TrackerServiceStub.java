package ru.yandex.market.mbo.tracker;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import ru.yandex.startrek.client.model.SearchRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TrackerServiceStub implements TrackerService {

    private final Map<String, AtomicInteger> ticketKeyIndexes = new HashMap<>();
    private final Map<String, Ticket> tickets = new HashMap<>();
    private final Map<String, String> links = new HashMap<>();
    private final Multimap<String, String> comments = HashMultimap.create();

    @Override
    public Ticket getTicket(String ticketKey) {
        return new Ticket(tickets.get(ticketKey));
    }

    @Override
    public Ticket createTicket(Ticket ticket) {
        AtomicInteger idGenerator =
            ticketKeyIndexes.computeIfAbsent(ticket.getQueue(), (k) -> new AtomicInteger());
        int id = idGenerator.incrementAndGet();
        String key = ticket.getQueue() + "-" + id;

        Ticket newTicket = new Ticket(ticket);
        newTicket.setKey(key);
        newTicket.setTicketStatus(TicketStatus.OPEN);
        tickets.put(key, newTicket);

        return newTicket;
    }

    @Override
    public Ticket updateTicket(Ticket ticket) {
        Ticket newTicket = new Ticket(ticket);
        tickets.put(ticket.getKey(), new Ticket(ticket));
        return newTicket;
    }

    @Override
    public void comment(Ticket ticket, TicketComment ticketComment) {
        comments.put(ticket.getKey(), ticketComment.getComment());
    }

    @Override
    public void comment(String ticketKey, TicketComment ticketComment) {
        comments.put(ticketKey, ticketComment.getComment());
    }

    @Override
    public void linkTickets(String ticketKey1, String ticketKey2) {
        links.put(ticketKey1, ticketKey2);
        links.put(ticketKey2, ticketKey1);
    }

    @Override
    public boolean isLinked(String key1, String key2) {
        return key2.equals(links.get(key1)) || key1.equals(links.get(key2));
    }

    @Override
    public List<Ticket> findTickets(SearchRequest request) {
        List<Ticket> filteredTickets = new ArrayList<>(tickets.values());
        for (Map.Entry<String, Object> filter : request.getFilter().entrySet()) {
            switch (filter.getKey()) {
                case TrackerConstants.QUEUE:
                    filteredTickets = filteredTickets.stream()
                        .filter(t -> t.getQueue().equals(filter.getValue()))
                        .collect(Collectors.toList());
                    break;
                case TrackerConstants.TAGS:
                    filteredTickets = filteredTickets.stream()
                        .filter(t -> t.getTags().contains(filter.getValue()))
                        .collect(Collectors.toList());
                    break;
                case TrackerConstants.RESOLUTION:
                    break;
                default:
                    throw new UnsupportedOperationException("Filter " + filter.getKey() + " is not supported");
            }
        }
        return filteredTickets;
    }

    public Collection<String> getComments(Ticket ticket) {
        return comments.get(ticket.getKey());
    }

    public Collection<Ticket> getAllTickets() {
        return tickets.values();
    }
}
