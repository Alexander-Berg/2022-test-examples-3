([{
    block: 'i-global',
    params: {
        login: 'lego-team',
        uid: '4001140776'
    }
}, {
    block: 'x-page',
    title: 'header2',
    content: [
        {
            block: 'gemini',
            mix: {block: 'fixed'},
            attrs: {id: 'fixed'},
            content: {
                block: 'header2',
                mods: {fixed: 'yes', border: 'white'},
                logo: {elem: 'logo', elemMods: {preset: 'ru'}},
                left: [
                    {
                        block: 'search2',
                        content: [
                            {
                                block: 'header2',
                                elem: 'nameplate',
                                service: 'market'
                            },
                            {
                                block: 'input',
                                mods: {size: 'm', type: 'normal', 'after-nameplate': 'yes', pin: 'brick-round'},
                                content: [
                                    {elem: 'control'}
                                ]
                            }
                        ]
                    }
                ],
                right: [
                    {
                        elem: 'action',
                        elemMods: {type: 'filter'},
                        js: {panel: 'filter'}
                    },
                    {
                        elem: 'action',
                        elemMods: {type: 'tableau'},
                        js: {panel: 'tableau'}
                    },
                    {
                        block: 'user',
                        content: [
                            {elem: 'icon', avatarId: '1450/4001140776-39966'},
                            {elem: 'name'}
                        ]
                    }
                ]
            }
        },

        {
            block: 'gemini',
            mix: {block: 'action'},
            mods: {paranja: 'yes', tooltip: 'yes'},
            js: true,
            content: {
                block: 'header2',
                mods: {panels: 'yes', example: 'service'},
                logo: {
                    elem: 'logo',
                    elemMods: {size: 'm'},
                    content: {
                        block: 'logo',
                        mods: {type: 'link', name: 'ru-84x36'},
                        tabindex: -1,
                        url: 'https://yandex.com.tr'
                    }
                },
                left: [
                    {
                        block: 'search2',
                        content: [
                            {
                                block: 'header2',
                                elem: 'nameplate',
                                service: 'market'
                            },
                            {
                                block: 'input',
                                mods: {size: 'm', type: 'normal', 'after-nameplate': 'yes', pin: 'brick-round'},
                                content: [
                                    {elem: 'control'}
                                ]
                            }
                        ]
                    }
                ],
                right: [
                    {
                        elem: 'action',
                        elemMods: {type: 'settings'},
                        js: {notice: true, panel: 'settings'}
                    },
                    {
                        block: 'header2',
                        elem: 'gap'
                    },
                    {
                        block: 'user',
                        mods: {menu: 'multiauth'},
                        content: [
                            {elem: 'icon', avatarId: '1450/4001140776-39966'},
                            {elem: 'name'}
                        ]
                    }
                ],
                under: [
                    {
                        elem: 'progress'
                    },
                    {
                        elem: 'paranja'
                    },
                    {
                        block: 'tooltip',
                        mods: {size: 's', theme: 'success'},
                        content: 'По событию на шапке открывается тултип и отображается паранжа'
                    }
                ]
            }
        },
        {
            block: 'gemini',
            attrs: {id: 'serp'},
            content: {
                block: 'header2',
                mods: {border: 'transparent'},
                logo: {
                    elem: 'logo',
                    elemMods: {size: 'm'},
                    content: {
                        block: 'logo',
                        mods: {type: 'link', name: 'ru-84x36'},
                        tabindex: -1,
                        url: 'https://yandex.com.tr'
                    }
                },
                left: [
                    {
                        block: 'search2',
                        mods: {theme: 'websearch'},
                        input: {
                            block: 'textinput',
                            mods: {size: 'ws-head', theme: 'websearch'},
                            text: 'москва'
                        },
                        button: {
                            block: 'button2',
                            mods: {size: 'ws-head', type: 'submit', theme: 'websearch'},
                            attrs: {tabindex: -1},
                            text: 'Найти'
                        }
                    }
                ],
                right: [
                    {
                        block: 'button2',
                        mods: {theme: 'normal', size: 'head'},
                        url: 'https://passport.yandex.ru/passport?mode=passport',
                        text: 'Войти'
                    }
                ]
            }
        }
    ]
}, {
    block: 'x-deps',
    content: [
        {block: 'i-services', elem: 'uri'},
        {block: 'header2', elem: 'right'}
    ]
}]);
