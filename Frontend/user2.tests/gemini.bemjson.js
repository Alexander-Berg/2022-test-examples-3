({
    block: 'x-page',
    content: [
        {
            block: 'gemini',
            mods: {full: 'yes'},
            content: {
                block: 'user2',
                uid: 6789054321,
                yu: 123456789012345678,
                retpath: 'https://yandex.ru',
                passportHost: 'https://passport.yandex.ru',
                name: 'John Doe',
                subname: 'johndoe@ya.ru',
                avatarId: '20706/84473936-5041676',
                unreadCount: 25,

                accounts: [
                    {
                        block: 'user-account',
                        mods: {
                            'has-subname': 'yes',
                            'has-ticker': 'yes' // react
                        },
                        pic: {avatarId: '21377/28004787-1253930'},
                        name: 'Jane Doe',
                        ticker: {count: 17},
                        subname: {provider: 'gg'}
                    },
                    {
                        block: 'user-account',
                        mods: {
                            'has-subname': 'yes',
                            'has-ticker': 'yes' // react
                        },
                        pic: {avatarId: '0/0-0'},
                        name: 'John Smith',
                        ticker: {count: 7},
                        subname: 'john-smith@ya.ru'
                    }
                ]
            }
        },

        {
            block: 'gemini',
            mods: {'no-auth': 'yes'},
            content: {
                block: 'user2'
            }
        },

        {
            block: 'wrap',
            content: {
                block: 'gemini',
                mods: {loader: 'yes'},
                content: {
                    block: 'user2',
                    uid: 6789054321,
                    yu: 123456789012345678,
                    retpath: 'https://yandex.ru',
                    passportHost: 'https://passport.yandex.ru',

                    content: [
                        {
                            elem: 'current-account',
                            name: false,
                            unreadCount: 3
                        },
                        {
                            elem: 'popup',
                            content: {
                                block: 'user2',
                                elem: 'menu',
                                content: {
                                    block: 'menu',
                                    mix: [{block: 'user2', elem: 'menu'}],
                                    mods: {type: 'navigation'},
                                    content: [
                                        {
                                            elem: 'group',
                                            content: [
                                                {
                                                    action: 'mail-compose',
                                                    block: 'user2',
                                                    elem: 'menu-item',
                                                    elemMods: {
                                                        action: 'mail-compose'
                                                    }
                                                }
                                            ]
                                        },
                                        {
                                            block: 'user2',
                                            elem: 'multi-auth',
                                            content: [
                                                {
                                                    elem: 'accounts',
                                                    content: [
                                                        {
                                                            block: 'spin2',
                                                            mods: {size: 'xs', progress: 'yes'},
                                                            mix: {
                                                                block: 'user2',
                                                                elem: 'accounts-spin'
                                                            }
                                                        },
                                                        {elem: 'add-account'}
                                                    ]
                                                },
                                                {
                                                    block: 'user2',
                                                    elem: 'menu-footer'
                                                }
                                            ]
                                        }
                                    ]
                                }
                            }
                        }
                    ]
                }
            }
        },

        {
            block: 'mock-element',
            tag: 'i',
            content: ' '
        }
    ]
});
