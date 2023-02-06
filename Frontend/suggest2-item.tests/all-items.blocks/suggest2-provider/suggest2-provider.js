/**
 * Мок для провайдера, всегда возвращающий статические данные
 */

BEM.decl('suggest2-provider', {

    _getCompletions: function(text) {
        if(!this._mockedCustomTypes) {
            this._weatherStates = [
                'bkn_+ra_d', 'bkn_+ra_n', 'bkn_+sn_d', 'bkn_+sn_n', 'bkn_-ra_d', 'bkn_-ra_n',
                'bkn_-sn_d', 'bkn_-sn_n', 'bkn_d', 'bkn_n', 'bkn_ra_d', 'bkn_ra_n', 'bkn_sn_d', 'bkn_sn_n', 'bl',
                'fg_d', 'fg_n', 'ovc', 'ovc_+ra', 'ovc_+sn', 'ovc_-ra', 'ovc_-sn', 'ovc_gr', 'ovc_ra',
                'ovc_sn', 'ovc_ts_ra', 'skc_d', 'skc_n'
            ];
            this._weatherFunc = function(el, ix) {
                return [
                    'weather',
                    'погода в Москве',
                    (-5 + ix) + ' °C',
                    el,
                    'pogoda.yandex.ru/moscow/',
                    {
                        hl: [[0, 6]]
                    }
                ];
            };
            this._mockedCustomTypes = {

                0: [
                    [
                        'nav',
                        'яндекс',
                        '«Яндекс.ru» — поисковая система',
                        'ok.ru',
                        'https://yandex.ru/clck/jsredir'
                    ]
                ],
                1: [
                    [
                        'nav',
                        'одноклассники',
                        '«Одноклассники.ru» — социальная сеть',
                        'ok.ru',
                        'https://yandex.ru/clck/jsredir'
                    ],
                    [
                        'nav',
                        'вконтакте',
                        '«Вконтакте.ru» — социальная сеть',
                        'vk.ru',
                        'https://yandex.ru/clck/jsredir'
                    ],
                    [
                        'nav',
                        'яндекс',
                        '«Яндекс.ru» — поисковая система',
                        'yandex.ru',
                        'https://yandex.ru/clck/jsredir'
                    ]
                ],
                2: [
                    [
                        '',
                        'персональная подсказка',
                        {
                            pers: 1
                        }
                    ],
                    [
                        'fact',
                        'персональная подсказка',
                        'с фактом',
                        {
                            hl: [[0, 8]],
                            pers: 1
                        }
                    ],
                    [
                        'traffic',
                        'пробки в москве',
                        '5 баллов ↑',
                        'red',
                        'http://maps.yandex.ru/moscow_traffic',
                        {
                            hl: [[0, 4]],
                            pers: 1
                        }
                    ]
                ],
                3: [
                    [
                        'fact',
                        'персональная подсказка',
                        'с фактом',
                        {
                            hl: [[0, 8]]
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
                    ]
                ],
                4: [
                    [
                        'html',
                        'html-тип',
                        {
                            body: '<span class=\"suggest2-item__text\">марш империи</span>'
                        }
                    ],
                    [
                        'html',
                        'html-тип',
                        {
                            body: '<span class="suggest2-item__text">' +
                            'şans topu loto sonuçları ' +
                            '<span class="suggest2-item__icon suggest2-item__icon_size_m" style=";' +
                            'background-image:url(&quot;//yastatic.net/suggest-flag-icons/icon.loto.2.png&quot;);' +
                            'margin: 0 2px;"></span>' +
                                '<span class="suggest2-item__icon suggest2-item__icon_size_m" style=";' +
                            'background-image:url(&quot;//yastatic.net/suggest-flag-icons/icon.loto.7.png&quot;);' +
                            'margin: 0 2px;"></span>' +
                                '<span class="suggest2-item__icon suggest2-item__icon_size_m" style=";' +
                            'background-image:url(&quot;//yastatic.net/suggest-flag-icons/icon.loto.12.png&quot;);' +
                            'margin: 0 2px;"></span>' +
                            '<span class="suggest2-item__icon suggest2-item__icon_size_m" style=";' +
                            'background-image:url(&quot;//yastatic.net/suggest-flag-icons/icon.loto.21.png&quot;);' +
                            'margin: 0 2px;"></span>' +
                            '<span class="suggest2-item__icon suggest2-item__icon_size_m" style=";' +
                            'background-image:url(&quot;//yastatic.net/suggest-flag-icons/icon.loto.28.png&quot;);' +
                            'margin: 0 2px;"></span>' +
                            ' + ' +
                            '<span class="suggest2-item__icon suggest2-item__icon_size_m" style=";' +
                            'background-image:url(&quot;//yastatic.net/suggest-flag-icons/icon.loto.7.png&quot;);' +
                            'margin: 0 2px;"></span>(5 Kasım)</span>'
                        }
                    ],
                    [
                        'html',
                        'html-тип',
                        {
                            body: '<span class="suggest2-item__text">по<b>год</b>а в Москве</span>' +
                                '<span class="suggest2-item__icon suggest2-item__icon_size_m ' +
                            'suggest2-item__icon_weather_bkn-d"></span>' +
                                '<span class="suggest2-item__fact">' +
                                    '<span class="suggest2__a11y">Быстрый ответ: </span>' +
                                    '-2 °C' +
                                '</span>'
                        }
                    ]
                ],
                5: [
                    [
                        'icon',
                        'флаг англии',
                        {
                            icon: [
                                [
                                    'svg',
                                    '//yastatic.net/suggest-flag-icons/icon.flag.england-16.svg'
                                ],
                                [
                                    'png',
                                    '//yastatic.net/suggest-flag-icons/icon.flag.england-16.png'
                                ]
                            ],
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
                                '//favicon.yandex.net/favicon/www.odnoklassniki.ru'
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
                                '//favicon.yandex.net/favicon/www.artlebedev.ru'
                            ],
                            fact: '«Хабрахабр» — социальное СМИ об IT, источник вдохновения, ' +
                            'прокрастинации и пиар–компаний'
                        }
                    ]
                ],
                6: [
                    [
                        'traffic',
                        'пробки в москве',
                        '5 баллов ↑',
                        'red',
                        'http://maps.yandex.ru/moscow_traffic',
                        {
                            hl: [[0, 4]]
                        }
                    ],
                    [
                        'traffic',
                        'пробки в белгороде',
                        '3 баллов ↑',
                        'yellow',
                        'http://maps.yandex.ru/moscow_traffic',
                        {
                            hl: [[0, 4]]
                        }
                    ],
                    [
                        'traffic',
                        'пробки в сургуте',
                        '0 баллов',
                        'green',
                        'http://maps.yandex.ru/moscow_traffic',
                        {
                            hl: [[0, 4]]
                        }
                    ]
                ],
                7: this._weatherStates.slice(0, 14).map(this._weatherFunc),
                8: this._weatherStates.slice(14, 28).map(this._weatherFunc),
                9: [
                    [
                        'bemjson',
                        'петр первый википедия',
                        {
                            url: 'http://ru.wikipedia.org/wiki/Пётр_I',
                            bemjson: [
                                {
                                    elem: 'text',
                                    elemMods: {
                                        type: 'title-url'
                                    },
                                    content: 'Пётр I — Википедия'
                                },
                                {
                                    elem: 'text',
                                    elemMods: {
                                        type: 'green-url'
                                    },
                                    content: 'ru.wikipedia.org/wiki/Пётр_I'
                                },
                                {
                                    elem: 'info',
                                    content: [
                                        {
                                            elem: 'img',
                                            elemMods: {
                                                size: 'm'
                                            },
                                            src: 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///' +
                                            'yH5BAEAAAAALAAAAAABAAEAAAIBRAA7'
                                        },
                                        {
                                            elem: 'desc',
                                            content: 'Последний царь всея Руси и первый Император Всероссийский.'
                                        }
                                    ]
                                }
                            ]
                        }
                    ],
                    [
                        'bemjson',
                        'текст для подстановки в инпут',
                        {
                            bemjson: {
                                elem: 'text',
                                content: 'текст подсказки'
                            }
                        }
                    ],
                    [
                        'bemjson',
                        'одноклассники',
                        {
                            bemjson: {
                                elem: 'text',
                                content: [
                                    'о',
                                    {
                                        elem: 'highlight',
                                        content: 'дно'
                                    },
                                    'классники'
                                ]
                            }
                        }
                    ],
                    [
                        'bemjson',
                        'яндекс',
                        {
                            url: 'https://yandex.ru',
                            bemjson: [
                                {
                                    elem: 'text',
                                    elemMods: {
                                        type: 'title-url'
                                    },
                                    content: 'Яндекс — поисковая система и интернет-портал'
                                },
                                {
                                    elem: 'text',
                                    elemMods: {
                                        type: 'green-url'
                                    },
                                    content: 'www.yandex.ru'
                                },
                                {
                                    elem: 'info',
                                    content: [
                                        {
                                            elem: 'thumb',
                                            content: {
                                                elem: 'thumb-wrapper',
                                                content: {
                                                    elem: 'thumb-img',
                                                    attrs: {
                                                        src: 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///' +
                                                        'yH5BAEAAAAALAAAAAABAAEAAAIBRAA7',
                                                        alt: 'Яндекс — поисковая система и интернет-портал'
                                                    }
                                                }
                                            }
                                        },
                                        {
                                            elem: 'desc',
                                            content: 'Поисковая система Yandex.ru'
                                        }
                                    ]
                                }
                            ]
                        }
                    ],
                    [
                        'bemjson',
                        '2.0',
                        {
                            bemjson: [
                                {
                                    elem: 'text',
                                    content: 'подсказка'
                                },
                                {
                                    elem: 'bullet'
                                },
                                {
                                    elem: 'fact',
                                    content: 'с фактом'
                                }
                            ]
                        }
                    ],
                    [
                        'bemjson',
                        '2.1',
                        {
                            bemjson: [
                                {
                                    elem: 'text',
                                    tag: 'u',
                                    content: 'тег <u> и произвольный цвет'
                                },
                                {
                                    elem: 'bullet'
                                },
                                {
                                    elem: 'fact',
                                    content: 'зелёный',
                                    color: '#007700'
                                }
                            ]
                        }
                    ],
                    [
                        'bemjson',
                        '2.2',
                        {
                            bemjson: [
                                {
                                    elem: 'text',
                                    content: 'произвольный цвет и курсив одновременно'
                                },
                                {
                                    elem: 'bullet'
                                },
                                {
                                    elem: 'fact',
                                    content: 'зелёный с курсивом',
                                    color: '#007700',
                                    attrs: {
                                        style: 'font-style: italic'
                                    }
                                }
                            ]
                        }
                    ]
                ],
                10: [
                    [
                        'bemjson',
                        'вконтакте',
                        {
                            url: 'https://vk.com',
                            bemjson: [
                                {
                                    elem: 'text',
                                    elemMods: {
                                        type: 'title-url'
                                    },
                                    content: '«ВКонтакте» — социальная сеть'
                                },
                                {
                                    elem: 'thumb',
                                    src: 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///' +
                                    'yH5BAEAAAAALAAAAAABAAEAAAIBRAA7',
                                    alt: '«ВКонтакте» — социальная сеть',
                                    bgColor: '#45759e'
                                }
                            ]
                        }
                    ],
                    [
                        'bemjson',
                        '3',
                        {
                            bemjson: [
                                {
                                    elem: 'text',
                                    content: 'погода в Москве'
                                },
                                {
                                    elem: 'icon',
                                    elemMods: {
                                        size: 'l',
                                        weather: 'bkn_d'
                                    }
                                },
                                {
                                    elem: 'fact',
                                    content: '-2 °C'
                                }
                            ]
                        }
                    ],
                    [
                        'bemjson',
                        '4',
                        {
                            bemjson: [
                                {
                                    elem: 'text',
                                    content: 'пробки в Москве'
                                },
                                {
                                    elem: 'icon',
                                    elemMods: {
                                        size: 'm',
                                        traffic: 'yellow'
                                    }
                                },
                                {
                                    elem: 'fact',
                                    content: '5 баллов ↑'
                                }
                            ]
                        }
                    ],
                    [
                        'bemjson',
                        '5',
                        {
                            url: '//yandex.ru',
                            bemjson: [
                                {
                                    elem: 'text',
                                    elemMods: {
                                        type: 'url'
                                    },
                                    content: 'синий подчёркнутый текст'
                                }
                            ]
                        }
                    ],
                    [
                        'bemjson',
                        '6',
                        {
                            bemjson: [
                                {
                                    elem: 'text',
                                    attrs: {
                                        style: 'color: blueviolet'
                                    },
                                    content: 'фиолетовая подсказка'
                                }
                            ]
                        }
                    ],
                    [
                        'bemjson',
                        '',
                        {
                            bemjson: [
                                {
                                    elem: 'fact',
                                    content: 'История ваших поисков не сохраняется. '
                                },
                                {
                                    elem: 'text',
                                    elemMods: {
                                        type: 'url'
                                    },
                                    content: 'Сохранять'
                                },
                                {
                                    tag: 'img',
                                    attrs: {
                                        src: 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///' +
                                        'yH5BAEAAAAALAAAAAABAAEAAAIBRAA7'
                                    }
                                }
                            ],
                            url: 'https://passport.yandex-team.ru/passport?mode=passport',
                            target: '_self'
                        }
                    ],
                    [
                        'bemjson',
                        'Служба спасения',
                        {
                            bemjson: [
                                {
                                    elem: 'icon',
                                    elemMods: {
                                        size: 's'
                                    },
                                    svg: 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///' +
                                    'yH5BAEAAAAALAAAAAABAAEAAAIBRAA7',
                                    attrs: {
                                        style: 'margin-left: 0'
                                    }
                                },
                                {
                                    elem: 'text',
                                    content: '112'
                                },
                                {
                                    elem: 'bullet'
                                },
                                {
                                    elem: 'fact',
                                    content: 'Служба спасения'
                                }
                            ],
                            url: 'tel:112'
                        }
                    ],
                    [
                        'bemjson',
                        'вызов такси',
                        {
                            bemjson: [
                                {
                                    block: 'button2',
                                    mods: {size: 's', theme: 'action'},
                                    url: '#',
                                    text: 'Вызвать такси сейчас'
                                }
                            ],
                            label: 'Без заморочек'
                        }
                    ]
                ]

            };
        }

        return this._mockedCustomTypes[text];
    },

    get: function(text, pos, callback) {
        var self = this,
            completions = this._getCompletions(pos);

        this.afterCurrentEvent(function() {
            callback.call(self, text, pos, {
                orig: text,
                items: completions,
                meta: null
            });
        });
    }
});
