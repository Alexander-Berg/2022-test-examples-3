module.exports = [
    {
        group: 'Списки (list, womList)',
        tests: [
            {
                markup: '*\n*\n*\n*\n',
                title: 'Ненумерованный пустой список',
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
                            column: 1,
                            line: 5,
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
                                    column: 2,
                                    line: 4,
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
                                            offset: 1,
                                            column: 2,
                                            line: 1,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                    ],
                                },
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 2,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 3,
                                            column: 2,
                                            line: 2,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                    ],
                                },
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: false,
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
                                    restart: null,
                                    children: [
                                    ],
                                },
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 6,
                                            column: 1,
                                            line: 4,
                                        },
                                        end: {
                                            offset: 7,
                                            column: 2,
                                            line: 4,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: ' -\n\n',
                title: 'Одиночные минусы это не пустой список',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 4,
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
                                            offset: 1,
                                            column: 2,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 2,
                                            column: 3,
                                            line: 1,
                                        },
                                    },
                                    value: '-',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: ' *\n\n',
                title: 'Одиночные звезды это не пустой список',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 4,
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
                                            offset: 1,
                                            column: 2,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 2,
                                            column: 3,
                                            line: 1,
                                        },
                                    },
                                    value: '*',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: ' +\n\n',
                title: 'Одиночные плюсы это не пустой список',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 4,
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
                                            offset: 1,
                                            column: 2,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 2,
                                            column: 3,
                                            line: 1,
                                        },
                                    },
                                    value: '+',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '+   One:\n    +   Nested one;\n    +   Nested two:\n        +   Nested three.\n+   Two;\n+   Three.\n',
                title: 'Ненумерованный список через +',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 95,
                            column: 1,
                            line: 7,
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
                                    offset: 94,
                                    column: 11,
                                    line: 6,
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
                                            offset: 74,
                                            column: 26,
                                            line: 4,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 4,
                                                    column: 5,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 13,
                                                    column: 5,
                                                    line: 2,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 4,
                                                            column: 5,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 8,
                                                            column: 9,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'One:',
                                                },
                                            ],
                                        },
                                        {
                                            type: 'list',
                                            start: null,
                                            loose: false,
                                            ordered: false,
                                            position: {
                                                start: {
                                                    offset: 13,
                                                    column: 5,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 74,
                                                    column: 26,
                                                    line: 4,
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
                                                            offset: 13,
                                                            column: 5,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 28,
                                                            column: 20,
                                                            line: 2,
                                                        },
                                                    },
                                                    restart: null,
                                                    children: [
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 17,
                                                                    column: 9,
                                                                    line: 2,
                                                                },
                                                                end: {
                                                                    offset: 28,
                                                                    column: 20,
                                                                    line: 2,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'text',
                                                                    position: {
                                                                        start: {
                                                                            offset: 17,
                                                                            column: 9,
                                                                            line: 2,
                                                                        },
                                                                        end: {
                                                                            offset: 28,
                                                                            column: 20,
                                                                            line: 2,
                                                                        },
                                                                    },
                                                                    value: 'Nested one;',
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
                                                            offset: 33,
                                                            column: 5,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 74,
                                                            column: 26,
                                                            line: 4,
                                                        },
                                                    },
                                                    restart: null,
                                                    children: [
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 37,
                                                                    column: 9,
                                                                    line: 3,
                                                                },
                                                                end: {
                                                                    offset: 57,
                                                                    column: 9,
                                                                    line: 4,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'text',
                                                                    position: {
                                                                        start: {
                                                                            offset: 37,
                                                                            column: 9,
                                                                            line: 3,
                                                                        },
                                                                        end: {
                                                                            offset: 48,
                                                                            column: 20,
                                                                            line: 3,
                                                                        },
                                                                    },
                                                                    value: 'Nested two:',
                                                                },
                                                            ],
                                                        },
                                                        {
                                                            type: 'list',
                                                            start: null,
                                                            loose: false,
                                                            ordered: false,
                                                            position: {
                                                                start: {
                                                                    offset: 57,
                                                                    column: 9,
                                                                    line: 4,
                                                                },
                                                                end: {
                                                                    offset: 74,
                                                                    column: 26,
                                                                    line: 4,
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
                                                                            offset: 57,
                                                                            column: 9,
                                                                            line: 4,
                                                                        },
                                                                        end: {
                                                                            offset: 74,
                                                                            column: 26,
                                                                            line: 4,
                                                                        },
                                                                    },
                                                                    restart: null,
                                                                    children: [
                                                                        {
                                                                            type: 'paragraph',
                                                                            position: {
                                                                                start: {
                                                                                    offset: 61,
                                                                                    column: 13,
                                                                                    line: 4,
                                                                                },
                                                                                end: {
                                                                                    offset: 74,
                                                                                    column: 26,
                                                                                    line: 4,
                                                                                },
                                                                            },
                                                                            children: [
                                                                                {
                                                                                    type: 'text',
                                                                                    position: {
                                                                                        start: {
                                                                                            offset: 61,
                                                                                            column: 13,
                                                                                            line: 4,
                                                                                        },
                                                                                        end: {
                                                                                            offset: 74,
                                                                                            column: 26,
                                                                                            line: 4,
                                                                                        },
                                                                                    },
                                                                                    value: 'Nested three.',
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
                                            offset: 75,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 83,
                                            column: 9,
                                            line: 5,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 79,
                                                    column: 5,
                                                    line: 5,
                                                },
                                                end: {
                                                    offset: 83,
                                                    column: 9,
                                                    line: 5,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 79,
                                                            column: 5,
                                                            line: 5,
                                                        },
                                                        end: {
                                                            offset: 83,
                                                            column: 9,
                                                            line: 5,
                                                        },
                                                    },
                                                    value: 'Two;',
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
                                            offset: 84,
                                            column: 1,
                                            line: 6,
                                        },
                                        end: {
                                            offset: 94,
                                            column: 11,
                                            line: 6,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 88,
                                                    column: 5,
                                                    line: 6,
                                                },
                                                end: {
                                                    offset: 94,
                                                    column: 11,
                                                    line: 6,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 88,
                                                            column: 5,
                                                            line: 6,
                                                        },
                                                        end: {
                                                            offset: 94,
                                                            column: 11,
                                                            line: 6,
                                                        },
                                                    },
                                                    value: 'Three.',
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
                markup: '-   One:\n    -   Nested one;\n    -   Nested two:\n        -   Nested three.\n-   Two;\n-   Three.\n',
                title: 'Ненумерованный список через -',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 95,
                            column: 1,
                            line: 7,
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
                                    offset: 94,
                                    column: 11,
                                    line: 6,
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
                                            offset: 74,
                                            column: 26,
                                            line: 4,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 4,
                                                    column: 5,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 13,
                                                    column: 5,
                                                    line: 2,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 4,
                                                            column: 5,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 8,
                                                            column: 9,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'One:',
                                                },
                                            ],
                                        },
                                        {
                                            type: 'list',
                                            start: null,
                                            loose: false,
                                            ordered: false,
                                            position: {
                                                start: {
                                                    offset: 13,
                                                    column: 5,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 74,
                                                    column: 26,
                                                    line: 4,
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
                                                            offset: 13,
                                                            column: 5,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 28,
                                                            column: 20,
                                                            line: 2,
                                                        },
                                                    },
                                                    restart: null,
                                                    children: [
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 17,
                                                                    column: 9,
                                                                    line: 2,
                                                                },
                                                                end: {
                                                                    offset: 28,
                                                                    column: 20,
                                                                    line: 2,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'text',
                                                                    position: {
                                                                        start: {
                                                                            offset: 17,
                                                                            column: 9,
                                                                            line: 2,
                                                                        },
                                                                        end: {
                                                                            offset: 28,
                                                                            column: 20,
                                                                            line: 2,
                                                                        },
                                                                    },
                                                                    value: 'Nested one;',
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
                                                            offset: 33,
                                                            column: 5,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 74,
                                                            column: 26,
                                                            line: 4,
                                                        },
                                                    },
                                                    restart: null,
                                                    children: [
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 37,
                                                                    column: 9,
                                                                    line: 3,
                                                                },
                                                                end: {
                                                                    offset: 57,
                                                                    column: 9,
                                                                    line: 4,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'text',
                                                                    position: {
                                                                        start: {
                                                                            offset: 37,
                                                                            column: 9,
                                                                            line: 3,
                                                                        },
                                                                        end: {
                                                                            offset: 48,
                                                                            column: 20,
                                                                            line: 3,
                                                                        },
                                                                    },
                                                                    value: 'Nested two:',
                                                                },
                                                            ],
                                                        },
                                                        {
                                                            type: 'list',
                                                            start: null,
                                                            loose: false,
                                                            ordered: false,
                                                            position: {
                                                                start: {
                                                                    offset: 57,
                                                                    column: 9,
                                                                    line: 4,
                                                                },
                                                                end: {
                                                                    offset: 74,
                                                                    column: 26,
                                                                    line: 4,
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
                                                                            offset: 57,
                                                                            column: 9,
                                                                            line: 4,
                                                                        },
                                                                        end: {
                                                                            offset: 74,
                                                                            column: 26,
                                                                            line: 4,
                                                                        },
                                                                    },
                                                                    restart: null,
                                                                    children: [
                                                                        {
                                                                            type: 'paragraph',
                                                                            position: {
                                                                                start: {
                                                                                    offset: 61,
                                                                                    column: 13,
                                                                                    line: 4,
                                                                                },
                                                                                end: {
                                                                                    offset: 74,
                                                                                    column: 26,
                                                                                    line: 4,
                                                                                },
                                                                            },
                                                                            children: [
                                                                                {
                                                                                    type: 'text',
                                                                                    position: {
                                                                                        start: {
                                                                                            offset: 61,
                                                                                            column: 13,
                                                                                            line: 4,
                                                                                        },
                                                                                        end: {
                                                                                            offset: 74,
                                                                                            column: 26,
                                                                                            line: 4,
                                                                                        },
                                                                                    },
                                                                                    value: 'Nested three.',
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
                                            offset: 75,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 83,
                                            column: 9,
                                            line: 5,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 79,
                                                    column: 5,
                                                    line: 5,
                                                },
                                                end: {
                                                    offset: 83,
                                                    column: 9,
                                                    line: 5,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 79,
                                                            column: 5,
                                                            line: 5,
                                                        },
                                                        end: {
                                                            offset: 83,
                                                            column: 9,
                                                            line: 5,
                                                        },
                                                    },
                                                    value: 'Two;',
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
                                            offset: 84,
                                            column: 1,
                                            line: 6,
                                        },
                                        end: {
                                            offset: 94,
                                            column: 11,
                                            line: 6,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 88,
                                                    column: 5,
                                                    line: 6,
                                                },
                                                end: {
                                                    offset: 94,
                                                    column: 11,
                                                    line: 6,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 88,
                                                            column: 5,
                                                            line: 6,
                                                        },
                                                        end: {
                                                            offset: 94,
                                                            column: 11,
                                                            line: 6,
                                                        },
                                                    },
                                                    value: 'Three.',
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
                markup: '* One:\n    * Nested one;\n    * Nested two:\n        * Nested three.\n* Two;\n* Three.\n',
                title: 'Ненумерованный список через *',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 83,
                            column: 1,
                            line: 7,
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
                                    offset: 82,
                                    column: 9,
                                    line: 6,
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
                                            offset: 66,
                                            column: 24,
                                            line: 4,
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
                                                    offset: 9,
                                                    column: 3,
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
                                                            offset: 6,
                                                            column: 7,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'One:',
                                                },
                                            ],
                                        },
                                        {
                                            type: 'list',
                                            start: null,
                                            loose: false,
                                            ordered: false,
                                            position: {
                                                start: {
                                                    offset: 9,
                                                    column: 3,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 66,
                                                    column: 24,
                                                    line: 4,
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
                                                            offset: 9,
                                                            column: 3,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 24,
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
                                                                    offset: 13,
                                                                    column: 7,
                                                                    line: 2,
                                                                },
                                                                end: {
                                                                    offset: 24,
                                                                    column: 18,
                                                                    line: 2,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'text',
                                                                    position: {
                                                                        start: {
                                                                            offset: 13,
                                                                            column: 7,
                                                                            line: 2,
                                                                        },
                                                                        end: {
                                                                            offset: 24,
                                                                            column: 18,
                                                                            line: 2,
                                                                        },
                                                                    },
                                                                    value: 'Nested one;',
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
                                                            offset: 27,
                                                            column: 3,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 66,
                                                            column: 24,
                                                            line: 4,
                                                        },
                                                    },
                                                    restart: null,
                                                    children: [
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 31,
                                                                    column: 7,
                                                                    line: 3,
                                                                },
                                                                end: {
                                                                    offset: 49,
                                                                    column: 7,
                                                                    line: 4,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'text',
                                                                    position: {
                                                                        start: {
                                                                            offset: 31,
                                                                            column: 7,
                                                                            line: 3,
                                                                        },
                                                                        end: {
                                                                            offset: 42,
                                                                            column: 18,
                                                                            line: 3,
                                                                        },
                                                                    },
                                                                    value: 'Nested two:',
                                                                },
                                                            ],
                                                        },
                                                        {
                                                            type: 'list',
                                                            start: null,
                                                            loose: false,
                                                            ordered: false,
                                                            position: {
                                                                start: {
                                                                    offset: 49,
                                                                    column: 7,
                                                                    line: 4,
                                                                },
                                                                end: {
                                                                    offset: 66,
                                                                    column: 24,
                                                                    line: 4,
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
                                                                            offset: 49,
                                                                            column: 7,
                                                                            line: 4,
                                                                        },
                                                                        end: {
                                                                            offset: 66,
                                                                            column: 24,
                                                                            line: 4,
                                                                        },
                                                                    },
                                                                    restart: null,
                                                                    children: [
                                                                        {
                                                                            type: 'paragraph',
                                                                            position: {
                                                                                start: {
                                                                                    offset: 53,
                                                                                    column: 11,
                                                                                    line: 4,
                                                                                },
                                                                                end: {
                                                                                    offset: 66,
                                                                                    column: 24,
                                                                                    line: 4,
                                                                                },
                                                                            },
                                                                            children: [
                                                                                {
                                                                                    type: 'text',
                                                                                    position: {
                                                                                        start: {
                                                                                            offset: 53,
                                                                                            column: 11,
                                                                                            line: 4,
                                                                                        },
                                                                                        end: {
                                                                                            offset: 66,
                                                                                            column: 24,
                                                                                            line: 4,
                                                                                        },
                                                                                    },
                                                                                    value: 'Nested three.',
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
                                            offset: 67,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 73,
                                            column: 7,
                                            line: 5,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 69,
                                                    column: 3,
                                                    line: 5,
                                                },
                                                end: {
                                                    offset: 73,
                                                    column: 7,
                                                    line: 5,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 69,
                                                            column: 3,
                                                            line: 5,
                                                        },
                                                        end: {
                                                            offset: 73,
                                                            column: 7,
                                                            line: 5,
                                                        },
                                                    },
                                                    value: 'Two;',
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
                                            offset: 74,
                                            column: 1,
                                            line: 6,
                                        },
                                        end: {
                                            offset: 82,
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
                                                    offset: 76,
                                                    column: 3,
                                                    line: 6,
                                                },
                                                end: {
                                                    offset: 82,
                                                    column: 9,
                                                    line: 6,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 76,
                                                            column: 3,
                                                            line: 6,
                                                        },
                                                        end: {
                                                            offset: 82,
                                                            column: 9,
                                                            line: 6,
                                                        },
                                                    },
                                                    value: 'Three.',
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
                markup: '1.\n2.\n3.\n4.\n',
                title: 'Нумерованный пустой список',
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
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 11,
                                    column: 3,
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
                                    restart: null,
                                    children: [
                                    ],
                                },
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 3,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 5,
                                            column: 3,
                                            line: 2,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                    ],
                                },
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 6,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 8,
                                            column: 3,
                                            line: 3,
                                        },
                                    },
                                    restart: null,
                                    children: [
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
                                            line: 4,
                                        },
                                        end: {
                                            offset: 11,
                                            column: 3,
                                            line: 4,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '2. foo;\n3. bar;\n4. baz.\n',
                title: 'Нумерованный список с инкрементом',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 24,
                            column: 1,
                            line: 4,
                        },
                    },
                    children: [
                        {
                            type: 'list',
                            start: 2,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 23,
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
                                                    value: 'foo;',
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
                                            offset: 8,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 15,
                                            column: 8,
                                            line: 2,
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
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 15,
                                                    column: 8,
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
                                                            offset: 15,
                                                            column: 8,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'bar;',
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
                                            offset: 16,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 23,
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
                                                    offset: 19,
                                                    column: 4,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 23,
                                                    column: 8,
                                                    line: 3,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 19,
                                                            column: 4,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 23,
                                                            column: 8,
                                                            line: 3,
                                                        },
                                                    },
                                                    value: 'baz.',
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
                markup: '2. foo;\n2. bar;\n2. baz.\n',
                title: 'Нумерованный список без инкремента',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 24,
                            column: 1,
                            line: 4,
                        },
                    },
                    children: [
                        {
                            type: 'list',
                            start: 2,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 23,
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
                                                    value: 'foo;',
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
                                            offset: 8,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 15,
                                            column: 8,
                                            line: 2,
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
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 15,
                                                    column: 8,
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
                                                            offset: 15,
                                                            column: 8,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'bar;',
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
                                            offset: 16,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 23,
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
                                                    offset: 19,
                                                    column: 4,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 23,
                                                    column: 8,
                                                    line: 3,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 19,
                                                            column: 4,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 23,
                                                            column: 8,
                                                            line: 3,
                                                        },
                                                    },
                                                    value: 'baz.',
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
                markup: '1. нумерованный список\n1. нумерованный список-2\n3. нумерованный список-3\n999. нумерованный список-4\n',
                title: 'Нумерованный список 123',
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
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 99,
                                    column: 27,
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
                                                            offset: 3,
                                                            column: 4,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 22,
                                                            column: 23,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'нумерованный список',
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
                                            offset: 23,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 47,
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
                                                    offset: 26,
                                                    column: 4,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 47,
                                                    column: 25,
                                                    line: 2,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 26,
                                                            column: 4,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 47,
                                                            column: 25,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'нумерованный список-2',
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
                                            offset: 48,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 72,
                                            column: 25,
                                            line: 3,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 51,
                                                    column: 4,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 72,
                                                    column: 25,
                                                    line: 3,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 51,
                                                            column: 4,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 72,
                                                            column: 25,
                                                            line: 3,
                                                        },
                                                    },
                                                    value: 'нумерованный список-3',
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
                                            offset: 73,
                                            column: 1,
                                            line: 4,
                                        },
                                        end: {
                                            offset: 99,
                                            column: 27,
                                            line: 4,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 78,
                                                    column: 6,
                                                    line: 4,
                                                },
                                                end: {
                                                    offset: 99,
                                                    column: 27,
                                                    line: 4,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 78,
                                                            column: 6,
                                                            line: 4,
                                                        },
                                                        end: {
                                                            offset: 99,
                                                            column: 27,
                                                            line: 4,
                                                        },
                                                    },
                                                    value: 'нумерованный список-4',
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
                markup: '5. нумерованный список\n1. нумерованный список-2\n1. нумерованный список-3\n1. нумерованный список-4\n',
                title: 'Нумерованный список 123 начиная c 5',
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
                            line: 5,
                        },
                    },
                    children: [
                        {
                            type: 'list',
                            start: 5,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 97,
                                    column: 25,
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
                                                            offset: 3,
                                                            column: 4,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 22,
                                                            column: 23,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'нумерованный список',
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
                                            offset: 23,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 47,
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
                                                    offset: 26,
                                                    column: 4,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 47,
                                                    column: 25,
                                                    line: 2,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 26,
                                                            column: 4,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 47,
                                                            column: 25,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'нумерованный список-2',
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
                                            offset: 48,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 72,
                                            column: 25,
                                            line: 3,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 51,
                                                    column: 4,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 72,
                                                    column: 25,
                                                    line: 3,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 51,
                                                            column: 4,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 72,
                                                            column: 25,
                                                            line: 3,
                                                        },
                                                    },
                                                    value: 'нумерованный список-3',
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
                                            offset: 73,
                                            column: 1,
                                            line: 4,
                                        },
                                        end: {
                                            offset: 97,
                                            column: 25,
                                            line: 4,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 76,
                                                    column: 4,
                                                    line: 4,
                                                },
                                                end: {
                                                    offset: 97,
                                                    column: 25,
                                                    line: 4,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 76,
                                                            column: 4,
                                                            line: 4,
                                                        },
                                                        end: {
                                                            offset: 97,
                                                            column: 25,
                                                            line: 4,
                                                        },
                                                    },
                                                    value: 'нумерованный список-4',
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
                markup: 'A. Верхний регистр\nA. Верхний регистр-2\n',
                title: 'Нумерованный список ABC',
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
                                    offset: 39,
                                    column: 21,
                                    line: 2,
                                },
                            },
                            styleType: 'upper-alpha',
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
                                            offset: 18,
                                            column: 19,
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
                                                            offset: 18,
                                                            column: 19,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'Верхний регистр',
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
                                            offset: 19,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 39,
                                            column: 21,
                                            line: 2,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 22,
                                                    column: 4,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 39,
                                                    column: 21,
                                                    line: 2,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 22,
                                                            column: 4,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 39,
                                                            column: 21,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'Верхний регистр-2',
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
                markup: 'D. Нижний регистр\nD. Нижний регистр-2\n',
                title: 'Нумерованный список ABC начиная c D',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 38,
                            column: 1,
                            line: 3,
                        },
                    },
                    children: [
                        {
                            type: 'list',
                            start: 4,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 37,
                                    column: 20,
                                    line: 2,
                                },
                            },
                            styleType: 'upper-alpha',
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
                                            offset: 17,
                                            column: 18,
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
                                                    offset: 17,
                                                    column: 18,
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
                                                    value: 'Нижний регистр',
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
                                            offset: 18,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 37,
                                            column: 20,
                                            line: 2,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 21,
                                                    column: 4,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 37,
                                                    column: 20,
                                                    line: 2,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 21,
                                                            column: 4,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 37,
                                                            column: 20,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'Нижний регистр-2',
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
                markup: 'a. Нижний регистр\na. Нижний регистр-2\n',
                title: 'Нумерованный список abc',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 38,
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
                                    offset: 37,
                                    column: 20,
                                    line: 2,
                                },
                            },
                            styleType: 'lower-alpha',
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
                                            offset: 17,
                                            column: 18,
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
                                                    offset: 17,
                                                    column: 18,
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
                                                    value: 'Нижний регистр',
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
                                            offset: 18,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 37,
                                            column: 20,
                                            line: 2,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 21,
                                                    column: 4,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 37,
                                                    column: 20,
                                                    line: 2,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 21,
                                                            column: 4,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 37,
                                                            column: 20,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'Нижний регистр-2',
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
                markup: 'w. Нижний регистр\na. Нижний регистр-2\n',
                title: 'Нумерованный список abc начиная c w',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 38,
                            column: 1,
                            line: 3,
                        },
                    },
                    children: [
                        {
                            type: 'list',
                            start: 23,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 37,
                                    column: 20,
                                    line: 2,
                                },
                            },
                            styleType: 'lower-alpha',
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
                                            offset: 17,
                                            column: 18,
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
                                                    offset: 17,
                                                    column: 18,
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
                                                    value: 'Нижний регистр',
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
                                            offset: 18,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 37,
                                            column: 20,
                                            line: 2,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 21,
                                                    column: 4,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 37,
                                                    column: 20,
                                                    line: 2,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 21,
                                                            column: 4,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 37,
                                                            column: 20,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'Нижний регистр-2',
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
                markup: 'I. Римские цифры\nI. Римские цифры-2\n',
                title: 'Нумерованный список IVX',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 36,
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
                                    offset: 35,
                                    column: 19,
                                    line: 2,
                                },
                            },
                            styleType: 'upper-roman',
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
                                            offset: 16,
                                            column: 17,
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
                                                    offset: 16,
                                                    column: 17,
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
                                                            offset: 16,
                                                            column: 17,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'Римские цифры',
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
                                            offset: 17,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 35,
                                            column: 19,
                                            line: 2,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 20,
                                                    column: 4,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 35,
                                                    column: 19,
                                                    line: 2,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 20,
                                                            column: 4,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 35,
                                                            column: 19,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'Римские цифры-2',
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
                markup: 'i. Римские цифры\ni. Римские цифры-2\n',
                title: 'Нумерованный список ivx',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 36,
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
                                    offset: 35,
                                    column: 19,
                                    line: 2,
                                },
                            },
                            styleType: 'lower-roman',
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
                                            offset: 16,
                                            column: 17,
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
                                                    offset: 16,
                                                    column: 17,
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
                                                            offset: 16,
                                                            column: 17,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'Римские цифры',
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
                                            offset: 17,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 35,
                                            column: 19,
                                            line: 2,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 20,
                                                    column: 4,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 35,
                                                    column: 19,
                                                    line: 2,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 20,
                                                            column: 4,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 35,
                                                            column: 19,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'Римские цифры-2',
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
                markup: '1. список-1\n1) список-2\na. список-3\na) список-4\ni) список-5\ni. список-6\n',
                title: 'Нумерованный смешанный список',
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
                                    offset: 11,
                                    column: 12,
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
                                            offset: 11,
                                            column: 12,
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
                                                    offset: 11,
                                                    column: 12,
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
                                                            offset: 11,
                                                            column: 12,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'список-1',
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
                                    offset: 12,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 23,
                                    column: 12,
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
                                            offset: 12,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 23,
                                            column: 12,
                                            line: 2,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 15,
                                                    column: 4,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 23,
                                                    column: 12,
                                                    line: 2,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 15,
                                                            column: 4,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 23,
                                                            column: 12,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'список-2',
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
                                    offset: 24,
                                    column: 1,
                                    line: 3,
                                },
                                end: {
                                    offset: 35,
                                    column: 12,
                                    line: 3,
                                },
                            },
                            styleType: 'lower-alpha',
                            children: [
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 24,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 35,
                                            column: 12,
                                            line: 3,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 27,
                                                    column: 4,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 35,
                                                    column: 12,
                                                    line: 3,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 27,
                                                            column: 4,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 35,
                                                            column: 12,
                                                            line: 3,
                                                        },
                                                    },
                                                    value: 'список-3',
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
                                    offset: 36,
                                    column: 1,
                                    line: 4,
                                },
                                end: {
                                    offset: 59,
                                    column: 12,
                                    line: 5,
                                },
                            },
                            styleType: 'lower-alpha',
                            children: [
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 36,
                                            column: 1,
                                            line: 4,
                                        },
                                        end: {
                                            offset: 47,
                                            column: 12,
                                            line: 4,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 39,
                                                    column: 4,
                                                    line: 4,
                                                },
                                                end: {
                                                    offset: 47,
                                                    column: 12,
                                                    line: 4,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 39,
                                                            column: 4,
                                                            line: 4,
                                                        },
                                                        end: {
                                                            offset: 47,
                                                            column: 12,
                                                            line: 4,
                                                        },
                                                    },
                                                    value: 'список-4',
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
                                            offset: 48,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 59,
                                            column: 12,
                                            line: 5,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 51,
                                                    column: 4,
                                                    line: 5,
                                                },
                                                end: {
                                                    offset: 59,
                                                    column: 12,
                                                    line: 5,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 51,
                                                            column: 4,
                                                            line: 5,
                                                        },
                                                        end: {
                                                            offset: 59,
                                                            column: 12,
                                                            line: 5,
                                                        },
                                                    },
                                                    value: 'список-5',
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
                                    offset: 60,
                                    column: 1,
                                    line: 6,
                                },
                                end: {
                                    offset: 71,
                                    column: 12,
                                    line: 6,
                                },
                            },
                            styleType: 'lower-roman',
                            children: [
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 60,
                                            column: 1,
                                            line: 6,
                                        },
                                        end: {
                                            offset: 71,
                                            column: 12,
                                            line: 6,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 63,
                                                    column: 4,
                                                    line: 6,
                                                },
                                                end: {
                                                    offset: 71,
                                                    column: 12,
                                                    line: 6,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 63,
                                                            column: 4,
                                                            line: 6,
                                                        },
                                                        end: {
                                                            offset: 71,
                                                            column: 12,
                                                            line: 6,
                                                        },
                                                    },
                                                    value: 'список-6',
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
                markup: '1. список\n---\n',
                title: 'Cписок прерывается разделителем',
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
                                    offset: 9,
                                    column: 10,
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
                                            offset: 9,
                                            column: 10,
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
                                                    offset: 9,
                                                    column: 10,
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
                                                            offset: 9,
                                                            column: 10,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'список',
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
                                    offset: 10,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 13,
                                    column: 4,
                                    line: 2,
                                },
                            },
                        },
                    ],
                },
            },
            {
                markup: '1. foo\n```js\ncode();\n```\n',
                title: 'Cписок прерывается кодом',
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
                                                    offset: 3,
                                                    column: 4,
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
                                                            offset: 3,
                                                            column: 4,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 6,
                                                            column: 7,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'foo',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'code',
                            lang: 'js',
                            meta: null,
                            position: {
                                start: {
                                    offset: 7,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 24,
                                    column: 4,
                                    line: 4,
                                },
                            },
                            value: 'code();',
                        },
                    ],
                },
            },
            {
                markup: '1. список\n## Заголовок\n',
                title: 'Cписок прерывается MD заголовком (feat heading)',
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
                                    offset: 9,
                                    column: 10,
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
                                            offset: 9,
                                            column: 10,
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
                                                    offset: 9,
                                                    column: 10,
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
                                                            offset: 9,
                                                            column: 10,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'список',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'heading',
                            depth: 2,
                            position: {
                                start: {
                                    offset: 10,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 22,
                                    column: 13,
                                    line: 2,
                                },
                            },
                            section_local: 1,
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 13,
                                            column: 4,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 22,
                                            column: 13,
                                            line: 2,
                                        },
                                    },
                                    value: 'Заголовок',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '1. список\n==Заголовок\n',
                title: 'Cписок прерывается WOM заголовком (feat heading)',
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
                                    offset: 9,
                                    column: 10,
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
                                            offset: 9,
                                            column: 10,
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
                                                    offset: 9,
                                                    column: 10,
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
                                                            offset: 9,
                                                            column: 10,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'список',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'womHeading',
                            anchor: null,
                            depth: 1,
                            expandable: false,
                            position: {
                                start: {
                                    offset: 10,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 21,
                                    column: 12,
                                    line: 2,
                                },
                            },
                            section_local: 1,
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 12,
                                            column: 3,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 21,
                                            column: 12,
                                            line: 2,
                                        },
                                    },
                                    value: 'Заголовок',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '1. список %%code%%\n1. text %%code code code\ncode code code code code\ncode code code code code%% text\n1. список\n%%code%%\n',
                title: 'Cписок с инлайн форматтером (feat formatter)',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 120,
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
                                    offset: 119,
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
                                            offset: 18,
                                            column: 19,
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
                                                            offset: 10,
                                                            column: 11,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'список ',
                                                },
                                                {
                                                    type: 'womFormatter',
                                                    inline: true,
                                                    position: {
                                                        start: {
                                                            offset: 10,
                                                            column: 11,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 18,
                                                            column: 19,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'code',
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
                                            offset: 19,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 100,
                                            column: 32,
                                            line: 4,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 22,
                                                    column: 4,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 100,
                                                    column: 32,
                                                    line: 4,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 22,
                                                            column: 4,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 27,
                                                            column: 9,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'text ',
                                                },
                                                {
                                                    type: 'womFormatter',
                                                    inline: true,
                                                    position: {
                                                        start: {
                                                            offset: 27,
                                                            column: 9,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 95,
                                                            column: 27,
                                                            line: 4,
                                                        },
                                                    },
                                                    value: 'code code code\ncode code code code code\ncode code code code code',
                                                },
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 95,
                                                            column: 27,
                                                            line: 4,
                                                        },
                                                        end: {
                                                            offset: 100,
                                                            column: 32,
                                                            line: 4,
                                                        },
                                                    },
                                                    value: ' text',
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
                                            offset: 101,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 119,
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
                                                    offset: 104,
                                                    column: 4,
                                                    line: 5,
                                                },
                                                end: {
                                                    offset: 119,
                                                    column: 9,
                                                    line: 6,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 104,
                                                            column: 4,
                                                            line: 5,
                                                        },
                                                        end: {
                                                            offset: 111,
                                                            column: 1,
                                                            line: 6,
                                                        },
                                                    },
                                                    value: 'список\n',
                                                },
                                                {
                                                    type: 'womFormatter',
                                                    inline: true,
                                                    position: {
                                                        start: {
                                                            offset: 111,
                                                            column: 1,
                                                            line: 6,
                                                        },
                                                        end: {
                                                            offset: 119,
                                                            column: 9,
                                                            line: 6,
                                                        },
                                                    },
                                                    value: 'code',
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
                markup: '1. список\n%%(markdown)\n   - еще список\n   - еще список\n%%\n',
                title: 'Cписок c markdown форматтером (interrupt list)',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 58,
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
                                    offset: 9,
                                    column: 10,
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
                                            offset: 9,
                                            column: 10,
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
                                                    offset: 9,
                                                    column: 10,
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
                                                            offset: 9,
                                                            column: 10,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'список',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'womMarkdown',
                            attributes: {
                            },
                            format: 'markdown',
                            inline: false,
                            position: {
                                start: {
                                    offset: 10,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 57,
                                    column: 3,
                                    line: 5,
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
                                            offset: 23,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 54,
                                            column: 16,
                                            line: 4,
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
                                                    offset: 23,
                                                    column: 1,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 38,
                                                    column: 16,
                                                    line: 3,
                                                },
                                            },
                                            restart: null,
                                            children: [
                                                {
                                                    type: 'paragraph',
                                                    position: {
                                                        start: {
                                                            offset: 28,
                                                            column: 6,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 38,
                                                            column: 16,
                                                            line: 3,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'text',
                                                            position: {
                                                                start: {
                                                                    offset: 28,
                                                                    column: 6,
                                                                    line: 3,
                                                                },
                                                                end: {
                                                                    offset: 38,
                                                                    column: 16,
                                                                    line: 3,
                                                                },
                                                            },
                                                            value: 'еще список',
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
                                                    offset: 54,
                                                    column: 16,
                                                    line: 4,
                                                },
                                            },
                                            restart: null,
                                            children: [
                                                {
                                                    type: 'paragraph',
                                                    position: {
                                                        start: {
                                                            offset: 44,
                                                            column: 6,
                                                            line: 4,
                                                        },
                                                        end: {
                                                            offset: 54,
                                                            column: 16,
                                                            line: 4,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'text',
                                                            position: {
                                                                start: {
                                                                    offset: 44,
                                                                    column: 6,
                                                                    line: 4,
                                                                },
                                                                end: {
                                                                    offset: 54,
                                                                    column: 16,
                                                                    line: 4,
                                                                },
                                                            },
                                                            value: 'еще список',
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
                markup: '1. список\n   %%(markdown)\n   - еще список\n   - еще список\n   %%\n',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 64,
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
                                    offset: 63,
                                    column: 6,
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
                                            offset: 63,
                                            column: 6,
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
                                                    offset: 13,
                                                    column: 4,
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
                                                            offset: 9,
                                                            column: 10,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'список',
                                                },
                                            ],
                                        },
                                        {
                                            type: 'womMarkdown',
                                            attributes: {
                                            },
                                            format: 'markdown',
                                            inline: false,
                                            position: {
                                                start: {
                                                    offset: 13,
                                                    column: 4,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 63,
                                                    column: 6,
                                                    line: 5,
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
                                                            offset: 29,
                                                            column: 4,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 57,
                                                            column: 16,
                                                            line: 4,
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
                                                                    offset: 29,
                                                                    column: 4,
                                                                    line: 3,
                                                                },
                                                                end: {
                                                                    offset: 41,
                                                                    column: 16,
                                                                    line: 3,
                                                                },
                                                            },
                                                            restart: null,
                                                            children: [
                                                                {
                                                                    type: 'paragraph',
                                                                    position: {
                                                                        start: {
                                                                            offset: 31,
                                                                            column: 6,
                                                                            line: 3,
                                                                        },
                                                                        end: {
                                                                            offset: 41,
                                                                            column: 16,
                                                                            line: 3,
                                                                        },
                                                                    },
                                                                    children: [
                                                                        {
                                                                            type: 'text',
                                                                            position: {
                                                                                start: {
                                                                                    offset: 31,
                                                                                    column: 6,
                                                                                    line: 3,
                                                                                },
                                                                                end: {
                                                                                    offset: 41,
                                                                                    column: 16,
                                                                                    line: 3,
                                                                                },
                                                                            },
                                                                            value: 'еще список',
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
                                                                    offset: 45,
                                                                    column: 4,
                                                                    line: 4,
                                                                },
                                                                end: {
                                                                    offset: 57,
                                                                    column: 16,
                                                                    line: 4,
                                                                },
                                                            },
                                                            restart: null,
                                                            children: [
                                                                {
                                                                    type: 'paragraph',
                                                                    position: {
                                                                        start: {
                                                                            offset: 47,
                                                                            column: 6,
                                                                            line: 4,
                                                                        },
                                                                        end: {
                                                                            offset: 57,
                                                                            column: 16,
                                                                            line: 4,
                                                                        },
                                                                    },
                                                                    children: [
                                                                        {
                                                                            type: 'text',
                                                                            position: {
                                                                                start: {
                                                                                    offset: 47,
                                                                                    column: 6,
                                                                                    line: 4,
                                                                                },
                                                                                end: {
                                                                                    offset: 57,
                                                                                    column: 16,
                                                                                    line: 4,
                                                                                },
                                                                            },
                                                                            value: 'еще список',
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
                    ],
                },
            },
            {
                markup: '1. %%(markdown)\n   - еще список\n   - еще список\n%%\n',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 51,
                            column: 1,
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
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 50,
                                    column: 3,
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
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 50,
                                            column: 3,
                                            line: 4,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'womMarkdown',
                                            attributes: {
                                            },
                                            format: 'markdown',
                                            inline: false,
                                            position: {
                                                start: {
                                                    offset: 3,
                                                    column: 4,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 50,
                                                    column: 3,
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
                                                            offset: 16,
                                                            column: 1,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 47,
                                                            column: 16,
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
                                                                    offset: 16,
                                                                    column: 1,
                                                                    line: 2,
                                                                },
                                                                end: {
                                                                    offset: 31,
                                                                    column: 16,
                                                                    line: 2,
                                                                },
                                                            },
                                                            restart: null,
                                                            children: [
                                                                {
                                                                    type: 'paragraph',
                                                                    position: {
                                                                        start: {
                                                                            offset: 21,
                                                                            column: 6,
                                                                            line: 2,
                                                                        },
                                                                        end: {
                                                                            offset: 31,
                                                                            column: 16,
                                                                            line: 2,
                                                                        },
                                                                    },
                                                                    children: [
                                                                        {
                                                                            type: 'text',
                                                                            position: {
                                                                                start: {
                                                                                    offset: 21,
                                                                                    column: 6,
                                                                                    line: 2,
                                                                                },
                                                                                end: {
                                                                                    offset: 31,
                                                                                    column: 16,
                                                                                    line: 2,
                                                                                },
                                                                            },
                                                                            value: 'еще список',
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
                                                                    offset: 32,
                                                                    column: 1,
                                                                    line: 3,
                                                                },
                                                                end: {
                                                                    offset: 47,
                                                                    column: 16,
                                                                    line: 3,
                                                                },
                                                            },
                                                            restart: null,
                                                            children: [
                                                                {
                                                                    type: 'paragraph',
                                                                    position: {
                                                                        start: {
                                                                            offset: 37,
                                                                            column: 6,
                                                                            line: 3,
                                                                        },
                                                                        end: {
                                                                            offset: 47,
                                                                            column: 16,
                                                                            line: 3,
                                                                        },
                                                                    },
                                                                    children: [
                                                                        {
                                                                            type: 'text',
                                                                            position: {
                                                                                start: {
                                                                                    offset: 37,
                                                                                    column: 6,
                                                                                    line: 3,
                                                                                },
                                                                                end: {
                                                                                    offset: 47,
                                                                                    column: 16,
                                                                                    line: 3,
                                                                                },
                                                                            },
                                                                            value: 'еще список',
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
                    ],
                },
            },
            {
                markup: '1. список\n<{кат\n   - пункт списка\n   - пункт списка\n}>\n',
                title: 'Cписок c катом 1 (feat cut)',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 55,
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
                                    offset: 9,
                                    column: 10,
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
                                            offset: 9,
                                            column: 10,
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
                                                    offset: 9,
                                                    column: 10,
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
                                                            offset: 9,
                                                            column: 10,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'список',
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
                                {
                                    type: 'paragraph',
                                    position: {
                                        start: {
                                            offset: 12,
                                            column: 3,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 15,
                                            column: 6,
                                            line: 2,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 12,
                                                    column: 3,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 15,
                                                    column: 6,
                                                    line: 2,
                                                },
                                            },
                                            value: 'кат',
                                        },
                                    ],
                                },
                            ],
                            inline: false,
                            position: {
                                start: {
                                    offset: 10,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 54,
                                    column: 3,
                                    line: 5,
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
                                            offset: 16,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 51,
                                            column: 18,
                                            line: 4,
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
                                                    offset: 16,
                                                    column: 1,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 33,
                                                    column: 18,
                                                    line: 3,
                                                },
                                            },
                                            restart: null,
                                            children: [
                                                {
                                                    type: 'paragraph',
                                                    position: {
                                                        start: {
                                                            offset: 21,
                                                            column: 6,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 33,
                                                            column: 18,
                                                            line: 3,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'text',
                                                            position: {
                                                                start: {
                                                                    offset: 21,
                                                                    column: 6,
                                                                    line: 3,
                                                                },
                                                                end: {
                                                                    offset: 33,
                                                                    column: 18,
                                                                    line: 3,
                                                                },
                                                            },
                                                            value: 'пункт списка',
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
                                                    offset: 34,
                                                    column: 1,
                                                    line: 4,
                                                },
                                                end: {
                                                    offset: 51,
                                                    column: 18,
                                                    line: 4,
                                                },
                                            },
                                            restart: null,
                                            children: [
                                                {
                                                    type: 'paragraph',
                                                    position: {
                                                        start: {
                                                            offset: 39,
                                                            column: 6,
                                                            line: 4,
                                                        },
                                                        end: {
                                                            offset: 51,
                                                            column: 18,
                                                            line: 4,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'text',
                                                            position: {
                                                                start: {
                                                                    offset: 39,
                                                                    column: 6,
                                                                    line: 4,
                                                                },
                                                                end: {
                                                                    offset: 51,
                                                                    column: 18,
                                                                    line: 4,
                                                                },
                                                            },
                                                            value: 'пункт списка',
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
                markup: '1. список\n   <{кат\n   - пункт списка\n   - пункт списка\n   }>\n',
                title: 'Cписок c катом 2 (feat cut)',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 61,
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
                                    offset: 60,
                                    column: 6,
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
                                            offset: 60,
                                            column: 6,
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
                                                    offset: 13,
                                                    column: 4,
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
                                                            offset: 9,
                                                            column: 10,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'список',
                                                },
                                            ],
                                        },
                                        {
                                            type: 'womCut',
                                            title: [
                                                {
                                                    type: 'paragraph',
                                                    position: {
                                                        start: {
                                                            offset: 15,
                                                            column: 6,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 18,
                                                            column: 9,
                                                            line: 2,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'text',
                                                            position: {
                                                                start: {
                                                                    offset: 15,
                                                                    column: 6,
                                                                    line: 2,
                                                                },
                                                                end: {
                                                                    offset: 18,
                                                                    column: 9,
                                                                    line: 2,
                                                                },
                                                            },
                                                            value: 'кат',
                                                        },
                                                    ],
                                                },
                                            ],
                                            inline: false,
                                            position: {
                                                start: {
                                                    offset: 13,
                                                    column: 4,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 60,
                                                    column: 6,
                                                    line: 5,
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
                                                            offset: 22,
                                                            column: 4,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 54,
                                                            column: 18,
                                                            line: 4,
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
                                                                    offset: 22,
                                                                    column: 4,
                                                                    line: 3,
                                                                },
                                                                end: {
                                                                    offset: 36,
                                                                    column: 18,
                                                                    line: 3,
                                                                },
                                                            },
                                                            restart: null,
                                                            children: [
                                                                {
                                                                    type: 'paragraph',
                                                                    position: {
                                                                        start: {
                                                                            offset: 24,
                                                                            column: 6,
                                                                            line: 3,
                                                                        },
                                                                        end: {
                                                                            offset: 36,
                                                                            column: 18,
                                                                            line: 3,
                                                                        },
                                                                    },
                                                                    children: [
                                                                        {
                                                                            type: 'text',
                                                                            position: {
                                                                                start: {
                                                                                    offset: 24,
                                                                                    column: 6,
                                                                                    line: 3,
                                                                                },
                                                                                end: {
                                                                                    offset: 36,
                                                                                    column: 18,
                                                                                    line: 3,
                                                                                },
                                                                            },
                                                                            value: 'пункт списка',
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
                                                                    offset: 40,
                                                                    column: 4,
                                                                    line: 4,
                                                                },
                                                                end: {
                                                                    offset: 54,
                                                                    column: 18,
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
                                                                            column: 6,
                                                                            line: 4,
                                                                        },
                                                                        end: {
                                                                            offset: 54,
                                                                            column: 18,
                                                                            line: 4,
                                                                        },
                                                                    },
                                                                    children: [
                                                                        {
                                                                            type: 'text',
                                                                            position: {
                                                                                start: {
                                                                                    offset: 42,
                                                                                    column: 6,
                                                                                    line: 4,
                                                                                },
                                                                                end: {
                                                                                    offset: 54,
                                                                                    column: 18,
                                                                                    line: 4,
                                                                                },
                                                                            },
                                                                            value: 'пункт списка',
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
                    ],
                },
            },
            {
                markup: '1. <{кат\n   - пункт списка\n   - пункт списка\n}>\n',
                title: 'Cписок c катом 3 (feat cut)',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 48,
                            column: 1,
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
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 47,
                                    column: 3,
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
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 47,
                                            column: 3,
                                            line: 4,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'womCut',
                                            title: [
                                                {
                                                    type: 'paragraph',
                                                    position: {
                                                        start: {
                                                            offset: 5,
                                                            column: 6,
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
                                                                    offset: 5,
                                                                    column: 6,
                                                                    line: 1,
                                                                },
                                                                end: {
                                                                    offset: 8,
                                                                    column: 9,
                                                                    line: 1,
                                                                },
                                                            },
                                                            value: 'кат',
                                                        },
                                                    ],
                                                },
                                            ],
                                            inline: false,
                                            position: {
                                                start: {
                                                    offset: 3,
                                                    column: 4,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 47,
                                                    column: 3,
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
                                                            offset: 9,
                                                            column: 1,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 44,
                                                            column: 18,
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
                                                                    offset: 9,
                                                                    column: 1,
                                                                    line: 2,
                                                                },
                                                                end: {
                                                                    offset: 26,
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
                                                                            offset: 14,
                                                                            column: 6,
                                                                            line: 2,
                                                                        },
                                                                        end: {
                                                                            offset: 26,
                                                                            column: 18,
                                                                            line: 2,
                                                                        },
                                                                    },
                                                                    children: [
                                                                        {
                                                                            type: 'text',
                                                                            position: {
                                                                                start: {
                                                                                    offset: 14,
                                                                                    column: 6,
                                                                                    line: 2,
                                                                                },
                                                                                end: {
                                                                                    offset: 26,
                                                                                    column: 18,
                                                                                    line: 2,
                                                                                },
                                                                            },
                                                                            value: 'пункт списка',
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
                                                                    offset: 27,
                                                                    column: 1,
                                                                    line: 3,
                                                                },
                                                                end: {
                                                                    offset: 44,
                                                                    column: 18,
                                                                    line: 3,
                                                                },
                                                            },
                                                            restart: null,
                                                            children: [
                                                                {
                                                                    type: 'paragraph',
                                                                    position: {
                                                                        start: {
                                                                            offset: 32,
                                                                            column: 6,
                                                                            line: 3,
                                                                        },
                                                                        end: {
                                                                            offset: 44,
                                                                            column: 18,
                                                                            line: 3,
                                                                        },
                                                                    },
                                                                    children: [
                                                                        {
                                                                            type: 'text',
                                                                            position: {
                                                                                start: {
                                                                                    offset: 32,
                                                                                    column: 6,
                                                                                    line: 3,
                                                                                },
                                                                                end: {
                                                                                    offset: 44,
                                                                                    column: 18,
                                                                                    line: 3,
                                                                                },
                                                                            },
                                                                            value: 'пункт списка',
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
                    ],
                },
            },
            {
                markup: '* Список\n<{Кат в котором есть список\n* item\n* item\n}>\n',
                title: 'Cписок c катом без отступов (feat cut)',
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
                            line: 6,
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
                                    offset: 8,
                                    column: 9,
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
                                                    offset: 2,
                                                    column: 3,
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
                                                            offset: 2,
                                                            column: 3,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 8,
                                                            column: 9,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'Список',
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
                                {
                                    type: 'paragraph',
                                    position: {
                                        start: {
                                            offset: 11,
                                            column: 3,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 36,
                                            column: 28,
                                            line: 2,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 11,
                                                    column: 3,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 36,
                                                    column: 28,
                                                    line: 2,
                                                },
                                            },
                                            value: 'Кат в котором есть список',
                                        },
                                    ],
                                },
                            ],
                            inline: false,
                            position: {
                                start: {
                                    offset: 9,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 53,
                                    column: 3,
                                    line: 5,
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
                                            offset: 37,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 50,
                                            column: 7,
                                            line: 4,
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
                                                    offset: 37,
                                                    column: 1,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 43,
                                                    column: 7,
                                                    line: 3,
                                                },
                                            },
                                            restart: null,
                                            children: [
                                                {
                                                    type: 'paragraph',
                                                    position: {
                                                        start: {
                                                            offset: 39,
                                                            column: 3,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 43,
                                                            column: 7,
                                                            line: 3,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'text',
                                                            position: {
                                                                start: {
                                                                    offset: 39,
                                                                    column: 3,
                                                                    line: 3,
                                                                },
                                                                end: {
                                                                    offset: 43,
                                                                    column: 7,
                                                                    line: 3,
                                                                },
                                                            },
                                                            value: 'item',
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
                                                    offset: 44,
                                                    column: 1,
                                                    line: 4,
                                                },
                                                end: {
                                                    offset: 50,
                                                    column: 7,
                                                    line: 4,
                                                },
                                            },
                                            restart: null,
                                            children: [
                                                {
                                                    type: 'paragraph',
                                                    position: {
                                                        start: {
                                                            offset: 46,
                                                            column: 3,
                                                            line: 4,
                                                        },
                                                        end: {
                                                            offset: 50,
                                                            column: 7,
                                                            line: 4,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'text',
                                                            position: {
                                                                start: {
                                                                    offset: 46,
                                                                    column: 3,
                                                                    line: 4,
                                                                },
                                                                end: {
                                                                    offset: 50,
                                                                    column: 7,
                                                                    line: 4,
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
                markup: '1. {[Элемент списка в котором несколько строк\nнужно оборачивать в разметку типа «Блок»,\nнапример: ##{[элемент списка]}##]}\n',
                title: 'Список со строчным блоком',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 123,
                            column: 1,
                            line: 4,
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
                                    offset: 122,
                                    column: 35,
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
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 122,
                                            column: 35,
                                            line: 3,
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
                                                    offset: 122,
                                                    column: 35,
                                                    line: 3,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'womBlock',
                                                    inline: true,
                                                    position: {
                                                        start: {
                                                            offset: 3,
                                                            column: 4,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 122,
                                                            column: 35,
                                                            line: 3,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 5,
                                                                    column: 6,
                                                                    line: 1,
                                                                },
                                                                end: {
                                                                    offset: 120,
                                                                    column: 33,
                                                                    line: 3,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'text',
                                                                    position: {
                                                                        start: {
                                                                            offset: 5,
                                                                            column: 6,
                                                                            line: 1,
                                                                        },
                                                                        end: {
                                                                            offset: 98,
                                                                            column: 11,
                                                                            line: 3,
                                                                        },
                                                                    },
                                                                    value: 'Элемент списка в котором несколько строк\nнужно оборачивать в разметку типа «Блок»,\nнапример: ',
                                                                },
                                                                {
                                                                    type: 'womMonospace',
                                                                    position: {
                                                                        start: {
                                                                            offset: 98,
                                                                            column: 11,
                                                                            line: 3,
                                                                        },
                                                                        end: {
                                                                            offset: 120,
                                                                            column: 33,
                                                                            line: 3,
                                                                        },
                                                                    },
                                                                    children: [
                                                                        {
                                                                            type: 'womBlock',
                                                                            inline: true,
                                                                            position: {
                                                                                start: {
                                                                                    offset: 100,
                                                                                    column: 13,
                                                                                    line: 3,
                                                                                },
                                                                                end: {
                                                                                    offset: 118,
                                                                                    column: 31,
                                                                                    line: 3,
                                                                                },
                                                                            },
                                                                            children: [
                                                                                {
                                                                                    type: 'paragraph',
                                                                                    position: {
                                                                                        start: {
                                                                                            offset: 102,
                                                                                            column: 15,
                                                                                            line: 3,
                                                                                        },
                                                                                        end: {
                                                                                            offset: 116,
                                                                                            column: 29,
                                                                                            line: 3,
                                                                                        },
                                                                                    },
                                                                                    children: [
                                                                                        {
                                                                                            type: 'text',
                                                                                            position: {
                                                                                                start: {
                                                                                                    offset: 102,
                                                                                                    column: 15,
                                                                                                    line: 3,
                                                                                                },
                                                                                                end: {
                                                                                                    offset: 116,
                                                                                                    column: 29,
                                                                                                    line: 3,
                                                                                                },
                                                                                            },
                                                                                            value: 'элемент списка',
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
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '1. {[\n   Элемент списка в котором несколько строк\n   нужно оборачивать в разметку типа «Блок»\n   ]}\n',
                title: 'Список с блочным блоком',
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
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 99,
                                    column: 6,
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
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 99,
                                            column: 6,
                                            line: 4,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'womBlock',
                                            inline: false,
                                            position: {
                                                start: {
                                                    offset: 3,
                                                    column: 4,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 99,
                                                    column: 6,
                                                    line: 4,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'paragraph',
                                                    position: {
                                                        start: {
                                                            offset: 9,
                                                            column: 4,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 93,
                                                            column: 44,
                                                            line: 3,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'text',
                                                            position: {
                                                                start: {
                                                                    offset: 9,
                                                                    column: 4,
                                                                    line: 2,
                                                                },
                                                                end: {
                                                                    offset: 93,
                                                                    column: 44,
                                                                    line: 3,
                                                                },
                                                            },
                                                            value: 'Элемент списка в котором несколько строк\nнужно оборачивать в разметку типа «Блок»',
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
                markup: '- item\n- item\n- item\n\n1. item\n1. item\n1. item\n\n---\n\n1) item\n1) item\n1) item\n1. item\n1. item\n1. item\n',
                title: 'List after List',
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
                            line: 17,
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
                                    offset: 20,
                                    column: 7,
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
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: false,
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
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 9,
                                                    column: 3,
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
                                                            offset: 9,
                                                            column: 3,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 13,
                                                            column: 7,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'item',
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
                                            offset: 14,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 20,
                                            column: 7,
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
                                                    offset: 20,
                                                    column: 7,
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
                                                            offset: 20,
                                                            column: 7,
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
                        {
                            type: 'list',
                            start: 1,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 22,
                                    column: 1,
                                    line: 5,
                                },
                                end: {
                                    offset: 45,
                                    column: 8,
                                    line: 7,
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
                                            offset: 22,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 29,
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
                                                    offset: 25,
                                                    column: 4,
                                                    line: 5,
                                                },
                                                end: {
                                                    offset: 29,
                                                    column: 8,
                                                    line: 5,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 25,
                                                            column: 4,
                                                            line: 5,
                                                        },
                                                        end: {
                                                            offset: 29,
                                                            column: 8,
                                                            line: 5,
                                                        },
                                                    },
                                                    value: 'item',
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
                                            offset: 30,
                                            column: 1,
                                            line: 6,
                                        },
                                        end: {
                                            offset: 37,
                                            column: 8,
                                            line: 6,
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
                                                    line: 6,
                                                },
                                                end: {
                                                    offset: 37,
                                                    column: 8,
                                                    line: 6,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 33,
                                                            column: 4,
                                                            line: 6,
                                                        },
                                                        end: {
                                                            offset: 37,
                                                            column: 8,
                                                            line: 6,
                                                        },
                                                    },
                                                    value: 'item',
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
                                            offset: 38,
                                            column: 1,
                                            line: 7,
                                        },
                                        end: {
                                            offset: 45,
                                            column: 8,
                                            line: 7,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 41,
                                                    column: 4,
                                                    line: 7,
                                                },
                                                end: {
                                                    offset: 45,
                                                    column: 8,
                                                    line: 7,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 41,
                                                            column: 4,
                                                            line: 7,
                                                        },
                                                        end: {
                                                            offset: 45,
                                                            column: 8,
                                                            line: 7,
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
                            type: 'thematicBreak',
                            position: {
                                start: {
                                    offset: 47,
                                    column: 1,
                                    line: 9,
                                },
                                end: {
                                    offset: 50,
                                    column: 4,
                                    line: 9,
                                },
                            },
                        },
                        {
                            type: 'list',
                            start: 1,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 52,
                                    column: 1,
                                    line: 11,
                                },
                                end: {
                                    offset: 75,
                                    column: 8,
                                    line: 13,
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
                                            offset: 52,
                                            column: 1,
                                            line: 11,
                                        },
                                        end: {
                                            offset: 59,
                                            column: 8,
                                            line: 11,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 55,
                                                    column: 4,
                                                    line: 11,
                                                },
                                                end: {
                                                    offset: 59,
                                                    column: 8,
                                                    line: 11,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 55,
                                                            column: 4,
                                                            line: 11,
                                                        },
                                                        end: {
                                                            offset: 59,
                                                            column: 8,
                                                            line: 11,
                                                        },
                                                    },
                                                    value: 'item',
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
                                            offset: 60,
                                            column: 1,
                                            line: 12,
                                        },
                                        end: {
                                            offset: 67,
                                            column: 8,
                                            line: 12,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 63,
                                                    column: 4,
                                                    line: 12,
                                                },
                                                end: {
                                                    offset: 67,
                                                    column: 8,
                                                    line: 12,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 63,
                                                            column: 4,
                                                            line: 12,
                                                        },
                                                        end: {
                                                            offset: 67,
                                                            column: 8,
                                                            line: 12,
                                                        },
                                                    },
                                                    value: 'item',
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
                                            offset: 68,
                                            column: 1,
                                            line: 13,
                                        },
                                        end: {
                                            offset: 75,
                                            column: 8,
                                            line: 13,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 71,
                                                    column: 4,
                                                    line: 13,
                                                },
                                                end: {
                                                    offset: 75,
                                                    column: 8,
                                                    line: 13,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 71,
                                                            column: 4,
                                                            line: 13,
                                                        },
                                                        end: {
                                                            offset: 75,
                                                            column: 8,
                                                            line: 13,
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
                                    offset: 76,
                                    column: 1,
                                    line: 14,
                                },
                                end: {
                                    offset: 99,
                                    column: 8,
                                    line: 16,
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
                                            offset: 76,
                                            column: 1,
                                            line: 14,
                                        },
                                        end: {
                                            offset: 83,
                                            column: 8,
                                            line: 14,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 79,
                                                    column: 4,
                                                    line: 14,
                                                },
                                                end: {
                                                    offset: 83,
                                                    column: 8,
                                                    line: 14,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 79,
                                                            column: 4,
                                                            line: 14,
                                                        },
                                                        end: {
                                                            offset: 83,
                                                            column: 8,
                                                            line: 14,
                                                        },
                                                    },
                                                    value: 'item',
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
                                            offset: 84,
                                            column: 1,
                                            line: 15,
                                        },
                                        end: {
                                            offset: 91,
                                            column: 8,
                                            line: 15,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 87,
                                                    column: 4,
                                                    line: 15,
                                                },
                                                end: {
                                                    offset: 91,
                                                    column: 8,
                                                    line: 15,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 87,
                                                            column: 4,
                                                            line: 15,
                                                        },
                                                        end: {
                                                            offset: 91,
                                                            column: 8,
                                                            line: 15,
                                                        },
                                                    },
                                                    value: 'item',
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
                                            offset: 92,
                                            column: 1,
                                            line: 16,
                                        },
                                        end: {
                                            offset: 99,
                                            column: 8,
                                            line: 16,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 95,
                                                    column: 4,
                                                    line: 16,
                                                },
                                                end: {
                                                    offset: 99,
                                                    column: 8,
                                                    line: 16,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 95,
                                                            column: 4,
                                                            line: 16,
                                                        },
                                                        end: {
                                                            offset: 99,
                                                            column: 8,
                                                            line: 16,
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
                markup: '* hello\n  world\n\n  how\n  are\n* you\n\n\n\nbetter behavior:\n\n* hello\n  * world\n    how\n\n    are\n    you\n\n  * today\n* hi\n',
                title: 'List loose',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 115,
                            column: 1,
                            line: 21,
                        },
                    },
                    children: [
                        {
                            type: 'list',
                            start: null,
                            loose: true,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 34,
                                    column: 6,
                                    line: 6,
                                },
                            },
                            children: [
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: true,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 28,
                                            column: 6,
                                            line: 5,
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
                                                    offset: 16,
                                                    column: 1,
                                                    line: 3,
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
                                                            offset: 15,
                                                            column: 8,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'hello\nworld',
                                                },
                                            ],
                                        },
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 19,
                                                    column: 3,
                                                    line: 4,
                                                },
                                                end: {
                                                    offset: 28,
                                                    column: 6,
                                                    line: 5,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 19,
                                                            column: 3,
                                                            line: 4,
                                                        },
                                                        end: {
                                                            offset: 28,
                                                            column: 6,
                                                            line: 5,
                                                        },
                                                    },
                                                    value: 'how\nare',
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
                                            offset: 29,
                                            column: 1,
                                            line: 6,
                                        },
                                        end: {
                                            offset: 34,
                                            column: 6,
                                            line: 6,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 31,
                                                    column: 3,
                                                    line: 6,
                                                },
                                                end: {
                                                    offset: 34,
                                                    column: 6,
                                                    line: 6,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 31,
                                                            column: 3,
                                                            line: 6,
                                                        },
                                                        end: {
                                                            offset: 34,
                                                            column: 6,
                                                            line: 6,
                                                        },
                                                    },
                                                    value: 'you',
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
                                    offset: 38,
                                    column: 1,
                                    line: 10,
                                },
                                end: {
                                    offset: 55,
                                    column: 1,
                                    line: 11,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 38,
                                            column: 1,
                                            line: 10,
                                        },
                                        end: {
                                            offset: 54,
                                            column: 17,
                                            line: 10,
                                        },
                                    },
                                    value: 'better behavior:',
                                },
                            ],
                        },
                        {
                            type: 'list',
                            start: null,
                            loose: true,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 56,
                                    column: 1,
                                    line: 12,
                                },
                                end: {
                                    offset: 114,
                                    column: 5,
                                    line: 20,
                                },
                            },
                            children: [
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: true,
                                    position: {
                                        start: {
                                            offset: 56,
                                            column: 1,
                                            line: 12,
                                        },
                                        end: {
                                            offset: 109,
                                            column: 10,
                                            line: 19,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 58,
                                                    column: 3,
                                                    line: 12,
                                                },
                                                end: {
                                                    offset: 66,
                                                    column: 3,
                                                    line: 13,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 58,
                                                            column: 3,
                                                            line: 12,
                                                        },
                                                        end: {
                                                            offset: 63,
                                                            column: 8,
                                                            line: 12,
                                                        },
                                                    },
                                                    value: 'hello',
                                                },
                                            ],
                                        },
                                        {
                                            type: 'list',
                                            start: null,
                                            loose: true,
                                            ordered: false,
                                            position: {
                                                start: {
                                                    offset: 66,
                                                    column: 3,
                                                    line: 13,
                                                },
                                                end: {
                                                    offset: 109,
                                                    column: 10,
                                                    line: 19,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'listItem',
                                                    checked: null,
                                                    expandable: false,
                                                    loose: true,
                                                    position: {
                                                        start: {
                                                            offset: 66,
                                                            column: 3,
                                                            line: 13,
                                                        },
                                                        end: {
                                                            offset: 99,
                                                            column: 1,
                                                            line: 18,
                                                        },
                                                    },
                                                    restart: null,
                                                    children: [
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 68,
                                                                    column: 5,
                                                                    line: 13,
                                                                },
                                                                end: {
                                                                    offset: 82,
                                                                    column: 1,
                                                                    line: 15,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'text',
                                                                    position: {
                                                                        start: {
                                                                            offset: 68,
                                                                            column: 5,
                                                                            line: 13,
                                                                        },
                                                                        end: {
                                                                            offset: 81,
                                                                            column: 8,
                                                                            line: 14,
                                                                        },
                                                                    },
                                                                    value: 'world\nhow',
                                                                },
                                                            ],
                                                        },
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 87,
                                                                    column: 5,
                                                                    line: 16,
                                                                },
                                                                end: {
                                                                    offset: 99,
                                                                    column: 1,
                                                                    line: 18,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'text',
                                                                    position: {
                                                                        start: {
                                                                            offset: 87,
                                                                            column: 5,
                                                                            line: 16,
                                                                        },
                                                                        end: {
                                                                            offset: 98,
                                                                            column: 8,
                                                                            line: 17,
                                                                        },
                                                                    },
                                                                    value: 'are\nyou',
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
                                                            offset: 102,
                                                            column: 3,
                                                            line: 19,
                                                        },
                                                        end: {
                                                            offset: 109,
                                                            column: 10,
                                                            line: 19,
                                                        },
                                                    },
                                                    restart: null,
                                                    children: [
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 104,
                                                                    column: 5,
                                                                    line: 19,
                                                                },
                                                                end: {
                                                                    offset: 109,
                                                                    column: 10,
                                                                    line: 19,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'text',
                                                                    position: {
                                                                        start: {
                                                                            offset: 104,
                                                                            column: 5,
                                                                            line: 19,
                                                                        },
                                                                        end: {
                                                                            offset: 109,
                                                                            column: 10,
                                                                            line: 19,
                                                                        },
                                                                    },
                                                                    value: 'today',
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
                                            offset: 110,
                                            column: 1,
                                            line: 20,
                                        },
                                        end: {
                                            offset: 114,
                                            column: 5,
                                            line: 20,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 112,
                                                    column: 3,
                                                    line: 20,
                                                },
                                                end: {
                                                    offset: 114,
                                                    column: 5,
                                                    line: 20,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 112,
                                                            column: 3,
                                                            line: 20,
                                                        },
                                                        end: {
                                                            offset: 114,
                                                            column: 5,
                                                            line: 20,
                                                        },
                                                    },
                                                    value: 'hi',
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
                markup: '*   This is a list item\n\n\n    This is paragraph\n',
                title: 'List and Code',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 48,
                            column: 1,
                            line: 5,
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
                                    offset: 23,
                                    column: 24,
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
                                            offset: 23,
                                            column: 24,
                                            line: 1,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 4,
                                                    column: 5,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 23,
                                                    column: 24,
                                                    line: 1,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 4,
                                                            column: 5,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 23,
                                                            column: 24,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'This is a list item',
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
                                    offset: 26,
                                    column: 1,
                                    line: 4,
                                },
                                end: {
                                    offset: 48,
                                    column: 1,
                                    line: 5,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 30,
                                            column: 5,
                                            line: 4,
                                        },
                                        end: {
                                            offset: 47,
                                            column: 22,
                                            line: 4,
                                        },
                                    },
                                    value: 'This is paragraph',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '- Hello 1a\n\n World 1a.\n\n- Hello 1b\n\n  World 1b.\n',
                title: 'List indentation 1',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 48,
                            column: 1,
                            line: 8,
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
                                    offset: 10,
                                    column: 11,
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
                                            offset: 10,
                                            column: 11,
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
                                                    offset: 10,
                                                    column: 11,
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
                                                            offset: 10,
                                                            column: 11,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'Hello 1a',
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
                                    offset: 12,
                                    column: 1,
                                    line: 3,
                                },
                                end: {
                                    offset: 23,
                                    column: 1,
                                    line: 4,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 13,
                                            column: 2,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 22,
                                            column: 11,
                                            line: 3,
                                        },
                                    },
                                    value: 'World 1a.',
                                },
                            ],
                        },
                        {
                            type: 'list',
                            start: null,
                            loose: true,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 24,
                                    column: 1,
                                    line: 5,
                                },
                                end: {
                                    offset: 47,
                                    column: 12,
                                    line: 7,
                                },
                            },
                            children: [
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: true,
                                    position: {
                                        start: {
                                            offset: 24,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 47,
                                            column: 12,
                                            line: 7,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 26,
                                                    column: 3,
                                                    line: 5,
                                                },
                                                end: {
                                                    offset: 35,
                                                    column: 1,
                                                    line: 6,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 26,
                                                            column: 3,
                                                            line: 5,
                                                        },
                                                        end: {
                                                            offset: 34,
                                                            column: 11,
                                                            line: 5,
                                                        },
                                                    },
                                                    value: 'Hello 1b',
                                                },
                                            ],
                                        },
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 38,
                                                    column: 3,
                                                    line: 7,
                                                },
                                                end: {
                                                    offset: 47,
                                                    column: 12,
                                                    line: 7,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 38,
                                                            column: 3,
                                                            line: 7,
                                                        },
                                                        end: {
                                                            offset: 47,
                                                            column: 12,
                                                            line: 7,
                                                        },
                                                    },
                                                    value: 'World 1b.',
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
                markup: '-  Hello 2a\n\n  World 2a.\n\n-  Hello 2b\n\n   World 2b.\n',
                title: 'List indentation 2',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 52,
                            column: 1,
                            line: 8,
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
                                    offset: 11,
                                    column: 12,
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
                                            offset: 11,
                                            column: 12,
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
                                                    offset: 11,
                                                    column: 12,
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
                                                            offset: 11,
                                                            column: 12,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'Hello 2a',
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
                                    offset: 13,
                                    column: 1,
                                    line: 3,
                                },
                                end: {
                                    offset: 25,
                                    column: 1,
                                    line: 4,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 15,
                                            column: 3,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 24,
                                            column: 12,
                                            line: 3,
                                        },
                                    },
                                    value: 'World 2a.',
                                },
                            ],
                        },
                        {
                            type: 'list',
                            start: null,
                            loose: true,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 26,
                                    column: 1,
                                    line: 5,
                                },
                                end: {
                                    offset: 51,
                                    column: 13,
                                    line: 7,
                                },
                            },
                            children: [
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: true,
                                    position: {
                                        start: {
                                            offset: 26,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 51,
                                            column: 13,
                                            line: 7,
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
                                                    offset: 38,
                                                    column: 1,
                                                    line: 6,
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
                                                            offset: 37,
                                                            column: 12,
                                                            line: 5,
                                                        },
                                                    },
                                                    value: 'Hello 2b',
                                                },
                                            ],
                                        },
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 42,
                                                    column: 4,
                                                    line: 7,
                                                },
                                                end: {
                                                    offset: 51,
                                                    column: 13,
                                                    line: 7,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 42,
                                                            column: 4,
                                                            line: 7,
                                                        },
                                                        end: {
                                                            offset: 51,
                                                            column: 13,
                                                            line: 7,
                                                        },
                                                    },
                                                    value: 'World 2b.',
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
                markup: '-   Hello 3a\n\n   World 3a.\n\n-   Hello 3b\n\n    World 3b.\n',
                title: 'List indentation 3',
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
                            line: 8,
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
                                    offset: 12,
                                    column: 13,
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
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 4,
                                                    column: 5,
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
                                                            offset: 4,
                                                            column: 5,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 12,
                                                            column: 13,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'Hello 3a',
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
                                    offset: 14,
                                    column: 1,
                                    line: 3,
                                },
                                end: {
                                    offset: 27,
                                    column: 1,
                                    line: 4,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 17,
                                            column: 4,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 26,
                                            column: 13,
                                            line: 3,
                                        },
                                    },
                                    value: 'World 3a.',
                                },
                            ],
                        },
                        {
                            type: 'list',
                            start: null,
                            loose: true,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 28,
                                    column: 1,
                                    line: 5,
                                },
                                end: {
                                    offset: 55,
                                    column: 14,
                                    line: 7,
                                },
                            },
                            children: [
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: true,
                                    position: {
                                        start: {
                                            offset: 28,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 55,
                                            column: 14,
                                            line: 7,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 32,
                                                    column: 5,
                                                    line: 5,
                                                },
                                                end: {
                                                    offset: 41,
                                                    column: 1,
                                                    line: 6,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 32,
                                                            column: 5,
                                                            line: 5,
                                                        },
                                                        end: {
                                                            offset: 40,
                                                            column: 13,
                                                            line: 5,
                                                        },
                                                    },
                                                    value: 'Hello 3b',
                                                },
                                            ],
                                        },
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 46,
                                                    column: 5,
                                                    line: 7,
                                                },
                                                end: {
                                                    offset: 55,
                                                    column: 14,
                                                    line: 7,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 46,
                                                            column: 5,
                                                            line: 7,
                                                        },
                                                        end: {
                                                            offset: 55,
                                                            column: 14,
                                                            line: 7,
                                                        },
                                                    },
                                                    value: 'World 3b.',
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
                markup: '-    Hello 4a\n\n    World 4a.\n\n-    Hello 4b\n\n     World 4b.\n',
                title: 'List indentation 4',
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
                            line: 8,
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
                                    column: 14,
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
                                            offset: 13,
                                            column: 14,
                                            line: 1,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 5,
                                                    column: 6,
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
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 5,
                                                            column: 6,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 13,
                                                            column: 14,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'Hello 4a',
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
                                    offset: 15,
                                    column: 1,
                                    line: 3,
                                },
                                end: {
                                    offset: 29,
                                    column: 1,
                                    line: 4,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 19,
                                            column: 5,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 28,
                                            column: 14,
                                            line: 3,
                                        },
                                    },
                                    value: 'World 4a.',
                                },
                            ],
                        },
                        {
                            type: 'list',
                            start: null,
                            loose: true,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 30,
                                    column: 1,
                                    line: 5,
                                },
                                end: {
                                    offset: 59,
                                    column: 15,
                                    line: 7,
                                },
                            },
                            children: [
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: true,
                                    position: {
                                        start: {
                                            offset: 30,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 59,
                                            column: 15,
                                            line: 7,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 35,
                                                    column: 6,
                                                    line: 5,
                                                },
                                                end: {
                                                    offset: 44,
                                                    column: 1,
                                                    line: 6,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 35,
                                                            column: 6,
                                                            line: 5,
                                                        },
                                                        end: {
                                                            offset: 43,
                                                            column: 14,
                                                            line: 5,
                                                        },
                                                    },
                                                    value: 'Hello 4b',
                                                },
                                            ],
                                        },
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 50,
                                                    column: 6,
                                                    line: 7,
                                                },
                                                end: {
                                                    offset: 59,
                                                    column: 15,
                                                    line: 7,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 50,
                                                            column: 6,
                                                            line: 7,
                                                        },
                                                        end: {
                                                            offset: 59,
                                                            column: 15,
                                                            line: 7,
                                                        },
                                                    },
                                                    value: 'World 4b.',
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
                markup: '-     Hello 5a\n\n     World 5a.\n\n-     Hello 5b\n\n      World 5b.\n',
                title: 'List indentation 5',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 64,
                            column: 1,
                            line: 8,
                        },
                    },
                    children: [
                        {
                            type: 'list',
                            start: null,
                            loose: true,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 63,
                                    column: 16,
                                    line: 7,
                                },
                            },
                            children: [
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: true,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 31,
                                            column: 1,
                                            line: 4,
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
                                                    offset: 15,
                                                    column: 1,
                                                    line: 2,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 6,
                                                            column: 7,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 14,
                                                            column: 15,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'Hello 5a',
                                                },
                                            ],
                                        },
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 18,
                                                    column: 3,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 31,
                                                    column: 1,
                                                    line: 4,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 21,
                                                            column: 6,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 30,
                                                            column: 15,
                                                            line: 3,
                                                        },
                                                    },
                                                    value: 'World 5a.',
                                                },
                                            ],
                                        },
                                    ],
                                },
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: true,
                                    position: {
                                        start: {
                                            offset: 32,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 63,
                                            column: 16,
                                            line: 7,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 34,
                                                    column: 3,
                                                    line: 5,
                                                },
                                                end: {
                                                    offset: 47,
                                                    column: 1,
                                                    line: 6,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 38,
                                                            column: 7,
                                                            line: 5,
                                                        },
                                                        end: {
                                                            offset: 46,
                                                            column: 15,
                                                            line: 5,
                                                        },
                                                    },
                                                    value: 'Hello 5b',
                                                },
                                            ],
                                        },
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 50,
                                                    column: 3,
                                                    line: 7,
                                                },
                                                end: {
                                                    offset: 63,
                                                    column: 16,
                                                    line: 7,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 54,
                                                            column: 7,
                                                            line: 7,
                                                        },
                                                        end: {
                                                            offset: 63,
                                                            column: 16,
                                                            line: 7,
                                                        },
                                                    },
                                                    value: 'World 5b.',
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
                markup: '*one\n\n1.two\n',
                title: 'ListItem with no space',
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
                                    value: '*one',
                                },
                            ],
                        },
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 6,
                                    column: 1,
                                    line: 3,
                                },
                                end: {
                                    offset: 12,
                                    column: 1,
                                    line: 4,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 6,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 11,
                                            column: 6,
                                            line: 3,
                                        },
                                    },
                                    value: '1.two',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '*\n\n1.\n',
                title: 'ListItem empty with no space',
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
                                    offset: 2,
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
                                    value: '*',
                                },
                            ],
                        },
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 3,
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
                                            offset: 3,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 5,
                                            column: 3,
                                            line: 3,
                                        },
                                    },
                                    value: '1.',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '-   Foo\n-\n    Bar\n',
                title: 'ListItem starting with a blank line',
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
                                    offset: 17,
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
                                                    offset: 4,
                                                    column: 5,
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
                                                            offset: 4,
                                                            column: 5,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 7,
                                                            column: 8,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'Foo',
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
                                            offset: 8,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 17,
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
                                                    column: 2,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 17,
                                                    column: 8,
                                                    line: 3,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 14,
                                                            column: 5,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 17,
                                                            column: 8,
                                                            line: 3,
                                                        },
                                                    },
                                                    value: 'Bar',
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
                markup: '  * item1\n\n    * item2\n\n  text\n',
                title: 'ListItem text',
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
                            line: 6,
                        },
                    },
                    children: [
                        {
                            type: 'list',
                            start: null,
                            loose: true,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 22,
                                    column: 12,
                                    line: 3,
                                },
                            },
                            children: [
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: true,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 22,
                                            column: 12,
                                            line: 3,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 4,
                                                    column: 5,
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
                                                            offset: 4,
                                                            column: 5,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 9,
                                                            column: 10,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'item1',
                                                },
                                            ],
                                        },
                                        {
                                            type: 'list',
                                            start: null,
                                            loose: false,
                                            ordered: false,
                                            position: {
                                                start: {
                                                    offset: 15,
                                                    column: 5,
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
                                                    type: 'listItem',
                                                    checked: null,
                                                    expandable: false,
                                                    loose: false,
                                                    position: {
                                                        start: {
                                                            offset: 15,
                                                            column: 5,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 22,
                                                            column: 12,
                                                            line: 3,
                                                        },
                                                    },
                                                    restart: null,
                                                    children: [
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 17,
                                                                    column: 7,
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
                                                                            offset: 17,
                                                                            column: 7,
                                                                            line: 3,
                                                                        },
                                                                        end: {
                                                                            offset: 22,
                                                                            column: 12,
                                                                            line: 3,
                                                                        },
                                                                    },
                                                                    value: 'item2',
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
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 24,
                                    column: 1,
                                    line: 5,
                                },
                                end: {
                                    offset: 31,
                                    column: 1,
                                    line: 6,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 26,
                                            column: 3,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 30,
                                            column: 7,
                                            line: 5,
                                        },
                                    },
                                    value: 'text',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '1. foo bar baz.\n\n<!--  -->\n\n99. foo bar baz.\n\n<!--  -->\n\n999. foo bar baz.\n\n<!--  -->\n\n1. foo bar baz.\n   foo bar baz.\n\n<!--  -->\n\n99. foo bar baz.\n    foo bar baz.\n\n<!--  -->\n\n999. foo bar baz.\n     foo bar baz.\n\n<!--  -->\n\n- foo bar baz.\n\n<!--  -->\n\n- foo bar baz.\n  foo bar baz.\n',
                title: 'ListItem indent = 1',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 282,
                            column: 1,
                            line: 34,
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
                                    offset: 15,
                                    column: 16,
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
                                            offset: 15,
                                            column: 16,
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
                                                            offset: 15,
                                                            column: 16,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'foo bar baz.',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'html',
                            position: {
                                start: {
                                    offset: 17,
                                    column: 1,
                                    line: 3,
                                },
                                end: {
                                    offset: 26,
                                    column: 10,
                                    line: 3,
                                },
                            },
                            value: '<!--  -->',
                        },
                        {
                            type: 'list',
                            start: 99,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 28,
                                    column: 1,
                                    line: 5,
                                },
                                end: {
                                    offset: 44,
                                    column: 17,
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
                                            offset: 28,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 44,
                                            column: 17,
                                            line: 5,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 32,
                                                    column: 5,
                                                    line: 5,
                                                },
                                                end: {
                                                    offset: 44,
                                                    column: 17,
                                                    line: 5,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 32,
                                                            column: 5,
                                                            line: 5,
                                                        },
                                                        end: {
                                                            offset: 44,
                                                            column: 17,
                                                            line: 5,
                                                        },
                                                    },
                                                    value: 'foo bar baz.',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'html',
                            position: {
                                start: {
                                    offset: 46,
                                    column: 1,
                                    line: 7,
                                },
                                end: {
                                    offset: 55,
                                    column: 10,
                                    line: 7,
                                },
                            },
                            value: '<!--  -->',
                        },
                        {
                            type: 'list',
                            start: 999,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 57,
                                    column: 1,
                                    line: 9,
                                },
                                end: {
                                    offset: 74,
                                    column: 18,
                                    line: 9,
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
                                            offset: 57,
                                            column: 1,
                                            line: 9,
                                        },
                                        end: {
                                            offset: 74,
                                            column: 18,
                                            line: 9,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 62,
                                                    column: 6,
                                                    line: 9,
                                                },
                                                end: {
                                                    offset: 74,
                                                    column: 18,
                                                    line: 9,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 62,
                                                            column: 6,
                                                            line: 9,
                                                        },
                                                        end: {
                                                            offset: 74,
                                                            column: 18,
                                                            line: 9,
                                                        },
                                                    },
                                                    value: 'foo bar baz.',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'html',
                            position: {
                                start: {
                                    offset: 76,
                                    column: 1,
                                    line: 11,
                                },
                                end: {
                                    offset: 85,
                                    column: 10,
                                    line: 11,
                                },
                            },
                            value: '<!--  -->',
                        },
                        {
                            type: 'list',
                            start: 1,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 87,
                                    column: 1,
                                    line: 13,
                                },
                                end: {
                                    offset: 118,
                                    column: 16,
                                    line: 14,
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
                                            offset: 87,
                                            column: 1,
                                            line: 13,
                                        },
                                        end: {
                                            offset: 118,
                                            column: 16,
                                            line: 14,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 90,
                                                    column: 4,
                                                    line: 13,
                                                },
                                                end: {
                                                    offset: 118,
                                                    column: 16,
                                                    line: 14,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 90,
                                                            column: 4,
                                                            line: 13,
                                                        },
                                                        end: {
                                                            offset: 118,
                                                            column: 16,
                                                            line: 14,
                                                        },
                                                    },
                                                    value: 'foo bar baz.\nfoo bar baz.',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'html',
                            position: {
                                start: {
                                    offset: 120,
                                    column: 1,
                                    line: 16,
                                },
                                end: {
                                    offset: 129,
                                    column: 10,
                                    line: 16,
                                },
                            },
                            value: '<!--  -->',
                        },
                        {
                            type: 'list',
                            start: 99,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 131,
                                    column: 1,
                                    line: 18,
                                },
                                end: {
                                    offset: 164,
                                    column: 17,
                                    line: 19,
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
                                            offset: 131,
                                            column: 1,
                                            line: 18,
                                        },
                                        end: {
                                            offset: 164,
                                            column: 17,
                                            line: 19,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 135,
                                                    column: 5,
                                                    line: 18,
                                                },
                                                end: {
                                                    offset: 164,
                                                    column: 17,
                                                    line: 19,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 135,
                                                            column: 5,
                                                            line: 18,
                                                        },
                                                        end: {
                                                            offset: 164,
                                                            column: 17,
                                                            line: 19,
                                                        },
                                                    },
                                                    value: 'foo bar baz.\nfoo bar baz.',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'html',
                            position: {
                                start: {
                                    offset: 166,
                                    column: 1,
                                    line: 21,
                                },
                                end: {
                                    offset: 175,
                                    column: 10,
                                    line: 21,
                                },
                            },
                            value: '<!--  -->',
                        },
                        {
                            type: 'list',
                            start: 999,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 177,
                                    column: 1,
                                    line: 23,
                                },
                                end: {
                                    offset: 212,
                                    column: 18,
                                    line: 24,
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
                                            offset: 177,
                                            column: 1,
                                            line: 23,
                                        },
                                        end: {
                                            offset: 212,
                                            column: 18,
                                            line: 24,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 182,
                                                    column: 6,
                                                    line: 23,
                                                },
                                                end: {
                                                    offset: 212,
                                                    column: 18,
                                                    line: 24,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 182,
                                                            column: 6,
                                                            line: 23,
                                                        },
                                                        end: {
                                                            offset: 212,
                                                            column: 18,
                                                            line: 24,
                                                        },
                                                    },
                                                    value: 'foo bar baz.\nfoo bar baz.',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'html',
                            position: {
                                start: {
                                    offset: 214,
                                    column: 1,
                                    line: 26,
                                },
                                end: {
                                    offset: 223,
                                    column: 10,
                                    line: 26,
                                },
                            },
                            value: '<!--  -->',
                        },
                        {
                            type: 'list',
                            start: null,
                            loose: false,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 225,
                                    column: 1,
                                    line: 28,
                                },
                                end: {
                                    offset: 239,
                                    column: 15,
                                    line: 28,
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
                                            offset: 225,
                                            column: 1,
                                            line: 28,
                                        },
                                        end: {
                                            offset: 239,
                                            column: 15,
                                            line: 28,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 227,
                                                    column: 3,
                                                    line: 28,
                                                },
                                                end: {
                                                    offset: 239,
                                                    column: 15,
                                                    line: 28,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 227,
                                                            column: 3,
                                                            line: 28,
                                                        },
                                                        end: {
                                                            offset: 239,
                                                            column: 15,
                                                            line: 28,
                                                        },
                                                    },
                                                    value: 'foo bar baz.',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'html',
                            position: {
                                start: {
                                    offset: 241,
                                    column: 1,
                                    line: 30,
                                },
                                end: {
                                    offset: 250,
                                    column: 10,
                                    line: 30,
                                },
                            },
                            value: '<!--  -->',
                        },
                        {
                            type: 'list',
                            start: null,
                            loose: false,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 252,
                                    column: 1,
                                    line: 32,
                                },
                                end: {
                                    offset: 281,
                                    column: 15,
                                    line: 33,
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
                                            offset: 252,
                                            column: 1,
                                            line: 32,
                                        },
                                        end: {
                                            offset: 281,
                                            column: 15,
                                            line: 33,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 254,
                                                    column: 3,
                                                    line: 32,
                                                },
                                                end: {
                                                    offset: 281,
                                                    column: 15,
                                                    line: 33,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 254,
                                                            column: 3,
                                                            line: 32,
                                                        },
                                                        end: {
                                                            offset: 281,
                                                            column: 15,
                                                            line: 33,
                                                        },
                                                    },
                                                    value: 'foo bar baz.\nfoo bar baz.',
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
                markup: '1.\tfoo bar baz.\n\n<!--  -->\n\n99.\tfoo bar baz.\n\n<!--  -->\n\n999.\tfoo bar baz.\n\n<!--  -->\n\n1.\tfoo bar baz.\n\tfoo bar baz.\n\n<!--  -->\n\n99.\tfoo bar baz.\n\tfoo bar baz.\n\n<!--  -->\n\n999.\tfoo bar baz.\n\tfoo bar baz.\n\n<!--  -->\n\n-\tfoo bar baz.\n\n<!--  -->\n\n-\tfoo bar baz.\n\tfoo bar baz.\n',
                title: 'ListItem indent = tab',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 272,
                            column: 1,
                            line: 34,
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
                                    offset: 15,
                                    column: 16,
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
                                            offset: 15,
                                            column: 16,
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
                                                            offset: 15,
                                                            column: 16,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'foo bar baz.',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'html',
                            position: {
                                start: {
                                    offset: 17,
                                    column: 1,
                                    line: 3,
                                },
                                end: {
                                    offset: 26,
                                    column: 10,
                                    line: 3,
                                },
                            },
                            value: '<!--  -->',
                        },
                        {
                            type: 'list',
                            start: 99,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 28,
                                    column: 1,
                                    line: 5,
                                },
                                end: {
                                    offset: 44,
                                    column: 17,
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
                                            offset: 28,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 44,
                                            column: 17,
                                            line: 5,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 32,
                                                    column: 5,
                                                    line: 5,
                                                },
                                                end: {
                                                    offset: 44,
                                                    column: 17,
                                                    line: 5,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 32,
                                                            column: 5,
                                                            line: 5,
                                                        },
                                                        end: {
                                                            offset: 44,
                                                            column: 17,
                                                            line: 5,
                                                        },
                                                    },
                                                    value: 'foo bar baz.',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'html',
                            position: {
                                start: {
                                    offset: 46,
                                    column: 1,
                                    line: 7,
                                },
                                end: {
                                    offset: 55,
                                    column: 10,
                                    line: 7,
                                },
                            },
                            value: '<!--  -->',
                        },
                        {
                            type: 'list',
                            start: 999,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 57,
                                    column: 1,
                                    line: 9,
                                },
                                end: {
                                    offset: 74,
                                    column: 18,
                                    line: 9,
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
                                            offset: 57,
                                            column: 1,
                                            line: 9,
                                        },
                                        end: {
                                            offset: 74,
                                            column: 18,
                                            line: 9,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 62,
                                                    column: 6,
                                                    line: 9,
                                                },
                                                end: {
                                                    offset: 74,
                                                    column: 18,
                                                    line: 9,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 62,
                                                            column: 6,
                                                            line: 9,
                                                        },
                                                        end: {
                                                            offset: 74,
                                                            column: 18,
                                                            line: 9,
                                                        },
                                                    },
                                                    value: 'foo bar baz.',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'html',
                            position: {
                                start: {
                                    offset: 76,
                                    column: 1,
                                    line: 11,
                                },
                                end: {
                                    offset: 85,
                                    column: 10,
                                    line: 11,
                                },
                            },
                            value: '<!--  -->',
                        },
                        {
                            type: 'list',
                            start: 1,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 87,
                                    column: 1,
                                    line: 13,
                                },
                                end: {
                                    offset: 116,
                                    column: 14,
                                    line: 14,
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
                                            offset: 87,
                                            column: 1,
                                            line: 13,
                                        },
                                        end: {
                                            offset: 116,
                                            column: 14,
                                            line: 14,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 90,
                                                    column: 4,
                                                    line: 13,
                                                },
                                                end: {
                                                    offset: 116,
                                                    column: 14,
                                                    line: 14,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 90,
                                                            column: 4,
                                                            line: 13,
                                                        },
                                                        end: {
                                                            offset: 116,
                                                            column: 14,
                                                            line: 14,
                                                        },
                                                    },
                                                    value: 'foo bar baz.\nfoo bar baz.',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'html',
                            position: {
                                start: {
                                    offset: 118,
                                    column: 1,
                                    line: 16,
                                },
                                end: {
                                    offset: 127,
                                    column: 10,
                                    line: 16,
                                },
                            },
                            value: '<!--  -->',
                        },
                        {
                            type: 'list',
                            start: 99,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 129,
                                    column: 1,
                                    line: 18,
                                },
                                end: {
                                    offset: 159,
                                    column: 14,
                                    line: 19,
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
                                            offset: 129,
                                            column: 1,
                                            line: 18,
                                        },
                                        end: {
                                            offset: 159,
                                            column: 14,
                                            line: 19,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 133,
                                                    column: 5,
                                                    line: 18,
                                                },
                                                end: {
                                                    offset: 159,
                                                    column: 14,
                                                    line: 19,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 133,
                                                            column: 5,
                                                            line: 18,
                                                        },
                                                        end: {
                                                            offset: 159,
                                                            column: 14,
                                                            line: 19,
                                                        },
                                                    },
                                                    value: 'foo bar baz.\nfoo bar baz.',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'html',
                            position: {
                                start: {
                                    offset: 161,
                                    column: 1,
                                    line: 21,
                                },
                                end: {
                                    offset: 170,
                                    column: 10,
                                    line: 21,
                                },
                            },
                            value: '<!--  -->',
                        },
                        {
                            type: 'list',
                            start: 999,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 172,
                                    column: 1,
                                    line: 23,
                                },
                                end: {
                                    offset: 203,
                                    column: 14,
                                    line: 24,
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
                                            offset: 172,
                                            column: 1,
                                            line: 23,
                                        },
                                        end: {
                                            offset: 203,
                                            column: 14,
                                            line: 24,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 177,
                                                    column: 6,
                                                    line: 23,
                                                },
                                                end: {
                                                    offset: 203,
                                                    column: 14,
                                                    line: 24,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 177,
                                                            column: 6,
                                                            line: 23,
                                                        },
                                                        end: {
                                                            offset: 203,
                                                            column: 14,
                                                            line: 24,
                                                        },
                                                    },
                                                    value: 'foo bar baz.\nfoo bar baz.',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'html',
                            position: {
                                start: {
                                    offset: 205,
                                    column: 1,
                                    line: 26,
                                },
                                end: {
                                    offset: 214,
                                    column: 10,
                                    line: 26,
                                },
                            },
                            value: '<!--  -->',
                        },
                        {
                            type: 'list',
                            start: null,
                            loose: false,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 216,
                                    column: 1,
                                    line: 28,
                                },
                                end: {
                                    offset: 230,
                                    column: 15,
                                    line: 28,
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
                                            offset: 216,
                                            column: 1,
                                            line: 28,
                                        },
                                        end: {
                                            offset: 230,
                                            column: 15,
                                            line: 28,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 218,
                                                    column: 3,
                                                    line: 28,
                                                },
                                                end: {
                                                    offset: 230,
                                                    column: 15,
                                                    line: 28,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 218,
                                                            column: 3,
                                                            line: 28,
                                                        },
                                                        end: {
                                                            offset: 230,
                                                            column: 15,
                                                            line: 28,
                                                        },
                                                    },
                                                    value: 'foo bar baz.',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'html',
                            position: {
                                start: {
                                    offset: 232,
                                    column: 1,
                                    line: 30,
                                },
                                end: {
                                    offset: 241,
                                    column: 10,
                                    line: 30,
                                },
                            },
                            value: '<!--  -->',
                        },
                        {
                            type: 'list',
                            start: null,
                            loose: false,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 243,
                                    column: 1,
                                    line: 32,
                                },
                                end: {
                                    offset: 271,
                                    column: 14,
                                    line: 33,
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
                                            offset: 243,
                                            column: 1,
                                            line: 32,
                                        },
                                        end: {
                                            offset: 271,
                                            column: 14,
                                            line: 33,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 245,
                                                    column: 3,
                                                    line: 32,
                                                },
                                                end: {
                                                    offset: 271,
                                                    column: 14,
                                                    line: 33,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 245,
                                                            column: 3,
                                                            line: 32,
                                                        },
                                                        end: {
                                                            offset: 271,
                                                            column: 14,
                                                            line: 33,
                                                        },
                                                    },
                                                    value: 'foo bar baz.\nfoo bar baz.',
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
                markup: '1.  foo bar baz.\n\n<!--  -->\n\n99. foo bar baz.\n\n<!--  -->\n\n999.\tfoo bar baz.\n\n<!--  -->\n\n1.\tfoo bar baz.\n   foo bar baz.\n\n<!--  -->\n\n99.    foo bar baz.\n\tfoo bar baz.\n\n<!--  -->\n\n999. foo bar baz.\n\tfoo bar baz.\n\n<!--  -->\n\n-\tfoo bar baz.\n\n<!--  -->\n\n-\tfoo bar baz.\n\tfoo bar baz.\n',
                title: 'ListItem indent = mixed',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 278,
                            column: 1,
                            line: 34,
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
                                    offset: 16,
                                    column: 17,
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
                                            offset: 16,
                                            column: 17,
                                            line: 1,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 4,
                                                    column: 5,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 16,
                                                    column: 17,
                                                    line: 1,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 4,
                                                            column: 5,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 16,
                                                            column: 17,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'foo bar baz.',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'html',
                            position: {
                                start: {
                                    offset: 18,
                                    column: 1,
                                    line: 3,
                                },
                                end: {
                                    offset: 27,
                                    column: 10,
                                    line: 3,
                                },
                            },
                            value: '<!--  -->',
                        },
                        {
                            type: 'list',
                            start: 99,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 29,
                                    column: 1,
                                    line: 5,
                                },
                                end: {
                                    offset: 45,
                                    column: 17,
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
                                            offset: 29,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 45,
                                            column: 17,
                                            line: 5,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 33,
                                                    column: 5,
                                                    line: 5,
                                                },
                                                end: {
                                                    offset: 45,
                                                    column: 17,
                                                    line: 5,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 33,
                                                            column: 5,
                                                            line: 5,
                                                        },
                                                        end: {
                                                            offset: 45,
                                                            column: 17,
                                                            line: 5,
                                                        },
                                                    },
                                                    value: 'foo bar baz.',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'html',
                            position: {
                                start: {
                                    offset: 47,
                                    column: 1,
                                    line: 7,
                                },
                                end: {
                                    offset: 56,
                                    column: 10,
                                    line: 7,
                                },
                            },
                            value: '<!--  -->',
                        },
                        {
                            type: 'list',
                            start: 999,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 58,
                                    column: 1,
                                    line: 9,
                                },
                                end: {
                                    offset: 75,
                                    column: 18,
                                    line: 9,
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
                                            offset: 58,
                                            column: 1,
                                            line: 9,
                                        },
                                        end: {
                                            offset: 75,
                                            column: 18,
                                            line: 9,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 63,
                                                    column: 6,
                                                    line: 9,
                                                },
                                                end: {
                                                    offset: 75,
                                                    column: 18,
                                                    line: 9,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 63,
                                                            column: 6,
                                                            line: 9,
                                                        },
                                                        end: {
                                                            offset: 75,
                                                            column: 18,
                                                            line: 9,
                                                        },
                                                    },
                                                    value: 'foo bar baz.',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'html',
                            position: {
                                start: {
                                    offset: 77,
                                    column: 1,
                                    line: 11,
                                },
                                end: {
                                    offset: 86,
                                    column: 10,
                                    line: 11,
                                },
                            },
                            value: '<!--  -->',
                        },
                        {
                            type: 'list',
                            start: 1,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 88,
                                    column: 1,
                                    line: 13,
                                },
                                end: {
                                    offset: 119,
                                    column: 16,
                                    line: 14,
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
                                            offset: 88,
                                            column: 1,
                                            line: 13,
                                        },
                                        end: {
                                            offset: 119,
                                            column: 16,
                                            line: 14,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 91,
                                                    column: 4,
                                                    line: 13,
                                                },
                                                end: {
                                                    offset: 119,
                                                    column: 16,
                                                    line: 14,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 91,
                                                            column: 4,
                                                            line: 13,
                                                        },
                                                        end: {
                                                            offset: 119,
                                                            column: 16,
                                                            line: 14,
                                                        },
                                                    },
                                                    value: 'foo bar baz.\nfoo bar baz.',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'html',
                            position: {
                                start: {
                                    offset: 121,
                                    column: 1,
                                    line: 16,
                                },
                                end: {
                                    offset: 130,
                                    column: 10,
                                    line: 16,
                                },
                            },
                            value: '<!--  -->',
                        },
                        {
                            type: 'list',
                            start: 99,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 132,
                                    column: 1,
                                    line: 18,
                                },
                                end: {
                                    offset: 165,
                                    column: 14,
                                    line: 19,
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
                                            offset: 132,
                                            column: 1,
                                            line: 18,
                                        },
                                        end: {
                                            offset: 165,
                                            column: 14,
                                            line: 19,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 139,
                                                    column: 8,
                                                    line: 18,
                                                },
                                                end: {
                                                    offset: 165,
                                                    column: 14,
                                                    line: 19,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 139,
                                                            column: 8,
                                                            line: 18,
                                                        },
                                                        end: {
                                                            offset: 165,
                                                            column: 14,
                                                            line: 19,
                                                        },
                                                    },
                                                    value: 'foo bar baz.\nfoo bar baz.',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'html',
                            position: {
                                start: {
                                    offset: 167,
                                    column: 1,
                                    line: 21,
                                },
                                end: {
                                    offset: 176,
                                    column: 10,
                                    line: 21,
                                },
                            },
                            value: '<!--  -->',
                        },
                        {
                            type: 'list',
                            start: 999,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 178,
                                    column: 1,
                                    line: 23,
                                },
                                end: {
                                    offset: 209,
                                    column: 14,
                                    line: 24,
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
                                            offset: 178,
                                            column: 1,
                                            line: 23,
                                        },
                                        end: {
                                            offset: 209,
                                            column: 14,
                                            line: 24,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 183,
                                                    column: 6,
                                                    line: 23,
                                                },
                                                end: {
                                                    offset: 209,
                                                    column: 14,
                                                    line: 24,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 183,
                                                            column: 6,
                                                            line: 23,
                                                        },
                                                        end: {
                                                            offset: 209,
                                                            column: 14,
                                                            line: 24,
                                                        },
                                                    },
                                                    value: 'foo bar baz.\nfoo bar baz.',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'html',
                            position: {
                                start: {
                                    offset: 211,
                                    column: 1,
                                    line: 26,
                                },
                                end: {
                                    offset: 220,
                                    column: 10,
                                    line: 26,
                                },
                            },
                            value: '<!--  -->',
                        },
                        {
                            type: 'list',
                            start: null,
                            loose: false,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 222,
                                    column: 1,
                                    line: 28,
                                },
                                end: {
                                    offset: 236,
                                    column: 15,
                                    line: 28,
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
                                            offset: 222,
                                            column: 1,
                                            line: 28,
                                        },
                                        end: {
                                            offset: 236,
                                            column: 15,
                                            line: 28,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 224,
                                                    column: 3,
                                                    line: 28,
                                                },
                                                end: {
                                                    offset: 236,
                                                    column: 15,
                                                    line: 28,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 224,
                                                            column: 3,
                                                            line: 28,
                                                        },
                                                        end: {
                                                            offset: 236,
                                                            column: 15,
                                                            line: 28,
                                                        },
                                                    },
                                                    value: 'foo bar baz.',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'html',
                            position: {
                                start: {
                                    offset: 238,
                                    column: 1,
                                    line: 30,
                                },
                                end: {
                                    offset: 247,
                                    column: 10,
                                    line: 30,
                                },
                            },
                            value: '<!--  -->',
                        },
                        {
                            type: 'list',
                            start: null,
                            loose: false,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 249,
                                    column: 1,
                                    line: 32,
                                },
                                end: {
                                    offset: 277,
                                    column: 14,
                                    line: 33,
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
                                            offset: 249,
                                            column: 1,
                                            line: 32,
                                        },
                                        end: {
                                            offset: 277,
                                            column: 14,
                                            line: 33,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 251,
                                                    column: 3,
                                                    line: 32,
                                                },
                                                end: {
                                                    offset: 277,
                                                    column: 14,
                                                    line: 33,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 251,
                                                            column: 3,
                                                            line: 32,
                                                        },
                                                        end: {
                                                            offset: 277,
                                                            column: 14,
                                                            line: 33,
                                                        },
                                                    },
                                                    value: 'foo bar baz.\nfoo bar baz.',
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
                markup: '1. нумерованный список\n1. нумерованный список-2\n1.#8 нумерованный список-2, с пропуском пунктов\n3. это девятый пункт\n',
                title: 'Нумерованный список с пропуском пунктов',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 117,
                            column: 1,
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
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 116,
                                    column: 21,
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
                                                            offset: 3,
                                                            column: 4,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 22,
                                                            column: 23,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'нумерованный список',
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
                                            offset: 23,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 47,
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
                                                    offset: 26,
                                                    column: 4,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 47,
                                                    column: 25,
                                                    line: 2,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 26,
                                                            column: 4,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 47,
                                                            column: 25,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'нумерованный список-2',
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
                                            offset: 48,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 95,
                                            column: 48,
                                            line: 3,
                                        },
                                    },
                                    restart: 8,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 53,
                                                    column: 6,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 95,
                                                    column: 48,
                                                    line: 3,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 53,
                                                            column: 6,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 95,
                                                            column: 48,
                                                            line: 3,
                                                        },
                                                    },
                                                    value: 'нумерованный список-2, с пропуском пунктов',
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
                                            offset: 96,
                                            column: 1,
                                            line: 4,
                                        },
                                        end: {
                                            offset: 116,
                                            column: 21,
                                            line: 4,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 99,
                                                    column: 4,
                                                    line: 4,
                                                },
                                                end: {
                                                    offset: 116,
                                                    column: 21,
                                                    line: 4,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 99,
                                                            column: 4,
                                                            line: 4,
                                                        },
                                                        end: {
                                                            offset: 116,
                                                            column: 21,
                                                            line: 4,
                                                        },
                                                    },
                                                    value: 'это девятый пункт',
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
                markup: '*#99 ненумерованный список, с пропуском пунктов\n* это не сотый пункт\n',
                title: 'Ненумерованный список с пропуском пунктов',
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
                                    offset: 48,
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
                                            offset: 47,
                                            column: 48,
                                            line: 1,
                                        },
                                    },
                                    value: '*#99 ненумерованный список, с пропуском пунктов',
                                },
                            ],
                        },
                        {
                            type: 'list',
                            start: null,
                            loose: false,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 48,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 68,
                                    column: 21,
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
                                            offset: 48,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 68,
                                            column: 21,
                                            line: 2,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 50,
                                                    column: 3,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 68,
                                                    column: 21,
                                                    line: 2,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 50,
                                                            column: 3,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 68,
                                                            column: 21,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'это не сотый пункт',
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
                markup: '- [x] Finish my changes\n- [ ] Push my commits to GitHub\n- [x] Open a pull request\n- [] Oopsie\n',
                title: 'Списки с тудушками',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 94,
                            column: 1,
                            line: 5,
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
                                    offset: 93,
                                    column: 12,
                                    line: 4,
                                },
                            },
                            children: [
                                {
                                    type: 'listItem',
                                    checked: true,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 23,
                                            column: 24,
                                            line: 1,
                                        },
                                    },
                                    restart: null,
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
                                                    offset: 23,
                                                    column: 24,
                                                    line: 1,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 6,
                                                            column: 7,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 23,
                                                            column: 24,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'Finish my changes',
                                                },
                                            ],
                                        },
                                    ],
                                },
                                {
                                    type: 'listItem',
                                    checked: false,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 24,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 55,
                                            column: 32,
                                            line: 2,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 30,
                                                    column: 7,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 55,
                                                    column: 32,
                                                    line: 2,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 30,
                                                            column: 7,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 55,
                                                            column: 32,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'Push my commits to GitHub',
                                                },
                                            ],
                                        },
                                    ],
                                },
                                {
                                    type: 'listItem',
                                    checked: true,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 56,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 81,
                                            column: 26,
                                            line: 3,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 62,
                                                    column: 7,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 81,
                                                    column: 26,
                                                    line: 3,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 62,
                                                            column: 7,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 81,
                                                            column: 26,
                                                            line: 3,
                                                        },
                                                    },
                                                    value: 'Open a pull request',
                                                },
                                            ],
                                        },
                                    ],
                                },
                                {
                                    type: 'listItem',
                                    checked: false,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 82,
                                            column: 1,
                                            line: 4,
                                        },
                                        end: {
                                            offset: 93,
                                            column: 12,
                                            line: 4,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 87,
                                                    column: 6,
                                                    line: 4,
                                                },
                                                end: {
                                                    offset: 93,
                                                    column: 12,
                                                    line: 4,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 87,
                                                            column: 6,
                                                            line: 4,
                                                        },
                                                        end: {
                                                            offset: 93,
                                                            column: 12,
                                                            line: 4,
                                                        },
                                                    },
                                                    value: 'Oopsie',
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
                markup: '1. список\n    1. вложенный список\n       * ещё более вложенный список\n    1. вложенный список-2\n2. список-2\n',
                title: 'Вложенные списки',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 108,
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
                                    offset: 107,
                                    column: 12,
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
                                            offset: 95,
                                            column: 26,
                                            line: 4,
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
                                                    offset: 14,
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
                                                            column: 4,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 9,
                                                            column: 10,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'список',
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
                                                    offset: 14,
                                                    column: 5,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 95,
                                                    column: 26,
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
                                                            offset: 14,
                                                            column: 5,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 69,
                                                            column: 36,
                                                            line: 3,
                                                        },
                                                    },
                                                    restart: null,
                                                    children: [
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 17,
                                                                    column: 8,
                                                                    line: 2,
                                                                },
                                                                end: {
                                                                    offset: 41,
                                                                    column: 8,
                                                                    line: 3,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'text',
                                                                    position: {
                                                                        start: {
                                                                            offset: 17,
                                                                            column: 8,
                                                                            line: 2,
                                                                        },
                                                                        end: {
                                                                            offset: 33,
                                                                            column: 24,
                                                                            line: 2,
                                                                        },
                                                                    },
                                                                    value: 'вложенный список',
                                                                },
                                                            ],
                                                        },
                                                        {
                                                            type: 'list',
                                                            start: null,
                                                            loose: false,
                                                            ordered: false,
                                                            position: {
                                                                start: {
                                                                    offset: 41,
                                                                    column: 8,
                                                                    line: 3,
                                                                },
                                                                end: {
                                                                    offset: 69,
                                                                    column: 36,
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
                                                                            offset: 41,
                                                                            column: 8,
                                                                            line: 3,
                                                                        },
                                                                        end: {
                                                                            offset: 69,
                                                                            column: 36,
                                                                            line: 3,
                                                                        },
                                                                    },
                                                                    restart: null,
                                                                    children: [
                                                                        {
                                                                            type: 'paragraph',
                                                                            position: {
                                                                                start: {
                                                                                    offset: 43,
                                                                                    column: 10,
                                                                                    line: 3,
                                                                                },
                                                                                end: {
                                                                                    offset: 69,
                                                                                    column: 36,
                                                                                    line: 3,
                                                                                },
                                                                            },
                                                                            children: [
                                                                                {
                                                                                    type: 'text',
                                                                                    position: {
                                                                                        start: {
                                                                                            offset: 43,
                                                                                            column: 10,
                                                                                            line: 3,
                                                                                        },
                                                                                        end: {
                                                                                            offset: 69,
                                                                                            column: 36,
                                                                                            line: 3,
                                                                                        },
                                                                                    },
                                                                                    value: 'ещё более вложенный список',
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
                                                            offset: 74,
                                                            column: 5,
                                                            line: 4,
                                                        },
                                                        end: {
                                                            offset: 95,
                                                            column: 26,
                                                            line: 4,
                                                        },
                                                    },
                                                    restart: null,
                                                    children: [
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 77,
                                                                    column: 8,
                                                                    line: 4,
                                                                },
                                                                end: {
                                                                    offset: 95,
                                                                    column: 26,
                                                                    line: 4,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'text',
                                                                    position: {
                                                                        start: {
                                                                            offset: 77,
                                                                            column: 8,
                                                                            line: 4,
                                                                        },
                                                                        end: {
                                                                            offset: 95,
                                                                            column: 26,
                                                                            line: 4,
                                                                        },
                                                                    },
                                                                    value: 'вложенный список-2',
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
                                            offset: 96,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 107,
                                            column: 12,
                                            line: 5,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 99,
                                                    column: 4,
                                                    line: 5,
                                                },
                                                end: {
                                                    offset: 107,
                                                    column: 12,
                                                    line: 5,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 99,
                                                            column: 4,
                                                            line: 5,
                                                        },
                                                        end: {
                                                            offset: 107,
                                                            column: 12,
                                                            line: 5,
                                                        },
                                                    },
                                                    value: 'список-2',
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
                markup: '1.#10+ раз\n1.+ два, свернутый пункт\nСкрытый текст списка\n1. три\n---\n1.+ раз\n1.#10+ два\n',
                title: 'Свернутые списки',
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
                            column: 1,
                            line: 8,
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
                                    offset: 63,
                                    column: 7,
                                    line: 4,
                                },
                            },
                            styleType: 'decimal',
                            children: [
                                {
                                    type: 'listItem',
                                    title: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 7,
                                                    column: 8,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 10,
                                                    column: 11,
                                                    line: 1,
                                                },
                                            },
                                            value: 'раз',
                                        },
                                    ],
                                    checked: null,
                                    expandable: true,
                                    loose: false,
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
                                    restart: 10,
                                    children: [
                                    ],
                                },
                                {
                                    type: 'listItem',
                                    title: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 15,
                                                    column: 5,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 35,
                                                    column: 25,
                                                    line: 2,
                                                },
                                            },
                                            value: 'два, свернутый пункт',
                                        },
                                    ],
                                    checked: null,
                                    expandable: true,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 11,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 56,
                                            column: 21,
                                            line: 3,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 36,
                                                    column: 1,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 56,
                                                    column: 21,
                                                    line: 3,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 36,
                                                            column: 1,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 56,
                                                            column: 21,
                                                            line: 3,
                                                        },
                                                    },
                                                    value: 'Скрытый текст списка',
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
                                            offset: 57,
                                            column: 1,
                                            line: 4,
                                        },
                                        end: {
                                            offset: 63,
                                            column: 7,
                                            line: 4,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 60,
                                                    column: 4,
                                                    line: 4,
                                                },
                                                end: {
                                                    offset: 63,
                                                    column: 7,
                                                    line: 4,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 60,
                                                            column: 4,
                                                            line: 4,
                                                        },
                                                        end: {
                                                            offset: 63,
                                                            column: 7,
                                                            line: 4,
                                                        },
                                                    },
                                                    value: 'три',
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
                                    offset: 64,
                                    column: 1,
                                    line: 5,
                                },
                                end: {
                                    offset: 67,
                                    column: 4,
                                    line: 5,
                                },
                            },
                        },
                        {
                            type: 'list',
                            start: 1,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 68,
                                    column: 1,
                                    line: 6,
                                },
                                end: {
                                    offset: 86,
                                    column: 11,
                                    line: 7,
                                },
                            },
                            styleType: 'decimal',
                            children: [
                                {
                                    type: 'listItem',
                                    title: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 72,
                                                    column: 5,
                                                    line: 6,
                                                },
                                                end: {
                                                    offset: 75,
                                                    column: 8,
                                                    line: 6,
                                                },
                                            },
                                            value: 'раз',
                                        },
                                    ],
                                    checked: null,
                                    expandable: true,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 68,
                                            column: 1,
                                            line: 6,
                                        },
                                        end: {
                                            offset: 75,
                                            column: 8,
                                            line: 6,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                    ],
                                },
                                {
                                    type: 'listItem',
                                    title: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 83,
                                                    column: 8,
                                                    line: 7,
                                                },
                                                end: {
                                                    offset: 86,
                                                    column: 11,
                                                    line: 7,
                                                },
                                            },
                                            value: 'два',
                                        },
                                    ],
                                    checked: null,
                                    expandable: true,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 76,
                                            column: 1,
                                            line: 7,
                                        },
                                        end: {
                                            offset: 86,
                                            column: 11,
                                            line: 7,
                                        },
                                    },
                                    restart: 10,
                                    children: [
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '1.+свернутый пункт\n1.#10+еще один\n',
                title: 'Свернутые списки без пробела',
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
                                    offset: 34,
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
                                            offset: 33,
                                            column: 15,
                                            line: 2,
                                        },
                                    },
                                    value: '1.+свернутый пункт\n1.#10+еще один',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '1.+ свернутый пункт\n<{открыть\nСкрытый текст списка\n}>\n',
                title: 'Свернутые списки c катом',
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
                            styleType: 'decimal',
                            children: [
                                {
                                    type: 'listItem',
                                    title: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 4,
                                                    column: 5,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 19,
                                                    column: 20,
                                                    line: 1,
                                                },
                                            },
                                            value: 'свернутый пункт',
                                        },
                                    ],
                                    checked: null,
                                    expandable: true,
                                    loose: false,
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
                                    restart: null,
                                    children: [
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'womCut',
                            title: [
                                {
                                    type: 'paragraph',
                                    position: {
                                        start: {
                                            offset: 22,
                                            column: 3,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 29,
                                            column: 10,
                                            line: 2,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 22,
                                                    column: 3,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 29,
                                                    column: 10,
                                                    line: 2,
                                                },
                                            },
                                            value: 'открыть',
                                        },
                                    ],
                                },
                            ],
                            inline: false,
                            position: {
                                start: {
                                    offset: 20,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 53,
                                    column: 3,
                                    line: 4,
                                },
                            },
                            children: [
                                {
                                    type: 'paragraph',
                                    position: {
                                        start: {
                                            offset: 30,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 50,
                                            column: 21,
                                            line: 3,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 30,
                                                    column: 1,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 50,
                                                    column: 21,
                                                    line: 3,
                                                },
                                            },
                                            value: 'Скрытый текст списка',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '1. список\n   1. вложенный список\n   1. вложенный список\n   2.+ вложенный список, свернутый пункт\n       * Скрытый пункт списка\n         * Скрытый пункт списка\n       * Скрытый пункт списка\n2. список-2\n',
                title: 'Свернутые вложенные списки',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 201,
                            column: 1,
                            line: 9,
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
                                    offset: 200,
                                    column: 12,
                                    line: 8,
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
                                            offset: 188,
                                            column: 30,
                                            line: 7,
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
                                                    offset: 13,
                                                    column: 4,
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
                                                            offset: 9,
                                                            column: 10,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'список',
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
                                                    offset: 13,
                                                    column: 4,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 188,
                                                    column: 30,
                                                    line: 7,
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
                                                            offset: 13,
                                                            column: 4,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 32,
                                                            column: 23,
                                                            line: 2,
                                                        },
                                                    },
                                                    restart: null,
                                                    children: [
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 16,
                                                                    column: 7,
                                                                    line: 2,
                                                                },
                                                                end: {
                                                                    offset: 32,
                                                                    column: 23,
                                                                    line: 2,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'text',
                                                                    position: {
                                                                        start: {
                                                                            offset: 16,
                                                                            column: 7,
                                                                            line: 2,
                                                                        },
                                                                        end: {
                                                                            offset: 32,
                                                                            column: 23,
                                                                            line: 2,
                                                                        },
                                                                    },
                                                                    value: 'вложенный список',
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
                                                            offset: 36,
                                                            column: 4,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 55,
                                                            column: 23,
                                                            line: 3,
                                                        },
                                                    },
                                                    restart: null,
                                                    children: [
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 39,
                                                                    column: 7,
                                                                    line: 3,
                                                                },
                                                                end: {
                                                                    offset: 55,
                                                                    column: 23,
                                                                    line: 3,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'text',
                                                                    position: {
                                                                        start: {
                                                                            offset: 39,
                                                                            column: 7,
                                                                            line: 3,
                                                                        },
                                                                        end: {
                                                                            offset: 55,
                                                                            column: 23,
                                                                            line: 3,
                                                                        },
                                                                    },
                                                                    value: 'вложенный список',
                                                                },
                                                            ],
                                                        },
                                                    ],
                                                },
                                                {
                                                    type: 'listItem',
                                                    title: [
                                                        {
                                                            type: 'text',
                                                            position: {
                                                                start: {
                                                                    offset: 63,
                                                                    column: 8,
                                                                    line: 4,
                                                                },
                                                                end: {
                                                                    offset: 96,
                                                                    column: 41,
                                                                    line: 4,
                                                                },
                                                            },
                                                            value: 'вложенный список, свернутый пункт',
                                                        },
                                                    ],
                                                    checked: null,
                                                    expandable: true,
                                                    loose: false,
                                                    position: {
                                                        start: {
                                                            offset: 59,
                                                            column: 4,
                                                            line: 4,
                                                        },
                                                        end: {
                                                            offset: 188,
                                                            column: 30,
                                                            line: 7,
                                                        },
                                                    },
                                                    restart: null,
                                                    children: [
                                                        {
                                                            type: 'list',
                                                            start: null,
                                                            loose: false,
                                                            ordered: false,
                                                            position: {
                                                                start: {
                                                                    offset: 104,
                                                                    column: 8,
                                                                    line: 5,
                                                                },
                                                                end: {
                                                                    offset: 188,
                                                                    column: 30,
                                                                    line: 7,
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
                                                                            offset: 104,
                                                                            column: 8,
                                                                            line: 5,
                                                                        },
                                                                        end: {
                                                                            offset: 158,
                                                                            column: 32,
                                                                            line: 6,
                                                                        },
                                                                    },
                                                                    restart: null,
                                                                    children: [
                                                                        {
                                                                            type: 'paragraph',
                                                                            position: {
                                                                                start: {
                                                                                    offset: 106,
                                                                                    column: 10,
                                                                                    line: 5,
                                                                                },
                                                                                end: {
                                                                                    offset: 136,
                                                                                    column: 10,
                                                                                    line: 6,
                                                                                },
                                                                            },
                                                                            children: [
                                                                                {
                                                                                    type: 'text',
                                                                                    position: {
                                                                                        start: {
                                                                                            offset: 106,
                                                                                            column: 10,
                                                                                            line: 5,
                                                                                        },
                                                                                        end: {
                                                                                            offset: 126,
                                                                                            column: 30,
                                                                                            line: 5,
                                                                                        },
                                                                                    },
                                                                                    value: 'Скрытый пункт списка',
                                                                                },
                                                                            ],
                                                                        },
                                                                        {
                                                                            type: 'list',
                                                                            start: null,
                                                                            loose: false,
                                                                            ordered: false,
                                                                            position: {
                                                                                start: {
                                                                                    offset: 136,
                                                                                    column: 10,
                                                                                    line: 6,
                                                                                },
                                                                                end: {
                                                                                    offset: 158,
                                                                                    column: 32,
                                                                                    line: 6,
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
                                                                                            offset: 136,
                                                                                            column: 10,
                                                                                            line: 6,
                                                                                        },
                                                                                        end: {
                                                                                            offset: 158,
                                                                                            column: 32,
                                                                                            line: 6,
                                                                                        },
                                                                                    },
                                                                                    restart: null,
                                                                                    children: [
                                                                                        {
                                                                                            type: 'paragraph',
                                                                                            position: {
                                                                                                start: {
                                                                                                    offset: 138,
                                                                                                    column: 12,
                                                                                                    line: 6,
                                                                                                },
                                                                                                end: {
                                                                                                    offset: 158,
                                                                                                    column: 32,
                                                                                                    line: 6,
                                                                                                },
                                                                                            },
                                                                                            children: [
                                                                                                {
                                                                                                    type: 'text',
                                                                                                    position: {
                                                                                                        start: {
                                                                                                            offset: 138,
                                                                                                            column: 12,
                                                                                                            line: 6,
                                                                                                        },
                                                                                                        end: {
                                                                                                            offset: 158,
                                                                                                            column: 32,
                                                                                                            line: 6,
                                                                                                        },
                                                                                                    },
                                                                                                    value: 'Скрытый пункт списка',
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
                                                                            offset: 166,
                                                                            column: 8,
                                                                            line: 7,
                                                                        },
                                                                        end: {
                                                                            offset: 188,
                                                                            column: 30,
                                                                            line: 7,
                                                                        },
                                                                    },
                                                                    restart: null,
                                                                    children: [
                                                                        {
                                                                            type: 'paragraph',
                                                                            position: {
                                                                                start: {
                                                                                    offset: 168,
                                                                                    column: 10,
                                                                                    line: 7,
                                                                                },
                                                                                end: {
                                                                                    offset: 188,
                                                                                    column: 30,
                                                                                    line: 7,
                                                                                },
                                                                            },
                                                                            children: [
                                                                                {
                                                                                    type: 'text',
                                                                                    position: {
                                                                                        start: {
                                                                                            offset: 168,
                                                                                            column: 10,
                                                                                            line: 7,
                                                                                        },
                                                                                        end: {
                                                                                            offset: 188,
                                                                                            column: 30,
                                                                                            line: 7,
                                                                                        },
                                                                                    },
                                                                                    value: 'Скрытый пункт списка',
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
                                            offset: 189,
                                            column: 1,
                                            line: 8,
                                        },
                                        end: {
                                            offset: 200,
                                            column: 12,
                                            line: 8,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 192,
                                                    column: 4,
                                                    line: 8,
                                                },
                                                end: {
                                                    offset: 200,
                                                    column: 12,
                                                    line: 8,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 192,
                                                            column: 4,
                                                            line: 8,
                                                        },
                                                        end: {
                                                            offset: 200,
                                                            column: 12,
                                                            line: 8,
                                                        },
                                                    },
                                                    value: 'список-2',
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
                markup: '*+ свернутый пункт\nСкрытый текст списка\n',
                title: 'Свернутые ненумерованные списки',
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
                                    offset: 39,
                                    column: 21,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'listItem',
                                    title: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 3,
                                                    column: 4,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 18,
                                                    column: 19,
                                                    line: 1,
                                                },
                                            },
                                            value: 'свернутый пункт',
                                        },
                                    ],
                                    checked: null,
                                    expandable: true,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 39,
                                            column: 21,
                                            line: 2,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 19,
                                                    column: 1,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 39,
                                                    column: 21,
                                                    line: 2,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 19,
                                                            column: 1,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 39,
                                                            column: 21,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'Скрытый текст списка',
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
                markup: '1. Ordered List\n2. text %%code%% text\n3. text %%code code code\ncode code code code code\ncode code code code code%% text\n4.#99 text\n5. %%(cs)\ncodecodecodecodecode\ncodecodecodecodecode\ncode code code code code\n%%\n6. вложенный список\n    2.+ вложенный список, свёртнутый пункт\n        * %%code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code %%\n           * %%codecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecode%%\n        * Скрытый пункт %%списка%%\n',
                title: 'Сложненький список (feat formatter)',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 840,
                            column: 1,
                            line: 17,
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
                                    offset: 839,
                                    column: 35,
                                    line: 16,
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
                                            offset: 15,
                                            column: 16,
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
                                                            offset: 15,
                                                            column: 16,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'Ordered List',
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
                                            offset: 16,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 37,
                                            column: 22,
                                            line: 2,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 19,
                                                    column: 4,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 37,
                                                    column: 22,
                                                    line: 2,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 19,
                                                            column: 4,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 24,
                                                            column: 9,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'text ',
                                                },
                                                {
                                                    type: 'womFormatter',
                                                    inline: true,
                                                    position: {
                                                        start: {
                                                            offset: 24,
                                                            column: 9,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 32,
                                                            column: 17,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'code',
                                                },
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 32,
                                                            column: 17,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 37,
                                                            column: 22,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: ' text',
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
                                            offset: 38,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 119,
                                            column: 32,
                                            line: 5,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 41,
                                                    column: 4,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 119,
                                                    column: 32,
                                                    line: 5,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 41,
                                                            column: 4,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 46,
                                                            column: 9,
                                                            line: 3,
                                                        },
                                                    },
                                                    value: 'text ',
                                                },
                                                {
                                                    type: 'womFormatter',
                                                    inline: true,
                                                    position: {
                                                        start: {
                                                            offset: 46,
                                                            column: 9,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 114,
                                                            column: 27,
                                                            line: 5,
                                                        },
                                                    },
                                                    value: 'code code code\ncode code code code code\ncode code code code code',
                                                },
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 114,
                                                            column: 27,
                                                            line: 5,
                                                        },
                                                        end: {
                                                            offset: 119,
                                                            column: 32,
                                                            line: 5,
                                                        },
                                                    },
                                                    value: ' text',
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
                                            offset: 120,
                                            column: 1,
                                            line: 6,
                                        },
                                        end: {
                                            offset: 130,
                                            column: 11,
                                            line: 6,
                                        },
                                    },
                                    restart: 99,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 126,
                                                    column: 7,
                                                    line: 6,
                                                },
                                                end: {
                                                    offset: 130,
                                                    column: 11,
                                                    line: 6,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 126,
                                                            column: 7,
                                                            line: 6,
                                                        },
                                                        end: {
                                                            offset: 130,
                                                            column: 11,
                                                            line: 6,
                                                        },
                                                    },
                                                    value: 'text',
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
                                            offset: 131,
                                            column: 1,
                                            line: 7,
                                        },
                                        end: {
                                            offset: 210,
                                            column: 3,
                                            line: 11,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'womFormatter',
                                            attributes: {
                                            },
                                            format: 'cs',
                                            inline: false,
                                            position: {
                                                start: {
                                                    offset: 134,
                                                    column: 4,
                                                    line: 7,
                                                },
                                                end: {
                                                    offset: 210,
                                                    column: 3,
                                                    line: 11,
                                                },
                                            },
                                            value: 'codecodecodecodecode\ncodecodecodecodecode\ncode code code code code\n',
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
                                            offset: 211,
                                            column: 1,
                                            line: 12,
                                        },
                                        end: {
                                            offset: 839,
                                            column: 35,
                                            line: 16,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 214,
                                                    column: 4,
                                                    line: 12,
                                                },
                                                end: {
                                                    offset: 235,
                                                    column: 5,
                                                    line: 13,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 214,
                                                            column: 4,
                                                            line: 12,
                                                        },
                                                        end: {
                                                            offset: 230,
                                                            column: 20,
                                                            line: 12,
                                                        },
                                                    },
                                                    value: 'вложенный список',
                                                },
                                            ],
                                        },
                                        {
                                            type: 'list',
                                            start: 2,
                                            loose: false,
                                            ordered: true,
                                            position: {
                                                start: {
                                                    offset: 235,
                                                    column: 5,
                                                    line: 13,
                                                },
                                                end: {
                                                    offset: 839,
                                                    column: 35,
                                                    line: 16,
                                                },
                                            },
                                            styleType: 'decimal',
                                            children: [
                                                {
                                                    type: 'listItem',
                                                    title: [
                                                        {
                                                            type: 'text',
                                                            position: {
                                                                start: {
                                                                    offset: 239,
                                                                    column: 9,
                                                                    line: 13,
                                                                },
                                                                end: {
                                                                    offset: 273,
                                                                    column: 43,
                                                                    line: 13,
                                                                },
                                                            },
                                                            value: 'вложенный список, свёртнутый пункт',
                                                        },
                                                    ],
                                                    checked: null,
                                                    expandable: true,
                                                    loose: false,
                                                    position: {
                                                        start: {
                                                            offset: 235,
                                                            column: 5,
                                                            line: 13,
                                                        },
                                                        end: {
                                                            offset: 839,
                                                            column: 35,
                                                            line: 16,
                                                        },
                                                    },
                                                    restart: null,
                                                    children: [
                                                        {
                                                            type: 'list',
                                                            start: null,
                                                            loose: false,
                                                            ordered: false,
                                                            position: {
                                                                start: {
                                                                    offset: 282,
                                                                    column: 9,
                                                                    line: 14,
                                                                },
                                                                end: {
                                                                    offset: 839,
                                                                    column: 35,
                                                                    line: 16,
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
                                                                            offset: 282,
                                                                            column: 9,
                                                                            line: 14,
                                                                        },
                                                                        end: {
                                                                            offset: 804,
                                                                            column: 306,
                                                                            line: 15,
                                                                        },
                                                                    },
                                                                    restart: null,
                                                                    children: [
                                                                        {
                                                                            type: 'paragraph',
                                                                            position: {
                                                                                start: {
                                                                                    offset: 284,
                                                                                    column: 11,
                                                                                    line: 14,
                                                                                },
                                                                                end: {
                                                                                    offset: 509,
                                                                                    column: 11,
                                                                                    line: 15,
                                                                                },
                                                                            },
                                                                            children: [
                                                                                {
                                                                                    type: 'womFormatter',
                                                                                    inline: true,
                                                                                    position: {
                                                                                        start: {
                                                                                            offset: 284,
                                                                                            column: 11,
                                                                                            line: 14,
                                                                                        },
                                                                                        end: {
                                                                                            offset: 498,
                                                                                            column: 225,
                                                                                            line: 14,
                                                                                        },
                                                                                    },
                                                                                    value: 'code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code code ',
                                                                                },
                                                                            ],
                                                                        },
                                                                        {
                                                                            type: 'list',
                                                                            start: null,
                                                                            loose: false,
                                                                            ordered: false,
                                                                            position: {
                                                                                start: {
                                                                                    offset: 509,
                                                                                    column: 11,
                                                                                    line: 15,
                                                                                },
                                                                                end: {
                                                                                    offset: 804,
                                                                                    column: 306,
                                                                                    line: 15,
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
                                                                                            offset: 509,
                                                                                            column: 11,
                                                                                            line: 15,
                                                                                        },
                                                                                        end: {
                                                                                            offset: 804,
                                                                                            column: 306,
                                                                                            line: 15,
                                                                                        },
                                                                                    },
                                                                                    restart: null,
                                                                                    children: [
                                                                                        {
                                                                                            type: 'paragraph',
                                                                                            position: {
                                                                                                start: {
                                                                                                    offset: 512,
                                                                                                    column: 14,
                                                                                                    line: 15,
                                                                                                },
                                                                                                end: {
                                                                                                    offset: 804,
                                                                                                    column: 306,
                                                                                                    line: 15,
                                                                                                },
                                                                                            },
                                                                                            children: [
                                                                                                {
                                                                                                    type: 'womFormatter',
                                                                                                    inline: true,
                                                                                                    position: {
                                                                                                        start: {
                                                                                                            offset: 512,
                                                                                                            column: 14,
                                                                                                            line: 15,
                                                                                                        },
                                                                                                        end: {
                                                                                                            offset: 804,
                                                                                                            column: 306,
                                                                                                            line: 15,
                                                                                                        },
                                                                                                    },
                                                                                                    value: 'codecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecodecode',
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
                                                                            offset: 813,
                                                                            column: 9,
                                                                            line: 16,
                                                                        },
                                                                        end: {
                                                                            offset: 839,
                                                                            column: 35,
                                                                            line: 16,
                                                                        },
                                                                    },
                                                                    restart: null,
                                                                    children: [
                                                                        {
                                                                            type: 'paragraph',
                                                                            position: {
                                                                                start: {
                                                                                    offset: 815,
                                                                                    column: 11,
                                                                                    line: 16,
                                                                                },
                                                                                end: {
                                                                                    offset: 839,
                                                                                    column: 35,
                                                                                    line: 16,
                                                                                },
                                                                            },
                                                                            children: [
                                                                                {
                                                                                    type: 'text',
                                                                                    position: {
                                                                                        start: {
                                                                                            offset: 815,
                                                                                            column: 11,
                                                                                            line: 16,
                                                                                        },
                                                                                        end: {
                                                                                            offset: 829,
                                                                                            column: 25,
                                                                                            line: 16,
                                                                                        },
                                                                                    },
                                                                                    value: 'Скрытый пункт ',
                                                                                },
                                                                                {
                                                                                    type: 'womFormatter',
                                                                                    inline: true,
                                                                                    position: {
                                                                                        start: {
                                                                                            offset: 829,
                                                                                            column: 25,
                                                                                            line: 16,
                                                                                        },
                                                                                        end: {
                                                                                            offset: 839,
                                                                                            column: 35,
                                                                                            line: 16,
                                                                                        },
                                                                                    },
                                                                                    value: 'списка',
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
                            ],
                        },
                    ],
                },
            },
            {
                markup: '* <{tracker\n\n  Текст текст текст}>\n* <{tracker-client\n\n  Текст текст текст}>\n',
                title: 'Списки с катами (feat cut)',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 77,
                            column: 1,
                            line: 7,
                        },
                    },
                    children: [
                        {
                            type: 'list',
                            start: null,
                            loose: true,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 76,
                                    column: 22,
                                    line: 6,
                                },
                            },
                            children: [
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: true,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 34,
                                            column: 22,
                                            line: 3,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'womCut',
                                            title: [
                                                {
                                                    type: 'paragraph',
                                                    position: {
                                                        start: {
                                                            offset: 4,
                                                            column: 5,
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
                                                            type: 'text',
                                                            position: {
                                                                start: {
                                                                    offset: 4,
                                                                    column: 5,
                                                                    line: 1,
                                                                },
                                                                end: {
                                                                    offset: 11,
                                                                    column: 12,
                                                                    line: 1,
                                                                },
                                                            },
                                                            value: 'tracker',
                                                        },
                                                    ],
                                                },
                                            ],
                                            inline: false,
                                            position: {
                                                start: {
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 34,
                                                    column: 22,
                                                    line: 3,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'paragraph',
                                                    position: {
                                                        start: {
                                                            offset: 15,
                                                            column: 3,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 32,
                                                            column: 20,
                                                            line: 3,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'text',
                                                            position: {
                                                                start: {
                                                                    offset: 15,
                                                                    column: 3,
                                                                    line: 3,
                                                                },
                                                                end: {
                                                                    offset: 32,
                                                                    column: 20,
                                                                    line: 3,
                                                                },
                                                            },
                                                            value: 'Текст текст текст',
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
                                    loose: true,
                                    position: {
                                        start: {
                                            offset: 35,
                                            column: 1,
                                            line: 4,
                                        },
                                        end: {
                                            offset: 76,
                                            column: 22,
                                            line: 6,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'womCut',
                                            title: [
                                                {
                                                    type: 'paragraph',
                                                    position: {
                                                        start: {
                                                            offset: 39,
                                                            column: 5,
                                                            line: 4,
                                                        },
                                                        end: {
                                                            offset: 53,
                                                            column: 19,
                                                            line: 4,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'text',
                                                            position: {
                                                                start: {
                                                                    offset: 39,
                                                                    column: 5,
                                                                    line: 4,
                                                                },
                                                                end: {
                                                                    offset: 53,
                                                                    column: 19,
                                                                    line: 4,
                                                                },
                                                            },
                                                            value: 'tracker-client',
                                                        },
                                                    ],
                                                },
                                            ],
                                            inline: false,
                                            position: {
                                                start: {
                                                    offset: 37,
                                                    column: 3,
                                                    line: 4,
                                                },
                                                end: {
                                                    offset: 76,
                                                    column: 22,
                                                    line: 6,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'paragraph',
                                                    position: {
                                                        start: {
                                                            offset: 57,
                                                            column: 3,
                                                            line: 6,
                                                        },
                                                        end: {
                                                            offset: 74,
                                                            column: 20,
                                                            line: 6,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'text',
                                                            position: {
                                                                start: {
                                                                    offset: 57,
                                                                    column: 3,
                                                                    line: 6,
                                                                },
                                                                end: {
                                                                    offset: 74,
                                                                    column: 20,
                                                                    line: 6,
                                                                },
                                                            },
                                                            value: 'Текст текст текст',
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
                markup: '<[section1]>\n* [x] ABC-1234[ --Важная задача-- ](  )\n\n<[section2]>\n* [ ] ABC-1233[ Важная задача 2 ](  )\n\n\n* ABC-1232[ Важная задача 3 ](  ) - должно закрыться после п.1\n\n==== section3\n* [] ABC-1231[ Важная задача 4 ](  )\n\n',
                title: 'Списки через цитирование и заголовки (feat blockquote, ticket, heading)',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 223,
                            column: 1,
                            line: 13,
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
                                    type: 'womBlockquote',
                                    inline: true,
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
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 10,
                                                    column: 11,
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
                                                            offset: 10,
                                                            column: 11,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'section1',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'list',
                            start: null,
                            loose: false,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 13,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 52,
                                    column: 40,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'listItem',
                                    checked: true,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 13,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 52,
                                            column: 40,
                                            line: 2,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 19,
                                                    column: 7,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 52,
                                                    column: 40,
                                                    line: 2,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'womTicket',
                                                    position: {
                                                        start: {
                                                            offset: 19,
                                                            column: 7,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 52,
                                                            column: 40,
                                                            line: 2,
                                                        },
                                                    },
                                                    url: 'https://st.yandex-team.ru/ABC-1234',
                                                    value: 'ABC-1234',
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
                                    offset: 54,
                                    column: 1,
                                    line: 4,
                                },
                                end: {
                                    offset: 67,
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
                                            offset: 54,
                                            column: 1,
                                            line: 4,
                                        },
                                        end: {
                                            offset: 66,
                                            column: 13,
                                            line: 4,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 56,
                                                    column: 3,
                                                    line: 4,
                                                },
                                                end: {
                                                    offset: 64,
                                                    column: 11,
                                                    line: 4,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 56,
                                                            column: 3,
                                                            line: 4,
                                                        },
                                                        end: {
                                                            offset: 64,
                                                            column: 11,
                                                            line: 4,
                                                        },
                                                    },
                                                    value: 'section2',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'list',
                            start: null,
                            loose: false,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 67,
                                    column: 1,
                                    line: 5,
                                },
                                end: {
                                    offset: 104,
                                    column: 38,
                                    line: 5,
                                },
                            },
                            children: [
                                {
                                    type: 'listItem',
                                    checked: false,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 67,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 104,
                                            column: 38,
                                            line: 5,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 73,
                                                    column: 7,
                                                    line: 5,
                                                },
                                                end: {
                                                    offset: 104,
                                                    column: 38,
                                                    line: 5,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'womTicket',
                                                    position: {
                                                        start: {
                                                            offset: 73,
                                                            column: 7,
                                                            line: 5,
                                                        },
                                                        end: {
                                                            offset: 104,
                                                            column: 38,
                                                            line: 5,
                                                        },
                                                    },
                                                    url: 'https://st.yandex-team.ru/ABC-1233',
                                                    value: 'ABC-1233',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'list',
                            start: null,
                            loose: false,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 107,
                                    column: 1,
                                    line: 8,
                                },
                                end: {
                                    offset: 169,
                                    column: 63,
                                    line: 8,
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
                                            offset: 107,
                                            column: 1,
                                            line: 8,
                                        },
                                        end: {
                                            offset: 169,
                                            column: 63,
                                            line: 8,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 109,
                                                    column: 3,
                                                    line: 8,
                                                },
                                                end: {
                                                    offset: 169,
                                                    column: 63,
                                                    line: 8,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'womTicket',
                                                    position: {
                                                        start: {
                                                            offset: 109,
                                                            column: 3,
                                                            line: 8,
                                                        },
                                                        end: {
                                                            offset: 140,
                                                            column: 34,
                                                            line: 8,
                                                        },
                                                    },
                                                    url: 'https://st.yandex-team.ru/ABC-1232',
                                                    value: 'ABC-1232',
                                                },
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 140,
                                                            column: 34,
                                                            line: 8,
                                                        },
                                                        end: {
                                                            offset: 169,
                                                            column: 63,
                                                            line: 8,
                                                        },
                                                    },
                                                    value: ' - должно закрыться после п.1',
                                                },
                                            ],
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'womHeading',
                            anchor: null,
                            depth: 3,
                            expandable: false,
                            position: {
                                start: {
                                    offset: 171,
                                    column: 1,
                                    line: 10,
                                },
                                end: {
                                    offset: 184,
                                    column: 14,
                                    line: 10,
                                },
                            },
                            section_local: 1,
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 176,
                                            column: 6,
                                            line: 10,
                                        },
                                        end: {
                                            offset: 184,
                                            column: 14,
                                            line: 10,
                                        },
                                    },
                                    value: 'section3',
                                },
                            ],
                        },
                        {
                            type: 'list',
                            start: null,
                            loose: false,
                            ordered: false,
                            position: {
                                start: {
                                    offset: 185,
                                    column: 1,
                                    line: 11,
                                },
                                end: {
                                    offset: 221,
                                    column: 37,
                                    line: 11,
                                },
                            },
                            children: [
                                {
                                    type: 'listItem',
                                    checked: false,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 185,
                                            column: 1,
                                            line: 11,
                                        },
                                        end: {
                                            offset: 221,
                                            column: 37,
                                            line: 11,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 190,
                                                    column: 6,
                                                    line: 11,
                                                },
                                                end: {
                                                    offset: 221,
                                                    column: 37,
                                                    line: 11,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'womTicket',
                                                    position: {
                                                        start: {
                                                            offset: 190,
                                                            column: 6,
                                                            line: 11,
                                                        },
                                                        end: {
                                                            offset: 221,
                                                            column: 37,
                                                            line: 11,
                                                        },
                                                    },
                                                    url: 'https://st.yandex-team.ru/ABC-1231',
                                                    value: 'ABC-1231',
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
