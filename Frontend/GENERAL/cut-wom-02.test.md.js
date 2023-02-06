module.exports = [
    {
        group: 'womCut <{}>',
        tests: [
            {
                markup: '<{ Прочитать !!red!! целиком\nЭтот текст можно увидеть, кликнув по ссылке "прочитать целиком".\n}>\n',
                title: 'Врезка (кат)',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 97,
                            column: 1,
                            line: 4,
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
                                                    offset: 3,
                                                    column: 4,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 13,
                                                    column: 14,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Прочитать ',
                                        },
                                        {
                                            type: 'womRemark',
                                            color: {
                                                type: 'color',
                                                raw: null,
                                                value: '@red',
                                            },
                                            position: {
                                                start: {
                                                    offset: 13,
                                                    column: 14,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 20,
                                                    column: 21,
                                                    line: 1,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 15,
                                                            column: 16,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 18,
                                                            column: 19,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'red',
                                                },
                                            ],
                                        },
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 20,
                                                    column: 21,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 28,
                                                    column: 29,
                                                    line: 1,
                                                },
                                            },
                                            value: ' целиком',
                                        },
                                    ],
                                },
                            ],
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 96,
                                    column: 3,
                                    line: 3,
                                },
                            },
                            children: [
                                {
                                    type: 'paragraph',
                                    position: {
                                        start: {
                                            offset: 29,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 93,
                                            column: 65,
                                            line: 2,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 29,
                                                    column: 1,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 93,
                                                    column: 65,
                                                    line: 2,
                                                },
                                            },
                                            value: 'Этот текст можно увидеть, кликнув по ссылке "прочитать целиком".',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '<{ One-line quote }>\n',
                title: 'Однострочная врезка',
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
                                                    offset: 18,
                                                    column: 19,
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
                                                            offset: 17,
                                                            column: 18,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'One-line quote',
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
                                            offset: 20,
                                            column: 21,
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
                markup: '<{пустой кат\n}>\n',
                title: 'Пустой кат',
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
                                            offset: 12,
                                            column: 13,
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
                                                    offset: 12,
                                                    column: 13,
                                                    line: 1,
                                                },
                                            },
                                            value: 'пустой кат',
                                        },
                                    ],
                                },
                            ],
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
                                    line: 2,
                                },
                            },
                            children: [
                            ],
                        },
                    ],
                },
            },
            {
                markup: '<{\n}>\n',
                title: 'Кат без заголовка',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 6,
                            column: 1,
                            line: 3,
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
                                    offset: 5,
                                    column: 3,
                                    line: 2,
                                },
                            },
                            children: [
                            ],
                        },
                    ],
                },
            },
            {
                markup: '<{\n',
                title: 'Незакрытый кат',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 3,
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
                                    offset: 3,
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
                                            offset: 2,
                                            column: 3,
                                            line: 1,
                                        },
                                    },
                                    value: '<{',
                                },
                            ],
                        },
                    ],
                },
            },
        ],
    },
];
