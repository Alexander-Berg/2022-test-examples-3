module.exports = [
    {
        group: 'womBlockquote <[]>',
        tests: [
            {
                markup: '<[\nЦитирование текста\n]>\n',
                title: 'Блочное цитирование текста РВЕТ параграф',
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
                            line: 4,
                        },
                    },
                    children: [
                        {
                            type: 'womBlockquote',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 24,
                                    column: 3,
                                    line: 3,
                                },
                            },
                            children: [
                                {
                                    type: 'paragraph',
                                    position: {
                                        start: {
                                            offset: 3,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 21,
                                            column: 19,
                                            line: 2,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 3,
                                                    column: 1,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 21,
                                                    column: 19,
                                                    line: 2,
                                                },
                                            },
                                            value: 'Цитирование текста',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '1. test\n<[\nЦитирование текста\n]>\n1. test\n',
                title: 'Блочное цитирование текста РВЕТ список',
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
                            line: 6,
                        },
                    },
                    children: [
                        {
                            type: 'list',
                            start: 1,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 7,
                                    column: 8,
                                    line: 1,
                                },
                            },
                            styleType: 'decimal',
                            children: [
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 7,
                                            column: 8,
                                            line: 1,
                                        },
                                    },
                                    restart: null,
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
                                                    offset: 7,
                                                    column: 8,
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
                                                            offset: 7,
                                                            column: 8,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'test',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'womBlockquote',
                            inline: false,
                            position: {
                                start: {
                                    offset: 8,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 32,
                                    column: 3,
                                    line: 4,
                                },
                            },
                            children: [
                                {
                                    type: 'paragraph',
                                    position: {
                                        start: {
                                            offset: 11,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 29,
                                            column: 19,
                                            line: 3,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 11,
                                                    column: 1,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 29,
                                                    column: 19,
                                                    line: 3,
                                                },
                                            },
                                            value: 'Цитирование текста',
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'list',
                            start: 1,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 33,
                                    column: 1,
                                    line: 5,
                                },
                                end: {
                                    offset: 40,
                                    column: 8,
                                    line: 5,
                                },
                            },
                            styleType: 'decimal',
                            children: [
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 33,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 40,
                                            column: 8,
                                            line: 5,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 36,
                                                    column: 4,
                                                    line: 5,
                                                },
                                                end: {
                                                    offset: 40,
                                                    column: 8,
                                                    line: 5,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 36,
                                                            column: 4,
                                                            line: 5,
                                                        },
                                                        end: {
                                                            offset: 40,
                                                            column: 8,
                                                            line: 5,
                                                        },
                                                    },
                                                    value: 'test',
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
                markup: '<[ Цитирование текста ]>\n',
                title: 'Строчное цитирование текста НЕ РВЕТ параграф',
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
                                    type: 'womBlockquote',
                                    inline: true,
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
                                                    offset: 21,
                                                    column: 22,
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
                                                            offset: 21,
                                                            column: 22,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'Цитирование текста',
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
                markup: '<[ Цитирование текста,\nдлинного,\nс переносами\n]>\n',
                title: 'Строчное цитирование текста с переносами НЕ РВЕТ параграф',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 49,
                            column: 1,
                            line: 5,
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
                                    offset: 49,
                                    column: 1,
                                    line: 5,
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
                                            offset: 48,
                                            column: 3,
                                            line: 4,
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
                                                    offset: 45,
                                                    column: 13,
                                                    line: 3,
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
                                                            offset: 45,
                                                            column: 13,
                                                            line: 3,
                                                        },
                                                    },
                                                    value: 'Цитирование текста,\nдлинного,\nс переносами',
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
                markup: '1. test\n<[ Цитирование текста ]>\n',
                title: 'Строчное цитирование текста НЕ РВЕТ список',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 33,
                            column: 1,
                            line: 3,
                        },
                    },
                    children: [
                        {
                            type: 'list',
                            start: 1,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 32,
                                    column: 25,
                                    line: 2,
                                },
                            },
                            styleType: 'decimal',
                            children: [
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 32,
                                            column: 25,
                                            line: 2,
                                        },
                                    },
                                    restart: null,
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
                                                    offset: 32,
                                                    column: 25,
                                                    line: 2,
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
                                                            offset: 8,
                                                            column: 1,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'test\n',
                                                },
                                                {
                                                    type: 'womBlockquote',
                                                    inline: true,
                                                    position: {
                                                        start: {
                                                            offset: 8,
                                                            column: 1,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 32,
                                                            column: 25,
                                                            line: 2,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 11,
                                                                    column: 4,
                                                                    line: 2,
                                                                },
                                                                end: {
                                                                    offset: 29,
                                                                    column: 22,
                                                                    line: 2,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'text',
                                                                    position: {
                                                                        start: {
                                                                            offset: 11,
                                                                            column: 4,
                                                                            line: 2,
                                                                        },
                                                                        end: {
                                                                            offset: 29,
                                                                            column: 22,
                                                                            line: 2,
                                                                        },
                                                                    },
                                                                    value: 'Цитирование текста',
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
                markup: '1. test\n<[ Цитирование текста\nи тут продолжение,\nи тут тоже\n]>\n',
                title: 'Строчное цитирование текста с переносами НЕ РВЕТ список',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 63,
                            column: 1,
                            line: 6,
                        },
                    },
                    children: [
                        {
                            type: 'list',
                            start: 1,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 62,
                                    column: 3,
                                    line: 5,
                                },
                            },
                            styleType: 'decimal',
                            children: [
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 62,
                                            column: 3,
                                            line: 5,
                                        },
                                    },
                                    restart: null,
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
                                                    offset: 62,
                                                    column: 3,
                                                    line: 5,
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
                                                            offset: 8,
                                                            column: 1,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'test\n',
                                                },
                                                {
                                                    type: 'womBlockquote',
                                                    inline: true,
                                                    position: {
                                                        start: {
                                                            offset: 8,
                                                            column: 1,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 62,
                                                            column: 3,
                                                            line: 5,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 11,
                                                                    column: 4,
                                                                    line: 2,
                                                                },
                                                                end: {
                                                                    offset: 59,
                                                                    column: 11,
                                                                    line: 4,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'text',
                                                                    position: {
                                                                        start: {
                                                                            offset: 11,
                                                                            column: 4,
                                                                            line: 2,
                                                                        },
                                                                        end: {
                                                                            offset: 59,
                                                                            column: 11,
                                                                            line: 4,
                                                                        },
                                                                    },
                                                                    value: 'Цитирование текста\nи тут продолжение,\nи тут тоже',
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
        ],
    },
];
