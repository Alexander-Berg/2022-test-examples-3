module.exports = [
    {
        group: 'womFormatter %%%%',
        tests: [
            {
                markup: '%%\nБлок текста\n%%\n',
                title: 'Блочный форматер РВЕТ параграф',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 18,
                            column: 1,
                            line: 4,
                        },
                    },
                    children: [
                        {
                            type: 'womFormatter',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 17,
                                    column: 3,
                                    line: 3,
                                },
                            },
                            value: 'Блок текста\n',
                        },
                    ],
                },
            },
            {
                markup: '1. test\n%%\nБлок текста\n%%\n1. test\n',
                title: 'Блочный форматер РВЕТ список',
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
                            type: 'womFormatter',
                            inline: false,
                            position: {
                                start: {
                                    offset: 8,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 25,
                                    column: 3,
                                    line: 4,
                                },
                            },
                            value: 'Блок текста\n',
                        },
                        {
                            type: 'list',
                            start: 1,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 26,
                                    column: 1,
                                    line: 5,
                                },
                                end: {
                                    offset: 33,
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
                                            offset: 26,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 33,
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
                                                    offset: 29,
                                                    column: 4,
                                                    line: 5,
                                                },
                                                end: {
                                                    offset: 33,
                                                    column: 8,
                                                    line: 5,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 29,
                                                            column: 4,
                                                            line: 5,
                                                        },
                                                        end: {
                                                            offset: 33,
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
                markup: '%% Блок текста %%\n',
                title: 'Строчный форматер НЕ РВЕТ параграф',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 18,
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
                                    offset: 18,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womFormatter',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 17,
                                            column: 18,
                                            line: 1,
                                        },
                                    },
                                    value: 'Блок текста',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'Я параграф,\n%% Блок текста,\nдлинного,\nс переносами\n%%\nи я все еще параграф\n',
                title: 'Строчный форматер с переносами НЕ РВЕТ параграф (1)',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 75,
                            column: 1,
                            line: 7,
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
                                    offset: 75,
                                    column: 1,
                                    line: 7,
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
                                            column: 1,
                                            line: 2,
                                        },
                                    },
                                    value: 'Я параграф,\n',
                                },
                                {
                                    type: 'womFormatter',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 12,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 53,
                                            column: 3,
                                            line: 5,
                                        },
                                    },
                                    value: ' Блок текста,\nдлинного,\nс переносами\n',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 53,
                                            column: 3,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 74,
                                            column: 21,
                                            line: 6,
                                        },
                                    },
                                    value: '\nи я все еще параграф',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'Я параграф, %% Блок текста,\nдлинного,\nс переносами\n%%\nи я все еще параграф\n',
                title: 'Строчный форматер с переносами НЕ РВЕТ параграф (2)',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 75,
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
                                    offset: 75,
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
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                    },
                                    value: 'Я параграф, ',
                                },
                                {
                                    type: 'womFormatter',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 53,
                                            column: 3,
                                            line: 4,
                                        },
                                    },
                                    value: ' Блок текста,\nдлинного,\nс переносами\n',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 53,
                                            column: 3,
                                            line: 4,
                                        },
                                        end: {
                                            offset: 74,
                                            column: 21,
                                            line: 5,
                                        },
                                    },
                                    value: '\nи я все еще параграф',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'Я параграф, %% Блок текста,\nдлинного,\nс переносами\n%%\nи я все еще параграф %%и тут все очевидно%%\n',
                title: 'Строчный форматер с переносами НЕ РВЕТ параграф (2.1)',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 98,
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
                                    offset: 98,
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
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                    },
                                    value: 'Я параграф, ',
                                },
                                {
                                    type: 'womFormatter',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 53,
                                            column: 3,
                                            line: 4,
                                        },
                                    },
                                    value: ' Блок текста,\nдлинного,\nс переносами\n',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 53,
                                            column: 3,
                                            line: 4,
                                        },
                                        end: {
                                            offset: 75,
                                            column: 22,
                                            line: 5,
                                        },
                                    },
                                    value: '\nи я все еще параграф ',
                                },
                                {
                                    type: 'womFormatter',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 75,
                                            column: 22,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 97,
                                            column: 44,
                                            line: 5,
                                        },
                                    },
                                    value: 'и тут все очевидно',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'Я параграф, %% Блок текста,\nдлинного,\nс переносами\n%% и я все еще параграф\n',
                title: 'Строчный форматер с переносами НЕ РВЕТ параграф (3)',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 75,
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
                                    offset: 75,
                                    column: 1,
                                    line: 5,
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
                                    value: 'Я параграф, ',
                                },
                                {
                                    type: 'womFormatter',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 53,
                                            column: 3,
                                            line: 4,
                                        },
                                    },
                                    value: ' Блок текста,\nдлинного,\nс переносами\n',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 53,
                                            column: 3,
                                            line: 4,
                                        },
                                        end: {
                                            offset: 74,
                                            column: 24,
                                            line: 4,
                                        },
                                    },
                                    value: ' и я все еще параграф',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '1. test\n%% Блок текста %%\n',
                title: 'Строчный форматер НЕ РВЕТ список',
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
                                    offset: 25,
                                    column: 18,
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
                                            offset: 25,
                                            column: 18,
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
                                                    offset: 25,
                                                    column: 18,
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
                                                    type: 'womFormatter',
                                                    inline: true,
                                                    position: {
                                                        start: {
                                                            offset: 8,
                                                            column: 1,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 25,
                                                            column: 18,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'Блок текста',
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
                markup: '1. test\n%% Блок текста\nи тут продолжение,\nи тут тоже\n%%\n',
                title: 'Строчный форматер с переносами НЕ РВЕТ список',
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
                                    offset: 55,
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
                                            offset: 55,
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
                                                    offset: 55,
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
                                                    type: 'womFormatter',
                                                    inline: true,
                                                    position: {
                                                        start: {
                                                            offset: 8,
                                                            column: 1,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 55,
                                                            column: 3,
                                                            line: 5,
                                                        },
                                                    },
                                                    value: ' Блок текста\nи тут продолжение,\nи тут тоже\n',
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
