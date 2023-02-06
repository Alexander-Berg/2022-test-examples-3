({
    block: 'b-page',
    title: 'dropdown',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'gemini-usual',
            content: [
                {
                    block: 'dropdown',
                    content: [
                        {
                            block: 'link',
                            mix: [{block: 'dropdown', elem: 'switcher'}],
                            mods: {theme: 'pseudo', pseudo: 'yes'},
                            url: 'http://yandex.ru/all',
                            content: 'ещё'
                        },
                        {
                            elem: 'popup',
                            attrs: {name: 'usual'},
                            content: 'I am a dropdown here!'
                        }
                    ]
                }
            ]
        },
        {
            block: 'gemini-button',
            content: [
                {
                    block: 'dropdown',
                    content: [
                        {
                            block: 'button',
                            mix: [{block: 'dropdown', elem: 'switcher'}],
                            mods: {size: 'm', arrow: 'down', theme: 'normal'},
                            content: 'Действия'
                        },
                        {
                            elem: 'popup',
                            attrs: {style: 'width: 300px;', name: 'button'},
                            js: {
                                directions: 'bottom-left-center'
                            },
                            // Ширина в popup задана в css примера
                            content: [
                                'Душа моя озарена неземной радостью, как эти чудесные весенние утра, ',
                                'которыми я наслаждаюсь от всего сердца. Я совсем один и блаженствую' +
                                ' в здешнем краю, ',
                                'словно созданном для таких, как я. Я так счастлив, мой друг, так упоен' +
                                ' ощущением покоя, ',
                                'что искусство мое страдает от этого.'
                            ].join('')
                        }
                    ]
                }
            ]
        },
        {
            block: 'gemini-has-close',
            content: {
                block: 'dropdown',
                content: [
                    {
                        block: 'link',
                        mix: [{block: 'dropdown', elem: 'switcher'}],
                        mods: {theme: 'pseudo', pseudo: 'yes'},
                        url: 'http://yandex.ru/all',
                        content: 'ещё'
                    },
                    {
                        elem: 'popup',
                        attrs: {name: 'has-close'},
                        elemMods: {'has-close': 'yes'},
                        content: 'Какой-то контент'
                    }
                ]
            }
        },
        {
            block: 'gemini-button-disabled',
            js: true,
            content: [
                {
                    tag: 'p',
                    bem: false,
                    content: {
                        block: 'button',
                        mods: {theme: 'action', size: 'm'},
                        content: 'Активировать/Деактивировать действия'
                    }
                },
                {
                    block: 'dropdown',
                    content: [
                        {
                            block: 'button',
                            mix: [{block: 'dropdown', elem: 'switcher'}],
                            mods: {size: 'm', arrow: 'down', disabled: 'yes', theme: 'normal'},
                            content: 'Действия'
                        },
                        {
                            elem: 'popup',
                            attrs: {style: 'width: 300px;', name: 'button-disabled'},
                            js: {
                                directions: 'bottom-left-center'
                            },
                            // Ширина в popup задана в css примера
                            content: [
                                'Душа моя озарена неземной радостью, как эти чудесные весенние утра, ',
                                'которыми я наслаждаюсь от всего сердца. Я совсем один и блаженствую в' +
                                ' здешнем краю, ',
                                'словно созданном для таких, как я. Я так счастлив, мой друг, так упоен' +
                                ' ощущением покоя, ',
                                'что искусство мое страдает от этого.'
                            ].join('')
                        }
                    ]
                }
            ]
        },
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
