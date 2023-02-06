({
    block: 'b-page',
    title: 'user',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'gemini',
            content: [
                {
                    block: 'gemini-username',
                    mix: {block: 'gemini-block'},
                    content: [
                        {
                            block: 'i-global',
                            params: {login: 'big.green.frog'}
                        },
                        {
                            block: 'user',
                            mods: {menu: 'yes'},
                            js: {mail: 101, uid: '12345678'},
                            content: {elem: 'name'}
                        }
                    ]
                },
                {
                    block: 'gemini-username-with-at',
                    mix: {block: 'gemini-block'},
                    content: [
                        {
                            block: 'i-global',
                            params: {login: 'b@green.frog'}
                        },
                        {
                            block: 'user',
                            mods: {menu: 'yes'},
                            js: {mail: 101, uid: '12345678'},
                            content: {elem: 'name'}
                        }
                    ]
                },
                {
                    block: 'gemini-usericon',
                    mix: {block: 'gemini-block'},
                    content: [
                        {
                            block: 'i-global',
                            params: {login: 'big.green.frog'}
                        },
                        {
                            block: 'user',
                            mods: {menu: 'yes'},
                            content: {elem: 'icon'},
                            js: {mail: 101, uid: '12345678'}
                        }
                    ]
                },
                {tag: 'br', attrs: {clear: 'both'}},
                {
                    block: 'gemini-full',
                    mix: {block: 'gemini-block'},
                    content: [
                        {
                            block: 'i-global',
                            params: {login: 'big.green.frog'}
                        },
                        {
                            block: 'user',
                            js: {mail: 101, uid: '12345678'},
                            mods: {menu: 'yes'},
                            content: [
                                {elem: 'icon'},
                                {elem: 'name'}
                            ]
                        }
                    ]
                },
                {
                    block: 'gemini-full-skip',
                    mix: {block: 'gemini-block'},
                    content: [
                        {
                            block: 'i-global',
                            params: {login: 'big.green.frog'}
                        },
                        {
                            block: 'user',
                            mods: {menu: 'yes'},
                            js: {uid: '12345678'},
                            content: [
                                {elem: 'icon'},
                                {elem: 'name'}
                            ]
                        }
                    ]
                },
                {
                    block: 'gemini-full-ticker',
                    mix: {block: 'gemini-block'},
                    content: [
                        {
                            block: 'i-global',
                            params: {login: 'big.green.frog'}
                        },
                        {
                            block: 'user',
                            mods: {menu: 'yes'},
                            js: {uid: '12345678'},
                            content: [
                                {elem: 'icon'},
                                {elem: 'name'},
                                {elem: 'ticker', count: 2}
                            ]
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
