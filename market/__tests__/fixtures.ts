export const validBreadcrumbs = [
    '*',
    'ticket',
    'ticket-order',
    'ticket-order-user',
    'someTicket',
    'someTicket-someOrder-someUser',
];

export const invalidBreadcrumbs = [
    ' * ',
    '**-ticket-order',
    '123123',
    '123t-123order-123call',
    '',
    '* 123',
    '102>*',
    'ticket>>ticket',
    '?>ticket',
    'ticket>',
    'ticket@123-ticket',
];
