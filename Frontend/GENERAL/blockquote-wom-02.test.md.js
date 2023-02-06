module.exports = [
    {
        group: 'womBlockquote <[]>',
        tests: [
            {
                markup: 'Можно даже прямо внутри <[Процитировать Ницше]>. Да-да.\n',
                title: 'Инлайн цитирование текста',
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
                                    type: 'text',
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
                                    value: 'Можно даже прямо внутри ',
                                },
                                {
                                    type: 'womBlockquote',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 24,
                                            column: 25,
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
                                                    offset: 26,
                                                    column: 27,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 45,
                                                    column: 46,
                                                    line: 1,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 26,
                                                            column: 27,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 45,
                                                            column: 46,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'Процитировать Ницше',
                                                },
                                            ],
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 47,
                                            column: 48,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 55,
                                            column: 56,
                                            line: 1,
                                        },
                                    },
                                    value: '. Да-да.',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '<[ Цитирование верхнего уровня ,\n  <[ А внутри еще длинного,\nс переносами ]>\n И низвоуровневое цитирование\n ]>\n',
                title: 'Вложенное строчное цитирование текста',
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
                                    offset: 111,
                                    column: 1,
                                    line: 6,
                                },
                            },
                            children: [
                                {
                                    type: 'womBlockquote',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 110,
                                            column: 4,
                                            line: 5,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 3,
                                                    column: 4,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 106,
                                                    column: 30,
                                                    line: 4,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 3,
                                                            column: 4,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 35,
                                                            column: 3,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'Цитирование верхнего уровня ,\n  ',
                                                },
                                                {
                                                    type: 'womBlockquote',
                                                    inline: true,
                                                    position: {
                                                        start: {
                                                            offset: 35,
                                                            column: 3,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 76,
                                                            column: 16,
                                                            line: 3,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 38,
                                                                    column: 6,
                                                                    line: 2,
                                                                },
                                                                end: {
                                                                    offset: 73,
                                                                    column: 13,
                                                                    line: 3,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'text',
                                                                    position: {
                                                                        start: {
                                                                            offset: 38,
                                                                            column: 6,
                                                                            line: 2,
                                                                        },
                                                                        end: {
                                                                            offset: 73,
                                                                            column: 13,
                                                                            line: 3,
                                                                        },
                                                                    },
                                                                    value: 'А внутри еще длинного,\nс переносами',
                                                                },
                                                            ],
                                                        },
                                                    ],
                                                },
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 76,
                                                            column: 16,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 106,
                                                            column: 30,
                                                            line: 4,
                                                        },
                                                    },
                                                    value: '\n И низвоуровневое цитирование',
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
                markup: '>> Однострочное цитирование\n>\n> Да, это оно\n\nА это обычный текст\n',
                title: 'Однострочное цитирование',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 65,
                            column: 1,
                            line: 6,
                        },
                    },
                    children: [
                        {
                            type: 'blockquote',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 43,
                                    column: 14,
                                    line: 3,
                                },
                            },
                            children: [
                                {
                                    type: 'blockquote',
                                    position: {
                                        start: {
                                            offset: 1,
                                            column: 2,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 27,
                                            column: 28,
                                            line: 1,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 3,
                                                    column: 4,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 27,
                                                    column: 28,
                                                    line: 1,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 3,
                                                            column: 4,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 27,
                                                            column: 28,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'Однострочное цитирование',
                                                },
                                            ],
                                        },
                                    ],
                                },
                                {
                                    type: 'paragraph',
                                    position: {
                                        start: {
                                            offset: 32,
                                            column: 3,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 43,
                                            column: 14,
                                            line: 3,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 32,
                                                    column: 3,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 43,
                                                    column: 14,
                                                    line: 3,
                                                },
                                            },
                                            value: 'Да, это оно',
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 45,
                                    column: 1,
                                    line: 5,
                                },
                                end: {
                                    offset: 65,
                                    column: 1,
                                    line: 6,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 45,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 64,
                                            column: 20,
                                            line: 5,
                                        },
                                    },
                                    value: 'А это обычный текст',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '**Однострочное цитирование**\n>> Однострочное цитирование\n>Да, это оно\nА это обычный текст',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 89,
                            column: 20,
                            line: 4,
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
                                    type: 'strong',
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
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 26,
                                                    column: 27,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Однострочное цитирование',
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'blockquote',
                            position: {
                                start: {
                                    offset: 29,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 69,
                                    column: 13,
                                    line: 3,
                                },
                            },
                            children: [
                                {
                                    type: 'blockquote',
                                    position: {
                                        start: {
                                            offset: 30,
                                            column: 2,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 56,
                                            column: 28,
                                            line: 2,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 32,
                                                    column: 4,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 56,
                                                    column: 28,
                                                    line: 2,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 32,
                                                            column: 4,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 56,
                                                            column: 28,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'Однострочное цитирование',
                                                },
                                            ],
                                        },
                                    ],
                                },
                                {
                                    type: 'paragraph',
                                    position: {
                                        start: {
                                            offset: 58,
                                            column: 2,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 69,
                                            column: 13,
                                            line: 3,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 58,
                                                    column: 2,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 69,
                                                    column: 13,
                                                    line: 3,
                                                },
                                            },
                                            value: 'Да, это оно',
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 70,
                                    column: 1,
                                    line: 4,
                                },
                                end: {
                                    offset: 89,
                                    column: 20,
                                    line: 4,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 70,
                                            column: 1,
                                            line: 4,
                                        },
                                        end: {
                                            offset: 89,
                                            column: 20,
                                            line: 4,
                                        },
                                    },
                                    value: 'А это обычный текст',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '<[https://lk.taximeter.yandex.ru]>\n',
                title: '<[https://lk.taximeter.yandex.ru]>',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 35,
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
                                    offset: 35,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womBlockquote',
                                    inline: true,
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
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 32,
                                                    column: 33,
                                                    line: 1,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'link',
                                                    title: null,
                                                    position: {
                                                        start: {
                                                            offset: 2,
                                                            column: 3,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 32,
                                                            column: 33,
                                                            line: 1,
                                                        },
                                                    },
                                                    url: 'https://lk.taximeter.yandex.ru',
                                                    children: [
                                                        {
                                                            type: 'text',
                                                            position: {
                                                                start: {
                                                                    offset: 2,
                                                                    column: 3,
                                                                    line: 1,
                                                                },
                                                                end: {
                                                                    offset: 32,
                                                                    column: 33,
                                                                    line: 1,
                                                                },
                                                            },
                                                            value: 'https://lk.taximeter.yandex.ru',
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
        ],
    },
];
