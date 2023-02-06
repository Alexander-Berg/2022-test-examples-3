([
    {
        block: 'i-global',
        params: {login: 'gemini'}
    },
    {
        block: 'b-page',
        title: 'header-layout',
        head: [
            {elem: 'css', url: '_gemini.css', ie: false},
            {elem: 'css', url: '_gemini', ie: true}
        ],
        content: [
            {
                block: 'gemini-small',
                // Suppress opera's 3px outline so gemini won't try to crop the element outside of the image
                attrs: {style: 'width: 1000px; padding-bottom: 10px; outline: 0;'},
                content: {
                    block: 'header',
                    js: true,
                    mix: [{block: 'layout', mods: {type: 'serp'}}],
                    content: [
                        {
                            elem: 'main',
                            content: [
                                {
                                    elem: 'logo',
                                    content: {
                                        block: 'logo',
                                        mods: {name: 'ru-84x36'}
                                    }
                                },
                                {
                                    elem: 'search',
                                    content: {
                                        block: 'arrow',
                                        content: {
                                            elem: 'text',
                                            content: 'Gemini in action'
                                        }
                                    }
                                },
                                {
                                    elem: 'nav',
                                    content: [
                                        {
                                            block: 'header',
                                            elem: 'action',
                                            elemMods: {type: 'adv'}
                                        },
                                        {
                                            block: 'header',
                                            elem: 'action',
                                            elemMods: {type: 'srv'}
                                        },
                                        {
                                            block: 'user',
                                            js: {mail: 101},
                                            mods: {menu: 'yes'},
                                            content: [
                                                {elem: 'icon'},
                                                {elem: 'name'}
                                            ]
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            },
            {block: 'separator', tag: 'br'},
            {
                block: 'gemini-small-en',
                // Suppress opera's 3px outline so gemini won't try to crop the element outside of the image
                attrs: {style: 'width: 1000px; padding-bottom: 10px; outline: 0;'},
                content: {
                    block: 'header',
                    js: true,
                    mix: [{block: 'layout', mods: {type: 'serp'}}],
                    content: [
                        {
                            elem: 'main',
                            content: [
                                {
                                    elem: 'logo',
                                    content: {
                                        block: 'logo',
                                        mods: {name: 'en-84x36'}
                                    }
                                },
                                {
                                    elem: 'search',
                                    content: {
                                        block: 'arrow',
                                        content: {
                                            elem: 'text',
                                            content: 'Gemini in action'
                                        }
                                    }
                                }
                            ]
                        }
                    ]
                }
            },
            {block: 'separator', tag: 'br'},
            {
                block: 'gemini-full',
                // Suppress opera's 3px outline so gemini won't try to crop the element outside of the image bounds
                attrs: {style: 'height: 380px; outline: 0;'},
                content: [
                    {
                        block: 'header',
                        // Same as above
                        attrs: {style: 'outline: 0;'},
                        js: true,
                        mix: [{block: 'layout', mods: {type: 'serp'}}],
                        content: [
                            {
                                elem: 'main',
                                content: [
                                    {
                                        elem: 'logo',
                                        content: {
                                            block: 'logo',
                                            mods: {name: 'ru-84x36'}
                                        }
                                    },
                                    {
                                        elem: 'search',
                                        content: {
                                            block: 'arrow',
                                            content: {
                                                elem: 'text',
                                                content: 'Gemini in action'
                                            }
                                        }
                                    },
                                    {
                                        elem: 'nav',
                                        content: [
                                            {
                                                block: 'header',
                                                elem: 'action',
                                                elemMods: {type: 'adv'},
                                                js: {
                                                    releaseByHeaderOutsideClick: false,
                                                    releaseByDocumentScroll: false
                                                }
                                            },
                                            {
                                                block: 'header',
                                                elem: 'action',
                                                elemMods: {type: 'srv'}
                                            },
                                            {
                                                block: 'user',
                                                js: {mail: 101},
                                                mods: {menu: 'yes'},
                                                content: [
                                                    {elem: 'icon'},
                                                    {elem: 'name'}
                                                ]
                                            }
                                        ]
                                    }
                                ]
                            },
                            {
                                elem: 'under',
                                content: [
                                    {
                                        block: 'slide',
                                        js: {
                                            rel: [
                                                {elem: '.header__action_type_adv', event: 'pressed', method: 'open'},
                                                {elem: '.header__action_type_adv', event: 'released', method: 'close'}
                                            ]
                                        },
                                        content: {
                                            block: 'b-block',
                                            attrs: {style: 'width: 600px; height: 200px; background: #eee;' +
                                            ' margin: 15px auto;'},
                                            content: 'Здесь может быть любой контент'
                                        }
                                    },
                                    {
                                        block: 'slide',
                                        js: {
                                            rel: [
                                                {elem: '.header__action_type_srv', event: 'pressed', method: 'open'},
                                                {elem: '.header__action_type_srv', event: 'released', method: 'close'}
                                            ]
                                        },
                                        content: {
                                            block: 'services-table',
                                            mods: {type: 'turkish'}
                                        }
                                    }
                                ]
                            }
                        ]
                    },
                    {
                        block: 'paranja',
                        js: {
                            rel: [
                                {elem: '.header__action_type_adv', event: 'pressed', method: 'open'},
                                {elem: '.header__action_type_adv', event: 'released', method: 'close'}
                            ]
                        }
                    },
                    {
                        block: 'paranja',
                        js: {
                            rel: [
                                {elem: '.header__action_type_srv', event: 'pressed', method: 'open'},
                                {elem: '.header__action_type_srv', event: 'released', method: 'close'}
                            ]
                        }
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
    }
]);
