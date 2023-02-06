module.exports = [
    {
        group: 'womTicket (TEST-1234)',
        tests: [
            {
                markup: 'ABCDEFGHIJKLMNOPQRST-1234',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 25,
                            column: 26,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 25,
                                    column: 26,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womTicket',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 25,
                                            column: 26,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://st.yandex-team.ru/ABCDEFGHIJKLMNOPQRST-1234',
                                    value: 'ABCDEFGHIJKLMNOPQRST-1234',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'ABCDEFGHIJKLMNOPQRSTU-1234',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 26,
                            column: 27,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 26,
                                    column: 27,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 26,
                                            column: 27,
                                            line: 1,
                                        },
                                    },
                                    value: 'ABCDEFGHIJKLMNOPQRSTU-1234',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'https://st.yandex-team.ru/WIKI-1234#test',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 40,
                            column: 41,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 40,
                                    column: 41,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womTicket',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 40,
                                            column: 41,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://st.yandex-team.ru/WIKI-1234#test',
                                    value: 'WIKI-1234',
                                },
                            ],
                        },
                    ],
                },
            },
        ],
    },
    {
        group: 'Ссылки (link, image, ticket, staff, club)',
        tests: [
            {
                markup: 'https://st.yandex-team.ru/STARTREK-14991[ --Починить автотесты: Редактирование полей тикета-- ]( rodzewich )\n',
                title: 'Сложная ссылка с тикетом',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 109,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 109,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womTicket',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 108,
                                            column: 109,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://st.yandex-team.ru/STARTREK-14991',
                                    value: 'STARTREK-14991',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'QUEUE-1234\n',
                title: 'Короткая ссылка на тикет',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 11,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 11,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womTicket',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://st.yandex-team.ru/QUEUE-1234',
                                    value: 'QUEUE-1234',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'QUEUE-1234[ --Важный тикет: "Сделать!"-- ]( mrtwister )\n',
                title: 'Ссылка на тикет с расширенным синтаксисом (WIKI-11815)',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 56,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 56,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womTicket',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 55,
                                            column: 56,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://st.yandex-team.ru/QUEUE-1234',
                                    value: 'QUEUE-1234',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'https://jira.yandex-team.ru/QUEUE-1234[ "Важный тикет: Сделать ](  )\n',
                title: 'Полная ссылка с realm без assignee (WIKI-11815)',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 69,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 69,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womTicket',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 68,
                                            column: 69,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://jira.yandex-team.ru/QUEUE-1234',
                                    value: 'QUEUE-1234',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'QUEUE-1234[\n',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 12,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 12,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womTicket',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://st.yandex-team.ru/QUEUE-1234',
                                    value: 'QUEUE-1234',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 11,
                                            column: 12,
                                            line: 1,
                                        },
                                    },
                                    value: '[',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'QUEUE-1234[]\n',
                title: 'Битая ссылка на тикет с квадратными скобками',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 13,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 13,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womTicket',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://st.yandex-team.ru/QUEUE-1234',
                                    value: 'QUEUE-1234',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                    },
                                    value: '[]',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'QUEUE-1234[](\n',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 14,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 14,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womTicket',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://st.yandex-team.ru/QUEUE-1234',
                                    value: 'QUEUE-1234',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 13,
                                            column: 14,
                                            line: 1,
                                        },
                                    },
                                    value: '[](',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'QUEUE-1234[ D ](  )\n',
                title: 'Пустая ссылка на тикет с квадратными и круглыми скобками',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 20,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 20,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womTicket',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 19,
                                            column: 20,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://st.yandex-team.ru/QUEUE-1234',
                                    value: 'QUEUE-1234',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'UPPERCASE some text QUEUE-1234\n',
                title: 'Ссылка на тикет должна парситься, даже если есть большие буквы перед ссылкой',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 31,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 31,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 20,
                                            column: 21,
                                            line: 1,
                                        },
                                    },
                                    value: 'UPPERCASE some text ',
                                },
                                {
                                    type: 'womTicket',
                                    position: {
                                        start: {
                                            offset: 20,
                                            column: 21,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 30,
                                            column: 31,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://st.yandex-team.ru/QUEUE-1234',
                                    value: 'QUEUE-1234',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'UPPERCASE some text QUEUE-1234 yes yes\n',
                title: 'Ссылка на тикет должна парситься, даже если есть большие буквы и перед ссылкой, и после',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 39,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 39,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 20,
                                            column: 21,
                                            line: 1,
                                        },
                                    },
                                    value: 'UPPERCASE some text ',
                                },
                                {
                                    type: 'womTicket',
                                    position: {
                                        start: {
                                            offset: 20,
                                            column: 21,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 30,
                                            column: 31,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://st.yandex-team.ru/QUEUE-1234',
                                    value: 'QUEUE-1234',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 30,
                                            column: 31,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 38,
                                            column: 39,
                                            line: 1,
                                        },
                                    },
                                    value: ' yes yes',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'textA QUEUE-1234[ text ](  )\n',
                title: 'Ссылка на тикет должна парситься даже если одна большая буква',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 29,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 29,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 6,
                                            column: 7,
                                            line: 1,
                                        },
                                    },
                                    value: 'textA ',
                                },
                                {
                                    type: 'womTicket',
                                    position: {
                                        start: {
                                            offset: 6,
                                            column: 7,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 28,
                                            column: 29,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://st.yandex-team.ru/QUEUE-1234',
                                    value: 'QUEUE-1234',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'https://jira.yandex-team.ru/QUEUE-1234[]\n',
                title: 'Битая полная ссылка на тикет с квадратными скобками',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 41,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 41,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 40,
                                            column: 41,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://jira.yandex-team.ru/QUEUE-1234[]',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 0,
                                                    column: 1,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 40,
                                                    column: 41,
                                                    line: 1,
                                                },
                                            },
                                            value: 'https://jira.yandex-team.ru/QUEUE-1234[]',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'https://jira.yandex-team.ru/QUEUE-1234()\n',
                title: 'Битая полная ссылка на тикет с круглыми скобками',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 41,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 41,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 40,
                                            column: 41,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://jira.yandex-team.ru/QUEUE-1234()',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 0,
                                                    column: 1,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 40,
                                                    column: 41,
                                                    line: 1,
                                                },
                                            },
                                            value: 'https://jira.yandex-team.ru/QUEUE-1234()',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'st.yandex-team.ru/QUEUE-1234[ Summary  ](  )\n',
                title: 'Полная ссылка на тикет без протокола',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 45,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 45,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 28,
                                            column: 29,
                                            line: 1,
                                        },
                                    },
                                    value: 'st.yandex-team.ru/QUEUE-1234',
                                },
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 28,
                                            column: 29,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 44,
                                            column: 45,
                                            line: 1,
                                        },
                                    },
                                    url: '',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 29,
                                                    column: 30,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 39,
                                                    column: 40,
                                                    line: 1,
                                                },
                                            },
                                            value: ' Summary  ',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '//home/woofmd/SIDEBYSIDE-100500/yes-yes\n',
                title: 'Встроенный в текст номер тикета не должен парситься',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 40,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 40,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 39,
                                            column: 40,
                                            line: 1,
                                        },
                                    },
                                    value: '//home/woofmd/SIDEBYSIDE-100500/yes-yes',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '[Якорь](#woofmd)\n',
                title: 'Якорь-якорек',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 17,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 17,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 16,
                                            column: 17,
                                            line: 1,
                                        },
                                    },
                                    url: '#woofmd',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 1,
                                                    column: 2,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 6,
                                                    column: 7,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Якорь',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'кто:egorova\nq@q\n',
                title: 'кто:egorova\\nq@q',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 16,
                            column: 1,
                            line: 3,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 16,
                                    column: 1,
                                    line: 3,
                                },
                            },
                            children: [
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кто',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 11,
                                            column: 12,
                                            line: 1,
                                        },
                                    },
                                    value: 'egorova',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 11,
                                            column: 12,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 15,
                                            column: 4,
                                            line: 2,
                                        },
                                    },
                                    value: '\nq@q',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'кто:egorova',
                title: 'кто:egorova',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 11,
                            column: 12,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 11,
                                    column: 12,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кто',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 11,
                                            column: 12,
                                            line: 1,
                                        },
                                    },
                                    value: 'egorova',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'кто:egorova-a',
                title: 'кто:egorova-a',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 13,
                            column: 14,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 13,
                                    column: 14,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кто',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 13,
                                            column: 14,
                                            line: 1,
                                        },
                                    },
                                    value: 'egorova-a',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'кто:egorova_a',
                title: 'кто:egorova_a',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 13,
                            column: 14,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 13,
                                    column: 14,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кто',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 13,
                                            column: 14,
                                            line: 1,
                                        },
                                    },
                                    value: 'egorova_a',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'кто:egorova.a',
                title: 'кто:egorova.a',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 13,
                            column: 14,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 13,
                                    column: 14,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кто',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 13,
                                            column: 14,
                                            line: 1,
                                        },
                                    },
                                    value: 'egorova.a',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'кого:egorova',
                title: 'кого:egorova',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 12,
                            column: 13,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 12,
                                    column: 13,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кого',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                    },
                                    value: 'egorova',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'кому:egorova',
                title: 'кому:egorova',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 12,
                            column: 13,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 12,
                                    column: 13,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кому',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                    },
                                    value: 'egorova',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'кем:egorova',
                title: 'кем:egorova',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 11,
                            column: 12,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 11,
                                    column: 12,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кем',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 11,
                                            column: 12,
                                            line: 1,
                                        },
                                    },
                                    value: 'egorova',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'ком:egorova',
                title: 'ком:egorova',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 11,
                            column: 12,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 11,
                                    column: 12,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'ком',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 11,
                                            column: 12,
                                            line: 1,
                                        },
                                    },
                                    value: 'egorova',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'оком:egorova',
                title: 'оком:egorova',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 12,
                            column: 13,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 12,
                                    column: 13,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'оком',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                    },
                                    value: 'egorova',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'укого:egorova',
                title: 'укого:egorova',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 13,
                            column: 14,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 13,
                                    column: 14,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'укого',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 13,
                                            column: 14,
                                            line: 1,
                                        },
                                    },
                                    value: 'egorova',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'ского:egorova',
                title: 'ского:egorova',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 13,
                            column: 14,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 13,
                                    column: 14,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'ского',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 13,
                                            column: 14,
                                            line: 1,
                                        },
                                    },
                                    value: 'egorova',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'staff:egorova',
                title: 'staff:egorova',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 13,
                            column: 14,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 13,
                                    column: 14,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'staff',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 13,
                                            column: 14,
                                            line: 1,
                                        },
                                    },
                                    value: 'egorova',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'egorova@',
                title: 'egorova@',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 8,
                            column: 9,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 8,
                                    column: 9,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womStaff',
                                    at: 'suffix',
                                    case: null,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 8,
                                            column: 9,
                                            line: 1,
                                        },
                                    },
                                    value: 'egorova',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '@egorova',
                title: '@egorova',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 8,
                            column: 9,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 8,
                                    column: 9,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womStaff',
                                    at: 'prefix',
                                    case: null,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 8,
                                            column: 9,
                                            line: 1,
                                        },
                                    },
                                    value: 'egorova',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'текст тексткто:egorova',
                title: 'текст тексткто:egorova',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 22,
                            column: 23,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 22,
                                    column: 23,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 22,
                                            column: 23,
                                            line: 1,
                                        },
                                    },
                                    value: 'текст тексткто:egorova',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'текст текст@egorova',
                title: 'текст текст@egorova',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 19,
                            column: 20,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 19,
                                    column: 20,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 19,
                                            column: 20,
                                            line: 1,
                                        },
                                    },
                                    value: 'текст текст@egorova',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'club:internet',
                title: 'club:internet',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 13,
                            column: 14,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 13,
                                    column: 14,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womClub',
                                    at: null,
                                    case: 'club',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 13,
                                            column: 14,
                                            line: 1,
                                        },
                                    },
                                    value: 'internet',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'клуб:интернет',
                title: 'клуб:интернет',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 13,
                            column: 14,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 13,
                                    column: 14,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womClub',
                                    at: null,
                                    case: 'клуб',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 13,
                                            column: 14,
                                            line: 1,
                                        },
                                    },
                                    value: 'интернет',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'Perfecto is real! Right, staff:johnson?\n\nPerfecto is real! Right, johnson@? Left, @sonjohn?\n\njohnson@ @sonjohn\n',
                title: 'Checking for false-positives in staff links',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 111,
                            column: 1,
                            line: 6,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 40,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 25,
                                            column: 26,
                                            line: 1,
                                        },
                                    },
                                    value: 'Perfecto is real! Right, ',
                                },
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'staff',
                                    position: {
                                        start: {
                                            offset: 25,
                                            column: 26,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 38,
                                            column: 39,
                                            line: 1,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 38,
                                            column: 39,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 39,
                                            column: 40,
                                            line: 1,
                                        },
                                    },
                                    value: '?',
                                },
                            ],
                        },
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 41,
                                    column: 1,
                                    line: 3,
                                },
                                end: {
                                    offset: 92,
                                    column: 1,
                                    line: 4,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 41,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 66,
                                            column: 26,
                                            line: 3,
                                        },
                                    },
                                    value: 'Perfecto is real! Right, ',
                                },
                                {
                                    type: 'womStaff',
                                    at: 'suffix',
                                    case: null,
                                    position: {
                                        start: {
                                            offset: 66,
                                            column: 26,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 74,
                                            column: 34,
                                            line: 3,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 74,
                                            column: 34,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 82,
                                            column: 42,
                                            line: 3,
                                        },
                                    },
                                    value: '? Left, ',
                                },
                                {
                                    type: 'womStaff',
                                    at: 'prefix',
                                    case: null,
                                    position: {
                                        start: {
                                            offset: 82,
                                            column: 42,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 90,
                                            column: 50,
                                            line: 3,
                                        },
                                    },
                                    value: 'sonjohn',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 90,
                                            column: 50,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 91,
                                            column: 51,
                                            line: 3,
                                        },
                                    },
                                    value: '?',
                                },
                            ],
                        },
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 93,
                                    column: 1,
                                    line: 5,
                                },
                                end: {
                                    offset: 111,
                                    column: 1,
                                    line: 6,
                                },
                            },
                            children: [
                                {
                                    type: 'womStaff',
                                    at: 'suffix',
                                    case: null,
                                    position: {
                                        start: {
                                            offset: 93,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 101,
                                            column: 9,
                                            line: 5,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 101,
                                            column: 9,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 102,
                                            column: 10,
                                            line: 5,
                                        },
                                    },
                                    value: ' ',
                                },
                                {
                                    type: 'womStaff',
                                    at: 'prefix',
                                    case: null,
                                    position: {
                                        start: {
                                            offset: 102,
                                            column: 10,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 110,
                                            column: 18,
                                            line: 5,
                                        },
                                    },
                                    value: 'sonjohn',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '--staff:login--\n',
                title: 'Перечеркнутый стафф',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 16,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 16,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womStrike',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 15,
                                            column: 16,
                                            line: 1,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'womStaff',
                                            at: null,
                                            case: 'staff',
                                            position: {
                                                start: {
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 13,
                                                    column: 14,
                                                    line: 1,
                                                },
                                            },
                                            value: 'login',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '@@login@@\n',
                title: '@@login@@',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 10,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 10,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 1,
                                            column: 2,
                                            line: 1,
                                        },
                                    },
                                    value: '@',
                                },
                                {
                                    type: 'womStaff',
                                    at: 'prefix',
                                    case: null,
                                    position: {
                                        start: {
                                            offset: 1,
                                            column: 2,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 7,
                                            column: 8,
                                            line: 1,
                                        },
                                    },
                                    value: 'login',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 7,
                                            column: 8,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 9,
                                            column: 10,
                                            line: 1,
                                        },
                                    },
                                    value: '@@',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '@login...\n',
                title: '@login...',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 10,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 10,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womStaff',
                                    at: 'prefix',
                                    case: null,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 6,
                                            column: 7,
                                            line: 1,
                                        },
                                    },
                                    value: 'login',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 6,
                                            column: 7,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 9,
                                            column: 10,
                                            line: 1,
                                        },
                                    },
                                    value: '...',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '@...\n',
                title: '@...',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 5,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 5,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 4,
                                            column: 5,
                                            line: 1,
                                        },
                                    },
                                    value: '@...',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'кого:johnson.\nкого:johnson,\nкого:johnson;\nкого:johnson:\nкого:johnson)\nкого:johnson!\nкого:johnson?\n.кого:johnson\n,кого:johnson\n;кого:johnson\n:кого:johnson\n(кого:johnson\n!кого:johnson\n?кого:johnson\n.кого:johnson.\n,кого:johnson,\n;кого:johnson;\n:кого:johnson:\n(кого:johnson)\n!кого:johnson!\n?кого:johnson?\n',
                title: 'staff with punctuation marks',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 301,
                            column: 1,
                            line: 22,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 301,
                                    column: 1,
                                    line: 22,
                                },
                            },
                            children: [
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кого',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 14,
                                            column: 1,
                                            line: 2,
                                        },
                                    },
                                    value: '.\n',
                                },
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кого',
                                    position: {
                                        start: {
                                            offset: 14,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 26,
                                            column: 13,
                                            line: 2,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 26,
                                            column: 13,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 28,
                                            column: 1,
                                            line: 3,
                                        },
                                    },
                                    value: ',\n',
                                },
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кого',
                                    position: {
                                        start: {
                                            offset: 28,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 40,
                                            column: 13,
                                            line: 3,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 40,
                                            column: 13,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 42,
                                            column: 1,
                                            line: 4,
                                        },
                                    },
                                    value: ';\n',
                                },
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кого',
                                    position: {
                                        start: {
                                            offset: 42,
                                            column: 1,
                                            line: 4,
                                        },
                                        end: {
                                            offset: 54,
                                            column: 13,
                                            line: 4,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 54,
                                            column: 13,
                                            line: 4,
                                        },
                                        end: {
                                            offset: 56,
                                            column: 1,
                                            line: 5,
                                        },
                                    },
                                    value: ':\n',
                                },
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кого',
                                    position: {
                                        start: {
                                            offset: 56,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 68,
                                            column: 13,
                                            line: 5,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 68,
                                            column: 13,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 70,
                                            column: 1,
                                            line: 6,
                                        },
                                    },
                                    value: ')\n',
                                },
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кого',
                                    position: {
                                        start: {
                                            offset: 70,
                                            column: 1,
                                            line: 6,
                                        },
                                        end: {
                                            offset: 82,
                                            column: 13,
                                            line: 6,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 82,
                                            column: 13,
                                            line: 6,
                                        },
                                        end: {
                                            offset: 84,
                                            column: 1,
                                            line: 7,
                                        },
                                    },
                                    value: '!\n',
                                },
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кого',
                                    position: {
                                        start: {
                                            offset: 84,
                                            column: 1,
                                            line: 7,
                                        },
                                        end: {
                                            offset: 96,
                                            column: 13,
                                            line: 7,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 96,
                                            column: 13,
                                            line: 7,
                                        },
                                        end: {
                                            offset: 99,
                                            column: 2,
                                            line: 8,
                                        },
                                    },
                                    value: '?\n.',
                                },
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кого',
                                    position: {
                                        start: {
                                            offset: 99,
                                            column: 2,
                                            line: 8,
                                        },
                                        end: {
                                            offset: 111,
                                            column: 14,
                                            line: 8,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 111,
                                            column: 14,
                                            line: 8,
                                        },
                                        end: {
                                            offset: 113,
                                            column: 2,
                                            line: 9,
                                        },
                                    },
                                    value: '\n,',
                                },
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кого',
                                    position: {
                                        start: {
                                            offset: 113,
                                            column: 2,
                                            line: 9,
                                        },
                                        end: {
                                            offset: 125,
                                            column: 14,
                                            line: 9,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 125,
                                            column: 14,
                                            line: 9,
                                        },
                                        end: {
                                            offset: 127,
                                            column: 2,
                                            line: 10,
                                        },
                                    },
                                    value: '\n;',
                                },
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кого',
                                    position: {
                                        start: {
                                            offset: 127,
                                            column: 2,
                                            line: 10,
                                        },
                                        end: {
                                            offset: 139,
                                            column: 14,
                                            line: 10,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 139,
                                            column: 14,
                                            line: 10,
                                        },
                                        end: {
                                            offset: 141,
                                            column: 2,
                                            line: 11,
                                        },
                                    },
                                    value: '\n:',
                                },
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кого',
                                    position: {
                                        start: {
                                            offset: 141,
                                            column: 2,
                                            line: 11,
                                        },
                                        end: {
                                            offset: 153,
                                            column: 14,
                                            line: 11,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 153,
                                            column: 14,
                                            line: 11,
                                        },
                                        end: {
                                            offset: 155,
                                            column: 2,
                                            line: 12,
                                        },
                                    },
                                    value: '\n(',
                                },
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кого',
                                    position: {
                                        start: {
                                            offset: 155,
                                            column: 2,
                                            line: 12,
                                        },
                                        end: {
                                            offset: 167,
                                            column: 14,
                                            line: 12,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 167,
                                            column: 14,
                                            line: 12,
                                        },
                                        end: {
                                            offset: 169,
                                            column: 2,
                                            line: 13,
                                        },
                                    },
                                    value: '\n!',
                                },
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кого',
                                    position: {
                                        start: {
                                            offset: 169,
                                            column: 2,
                                            line: 13,
                                        },
                                        end: {
                                            offset: 181,
                                            column: 14,
                                            line: 13,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 181,
                                            column: 14,
                                            line: 13,
                                        },
                                        end: {
                                            offset: 183,
                                            column: 2,
                                            line: 14,
                                        },
                                    },
                                    value: '\n?',
                                },
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кого',
                                    position: {
                                        start: {
                                            offset: 183,
                                            column: 2,
                                            line: 14,
                                        },
                                        end: {
                                            offset: 195,
                                            column: 14,
                                            line: 14,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 195,
                                            column: 14,
                                            line: 14,
                                        },
                                        end: {
                                            offset: 197,
                                            column: 2,
                                            line: 15,
                                        },
                                    },
                                    value: '\n.',
                                },
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кого',
                                    position: {
                                        start: {
                                            offset: 197,
                                            column: 2,
                                            line: 15,
                                        },
                                        end: {
                                            offset: 209,
                                            column: 14,
                                            line: 15,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 209,
                                            column: 14,
                                            line: 15,
                                        },
                                        end: {
                                            offset: 212,
                                            column: 2,
                                            line: 16,
                                        },
                                    },
                                    value: '.\n,',
                                },
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кого',
                                    position: {
                                        start: {
                                            offset: 212,
                                            column: 2,
                                            line: 16,
                                        },
                                        end: {
                                            offset: 224,
                                            column: 14,
                                            line: 16,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 224,
                                            column: 14,
                                            line: 16,
                                        },
                                        end: {
                                            offset: 227,
                                            column: 2,
                                            line: 17,
                                        },
                                    },
                                    value: ',\n;',
                                },
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кого',
                                    position: {
                                        start: {
                                            offset: 227,
                                            column: 2,
                                            line: 17,
                                        },
                                        end: {
                                            offset: 239,
                                            column: 14,
                                            line: 17,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 239,
                                            column: 14,
                                            line: 17,
                                        },
                                        end: {
                                            offset: 242,
                                            column: 2,
                                            line: 18,
                                        },
                                    },
                                    value: ';\n:',
                                },
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кого',
                                    position: {
                                        start: {
                                            offset: 242,
                                            column: 2,
                                            line: 18,
                                        },
                                        end: {
                                            offset: 254,
                                            column: 14,
                                            line: 18,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 254,
                                            column: 14,
                                            line: 18,
                                        },
                                        end: {
                                            offset: 257,
                                            column: 2,
                                            line: 19,
                                        },
                                    },
                                    value: ':\n(',
                                },
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кого',
                                    position: {
                                        start: {
                                            offset: 257,
                                            column: 2,
                                            line: 19,
                                        },
                                        end: {
                                            offset: 269,
                                            column: 14,
                                            line: 19,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 269,
                                            column: 14,
                                            line: 19,
                                        },
                                        end: {
                                            offset: 272,
                                            column: 2,
                                            line: 20,
                                        },
                                    },
                                    value: ')\n!',
                                },
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кого',
                                    position: {
                                        start: {
                                            offset: 272,
                                            column: 2,
                                            line: 20,
                                        },
                                        end: {
                                            offset: 284,
                                            column: 14,
                                            line: 20,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 284,
                                            column: 14,
                                            line: 20,
                                        },
                                        end: {
                                            offset: 287,
                                            column: 2,
                                            line: 21,
                                        },
                                    },
                                    value: '!\n?',
                                },
                                {
                                    type: 'womStaff',
                                    at: null,
                                    case: 'кого',
                                    position: {
                                        start: {
                                            offset: 287,
                                            column: 2,
                                            line: 21,
                                        },
                                        end: {
                                            offset: 299,
                                            column: 14,
                                            line: 21,
                                        },
                                    },
                                    value: 'johnson',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 299,
                                            column: 14,
                                            line: 21,
                                        },
                                        end: {
                                            offset: 300,
                                            column: 15,
                                            line: 21,
                                        },
                                    },
                                    value: '?',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: ':login@\n(login@)\n,login@,\n,login@.\nlogin@...\n',
                title: 'at suffix and punctuation',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 45,
                            column: 1,
                            line: 6,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 45,
                                    column: 1,
                                    line: 6,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 1,
                                            column: 2,
                                            line: 1,
                                        },
                                    },
                                    value: ':',
                                },
                                {
                                    type: 'womStaff',
                                    at: 'suffix',
                                    case: null,
                                    position: {
                                        start: {
                                            offset: 1,
                                            column: 2,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 7,
                                            column: 8,
                                            line: 1,
                                        },
                                    },
                                    value: 'login',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 7,
                                            column: 8,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 9,
                                            column: 2,
                                            line: 2,
                                        },
                                    },
                                    value: '\n(',
                                },
                                {
                                    type: 'womStaff',
                                    at: 'suffix',
                                    case: null,
                                    position: {
                                        start: {
                                            offset: 9,
                                            column: 2,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 15,
                                            column: 8,
                                            line: 2,
                                        },
                                    },
                                    value: 'login',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 15,
                                            column: 8,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 18,
                                            column: 2,
                                            line: 3,
                                        },
                                    },
                                    value: ')\n,',
                                },
                                {
                                    type: 'womStaff',
                                    at: 'suffix',
                                    case: null,
                                    position: {
                                        start: {
                                            offset: 18,
                                            column: 2,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 24,
                                            column: 8,
                                            line: 3,
                                        },
                                    },
                                    value: 'login',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 24,
                                            column: 8,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 27,
                                            column: 2,
                                            line: 4,
                                        },
                                    },
                                    value: ',\n,',
                                },
                                {
                                    type: 'womStaff',
                                    at: 'suffix',
                                    case: null,
                                    position: {
                                        start: {
                                            offset: 27,
                                            column: 2,
                                            line: 4,
                                        },
                                        end: {
                                            offset: 33,
                                            column: 8,
                                            line: 4,
                                        },
                                    },
                                    value: 'login',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 33,
                                            column: 8,
                                            line: 4,
                                        },
                                        end: {
                                            offset: 35,
                                            column: 1,
                                            line: 5,
                                        },
                                    },
                                    value: '.\n',
                                },
                                {
                                    type: 'womStaff',
                                    at: 'suffix',
                                    case: null,
                                    position: {
                                        start: {
                                            offset: 35,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 41,
                                            column: 7,
                                            line: 5,
                                        },
                                    },
                                    value: 'login',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 41,
                                            column: 7,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 44,
                                            column: 10,
                                            line: 5,
                                        },
                                    },
                                    value: '...',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '((mailto:mail@woofmd-team.ru mail@))\n',
                title: 'Почта полная',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 37,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 37,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womLink',
                                    brackets: false,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 36,
                                            column: 37,
                                            line: 1,
                                        },
                                    },
                                    url: 'mailto:mail@woofmd-team.ru',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 29,
                                                    column: 30,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 34,
                                                    column: 35,
                                                    line: 1,
                                                },
                                            },
                                            value: 'mail@',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'mail@mail.ru\n',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 13,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 13,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                    },
                                    value: 'mail@mail.ru',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'mailto:mail@mail.ru\n',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 20,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 20,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 19,
                                            column: 20,
                                            line: 1,
                                        },
                                    },
                                    url: 'mailto:mail@mail.ru',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 0,
                                                    column: 1,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 12,
                                                    column: 13,
                                                    line: 1,
                                                },
                                            },
                                            value: 'mail@mail.ru',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '((http://www.woofmd.ru))\n',
                title: 'Одинокая ссылка в круглых скобках',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 25,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 25,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womLink',
                                    brackets: false,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 24,
                                            column: 25,
                                            line: 1,
                                        },
                                    },
                                    url: 'http://www.woofmd.ru',
                                    children: [
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '(((((((http://www.woofmd.ru)))\n',
                title: 'Множество скобок перед ссылками',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 31,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 31,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 5,
                                            column: 6,
                                            line: 1,
                                        },
                                    },
                                    value: '(((((',
                                },
                                {
                                    type: 'womLink',
                                    brackets: false,
                                    position: {
                                        start: {
                                            offset: 5,
                                            column: 6,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 29,
                                            column: 30,
                                            line: 1,
                                        },
                                    },
                                    url: 'http://www.woofmd.ru',
                                    children: [
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 29,
                                            column: 30,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 30,
                                            column: 31,
                                            line: 1,
                                        },
                                    },
                                    value: ')',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '[[http://www.woofmd.ru]]\n',
                title: 'Одинокая ссылка в квадратных скобках',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 25,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 25,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womLink',
                                    brackets: true,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 24,
                                            column: 25,
                                            line: 1,
                                        },
                                    },
                                    url: 'http://www.woofmd.ru',
                                    children: [
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '((http://www.woofmd.ru WoofMD в круглых скобках))\n',
                title: 'Ссылка с текстом в круглых скобках',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 50,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 50,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womLink',
                                    brackets: false,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 49,
                                            column: 50,
                                            line: 1,
                                        },
                                    },
                                    url: 'http://www.woofmd.ru',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 23,
                                                    column: 24,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 47,
                                                    column: 48,
                                                    line: 1,
                                                },
                                            },
                                            value: 'WoofMD в круглых скобках',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '[[http://www.woofmd.ru WoofMD в квадратных скобках]]\n',
                title: 'Ссылка с текстом в квадратных скобках',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 53,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 53,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womLink',
                                    brackets: true,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 52,
                                            column: 53,
                                            line: 1,
                                        },
                                    },
                                    url: 'http://www.woofmd.ru',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 23,
                                                    column: 24,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 50,
                                                    column: 51,
                                                    line: 1,
                                                },
                                            },
                                            value: 'WoofMD в квадратных скобках',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '((Устафф))\n',
                title: 'Относительная ссылка в круглых скобках',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 11,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 11,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womLink',
                                    brackets: false,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                    },
                                    url: 'Устафф',
                                    children: [
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '[[Устафф]]\n',
                title: 'Относительная ссылка в квадратных скобках',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 11,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 11,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womLink',
                                    brackets: true,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                    },
                                    url: 'Устафф',
                                    children: [
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '((Устафф Страница про устав))\n',
                title: 'Относительная ссылка c описанием в круглых скобках',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 30,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 30,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womLink',
                                    brackets: false,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 29,
                                            column: 30,
                                            line: 1,
                                        },
                                    },
                                    url: 'Устафф',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 9,
                                                    column: 10,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 27,
                                                    column: 28,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Страница про устав',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '[[Устафф Страница про устафф]]\n',
                title: 'Относительная ссылка с описанием в квадратных скобках',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 31,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 31,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womLink',
                                    brackets: true,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 30,
                                            column: 31,
                                            line: 1,
                                        },
                                    },
                                    url: 'Устафф',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 9,
                                                    column: 10,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 28,
                                                    column: 29,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Страница про устафф',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '((/HomePage#TOC_1))\n',
                title: 'Битая ссылка на якорь',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 20,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 20,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womLink',
                                    brackets: false,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 19,
                                            column: 20,
                                            line: 1,
                                        },
                                    },
                                    url: '/HomePage#TOC_1',
                                    children: [
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '((/homepage 10x10:https://woof.ru/5.jpg))\n',
                title: 'Ссылка с картинкой внутри',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 42,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 42,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womLink',
                                    brackets: false,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 41,
                                            column: 42,
                                            line: 1,
                                        },
                                    },
                                    url: '/homepage',
                                    children: [
                                        {
                                            type: 'womImage',
                                            height: 10,
                                            position: {
                                                start: {
                                                    offset: 12,
                                                    column: 13,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 39,
                                                    column: 40,
                                                    line: 1,
                                                },
                                            },
                                            url: 'https://woof.ru/5.jpg',
                                            width: 10,
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '((/homepage ~10x10:https://woof.ru/5.jpg))\n',
                title: 'Ссылка с экранированием внутри (~)',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 43,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 43,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womLink',
                                    brackets: false,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 42,
                                            column: 43,
                                            line: 1,
                                        },
                                    },
                                    url: '/homepage',
                                    children: [
                                        {
                                            type: 'womEscape',
                                            position: {
                                                start: {
                                                    offset: 12,
                                                    column: 13,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 40,
                                                    column: 41,
                                                    line: 1,
                                                },
                                            },
                                            raw: '~10x10:https://woof.ru/5.jpg',
                                            value: '10x10:https://woof.ru/5.jpg',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '((/homepage ""10x10:https://woof.ru/5.jpg""))\n',
                title: 'Ссылка с экранированием внутри ("")',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 46,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 46,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womLink',
                                    brackets: false,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 45,
                                            column: 46,
                                            line: 1,
                                        },
                                    },
                                    url: '/homepage',
                                    children: [
                                        {
                                            type: 'womEscape',
                                            position: {
                                                start: {
                                                    offset: 12,
                                                    column: 13,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 43,
                                                    column: 44,
                                                    line: 1,
                                                },
                                            },
                                            raw: '""10x10:https://woof.ru/5.jpg""',
                                            value: '10x10:https://woof.ru/5.jpg',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '((/homepage @staff))\n',
                title: 'Cтафф блок не должен парситься внутри ссылки',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 21,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 21,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womLink',
                                    brackets: false,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 20,
                                            column: 21,
                                            line: 1,
                                        },
                                    },
                                    url: '/homepage',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 12,
                                                    column: 13,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 18,
                                                    column: 19,
                                                    line: 1,
                                                },
                                            },
                                            value: '@staff',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '((https://wiki.woofmd-team.ru/wiki/vodstvo/file/.files/bobrujjsk.doc ссылка на файл))',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 85,
                            column: 86,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 85,
                                    column: 86,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womLink',
                                    brackets: false,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 85,
                                            column: 86,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://wiki.woofmd-team.ru/wiki/vodstvo/file/.files/bobrujjsk.doc',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 69,
                                                    column: 70,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 83,
                                                    column: 84,
                                                    line: 1,
                                                },
                                            },
                                            value: 'ссылка на файл',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'http://img.woofmd.net/i/logo95x37x8.png\n',
                title: 'Прямая ссылка на картинку',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 40,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 40,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 39,
                                            column: 40,
                                            line: 1,
                                        },
                                    },
                                    url: 'http://img.woofmd.net/i/logo95x37x8.png',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 0,
                                                    column: 1,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 39,
                                                    column: 40,
                                                    line: 1,
                                                },
                                            },
                                            value: 'http://img.woofmd.net/i/logo95x37x8.png',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'https://abc.woofmd-team.ru/services/_wiki_\n',
                title: 'Ссылка с элементами форматирования не должна быть отформатирована',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 43,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 43,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 42,
                                            column: 43,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://abc.woofmd-team.ru/services/_wiki_',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 0,
                                                    column: 1,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 42,
                                                    column: 43,
                                                    line: 1,
                                                },
                                            },
                                            value: 'https://abc.woofmd-team.ru/services/_wiki_',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'https://awaps.woofmd.ru/15/35819/(14400891/0)\n',
                title: 'Не должен обрезаться ) от ссылки',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 46,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 46,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 45,
                                            column: 46,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://awaps.woofmd.ru/15/35819/(14400891/0)',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 0,
                                                    column: 1,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 45,
                                                    column: 46,
                                                    line: 1,
                                                },
                                            },
                                            value: 'https://awaps.woofmd.ru/15/35819/(14400891/0)',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '100x100:https://wiki.woofmd-team.ru/wiki/vodstvo/pictures/.files/e1.jpg\n',
                title: 'Картинка с заданным размером',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 72,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 72,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womImage',
                                    height: 100,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 71,
                                            column: 72,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://wiki.woofmd-team.ru/wiki/vodstvo/pictures/.files/e1.jpg',
                                    width: 100,
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '((/HomePage http://img.woofmd.net/i/logo95x37x8.png))\n',
                title: 'Картинка-ссылка',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 54,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 54,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womLink',
                                    brackets: false,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 53,
                                            column: 54,
                                            line: 1,
                                        },
                                    },
                                    url: '/HomePage',
                                    children: [
                                        {
                                            type: 'link',
                                            title: null,
                                            position: {
                                                start: {
                                                    offset: 12,
                                                    column: 13,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 51,
                                                    column: 52,
                                                    line: 1,
                                                },
                                            },
                                            url: 'http://img.woofmd.net/i/logo95x37x8.png',
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 12,
                                                            column: 13,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 51,
                                                            column: 52,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'http://img.woofmd.net/i/logo95x37x8.png',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '((http://img.woofmd.net/i/www/citylogos/gramota2-logo-ru.png http://img.woofmd.net/i/www/logo.png))\n',
                title: 'Картинка-ссылка на картинку',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 100,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 100,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womLink',
                                    brackets: false,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 99,
                                            column: 100,
                                            line: 1,
                                        },
                                    },
                                    url: 'http://img.woofmd.net/i/www/citylogos/gramota2-logo-ru.png',
                                    children: [
                                        {
                                            type: 'link',
                                            title: null,
                                            position: {
                                                start: {
                                                    offset: 61,
                                                    column: 62,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 97,
                                                    column: 98,
                                                    line: 1,
                                                },
                                            },
                                            url: 'http://img.woofmd.net/i/www/logo.png',
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 61,
                                                            column: 62,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 97,
                                                            column: 98,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'http://img.woofmd.net/i/www/logo.png',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '100x100:file:/dog.jpg\n',
                title: 'Картика с file',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 22,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 22,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womImage',
                                    height: 100,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 21,
                                            column: 22,
                                            line: 1,
                                        },
                                    },
                                    url: 'file:/dog.jpg',
                                    width: 100,
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'wiki.woof-team.ru/link\nhttps://st.woof-team.ru/WIKI-123\n',
                title: 'Сочетание ссылок',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 56,
                            column: 1,
                            line: 3,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 56,
                                    column: 1,
                                    line: 3,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 23,
                                            column: 1,
                                            line: 2,
                                        },
                                    },
                                    value: 'wiki.woof-team.ru/link\n',
                                },
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 23,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 55,
                                            column: 33,
                                            line: 2,
                                        },
                                    },
                                    url: 'https://st.woof-team.ru/WIKI-123',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 23,
                                                    column: 1,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 55,
                                                    column: 33,
                                                    line: 2,
                                                },
                                            },
                                            value: 'https://st.woof-team.ru/WIKI-123',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
        ],
    },
];
