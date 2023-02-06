module.exports = [
    {
        group: 'womAction {{}}',
        tests: [
            {
                markup: 'blablaska {{linkstree root=HomePage }}',
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
                            column: 39,
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
                                    offset: 38,
                                    column: 39,
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
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                    },
                                    value: 'blablaska ',
                                },
                                {
                                    type: 'womAction',
                                    inline: true,
                                    name: 'linkstree',
                                    params: {
                                        root: 'HomePage',
                                    },
                                    position: {
                                        start: {
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 38,
                                            column: 39,
                                            line: 1,
                                        },
                                    },
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '   {{iframe}}',
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
                            column: 14,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'womAction',
                            inline: false,
                            name: 'iframe',
                            params: {
                            },
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
                        },
                    ],
                },
            },
            {
                markup: '{{\nGrid\npage="/test"\nreadonly="1"\n}}',
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
                            column: 3,
                            line: 5,
                        },
                    },
                    children: [
                        {
                            type: 'womAction',
                            inline: false,
                            name: 'grid',
                            params: {
                                page: '/test',
                                readonly: '1',
                            },
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 36,
                                    column: 3,
                                    line: 5,
                                },
                            },
                        },
                    ],
                },
            },
            {
                markup: '{{\ngrid\npage="/test"\nreadonly="1"\n}}',
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
                            column: 3,
                            line: 5,
                        },
                    },
                    children: [
                        {
                            type: 'womAction',
                            inline: false,
                            name: 'grid',
                            params: {
                                page: '/test',
                                readonly: '1',
                            },
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 36,
                                    column: 3,
                                    line: 5,
                                },
                            },
                        },
                    ],
                },
            },
            {
                markup: '{{grid\npage="/test"\nreadonly="1"\n}}',
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
                            column: 3,
                            line: 4,
                        },
                    },
                    children: [
                        {
                            type: 'womAction',
                            inline: false,
                            name: 'grid',
                            params: {
                                page: '/test',
                                readonly: '1',
                            },
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 35,
                                    column: 3,
                                    line: 4,
                                },
                            },
                        },
                    ],
                },
            },
            {
                markup: '{{iframe src="https://wiki.woofmd-team.ru" frameborder=0 width=700px height=600px scrolling=no}}',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 96,
                            column: 97,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'womAction',
                            inline: false,
                            name: 'iframe',
                            params: {
                                frameborder: '0',
                                height: '600px',
                                scrolling: 'no',
                                src: 'https://wiki.woofmd-team.ru',
                                width: '700px',
                            },
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 96,
                                    column: 97,
                                    line: 1,
                                },
                            },
                        },
                    ],
                },
            },
            {
                markup: '{{linkstree}}',
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
                            column: 14,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'womAction',
                            inline: false,
                            name: 'linkstree',
                            params: {
                            },
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
                        },
                    ],
                },
            },
            {
                markup: '{{ linkstree root=HomePage levels }}',
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
                            column: 37,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'womAction',
                            inline: false,
                            name: 'linkstree',
                            params: {
                                levels: null,
                                root: 'HomePage',
                            },
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 36,
                                    column: 37,
                                    line: 1,
                                },
                            },
                        },
                    ],
                },
            },
            {
                markup: '{{linkstree root=HomePage levels}}',
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
                            type: 'womAction',
                            inline: false,
                            name: 'linkstree',
                            params: {
                                levels: null,
                                root: 'HomePage',
                            },
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
                        },
                    ],
                },
            },
            {
                markup: '{{a}}{{anchor}}',
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
                            column: 16,
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
                                    offset: 15,
                                    column: 16,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womAction',
                                    inline: true,
                                    name: 'a',
                                    params: {
                                    },
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 5,
                                            column: 6,
                                            line: 1,
                                        },
                                    },
                                },
                                {
                                    type: 'womAction',
                                    inline: true,
                                    name: 'anchor',
                                    params: {
                                    },
                                    position: {
                                        start: {
                                            offset: 5,
                                            column: 6,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 15,
                                            column: 16,
                                            line: 1,
                                        },
                                    },
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '{{a}} text {{anchor}}',
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
                            column: 22,
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
                                    offset: 21,
                                    column: 22,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womAction',
                                    inline: true,
                                    name: 'a',
                                    params: {
                                    },
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 5,
                                            column: 6,
                                            line: 1,
                                        },
                                    },
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 5,
                                            column: 6,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 11,
                                            column: 12,
                                            line: 1,
                                        },
                                    },
                                    value: ' text ',
                                },
                                {
                                    type: 'womAction',
                                    inline: true,
                                    name: 'anchor',
                                    params: {
                                    },
                                    position: {
                                        start: {
                                            offset: 11,
                                            column: 12,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 21,
                                            column: 22,
                                            line: 1,
                                        },
                                    },
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'test\n{{a name=x}}\ntest',
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
                            column: 5,
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
                                    value: 'test',
                                },
                            ],
                        },
                        {
                            type: 'womAction',
                            inline: false,
                            name: 'a',
                            params: {
                                name: 'x',
                            },
                            position: {
                                start: {
                                    offset: 5,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 17,
                                    column: 13,
                                    line: 2,
                                },
                            },
                        },
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 18,
                                    column: 1,
                                    line: 3,
                                },
                                end: {
                                    offset: 22,
                                    column: 5,
                                    line: 3,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 18,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 22,
                                            column: 5,
                                            line: 3,
                                        },
                                    },
                                    value: 'test',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'test\n\n{{a name=x}}\n\ntest',
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
                            column: 5,
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
                                    value: 'test',
                                },
                            ],
                        },
                        {
                            type: 'womAction',
                            inline: false,
                            name: 'a',
                            params: {
                                name: 'x',
                            },
                            position: {
                                start: {
                                    offset: 6,
                                    column: 1,
                                    line: 3,
                                },
                                end: {
                                    offset: 18,
                                    column: 13,
                                    line: 3,
                                },
                            },
                        },
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 20,
                                    column: 1,
                                    line: 5,
                                },
                                end: {
                                    offset: 24,
                                    column: 5,
                                    line: 5,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 20,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 24,
                                            column: 5,
                                            line: 5,
                                        },
                                    },
                                    value: 'test',
                                },
                            ],
                        },
                    ],
                },
            },
        ],
    },
];
