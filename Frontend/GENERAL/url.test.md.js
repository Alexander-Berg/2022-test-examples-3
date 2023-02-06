module.exports = [
    {
        group: 'link',
        tests: [
            {
                markup: '[added: https://ya.ru]',
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
                                    identifier: 'added: https://ya.ru',
                                    label: 'added: https://ya.ru',
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
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 1,
                                                    column: 2,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 8,
                                                    column: 9,
                                                    line: 1,
                                                },
                                            },
                                            value: 'added: ',
                                        },
                                        {
                                            type: 'link',
                                            title: null,
                                            position: {
                                                start: {
                                                    offset: 8,
                                                    column: 9,
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
                                                            offset: 8,
                                                            column: 9,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 21,
                                                            column: 22,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'https://ya.ru',
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
                markup: 'This should be a link: http://example.com/hello-world.',
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
                            column: 55,
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
                                    offset: 54,
                                    column: 55,
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
                                            offset: 23,
                                            column: 24,
                                            line: 1,
                                        },
                                    },
                                    value: 'This should be a link: ',
                                },
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 23,
                                            column: 24,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 53,
                                            column: 54,
                                            line: 1,
                                        },
                                    },
                                    url: 'http://example.com/hello-world',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 23,
                                                    column: 24,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 53,
                                                    column: 54,
                                                    line: 1,
                                                },
                                            },
                                            value: 'http://example.com/hello-world',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 53,
                                            column: 54,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 54,
                                            column: 55,
                                            line: 1,
                                        },
                                    },
                                    value: '.',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'Same for https: https://example.com/hello-world.',
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
                                            offset: 16,
                                            column: 17,
                                            line: 1,
                                        },
                                    },
                                    value: 'Same for https: ',
                                },
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 16,
                                            column: 17,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 47,
                                            column: 48,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://example.com/hello-world',
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
                                                    offset: 47,
                                                    column: 48,
                                                    line: 1,
                                                },
                                            },
                                            value: 'https://example.com/hello-world',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 47,
                                            column: 48,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 48,
                                            column: 49,
                                            line: 1,
                                        },
                                    },
                                    value: '.',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'Also, subdomain should be a part of the link (http://foo.example.com/(hello[world])).',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 85,
                            column: 86,
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
                                    offset: 85,
                                    column: 86,
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
                                            offset: 46,
                                            column: 47,
                                            line: 1,
                                        },
                                    },
                                    value: 'Also, subdomain should be a part of the link (',
                                },
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 46,
                                            column: 47,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 83,
                                            column: 84,
                                            line: 1,
                                        },
                                    },
                                    url: 'http://foo.example.com/(hello[world])',
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
                                                    offset: 83,
                                                    column: 84,
                                                    line: 1,
                                                },
                                            },
                                            value: 'http://foo.example.com/(hello[world])',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 83,
                                            column: 84,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 85,
                                            column: 86,
                                            line: 1,
                                        },
                                    },
                                    value: ').',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'So should this: mailto:foo@bar.com',
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
                            type: 'paragraph',
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
                                    type: 'text',
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
                                    value: 'So should this: ',
                                },
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 16,
                                            column: 17,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 34,
                                            column: 35,
                                            line: 1,
                                        },
                                    },
                                    url: 'mailto:foo@bar.com',
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
                                            value: 'foo@bar.com',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'ref:http://img.woofmd.net/i/logo95x37x8.png.',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 44,
                            column: 45,
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
                                    offset: 44,
                                    column: 45,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 43,
                                            column: 44,
                                            line: 1,
                                        },
                                    },
                                    ref: true,
                                    url: 'http://img.woofmd.net/i/logo95x37x8.png',
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
                                                    offset: 43,
                                                    column: 44,
                                                    line: 1,
                                                },
                                            },
                                            value: 'http://img.woofmd.net/i/logo95x37x8.png',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 43,
                                            column: 44,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 44,
                                            column: 45,
                                            line: 1,
                                        },
                                    },
                                    value: '.',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'ref:https://img.woofmd.net/i/logo95x37x8.png',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 44,
                            column: 45,
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
                                    offset: 44,
                                    column: 45,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 44,
                                            column: 45,
                                            line: 1,
                                        },
                                    },
                                    ref: true,
                                    url: 'https://img.woofmd.net/i/logo95x37x8.png',
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
                                                    offset: 44,
                                                    column: 45,
                                                    line: 1,
                                                },
                                            },
                                            value: 'https://img.woofmd.net/i/logo95x37x8.png',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'https://img.woofmd.net/i/logo95x37x8.png?foo=bar&amp;x=y',
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
                            column: 57,
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
                                    offset: 56,
                                    column: 57,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 56,
                                            column: 57,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://img.woofmd.net/i/logo95x37x8.png?foo=bar&x=y',
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
                                                    offset: 56,
                                                    column: 57,
                                                    line: 1,
                                                },
                                            },
                                            value: 'https://img.woofmd.net/i/logo95x37x8.png?foo=bar&x=y',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'file:/group/gods/dog.jpg',
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
                                    type: 'link',
                                    title: null,
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
                                    url: 'file:/group/gods/dog.jpg',
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
                                                    offset: 24,
                                                    column: 25,
                                                    line: 1,
                                                },
                                            },
                                            value: 'file:/group/gods/dog.jpg',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'file:https://jing.yandex-team.ru/files/ljql/shutter%202020-03-20%2012%3A51%3A18.png',
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
                            column: 84,
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
                                    offset: 83,
                                    column: 84,
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
                                            offset: 5,
                                            column: 6,
                                            line: 1,
                                        },
                                    },
                                    value: 'file:',
                                },
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 5,
                                            column: 6,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 83,
                                            column: 84,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://jing.yandex-team.ru/files/ljql/shutter%202020-03-20%2012%3A51%3A18.png',
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
                                                    offset: 83,
                                                    column: 84,
                                                    line: 1,
                                                },
                                            },
                                            value: 'https://jing.yandex-team.ru/files/ljql/shutter%202020-03-20%2012%3A51%3A18.png',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'Some text file:https://jing.yandex-team.ru/files/ljql/shutter%202020-03-20%2012%3A51%3A18.png',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 93,
                            column: 94,
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
                                    offset: 93,
                                    column: 94,
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
                                            offset: 15,
                                            column: 16,
                                            line: 1,
                                        },
                                    },
                                    value: 'Some text file:',
                                },
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 15,
                                            column: 16,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 93,
                                            column: 94,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://jing.yandex-team.ru/files/ljql/shutter%202020-03-20%2012%3A51%3A18.png',
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
                                                    offset: 93,
                                                    column: 94,
                                                    line: 1,
                                                },
                                            },
                                            value: 'https://jing.yandex-team.ru/files/ljql/shutter%202020-03-20%2012%3A51%3A18.png',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'MAILTO:mail@mail.ru',
                expect: {
                    type: 'root',
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
                                    offset: 19,
                                    column: 20,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'link',
                                    title: null,
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
                                    url: 'MAILTO:mail@mail.ru',
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
                                                    offset: 12,
                                                    column: 13,
                                                    line: 1,
                                                },
                                            },
                                            value: 'mail@mail.ru',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'some text MAILTO:mail@mail.ru',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 29,
                            column: 30,
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
                                    offset: 29,
                                    column: 30,
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
                                            offset: 29,
                                            column: 30,
                                            line: 1,
                                        },
                                    },
                                    value: 'some text MAILTO:mail@mail.ru',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'No at mailto:mailmail.ru',
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
                                            offset: 24,
                                            column: 25,
                                            line: 1,
                                        },
                                    },
                                    value: 'No at mailto:mailmail.ru',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'ref:file:/users/masloval/mobiletest.jpg',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 39,
                            column: 40,
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
                                    offset: 39,
                                    column: 40,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 39,
                                            column: 40,
                                            line: 1,
                                        },
                                    },
                                    ref: true,
                                    url: 'file:/users/masloval/mobiletest.jpg',
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
                                                    offset: 39,
                                                    column: 40,
                                                    line: 1,
                                                },
                                            },
                                            value: 'file:/users/masloval/mobiletest.jpg',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'ref:file:https://jing.yandex-team.ru/files/ljql/shutter%202020-03-20%2012%3A51%3A18.png',
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
                            column: 88,
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
                                    offset: 87,
                                    column: 88,
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
                                            offset: 9,
                                            column: 10,
                                            line: 1,
                                        },
                                    },
                                    value: 'ref:file:',
                                },
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 9,
                                            column: 10,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 87,
                                            column: 88,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://jing.yandex-team.ru/files/ljql/shutter%202020-03-20%2012%3A51%3A18.png',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 9,
                                                    column: 10,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 87,
                                                    column: 88,
                                                    line: 1,
                                                },
                                            },
                                            value: 'https://jing.yandex-team.ru/files/ljql/shutter%202020-03-20%2012%3A51%3A18.png',
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
