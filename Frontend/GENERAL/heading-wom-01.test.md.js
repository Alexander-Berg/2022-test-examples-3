module.exports = [
    {
        group: 'womHeading',
        tests: [
            {
                markup: '====Статистика попапа "Окно заблокировано"\n<["client_id": "popup"]>',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 67,
                            column: 25,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'womHeading',
                            anchor: null,
                            depth: 3,
                            expandable: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 42,
                                    column: 43,
                                    line: 1,
                                },
                            },
                            section_local: 1,
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
                                            offset: 42,
                                            column: 43,
                                            line: 1,
                                        },
                                    },
                                    value: 'Статистика попапа "Окно заблокировано"',
                                },
                            ],
                        },
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 43,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 67,
                                    column: 25,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womBlockquote',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 43,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 67,
                                            column: 25,
                                            line: 2,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 45,
                                                    column: 3,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 65,
                                                    column: 23,
                                                    line: 2,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 45,
                                                            column: 3,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 65,
                                                            column: 23,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: '"client_id": "popup"',
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
                markup: '==test==',
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
                            type: 'womHeading',
                            anchor: null,
                            depth: 1,
                            expandable: false,
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
                            section_local: 1,
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
            },
            {
                markup: '==test==test==',
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
                            column: 15,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'womHeading',
                            anchor: null,
                            depth: 1,
                            expandable: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 14,
                                    column: 15,
                                    line: 1,
                                },
                            },
                            section_local: 1,
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
                                    value: 'test==test',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: "==<#<span style='color: darkred'>Уровень 1</span>#>==",
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 53,
                            column: 54,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'womHeading',
                            anchor: null,
                            depth: 1,
                            expandable: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 53,
                                    column: 54,
                                    line: 1,
                                },
                            },
                            section_local: 1,
                            children: [
                                {
                                    type: 'womHtml',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 2,
                                            column: 3,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 51,
                                            column: 52,
                                            line: 1,
                                        },
                                    },
                                    value: "<span style='color: darkred'>Уровень 1</span>",
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '#|||== {[1]}|||#',
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
                            column: 17,
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
                                    offset: 16,
                                    column: 17,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womTable',
                                    inline: true,
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
                                    children: [
                                        {
                                            type: 'womTableRow',
                                            position: {
                                                start: {
                                                    offset: 2,
                                                    column: 3,
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
                                                    type: 'womTableCell',
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
                                                    children: [
                                                        {
                                                            type: 'womHeading',
                                                            anchor: null,
                                                            depth: 1,
                                                            expandable: false,
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
                                                            section_local: 1,
                                                            children: [
                                                                {
                                                                    type: 'womBlock',
                                                                    inline: true,
                                                                    position: {
                                                                        start: {
                                                                            offset: 7,
                                                                            column: 8,
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
                                                                                    offset: 9,
                                                                                    column: 10,
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
                                                                                            offset: 9,
                                                                                            column: 10,
                                                                                            line: 1,
                                                                                        },
                                                                                        end: {
                                                                                            offset: 10,
                                                                                            column: 11,
                                                                                            line: 1,
                                                                                        },
                                                                                    },
                                                                                    value: '1',
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
                markup: '= Не заголовок',
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
                            column: 15,
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
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 14,
                                            column: 15,
                                            line: 1,
                                        },
                                    },
                                    value: '= Не заголовок',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '== Большой заголовок',
                expect: {
                    type: 'root',
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
                        {
                            type: 'womHeading',
                            anchor: null,
                            depth: 1,
                            expandable: false,
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
                            section_local: 1,
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
                                            offset: 20,
                                            column: 21,
                                            line: 1,
                                        },
                                    },
                                    value: 'Большой заголовок',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '==+ Большой раскрывающийся заголовок',
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
                            type: 'womHeading',
                            anchor: null,
                            depth: 1,
                            expandable: true,
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
                            section_local: 1,
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
                                            offset: 36,
                                            column: 37,
                                            line: 1,
                                        },
                                    },
                                    value: 'Большой раскрывающийся заголовок',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '======== Не заголовок',
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
                                            offset: 21,
                                            column: 22,
                                            line: 1,
                                        },
                                    },
                                    value: '======== Не заголовок',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '==(intro) Введение',
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
                            column: 19,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'womHeading',
                            anchor: 'intro',
                            depth: 1,
                            expandable: false,
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
                            section_local: 1,
                            children: [
                                {
                                    type: 'text',
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
                                    value: 'Введение',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '==+(intro) Введение',
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
                            type: 'womHeading',
                            anchor: 'intro',
                            depth: 1,
                            expandable: true,
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
                            section_local: 1,
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 11,
                                            column: 12,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 19,
                                            column: 20,
                                            line: 1,
                                        },
                                    },
                                    value: 'Введение',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '==(intro)+ Введение',
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
                            type: 'womHeading',
                            anchor: 'intro',
                            depth: 1,
                            expandable: true,
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
                            section_local: 1,
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 11,
                                            column: 12,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 19,
                                            column: 20,
                                            line: 1,
                                        },
                                    },
                                    value: 'Введение',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '==() Введение',
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
                            type: 'womHeading',
                            anchor: '',
                            depth: 1,
                            expandable: false,
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
                            section_local: 1,
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
                                    value: 'Введение',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '== Библиотека %%глазировка%%',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
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
                            type: 'womHeading',
                            anchor: null,
                            depth: 1,
                            expandable: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 28,
                                    column: 29,
                                    line: 1,
                                },
                            },
                            section_local: 1,
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
                                    value: 'Библиотека ',
                                },
                                {
                                    type: 'womFormatter',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 14,
                                            column: 15,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 28,
                                            column: 29,
                                            line: 1,
                                        },
                                    },
                                    value: 'глазировка',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '===((https://ya.ru Yandex))',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
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
                            type: 'womHeading',
                            anchor: null,
                            depth: 2,
                            expandable: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 27,
                                    column: 28,
                                    line: 1,
                                },
                            },
                            section_local: 1,
                            children: [
                                {
                                    type: 'womLink',
                                    brackets: false,
                                    position: {
                                        start: {
                                            offset: 3,
                                            column: 4,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 27,
                                            column: 28,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://ya.ru',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 19,
                                                    column: 20,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 25,
                                                    column: 26,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Yandex',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '=== Заголовок поменьше',
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
                            type: 'womHeading',
                            anchor: null,
                            depth: 2,
                            expandable: false,
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
                            section_local: 1,
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
                                            offset: 22,
                                            column: 23,
                                            line: 1,
                                        },
                                    },
                                    value: 'Заголовок поменьше',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '==== Средний заголовок',
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
                            type: 'womHeading',
                            anchor: null,
                            depth: 3,
                            expandable: false,
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
                            section_local: 1,
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
                                            offset: 22,
                                            column: 23,
                                            line: 1,
                                        },
                                    },
                                    value: 'Средний заголовок',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '===== Маленький заголовок',
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
                            column: 26,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'womHeading',
                            anchor: null,
                            depth: 4,
                            expandable: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 25,
                                    column: 26,
                                    line: 1,
                                },
                            },
                            section_local: 1,
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
                                            offset: 25,
                                            column: 26,
                                            line: 1,
                                        },
                                    },
                                    value: 'Маленький заголовок',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '====== Ну совсем маленький заголовок',
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
                            type: 'womHeading',
                            anchor: null,
                            depth: 5,
                            expandable: false,
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
                            section_local: 1,
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 7,
                                            column: 8,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 36,
                                            column: 37,
                                            line: 1,
                                        },
                                    },
                                    value: 'Ну совсем маленький заголовок',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '======= Меньше некуда заголовок',
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
                            column: 32,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'womHeading',
                            anchor: null,
                            depth: 6,
                            expandable: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 31,
                                    column: 32,
                                    line: 1,
                                },
                            },
                            section_local: 1,
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
                                            offset: 31,
                                            column: 32,
                                            line: 1,
                                        },
                                    },
                                    value: 'Меньше некуда заголовок',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '======= Меньше некуда заголовок ====================',
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
                            column: 53,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'womHeading',
                            anchor: null,
                            depth: 6,
                            expandable: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 52,
                                    column: 53,
                                    line: 1,
                                },
                            },
                            section_local: 1,
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
                                            offset: 31,
                                            column: 32,
                                            line: 1,
                                        },
                                    },
                                    value: 'Меньше некуда заголовок',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '=======+ Меньше некуда раскрывающийся заголовок',
                expect: {
                    type: 'root',
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
                    children: [
                        {
                            type: 'womHeading',
                            anchor: null,
                            depth: 6,
                            expandable: true,
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
                            section_local: 1,
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
                                            offset: 47,
                                            column: 48,
                                            line: 1,
                                        },
                                    },
                                    value: 'Меньше некуда раскрывающийся заголовок',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '== H1\n%%(wacko)\n=== H2\n=== H2\n%%\n== H1\n',
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
                            column: 1,
                            line: 7,
                        },
                    },
                    children: [
                        {
                            type: 'womHeading',
                            anchor: null,
                            depth: 1,
                            expandable: false,
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
                            section_local: 1,
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
                                            offset: 5,
                                            column: 6,
                                            line: 1,
                                        },
                                    },
                                    value: 'H1',
                                },
                            ],
                        },
                        {
                            type: 'womMarkdown',
                            attributes: {
                            },
                            format: 'wacko',
                            inline: false,
                            position: {
                                start: {
                                    offset: 6,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 32,
                                    column: 3,
                                    line: 5,
                                },
                            },
                            children: [
                                {
                                    type: 'womHeading',
                                    anchor: null,
                                    depth: 2,
                                    expandable: false,
                                    position: {
                                        start: {
                                            offset: 16,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 22,
                                            column: 7,
                                            line: 3,
                                        },
                                    },
                                    section_local: 2,
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 20,
                                                    column: 5,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 22,
                                                    column: 7,
                                                    line: 3,
                                                },
                                            },
                                            value: 'H2',
                                        },
                                    ],
                                },
                                {
                                    type: 'womHeading',
                                    anchor: null,
                                    depth: 2,
                                    expandable: false,
                                    position: {
                                        start: {
                                            offset: 23,
                                            column: 1,
                                            line: 4,
                                        },
                                        end: {
                                            offset: 29,
                                            column: 7,
                                            line: 4,
                                        },
                                    },
                                    section_local: 3,
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 27,
                                                    column: 5,
                                                    line: 4,
                                                },
                                                end: {
                                                    offset: 29,
                                                    column: 7,
                                                    line: 4,
                                                },
                                            },
                                            value: 'H2',
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
                                    offset: 33,
                                    column: 1,
                                    line: 6,
                                },
                                end: {
                                    offset: 38,
                                    column: 6,
                                    line: 6,
                                },
                            },
                            section_local: 4,
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 36,
                                            column: 4,
                                            line: 6,
                                        },
                                        end: {
                                            offset: 38,
                                            column: 6,
                                            line: 6,
                                        },
                                    },
                                    value: 'H1',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '# H1\n%%(wacko)\n<[\n### H3\n]>\n## H2\n%%\n# H1\n',
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
                            line: 9,
                        },
                    },
                    children: [
                        {
                            type: 'heading',
                            depth: 1,
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
                            section_local: 1,
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
                                    value: 'H1',
                                },
                            ],
                        },
                        {
                            type: 'womMarkdown',
                            attributes: {
                            },
                            format: 'wacko',
                            inline: false,
                            position: {
                                start: {
                                    offset: 5,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 36,
                                    column: 3,
                                    line: 7,
                                },
                            },
                            children: [
                                {
                                    type: 'womBlockquote',
                                    inline: false,
                                    position: {
                                        start: {
                                            offset: 15,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 27,
                                            column: 3,
                                            line: 5,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'heading',
                                            depth: 3,
                                            position: {
                                                start: {
                                                    offset: 18,
                                                    column: 1,
                                                    line: 4,
                                                },
                                                end: {
                                                    offset: 24,
                                                    column: 7,
                                                    line: 4,
                                                },
                                            },
                                            section_local: 2,
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 22,
                                                            column: 5,
                                                            line: 4,
                                                        },
                                                        end: {
                                                            offset: 24,
                                                            column: 7,
                                                            line: 4,
                                                        },
                                                    },
                                                    value: 'H3',
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
                                            offset: 28,
                                            column: 1,
                                            line: 6,
                                        },
                                        end: {
                                            offset: 33,
                                            column: 6,
                                            line: 6,
                                        },
                                    },
                                    section_local: 3,
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 31,
                                                    column: 4,
                                                    line: 6,
                                                },
                                                end: {
                                                    offset: 33,
                                                    column: 6,
                                                    line: 6,
                                                },
                                            },
                                            value: 'H2',
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'heading',
                            depth: 1,
                            position: {
                                start: {
                                    offset: 37,
                                    column: 1,
                                    line: 8,
                                },
                                end: {
                                    offset: 41,
                                    column: 5,
                                    line: 8,
                                },
                            },
                            section_local: 4,
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 39,
                                            column: 3,
                                            line: 8,
                                        },
                                        end: {
                                            offset: 41,
                                            column: 5,
                                            line: 8,
                                        },
                                    },
                                    value: 'H1',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '== H1\n%%(wacko)\n<[\n==== H3\n]>\n## H2\n%%\n# H1\n',
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
                            column: 1,
                            line: 9,
                        },
                    },
                    children: [
                        {
                            type: 'womHeading',
                            anchor: null,
                            depth: 1,
                            expandable: false,
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
                            section_local: 1,
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
                                            offset: 5,
                                            column: 6,
                                            line: 1,
                                        },
                                    },
                                    value: 'H1',
                                },
                            ],
                        },
                        {
                            type: 'womMarkdown',
                            attributes: {
                            },
                            format: 'wacko',
                            inline: false,
                            position: {
                                start: {
                                    offset: 6,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 38,
                                    column: 3,
                                    line: 7,
                                },
                            },
                            children: [
                                {
                                    type: 'womBlockquote',
                                    inline: false,
                                    position: {
                                        start: {
                                            offset: 16,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 29,
                                            column: 3,
                                            line: 5,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'womHeading',
                                            anchor: null,
                                            depth: 3,
                                            expandable: false,
                                            position: {
                                                start: {
                                                    offset: 19,
                                                    column: 1,
                                                    line: 4,
                                                },
                                                end: {
                                                    offset: 26,
                                                    column: 8,
                                                    line: 4,
                                                },
                                            },
                                            section_local: 2,
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 24,
                                                            column: 6,
                                                            line: 4,
                                                        },
                                                        end: {
                                                            offset: 26,
                                                            column: 8,
                                                            line: 4,
                                                        },
                                                    },
                                                    value: 'H3',
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
                                            offset: 30,
                                            column: 1,
                                            line: 6,
                                        },
                                        end: {
                                            offset: 35,
                                            column: 6,
                                            line: 6,
                                        },
                                    },
                                    section_local: 3,
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
                                                    offset: 35,
                                                    column: 6,
                                                    line: 6,
                                                },
                                            },
                                            value: 'H2',
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'heading',
                            depth: 1,
                            position: {
                                start: {
                                    offset: 39,
                                    column: 1,
                                    line: 8,
                                },
                                end: {
                                    offset: 43,
                                    column: 5,
                                    line: 8,
                                },
                            },
                            section_local: 4,
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 41,
                                            column: 3,
                                            line: 8,
                                        },
                                        end: {
                                            offset: 43,
                                            column: 5,
                                            line: 8,
                                        },
                                    },
                                    value: 'H1',
                                },
                            ],
                        },
                    ],
                },
            },
        ],
    },
];
