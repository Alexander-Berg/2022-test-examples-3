module.exports = [
    {
        group: 'womHtml {[]}',
        tests: [
            {
                markup: 'foo <##>test<##>\n',
                title: 'Форматтеры не должны схлопываться (inline)',
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
                                    value: 'foo ',
                                },
                                {
                                    type: 'womHtml',
                                    inline: true,
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
                                    value: '',
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
                                            offset: 12,
                                            column: 13,
                                            line: 1,
                                        },
                                    },
                                    value: 'test',
                                },
                                {
                                    type: 'womHtml',
                                    inline: true,
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
                                    value: '',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '<##>\ntest\n<##>\n',
                title: 'Форматтеры не должны схлопываться (block)',
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
                            line: 4,
                        },
                    },
                    children: [
                        {
                            type: 'womHtml',
                            inline: false,
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
                            value: '',
                        },
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 5,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 10,
                                    column: 1,
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
                                            line: 2,
                                        },
                                        end: {
                                            offset: 9,
                                            column: 5,
                                            line: 2,
                                        },
                                    },
                                    value: 'test',
                                },
                            ],
                        },
                        {
                            type: 'womHtml',
                            inline: false,
                            position: {
                                start: {
                                    offset: 10,
                                    column: 1,
                                    line: 3,
                                },
                                end: {
                                    offset: 14,
                                    column: 5,
                                    line: 3,
                                },
                            },
                            value: '',
                        },
                    ],
                },
            },
            {
                markup: '<##>\nhttps://staff.yandex-team.ru/departments/yandex_search_interface_libs/\n<##>\n',
                title: 'WIKI-12833',
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
                            line: 4,
                        },
                    },
                    children: [
                        {
                            type: 'womHtml',
                            inline: false,
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
                            value: '',
                        },
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 5,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 76,
                                    column: 1,
                                    line: 3,
                                },
                            },
                            children: [
                                {
                                    type: 'link',
                                    title: null,
                                    position: {
                                        start: {
                                            offset: 5,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 75,
                                            column: 71,
                                            line: 2,
                                        },
                                    },
                                    url: 'https://staff.yandex-team.ru/departments/yandex_search_interface_libs/',
                                    children: [
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 5,
                                                    column: 1,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 75,
                                                    column: 71,
                                                    line: 2,
                                                },
                                            },
                                            value: 'https://staff.yandex-team.ru/departments/yandex_search_interface_libs/',
                                        },
                                    ],
                                },
                            ],
                        },
                        {
                            type: 'womHtml',
                            inline: false,
                            position: {
                                start: {
                                    offset: 76,
                                    column: 1,
                                    line: 3,
                                },
                                end: {
                                    offset: 80,
                                    column: 5,
                                    line: 3,
                                },
                            },
                            value: '',
                        },
                    ],
                },
            },
            {
                markup: 'Hello: <# <input type="text"> #>, — done.\n',
                title: 'Вывод HTML как есть',
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
                                    offset: 42,
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
                                    value: 'Hello: ',
                                },
                                {
                                    type: 'womHtml',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 7,
                                            column: 8,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 32,
                                            column: 33,
                                            line: 1,
                                        },
                                    },
                                    value: '<input type="text">',
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
                                            offset: 41,
                                            column: 42,
                                            line: 1,
                                        },
                                    },
                                    value: ', — done.',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: "ABCDEFG: <# <span style='color:gray'> #>-0 (GRAY)<# </span> #>\n++\"\"[\"\"<# <span style='color:auto'> #>v1<# </span> #>  <# <span style='color:auto'> #>Δ=0<# </span> #>]++\n",
                title: 'Inline HTML как препятствие',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 169,
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
                                    offset: 169,
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
                                            offset: 9,
                                            column: 10,
                                            line: 1,
                                        },
                                    },
                                    value: 'ABCDEFG: ',
                                },
                                {
                                    type: 'womHtml',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 9,
                                            column: 10,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 40,
                                            column: 41,
                                            line: 1,
                                        },
                                    },
                                    value: "<span style='color:gray'>",
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 40,
                                            column: 41,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 49,
                                            column: 50,
                                            line: 1,
                                        },
                                    },
                                    value: '-0 (GRAY)',
                                },
                                {
                                    type: 'womHtml',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 49,
                                            column: 50,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 62,
                                            column: 63,
                                            line: 1,
                                        },
                                    },
                                    value: '</span>',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 62,
                                            column: 63,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 63,
                                            column: 1,
                                            line: 2,
                                        },
                                    },
                                    value: '\n',
                                },
                                {
                                    type: 'womSmall',
                                    position: {
                                        start: {
                                            offset: 63,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 168,
                                            column: 106,
                                            line: 2,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'womEscape',
                                            position: {
                                                start: {
                                                    offset: 65,
                                                    column: 3,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 70,
                                                    column: 8,
                                                    line: 2,
                                                },
                                            },
                                            raw: '""[""',
                                            value: '[',
                                        },
                                        {
                                            type: 'womHtml',
                                            inline: true,
                                            position: {
                                                start: {
                                                    offset: 70,
                                                    column: 8,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 101,
                                                    column: 39,
                                                    line: 2,
                                                },
                                            },
                                            value: "<span style='color:auto'>",
                                        },
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 101,
                                                    column: 39,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 103,
                                                    column: 41,
                                                    line: 2,
                                                },
                                            },
                                            value: 'v1',
                                        },
                                        {
                                            type: 'womHtml',
                                            inline: true,
                                            position: {
                                                start: {
                                                    offset: 103,
                                                    column: 41,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 116,
                                                    column: 54,
                                                    line: 2,
                                                },
                                            },
                                            value: '</span>',
                                        },
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 116,
                                                    column: 54,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 118,
                                                    column: 56,
                                                    line: 2,
                                                },
                                            },
                                            value: '  ',
                                        },
                                        {
                                            type: 'womHtml',
                                            inline: true,
                                            position: {
                                                start: {
                                                    offset: 118,
                                                    column: 56,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 149,
                                                    column: 87,
                                                    line: 2,
                                                },
                                            },
                                            value: "<span style='color:auto'>",
                                        },
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 149,
                                                    column: 87,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 152,
                                                    column: 90,
                                                    line: 2,
                                                },
                                            },
                                            value: 'Δ=0',
                                        },
                                        {
                                            type: 'womHtml',
                                            inline: true,
                                            position: {
                                                start: {
                                                    offset: 152,
                                                    column: 90,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 165,
                                                    column: 103,
                                                    line: 2,
                                                },
                                            },
                                            value: '</span>',
                                        },
                                        {
                                            type: 'text',
                                            position: {
                                                start: {
                                                    offset: 165,
                                                    column: 103,
                                                    line: 2,
                                                },
                                                end: {
                                                    offset: 166,
                                                    column: 104,
                                                    line: 2,
                                                },
                                            },
                                            value: ']',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: "<#\n<div style='color:#777'></div>\n#>\n<#\n<div style='color:#777'></div>\n#>\n",
                title: 'HTML препятствия с # внутри',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 74,
                            column: 1,
                            line: 7,
                        },
                    },
                    children: [
                        {
                            type: 'womHtml',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 36,
                                    column: 3,
                                    line: 3,
                                },
                            },
                            value: "<div style='color:#777'></div>",
                        },
                        {
                            type: 'womHtml',
                            inline: false,
                            position: {
                                start: {
                                    offset: 37,
                                    column: 1,
                                    line: 4,
                                },
                                end: {
                                    offset: 73,
                                    column: 3,
                                    line: 6,
                                },
                            },
                            value: "<div style='color:#777'></div>",
                        },
                    ],
                },
            },
            {
                markup: "<#\n<div style='color:#777'></div>\n<div style='color:#777'></div>\n#>\n",
                title: 'Двойные HTML препятствия с # внутри',
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
                            line: 5,
                        },
                    },
                    children: [
                        {
                            type: 'womHtml',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 67,
                                    column: 3,
                                    line: 4,
                                },
                            },
                            value: "<div style='color:#777'></div>\n<div style='color:#777'></div>",
                        },
                    ],
                },
            },
            {
                markup: '<# <table border=1> <tr><td>1</td><td>2</td></tr> <tr><td>3</td><td>4</td></tr> </table> #>\n',
                title: 'Таблица из html (feat html)',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 92,
                            column: 1,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'womHtml',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 91,
                                    column: 92,
                                    line: 1,
                                },
                            },
                            value: '<table border=1> <tr><td>1</td><td>2</td></tr> <tr><td>3</td><td>4</td></tr> </table>',
                        },
                    ],
                },
            },
            {
                markup: '<# <br/> #>\n',
                title: 'Обрезка пробелов 1',
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
                            type: 'womHtml',
                            inline: false,
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
                            value: '<br/>',
                        },
                    ],
                },
            },
            {
                markup: '<#  <br/>  #>\n',
                title: 'Обрезка пробелов 2',
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
                            type: 'womHtml',
                            inline: false,
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
                            value: '<br/>',
                        },
                    ],
                },
            },
            {
                markup: '<#  <br/>   #>\n',
                title: 'Обрезка пробелов 3',
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
                            type: 'womHtml',
                            inline: false,
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
                            value: '<br/>',
                        },
                    ],
                },
            },
        ],
    },
];
