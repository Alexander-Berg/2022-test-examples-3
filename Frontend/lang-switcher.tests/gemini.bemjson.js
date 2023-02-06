({
    block: 'b-page',
    title: 'gemini examples',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'gemini-simple',
            content: {
                block: 'lang-switcher',
                mods: {theme: 'normal', size: 'm'},
                lang: {lang: 'ru', name: 'Ru'},
                secretKey: 'test',
                content: [
                    {
                        elem: 'lang',
                        mix: {block: 'gemini-simple', elem: 'first'},
                        lang: {lang: 'be', name: 'Be'}
                    },
                    {
                        elem: 'lang',
                        lang: {lang: 'en', name: 'En'}
                    },
                    {
                        elem: 'lang',
                        lang: {lang: 'kk', name: 'Kk'}
                    },
                    {
                        elem: 'lang',
                        elemMods: {selected: 'yes'},
                        lang: {lang: 'ru', name: 'Ru'}
                    },
                    {
                        elem: 'lang',
                        lang: {lang: 'tr', name: 'Tr'}
                    },
                    {
                        elem: 'lang',
                        lang: {lang: 'tt', name: 'Tt'}
                    },
                    {
                        elem: 'lang',
                        mix: {block: 'gemini-simple', elem: 'last'},
                        lang: {lang: 'uk', name: 'Uk'}
                    }
                ]
            }
        },
        {
            block: 'gemini-without-flags',
            content: {
                block: 'lang-switcher',
                lang: {lang: 'ru', name: 'Ru'},
                mods: {theme: 'normal', size: 'm', noflags: 'yes'},
                secretKey: 'test',
                content: [
                    {
                        elem: 'lang',
                        mix: {block: 'gemini-without-flags', elem: 'first'},
                        lang: {lang: 'be', name: 'Be'}
                    },
                    {
                        elem: 'lang',
                        lang: {lang: 'en', name: 'En'}
                    },
                    {
                        elem: 'lang',
                        lang: {lang: 'kk', name: 'Kk'}
                    },
                    {
                        elem: 'lang',
                        elemMods: {selected: 'yes'},
                        lang: {lang: 'ru', name: 'Ru'}
                    },
                    {
                        elem: 'lang',
                        lang: {lang: 'tr', name: 'Tr'}
                    },
                    {
                        elem: 'lang',
                        lang: {lang: 'tt', name: 'Tt'}
                    },
                    {
                        elem: 'lang',
                        mix: {block: 'gemini-without-flags', elem: 'last'},
                        lang: {lang: 'uk', name: 'Uk'}
                    }
                ]
            }
        },
        {
            block: 'gemini-all',
            content: {
                block: 'lang-switcher',
                mods: {theme: 'normal', size: 'm'},
                lang: {lang: 'ru', name: 'Ru'},
                secretKey: 'test',
                content: [
                    {
                        elem: 'lang',
                        mix: {block: 'gemini-all', elem: 'first'},
                        lang: {lang: 'uk', name: 'Ua'},
                        url: '#'
                    },
                    {
                        elem: 'lang',
                        lang: {lang: 'en', name: 'En'},
                        url: '#'
                    },
                    {
                        elem: 'lang',
                        mix: {block: 'gemini-all', elem: 'last'},
                        lang: {lang: 'ru', name: 'Ru'},
                        elemMods: {selected: 'yes'},
                        url: '#'
                    }
                ]
            }
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
