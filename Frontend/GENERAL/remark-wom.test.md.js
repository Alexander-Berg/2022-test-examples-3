module.exports = [
    {
        group: 'womRemark',
        tests: [
            {
                markup: '!!Замечание!!\n',
                title: '!!Замечание!!',
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
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 11,
                                                    column: 12,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Замечание',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '!!(крас)Текст красного цвета!!\n',
                title: '!!(крас)Текст красного цвета!!',
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
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'крас',
                                        value: '@red',
                                    },
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
                                                    offset: 28,
                                                    column: 29,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Текст красного цвета',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '!!(зел)Текст зеленого цвета!!\n',
                title: '!!(зел)Текст зеленого цвета!!',
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
                                                    offset: 7,
                                                    column: 8,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 27,
                                                    column: 28,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Текст зеленого цвета',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '!!(син)Текст синего цвета!!\n',
                title: '!!(син)Текст синего цвета!!',
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
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'син',
                                        value: '@blue',
                                    },
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
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 7,
                                                    column: 8,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 25,
                                                    column: 26,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Текст синего цвета',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '!!(grey)Текст серого цвета!!\n',
                title: '!!(grey)Текст серого цвета!!',
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
                                        raw: 'grey',
                                        value: '@gray',
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
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 8,
                                                    column: 9,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 26,
                                                    column: 27,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Текст серого цвета',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '!!(yellow)Текст желтого цвета!!\n',
                title: '!!(yellow)Текст желтого цвета!!',
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
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'yellow',
                                        value: '@yellow',
                                    },
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
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 10,
                                                    column: 11,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 29,
                                                    column: 30,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Текст желтого цвета',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '!!Восклицание!!!\n',
                title: '!!Восклицание!!!',
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
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 13,
                                                    column: 14,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Восклицание',
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
                                            offset: 16,
                                            column: 17,
                                            line: 1,
                                        },
                                    },
                                    value: '!',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'текст текст !!Полужирный текст\nс переносом!! текст текст\n',
                title: 'Многострочное womRemark форматирование',
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
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: null,
                                        value: '@red',
                                    },
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
                markup: 'текст текст !!(red)Полужирный текст\nс переносом!! текст текст\n',
                title: 'Многострочное цветное womRemark форматирование',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 62,
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
                                    offset: 62,
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
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'red',
                                        value: '@red',
                                    },
                                    position: {
                                        start: {
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 49,
                                            column: 14,
                                            line: 2,
                                        },
                                    },
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
                                                    offset: 47,
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
                                            offset: 49,
                                            column: 14,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 61,
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
                markup: 'текст текст !!(red)Полужирный текст\nс переносом !! текст текст\n',
                title: 'Многострочное цветное womRemark форматирование c пробелом перед закрывающим маркером',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 63,
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
                                    offset: 63,
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
                                            offset: 62,
                                            column: 27,
                                            line: 2,
                                        },
                                    },
                                    value: 'текст текст !!(red)Полужирный текст\nс переносом !! текст текст',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'Привет!!!!!!!!!!!\n',
                title: 'Множественные !!',
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
                                    value: 'Привет!!!!!!!!!!!',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '!!!Замечание!!!!!\n',
                title: 'Начальные множественные !!!',
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
                                            offset: 1,
                                            column: 2,
                                            line: 1,
                                        },
                                    },
                                    value: '!',
                                },
                                {
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: null,
                                        value: '@red',
                                    },
                                    position: {
                                        start: {
                                            offset: 1,
                                            column: 2,
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
                                                    offset: 3,
                                                    column: 4,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 12,
                                                    column: 13,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Замечание',
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
                                            offset: 17,
                                            column: 18,
                                            line: 1,
                                        },
                                    },
                                    value: '!!!',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '!!!(grey)Замечание!!!!!\n',
                title: 'Начальные множественные !!! с цветом',
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
                                    value: '!',
                                },
                                {
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'grey',
                                        value: '@gray',
                                    },
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
                                                    offset: 18,
                                                    column: 19,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Замечание',
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
                                    value: '!!!',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '!! !!(blue)Син!! !! !!(grey)Замечание!!!!! !!(green)Зел!! !!! !!(yellow)Жел!! !!!\n',
                title: 'Начальные множественные !!! с вложением и соседями',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 82,
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
                                    offset: 82,
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
                                    value: '!! ',
                                },
                                {
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'blue',
                                        value: '@blue',
                                    },
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
                                                    offset: 11,
                                                    column: 12,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 14,
                                                    column: 15,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Син',
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
                                    value: ' !! ',
                                },
                                {
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'grey',
                                        value: '@gray',
                                    },
                                    position: {
                                        start: {
                                            offset: 20,
                                            column: 21,
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
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 28,
                                                    column: 29,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 37,
                                                    column: 38,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Замечание',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 39,
                                            column: 40,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 43,
                                            column: 44,
                                            line: 1,
                                        },
                                    },
                                    value: '!!! ',
                                },
                                {
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'green',
                                        value: '@green',
                                    },
                                    position: {
                                        start: {
                                            offset: 43,
                                            column: 44,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 57,
                                            column: 58,
                                            line: 1,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 52,
                                                    column: 53,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 55,
                                                    column: 56,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Зел',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 57,
                                            column: 58,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 62,
                                            column: 63,
                                            line: 1,
                                        },
                                    },
                                    value: ' !!! ',
                                },
                                {
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'yellow',
                                        value: '@yellow',
                                    },
                                    position: {
                                        start: {
                                            offset: 62,
                                            column: 63,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 77,
                                            column: 78,
                                            line: 1,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 72,
                                                    column: 73,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 75,
                                                    column: 76,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Жел',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 77,
                                            column: 78,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 81,
                                            column: 82,
                                            line: 1,
                                        },
                                    },
                                    value: ' !!!',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'Короче: !!(red)Каждый!! охотник !!(yellow)желает!!! !!(green)знать!!! где !!(син)сидит!!! фазан, !!(grey)Вася!!!\n',
                title: 'Последовательные !!',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 113,
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
                                    offset: 113,
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
                                            offset: 8,
                                            column: 9,
                                            line: 1,
                                        },
                                    },
                                    value: 'Короче: ',
                                },
                                {
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'red',
                                        value: '@red',
                                    },
                                    position: {
                                        start: {
                                            offset: 8,
                                            column: 9,
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
                                            value: 'Каждый',
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
                                            offset: 32,
                                            column: 33,
                                            line: 1,
                                        },
                                    },
                                    value: ' охотник ',
                                },
                                {
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'yellow',
                                        value: '@yellow',
                                    },
                                    position: {
                                        start: {
                                            offset: 32,
                                            column: 33,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 50,
                                            column: 51,
                                            line: 1,
                                        },
                                    },
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
                                                    offset: 48,
                                                    column: 49,
                                                    line: 1,
                                                },
                                            },
                                            value: 'желает',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 50,
                                            column: 51,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 52,
                                            column: 53,
                                            line: 1,
                                        },
                                    },
                                    value: '! ',
                                },
                                {
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'green',
                                        value: '@green',
                                    },
                                    position: {
                                        start: {
                                            offset: 52,
                                            column: 53,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 68,
                                            column: 69,
                                            line: 1,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 61,
                                                    column: 62,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 66,
                                                    column: 67,
                                                    line: 1,
                                                },
                                            },
                                            value: 'знать',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 68,
                                            column: 69,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 74,
                                            column: 75,
                                            line: 1,
                                        },
                                    },
                                    value: '! где ',
                                },
                                {
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'син',
                                        value: '@blue',
                                    },
                                    position: {
                                        start: {
                                            offset: 74,
                                            column: 75,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 88,
                                            column: 89,
                                            line: 1,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 81,
                                                    column: 82,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 86,
                                                    column: 87,
                                                    line: 1,
                                                },
                                            },
                                            value: 'сидит',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 88,
                                            column: 89,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 97,
                                            column: 98,
                                            line: 1,
                                        },
                                    },
                                    value: '! фазан, ',
                                },
                                {
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'grey',
                                        value: '@gray',
                                    },
                                    position: {
                                        start: {
                                            offset: 97,
                                            column: 98,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 111,
                                            column: 112,
                                            line: 1,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 105,
                                                    column: 106,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 109,
                                                    column: 110,
                                                    line: 1,
                                                },
                                            },
                                            value: 'Вася',
                                        },
                                    ],
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 111,
                                            column: 112,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 112,
                                            column: 113,
                                            line: 1,
                                        },
                                    },
                                    value: '!',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '!!red!!, !!(green)green!!\n',
                title: 'Хитрый кейс (сначала implicit, потом explicit)',
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
                                                    offset: 5,
                                                    column: 6,
                                                    line: 1,
                                                },
                                            },
                                            value: 'red',
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
                                    value: ', ',
                                },
                                {
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'green',
                                        value: '@green',
                                    },
                                    position: {
                                        start: {
                                            offset: 9,
                                            column: 10,
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
                                            value: 'green',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '!!(приветики медведики)!!\n',
                title: 'Хитрый кейс (как будто параметры, но нет: 1)',
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
                                            offset: 25,
                                            column: 26,
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
                                                    offset: 23,
                                                    column: 24,
                                                    line: 1,
                                                },
                                            },
                                            value: '(приветики медведики)',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '!!(приветики)!!\n',
                title: 'Хитрый кейс (как будто параметры, но нет: 2)',
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
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 13,
                                                    column: 14,
                                                    line: 1,
                                                },
                                            },
                                            value: '(приветики)',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '!!(зел)(приветики)!!\n',
                title: 'Хитрый кейс (как будто параметры, но нет: 3)',
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
                                                    offset: 7,
                                                    column: 8,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 18,
                                                    column: 19,
                                                    line: 1,
                                                },
                                            },
                                            value: '(приветики)',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '!!(зел) — нет, так не работает!!\n',
                title: 'Закрывающая круглая скобка не прилегает к тексту',
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
                                            offset: 32,
                                            column: 33,
                                            line: 1,
                                        },
                                    },
                                    value: '!!(зел) — нет, так не работает!!',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '193/!!(жел)384!!/!!(red)576!!\n',
                title: '193/!!(жел)384!!/!!(red)576!!',
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
                                    value: '193/',
                                },
                                {
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'жел',
                                        value: '@yellow',
                                    },
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
                                                    offset: 11,
                                                    column: 12,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 14,
                                                    column: 15,
                                                    line: 1,
                                                },
                                            },
                                            value: '384',
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
                                            offset: 17,
                                            column: 18,
                                            line: 1,
                                        },
                                    },
                                    value: '/',
                                },
                                {
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'red',
                                        value: '@red',
                                    },
                                    position: {
                                        start: {
                                            offset: 17,
                                            column: 18,
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
                                                    offset: 24,
                                                    column: 25,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 27,
                                                    column: 28,
                                                    line: 1,
                                                },
                                            },
                                            value: '576',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '!!(violet)test!!',
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
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'violet',
                                        value: '@violet',
                                    },
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
                    ],
                },
            },
            {
                markup: '!!(фиол)test!!',
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
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'фиол',
                                        value: '@violet',
                                    },
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
                                                    offset: 8,
                                                    column: 9,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 12,
                                                    column: 13,
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
                markup: '!!(фиолетовый)test!!',
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
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'фиолетовый',
                                        value: '@violet',
                                    },
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
                                                    offset: 14,
                                                    column: 15,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 18,
                                                    column: 19,
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
                markup: '!!(cyan)test!!',
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
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'cyan',
                                        value: '@cyan',
                                    },
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
                                                    offset: 8,
                                                    column: 9,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 12,
                                                    column: 13,
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
                markup: '!!(глб)test!!',
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
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'глб',
                                        value: '@cyan',
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
                                                    offset: 11,
                                                    column: 12,
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
                markup: '!!(голубой)test!!',
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
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'голубой',
                                        value: '@cyan',
                                    },
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
                                                    offset: 11,
                                                    column: 12,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 15,
                                                    column: 16,
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
                markup: '!!(orange)test!!',
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
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'orange',
                                        value: '@orange',
                                    },
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
                    ],
                },
            },
            {
                markup: '!!(оранж)test!!',
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
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'оранж',
                                        value: '@orange',
                                    },
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
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 9,
                                                    column: 10,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 13,
                                                    column: 14,
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
                markup: '!!(оранжевый)test!!',
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
                                    type: 'womRemark',
                                    color: {
                                        type: 'color',
                                        raw: 'оранжевый',
                                        value: '@orange',
                                    },
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
                                            value: 'test',
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
