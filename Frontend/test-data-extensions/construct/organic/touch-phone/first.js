var image = require('@yandex-int/gemini-serp-stubs').imageStub(150, 150);

module.exports = {
    type: 'snippet',
    data_stub: {
        num: 0,
        construct: {
            type: 'test',
            template: 'test',
            blocks: [
                {
                    block: 'organic',
                    favicon: { domain: 'ya.ru' },
                    title: {
                        text: 'Развлечение сознания'
                    },
                    url: {
                        url: '//ya.ru'
                    },
                    hint: {
                        text: 'на каждый см⁴'
                    },
                    path: [
                        { text: 'yandex.ru' }
                    ],
                    extralinks: [],
                    label: {
                        color: 'red',
                        text: 'хорошо!'
                    },
                    marker: 'моб. версия',
                    thumb: { cols: 3, image: image },
                    list: [
                        'Хорошо разогнаться',
                        'Выдернуть шнур',
                        'Выдавить стекло',
                        '????????',
                        'PROFIT!'
                    ],
                    meta: [
                        ['Полить цветы', 'Погладить кота']
                    ]
                }
            ]
        }
    }
};
