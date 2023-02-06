module.exports = [
    {
        group: 'womImage',
        tests: [
            {
                markup: '30х30:https://jing.yandex-team.ru/files/bekzhan29/kazakhstan-hi.jpg',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 67,
                            column: 68,
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
                                    offset: 67,
                                    column: 68,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womImage',
                                    height: 30,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 67,
                                            column: 68,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://jing.yandex-team.ru/files/bekzhan29/kazakhstan-hi.jpg',
                                    width: 30,
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '100х100:file:/users/masloval/mobiletest.jpg',
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
                            column: 44,
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
                                    offset: 43,
                                    column: 44,
                                    line: 1,
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
                                            offset: 43,
                                            column: 44,
                                            line: 1,
                                        },
                                    },
                                    url: 'file:/users/masloval/mobiletest.jpg',
                                    width: 100,
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '100х100:ref:file:/users/masloval/mobiletest.jpg',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 47,
                            column: 48,
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
                                    offset: 47,
                                    column: 48,
                                    line: 1,
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
                                            offset: 47,
                                            column: 48,
                                            line: 1,
                                        },
                                    },
                                    url: 'ref:file:/users/masloval/mobiletest.jpg',
                                    width: 100,
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '0x0:file:https://jing.yandex-team.ru/files/ljql/shutter%202020-03-20%2012%3A51%3A18.png',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 87,
                            column: 88,
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
                                    offset: 87,
                                    column: 88,
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
                                            offset: 9,
                                            column: 10,
                                            line: 1,
                                        },
                                    },
                                    value: '0x0:file:',
                                },
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 9,
                                            column: 10,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 87,
                                            column: 88,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://jing.yandex-team.ru/files/ljql/shutter%202020-03-20%2012%3A51%3A18.png',
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
                                                    offset: 87,
                                                    column: 88,
                                                    line: 1,
                                                },
                                            },
                                            value: 'https://jing.yandex-team.ru/files/ljql/shutter%202020-03-20%2012%3A51%3A18.png',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '#| || 300x300:https://wiki.yandex-team.ru/users/a-pianov/projects/opteum/doc/news/.files/1.png|%%![news_template_new_airport_1](https://tc.mobile.yandex.net/static/images/11896/fd6fce1229c34446b57cbfcd46894746)%%|| |#',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 217,
                            column: 218,
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
                                    offset: 217,
                                    column: 218,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womTable',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 217,
                                            column: 218,
                                            line: 1,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'womTableRow',
                                            position: {
                                                start: {
                                                    offset: 3,
                                                    column: 4,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 214,
                                                    column: 215,
                                                    line: 1,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'womTableCell',
                                                    position: {
                                                        start: {
                                                            offset: 4,
                                                            column: 5,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 95,
                                                            column: 96,
                                                            line: 1,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 6,
                                                                    column: 7,
                                                                    line: 1,
                                                                },
                                                                end: {
                                                                    offset: 94,
                                                                    column: 95,
                                                                    line: 1,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'womImage',
                                                                    height: 300,
                                                                    position: {
                                                                        start: {
                                                                            offset: 6,
                                                                            column: 7,
                                                                            line: 1,
                                                                        },
                                                                        end: {
                                                                            offset: 94,
                                                                            column: 95,
                                                                            line: 1,
                                                                        },
                                                                    },
                                                                    url: 'https://wiki.yandex-team.ru/users/a-pianov/projects/opteum/doc/news/.files/1.png',
                                                                    width: 300,
                                                                },
                                                            ],
                                                        },
                                                    ],
                                                },
                                                {
                                                    type: 'womTableCell',
                                                    position: {
                                                        start: {
                                                            offset: 94,
                                                            column: 95,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 213,
                                                            column: 214,
                                                            line: 1,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 95,
                                                                    column: 96,
                                                                    line: 1,
                                                                },
                                                                end: {
                                                                    offset: 212,
                                                                    column: 213,
                                                                    line: 1,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'womFormatter',
                                                                    inline: true,
                                                                    position: {
                                                                        start: {
                                                                            offset: 95,
                                                                            column: 96,
                                                                            line: 1,
                                                                        },
                                                                        end: {
                                                                            offset: 212,
                                                                            column: 213,
                                                                            line: 1,
                                                                        },
                                                                    },
                                                                    value: '![news_template_new_airport_1](https://tc.mobile.yandex.net/static/images/11896/fd6fce1229c34446b57cbfcd46894746)',
                                                                },
                                                            ],
                                                        },
                                                    ],
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
                markup: '0x0:https://imgs.com/1.png?x=y&a=b',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 34,
                            column: 35,
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
                                    offset: 34,
                                    column: 35,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womImage',
                                    height: 0,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 34,
                                            column: 35,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://imgs.com/1.png?x=y&a=b',
                                    width: 0,
                                },
                            ],
                        },
                    ],
                },
            },
        ],
    },
];
