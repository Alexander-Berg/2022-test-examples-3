from search.mon.warden.tests.utils.clients.startrek import MockTicket


def create_ticket_link(ticket_1: MockTicket, ticket_2: MockTicket):
    ticket_1.postpone_link(ticket_2)
    ticket_2.postpone_link(ticket_1)


def delete_ticket_link(ticket_1: MockTicket, ticket_2: MockTicket):
    ticket_1.delete_link(ticket_2)
    ticket_2.delete_link(ticket_1)
