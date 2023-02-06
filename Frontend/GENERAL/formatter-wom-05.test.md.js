module.exports = [
    {
        group: 'womFormatter %%%%',
        tests: [
            {
                markup: 'foo %%%\n',
                title: 'Форматтер не парсится',
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
                                    offset: 8,
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
                                            offset: 7,
                                            column: 8,
                                            line: 1,
                                        },
                                    },
                                    value: 'foo %%%',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'foo %%x%%\n',
                title: 'foo %%x%%',
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
                                    value: 'foo ',
                                },
                                {
                                    type: 'womFormatter',
                                    inline: true,
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
                                    value: 'x',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'foo x%%x%%\n',
                title: 'foo x%%x%%',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 11,
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
                                    offset: 11,
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
                                            offset: 5,
                                            column: 6,
                                            line: 1,
                                        },
                                    },
                                    value: 'foo x',
                                },
                                {
                                    type: 'womFormatter',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 5,
                                            column: 6,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                    },
                                    value: 'x',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'foo \\%%%x%%\n',
                title: 'foo \\%%%x%%',
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
                                    offset: 12,
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
                                    value: 'foo ',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 4,
                                            column: 5,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 6,
                                            column: 7,
                                            line: 1,
                                        },
                                    },
                                    value: '%',
                                },
                                {
                                    type: 'womFormatter',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 6,
                                            column: 7,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 11,
                                            column: 12,
                                            line: 1,
                                        },
                                    },
                                    value: 'x',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'foo \\%%x%%\n',
                title: 'foo \\%%x%%',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 11,
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
                                    offset: 11,
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
                                    value: 'foo ',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 4,
                                            column: 5,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 6,
                                            column: 7,
                                            line: 1,
                                        },
                                    },
                                    value: '%',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 6,
                                            column: 7,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                    },
                                    value: '%x%%',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'foo %%\\%%%\n',
                title: 'foo %%\\%%%',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 11,
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
                                    offset: 11,
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
                                    value: 'foo ',
                                },
                                {
                                    type: 'womFormatter',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 4,
                                            column: 5,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                    },
                                    value: '\\%',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'foo %%(math)X\\%%%\n',
                title: 'foo %%(math)X\\%%%',
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
                                    value: 'foo ',
                                },
                                {
                                    type: 'womFormatter',
                                    attributes: {
                                    },
                                    format: 'math',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 4,
                                            column: 5,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 17,
                                            column: 18,
                                            line: 1,
                                        },
                                    },
                                    value: 'X\\%',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'foo %%(math)X~%%%\n',
                title: 'foo %%(math)X~%%%',
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
                                    value: 'foo ',
                                },
                                {
                                    type: 'womFormatter',
                                    attributes: {
                                    },
                                    format: 'math',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 4,
                                            column: 5,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 17,
                                            column: 18,
                                            line: 1,
                                        },
                                    },
                                    value: 'X~%',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '%%(math)x%%\n',
                title: '%%(math)x%%',
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
                                    offset: 12,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womFormatter',
                                    attributes: {
                                    },
                                    format: 'math',
                                    inline: true,
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
                                    value: 'x',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'x%%x%%\n',
                title: 'x%%x%%',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 7,
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
                                    offset: 7,
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
                                    value: 'x',
                                },
                                {
                                    type: 'womFormatter',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 1,
                                            column: 2,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 6,
                                            column: 7,
                                            line: 1,
                                        },
                                    },
                                    value: 'x',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '%%\nx\n%%\n',
                title: '%%\\nx\\n%%',
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
                                    offset: 7,
                                    column: 3,
                                    line: 3,
                                },
                            },
                            value: 'x\n',
                        },
                    ],
                },
            },
            {
                markup: '%%x%%\n',
                title: '%%x%%',
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
                                    offset: 6,
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
                                            offset: 5,
                                            column: 6,
                                            line: 1,
                                        },
                                    },
                                    value: 'x',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '%%x%%\n%%x%%\n',
                title: '%%x%%\\n%%x%%',
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
                                    offset: 12,
                                    column: 1,
                                    line: 3,
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
                                            offset: 5,
                                            column: 6,
                                            line: 1,
                                        },
                                    },
                                    value: 'x',
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
                                            offset: 6,
                                            column: 1,
                                            line: 2,
                                        },
                                    },
                                    value: '\n',
                                },
                                {
                                    type: 'womFormatter',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 6,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 11,
                                            column: 6,
                                            line: 2,
                                        },
                                    },
                                    value: 'x',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '%%(md)\n*test*\n%%\n',
                title: '%%(md)\\n*test*\\n%%',
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
                            column: 1,
                            line: 4,
                        },
                    },
                    children: [
                        {
                            type: 'womMarkdown',
                            attributes: {
                            },
                            format: 'md',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 16,
                                    column: 3,
                                    line: 3,
                                },
                            },
                            children: [
                                {
                                    type: 'paragraph',
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
                markup: '%%(md)%%\n',
                title: '%%(md)%%',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 9,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'womMarkdown',
                            attributes: {
                            },
                            format: 'md',
                            inline: false,
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
                            ],
                        },
                    ],
                },
            },
        ],
    },
];
