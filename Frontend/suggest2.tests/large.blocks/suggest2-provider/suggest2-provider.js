/**
 * Мок для провайдера, всегда возвращающий статические данные
 */

BEM.decl('suggest2-provider', {

    get: function(text, pos, callback) {
        var self = this,
            completions = (text === 'тест тест тест') ? [
                [
                    '',
                    'подсказка',
                    {pers: 1}
                ],
                [
                    'fact',
                    'подсказка',
                    'с фактом',
                    {
                        hl: [[0, 8]]
                    }
                ],
                'тире: &mdash;',
                [
                    'weather',
                    'Москва',
                    '-2 °C',
                    'bkn_d',
                    'pogoda.yandex.ru/moscow/',
                    {
                        hl: [[2, 5]]
                    }
                ],
                [
                    'traffic',
                    'пробки',
                    '5 ↑',
                    'yellow',
                    'http://maps.yandex.ru/moscow_traffic',
                    {
                        hl: [[0, 4]]
                    }
                ]
            ] : [
                [
                    '',
                    'длинная персональная подсказка простая фиолетовая и эллипсисом в конце',
                    {pers: 1}
                ],
                [
                    'fact',
                    'персональная подсказка',
                    'с фактом',
                    {
                        hl: [[0, 8]]
                    }
                ],
                'очень длинная подсказка, которая должна обрезаться в конце многоточием, если не влезает по ширине',
                'спецсимвол длинного тире: &mdash;',
                [
                    'weather',
                    'погода в Москве',
                    '-2 °C',
                    'bkn_d',
                    'pogoda.yandex.ru/moscow/',
                    {
                        hl: [[2, 5]]
                    }
                ],
                [
                    'weather',
                    'погода на Юпитере, Уране, Венере и Сатурне во время парада планет',
                    'ужасные молнии и ураганы',
                    'ovc_ts_ra'
                ],
                [
                    'nav',
                    'навигационная подсказка',
                    'описание навигационной подсказки',
                    'www.yandex.ru',
                    'https://yandex.ru',
                    {
                        hl: [[0, 4]]
                    }
                ],
                [
                    'fact',
                    'html неразрывный пробел',
                    '&nbsp;',
                    {
                        hl: [[0, 4]]
                    }
                ],
                [
                    'fact',
                    'первое начало термодинамики',
                    'в любой изолированной системе запас энергии остаётся постоянным'
                ],
                [
                    'icon',
                    'иконка флага Албании',
                    {
                        icon: [[
                            'svg',
                            '//favicon.yandex.net/favicon/www.yandex.com'
                        ],
                        [
                            'png',
                            '//favicon.yandex.net/favicon/www.yandex.com'
                        ]],
                        hl: [[0, 4]]
                    }
                ],
                [
                    'icon',
                    'флаг Англии',
                    {
                        icon: [[
                            'svg',
                            '//favicon.yandex.net/favicon/www.yandex.com'
                        ],
                        [
                            'png',
                            '//favicon.yandex.net/favicon/www.yandex.com'
                        ]],
                        fact: 'см. так же флаг Великобритании'
                    }
                ],
                [
                    'icon',
                    'www.odnoklassniki.ru',
                    {
                        url: 'odnoklassniki.ru',
                        icon: [
                            'png',
                            '//favicon.yandex.net/favicon/www.yandex.com'
                        ],
                        fact: 'Иконка с ссылкой',
                        hl: [[8, 13]]
                    }
                ],
                [
                    'icon',
                    'habrahabr.ru',
                    {
                        url: 'http://habrahabr.ru/company/yandex/?a=1&b=2',
                        icon: [
                            'png',
                            '//favicon.yandex.net/favicon/www.yandex.com'
                        ],
                        fact: '«Хабрахабр» — социальное СМИ об IT, источник вдохновения,' +
                        ' прокрастинации и пиар–компаний'
                    }
                ],
                [
                    '',
                    'выделенный текст в подсказке',
                    {
                        hl: [[0, 16], [22, 26]]
                    }
                ],
                ['bemjson', 'şans topu loto sonuçları', {bemjson: [
                    {elem: 'text', content: [
                        'şans topu loto sonuçları ',
                        {
                            elem: 'icon',
                            elemMods: {size: 'm'},
                            png: '//yastatic.net/suggest-flag-icons/icon.loto.2.png',
                            attrs: {style: 'margin: 0 2px;'}
                        },
                        {
                            elem: 'icon',
                            elemMods: {size: 'm'},
                            png: '//yastatic.net/suggest-flag-icons/icon.loto.7.png',
                            attrs: {style: 'margin: 0 2px;'}
                        },
                        {
                            elem: 'icon',
                            elemMods: {size: 'm'},
                            png: '//yastatic.net/suggest-flag-icons/icon.loto.12.png',
                            attrs: {style: 'margin: 0 2px;'}
                        },
                        {
                            elem: 'icon',
                            elemMods: {size: 'm'},
                            png: '//yastatic.net/suggest-flag-icons/icon.loto.21.png',
                            attrs: {style: 'margin: 0 2px;'}
                        },
                        {
                            elem: 'icon',
                            elemMods: {size: 'm'},
                            png: '//yastatic.net/suggest-flag-icons/icon.loto.28.png',
                            attrs: {style: 'margin: 0 2px;'}
                        },
                        ' + ',
                        {
                            elem: 'icon',
                            elemMods: {size: 'm'},
                            png: '//yastatic.net/suggest-flag-icons/icon.loto.7.png',
                            attrs: {style: 'margin: 0 2px;'}
                        },
                        '(5 Kasım)'
                    ]}
                ]}],
                [
                    'traffic',
                    'пробки в Москве',
                    '5 баллов ↑',
                    'yellow',
                    'http://maps.yandex.ru/moscow_traffic',
                    {
                        hl: [[0, 4]]
                    }
                ]
            ];

        this.afterCurrentEvent(function() {
            callback.call(self, text, pos, {
                orig: text,
                items: completions,
                meta: null
            });
        });
    }
});
