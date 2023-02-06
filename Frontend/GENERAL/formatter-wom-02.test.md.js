module.exports = [
    {
        group: 'womFormatter %%%%',
        tests: [
            {
                markup: 'text %%niagara.yandex.net%% text\n\n%%\nniagara.yandex.net\nniagara.yandex.net\n%%\n',
                title: 'Инлайн vs Блок',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 78,
                            column: 1,
                            line: 7,
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
                                            offset: 5,
                                            column: 6,
                                            line: 1,
                                        },
                                    },
                                    value: 'text ',
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
                                            offset: 27,
                                            column: 28,
                                            line: 1,
                                        },
                                    },
                                    value: 'niagara.yandex.net',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 27,
                                            column: 28,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 32,
                                            column: 33,
                                            line: 1,
                                        },
                                    },
                                    value: ' text',
                                },
                            ],
                        },
                        {
                            type: 'womFormatter',
                            inline: false,
                            position: {
                                start: {
                                    offset: 34,
                                    column: 1,
                                    line: 3,
                                },
                                end: {
                                    offset: 77,
                                    column: 3,
                                    line: 6,
                                },
                            },
                            value: 'niagara.yandex.net\nniagara.yandex.net\n',
                        },
                    ],
                },
            },
            {
                markup: 'Вообще-то для %%моноширинного%% текста %%есть другая%% конструкция.\n',
                title: 'Инлайн вставки форматтера',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 68,
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
                                    offset: 68,
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
                                    value: 'Вообще-то для ',
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
                                            offset: 31,
                                            column: 32,
                                            line: 1,
                                        },
                                    },
                                    value: 'моноширинного',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 31,
                                            column: 32,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 39,
                                            column: 40,
                                            line: 1,
                                        },
                                    },
                                    value: ' текста ',
                                },
                                {
                                    type: 'womFormatter',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 39,
                                            column: 40,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 54,
                                            column: 55,
                                            line: 1,
                                        },
                                    },
                                    value: 'есть другая',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 54,
                                            column: 55,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 67,
                                            column: 68,
                                            line: 1,
                                        },
                                    },
                                    value: ' конструкция.',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'So %%kusok%% then %%(wacko wrapper=right) wacko-shmako %% and %%this one%% at the end\n',
                title: 'Инлайн вставки форматтера с выравниванием',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 86,
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
                                    offset: 86,
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
                                    value: 'So ',
                                },
                                {
                                    type: 'womFormatter',
                                    inline: true,
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
                                    value: 'kusok',
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
                                            offset: 18,
                                            column: 19,
                                            line: 1,
                                        },
                                    },
                                    value: ' then ',
                                },
                                {
                                    type: 'womMarkdown',
                                    attributes: {
                                        wrapper: 'right',
                                    },
                                    format: 'wacko',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 18,
                                            column: 19,
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
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 42,
                                                    column: 43,
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
                                                            offset: 42,
                                                            column: 43,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 54,
                                                            column: 55,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'wacko-shmako',
                                                },
                                            ],
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
                                    value: ' and ',
                                },
                                {
                                    type: 'womFormatter',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 62,
                                            column: 63,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 74,
                                            column: 75,
                                            line: 1,
                                        },
                                    },
                                    value: 'this one',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 74,
                                            column: 75,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 85,
                                            column: 86,
                                            line: 1,
                                        },
                                    },
                                    value: ' at the end',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'Начинаем %%(js) и не заканчиваем\n',
                title: 'Форматтер битым куском',
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
                                    value: 'Начинаем %%(js) и не заканчиваем',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: 'Жир **%%code%%** ?\n',
                title: 'Жирный форматтер',
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
                                            offset: 4,
                                            column: 5,
                                            line: 1,
                                        },
                                    },
                                    value: 'Жир ',
                                },
                                {
                                    type: 'strong',
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
                                            type: 'womFormatter',
                                            inline: true,
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
                                            value: 'code',
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
                                            offset: 18,
                                            column: 19,
                                            line: 1,
                                        },
                                    },
                                    value: ' ?',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '%%\nHello\n%%%%%%%%%%%%%%%\nWorld\n%%%%%%%%%%%%%%%\n%%\n',
                title: 'Форматтер с множественными %%',
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
                            line: 7,
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
                                    offset: 24,
                                    column: 16,
                                    line: 3,
                                },
                            },
                            value: 'Hello\n%%%%%%%%%%%%%',
                        },
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 25,
                                    column: 1,
                                    line: 4,
                                },
                                end: {
                                    offset: 31,
                                    column: 1,
                                    line: 5,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 25,
                                            column: 1,
                                            line: 4,
                                        },
                                        end: {
                                            offset: 30,
                                            column: 6,
                                            line: 4,
                                        },
                                    },
                                    value: 'World',
                                },
                            ],
                        },
                        {
                            type: 'womFormatter',
                            inline: false,
                            position: {
                                start: {
                                    offset: 31,
                                    column: 1,
                                    line: 5,
                                },
                                end: {
                                    offset: 49,
                                    column: 3,
                                    line: 6,
                                },
                            },
                            value: '%%%%%%%%%%%%%\n',
                        },
                    ],
                },
            },
            {
                markup: '%%(css nomark wrapper=box align=left width=270 border=0 nomark)\n.d { font-size:70% }\n%%\n',
                title: 'css formatter wrapper',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 88,
                            column: 1,
                            line: 4,
                        },
                    },
                    children: [
                        {
                            type: 'womFormatter',
                            attributes: {
                                align: 'left',
                                border: '0',
                                nomark: null,
                                width: '270',
                                wrapper: 'box',
                            },
                            format: 'css',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 87,
                                    column: 3,
                                    line: 3,
                                },
                            },
                            value: '.d { font-size:70% }\n',
                        },
                    ],
                },
            },
            {
                markup: '%%(javascript nomark wrapper=box border="5px dashed red")\nalert("hooray!");\n%%\n',
                title: 'javascript formatter',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 79,
                            column: 1,
                            line: 4,
                        },
                    },
                    children: [
                        {
                            type: 'womFormatter',
                            attributes: {
                                border: '5px dashed red',
                                nomark: null,
                                wrapper: 'box',
                            },
                            format: 'javascript',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 78,
                                    column: 3,
                                    line: 3,
                                },
                            },
                            value: 'alert("hooray!");\n',
                        },
                    ],
                },
            },
            {
                markup: '%%(css nomark wrapper=shade)\n.d2 { font-size:70% }\n%%\n',
                title: 'css formatter',
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
                            line: 4,
                        },
                    },
                    children: [
                        {
                            type: 'womFormatter',
                            attributes: {
                                nomark: null,
                                wrapper: 'shade',
                            },
                            format: 'css',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 53,
                                    column: 3,
                                    line: 3,
                                },
                            },
                            value: '.d2 { font-size:70% }\n',
                        },
                    ],
                },
            },
            {
                markup: '%%(wacko wrapper=text align=center) текст по центру %%\n',
                title: 'wacko text aligned',
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
                            type: 'womMarkdown',
                            attributes: {
                                align: 'center',
                                wrapper: 'text',
                            },
                            format: 'wacko',
                            inline: false,
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
                                            offset: 36,
                                            column: 37,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 51,
                                            column: 52,
                                            line: 1,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 36,
                                                    column: 37,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 51,
                                                    column: 52,
                                                    line: 1,
                                                },
                                            },
                                            value: 'текст по центру',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '%%(wacko wrapper=page wrapper_width=200) этот текст не может быть шире двухсот пикселей%%\n',
                title: 'wacko page wrapper',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 90,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'womMarkdown',
                            attributes: {
                                wrapper: 'page',
                                wrapper_width: '200',
                            },
                            format: 'wacko',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 89,
                                    column: 90,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'paragraph',
                                    position: {
                                        start: {
                                            offset: 41,
                                            column: 42,
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
                                                    offset: 41,
                                                    column: 42,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 87,
                                                    column: 88,
                                                    line: 1,
                                                },
                                            },
                                            value: 'этот текст не может быть шире двухсот пикселей',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '%%(markdown) `code` and **bold** %%\n',
                title: 'markdown page wrapper should parse inner markdown',
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
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'womMarkdown',
                            attributes: {
                            },
                            format: 'markdown',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 35,
                                    column: 36,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'paragraph',
                                    position: {
                                        start: {
                                            offset: 13,
                                            column: 14,
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
                                            type: 'inlineCode',
                                            position: {
                                                start: {
                                                    offset: 13,
                                                    column: 14,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 19,
                                                    column: 20,
                                                    line: 1,
                                                },
                                            },
                                            value: 'code',
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
                                                    offset: 24,
                                                    column: 25,
                                                    line: 1,
                                                },
                                            },
                                            value: ' and ',
                                        },
                                        {
                                            type: 'strong',
                                            position: {
                                                start: {
                                                    offset: 24,
                                                    column: 25,
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
                                                    type: 'text',
                                                    position: {
                                                        start: {
                                                            offset: 26,
                                                            column: 27,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 30,
                                                            column: 31,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: 'bold',
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
                markup: "%%(python nomark)\n@requires_authorization\ndef somefunc(param1, param2):\n    r'''A docstring'''\n%%\n",
                title: 'Пример 2: nomark',
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
                            line: 6,
                        },
                    },
                    children: [
                        {
                            type: 'womFormatter',
                            attributes: {
                                nomark: null,
                            },
                            format: 'python',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 97,
                                    column: 3,
                                    line: 5,
                                },
                            },
                            value: "@requires_authorization\ndef somefunc(param1, param2):\n    r'''A docstring'''\n",
                        },
                    ],
                },
            },
            {
                markup: "%%(Python)\nfrom django.contrib.auth.models import Group, Permission\ndifferent_users = Group(name='Different Users')\ndifferent_users.save()\n%%\n",
                title: 'Пример 3: язык с заглавной буквы',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 142,
                            column: 1,
                            line: 6,
                        },
                    },
                    children: [
                        {
                            type: 'womFormatter',
                            attributes: {
                            },
                            format: 'python',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 141,
                                    column: 3,
                                    line: 5,
                                },
                            },
                            value: "from django.contrib.auth.models import Group, Permission\ndifferent_users = Group(name='Different Users')\ndifferent_users.save()\n",
                        },
                    ],
                },
            },
            {
                markup: "%%(c++)\nfronm django.contrib.auth.models import Group, Permission\ndifferent_users = Group(name='Different Users')\ndifferent_users.save()\n%%\n",
                title: 'Пример 4: небуквенный символ в языке',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 140,
                            column: 1,
                            line: 6,
                        },
                    },
                    children: [
                        {
                            type: 'womFormatter',
                            attributes: {
                            },
                            format: 'c++',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 139,
                                    column: 3,
                                    line: 5,
                                },
                            },
                            value: "fronm django.contrib.auth.models import Group, Permission\ndifferent_users = Group(name='Different Users')\ndifferent_users.save()\n",
                        },
                    ],
                },
            },
            {
                markup: "%%(c)\nfrom django.contrib.auth.models import Group, Permission\ndifferent_users = Group(name='Different Users')\ndifferent_users.save()\n%%\n",
                title: 'Пример 5: язык из одного символа',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 137,
                            column: 1,
                            line: 6,
                        },
                    },
                    children: [
                        {
                            type: 'womFormatter',
                            attributes: {
                            },
                            format: 'c',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 136,
                                    column: 3,
                                    line: 5,
                                },
                            },
                            value: "from django.contrib.auth.models import Group, Permission\ndifferent_users = Group(name='Different Users')\ndifferent_users.save()\n",
                        },
                    ],
                },
            },
            {
                markup: '%%(123)\nvalue\n%%\n',
                title: 'Пример 6: язык из цифр',
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
                            type: 'womFormatter',
                            attributes: {
                            },
                            format: '123',
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
                            value: 'value\n',
                        },
                    ],
                },
            },
            {
                markup: '<{код функции\n%%(js)\nfunction is_pretty_num(n) { return 1; }\n%%\n}>\n',
                title: 'Cut с питон функцией',
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
                            column: 1,
                            line: 6,
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
                                                    offset: 13,
                                                    column: 14,
                                                    line: 1,
                                                },
                                            },
                                            value: 'код функции',
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
                                    offset: 66,
                                    column: 3,
                                    line: 5,
                                },
                            },
                            children: [
                                {
                                    type: 'womFormatter',
                                    attributes: {
                                    },
                                    format: 'js',
                                    inline: false,
                                    position: {
                                        start: {
                                            offset: 14,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 63,
                                            column: 3,
                                            line: 4,
                                        },
                                    },
                                    value: 'function is_pretty_num(n) { return 1; }\n',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '%%(math outline)\\int\\limits_{-\\infty}^{+\\infty} e^{-x^2/2} \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a} %%\n',
                title: 'math outline 1',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 97,
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
                                    offset: 97,
                                    column: 1,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'womFormatter',
                                    attributes: {
                                        outline: null,
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
                                            offset: 96,
                                            column: 97,
                                            line: 1,
                                        },
                                    },
                                    value: '\\int\\limits_{-\\infty}^{+\\infty} e^{-x^2/2} \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a} ',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '%%(math outline)\n\\alpha, \\beta, \\gamma, \\lambda, \\mu, \\omega, \\Gamma, \\Lambda, \\Omega\n%%\n',
                title: 'Греческие буквы',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 89,
                            column: 1,
                            line: 4,
                        },
                    },
                    children: [
                        {
                            type: 'womFormatter',
                            attributes: {
                                outline: null,
                            },
                            format: 'math',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 88,
                                    column: 3,
                                    line: 3,
                                },
                            },
                            value: '\\alpha, \\beta, \\gamma, \\lambda, \\mu, \\omega, \\Gamma, \\Lambda, \\Omega\n',
                        },
                    ],
                },
            },
            {
                markup: "%%(csv delimiter=; head='1')\nПараметр;Значение;Описание;Ага!\nПучеглазость; 0,5; Показывает степень удивления\nКрасноносость; средняя; Показывает температуру за дверью;ой\n%%\n",
                title: 'CSV formatter',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 172,
                            column: 1,
                            line: 6,
                        },
                    },
                    children: [
                        {
                            type: 'womFormatter',
                            attributes: {
                                delimiter: ';',
                                head: '1',
                            },
                            format: 'csv',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 171,
                                    column: 3,
                                    line: 5,
                                },
                            },
                            value: 'Параметр;Значение;Описание;Ага!\nПучеглазость; 0,5; Показывает степень удивления\nКрасноносость; средняя; Показывает температуру за дверью;ой\n',
                        },
                    ],
                },
            },
            {
                markup: '%\nfoo\n%\n',
                title: 'Блочный форматтер с одиним процентиком не работает',
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
                                            offset: 7,
                                            column: 2,
                                            line: 3,
                                        },
                                    },
                                    value: '%\nfoo\n%',
                                },
                            ],
                        },
                    ],
                },
            },
        ],
    },
];
