module.exports = [
    {
        group: 'womBlock {[]}',
        tests: [
            {
                markup: '{[\ntest\n]}',
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
                            column: 3,
                            line: 3,
                        },
                    },
                    children: [
                        {
                            type: 'womBlock',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 10,
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
                                            offset: 7,
                                            column: 5,
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
                                                    offset: 7,
                                                    column: 5,
                                                    line: 2,
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
            },
            {
                markup: '{[\n\nx\n\n{[\n\nx\n\n]}\n\nx\n\n]}',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 23,
                            column: 3,
                            line: 13,
                        },
                    },
                    children: [
                        {
                            type: 'womBlock',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 23,
                                    column: 3,
                                    line: 13,
                                },
                            },
                            children: [
                                {
                                    type: 'paragraph',
                                    position: {
                                        start: {
                                            offset: 4,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 6,
                                            column: 1,
                                            line: 4,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 4,
                                                    column: 1,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 5,
                                                    column: 2,
                                                    line: 3,
                                                },
                                            },
                                            value: 'x',
                                        },
                                    ],
                                },
                                {
                                    type: 'womBlock',
                                    inline: false,
                                    position: {
                                        start: {
                                            offset: 7,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 16,
                                            column: 3,
                                            line: 9,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 11,
                                                    column: 1,
                                                    line: 7,
                                                },
                                                end: {
                                                    offset: 12,
                                                    column: 2,
                                                    line: 7,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 11,
                                                            column: 1,
                                                            line: 7,
                                                        },
                                                        end: {
                                                            offset: 12,
                                                            column: 2,
                                                            line: 7,
                                                        },
                                                    },
                                                    value: 'x',
                                                },
                                            ],
                                        },
                                    ],
                                },
                                {
                                    type: 'paragraph',
                                    position: {
                                        start: {
                                            offset: 18,
                                            column: 1,
                                            line: 11,
                                        },
                                        end: {
                                            offset: 19,
                                            column: 2,
                                            line: 11,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 18,
                                                    column: 1,
                                                    line: 11,
                                                },
                                                end: {
                                                    offset: 19,
                                                    column: 2,
                                                    line: 11,
                                                },
                                            },
                                            value: 'x',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '{[\nx\n\n{[\nx\n]}\nx\n]}',
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
                            column: 3,
                            line: 8,
                        },
                    },
                    children: [
                        {
                            type: 'womBlock',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 18,
                                    column: 3,
                                    line: 8,
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
                                            offset: 5,
                                            column: 1,
                                            line: 3,
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
                                                    offset: 4,
                                                    column: 2,
                                                    line: 2,
                                                },
                                            },
                                            value: 'x',
                                        },
                                    ],
                                },
                                {
                                    type: 'womBlock',
                                    inline: false,
                                    position: {
                                        start: {
                                            offset: 6,
                                            column: 1,
                                            line: 4,
                                        },
                                        end: {
                                            offset: 13,
                                            column: 3,
                                            line: 6,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 9,
                                                    column: 1,
                                                    line: 5,
                                                },
                                                end: {
                                                    offset: 10,
                                                    column: 2,
                                                    line: 5,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 9,
                                                            column: 1,
                                                            line: 5,
                                                        },
                                                        end: {
                                                            offset: 10,
                                                            column: 2,
                                                            line: 5,
                                                        },
                                                    },
                                                    value: 'x',
                                                },
                                            ],
                                        },
                                    ],
                                },
                                {
                                    type: 'paragraph',
                                    position: {
                                        start: {
                                            offset: 14,
                                            column: 1,
                                            line: 7,
                                        },
                                        end: {
                                            offset: 15,
                                            column: 2,
                                            line: 7,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 14,
                                                    column: 1,
                                                    line: 7,
                                                },
                                                end: {
                                                    offset: 15,
                                                    column: 2,
                                                    line: 7,
                                                },
                                            },
                                            value: 'x',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '{[\nx\n{[\nx\n]}\nx\n]}',
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
                            column: 3,
                            line: 7,
                        },
                    },
                    children: [
                        {
                            type: 'womBlock',
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
                                    line: 7,
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
                                            offset: 5,
                                            column: 1,
                                            line: 3,
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
                                                    offset: 4,
                                                    column: 2,
                                                    line: 2,
                                                },
                                            },
                                            value: 'x',
                                        },
                                    ],
                                },
                                {
                                    type: 'womBlock',
                                    inline: false,
                                    position: {
                                        start: {
                                            offset: 5,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 12,
                                            column: 3,
                                            line: 5,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 8,
                                                    column: 1,
                                                    line: 4,
                                                },
                                                end: {
                                                    offset: 9,
                                                    column: 2,
                                                    line: 4,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 8,
                                                            column: 1,
                                                            line: 4,
                                                        },
                                                        end: {
                                                            offset: 9,
                                                            column: 2,
                                                            line: 4,
                                                        },
                                                    },
                                                    value: 'x',
                                                },
                                            ],
                                        },
                                    ],
                                },
                                {
                                    type: 'paragraph',
                                    position: {
                                        start: {
                                            offset: 13,
                                            column: 1,
                                            line: 6,
                                        },
                                        end: {
                                            offset: 14,
                                            column: 2,
                                            line: 6,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 13,
                                                    column: 1,
                                                    line: 6,
                                                },
                                                end: {
                                                    offset: 14,
                                                    column: 2,
                                                    line: 6,
                                                },
                                            },
                                            value: 'x',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '{[\nx\n{[x]}\nx\n]}',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 15,
                            column: 3,
                            line: 5,
                        },
                    },
                    children: [
                        {
                            type: 'womBlock',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 15,
                                    column: 3,
                                    line: 5,
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
                                            offset: 12,
                                            column: 2,
                                            line: 4,
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
                                                    offset: 5,
                                                    column: 1,
                                                    line: 3,
                                                },
                                            },
                                            value: 'x\n',
                                        },
                                        {
                                            type: 'womBlock',
                                            inline: true,
                                            position: {
                                                start: {
                                                    offset: 5,
                                                    column: 1,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 10,
                                                    column: 6,
                                                    line: 3,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'paragraph',
                                                    position: {
                                                        start: {
                                                            offset: 7,
                                                            column: 3,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 8,
                                                            column: 4,
                                                            line: 3,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'text',
                                                            position: {
                                                                start: {
                                                                    offset: 7,
                                                                    column: 3,
                                                                    line: 3,
                                                                },
                                                                end: {
                                                                    offset: 8,
                                                                    column: 4,
                                                                    line: 3,
                                                                },
                                                            },
                                                            value: 'x',
                                                        },
                                                    ],
                                                },
                                            ],
                                        },
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 10,
                                                    column: 6,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 12,
                                                    column: 2,
                                                    line: 4,
                                                },
                                            },
                                            value: '\nx',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '{[test]}',
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
                                    type: 'womBlock',
                                    inline: true,
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
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 6,
                                                    column: 7,
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
                                                            offset: 6,
                                                            column: 7,
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
                    ],
                },
            },
            {
                markup: '{[\nБлок текста\n]}\n',
                title: 'Блочный блок текста РВЕТ параграф',
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
                            type: 'womBlock',
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
                markup: '1. test\n{[\nБлок текста\n]}\n1. test\n',
                title: 'Блочный блок текста РВЕТ список',
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
                            type: 'womBlock',
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
                markup: '{[ Блок текста ]}\n',
                title: 'Строчный блок текста НЕ РВЕТ параграф',
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
                                    type: 'womBlock',
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
                                        {
                                            type: 'paragraph',
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
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '{[ Блок текста,\nдлинного,\nс переносами\n]}\n',
                title: 'Строчный блок текста с переносами НЕ РВЕТ параграф',
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
                                    offset: 42,
                                    column: 1,
                                    line: 5,
                                },
                            },
                            children: [
                                {
                                    type: 'womBlock',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 41,
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
                                                    offset: 38,
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
                                                            offset: 38,
                                                            column: 13,
                                                            line: 3,
                                                        },
                                                    },
                                                    value: 'Блок текста,\nдлинного,\nс переносами',
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
                markup: '1. test\n{[ Блок текста ]}\n',
                title: 'Строчный блок текста НЕ РВЕТ список',
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
                                                    type: 'womBlock',
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
                                                        {
                                                            type: 'paragraph',
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
                markup: '1. test\n{[ Блок текста\nи тут продолжение,\nи тут тоже\n]}\n',
                title: 'Строчный блок текста с переносами НЕ РВЕТ список',
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
                                                    type: 'womBlock',
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
                                                                    offset: 52,
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
                                                                            offset: 52,
                                                                            column: 11,
                                                                            line: 4,
                                                                        },
                                                                    },
                                                                    value: 'Блок текста\nи тут продолжение,\nи тут тоже',
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
