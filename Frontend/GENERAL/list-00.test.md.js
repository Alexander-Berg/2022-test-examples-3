module.exports = [
    {
        group: 'Списки (list, womList)',
        tests: [
            {
                markup: '* test1\n* ***\n* test2\n',
                title: 'Thematic break',
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
                            line: 4,
                        },
                    },
                    children: [
                        {
                            type: 'list',
                            start: null,
                            loose: false,
                            ordered: false,
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
                                                    offset: 2,
                                                    column: 3,
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
                                                            offset: 2,
                                                            column: 3,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 7,
                                                            column: 8,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'test1',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'thematicBreak',
                            position: {
                                start: {
                                    offset: 8,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 13,
                                    column: 6,
                                    line: 2,
                                },
                            },
                        },
                        {
                            type: 'list',
                            start: null,
                            loose: false,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 14,
                                    column: 1,
                                    line: 3,
                                },
                                end: {
                                    offset: 21,
                                    column: 8,
                                    line: 3,
                                },
                            },
                            children: [
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 14,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 21,
                                            column: 8,
                                            line: 3,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 16,
                                                    column: 3,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 21,
                                                    column: 8,
                                                    line: 3,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 16,
                                                            column: 3,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 21,
                                                            column: 8,
                                                            line: 3,
                                                        },
                                                    },
                                                    value: 'test2',
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
                markup: '* item\n\n1. item',
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
                            column: 8,
                            line: 3,
                        },
                    },
                    children: [
                        {
                            type: 'list',
                            start: null,
                            loose: false,
                            ordered: false,
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
                                            offset: 6,
                                            column: 7,
                                            line: 1,
                                        },
                                    },
                                    restart: null,
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
                                                    value: 'item',
                                                },
                                            ],
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
                                    offset: 8,
                                    column: 1,
                                    line: 3,
                                },
                                end: {
                                    offset: 15,
                                    column: 8,
                                    line: 3,
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
                                            offset: 8,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 15,
                                            column: 8,
                                            line: 3,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 11,
                                                    column: 4,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 15,
                                                    column: 8,
                                                    line: 3,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 11,
                                                            column: 4,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 15,
                                                            column: 8,
                                                            line: 3,
                                                        },
                                                    },
                                                    value: 'item',
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
                markup: '* item\n\n123item',
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
                            column: 8,
                            line: 3,
                        },
                    },
                    children: [
                        {
                            type: 'list',
                            start: null,
                            loose: false,
                            ordered: false,
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
                                            offset: 6,
                                            column: 7,
                                            line: 1,
                                        },
                                    },
                                    restart: null,
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
                                                    value: 'item',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 8,
                                    column: 1,
                                    line: 3,
                                },
                                end: {
                                    offset: 15,
                                    column: 8,
                                    line: 3,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 8,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 15,
                                            column: 8,
                                            line: 3,
                                        },
                                    },
                                    value: '123item',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '* item\n\n1.3item',
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
                            column: 8,
                            line: 3,
                        },
                    },
                    children: [
                        {
                            type: 'list',
                            start: null,
                            loose: false,
                            ordered: false,
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
                                            offset: 6,
                                            column: 7,
                                            line: 1,
                                        },
                                    },
                                    restart: null,
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
                                                    value: 'item',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 8,
                                    column: 1,
                                    line: 3,
                                },
                                end: {
                                    offset: 15,
                                    column: 8,
                                    line: 3,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 8,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 15,
                                            column: 8,
                                            line: 3,
                                        },
                                    },
                                    value: '1.3item',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '* item\nxitemx',
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
                            column: 7,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'list',
                            start: null,
                            loose: false,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 13,
                                    column: 7,
                                    line: 2,
                                },
                            },
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
                                            offset: 13,
                                            column: 7,
                                            line: 2,
                                        },
                                    },
                                    restart: null,
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
                                                    offset: 13,
                                                    column: 7,
                                                    line: 2,
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
                                                            offset: 13,
                                                            column: 7,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'item\nxitemx',
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
                markup: '* item\n*itemx',
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
                            column: 7,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'list',
                            start: null,
                            loose: false,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 13,
                                    column: 7,
                                    line: 2,
                                },
                            },
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
                                            offset: 13,
                                            column: 7,
                                            line: 2,
                                        },
                                    },
                                    restart: null,
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
                                                    offset: 13,
                                                    column: 7,
                                                    line: 2,
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
                                                            offset: 13,
                                                            column: 7,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'item\n*itemx',
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
                markup: '* item\n*item*',
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
                            column: 7,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'list',
                            start: null,
                            loose: false,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 13,
                                    column: 7,
                                    line: 2,
                                },
                            },
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
                                            offset: 13,
                                            column: 7,
                                            line: 2,
                                        },
                                    },
                                    restart: null,
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
                                                    offset: 13,
                                                    column: 7,
                                                    line: 2,
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
                                                            offset: 7,
                                                            column: 1,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'item\n',
                                                },
                                                {
                                                    type: 'emphasis',
                                                    position: {
                                                        start: {
                                                            offset: 7,
                                                            column: 1,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 13,
                                                            column: 7,
                                                            line: 2,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'text',
                                                            position: {
                                                                start: {
                                                                    offset: 8,
                                                                    column: 2,
                                                                    line: 2,
                                                                },
                                                                end: {
                                                                    offset: 12,
                                                                    column: 6,
                                                                    line: 2,
                                                                },
                                                            },
                                                            value: 'item',
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
                markup: '* item\n_item_',
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
                            column: 7,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'list',
                            start: null,
                            loose: false,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 13,
                                    column: 7,
                                    line: 2,
                                },
                            },
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
                                            offset: 13,
                                            column: 7,
                                            line: 2,
                                        },
                                    },
                                    restart: null,
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
                                                    offset: 13,
                                                    column: 7,
                                                    line: 2,
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
                                                            offset: 7,
                                                            column: 1,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'item\n',
                                                },
                                                {
                                                    type: 'emphasis',
                                                    position: {
                                                        start: {
                                                            offset: 7,
                                                            column: 1,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 13,
                                                            column: 7,
                                                            line: 2,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'text',
                                                            position: {
                                                                start: {
                                                                    offset: 8,
                                                                    column: 2,
                                                                    line: 2,
                                                                },
                                                                end: {
                                                                    offset: 12,
                                                                    column: 6,
                                                                    line: 2,
                                                                },
                                                            },
                                                            value: 'item',
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
