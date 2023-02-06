({
    block: 'x-page',
    title: 'search2',
    content: [
        {
            block: 'gemini',
            mods: {type: 'websearch'},
            content: [
                {
                    block: 'search2',
                    mods: {theme: 'websearch'},
                    url: 'http://yandex.ru/yandsearch',
                    input: {
                        block: 'textinput',
                        mods: {size: 'ws-head', theme: 'websearch'},
                        text: 'Москва',
                        found: ' — 515 млн ответов',
                        name: 'text',
                        controlAttrs: {
                            maxlength: 400
                        }
                    },
                    // Параметры кнопки такие же как и по-умолчанию в шаблоне
                    // search2, только с типом button, а не submit, чтобы в тесте
                    // кнопка не отправляла форму при клике.
                    button: {
                        block: 'button2',
                        mods: {size: 'ws-head', theme: 'websearch'},
                        text: 'Найти'
                    }
                },
                {
                    block: 'button2',
                    mods: {theme: 'normal', size: 'm'},
                    text: 'Кнопка'
                }
            ]
        },
        {
            block: 'gemini',
            mods: {type: 'ws-button2'},
            content: [
                {
                    block: 'search2',
                    mods: {theme: 'websearch'},
                    url: 'http://yandex.ru/yandsearch',
                    input: {
                        block: 'textinput',
                        mods: {size: 'ws-head', theme: 'websearch'},
                        text: 'Москва',
                        found: ' — 515 млн ответов',
                        name: 'text',
                        controlAttrs: {
                            maxlength: 400
                        },
                        filter: {
                            block: 'button2',
                            mods: {type: 'check', size: 'head', theme: 'clear'},
                            attrs: {tabindex: -1},
                            icon: {block: 'icon', mods: {type: 'filter'}}
                        }
                    },
                    // Параметры кнопки такие же как и по-умолчанию в шаблоне
                    // search2, только с типом button, а не submit, чтобы в тесте
                    // кнопка не отправляла форму при клике.
                    button: {
                        block: 'button2',
                        mods: {size: 'ws-head', theme: 'websearch'},
                        text: 'Найти'
                    }
                },
                {
                    block: 'button2',
                    mods: {theme: 'normal', size: 'm'},
                    text: 'Кнопка'
                }
            ]
        },
        {
            // because textinput has no clear by default
            block: 'x-deps',
            content: {
                block: 'textinput',
                elem: 'clear',
                mods: {
                    theme: 'normal'
                }
            }
        },
        {
            block: 'gemini',
            mods: {type: 'action'},
            content: [
                {
                    block: 'search2',
                    url: 'https://search.yandex-team.ru/search',
                    input: {
                        block: 'textinput',
                        mods: {size: 'm', theme: 'normal', 'has-clear': 'yes', pin: 'round-clear'},
                        name: 'text'
                    },
                    button: {
                        block: 'button2',
                        mods: {size: 'm', theme: 'action', type: 'submit', pin: 'brick-round'},
                        text: 'Найти'
                    }
                },
                {
                    block: 'button2',
                    mods: {theme: 'normal', size: 'm'},
                    text: 'Кнопка'
                }
            ]
        },
        {
            block: 'gemini',
            mods: {type: 'service'},
            content: [
                {
                    block: 'search2',
                    url: 'https://market.yandex.ru/search.xml',
                    content: [
                        {
                            block: 'header2',
                            elem: 'nameplate',
                            service: 'market'
                        },
                        {
                            block: 'input',
                            mods: {size: 'm', theme: 'normal', 'after-nameplate': 'yes'},
                            content: {elem: 'control', attrs: {name: 'text'}}
                        },
                        {
                            block: 'button2',
                            mods: {size: 'm', theme: 'normal', type: 'submit', pin: 'clear-round'},
                            text: 'Найти'
                        }
                    ]
                },
                {
                    block: 'button2',
                    mods: {theme: 'normal', size: 'm'},
                    text: 'Кнопка'
                }
            ]
        }
    ]
});
