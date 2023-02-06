package ru.yandex.market.tpl.common.startrek.domain;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;

import static ru.yandex.market.tpl.common.util.StringFormatter.sf;

/**
 * Тестовые классы для отслеживаемых тикетов.
 * Импортировать в тесты только при использовании StartrekModuleFullConfiguration!
 */
public class TestTicketStateFactory {

    private static final AtomicLong TICKET_COUNTER = new AtomicLong(1);

    private static final String DEFAULT_QUEUE = "TICKET";
    private static final String DEFAULT_STATUS = "open";

    @Autowired
    private TicketStateCommandService ticketStateCommandService;

    public TicketState createTicket() {
        return createTicket(sf("{}-{}", DEFAULT_QUEUE, TICKET_COUNTER.getAndIncrement()), DEFAULT_STATUS);
    }

    public TicketState createTicket(String key) {
        return createTicket(key, DEFAULT_STATUS);
    }

    public TicketState createTicket(String key, String status) {
        return ticketStateCommandService.listen(key, status);
    }

}
