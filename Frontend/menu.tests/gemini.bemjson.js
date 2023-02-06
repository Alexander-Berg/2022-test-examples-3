({
    block: 'b-page',
    title: 'Блок menu',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        ['xs', 's', 'm'].map(function(size) {
            return [
                {
                    cls: 'test-size-' + size,
                    block: 'test',
                    content: {
                        block: 'menu',
                        mods: {theme: 'normal', size: size},
                        items: [
                            {
                                title: 'Заголовок',
                                items: ['Каждый', 'Охотник']
                            },
                            'menu_item+menu_group',
                            {
                                items: ['Желает']
                            },
                            {
                                title: 'Заголовок',
                                items: ['Знать', 'Где', 'Сидит']
                            },
                            'Фазан'
                        ]
                    }
                }
            ];
        }),
        ['xs', 's', 'm'].map(function(size) {
            return [
                {
                    cls: 'test-icon-size-' + size,
                    block: 'test',
                    content: {
                        block: 'menu',
                        mods: {theme: 'normal', size: size},
                        items: [
                            {
                                title: 'Страна',
                                items: [
                                    {
                                        icon: {block: 'icon', mods: {type: 'ru'}},
                                        text: 'Россия'
                                    },
                                    {icon: {block: 'icon', mods: {type: 'by'}}}
                                ]
                            },
                            {icon: {block: 'icon', mods: {type: 'kz'}}}
                        ]
                    }
                }
            ];
        }),
        {
            cls: 'test-check',
            block: 'test',
            content: {
                block: 'menu',
                mods: {type: 'check', theme: 'normal', size: 's'},
                val: [2, 3],
                items: [
                    'Каждый',
                    {text: 'Охотник', val: 2},
                    {text: 'Желает', val: 3}
                ]
            }
        },
        {
            cls: 'test-navigation',
            block: 'test',
            content: {
                block: 'menu',
                mods: {type: 'navigation', theme: 'normal', size: 's'},
                items: [
                    {text: 'Поиск', url: 'https://yandex.ru/search/'},
                    {text: 'Картинки', url: 'https://yandex.ru/images/'},
                    {text: 'Видео'}
                ]
            }
        },
        {
            cls: 'test-disabled',
            block: 'test',
            content: {
                block: 'menu',
                mods: {type: 'radio', theme: 'normal', size: 's', disabled: 'yes'},
                items: ['Каждый', 'Охотник', 'Желает']
            }
        },
        {
            cls: 'test-disabled-item',
            block: 'test',
            content: {
                block: 'menu',
                mods: {type: 'radio', theme: 'normal', size: 's'},
                items: [
                    'Каждый',
                    {text: 'Охотник', elemMods: {disabled: 'yes'}},
                    'Желает'
                ]
            }
        },
        {
            cls: 'test-width-auto',
            block: 'test',
            content: {
                block: 'menu',
                mods: {theme: 'normal', size: 's', width: 'auto'},
                items: ['Каждый', 'Охотник', 'Желает']
            }
        },
        {
            cls: 'test-width-max',
            block: 'test',
            attrs: {style: 'width:200px'},
            content: {
                block: 'menu',
                mods: {theme: 'normal', size: 's', width: 'max'},
                items: ['Каждый', 'Охотник', 'Желает']
            }
        },
        ['xs', 's', 'm'].map(function(size) {
            return [
                {
                    cls: 'test-icon-size-' + size + '-radio',
                    block: 'test',
                    content: {
                        block: 'menu',
                        mods: {type: 'radio', theme: 'normal', size: size},
                        val: 'ru',
                        items: [
                            {
                                title: 'Страна',
                                items: [
                                    {
                                        val: 'ru',
                                        icon: {block: 'icon', mods: {type: 'ru'}},
                                        text: 'Россия'
                                    },
                                    {val: 'by', icon: {block: 'icon', mods: {type: 'by'}}}
                                ]
                            },
                            {val: 'kz', icon: {block: 'icon', mods: {type: 'kz'}}}
                        ]
                    }
                }
            ];
        }),
        ['xs', 's', 'm', 'n'].map(function(size, i) {
            var tones = ['default', 'red', 'grey', 'dark'];

            return [
                {
                    cls: 'test-default-size-' + size + '-radio',
                    block: 'test',
                    mix: {block: 'test', mods: {view: 'default'}},
                    content: {
                        block: 'menu',
                        mods: {
                            type: 'radio',
                            theme: 'normal',
                            view: 'default',
                            tone: tones[i],
                            size: size
                        },
                        val: 'ru',
                        items: [
                            {
                                title: 'Страна',
                                items: [
                                    {
                                        val: 'ru',
                                        icon: {block: 'icon', mods: {type: 'ru'}},
                                        text: 'Россия'
                                    },
                                    {val: 'by', icon: {block: 'icon', mods: {type: 'by'}}}
                                ]
                            },
                            {val: 'kz', icon: {block: 'icon', mods: {type: 'kz'}}}
                        ]
                    }
                }
            ];
        }),
        {
            elem: 'cc',
            condition: 'IE 8',
            content: {elem: 'js', url: 'https://yastatic.net/es5-shims/0.0.1/es5-shims.min.js'}
        },
        {
            elem: 'cc',
            condition: 'IE 8',
            content: {block: 'i-jquery', mods: {version: '1.8.3'}}
        },
        {
            elem: 'cc',
            condition: 'gt IE 8',
            others: true,
            content: {block: 'i-jquery', mods: {version: 'default'}}
        },
        {elem: 'js', url: '_gemini.js'}
    ]
});
