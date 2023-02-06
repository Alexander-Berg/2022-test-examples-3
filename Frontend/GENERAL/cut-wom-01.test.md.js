module.exports = [
    {
        group: 'womCut <{}>',
        tests: [
            {
                markup: '<{\nБлок текста\n}>\n',
                title: 'Блочный кат РВЕТ параграф',
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
                            type: 'womCut',
                            title: [
                            ],
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
                                            offset: 14,
                                            column: 12,
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
                                                    offset: 14,
                                                    column: 12,
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
            },
            {
                markup: '1. test\n<{\nБлок текста\n}>\n1. test\n',
                title: 'Блочный кат РВЕТ список',
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
                            type: 'womCut',
                            title: [
                            ],
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
                                            offset: 22,
                                            column: 12,
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
                                                    offset: 22,
                                                    column: 12,
                                                    line: 3,
                                                },
                                            },
                                            value: 'Блок текста',
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
                markup: '<{ Блок текста }>\n',
                title: 'Строчный кат НЕ РВЕТ параграф',
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
                                    type: 'womCut',
                                    title: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 2,
                                                    column: 3,
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
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 3,
                                                            column: 4,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 14,
                                                            column: 15,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'Блок текста',
                                                },
                                            ],
                                        },
                                    ],
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
                                    children: [
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '1. test\n<{ Блок текста }>\n',
                title: 'Строчный кат НЕ РВЕТ список',
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
                                                    type: 'womCut',
                                                    title: [
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 10,
                                                                    column: 3,
                                                                    line: 2,
                                                                },
                                                                end: {
                                                                    offset: 23,
                                                                    column: 16,
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
                                                                            offset: 22,
                                                                            column: 15,
                                                                            line: 2,
                                                                        },
                                                                    },
                                                                    value: 'Блок текста',
                                                                },
                                                            ],
                                                        },
                                                    ],
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
                                                    children: [
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
