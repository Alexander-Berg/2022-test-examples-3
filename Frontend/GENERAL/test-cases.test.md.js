module.exports = [
    {
        group: 'Базовые строчные элементы',
        tests: [
            {
                markup: '**Полужирный текст**\n',
                title: '**Полужирный текст**',
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
                                    type: 'strong',
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
                                            type: 'text',
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
                                            value: 'Полужирный текст',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '//Курсивный текст//\n',
                title: '//Курсивный текст//',
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
                                    offset: 20,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womItalic',
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
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 17,
                                                    column: 18,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Курсивный текст',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '//Курсивный \\//текст//\n',
                title: '//Курсивный \\//текст//',
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
                                    offset: 23,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womItalic',
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
                                            value: 'Курсивный ',
                                        },
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 12,
                                                    column: 13,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 14,
                                                    column: 15,
                                                    line: 1,
                                                },
                                            },
                                            value: '/',
                                        },
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 14,
                                                    column: 15,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 20,
                                                    column: 21,
                                                    line: 1,
                                                },
                                            },
                                            value: '/текст',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '__Подчеркнутый текст__\n',
                title: '__Подчеркнутый текст__',
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
                                    offset: 23,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womUnderline',
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
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 20,
                                                    column: 21,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Подчеркнутый текст',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '##Моноширинный текст##\n',
                title: '##Моноширинный текст##',
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
                                    offset: 23,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womMonospace',
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
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 20,
                                                    column: 21,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Моноширинный текст',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '++Мелкий текст++\n',
                title: '++Мелкий текст++',
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
                                    offset: 17,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womSmall',
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
                                            type: 'text',
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
                                            value: 'Мелкий текст',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '--Зачеркнутый текст--\n',
                title: '--Зачеркнутый текст--',
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
                                    offset: 22,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womStrike',
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
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 19,
                                                    column: 20,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Зачеркнутый текст',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '~~Зачеркнутый текст~~\n',
                title: '~~Зачеркнутый текст~~',
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
                                    offset: 22,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'delete',
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
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 19,
                                                    column: 20,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Зачеркнутый текст',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '??Вопрос??\n',
                title: '??Вопрос??',
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
                                    type: 'womQuestion',
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
                                            value: 'Вопрос',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '??Вопрошение???\n',
                title: '??Вопрошение???',
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
                                    offset: 16,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womQuestion',
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
                                            value: 'Вопрошение',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 14,
                                            column: 15,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 15,
                                            column: 16,
                                            line: 1,
                                        },
                                    },
                                    value: '?',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '** Полужирный текст**\n',
                title: 'Пробелы после маркера',
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
                                    offset: 22,
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
                                            offset: 21,
                                            column: 22,
                                            line: 1,
                                        },
                                    },
                                    value: '** Полужирный текст**',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '**Полужирный текст **\n',
                title: 'Пробелы до закрывающего маркера',
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
                                    offset: 22,
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
                                            offset: 21,
                                            column: 22,
                                            line: 1,
                                        },
                                    },
                                    value: '**Полужирный текст **',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '** Полужирный текст **\n',
                title: 'Пробелы вокруг маркеров',
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
                                    value: '** Полужирный текст **',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'текст текст **Полужирный текст\nс переносом** текст текст\n',
                title: 'Многострочное strong форматирование',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 57,
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
                                    offset: 57,
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
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                    },
                                    value: 'текст текст ',
                                },
                                {
                                    type: 'strong',
                                    position: {
                                        start: {
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 44,
                                            column: 14,
                                            line: 2,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 14,
                                                    column: 15,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 42,
                                                    column: 12,
                                                    line: 2,
                                                },
                                            },
                                            value: 'Полужирный текст\nс переносом',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 44,
                                            column: 14,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 56,
                                            column: 26,
                                            line: 2,
                                        },
                                    },
                                    value: ' текст текст',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '??Вопрошение++??++??\n',
                title: 'Смешение',
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
                                    type: 'womQuestion',
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
                                            type: 'text',
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
                                            value: 'Вопрошение++',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 16,
                                            column: 17,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 20,
                                            column: 21,
                                            line: 1,
                                        },
                                    },
                                    value: '++??',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'Привет###########\n',
                title: 'Множественные ##',
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
                                            offset: 17,
                                            column: 18,
                                            line: 1,
                                        },
                                    },
                                    value: 'Привет###########',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'Привет???????????\n',
                title: 'Множественные ??',
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
                                            offset: 17,
                                            column: 18,
                                            line: 1,
                                        },
                                    },
                                    value: 'Привет???????????',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'Привет***********\n',
                title: 'Множественные **',
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
                                            offset: 17,
                                            column: 18,
                                            line: 1,
                                        },
                                    },
                                    value: 'Привет***********',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'Привет+++++++++++\n',
                title: 'Множественные ++',
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
                                            offset: 17,
                                            column: 18,
                                            line: 1,
                                        },
                                    },
                                    value: 'Привет+++++++++++',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'Привет^^^^^^^^^^^\n',
                title: 'Множественные ^^',
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
                                            offset: 17,
                                            column: 18,
                                            line: 1,
                                        },
                                    },
                                    value: 'Привет^^^^^^^^^^^',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'Приветvvvvvvvvvvv\n',
                title: 'Множественные vv',
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
                                            offset: 17,
                                            column: 18,
                                            line: 1,
                                        },
                                    },
                                    value: 'Приветvvvvvvvvvvv',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '***Полужирный текст******\n',
                title: 'Начальные множественные ***',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 26,
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
                                    offset: 26,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'strong',
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
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 19,
                                                    column: 20,
                                                    line: 1,
                                                },
                                            },
                                            value: '*Полужирный текст',
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
                                            offset: 25,
                                            column: 26,
                                            line: 1,
                                        },
                                    },
                                    value: '****',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '///Курсивный текст/////\n',
                title: 'Начальные множественные ///',
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
                                    offset: 24,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womItalic',
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
                                            type: 'text',
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
                                            value: '/Курсивный текст',
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
                                            offset: 23,
                                            column: 24,
                                            line: 1,
                                        },
                                    },
                                    value: '///',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '___Подчеркнутый текст_____\n',
                title: 'Начальные множественные ___',
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
                                    offset: 27,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womUnderline',
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
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 21,
                                                    column: 22,
                                                    line: 1,
                                                },
                                            },
                                            value: '_Подчеркнутый текст',
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
                                            offset: 26,
                                            column: 27,
                                            line: 1,
                                        },
                                    },
                                    value: '___',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '###Моноширинный текст#####\n',
                title: 'Начальные множественные ###',
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
                                    offset: 27,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womMonospace',
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
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 21,
                                                    column: 22,
                                                    line: 1,
                                                },
                                            },
                                            value: '#Моноширинный текст',
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
                                            offset: 26,
                                            column: 27,
                                            line: 1,
                                        },
                                    },
                                    value: '###',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '+++Мелкий текст++++++\n',
                title: 'Начальные множественные +++',
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
                                    offset: 22,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womSmall',
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
                                            value: '+Мелкий текст',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 17,
                                            column: 18,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 21,
                                            column: 22,
                                            line: 1,
                                        },
                                    },
                                    value: '++++',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '???Вопрос????????\n',
                title: 'Начальные множественные ???',
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
                                    type: 'womQuestion',
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
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 9,
                                                    column: 10,
                                                    line: 1,
                                                },
                                            },
                                            value: '?Вопрос',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 11,
                                            column: 12,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 17,
                                            column: 18,
                                            line: 1,
                                        },
                                    },
                                    value: '??????',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '""**Жирный текст**""',
                title: '""**Жирный текст**""',
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
                            type: 'paragraph',
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
                                    type: 'womEscape',
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
                                    raw: '""**Жирный текст**""',
                                    value: '**Жирный текст**',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '~**Жирный_текст**',
                title: '~**Жирный_текст**',
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
                            column: 18,
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
                                    offset: 17,
                                    column: 18,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womEscape',
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
                                    raw: '~**Жирный_текст**',
                                    value: '**Жирный_текст**',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'E=mc^^2^^\n',
                title: 'Верхний индекс',
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
                                    value: 'E=mc',
                                },
                                {
                                    type: 'womSuperscript',
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
                                                    offset: 7,
                                                    column: 8,
                                                    line: 1,
                                                },
                                            },
                                            value: '2',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'H vv2vv O\n',
                title: 'Нижний индекс',
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
                                            offset: 2,
                                            column: 3,
                                            line: 1,
                                        },
                                    },
                                    value: 'H ',
                                },
                                {
                                    type: 'womSubscript',
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
                                                    offset: 4,
                                                    column: 5,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 5,
                                                    column: 6,
                                                    line: 1,
                                                },
                                            },
                                            value: '2',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 7,
                                            column: 8,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 9,
                                            column: 10,
                                            line: 1,
                                        },
                                    },
                                    value: ' O',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'H""""vv2vv""""O\n',
                title: 'Нижний индекс без пробелов',
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
                                    offset: 16,
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
                                    value: 'H',
                                },
                                {
                                    type: 'womEscape',
                                    position: {
                                        start: {
                                            offset: 1,
                                            column: 2,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 5,
                                            column: 6,
                                            line: 1,
                                        },
                                    },
                                    raw: '""""',
                                    value: '',
                                },
                                {
                                    type: 'womSubscript',
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
                                                    offset: 8,
                                                    column: 9,
                                                    line: 1,
                                                },
                                            },
                                            value: '2',
                                        },
                                    ],
                                },
                                {
                                    type: 'womEscape',
                                    position: {
                                        start: {
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 14,
                                            column: 15,
                                            line: 1,
                                        },
                                    },
                                    raw: '""""',
                                    value: '',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 14,
                                            column: 15,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 15,
                                            column: 16,
                                            line: 1,
                                        },
                                    },
                                    value: 'O',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: ' #ff0000 или #F00\n',
                title: 'Присвоение цвета',
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
                                    type: 'color',
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
                                    raw: '#ff0000',
                                    value: 'ff0000',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 8,
                                            column: 9,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 13,
                                            column: 14,
                                            line: 1,
                                        },
                                    },
                                    value: ' или ',
                                },
                                {
                                    type: 'color',
                                    position: {
                                        start: {
                                            offset: 13,
                                            column: 14,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 17,
                                            column: 18,
                                            line: 1,
                                        },
                                    },
                                    raw: '#F00',
                                    value: 'f00',
                                },
                            ],
                        },
                    ],
                },
            },
        ],
    },
    {
        group: 'Разделители (break)',
        tests: [
            {
                markup: '---\n',
                title: '---',
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
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'thematicBreak',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 3,
                                    column: 4,
                                    line: 1,
                                },
                            },
                        },
                    ],
                },
            },
            {
                markup: '____\n',
                title: '____',
                expect: {
                    type: 'root',
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
                            type: 'thematicBreak',
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
                        },
                    ],
                },
            },
            {
                markup: 'тест---шмест\n',
                title: 'Явный перевод строки',
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
                                    offset: 13,
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
                                    value: 'тест',
                                },
                                {
                                    type: 'womBreak',
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
                                    raw: '---',
                                },
                                {
                                    type: 'text',
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
                                    value: 'шмест',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'тест---шмест---гдест?---нигдест\n',
                title: 'Однострочные переводы пачкой',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 32,
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
                                    offset: 32,
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
                                    value: 'тест',
                                },
                                {
                                    type: 'womBreak',
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
                                    raw: '---',
                                },
                                {
                                    type: 'text',
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
                                    value: 'шмест',
                                },
                                {
                                    type: 'womBreak',
                                    position: {
                                        start: {
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 15,
                                            column: 16,
                                            line: 1,
                                        },
                                    },
                                    raw: '---',
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
                                            column: 22,
                                            line: 1,
                                        },
                                    },
                                    value: 'гдест?',
                                },
                                {
                                    type: 'womBreak',
                                    position: {
                                        start: {
                                            offset: 21,
                                            column: 22,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 24,
                                            column: 25,
                                            line: 1,
                                        },
                                    },
                                    raw: '---',
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
                                            offset: 31,
                                            column: 32,
                                            line: 1,
                                        },
                                    },
                                    value: 'нигдест',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'тест---шмест--зарезано--да---еще\n',
                title: 'Переводы пачкой с зачеркнутым (feat strike)',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 33,
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
                                    offset: 33,
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
                                    value: 'тест',
                                },
                                {
                                    type: 'womBreak',
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
                                    raw: '---',
                                },
                                {
                                    type: 'text',
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
                                    value: 'шмест',
                                },
                                {
                                    type: 'womStrike',
                                    position: {
                                        start: {
                                            offset: 12,
                                            column: 13,
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
                                                    offset: 14,
                                                    column: 15,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 22,
                                                    column: 23,
                                                    line: 1,
                                                },
                                            },
                                            value: 'зарезано',
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
                                    value: 'да',
                                },
                                {
                                    type: 'womBreak',
                                    position: {
                                        start: {
                                            offset: 26,
                                            column: 27,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 29,
                                            column: 30,
                                            line: 1,
                                        },
                                    },
                                    raw: '---',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 29,
                                            column: 30,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 32,
                                            column: 33,
                                            line: 1,
                                        },
                                    },
                                    value: 'еще',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '---тест---шмест---квест---\n',
                title: 'Переводы со всех сторон',
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
                                    offset: 27,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womBreak',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 3,
                                            column: 4,
                                            line: 1,
                                        },
                                    },
                                    raw: '---',
                                },
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
                                    value: 'тест',
                                },
                                {
                                    type: 'womBreak',
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
                                    raw: '---',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 15,
                                            column: 16,
                                            line: 1,
                                        },
                                    },
                                    value: 'шмест',
                                },
                                {
                                    type: 'womBreak',
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
                                    raw: '---',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 18,
                                            column: 19,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 23,
                                            column: 24,
                                            line: 1,
                                        },
                                    },
                                    value: 'квест',
                                },
                                {
                                    type: 'womBreak',
                                    position: {
                                        start: {
                                            offset: 23,
                                            column: 24,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 26,
                                            column: 27,
                                            line: 1,
                                        },
                                    },
                                    raw: '---',
                                },
                            ],
                        },
                    ],
                },
            },
        ],
    },
    {
        group: 'Термины (definition)',
        tests: [
            {
                markup: '(?Термин Вот тут всплыло развернутое определение термина?)\n',
                title: 'Термин',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 59,
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
                                    offset: 59,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womDefinition',
                                    title: 'Термин',
                                    equals: false,
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 58,
                                            column: 59,
                                            line: 1,
                                        },
                                    },
                                    value: 'Вот тут всплыло развернутое определение термина',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '(?Термин с пробелами==И тут тоже всплыло развернутое определение термина с пробелами?)\n',
                title: 'Термин с пробелами',
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
                                    offset: 87,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womDefinition',
                                    title: 'Термин с пробелами',
                                    equals: true,
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 86,
                                            column: 87,
                                            line: 1,
                                        },
                                    },
                                    value: 'И тут тоже всплыло развернутое определение термина с пробелами',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '(?Термин Описалово?)(?Еще==Еще описалово?)\n',
                title: 'Термины подряд разные',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 43,
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
                                    offset: 43,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womDefinition',
                                    title: 'Термин',
                                    equals: false,
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
                                    value: 'Описалово',
                                },
                                {
                                    type: 'womDefinition',
                                    title: 'Еще',
                                    equals: true,
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 20,
                                            column: 21,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 42,
                                            column: 43,
                                            line: 1,
                                        },
                                    },
                                    value: 'Еще описалово',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'Текст и тут (?Термин Описалово?), а потом еще текст и (?Еще термин==Еще описалово?). И текст в конце\n',
                title: 'Термины в тексте',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 101,
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
                                    offset: 101,
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
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                    },
                                    value: 'Текст и тут ',
                                },
                                {
                                    type: 'womDefinition',
                                    title: 'Термин',
                                    equals: false,
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 32,
                                            column: 33,
                                            line: 1,
                                        },
                                    },
                                    value: 'Описалово',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 32,
                                            column: 33,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 54,
                                            column: 55,
                                            line: 1,
                                        },
                                    },
                                    value: ', а потом еще текст и ',
                                },
                                {
                                    type: 'womDefinition',
                                    title: 'Еще термин',
                                    equals: true,
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 54,
                                            column: 55,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 83,
                                            column: 84,
                                            line: 1,
                                        },
                                    },
                                    value: 'Еще описалово',
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
                                            offset: 100,
                                            column: 101,
                                            line: 1,
                                        },
                                    },
                                    value: '. И текст в конце',
                                },
                            ],
                        },
                    ],
                },
            },
        ],
    },
    {
        group: 'Сноски (footnote, reference, definition)',
        tests: [
            {
                markup: 'Текст, потом сноска[[*]] и вторая[[**]]\n',
                title: 'Сноски',
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
                                    offset: 40,
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
                                            offset: 19,
                                            column: 20,
                                            line: 1,
                                        },
                                    },
                                    value: 'Текст, потом сноска',
                                },
                                {
                                    type: 'womFootnoteReference',
                                    identifier: '',
                                    label: null,
                                    position: {
                                        start: {
                                            offset: 19,
                                            column: 20,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 24,
                                            column: 25,
                                            line: 1,
                                        },
                                    },
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
                                            offset: 33,
                                            column: 34,
                                            line: 1,
                                        },
                                    },
                                    value: ' и вторая',
                                },
                                {
                                    type: 'womFootnoteReference',
                                    identifier: '*',
                                    label: null,
                                    position: {
                                        start: {
                                            offset: 33,
                                            column: 34,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 39,
                                            column: 40,
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
                markup: 'Текст, потом цифровая сноска[[*1]] и вторая[[*2]]\n',
                title: 'Еще сноски',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 50,
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
                                    offset: 50,
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
                                            offset: 28,
                                            column: 29,
                                            line: 1,
                                        },
                                    },
                                    value: 'Текст, потом цифровая сноска',
                                },
                                {
                                    type: 'womFootnoteReference',
                                    identifier: '1',
                                    label: null,
                                    position: {
                                        start: {
                                            offset: 28,
                                            column: 29,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 34,
                                            column: 35,
                                            line: 1,
                                        },
                                    },
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 34,
                                            column: 35,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 43,
                                            column: 44,
                                            line: 1,
                                        },
                                    },
                                    value: ' и вторая',
                                },
                                {
                                    type: 'womFootnoteReference',
                                    identifier: '2',
                                    label: null,
                                    position: {
                                        start: {
                                            offset: 43,
                                            column: 44,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 49,
                                            column: 50,
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
                markup: 'Это [[*)) не сноска\n',
                title: 'Это [[*)) не сноска',
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
                                    offset: 20,
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
                                            offset: 19,
                                            column: 20,
                                            line: 1,
                                        },
                                    },
                                    value: 'Это [[*)) не сноска',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'Текст, потом сноска((*)) и цифровая сноска((*1))\n',
                title: 'Сноски в круглых скобках',
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
                                            offset: 19,
                                            column: 20,
                                            line: 1,
                                        },
                                    },
                                    value: 'Текст, потом сноска',
                                },
                                {
                                    type: 'womFootnoteReference',
                                    identifier: '',
                                    label: null,
                                    position: {
                                        start: {
                                            offset: 19,
                                            column: 20,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 24,
                                            column: 25,
                                            line: 1,
                                        },
                                    },
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
                                            offset: 42,
                                            column: 43,
                                            line: 1,
                                        },
                                    },
                                    value: ' и цифровая сноска',
                                },
                                {
                                    type: 'womFootnoteReference',
                                    identifier: '1',
                                    label: null,
                                    position: {
                                        start: {
                                            offset: 42,
                                            column: 43,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 48,
                                            column: 49,
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
                markup: '[[*id label]] Cноска с ярлыком\n',
                title: 'Cноска с ярлыком',
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
                                    type: 'womFootnoteReference',
                                    identifier: 'id',
                                    label: 'label',
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
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 13,
                                            column: 14,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 30,
                                            column: 31,
                                            line: 1,
                                        },
                                    },
                                    value: ' Cноска с ярлыком',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '[[*id label with spaces]] Cноска с ярлыком c пробелами\n',
                title: 'Cноска с ярлыком c пробелами',
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
                                    offset: 55,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womFootnoteReference',
                                    identifier: 'id',
                                    label: 'label with spaces',
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
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 25,
                                            column: 26,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 54,
                                            column: 55,
                                            line: 1,
                                        },
                                    },
                                    value: ' Cноска с ярлыком c пробелами',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '[[#*]] Расшифровка первой сноски\n',
                title: 'Расшифровка первой сноски',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 33,
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
                                    offset: 33,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womFootnoteDefinition',
                                    identifier: '*',
                                    label: null,
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
                                            offset: 32,
                                            column: 33,
                                            line: 1,
                                        },
                                    },
                                    value: ' Расшифровка первой сноски',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '[[#**]] Расшифровка второй сноски\n',
                title: 'Расшифровка второй сноски',
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
                                    offset: 34,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womFootnoteDefinition',
                                    identifier: '**',
                                    label: null,
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
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 7,
                                            column: 8,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 33,
                                            column: 34,
                                            line: 1,
                                        },
                                    },
                                    value: ' Расшифровка второй сноски',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '[[#1]] Расшифровка цифровой сноски\n',
                title: 'Расшифровка цифровой сноски',
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
                                    offset: 35,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womFootnoteDefinition',
                                    identifier: '1',
                                    label: null,
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
                                            offset: 34,
                                            column: 35,
                                            line: 1,
                                        },
                                    },
                                    value: ' Расшифровка цифровой сноски',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '((#1)) Расшифровка сноски в круглых скобках\n',
                title: 'Расшифровка сноски в круглых скобках',
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
                                    offset: 44,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womFootnoteDefinition',
                                    identifier: '1',
                                    label: null,
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
                                            offset: 43,
                                            column: 44,
                                            line: 1,
                                        },
                                    },
                                    value: ' Расшифровка сноски в круглых скобках',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '[[#id label]] Расшифровка сноски с ярлыком\n',
                title: 'Расшифровка сноски с ярлыком',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 43,
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
                                    offset: 43,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womFootnoteDefinition',
                                    identifier: 'id',
                                    label: 'label',
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
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 13,
                                            column: 14,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 42,
                                            column: 43,
                                            line: 1,
                                        },
                                    },
                                    value: ' Расшифровка сноски с ярлыком',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '[[#]] Расшифровка сноски без id\n',
                title: 'Расшифровка сноски без id',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 32,
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
                                    offset: 32,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womFootnoteDefinition',
                                    identifier: '',
                                    label: null,
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
                                            offset: 31,
                                            column: 32,
                                            line: 1,
                                        },
                                    },
                                    value: ' Расшифровка сноски без id',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '[[# lab el]] Расшифровка сноски без id c ярлыком\n',
                title: 'Расшифровка сноски без id c ярлыком',
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
                                    type: 'womFootnoteDefinition',
                                    identifier: '',
                                    label: 'lab el',
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
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 48,
                                            column: 49,
                                            line: 1,
                                        },
                                    },
                                    value: ' Расшифровка сноски без id c ярлыком',
                                },
                            ],
                        },
                    ],
                },
            },
        ],
    },
    {
        group: 'tilde-based',
        tests: [
            {
                markup: '~~~\ncode\n~~~\n',
                title: 'Код через ~',
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
                            column: 1,
                            line: 4,
                        },
                    },
                    children: [
                        {
                            type: 'code',
                            lang: null,
                            meta: null,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 12,
                                    column: 4,
                                    line: 3,
                                },
                            },
                            value: 'code',
                        },
                    ],
                },
            },
            {
                markup: 'Тильда ~ внутри текста\n',
                title: 'Тильда ~ внутри текста',
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
                                    value: 'Тильда ~ внутри текста',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '~~strike~~ some text ~~~ ~!!red!!\n',
                title: '~~strike~~ some text ~~~ ~!!red!!',
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
                                    offset: 34,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'delete',
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
                                            value: 'strike',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 21,
                                            column: 22,
                                            line: 1,
                                        },
                                    },
                                    value: ' some text ',
                                },
                                {
                                    type: 'womEscape',
                                    position: {
                                        start: {
                                            offset: 21,
                                            column: 22,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 23,
                                            column: 24,
                                            line: 1,
                                        },
                                    },
                                    raw: '~~',
                                    value: '~',
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
                                            offset: 25,
                                            column: 26,
                                            line: 1,
                                        },
                                    },
                                    value: '~ ',
                                },
                                {
                                    type: 'womEscape',
                                    position: {
                                        start: {
                                            offset: 25,
                                            column: 26,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 33,
                                            column: 34,
                                            line: 1,
                                        },
                                    },
                                    raw: '~!!red!!',
                                    value: '!!red!!',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '~~ ~~\n',
                title: '~~ ~~',
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
                                    type: 'womEscape',
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
                                    raw: '~~',
                                    value: '~',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 2,
                                            column: 3,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 3,
                                            column: 4,
                                            line: 1,
                                        },
                                    },
                                    value: ' ',
                                },
                                {
                                    type: 'womEscape',
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
                                    raw: '~~',
                                    value: '~',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'aa ~bb--- cc\n',
                title: 'aa ~bb--- cc',
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
                                    offset: 13,
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
                                            offset: 3,
                                            column: 4,
                                            line: 1,
                                        },
                                    },
                                    value: 'aa ',
                                },
                                {
                                    type: 'womEscape',
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
                                    raw: '~bb---',
                                    value: 'bb---',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 9,
                                            column: 10,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                    },
                                    value: ' cc',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'aa ~~bb--- cc\n',
                title: 'aa ~~bb--- cc',
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
                                    offset: 14,
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
                                            offset: 3,
                                            column: 4,
                                            line: 1,
                                        },
                                    },
                                    value: 'aa ',
                                },
                                {
                                    type: 'womEscape',
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
                                    raw: '~~',
                                    value: '~',
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
                                            offset: 7,
                                            column: 8,
                                            line: 1,
                                        },
                                    },
                                    value: 'bb',
                                },
                                {
                                    type: 'womBreak',
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
                                    raw: '---',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 13,
                                            column: 14,
                                            line: 1,
                                        },
                                    },
                                    value: ' cc',
                                },
                            ],
                        },
                    ],
                },
            },
        ],
    },
    {
        group: 'Экшны (actions)',
        tests: [
            {
                markup: 'blablaska {{n root=HomePage }}\n',
                title: 'НЕэкшн с текстом перед ним',
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
                                            offset: 30,
                                            column: 31,
                                            line: 1,
                                        },
                                    },
                                    value: 'blablaska {{n root=HomePage }}',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'blablaska {{a root=HomePage }}\n',
                title: 'Инлайн экшн с текстом перед ним',
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
                                    name: 'a',
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
                                            offset: 30,
                                            column: 31,
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
                markup: '{{include}}{{include}}\n',
                title: 'Два блочных экшена подряд в строке',
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
                                    value: '{{include}}{{include}}',
                                },
                            ],
                        },
                    ],
                },
            },
        ],
    },
    {
        group: 'Строчные обертки ссылок',
        tests: [
            {
                markup: '_[test](https://_)\n',
                title: 'Не закрытый emphasis + ссылка с разделителем внутри (_)',
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
                                    offset: 19,
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
                                    value: '_',
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
                                            offset: 18,
                                            column: 19,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://_',
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
                    ],
                },
            },
            {
                markup: '_[test](https://_)_\n',
                title: 'Закрытый emphasis + ссылка с разделителем внутри (_)',
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
                                    offset: 20,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'emphasis',
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
                                                    offset: 1,
                                                    column: 2,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 18,
                                                    column: 19,
                                                    line: 1,
                                                },
                                            },
                                            url: 'https://_',
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
                            ],
                        },
                    ],
                },
            },
            {
                markup: '*[test](https://*)\n',
                title: 'Не закрытый emphasis + ссылка с разделителем внутри (*)',
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
                                    offset: 19,
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
                                            offset: 18,
                                            column: 19,
                                            line: 1,
                                        },
                                    },
                                    url: 'https://*',
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
                    ],
                },
            },
            {
                markup: '*[test](https://*)*\n',
                title: 'Закрытый emphasis + ссылка с разделителем внутри (*)',
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
                                    offset: 20,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'emphasis',
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
                                                    offset: 1,
                                                    column: 2,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 18,
                                                    column: 19,
                                                    line: 1,
                                                },
                                            },
                                            url: 'https://*',
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
                            ],
                        },
                    ],
                },
            },
            {
                markup: '//[Ссылка](https:\\//test)//\n',
                title: 'Markdown гиперссылка, обернутая в WOM italic (escape)',
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
                                    offset: 28,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womItalic',
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
                                            type: 'link',
                                            title: null,
                                            position: {
                                                start: {
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 25,
                                                    column: 26,
                                                    line: 1,
                                                },
                                            },
                                            url: 'https://test',
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
                                                    value: 'Ссылка',
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
                markup: '//[Ссылка](https://test)//\n',
                title: 'Markdown гиперссылка, обернутая в WOM italic',
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
                                    offset: 27,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womItalic',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 26,
                                            column: 27,
                                            line: 1,
                                        },
                                    },
                                    children: [
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
                                                    offset: 24,
                                                    column: 25,
                                                    line: 1,
                                                },
                                            },
                                            url: 'https://test',
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
                                                    value: 'Ссылка',
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
                markup: '**[Ссылка](https://\\**)**\n',
                title: 'Markdown гиперссылка, обернутая в bold (escape)',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 26,
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
                                    offset: 26,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'strong',
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
                                            type: 'link',
                                            title: null,
                                            position: {
                                                start: {
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 23,
                                                    column: 24,
                                                    line: 1,
                                                },
                                            },
                                            url: 'https://**',
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
                                                    value: 'Ссылка',
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
                markup: '**[Ссылка](https://**)**\n',
                title: 'Markdown гиперссылка, обернутая в bold',
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
                                    offset: 25,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'strong',
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
                                            url: 'https://**',
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
                                                    value: 'Ссылка',
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
                markup: '//[Ссылка](https://test) што [Ссылка](https://test) што [Ссылка](https://test)//\n',
                title: 'Несколько Markdown гиперссылок, обернутых в WOM italic',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 81,
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
                                    offset: 81,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womItalic',
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
                                            type: 'link',
                                            title: null,
                                            position: {
                                                start: {
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 24,
                                                    column: 25,
                                                    line: 1,
                                                },
                                            },
                                            url: 'https://test',
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
                                                    value: 'Ссылка',
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
                                                    offset: 29,
                                                    column: 30,
                                                    line: 1,
                                                },
                                            },
                                            value: ' што ',
                                        },
                                        {
                                            type: 'link',
                                            title: null,
                                            position: {
                                                start: {
                                                    offset: 29,
                                                    column: 30,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 51,
                                                    column: 52,
                                                    line: 1,
                                                },
                                            },
                                            url: 'https://test',
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 30,
                                                            column: 31,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 36,
                                                            column: 37,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'Ссылка',
                                                },
                                            ],
                                        },
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 51,
                                                    column: 52,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 56,
                                                    column: 57,
                                                    line: 1,
                                                },
                                            },
                                            value: ' што ',
                                        },
                                        {
                                            type: 'link',
                                            title: null,
                                            position: {
                                                start: {
                                                    offset: 56,
                                                    column: 57,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 78,
                                                    column: 79,
                                                    line: 1,
                                                },
                                            },
                                            url: 'https://test',
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 57,
                                                            column: 58,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 63,
                                                            column: 64,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'Ссылка',
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
                markup: '//((https://test Ссылка))//\n',
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
                                    offset: 28,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womItalic',
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
                                            type: 'womLink',
                                            brackets: false,
                                            position: {
                                                start: {
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 25,
                                                    column: 26,
                                                    line: 1,
                                                },
                                            },
                                            url: 'https://test',
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
                                                            offset: 23,
                                                            column: 24,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'Ссылка',
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
                markup: '//((https://test// Ссылка))//\n',
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
                                    offset: 30,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womItalic',
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
                                            type: 'womLink',
                                            brackets: false,
                                            position: {
                                                start: {
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 27,
                                                    column: 28,
                                                    line: 1,
                                                },
                                            },
                                            url: 'https://test//',
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
                                                    value: 'Ссылка',
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
                markup: '//[[https://test Ссылка]]//\n',
                title: 'WOM [[гиперссылка]], обернутая в WOM italic',
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
                                    offset: 28,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womItalic',
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
                                            type: 'womLink',
                                            brackets: true,
                                            position: {
                                                start: {
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 25,
                                                    column: 26,
                                                    line: 1,
                                                },
                                            },
                                            url: 'https://test',
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
                                                            offset: 23,
                                                            column: 24,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'Ссылка',
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
                markup: '!!(зел)((https://ya.ru ссылка))!!\n',
                title: 'WOM [[гиперссылка]], обернутая в WOM remark (explicit)',
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
                                    offset: 34,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'зел',
                                        value: '@green',
                                    },
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 33,
                                            column: 34,
                                            line: 1,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'womLink',
                                            brackets: false,
                                            position: {
                                                start: {
                                                    offset: 7,
                                                    column: 8,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 31,
                                                    column: 32,
                                                    line: 1,
                                                },
                                            },
                                            url: 'https://ya.ru',
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
                                                            offset: 29,
                                                            column: 30,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'ссылка',
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
                markup: '!!((https://ya.ru ссылка))!!\n',
                title: 'WOM [[гиперссылка]], обернутая в WOM remark (implicit)',
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
                                    offset: 29,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: null,
                                        value: '@red',
                                    },
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
                                            type: 'womLink',
                                            brackets: false,
                                            position: {
                                                start: {
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 26,
                                                    column: 27,
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
                                                            offset: 24,
                                                            column: 25,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'ссылка',
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
    {
        group: 'https://spec.commonmark.org/0.29/#inlines',
        tests: [
            {
                markup: '**foo bar**\n',
                title: '**foo bar**',
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
                                    type: 'strong',
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
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 9,
                                                    column: 10,
                                                    line: 1,
                                                },
                                            },
                                            value: 'foo bar',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'a ** foo bar**\n',
                title: 'a ** foo bar**',
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
                                    value: 'a ** foo bar**',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'test_test_test\n',
                title: 'test_test_test',
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
                                    value: 'test_test_test',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'test*test*test\n',
                title: 'test*test*test',
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
                                {
                                    type: 'emphasis',
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
                                                    offset: 9,
                                                    column: 10,
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
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 14,
                                            column: 15,
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
                markup: 'test//test//test\n',
                title: 'test//test//test',
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
                                    offset: 17,
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
                                {
                                    type: 'womItalic',
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
                                            value: 'test',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 16,
                                            column: 17,
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
                markup: '--strike--strike---what\n',
                title: '--strike--strike---what',
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
                                    offset: 24,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womStrike',
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
                                            value: 'strike',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 16,
                                            column: 17,
                                            line: 1,
                                        },
                                    },
                                    value: 'strike',
                                },
                                {
                                    type: 'womBreak',
                                    position: {
                                        start: {
                                            offset: 16,
                                            column: 17,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 19,
                                            column: 20,
                                            line: 1,
                                        },
                                    },
                                    raw: '---',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 19,
                                            column: 20,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 23,
                                            column: 24,
                                            line: 1,
                                        },
                                    },
                                    value: 'what',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '**text****\n',
                title: '**text****',
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
                                    type: 'strong',
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
                                            value: 'text',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 8,
                                            column: 9,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                    },
                                    value: '**',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'test_test_\n',
                title: 'test_test_',
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
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                    },
                                    value: 'test_test_',
                                },
                            ],
                        },
                    ],
                },
            },
        ],
    },
    {
        group: 'Тесты на positional info',
        tests: [
            {
                markup: '{{a name=x}}',
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
                            column: 13,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'womAction',
                            inline: false,
                            name: 'a',
                            params: {
                                name: 'x',
                            },
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
                        },
                    ],
                },
            },
            {
                markup: ' {{a name=x}} ',
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
                            type: 'womAction',
                            inline: false,
                            name: 'a',
                            params: {
                                name: 'x',
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
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 13,
                                    column: 14,
                                    line: 1,
                                },
                                end: {
                                    offset: 14,
                                    column: 15,
                                    line: 1,
                                },
                            },
                            children: [
                            ],
                        },
                    ],
                },
            },
            {
                markup: ' xxxxxxxxxxxx ',
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
                                            offset: 1,
                                            column: 2,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 13,
                                            column: 14,
                                            line: 1,
                                        },
                                    },
                                    value: 'xxxxxxxxxxxx',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'x\n{{a name=x}}\nx',
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
                            column: 2,
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
                                    value: 'x',
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
                                    offset: 2,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 14,
                                    column: 13,
                                    line: 2,
                                },
                            },
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
                                    offset: 16,
                                    column: 2,
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
                                            offset: 16,
                                            column: 2,
                                            line: 3,
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
                markup: 'x\n{{ \na\nname=x\n}}\nx',
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
                            column: 2,
                            line: 6,
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
                                    value: 'x',
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
                                    offset: 2,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 17,
                                    column: 3,
                                    line: 5,
                                },
                            },
                        },
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 18,
                                    column: 1,
                                    line: 6,
                                },
                                end: {
                                    offset: 19,
                                    column: 2,
                                    line: 6,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 18,
                                            column: 1,
                                            line: 6,
                                        },
                                        end: {
                                            offset: 19,
                                            column: 2,
                                            line: 6,
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
                markup: 'x\n{{iframe}}\nx',
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
                            column: 2,
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
                                    value: 'x',
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
                                    offset: 2,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 12,
                                    column: 11,
                                    line: 2,
                                },
                            },
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
                                    offset: 14,
                                    column: 2,
                                    line: 3,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 13,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 14,
                                            column: 2,
                                            line: 3,
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
                markup: 'x\n{{\niframe\n}}\nx',
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
                            column: 2,
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
                                    value: 'x',
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
                                    offset: 2,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 14,
                                    column: 3,
                                    line: 4,
                                },
                            },
                        },
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 15,
                                    column: 1,
                                    line: 5,
                                },
                                end: {
                                    offset: 16,
                                    column: 2,
                                    line: 5,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 15,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 16,
                                            column: 2,
                                            line: 5,
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
                markup: 'x\n<[\nx\n]>\nx',
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
                            column: 2,
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
                                    value: 'x',
                                },
                            ],
                        },
                        {
                            type: 'womBlockquote',
                            inline: false,
                            position: {
                                start: {
                                    offset: 2,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 9,
                                    column: 3,
                                    line: 4,
                                },
                            },
                            children: [
                                {
                                    type: 'paragraph',
                                    position: {
                                        start: {
                                            offset: 5,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 6,
                                            column: 2,
                                            line: 3,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 5,
                                                    column: 1,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 6,
                                                    column: 2,
                                                    line: 3,
                                                },
                                            },
                                            value: 'x',
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 10,
                                    column: 1,
                                    line: 5,
                                },
                                end: {
                                    offset: 11,
                                    column: 2,
                                    line: 5,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 10,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 11,
                                            column: 2,
                                            line: 5,
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
                markup: 'x<[x]>x',
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
                            column: 8,
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
                                    type: 'womBlockquote',
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
                                                    offset: 4,
                                                    column: 5,
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
                                                            offset: 4,
                                                            column: 5,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'x',
                                                },
                                            ],
                                        },
                                    ],
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
                                            offset: 7,
                                            column: 8,
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
                markup: 'x\n{[\nx\n]}\nx',
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
                            column: 2,
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
                                    value: 'x',
                                },
                            ],
                        },
                        {
                            type: 'womBlock',
                            inline: false,
                            position: {
                                start: {
                                    offset: 2,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 9,
                                    column: 3,
                                    line: 4,
                                },
                            },
                            children: [
                                {
                                    type: 'paragraph',
                                    position: {
                                        start: {
                                            offset: 5,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 6,
                                            column: 2,
                                            line: 3,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 5,
                                                    column: 1,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 6,
                                                    column: 2,
                                                    line: 3,
                                                },
                                            },
                                            value: 'x',
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 10,
                                    column: 1,
                                    line: 5,
                                },
                                end: {
                                    offset: 11,
                                    column: 2,
                                    line: 5,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 10,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 11,
                                            column: 2,
                                            line: 5,
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
                markup: 'x{[x]}x',
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
                            column: 8,
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
                                    type: 'womBlock',
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
                                                    offset: 4,
                                                    column: 5,
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
                                                            offset: 4,
                                                            column: 5,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'x',
                                                },
                                            ],
                                        },
                                    ],
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
                                            offset: 7,
                                            column: 8,
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
                markup: 'x\n<{t\nx\n}>\nx',
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
                            column: 2,
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
                                    value: 'x',
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
                                            offset: 4,
                                            column: 3,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 5,
                                            column: 4,
                                            line: 2,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 4,
                                                    column: 3,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 5,
                                                    column: 4,
                                                    line: 2,
                                                },
                                            },
                                            value: 't',
                                        },
                                    ],
                                },
                            ],
                            inline: false,
                            position: {
                                start: {
                                    offset: 2,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 10,
                                    column: 3,
                                    line: 4,
                                },
                            },
                            children: [
                                {
                                    type: 'paragraph',
                                    position: {
                                        start: {
                                            offset: 6,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 7,
                                            column: 2,
                                            line: 3,
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
                                                    offset: 7,
                                                    column: 2,
                                                    line: 3,
                                                },
                                            },
                                            value: 'x',
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 11,
                                    column: 1,
                                    line: 5,
                                },
                                end: {
                                    offset: 12,
                                    column: 2,
                                    line: 5,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 11,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 12,
                                            column: 2,
                                            line: 5,
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
                markup: 'x\n<{\nx\n}>\nx',
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
                            column: 2,
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
                                    value: 'x',
                                },
                            ],
                        },
                        {
                            type: 'womCut',
                            title: [
                            ],
                            inline: false,
                            position: {
                                start: {
                                    offset: 2,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 9,
                                    column: 3,
                                    line: 4,
                                },
                            },
                            children: [
                                {
                                    type: 'paragraph',
                                    position: {
                                        start: {
                                            offset: 5,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 6,
                                            column: 2,
                                            line: 3,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 5,
                                                    column: 1,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 6,
                                                    column: 2,
                                                    line: 3,
                                                },
                                            },
                                            value: 'x',
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 10,
                                    column: 1,
                                    line: 5,
                                },
                                end: {
                                    offset: 11,
                                    column: 2,
                                    line: 5,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 10,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 11,
                                            column: 2,
                                            line: 5,
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
                markup: 'x<{x}>x',
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
                            column: 8,
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
                                    type: 'womCut',
                                    title: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 3,
                                                    column: 4,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 4,
                                                    column: 5,
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
                                                            offset: 4,
                                                            column: 5,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'x',
                                                },
                                            ],
                                        },
                                    ],
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
                                    children: [
                                    ],
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
                                            offset: 7,
                                            column: 8,
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
                markup: 'x<{}>x',
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
                            column: 7,
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
                                    type: 'womCut',
                                    title: [
                                    ],
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 1,
                                            column: 2,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 5,
                                            column: 6,
                                            line: 1,
                                        },
                                    },
                                    children: [
                                    ],
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
                markup: 'x%%(md)x%%x',
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
                            column: 12,
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
                                    type: 'womMarkdown',
                                    attributes: {
                                    },
                                    format: 'md',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 1,
                                            column: 2,
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
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 7,
                                                    column: 8,
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
                                                            offset: 7,
                                                            column: 8,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 8,
                                                            column: 9,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'x',
                                                },
                                            ],
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 10,
                                            column: 11,
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
                markup: 'x\n%%(md) \nx\n%%\nx',
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
                            column: 2,
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
                                    value: 'x',
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
                                    offset: 2,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 14,
                                    column: 3,
                                    line: 4,
                                },
                            },
                            children: [
                                {
                                    type: 'paragraph',
                                    position: {
                                        start: {
                                            offset: 10,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 11,
                                            column: 2,
                                            line: 3,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 10,
                                                    column: 1,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 11,
                                                    column: 2,
                                                    line: 3,
                                                },
                                            },
                                            value: 'x',
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
                                    line: 5,
                                },
                                end: {
                                    offset: 16,
                                    column: 2,
                                    line: 5,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 15,
                                            column: 1,
                                            line: 5,
                                        },
                                        end: {
                                            offset: 16,
                                            column: 2,
                                            line: 5,
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
                markup: 'x#|||x|x|||#x',
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
                            type: 'paragraph',
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
                                    type: 'womTable',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 1,
                                            column: 2,
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
                                            type: 'womTableRow',
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
                                            children: [
                                                {
                                                    type: 'womTableCell',
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
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 5,
                                                                    column: 6,
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
                                                                            offset: 5,
                                                                            column: 6,
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
                                                {
                                                    type: 'womTableCell',
                                                    position: {
                                                        start: {
                                                            offset: 6,
                                                            column: 7,
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
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 7,
                                                                    column: 8,
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
                                                                            offset: 7,
                                                                            column: 8,
                                                                            line: 1,
                                                                        },
                                                                        end: {
                                                                            offset: 8,
                                                                            column: 9,
                                                                            line: 1,
                                                                        },
                                                                    },
                                                                    value: 'x',
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
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 13,
                                            column: 14,
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
                markup: 'x\n#|\n||\nx\n|\nx\n||\n|#\nx',
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
                            column: 2,
                            line: 9,
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
                                    value: 'x',
                                },
                            ],
                        },
                        {
                            type: 'womTable',
                            inline: false,
                            position: {
                                start: {
                                    offset: 2,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 19,
                                    column: 3,
                                    line: 8,
                                },
                            },
                            children: [
                                {
                                    type: 'womTableRow',
                                    position: {
                                        start: {
                                            offset: 5,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 16,
                                            column: 3,
                                            line: 7,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'womTableCell',
                                            position: {
                                                start: {
                                                    offset: 6,
                                                    column: 2,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 11,
                                                    column: 2,
                                                    line: 5,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'paragraph',
                                                    position: {
                                                        start: {
                                                            offset: 8,
                                                            column: 1,
                                                            line: 4,
                                                        },
                                                        end: {
                                                            offset: 9,
                                                            column: 2,
                                                            line: 4,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'text',
                                                            position: {
                                                                start: {
                                                                    offset: 8,
                                                                    column: 1,
                                                                    line: 4,
                                                                },
                                                                end: {
                                                                    offset: 9,
                                                                    column: 2,
                                                                    line: 4,
                                                                },
                                                            },
                                                            value: 'x',
                                                        },
                                                    ],
                                                },
                                            ],
                                        },
                                        {
                                            type: 'womTableCell',
                                            position: {
                                                start: {
                                                    offset: 10,
                                                    column: 1,
                                                    line: 5,
                                                },
                                                end: {
                                                    offset: 15,
                                                    column: 2,
                                                    line: 7,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'paragraph',
                                                    position: {
                                                        start: {
                                                            offset: 12,
                                                            column: 1,
                                                            line: 6,
                                                        },
                                                        end: {
                                                            offset: 13,
                                                            column: 2,
                                                            line: 6,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'text',
                                                            position: {
                                                                start: {
                                                                    offset: 12,
                                                                    column: 1,
                                                                    line: 6,
                                                                },
                                                                end: {
                                                                    offset: 13,
                                                                    column: 2,
                                                                    line: 6,
                                                                },
                                                            },
                                                            value: 'x',
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
                                    offset: 20,
                                    column: 1,
                                    line: 9,
                                },
                                end: {
                                    offset: 21,
                                    column: 2,
                                    line: 9,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 20,
                                            column: 1,
                                            line: 9,
                                        },
                                        end: {
                                            offset: 21,
                                            column: 2,
                                            line: 9,
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
                markup: 'x\n=== H',
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
                            column: 6,
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
                                    value: 'x',
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
                                    offset: 2,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 7,
                                    column: 6,
                                    line: 2,
                                },
                            },
                            section_local: 1,
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 6,
                                            column: 5,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 7,
                                            column: 6,
                                            line: 2,
                                        },
                                    },
                                    value: 'H',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'x\n===H',
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
                            column: 5,
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
                                    value: 'x',
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
                                    offset: 2,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 6,
                                    column: 5,
                                    line: 2,
                                },
                            },
                            section_local: 1,
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 5,
                                            column: 4,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 6,
                                            column: 5,
                                            line: 2,
                                        },
                                    },
                                    value: 'H',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '1. x',
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
                            column: 5,
                            line: 1,
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
                                    offset: 4,
                                    column: 5,
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
                                            offset: 4,
                                            column: 5,
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
                                                    offset: 4,
                                                    column: 5,
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
                                                            offset: 4,
                                                            column: 5,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'x',
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
                markup: '1. test\n{[\nx\n]}',
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
                            column: 3,
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
                            type: 'womBlock',
                            inline: false,
                            position: {
                                start: {
                                    offset: 8,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 15,
                                    column: 3,
                                    line: 4,
                                },
                            },
                            children: [
                                {
                                    type: 'paragraph',
                                    position: {
                                        start: {
                                            offset: 11,
                                            column: 1,
                                            line: 3,
                                        },
                                        end: {
                                            offset: 12,
                                            column: 2,
                                            line: 3,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 11,
                                                    column: 1,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 12,
                                                    column: 2,
                                                    line: 3,
                                                },
                                            },
                                            value: 'x',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '//x//',
                expect: {
                    type: 'root',
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
                                    column: 6,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womItalic',
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
                                                    offset: 3,
                                                    column: 4,
                                                    line: 1,
                                                },
                                            },
                                            value: 'x',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '{[\n\nx\n\n]}',
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
                            column: 3,
                            line: 5,
                        },
                    },
                    children: [
                        {
                            type: 'womBlock',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 9,
                                    column: 3,
                                    line: 5,
                                },
                            },
                            children: [
                                {
                                    type: 'paragraph',
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
                                    children: [
                                        {
                                            type: 'text',
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
                                            value: 'x',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '1. {[x]}',
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
                                    offset: 8,
                                    column: 9,
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
                                                    type: 'womBlock',
                                                    inline: true,
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
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 5,
                                                                    column: 6,
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
                                                                            offset: 5,
                                                                            column: 6,
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
                markup: '1. x\n{[x]}',
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
                            column: 6,
                            line: 2,
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
                                    offset: 10,
                                    column: 6,
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
                                            offset: 10,
                                            column: 6,
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
                                                    offset: 10,
                                                    column: 6,
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
                                                            offset: 5,
                                                            column: 1,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 'x\n',
                                                },
                                                {
                                                    type: 'womBlock',
                                                    inline: true,
                                                    position: {
                                                        start: {
                                                            offset: 5,
                                                            column: 1,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 10,
                                                            column: 6,
                                                            line: 2,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 7,
                                                                    column: 3,
                                                                    line: 2,
                                                                },
                                                                end: {
                                                                    offset: 8,
                                                                    column: 4,
                                                                    line: 2,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'text',
                                                                    position: {
                                                                        start: {
                                                                            offset: 7,
                                                                            column: 3,
                                                                            line: 2,
                                                                        },
                                                                        end: {
                                                                            offset: 8,
                                                                            column: 4,
                                                                            line: 2,
                                                                        },
                                                                    },
                                                                    value: 'x',
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
                markup: '1. x {[\nx\n]}',
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
                            column: 3,
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
                                    offset: 12,
                                    column: 3,
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
                                            offset: 12,
                                            column: 3,
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
                                                    offset: 12,
                                                    column: 3,
                                                    line: 3,
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
                                                            offset: 5,
                                                            column: 6,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'x ',
                                                },
                                                {
                                                    type: 'womBlock',
                                                    inline: true,
                                                    position: {
                                                        start: {
                                                            offset: 5,
                                                            column: 6,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 12,
                                                            column: 3,
                                                            line: 3,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'paragraph',
                                                            position: {
                                                                start: {
                                                                    offset: 8,
                                                                    column: 1,
                                                                    line: 2,
                                                                },
                                                                end: {
                                                                    offset: 9,
                                                                    column: 2,
                                                                    line: 2,
                                                                },
                                                            },
                                                            children: [
                                                                {
                                                                    type: 'text',
                                                                    position: {
                                                                        start: {
                                                                            offset: 8,
                                                                            column: 1,
                                                                            line: 2,
                                                                        },
                                                                        end: {
                                                                            offset: 9,
                                                                            column: 2,
                                                                            line: 2,
                                                                        },
                                                                    },
                                                                    value: 'x',
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
                markup: '1. x\n   {[\n   x\n   ]}',
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
                            column: 6,
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
                                    offset: 21,
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
                                            offset: 21,
                                            column: 6,
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
                                                    offset: 8,
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
                                                            offset: 4,
                                                            column: 5,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'x',
                                                },
                                            ],
                                        },
                                        {
                                            type: 'womBlock',
                                            inline: false,
                                            position: {
                                                start: {
                                                    offset: 8,
                                                    column: 4,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 21,
                                                    column: 6,
                                                    line: 4,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'paragraph',
                                                    position: {
                                                        start: {
                                                            offset: 14,
                                                            column: 4,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 15,
                                                            column: 5,
                                                            line: 3,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'text',
                                                            position: {
                                                                start: {
                                                                    offset: 14,
                                                                    column: 4,
                                                                    line: 3,
                                                                },
                                                                end: {
                                                                    offset: 15,
                                                                    column: 5,
                                                                    line: 3,
                                                                },
                                                            },
                                                            value: 'x',
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
                markup: '1. {[\n   x\n   x\n   ]}',
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
                            column: 6,
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
                                    offset: 21,
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
                                            offset: 21,
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
                                                    offset: 21,
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
                                                            offset: 15,
                                                            column: 5,
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
                                                                    offset: 15,
                                                                    column: 5,
                                                                    line: 3,
                                                                },
                                                            },
                                                            value: 'x\nx',
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
                markup: '<{t\n{{a name="x"}}\n}>',
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
                                            offset: 3,
                                            column: 4,
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
                                                    offset: 3,
                                                    column: 4,
                                                    line: 1,
                                                },
                                            },
                                            value: 't',
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
                                    offset: 21,
                                    column: 3,
                                    line: 3,
                                },
                            },
                            children: [
                                {
                                    type: 'womAction',
                                    inline: false,
                                    name: 'a',
                                    params: {
                                        name: 'x',
                                    },
                                    position: {
                                        start: {
                                            offset: 4,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 18,
                                            column: 15,
                                            line: 2,
                                        },
                                    },
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '<{t1\n<{t2\n{{a name="x"}}\n}>\n}>',
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
                            column: 3,
                            line: 5,
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
                                            offset: 4,
                                            column: 5,
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
                                                    offset: 4,
                                                    column: 5,
                                                    line: 1,
                                                },
                                            },
                                            value: 't1',
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
                                    offset: 30,
                                    column: 3,
                                    line: 5,
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
                                                    offset: 7,
                                                    column: 3,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 9,
                                                    column: 5,
                                                    line: 2,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 7,
                                                            column: 3,
                                                            line: 2,
                                                        },
                                                        end: {
                                                            offset: 9,
                                                            column: 5,
                                                            line: 2,
                                                        },
                                                    },
                                                    value: 't2',
                                                },
                                            ],
                                        },
                                    ],
                                    inline: false,
                                    position: {
                                        start: {
                                            offset: 5,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 27,
                                            column: 3,
                                            line: 4,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'womAction',
                                            inline: false,
                                            name: 'a',
                                            params: {
                                                name: 'x',
                                            },
                                            position: {
                                                start: {
                                                    offset: 10,
                                                    column: 1,
                                                    line: 3,
                                                },
                                                end: {
                                                    offset: 24,
                                                    column: 15,
                                                    line: 3,
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
                markup: '<{<{{{a name="x"}}}>}>',
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
                                                    offset: 20,
                                                    column: 21,
                                                    line: 1,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'womCut',
                                                    title: [
                                                        {
                                                            type: 'womAction',
                                                            inline: false,
                                                            name: 'a',
                                                            params: {
                                                                name: 'x',
                                                            },
                                                            position: {
                                                                start: {
                                                                    offset: 4,
                                                                    column: 5,
                                                                    line: 1,
                                                                },
                                                                end: {
                                                                    offset: 18,
                                                                    column: 19,
                                                                    line: 1,
                                                                },
                                                            },
                                                        },
                                                    ],
                                                    inline: true,
                                                    position: {
                                                        start: {
                                                            offset: 2,
                                                            column: 3,
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
                                    inline: true,
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
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '#|\n|| \n1 \n||\n|#',
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
                            column: 3,
                            line: 5,
                        },
                    },
                    children: [
                        {
                            type: 'womTable',
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
                                    line: 5,
                                },
                            },
                            children: [
                                {
                                    type: 'womTableRow',
                                    position: {
                                        start: {
                                            offset: 3,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 12,
                                            column: 3,
                                            line: 4,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'womTableCell',
                                            position: {
                                                start: {
                                                    offset: 4,
                                                    column: 2,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 11,
                                                    column: 2,
                                                    line: 4,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'paragraph',
                                                    position: {
                                                        start: {
                                                            offset: 7,
                                                            column: 1,
                                                            line: 3,
                                                        },
                                                        end: {
                                                            offset: 8,
                                                            column: 2,
                                                            line: 3,
                                                        },
                                                    },
                                                    children: [
                                                        {
                                                            type: 'text',
                                                            position: {
                                                                start: {
                                                                    offset: 7,
                                                                    column: 1,
                                                                    line: 3,
                                                                },
                                                                end: {
                                                                    offset: 8,
                                                                    column: 2,
                                                                    line: 3,
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
            },
            {
                markup: 'test\n*test*\ntest',
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
                                    offset: 16,
                                    column: 5,
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
                                            offset: 5,
                                            column: 1,
                                            line: 2,
                                        },
                                    },
                                    value: 'test\n',
                                },
                                {
                                    type: 'emphasis',
                                    position: {
                                        start: {
                                            offset: 5,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 11,
                                            column: 7,
                                            line: 2,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 6,
                                                    column: 2,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 10,
                                                    column: 6,
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
                                            offset: 11,
                                            column: 7,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 16,
                                            column: 5,
                                            line: 3,
                                        },
                                    },
                                    value: '\ntest',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'test&reg;test',
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
                            type: 'paragraph',
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
                                    type: 'text',
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
                                    value: 'test®test',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'https://foo.com/?a=1&b=2',
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
                                    url: 'https://foo.com/?a=1&b=2',
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
                                            value: 'https://foo.com/?a=1&b=2',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'ref:https://foo.com/?a=1&b=2',
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
                            type: 'paragraph',
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
                                    type: 'link',
                                    title: null,
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
                                    ref: true,
                                    url: 'https://foo.com/?a=1&b=2',
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
                                                    offset: 28,
                                                    column: 29,
                                                    line: 1,
                                                },
                                            },
                                            value: 'https://foo.com/?a=1&b=2',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'https://foo.com/?a=1&amp;b=2',
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
                            type: 'paragraph',
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
                                    type: 'link',
                                    title: null,
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
                                    url: 'https://foo.com/?a=1&b=2',
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
                                                    offset: 28,
                                                    column: 29,
                                                    line: 1,
                                                },
                                            },
                                            value: 'https://foo.com/?a=1&b=2',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'ref:https://foo.com/?a=1&amp;b=2',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 32,
                            column: 33,
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
                                    offset: 32,
                                    column: 33,
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
                                            offset: 32,
                                            column: 33,
                                            line: 1,
                                        },
                                    },
                                    ref: true,
                                    url: 'https://foo.com/?a=1&b=2',
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
                                                    offset: 32,
                                                    column: 33,
                                                    line: 1,
                                                },
                                            },
                                            value: 'https://foo.com/?a=1&b=2',
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
