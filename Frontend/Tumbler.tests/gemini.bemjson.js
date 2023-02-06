({
    block: 'b-page',
    title: 'tumbler',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        ['transparent', 'colored'].map(function(background) {
            return [
                {
                    block: 'gemini-tumblers',
                    mods: {
                        background: background,
                        disabled: 'no'
                    },
                    content: [
                        ['xs', 's', 'm', 'n'].map(function(size) {
                            return {
                                elem: 'tumbler',
                                content: {
                                    block: 'tumbler',
                                    mods: {
                                        theme: 'normal',
                                        size: size
                                    }
                                }
                            };
                        }),
                        {
                            elem: 'tumbler',
                            content: {
                                block: 'tumbler',
                                mods: {theme: 'tiny'}
                            }
                        }
                    ]
                },
                {
                    block: 'gemini-tumblers',
                    mods: {
                        background: background,
                        multiple: 'yes'
                    },
                    content: ['no', 'yes'].map(function(checked) {
                        return {
                            block: 'gemini-tumblers',
                            mods: {
                                background: background,
                                disabled: 'yes',
                                checked: checked
                            },
                            content: [
                                ['xs', 's', 'm', 'n'].map(function(size) {
                                    return {
                                        elem: 'tumbler',
                                        content: {
                                            block: 'tumbler',
                                            mods: {
                                                theme: 'normal',
                                                size: size,
                                                checked: checked === 'yes' ? 'yes' : undefined,
                                                disabled: 'yes'
                                            }
                                        }
                                    };
                                }),
                                {
                                    elem: 'tumbler',
                                    content: {
                                        block: 'tumbler',
                                        mods: {
                                            theme: 'tiny',
                                            checked: checked === 'yes' ? 'yes' : undefined,
                                            disabled: 'yes'
                                        }
                                    }
                                }
                            ]
                        };
                    })
                }
            ];
        }),
        {
            block: 'gemini-tumblers',
            mods: {context: 'inline'},
            content: [
                ['xs', 's', 'm', 'n'].map(function(size) {
                    return {
                        elem: 'tumbler',
                        elemMods: {size: size},
                        content: [
                            {
                                block: 'tumbler',
                                mods: {size: size, theme: 'normal'},
                                content: [
                                    {
                                        elem: 'option',
                                        side: 'left',
                                        content: 'выключить'
                                    },
                                    {
                                        elem: 'option',
                                        side: 'right',
                                        content: 'включить'
                                    }
                                ]
                            },
                            '\u00a0',
                            {
                                block: 'button2',
                                mods: {
                                    size: size,
                                    theme: 'normal'
                                },
                                text: 'Кнопка'
                            }
                        ]
                    };
                }),
                {
                    elem: 'tumbler',
                    elemMods: {size: 'xs'},
                    content: [
                        {
                            block: 'tumbler',
                            mods: {theme: 'tiny'},
                            content: [
                                {
                                    elem: 'option',
                                    side: 'left',
                                    content: 'выключить'
                                },
                                {
                                    elem: 'option',
                                    side: 'right',
                                    content: 'включить'
                                }
                            ]
                        },
                        '\u00a0',
                        {
                            block: 'button2',
                            mods: {
                                size: 'xs',
                                theme: 'normal'
                            },
                            text: 'Кнопка'
                        }
                    ]
                }
            ]
        },
        {
            block: 'gemini-tumblers',
            mix: {
                mods: {view: 'default'}
            },
            content: (function() {
                var tones = ['default', 'red', 'grey', 'dark'],
                    sizes = ['xs', 's', 'm', 'n'];

                return tones.map(function(tone) {
                    return [
                       sizes.map(function(size) {
                            return {
                                block: 'toner',
                                mods: {tone: tone, size: size},
                                content: {
                                    block: 'gemini-tumblers',
                                    elem: 'tumbler',
                                    content: [
                                        {
                                            block: 'tumbler',
                                            mods: {
                                                theme: 'normal',
                                                view: 'default',
                                                checked: 'yes',
                                                tone: tone,
                                                size: size
                                            },
                                            content: [
                                                {
                                                    elem: 'option',
                                                    side: 'left',
                                                    content: 'выключить'
                                                },
                                                {
                                                    elem: 'option',
                                                    side: 'right',
                                                    content: 'включить'
                                                }
                                            ]
                                        },
                                        '\u00a0',
                                        {
                                            block: 'button2',
                                            mods: {
                                                size: size,
                                                view: 'default',
                                                tone: tone,
                                                theme: 'normal'
                                            },
                                            text: 'Кнопка'
                                        },
                                        '\u00a0',
                                        {
                                            block: 'tumbler',
                                            mods: {
                                                theme: 'normal',
                                                view: 'default',
                                                tone: tone,
                                                size: size
                                            }
                                        }
                                    ]
                                }
                            };
                        }),
                        {tag: 'br'}
                    ];
                });
            })()
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
