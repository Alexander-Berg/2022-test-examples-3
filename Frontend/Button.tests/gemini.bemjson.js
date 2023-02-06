({
    block: 'b-page',
    title: 'Простая кнопка',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'gemini-wrapper',
            content: [
                {
                    block: 'button-normal',
                    attrs: {style: 'margin: 10px;'},
                    content: {
                        block: 'button',
                        mods: {size: 'm', theme: 'normal'},
                        content: 'Я.Button'
                    }
                },
                {
                    block: 'button-bg-color',
                    attrs: {style: 'margin: 10px;; background: #f5f5ea; padding: 5px; display: inline-block;'},
                    content: {
                        block: 'button',
                        mods: {size: 'm', theme: 'normal'},
                        content: 'Я.Button'
                    }
                },
                {
                    block: 'button-focus-no',
                    attrs: {style: 'margin: 10px;'},
                    content: {
                        block: 'button',
                        mods: {size: 'm', focus: 'no', theme: 'normal'},
                        content: 'Я.Button'
                    }
                },
                {
                    block: 'button-action',
                    attrs: {style: 'margin: 10px;'},
                    content: {
                        block: 'button',
                        mods: {theme: 'action', size: 'm'},
                        name: 'my-submit',
                        content: 'Я.Submit'
                    }
                },
                {
                    block: 'button-link',
                    attrs: {style: 'margin: 10px;'},
                    content: {
                        block: 'button',
                        mods: {size: 'm', theme: 'normal'},
                        url: '#',
                        content: 'Я.Ссылка'
                    }
                },
                {
                    block: 'button-pseudo',
                    attrs: {style: 'margin: 10px;'},
                    content: {
                        block: 'button',
                        mods: {size: 'm', theme: 'pseudo'},
                        content: 'Я.Псевдокнопка'
                    }
                },
                {
                    block: 'button-sides',
                    attrs: {style: 'margin: 10px;; display:inline-block; padding: 6px;'},
                    content: [
                        {
                            block: 'button',
                            mods: {size: 'm', side: 'left', theme: 'normal'},
                            content: 'side: left'
                        },
                        {
                            block: 'button',
                            mods: {size: 'm', side: 'right', theme: 'normal'},
                            content: 'side: right'
                        }
                    ]
                },
                {
                    block: 'button-xs',
                    attrs: {style: 'margin: 10px;'},
                    content: {
                        block: 'button',
                        mods: {size: 'xs', theme: 'normal'},
                        content: 'Я.Кнопка размером XS'
                    }
                },
                {
                    block: 'button-s',
                    attrs: {style: 'margin: 10px;'},
                    content: {
                        block: 'button',
                        mods: {size: 's', theme: 'normal'},
                        content: 'Я.Кнопка размером S'
                    }
                },
                {
                    block: 'button-m',
                    attrs: {style: 'margin: 10px;'},
                    content: {
                        block: 'button',
                        mods: {size: 'm', theme: 'normal'},
                        content: 'Я.Кнопка размером M'
                    }
                },
                {
                    block: 'button-l',
                    attrs: {style: 'margin: 10px;'},
                    content: {
                        block: 'button',
                        mods: {size: 'l', theme: 'normal'},
                        content: 'Я.Кнопка размером L'
                    }
                },
                {
                    block: 'button-only-icon-s',
                    attrs: {style: 'margin: 10px;'},
                    content: {
                        block: 'button',
                        mods: {size: 's', 'only-icon': 'yes', theme: 'normal'},
                        content: [
                            {
                                block: 'image',
                                mix: [{block: 'button', elem: 'icon', elemMods: {16: 'comment'}}],
                                alt: ''
                            }
                        ]
                    }
                },
                {
                    block: 'button-only-icon-link',
                    attrs: {style: 'margin: 10px;'},
                    content: {
                        block: 'button',
                        mods: {size: 'm', 'only-icon': 'yes', theme: 'normal'},
                        url: '#',
                        content: [
                            {
                                block: 'image',
                                mix: [{block: 'button', elem: 'icon', elemMods: {16: 'comment'}}],
                                alt: ''
                            }
                        ]
                    }
                }
            ]
        },
        {
            block: 'gemini-wrapper',
            content: [
                {
                    block: 'button-icon-text',
                    attrs: {style: 'margin: 10px;'},
                    content: {
                        block: 'button',
                        mods: {size: 'm', theme: 'normal'},
                        content: [
                            {
                                block: 'image',
                                mix: [{block: 'button', elem: 'icon', elemMods: {16: 'settings'}}],
                                alt: '/'
                            },
                            'Я.Кнопка'
                        ]
                    }
                },
                {
                    block: 'button-theme-clear-with-text',
                    attrs: {style: 'margin: 10px;'},
                    content: {
                        block: 'button',
                        mods: {theme: 'clear', size: 'm'},
                        content: [
                            {
                                block: 'image',
                                mix: [{block: 'button', elem: 'icon', elemMods: {16: 'settings'}}],
                                alt: '/'
                            },
                            'Я.Кнопка clear'
                        ]
                    }
                },
                {
                    block: 'button-theme-clear-rounded',
                    attrs: {style: 'margin: 10px;'},
                    content: {
                        block: 'button',
                        mods: {
                            theme: 'clear',
                            round: 'yes',
                            state: 'play'
                        }
                    }
                },
                {
                    block: 'button-icon-text-link',
                    attrs: {style: 'margin: 10px;'},
                    content: {
                        block: 'button',
                        mods: {size: 'm', theme: 'normal'},
                        url: '#',
                        content: [
                            {
                                block: 'image',
                                mix: [{block: 'button', elem: 'icon', elemMods: {16: 'settings'}}],
                                alt: '/'
                            },
                            'Я.Ссылка'
                        ]
                    }
                },
                {
                    block: 'button-disabled',
                    attrs: {style: 'margin: 10px;'},
                    content: [
                        {
                            block: 'button',
                            mods: {size: 's', theme: 'normal', disabled: 'yes'},
                            content: 'Я.Кнопка'
                        },
                        {tag: 'br'},
                        {tag: 'br'},
                        {
                            block: 'button',
                            mods: {size: 'm', 'only-icon': 'yes', disabled: 'yes', theme: 'normal'},
                            url: '#',
                            content: [
                                {
                                    block: 'image',
                                    mix: [{block: 'button', elem: 'icon', elemMods: {16: 'comment'}}],
                                    alt: ''
                                }
                            ]
                        },
                        {tag: 'br'},
                        {tag: 'br'},
                        {
                            block: 'button',
                            mods: {theme: 'action', size: 'm', disabled: 'yes'},
                            name: 'my-submit',
                            content: 'Я.Submit'
                        },
                        {tag: 'br'},
                        {tag: 'br'},
                        {
                            block: 'button',
                            mods: {size: 'm', theme: 'pseudo', disabled: 'yes'},
                            content: 'Я.Псевдокнопка'
                        }
                    ]
                },
                {
                    block: 'button-arrow',
                    attrs: {style: 'margin: 10px;'},
                    content: [
                        {
                            block: 'button',
                            mods: {size: 'xs', theme: 'normal', arrow: 'down'},
                            content: 'Я.Кнопка'
                        },
                        {tag: 'br'},
                        {tag: 'br'},
                        {
                            block: 'button',
                            mods: {size: 's', arrow: 'down', disabled: 'yes', theme: 'normal'},
                            content: 'Я.Кнопка'
                        },
                        {tag: 'br'},
                        {tag: 'br'},
                        {
                            block: 'button',
                            mods: {theme: 'action', size: 'm', arrow: 'up'},
                            name: 'my-submit',
                            content: 'Я.Submit'
                        },
                        {tag: 'br'},
                        {tag: 'br'},
                        {
                            block: 'button',
                            mods: {theme: 'action', size: 'l', arrow: 'up', disabled: 'yes'},
                            content: 'Я.Submit'
                        }
                    ]
                },
                {
                    block: 'button-round',
                    attrs: {style: 'margin: 10px;'},
                    content: [
                        {
                            block: 'button',
                            mods: {
                                round: 'yes',
                                state: 'pause',
                                theme: 'normal'
                            }
                        },
                        {tag: 'br'},
                        {tag: 'br'},
                        {
                            block: 'button',
                            mods: {
                                round: 'yes',
                                state: 'play',
                                theme: 'normal'
                            }
                        },
                        {tag: 'br'},
                        {tag: 'br'},
                        {
                            block: 'button',
                            mods: {
                                round: 'yes',
                                state: 'radio',
                                disabled: 'yes',
                                theme: 'normal'
                            }
                        }
                    ]
                },
                {
                    block: 'button-round-link',
                    attrs: {style: 'margin: 10px;'},
                    content: [
                        {
                            block: 'button',
                            url: '#',
                            mods: {
                                round: 'yes',
                                state: 'pause',
                                theme: 'normal'
                            }
                        },
                        {tag: 'br'},
                        {tag: 'br'},
                        {
                            block: 'button',
                            url: '#',
                            mods: {
                                round: 'yes',
                                state: 'play',
                                theme: 'normal'
                            }
                        },
                        {tag: 'br'},
                        {tag: 'br'},
                        {
                            block: 'button',
                            url: '#',
                            mods: {
                                round: 'yes',
                                state: 'radio',
                                disabled: 'yes',
                                theme: 'normal'
                            }
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
