({
    block: 'b-page',
    title: 'input',
    head: [
        {elem: 'css', url: '_popup.css', ie: false},
        {elem: 'css', url: '_popup', ie: true}
    ],
    content: [
        {
            block: 'gemini-common',
            mix: {block: 'popups'},
            content: [
                ['right', 'left', 'bottom', 'top'].map(function(side) {
                    return {
                        block: 'gemini-suggest-in-popup',
                        mix: {block: 'gemini-popup-' + side},
                        js: true,
                        content: [
                            {
                                block: 'button',
                                mods: {size: 'm', theme: 'normal'},
                                content: side
                            },
                            {
                                block: 'popup',
                                mods: {'gemini-container': 'yes', 'gemini-side': side},
                                attrs: {style: 'width: 220px'},
                                js: {
                                    directions: [(side === 'right' || side === 'left' ? side + '-top' : side)]
                                },
                                content: [
                                    {elem: 'tail'},
                                    {
                                        elem: 'content',
                                        mix: [
                                            {block: 'gemini-suggest-in-popup', elem: 'popup-content'}
                                        ],
                                        content: [
                                            {
                                                block: 'input',
                                                mods: {suggest: 'yes', size: 'm'},
                                                js: {
                                                    dataprovider: {
                                                        name: 'mock-data'
                                                    },
                                                    popupMods: {test: 'popup-' + side}
                                                },
                                                placeholder: 'Скажи "А"',
                                                content: [
                                                    {elem: 'control', attrs: {name: 'autocomplete'}}
                                                ]
                                            },
                                            {
                                                tag: 'p',
                                                content: [
                                                    'При выборе подсказки в саджесте, основной' +
                                                    ' попап не должен закрываться'
                                                ]
                                            }
                                        ]
                                    }
                                ]
                            }
                        ]
                    };
                }),
                {tag: 'br'},
                {tag: 'br'},

                {
                    block: 'gemini-suggest-foot',
                    content: {
                        block: 'input',
                        js: {
                            dataprovider: {
                                name: 'mock-data'
                            },
                            popupMods: {
                                size: 'm',
                                test: 'suggest-foot',
                                fade: 'yes',
                                gradient: 'yes'
                            },
                            foot: [
                                'foot', 'тут слово <a class="link" href="ya.ru">Мои находки</a>'
                            ]
                        },
                        mods: {suggest: 'yes', size: 'm'},
                        content: {
                            elem: 'control',
                            attrs: {name: 'autocomplete'}
                        }

                    }
                },
                {tag: 'br'},
                {tag: 'br'},

                {
                    block: 'gemini-big-suggest',
                    content: {
                        block: 'input',
                        mods: {suggest: 'yes', size: 'm'},
                        js: {
                            dataprovider: {
                                name: 'mock-data'
                            },
                            popupMods: {size: 'm', test: 'big-suggest'}
                        },
                        placeholder: 'Набери одноклассники',
                        content: [
                            {elem: 'control', attrs: {name: 'autocomplete'}}
                        ]
                    }
                }
            ]
        },

        {block: 'x-deps', content: {block: 'mock-data'}},

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
        {elem: 'js', url: '_popup.js'}
    ]
});
