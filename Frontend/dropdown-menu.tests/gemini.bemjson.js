({
    block: 'b-page',
    title: 'dropdown-menu',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'gemini-simple',
            content: {
                block: 'dropdown-menu',
                content: [

                    {
                        elem: 'switcher',
                        content: {
                            block: 'button',
                            mods: {size: 'm', arrow: 'down', theme: 'normal'},
                            content: 'Меню, пожалуйста'
                        }
                    },

                    {
                        elem: 'popup',
                        mix: [{block: 'gemini-simple-popup'}],
                        content: [
                            {
                                elem: 'item',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'pseudo', pseudo: 'yes'},
                                    content: 'Поиск'
                                }
                            },
                            {
                                elem: 'item',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'pseudo', pseudo: 'yes'},
                                    content: 'Карты'
                                }
                            },
                            {
                                elem: 'item',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'pseudo', pseudo: 'yes'},
                                    content: 'Маркет'
                                }
                            },
                            {
                                elem: 'item',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'pseudo', pseudo: 'yes'},
                                    content: 'Новости'
                                }
                            },
                            {
                                elem: 'item',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'pseudo', pseudo: 'yes'},
                                    content: 'Словари'
                                }
                            },
                            {
                                elem: 'item',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'pseudo', pseudo: 'yes'},
                                    content: 'Картинки'
                                }
                            },
                            {
                                elem: 'item',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'pseudo', pseudo: 'yes'},
                                    content: 'Видео'
                                }
                            },
                            {
                                elem: 'item',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'pseudo', pseudo: 'yes'},
                                    content: 'Музыка'
                                }
                            },
                            {
                                elem: 'item',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'pseudo', pseudo: 'yes'},
                                    content: 'Перевод'
                                }
                            },
                            {
                                elem: 'item',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'pseudo', pseudo: 'yes'},
                                    content: 'Блоги'
                                }
                            }
                        ]
                    }
                ]
            }
        },
        {
            block: 'gemini-button-dropdown',
            content: {
                block: 'dropdown-menu',
                content: [
                    {
                        block: 'button',
                        mods: {theme: 'action', size: 'm', side: 'left'},
                        content: 'Открыть 644×479'
                    },
                    {
                        block: 'button',
                        mix: [
                            {block: 'dropdown-menu', elem: 'switcher'}
                        ],
                        mods: {theme: 'action', size: 'm', 'only-icon': 'yes', side: 'right'},
                        content: [
                            {
                                block: 'image',
                                mix: [
                                    {block: 'button', elem: 'icon', elemMods: {8: 'down'}}
                                ],
                                alt: '/'
                            }
                        ]
                    },
                    {
                        elem: 'popup',
                        mix: [{block: 'button-dropdown'}, {block: 'gemini-button-dropdown-popup'}],
                        attrs: {name: 'gemini-button-dropdown'},
                        js: {directions: 'bottom'},
                        content: [
                            {
                                elem: 'item',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'pseudo', pseudo: 'yes'},
                                    url: '#640x480',
                                    content: '640x480'
                                }
                            },
                            {
                                elem: 'item',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'pseudo', pseudo: 'yes'},
                                    url: '#320x240',
                                    content: '320x240'
                                }
                            },
                            {
                                elem: 'item',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'pseudo', pseudo: 'yes'},
                                    url: '#128x64',
                                    content: '128x64'
                                }
                            }
                        ]
                    }
                ]
            }
        },
        {
            block: 'gemini-menu-has-title',
            content: {
                block: 'dropdown-menu',
                content: [
                    {
                        elem: 'switcher',
                        content: {
                            block: 'button',
                            mods: {size: 'm', arrow: 'down', theme: 'normal'},
                            content: 'Меню, пожалуйста'
                        }
                    },
                    {
                        elem: 'popup',
                        mix: [{block: 'gemini-menu-has-title-popup'}],
                        menuMods: {type: 'hovered'},
                        menuTitle: {elem: 'title', content: 'Сервисы'},
                        content: [
                            {
                                elem: 'item',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'pseudo', pseudo: 'yes'},
                                    content: 'Поиск'
                                }
                            },
                            {
                                elem: 'item',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'pseudo', pseudo: 'yes'},
                                    content: 'Карты'
                                }
                            },
                            {
                                elem: 'item',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'pseudo', pseudo: 'yes'},
                                    content: 'Маркет'
                                }
                            },
                            {
                                elem: 'item',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'pseudo', pseudo: 'yes'},
                                    content: 'Новости'
                                }
                            },
                            {
                                elem: 'item',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'pseudo', pseudo: 'yes'},
                                    content: 'Словари'
                                }
                            },
                            {
                                elem: 'item',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'pseudo', pseudo: 'yes'},
                                    content: 'Картинки'
                                }
                            },
                            {
                                elem: 'item',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'pseudo', pseudo: 'yes'},
                                    content: 'Видео'
                                }
                            },
                            {
                                elem: 'item',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'pseudo', pseudo: 'yes'},
                                    content: 'Музыка'
                                }
                            },
                            {
                                elem: 'item',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'pseudo', pseudo: 'yes'},
                                    content: 'Перевод'
                                }
                            },
                            {
                                elem: 'item',
                                content: {
                                    block: 'link',
                                    mods: {theme: 'pseudo', pseudo: 'yes'},
                                    content: 'Блоги'
                                }
                            }
                        ]
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
