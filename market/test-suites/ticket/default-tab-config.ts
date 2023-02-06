export const CASES = [
    {
        name: 'ocrm-580: Обращения исходящей телефонии',
        expectation: 'открываются на вкладке скрипт',
        script: `
    api.db.of('ticket')
        .withFilters(
            api.db.filters.eq('service', 'beruOutgoingCall'),
        )
    .limit(1)
    .get()
`,
        neededTab: 'data-tab-caption="Скрипты"',
    },
    {
        name: 'ocrm-581: Письменные обращения',
        expectation: 'открываются на вкладке сообщения',
        script: `
    api.db.of('ticket')
        .withFilters(
            api.db.filters.eq('service', 'beruQuestion'),
        )
    .limit(1)
    .get()
`,
        neededTab: 'data-tab-caption="Сообщения"',
    },
    {
        name: 'ocrm-582: Обращения входящей телефонии без заказа',
        expectation: 'открываются на вкладке поиск заказа',
        script: `
    api.db.of('ticket$firstLine')
        .withFilters(
            api.db.filters.eq('service', 'beruIncomingCall'),
            api.db.filters.eq('order', null),
        )
    .limit(1)
    .get()
`,
        neededTab: 'data-tab-caption="Поиск заказа"',
    },
    {
        name: 'ocrm-583: Обращения входящей телефонии с заказом',
        expectation: 'открываются на вкладке сообщения',
        script: `
    api.db.of('ticket$firstLine')
        .withFilters{
            eq('service', 'beruIncomingCall')
            not(eq('order', null))
        }
    .limit(1)
    .get()
`,
        neededTab: 'data-tab-caption="Сообщения"',
    },
];
