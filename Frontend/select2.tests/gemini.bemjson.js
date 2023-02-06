({
    block: 'b-page',
    title: 'select2',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    attrs: {style: 'padding:20px'},
    content: [
        ['xs', 's', 'm'].map(function(size) {
            return {
                block: 'test',
                content: {
                    cls: 'test-size-' + size,
                    block: 'select2',
                    mods: {
                        theme: 'normal',
                        size: size
                    },
                    val: 'orange',
                    items: [
                        {val: 'red', text: 'Каждый'},
                        {val: 'orange', text: 'Охотник'}
                    ]
                }
            };
        }),

        {tag: 'div', attrs: {style: 'height:70px'}},

        ['visible', 'hidden'].map(function(type) {
            return {
                block: 'test',
                content: {
                    cls: 'test-icon-' + type,
                    block: 'select2',
                    mods: {
                        type: 'radio',
                        theme: 'normal',
                        size: 's',
                        text: 'vary',
                        'with-icon': 'yes',
                        'item-icon-hidden': type === 'hidden' ? 'yes' : undefined
                    },
                    val: 'knife',
                    items: [
                        {
                            title: 'Наиболее вероятное',
                            items: [
                                {val: 'knife', icon: {block: 'icon', mods: {type: 'download'}}, text: 'Нож охотничий'},
                                {val: 'axe', icon: {block: 'icon', mods: {type: 'print'}}, text: 'Топор'},
                                {
                                    val: 'ruler',
                                    icon: {block: 'icon', mods: {type: 'play'}},
                                    text: 'Металлическая линейка'
                                }
                            ]
                        },
                        {
                            title: 'Наименее',
                            items: [
                                {val: 'fork', icon: {block: 'icon', mods: {type: 'camera'}}, text: 'Вилка'},
                                {val: 'brush', icon: {block: 'icon', mods: {type: 'trash'}}, text: 'Зубная щётка'},
                                {val: 'corkscrew', text: 'Штопор'},
                                {val: 'pliers', text: 'Плоскогубцы'}
                            ]
                        }
                    ]
                }
            };
        }),

        {
            block: 'x-deps',
            content: [
                {block: 'icon', mods: {type: 'download'}},
                {block: 'icon', mods: {glyph: 'type-print'}},
                {block: 'icon', mods: {type: 'print'}},
                {block: 'icon', mods: {type: 'play'}},
                {block: 'icon', mods: {type: 'camera'}},
                {block: 'icon', mods: {type: 'trash'}},
                ['default', 'red', 'grey', 'dark'].map(tone => [
                    {block: 'button2', mods: {tone}},
                    {block: 'popup2', mods: {tone}},
                    {block: 'menu', mods: {tone}}
                ]),
                ['menu', 'popup2'].map(block => ({block, mods: {view: 'default'}})),
                ['option', 'link'].map(type => ({block: 'menu', elem: 'item', mods: {type}})),
                ['group', 'text', 'icon'].map(elem => ({block: 'menu', ...{elem}}))
            ]
        },
        {tag: 'div', attrs: {style: 'height:70px'}},

        {
            block: 'gemini',
            mix: {block: 'test'},
            attrs: {style: 'width:120px'},
            content: {
                cls: 'test-width-auto',
                block: 'select2',
                mods: {
                    theme: 'normal',
                    size: 's'
                },
                items: [
                    {val: 'red', text: 'Каждый'},
                    {val: 'orange', text: 'Охотник'}
                ]
            }
        },
        {
            block: 'test',
            content: {
                cls: 'test-width-max',
                block: 'select2',
                mods: {
                    theme: 'normal',
                    size: 's',
                    width: 'max'
                },
                items: [
                    {val: 'red', text: 'Каждый'},
                    {val: 'orange', text: 'Охотник'}
                ]
            }
        },
        {tag: 'div', attrs: {style: 'height:70px'}},
        {
            block: 'gemini',
            mix: {block: 'test'},
            attrs: {style: 'width:120px'},
            content: {
                cls: 'test-width-fixed',
                block: 'select2',
                // TODO: https://st.yandex-team.ru/ISL-4323
                attrs: {style: 'width:78px'},
                mods: {
                    theme: 'normal',
                    size: 's',
                    width: 'fixed'
                },
                items: [
                    {val: 'red', text: 'Каждый'},
                    {val: 'orange', text: 'Охотник'}
                ]
            }
        },

        {tag: 'div', attrs: {style: 'height:70px'}},

        {
            block: 'test',
            content: {
                cls: 'test-height',
                block: 'select2',
                mods: {
                    type: 'check',
                    theme: 'normal',
                    size: 's',
                    text: 'vary'
                },
                js: {
                    optionsMaxHeight: 25
                },
                items: [
                    {val: 'red', text: 'Каждый'},
                    {val: 'orange', text: 'Охотник'}
                ]
            }
        },
        {tag: 'div', attrs: {style: 'height:70px'}},
        {
            block: 'gemini',
            mix: {block: 'test'},
            attrs: {style: 'width:120px'},
            content: {
                cls: 'test-checked',
                block: 'select2',
                control: true,
                mods: {
                    type: 'check',
                    theme: 'normal',
                    size: 's',
                    text: 'vary',
                    width: 'fixed'
                },
                // TODO: https://st.yandex-team.ru/ISL-4323
                attrs: {style: 'width:98px'},
                items: [
                    {val: 'red', text: 'Каждый'},
                    {val: 'orange', text: 'Охотник'}
                ]
            }
        },
        {tag: 'div', attrs: {style: 'height:200px'}},

        ['xs', 's', 'gap', 'm', 'n'].map(function(size, i) {
            var tones = ['default', 'red', 'default', 'grey', 'dark'];
            if(size === 'gap') {
                return {tag: 'div', attrs: {style: 'height:200px'}};
            }

            return {
                block: 'gemini',
                mix: {block: 'test'},
                attrs: {style: 'width:250px'},
                content: {
                    cls: 'test-default-' + size,
                    block: 'select2',
                    mods: {
                        type: 'radio',
                        theme: 'normal',
                        size: size,
                        view: 'default',
                        tone: tones[i],
                        text: 'vary',
                        'with-icon': 'yes'
                    },
                    val: 'knife',
                    items: [
                        {
                            title: 'Наиболее вероятное',
                            items: [
                                {
                                    val: 'knife',
                                    icon: {block: 'icon', mods: {glyph: 'type-print'}},
                                    text: 'Нож охотничий'
                                },
                                {val: 'axe', icon: {block: 'icon', mods: {type: 'print'}}, text: 'Топор'},
                                {
                                    val: 'ruler',
                                    icon: {block: 'icon', mods: {type: 'play'}},
                                    text: 'Металлическая линейка'
                                }
                            ]
                        },
                        {
                            title: 'Наименее',
                            items: [
                                {val: 'fork', icon: {block: 'icon', mods: {type: 'camera'}}, text: 'Вилка'},
                                {val: 'brush', icon: {block: 'icon', mods: {type: 'trash'}}, text: 'Зубная щётка'},
                                {val: 'corkscrew', text: 'Штопор'},
                                {val: 'pliers', text: 'Плоскогубцы'}
                            ]
                        }
                    ]
                }
            };
        }),

        {tag: 'br'},

        {
            block: 'gemini',
            mix: {block: 'test-default-check'}, // mix for react
            content: ['default', 'red', 'grey', 'dark'].map(function(tone) {
                return {
                    block: 'toner',
                    mods: {tone: tone},
                    attrs: {
                        style: 'display:inline-block;padding:10px;'
                    },
                    content: {
                        block: 'select2',
                        mods: {
                            type: 'check',
                            theme: 'normal',
                            size: 'n',
                            view: 'default',
                            tone: tone,
                            text: 'vary',
                            'with-icon': 'yes'
                        },
                        val: ['red', 'violet'],
                        items: [
                            {val: 'red', text: 'Каждый'},
                            {val: 'orange', text: 'Охотник'},
                            {val: 'yellow', text: 'Желает'},
                            {val: 'green', text: 'Знать'},
                            {val: 'lightblue', text: 'Где'},
                            {val: 'blue', text: 'Сидит'},
                            {val: 'violet', text: 'Фазан'}
                        ]
                    }
                };
            })
        },
        {
            elem: 'cc',
            condition: 'IE 8',
            content: [
                {elem: 'js', url: 'https://yastatic.net/es5-shims/0.0.1/es5-shims.min.js'},
                {block: 'i-jquery', mods: {version: '1.8.3'}}
            ]
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
