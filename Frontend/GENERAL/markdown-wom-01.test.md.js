module.exports = [
    {
        group: 'womMarkdown %%(md)%%',
        tests: [
            {
                markup: '%%(md)\nБлок текста\n%%\n',
                title: 'Блочный враппер РВЕТ параграф',
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
                                    offset: 21,
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
                                            offset: 18,
                                            column: 12,
                                            line: 2,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 7,
                                                    column: 1,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 18,
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
                markup: '1. test\n%%(md)\nБлок текста\n%%\n1. test\n',
                title: 'Блочный враппер РВЕТ список',
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
                            type: 'womMarkdown',
                            attributes: {
                            },
                            format: 'md',
                            inline: false,
                            position: {
                                start: {
                                    offset: 8,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 29,
                                    column: 3,
                                    line: 4,
                                },
                            },
                            children: [
                                {
                                    type: 'paragraph',
                                    position: {
                                        start: {
                                            offset: 15,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 26,
                                            column: 12,
                                            line: 3,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 15,
                                                    column: 1,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 26,
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
                                    offset: 30,
                                    column: 1,
                                    line: 5,
                                },
                                end: {
                                    offset: 37,
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
                                            offset: 30,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 37,
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
                                                    offset: 33,
                                                    column: 4,
                                                    line: 5,
                                                },
                                                end: {
                                                    offset: 37,
                                                    column: 8,
                                                    line: 5,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 33,
                                                            column: 4,
                                                            line: 5,
                                                        },
                                                        end: {
                                                            offset: 37,
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
                markup: 'параграф %%(md) Блок текста %%\n',
                title: 'Строчный враппер НЕ РВЕТ параграф',
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
                                    offset: 31,
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
                                            offset: 9,
                                            column: 10,
                                            line: 1,
                                        },
                                    },
                                    value: 'параграф ',
                                },
                                {
                                    type: 'womMarkdown',
                                    attributes: {
                                    },
                                    format: 'md',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 9,
                                            column: 10,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 30,
                                            column: 31,
                                            line: 1,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 16,
                                                    column: 17,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 27,
                                                    column: 28,
                                                    line: 1,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 16,
                                                            column: 17,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 27,
                                                            column: 28,
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
                markup: 'параграф %%(md) Блок текста,\nдлинного,\nс переносами\n%%\n',
                title: 'Строчный враппер с переносами НЕ РВЕТ параграф',
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
                                    offset: 55,
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
                                            offset: 9,
                                            column: 10,
                                            line: 1,
                                        },
                                    },
                                    value: 'параграф ',
                                },
                                {
                                    type: 'womMarkdown',
                                    attributes: {
                                    },
                                    format: 'md',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 9,
                                            column: 10,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 54,
                                            column: 3,
                                            line: 4,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 16,
                                                    column: 17,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 51,
                                                    column: 13,
                                                    line: 3,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 16,
                                                            column: 17,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 51,
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
                markup: '1. test\n%%(md) Блок текста %%\n',
                title: 'Строчный враппер НЕ РВЕТ список',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 30,
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
                                    offset: 29,
                                    column: 22,
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
                                            offset: 29,
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
                                                    offset: 3,
                                                    column: 4,
                                                    line: 1,
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
                                                    type: 'womMarkdown',
                                                    attributes: {
                                                    },
                                                    format: 'md',
                                                    inline: true,
                                                    position: {
                                                        start: {
                                                            offset: 8,
                                                            column: 1,
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
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 15,
                                                                    column: 8,
                                                                    line: 2,
                                                                },
                                                                end: {
                                                                    offset: 26,
                                                                    column: 19,
                                                                    line: 2,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'text',
                                                                    position: {
                                                                        start: {
                                                                            offset: 15,
                                                                            column: 8,
                                                                            line: 2,
                                                                        },
                                                                        end: {
                                                                            offset: 26,
                                                                            column: 19,
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
                markup: '1. test\n%%(md) Блок текста\nи тут продолжение,\nи тут тоже\n%%\n',
                title: 'Строчный враппер с переносами НЕ РВЕТ список',
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
                                    offset: 59,
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
                                            offset: 59,
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
                                                    offset: 59,
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
                                                    type: 'womMarkdown',
                                                    attributes: {
                                                    },
                                                    format: 'md',
                                                    inline: true,
                                                    position: {
                                                        start: {
                                                            offset: 8,
                                                            column: 1,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 59,
                                                            column: 3,
                                                            line: 5,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 15,
                                                                    column: 8,
                                                                    line: 2,
                                                                },
                                                                end: {
                                                                    offset: 56,
                                                                    column: 11,
                                                                    line: 4,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'text',
                                                                    position: {
                                                                        start: {
                                                                            offset: 15,
                                                                            column: 8,
                                                                            line: 2,
                                                                        },
                                                                        end: {
                                                                            offset: 56,
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
            {
                markup: 'параграф %%(wacko)test\n   {{iframe}}\n%%\n',
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
                                    offset: 40,
                                    column: 1,
                                    line: 4,
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
                                            offset: 9,
                                            column: 10,
                                            line: 1,
                                        },
                                    },
                                    value: 'параграф ',
                                },
                                {
                                    type: 'womMarkdown',
                                    attributes: {
                                    },
                                    format: 'wacko',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 9,
                                            column: 10,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 39,
                                            column: 3,
                                            line: 3,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 18,
                                                    column: 19,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 23,
                                                    column: 1,
                                                    line: 2,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 18,
                                                            column: 19,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 22,
                                                            column: 23,
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
                                            name: 'iframe',
                                            params: {
                                            },
                                            position: {
                                                start: {
                                                    offset: 23,
                                                    column: 1,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 36,
                                                    column: 14,
                                                    line: 2,
                                                },
                                            },
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'параграф %%(wacko foo=bar)((https://ya.ru ya))%%\n',
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
                                    offset: 49,
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
                                            offset: 9,
                                            column: 10,
                                            line: 1,
                                        },
                                    },
                                    value: 'параграф ',
                                },
                                {
                                    type: 'womMarkdown',
                                    attributes: {
                                        foo: 'bar',
                                    },
                                    format: 'wacko',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 9,
                                            column: 10,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 48,
                                            column: 49,
                                            line: 1,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 26,
                                                    column: 27,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 46,
                                                    column: 47,
                                                    line: 1,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'womLink',
                                                    brackets: false,
                                                    position: {
                                                        start: {
                                                            offset: 26,
                                                            column: 27,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 46,
                                                            column: 47,
                                                            line: 1,
                                                        },
                                                    },
                                                    url: 'https://ya.ru',
                                                    children: [
                                                        {
                                                            type: 'text',
                                                            position: {
                                                                start: {
                                                                    offset: 42,
                                                                    column: 43,
                                                                    line: 1,
                                                                },
                                                                end: {
                                                                    offset: 44,
                                                                    column: 45,
                                                                    line: 1,
                                                                },
                                                            },
                                                            value: 'ya',
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
