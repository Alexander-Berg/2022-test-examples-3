module.exports = [
    {
        group: 'Списки (list, womList)',
        tests: [
            {
                markup: '1. outer\n1. outer <{подробнее\n1. inner\n1. inner\n}>\n1. outer\n',
                title: 'Строчный кат в списке, внутри которого список',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 60,
                            column: 1,
                            line: 7,
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
                                    offset: 59,
                                    column: 9,
                                    line: 6,
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
                                            offset: 8,
                                            column: 9,
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
                                                    offset: 8,
                                                    column: 9,
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
                                                            offset: 8,
                                                            column: 9,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'outer',
                                                },
                                            ],
                                        },
                                    ],
                                },
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 9,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 50,
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
                                                    offset: 12,
                                                    column: 4,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 50,
                                                    column: 3,
                                                    line: 5,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 12,
                                                            column: 4,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 18,
                                                            column: 10,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'outer ',
                                                },
                                                {
                                                    type: 'womCut',
                                                    title: [
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 20,
                                                                    column: 12,
                                                                    line: 2,
                                                                },
                                                                end: {
                                                                    offset: 29,
                                                                    column: 21,
                                                                    line: 2,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'text',
                                                                    position: {
                                                                        start: {
                                                                            offset: 20,
                                                                            column: 12,
                                                                            line: 2,
                                                                        },
                                                                        end: {
                                                                            offset: 29,
                                                                            column: 21,
                                                                            line: 2,
                                                                        },
                                                                    },
                                                                    value: 'подробнее',
                                                                },
                                                            ],
                                                        },
                                                    ],
                                                    inline: true,
                                                    position: {
                                                        start: {
                                                            offset: 18,
                                                            column: 10,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 50,
                                                            column: 3,
                                                            line: 5,
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
                                                                    offset: 30,
                                                                    column: 1,
                                                                    line: 3,
                                                                },
                                                                end: {
                                                                    offset: 47,
                                                                    column: 9,
                                                                    line: 4,
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
                                                                            offset: 30,
                                                                            column: 1,
                                                                            line: 3,
                                                                        },
                                                                        end: {
                                                                            offset: 38,
                                                                            column: 9,
                                                                            line: 3,
                                                                        },
                                                                    },
                                                                    restart: null,
                                                                    children: [
                                                                        {
                                                                            type: 'paragraph',
                                                                            position: {
                                                                                start: {
                                                                                    offset: 33,
                                                                                    column: 4,
                                                                                    line: 3,
                                                                                },
                                                                                end: {
                                                                                    offset: 38,
                                                                                    column: 9,
                                                                                    line: 3,
                                                                                },
                                                                            },
                                                                            children: [
                                                                                {
                                                                                    type: 'text',
                                                                                    position: {
                                                                                        start: {
                                                                                            offset: 33,
                                                                                            column: 4,
                                                                                            line: 3,
                                                                                        },
                                                                                        end: {
                                                                                            offset: 38,
                                                                                            column: 9,
                                                                                            line: 3,
                                                                                        },
                                                                                    },
                                                                                    value: 'inner',
                                                                                },
                                                                            ],
                                                                        },
                                                                    ],
                                                                },
                                                                {
                                                                    type: 'listItem',
                                                                    checked: null,
                                                                    expandable: false,
                                                                    loose: false,
                                                                    position: {
                                                                        start: {
                                                                            offset: 39,
                                                                            column: 1,
                                                                            line: 4,
                                                                        },
                                                                        end: {
                                                                            offset: 47,
                                                                            column: 9,
                                                                            line: 4,
                                                                        },
                                                                    },
                                                                    restart: null,
                                                                    children: [
                                                                        {
                                                                            type: 'paragraph',
                                                                            position: {
                                                                                start: {
                                                                                    offset: 42,
                                                                                    column: 4,
                                                                                    line: 4,
                                                                                },
                                                                                end: {
                                                                                    offset: 47,
                                                                                    column: 9,
                                                                                    line: 4,
                                                                                },
                                                                            },
                                                                            children: [
                                                                                {
                                                                                    type: 'text',
                                                                                    position: {
                                                                                        start: {
                                                                                            offset: 42,
                                                                                            column: 4,
                                                                                            line: 4,
                                                                                        },
                                                                                        end: {
                                                                                            offset: 47,
                                                                                            column: 9,
                                                                                            line: 4,
                                                                                        },
                                                                                    },
                                                                                    value: 'inner',
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
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 51,
                                            column: 1,
                                            line: 6,
                                        },
                                        end: {
                                            offset: 59,
                                            column: 9,
                                            line: 6,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 54,
                                                    column: 4,
                                                    line: 6,
                                                },
                                                end: {
                                                    offset: 59,
                                                    column: 9,
                                                    line: 6,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 54,
                                                            column: 4,
                                                            line: 6,
                                                        },
                                                        end: {
                                                            offset: 59,
                                                            column: 9,
                                                            line: 6,
                                                        },
                                                    },
                                                    value: 'outer',
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
