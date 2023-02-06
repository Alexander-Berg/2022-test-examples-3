module.exports = [
    {
        group: 'womLink',
        tests: [
            {
                markup: '[[Ya](https://ya.ru)]',
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
                                    value: '[',
                                },
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 1,
                                            column: 2,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 20,
                                            column: 21,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://ya.ru',
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
                                                    offset: 4,
                                                    column: 5,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Ya',
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
                                            offset: 21,
                                            column: 22,
                                            line: 1,
                                        },
                                    },
                                    value: ']',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '[[[https://ya.ru Ya]]]',
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
                            column: 23,
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
                                    value: '[',
                                },
                                {
                                    type: 'womLink',
                                    brackets: true,
                                    position: {
                                        start: {
                                            offset: 1,
                                            column: 2,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 21,
                                            column: 22,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://ya.ru',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 17,
                                                    column: 18,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 19,
                                                    column: 20,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Ya',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 21,
                                            column: 22,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 22,
                                            column: 23,
                                            line: 1,
                                        },
                                    },
                                    value: ']',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '[[[[https://ya.ru Ya]]]]',
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
                            column: 25,
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
                                    offset: 24,
                                    column: 25,
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
                                            offset: 2,
                                            column: 3,
                                            line: 1,
                                        },
                                    },
                                    value: '[[',
                                },
                                {
                                    type: 'womLink',
                                    brackets: true,
                                    position: {
                                        start: {
                                            offset: 2,
                                            column: 3,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 22,
                                            column: 23,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://ya.ru',
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
                                                    offset: 20,
                                                    column: 21,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Ya',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 22,
                                            column: 23,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 24,
                                            column: 25,
                                            line: 1,
                                        },
                                    },
                                    value: ']]',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '[((https://ya.ru Ya))]',
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
                            column: 23,
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
                                    offset: 22,
                                    column: 23,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'linkReference',
                                    identifier: '((https://ya.ru ya))',
                                    label: '((https://ya.ru Ya))',
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
                                    referenceType: 'shortcut',
                                    children: [
                                        {
                                            type: 'womLink',
                                            brackets: false,
                                            position: {
                                                start: {
                                                    offset: 1,
                                                    column: 2,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 21,
                                                    column: 22,
                                                    line: 1,
                                                },
                                            },
                                            url: 'https://ya.ru',
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 17,
                                                            column: 18,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 19,
                                                            column: 20,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'Ya',
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
                markup: '[[((https://ya.ru Ya))]]',
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
                            column: 25,
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
                                    offset: 24,
                                    column: 25,
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
                                            offset: 1,
                                            column: 2,
                                            line: 1,
                                        },
                                    },
                                    value: '[',
                                },
                                {
                                    type: 'linkReference',
                                    identifier: '((https://ya.ru ya))',
                                    label: '((https://ya.ru Ya))',
                                    position: {
                                        start: {
                                            offset: 1,
                                            column: 2,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 23,
                                            column: 24,
                                            line: 1,
                                        },
                                    },
                                    referenceType: 'shortcut',
                                    children: [
                                        {
                                            type: 'womLink',
                                            brackets: false,
                                            position: {
                                                start: {
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 22,
                                                    column: 23,
                                                    line: 1,
                                                },
                                            },
                                            url: 'https://ya.ru',
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
                                                            offset: 20,
                                                            column: 21,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'Ya',
                                                },
                                            ],
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 23,
                                            column: 24,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 24,
                                            column: 25,
                                            line: 1,
                                        },
                                    },
                                    value: ']',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '[[test](https://foo.bar)]\n[[https://ya.ru test]]',
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
                            column: 23,
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
                                    offset: 48,
                                    column: 23,
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
                                    value: '[',
                                },
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 1,
                                            column: 2,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 24,
                                            column: 25,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://foo.bar',
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
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 24,
                                            column: 25,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 26,
                                            column: 1,
                                            line: 2,
                                        },
                                    },
                                    value: ']\n',
                                },
                                {
                                    type: 'womLink',
                                    brackets: true,
                                    position: {
                                        start: {
                                            offset: 26,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 48,
                                            column: 23,
                                            line: 2,
                                        },
                                    },
                                    url: 'https://ya.ru',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 42,
                                                    column: 17,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 46,
                                                    column: 21,
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
                markup: '[[test](https://foo.bar)] [[https://ya.ru test]]',
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
                            column: 49,
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
                                    offset: 48,
                                    column: 49,
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
                                            offset: 1,
                                            column: 2,
                                            line: 1,
                                        },
                                    },
                                    value: '[',
                                },
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 1,
                                            column: 2,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 24,
                                            column: 25,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://foo.bar',
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
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 24,
                                            column: 25,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 26,
                                            column: 27,
                                            line: 1,
                                        },
                                    },
                                    value: '] ',
                                },
                                {
                                    type: 'womLink',
                                    brackets: true,
                                    position: {
                                        start: {
                                            offset: 26,
                                            column: 27,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 48,
                                            column: 49,
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
                                                    offset: 46,
                                                    column: 47,
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
            },
            {
                markup: '[[test](https://foo.bar)]\n[[((https://ya.ru test))]]',
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
                            column: 27,
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
                                    offset: 52,
                                    column: 27,
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
                                    value: '[',
                                },
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 1,
                                            column: 2,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 24,
                                            column: 25,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://foo.bar',
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
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 24,
                                            column: 25,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 27,
                                            column: 2,
                                            line: 2,
                                        },
                                    },
                                    value: ']\n[',
                                },
                                {
                                    type: 'linkReference',
                                    identifier: '((https://ya.ru test))',
                                    label: '((https://ya.ru test))',
                                    position: {
                                        start: {
                                            offset: 27,
                                            column: 2,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 51,
                                            column: 26,
                                            line: 2,
                                        },
                                    },
                                    referenceType: 'shortcut',
                                    children: [
                                        {
                                            type: 'womLink',
                                            brackets: false,
                                            position: {
                                                start: {
                                                    offset: 28,
                                                    column: 3,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 50,
                                                    column: 25,
                                                    line: 2,
                                                },
                                            },
                                            url: 'https://ya.ru',
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 44,
                                                            column: 19,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 48,
                                                            column: 23,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'test',
                                                },
                                            ],
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 51,
                                            column: 26,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 52,
                                            column: 27,
                                            line: 2,
                                        },
                                    },
                                    value: ']',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '[[test](https://foo.bar)]\n(([[https://ya.ru test]]))',
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
                            column: 27,
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
                                    offset: 52,
                                    column: 27,
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
                                    value: '[',
                                },
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 1,
                                            column: 2,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 24,
                                            column: 25,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://foo.bar',
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
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 24,
                                            column: 25,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 28,
                                            column: 3,
                                            line: 2,
                                        },
                                    },
                                    value: ']\n((',
                                },
                                {
                                    type: 'womLink',
                                    brackets: true,
                                    position: {
                                        start: {
                                            offset: 28,
                                            column: 3,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 50,
                                            column: 25,
                                            line: 2,
                                        },
                                    },
                                    url: 'https://ya.ru',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 44,
                                                    column: 19,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 48,
                                                    column: 23,
                                                    line: 2,
                                                },
                                            },
                                            value: 'test',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 50,
                                            column: 25,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 52,
                                            column: 27,
                                            line: 2,
                                        },
                                    },
                                    value: '))',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'inline <a >\n\n((https://ya.ru))\n',
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
                                            offset: 7,
                                            column: 8,
                                            line: 1,
                                        },
                                    },
                                    value: 'inline ',
                                },
                                {
                                    type: 'html',
                                    position: {
                                        start: {
                                            offset: 7,
                                            column: 8,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 11,
                                            column: 12,
                                            line: 1,
                                        },
                                    },
                                    value: '<a >',
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
                                    offset: 31,
                                    column: 1,
                                    line: 4,
                                },
                            },
                            children: [
                                {
                                    type: 'womLink',
                                    brackets: false,
                                    position: {
                                        start: {
                                            offset: 13,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 30,
                                            column: 18,
                                            line: 3,
                                        },
                                    },
                                    url: 'https://ya.ru',
                                    children: [
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '((https://ya.ru Ya\n))',
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
                            column: 3,
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
                                    column: 3,
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
                                    value: '((',
                                },
                                {
                                    type: 'link',
                                    title: null,
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
                                    url: 'https://ya.ru',
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
                                                    column: 16,
                                                    line: 1,
                                                },
                                            },
                                            value: 'https://ya.ru',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 15,
                                            column: 16,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 21,
                                            column: 3,
                                            line: 2,
                                        },
                                    },
                                    value: ' Ya\n))',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '((https://st.yandex-team.ru/EXPERIMENTS-45315 Отключение редиров на десктопах\u2028))',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 80,
                            column: 81,
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
                                    offset: 80,
                                    column: 81,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womLink',
                                    brackets: false,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 80,
                                            column: 81,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://st.yandex-team.ru/EXPERIMENTS-45315',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 46,
                                                    column: 47,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 78,
                                                    column: 79,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Отключение редиров на десктопах\u2028',
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
