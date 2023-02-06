({
    block: 'x-page',
    title: 'Радио-группа',
    content: [
        {
            block: 'gemini',
            mix: {block: 'radio-tones'},
            content: (function() {
                var tones = ['default', 'red', 'grey', 'dark'],
                    sizes = ['s', 'm', 'n'];

                return tones.map(function(tone) {
                    return sizes.map(function(size) {
                        return {
                            block: 'gemini',
                            content: {
                                block: 'radio-button',
                                mods: {theme: 'normal', size: size, view: 'default', tone: tone},
                                name: 'tone-' + tone + '-' + size,
                                value: 'bass',
                                content: [
                                    {
                                        elem: 'radio',
                                        controlAttrs: {value: 'bass'},
                                        content: 'Bass'
                                    },
                                    {
                                        elem: 'radio',
                                        controlAttrs: {value: 'mid'},
                                        content: 'Mid'
                                    },
                                    {
                                        elem: 'radio',
                                        controlAttrs: {value: 'high'},
                                        content: 'High'
                                    }
                                ]
                            }
                        };
                    });
                });
            })()
        },
        {
            block: 'gemini',
            mix: {block: 'radio-normal'},
            content: {
                block: 'gemini',
                content: {
                    block: 'radio-button',
                    mods: {theme: 'normal', size: 'm'},
                    name: 'show_to',
                    value: 'friends',
                    content: [
                        {
                            elem: 'radio',
                            controlAttrs: {value: 'all'},
                            elemMods: {disabled: 'yes'},
                            content: 'всем'
                        },
                        {
                            elem: 'radio',
                            controlAttrs: {value: 'friends'},
                            content: 'друзьям'
                        },
                        {
                            elem: 'radio',
                            controlAttrs: {value: 'me'},
                            elemMods: {disabled: 'yes'},
                            content: 'мне'
                        },
                        {
                            elem: 'radio',
                            controlAttrs: {value: 'other'},
                            content: 'не мне'
                        }
                    ]
                }
            }
        },
        {
            block: 'gemini',
            mix: {block: 'radio-pseudo'},
            attrs: {style: 'margin-left: 0;padding: 20px;background: #b7e2eb;'},
            content: {
                block: 'radio-button',
                mods: {size: 'm', theme: 'pseudo'},
                name: 'show_to1',
                value: 'friends',
                content: [
                    {
                        elem: 'radio',
                        controlAttrs: {value: 'all'},
                        elemMods: {disabled: 'yes'},
                        content: 'всем'
                    },
                    {
                        elem: 'radio',
                        controlAttrs: {value: 'friends'},
                        content: 'друзьям'
                    },
                    {
                        elem: 'radio',
                        controlAttrs: {value: 'me'},
                        content: 'мне'
                    },
                    {
                        elem: 'radio',
                        controlAttrs: {value: 'other'},
                        content: 'не мне'
                    }
                ]
            }
        },

        // Тестирование клавиатурной навигации.
        {
            block: 'gemini',
            mix: {block: 'keyboard'},
            attrs: {style: 'display: inline-block'},
            content: {
                block: 'radio-button',
                mods: {theme: 'normal', size: 'm'},
                name: 'color',
                content: [
                    {
                        elem: 'radio',
                        controlAttrs: {value: 'violet'},
                        content: 'violet'
                    },
                    {
                        elem: 'radio',
                        controlAttrs: {value: 'orange'},
                        content: 'orange'
                    },
                    {
                        elem: 'radio',
                        controlAttrs: {value: 'yellow'},
                        elemMods: {disabled: 'yes'},
                        content: 'yellow'
                    },
                    {
                        elem: 'radio',
                        controlAttrs: {value: 'green'},
                        content: 'green'
                    }
                ]
            }
        },
        {
            block: 'gemini',
            mix: {block: 'radio-sizes'},
            content: [
                {
                    block: 'gemini',
                    content: {
                        block: 'radio-button',
                        mods: {theme: 'normal', size: 'xs'},
                        name: 'show_to_xs',
                        value: 'my',
                        content: [
                            {
                                elem: 'radio',
                                controlAttrs: {value: 'all'},
                                content: 'всем'
                            },
                            {
                                elem: 'radio',
                                controlAttrs: {value: 'fr'},
                                content: 'друзьям'
                            },
                            {
                                elem: 'radio',
                                controlAttrs: {value: 'my'},
                                content: 'мне'
                            },
                            {
                                elem: 'radio',
                                controlAttrs: {value: 'yours'},
                                content: 'не мне'
                            }
                        ]
                    }
                },
                {
                    block: 'gemini',
                    content: {
                        block: 'radio-button',
                        mods: {theme: 'normal', size: 's'},
                        name: 'show_to_s',
                        value: 'my',
                        content: [
                            {
                                elem: 'radio',
                                controlAttrs: {value: 'all'},
                                content: 'всем'
                            },
                            {
                                elem: 'radio',
                                controlAttrs: {value: 'fr'},
                                content: 'друзьям'
                            },
                            {
                                elem: 'radio',
                                controlAttrs: {value: 'my'},
                                content: 'мне'
                            },
                            {
                                elem: 'radio',
                                controlAttrs: {value: 'yours'},
                                content: 'не мне'
                            }
                        ]
                    }
                },
                {
                    block: 'gemini',
                    content: {
                        block: 'radio-button',
                        mods: {theme: 'normal', size: 'm'},
                        name: 'show_to_m',
                        value: 'val-3',
                        content: [
                            {
                                elem: 'radio',
                                controlAttrs: {value: 'val-1'},
                                content: 'всем'
                            },
                            {
                                elem: 'radio',
                                controlAttrs: {value: 'val-2'},
                                content: 'друзьям'
                            },
                            {
                                elem: 'radio',
                                controlAttrs: {value: 'val-3'},
                                content: 'мне'
                            },
                            {
                                elem: 'radio',
                                controlAttrs: {value: 'val-4'},
                                content: 'не мне'
                            }
                        ]
                    }
                },
                {
                    block: 'gemini',
                    content: {
                        block: 'radio-button',
                        mods: {theme: 'normal', size: 'l'},
                        name: 'show_to_l',
                        value: 'val-3',
                        content: [
                            {
                                elem: 'radio',
                                controlAttrs: {value: 'val-1'},
                                content: 'всем'
                            },
                            {
                                elem: 'radio',
                                controlAttrs: {value: 'val-2'},
                                content: 'друзьям'
                            },
                            {
                                elem: 'radio',
                                controlAttrs: {value: 'val-3'},
                                content: 'мне'
                            }
                        ]
                    }
                },
                {
                    block: 'gemini',
                    content: {
                        block: 'radio-button',
                        mods: {theme: 'normal', size: 'head'},
                        name: 'show_to_l',
                        value: 'val-3',
                        content: [
                            {
                                elem: 'radio',
                                controlAttrs: {value: 'val-1'},
                                content: 'всем'
                            },
                            {
                                elem: 'radio',
                                controlAttrs: {value: 'val-2'},
                                content: 'друзьям'
                            },
                            {
                                elem: 'radio',
                                controlAttrs: {value: 'val-3'},
                                content: 'мне'
                            },
                            {
                                elem: 'radio',
                                controlAttrs: {value: 'val-4'},
                                content: 'не мне'
                            }
                        ]
                    }
                }
            ]
        },
        {
            block: 'gemini',
            mix: {block: 'radio-icon'},
            attrs: {style: 'display: inline-block'},
            content: [
                {
                    block: 'gemini',
                    content: [
                        {
                            block: 'radio-button',
                            mods: {theme: 'normal', size: 'm'},
                            mix: {block: 'gemini', elem: 'item'},
                            name: 'show_to3',
                            content: [
                                {
                                    elem: 'radio',
                                    controlAttrs: {value: 'val-2'},
                                    content: [
                                        {
                                            block: 'image',
                                            mix: [{block: 'radio-button', elem: 'icon', elemMods: {16: 'comment'}}]
                                        },
                                        'Коммент'
                                    ]
                                },
                                {
                                    elem: 'radio',
                                    elemMods: {disabled: 'yes'},
                                    controlAttrs: {value: 'val-4'},
                                    content: [
                                        {
                                            block: 'image',
                                            mix: [{block: 'radio-button', elem: 'icon', elemMods: {16: 'view'}}]
                                        },
                                        'Просмотр'
                                    ]
                                }
                            ]
                        },
                        {
                            block: 'button2',
                            mods: {size: 'm', theme: 'normal'},
                            mix: {block: 'gemini', elem: 'item'},
                            url: '#',
                            iconLeft:
                            {
                                block: 'icon',
                                mix: [{block: 'button2', elem: 'icon'}],
                                url: 'https://yastatic.net/lego/_/Kx6F6RQnQFitm0qRxX7vpvfP0K0.png',
                                alt: 'Иконка Серпа'
                            },
                            text: 'Я.Кнопка ссылка'

                        }
                    ]
                },
                {
                    block: 'gemini',
                    content: [
                        {
                            block: 'radio-button',
                            mods: {theme: 'normal', size: 'm'},
                            mix: {block: 'gemini', elem: 'item'},
                            name: 'show_to_m',
                            value: 'photo',
                            content: [
                                {
                                    elem: 'radio',
                                    controlAttrs: {value: 'photo'},
                                    elemMods: {'only-icon': 'yes'},
                                    content: [
                                        {
                                            block: 'image',
                                            mix: [{block: 'radio-button', elem: 'icon', elemMods: {16: 'photo'}}],
                                            alt: 'Photo'
                                        }
                                    ]
                                },
                                {
                                    elem: 'radio',
                                    controlAttrs: {value: 'comment'},
                                    elemMods: {'only-icon': 'yes'},
                                    content: [
                                        {
                                            block: 'image',
                                            mix: [{block: 'radio-button', elem: 'icon', elemMods: {16: 'comment'}}],
                                            alt: 'Comment'
                                        }
                                    ]
                                },
                                {
                                    elem: 'radio',
                                    controlAttrs: {value: 'settings'},
                                    elemMods: {'only-icon': 'yes'},
                                    content: [
                                        {
                                            block: 'image',
                                            mix: [{block: 'radio-button', elem: 'icon', elemMods: {16: 'settings'}}],
                                            alt: 'Settings'
                                        }
                                    ]
                                },
                                {
                                    elem: 'radio',
                                    controlAttrs: {value: 'view'},
                                    elemMods: {'only-icon': 'yes', disabled: 'yes'},
                                    content: [
                                        {
                                            block: 'image',
                                            mix: [{block: 'radio-button', elem: 'icon', elemMods: {16: 'view'}}],
                                            alt: 'View'
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            block: 'button2',
                            mods: {size: 'm', 'only-icon': 'yes', theme: 'normal'},
                            mix: {block: 'gemini', elem: 'item'},
                            url: '#',
                            content: [
                                {
                                    block: 'icon',
                                    mix: [{block: 'button2', elem: 'icon'}],
                                    url: 'https://yastatic.net/lego/_/sturUGEk_m_IG0u9Oo1lHK_4tGk.png',
                                    alt: 'Андроид'
                                }
                            ]
                        }
                    ]
                }
            ]
        },
        {
            block: 'gemini',
            mix: {block: 'radio-different-focus-checked'},
            content: {
                block: 'radio-button',
                mods: {theme: 'normal', size: 'm'},
                name: 'show_to4',
                content: [
                    {
                        elem: 'radio',
                        controlAttrs: {id: 'show_all', value: 'all'},
                        content: 'всем'
                    },
                    {
                        elem: 'radio',
                        controlAttrs: {value: 'friends'},
                        content: 'друзьям'
                    },
                    {
                        elem: 'radio',
                        controlAttrs: {value: 'me'},
                        elemMods: {checked: 'yes', focused: 'yes'},
                        content: 'мне'
                    },
                    {
                        elem: 'radio',
                        controlAttrs: {value: 'other'},
                        content: 'не мне'
                    }
                ]
            }
        },
        {
            block: 'gemini',
            mix: {block: 'radio-single'},
            attrs: {style: 'display: inline-block'},
            content: [
                {
                    block: 'radio-button',
                    mods: {theme: 'normal', size: 'm'},
                    mix: {block: 'gemini', elem: 'item'},
                    name: 'single_icon',
                    value: 'photo',
                    content: {
                        elem: 'radio',
                        controlAttrs: {value: 'photo'},
                        elemMods: {'only-icon': 'yes'},
                        content: [
                            {
                                block: 'image',
                                mix: [{block: 'radio-button', elem: 'icon', elemMods: {16: 'photo'}}],
                                alt: 'Photo'
                            }
                        ]
                    }
                },
                {
                    block: 'radio-button',
                    mods: {theme: 'normal', size: 'm'},
                    mix: {block: 'gemini', elem: 'item'},
                    name: 'single_text',
                    value: 'photo',
                    content: {
                        elem: 'radio',
                        controlAttrs: {value: 'photo'},
                        content: 'Одинокая радиокнопка'
                    }
                },
                {
                    block: 'radio-button',
                    mods: {theme: 'normal', size: 'm'},
                    mix: {block: 'gemini', elem: 'item'},
                    name: 'single_icon_text',
                    value: 'val',
                    content: {
                        elem: 'radio',
                        controlAttrs: {value: 'val'},
                        content: [
                            {
                                block: 'image',
                                mix: [{block: 'radio-button', elem: 'icon', elemMods: {16: 'photo'}}]
                            },
                            'Одинокая радиокнопка с иконкой'
                        ]
                    }
                }
            ]
        }
    ]
});
