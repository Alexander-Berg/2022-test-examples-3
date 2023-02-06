({
    block: 'b-page',
    title: 'Радио-группа',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true},
        {elem: 'meta', attrs: {name: 'viewport', content: 'initial-scale=1'}}
    ],
    content: [
        {
            block: 'gemini',
            attrs: {id: 'simple', style: 'margin: 20px'},
            content: {
                block: 'radiobox',
                mods: {size: 's', theme: 'normal'},
                name: 'bla',
                content: [
                    {
                        elem: 'radio',
                        content: 'Радио 1',
                        value: 'val-1'
                    },
                    {
                        elem: 'radio',
                        content: 'Радио 2',
                        value: 'val-2'
                    },
                    {
                        elem: 'radio',
                        content: 'Радио 3',
                        value: 'val-3'
                    }
                ]
            }
        },
        {
            block: 'gemini',
            attrs: {id: 'simple-m', style: 'margin: 20px'},
            content: {
                block: 'radiobox',
                mods: {size: 'm', theme: 'normal'},
                name: 'bla',
                content: [
                    {
                        elem: 'radio',
                        content: 'Радио 1',
                        value: 'val-1'
                    },
                    {
                        elem: 'radio',
                        content: 'Радио 2',
                        value: 'val-2'
                    },
                    {
                        elem: 'radio',
                        content: 'Радио 3',
                        value: 'val-3'
                    }
                ]
            }
        },
        {
            block: 'gemini',
            attrs: {id: 'bg-color', style: 'margin: 20px;background: #f5f5ea;padding: 5px;display: inline-block;'},
            content: {
                block: 'radiobox',
                mods: {size: 's', theme: 'normal'},
                name: 'bg-color',
                content: {
                    elem: 'radio',
                    content: 'Радио 1',
                    value: 'bg-color'
                }
            }
        },

        {
            block: 'gemini',
            attrs: {id: 'simple-pseudo', style: 'margin: 20px'},
            content: {
                block: 'radiobox',
                mods: {size: 's', theme: 'pseudo'},
                name: 'bla',
                content: [
                    {
                        elem: 'radio',
                        content: 'Радио 1',
                        value: 'val-1'
                    },
                    {
                        elem: 'radio',
                        content: 'Радио 2',
                        value: 'val-2'
                    },
                    {
                        elem: 'radio',
                        content: 'Радио 3',
                        value: 'val-3'
                    }
                ]
            }
        },
        {
            block: 'gemini',
            attrs: {
                id: 'bg-color-pseudo',
                style: 'margin: 20px;background: #f5f5ea;padding: 5px;display: inline-block;'
            },
            content: {
                block: 'radiobox',
                mods: {size: 's', theme: 'pseudo'},
                name: 'bg-color',
                content: {
                    elem: 'radio',
                    content: 'Радио 1',
                    value: 'bg-color'
                }
            }
        },

        {
            block: 'gemini',
            attrs: {id: 'disabled', style: 'margin: 20px'},
            content: [
                {
                    block: 'radiobox',
                    mods: {size: 's', disabled: 'yes', theme: 'normal'},
                    name: 'bla-3',
                    content: [
                        {
                            elem: 'radio',
                            content: 'только друзьям',
                            value: 'val-1'
                        },
                        {
                            elem: 'radio',
                            elemMods: {checked: 'yes'},
                            content: 'только мне',
                            value: 'val-2'
                        },
                        {
                            elem: 'radio',
                            content: 'только не мне',
                            value: 'val-3'
                        }
                    ]
                }
            ]
        },

        {
            block: 'gemini',
            attrs: {id: 'disabled-pseudo', style: 'margin: 20px'},
            content: [
                {
                    block: 'radiobox',
                    mods: {size: 's', disabled: 'yes', theme: 'pseudo'},
                    name: 'bla-3',
                    content: [
                        {
                            elem: 'radio',
                            content: 'только друзьям',
                            value: 'val-1'
                        },
                        {
                            elem: 'radio',
                            elemMods: {checked: 'yes'},
                            content: 'только мне',
                            value: 'val-2'
                        },
                        {
                            elem: 'radio',
                            content: 'только не мне',
                            value: 'val-3'
                        }
                    ]
                }
            ]
        },

        {
            block: 'gemini',
            attrs: {id: 'different', style: 'margin: 20px'},
            content: [
                {
                    block: 'radiobox',
                    mods: {size: 's', theme: 'normal'},
                    name: 'bla-7',
                    content: [
                        {
                            elem: 'radio',
                            elemMods: {checked: 'yes'},
                            content: 'только друзьям',
                            value: 'val-1'
                        },
                        {
                            elem: 'radio',
                            elemMods: {focused: 'yes'},
                            content: 'только мне',
                            value: 'val-2'
                        },
                        {
                            elem: 'radio',
                            elemMods: {disabled: 'yes'},
                            content: 'только не мне',
                            value: 'val-3'
                        }
                    ]
                }
            ]
        },
        {
            block: 'gemini',
            attrs: {id: 'new', style: 'margin: 20px'},
            content: (function() {
                var tones = ['default', 'red', 'grey', 'dark'],
                    sizes = ['s', 'm', 'n'];

                return tones.map(function(tone) {
                    return [
                        sizes.map(function(size) {
                            return {
                                elem: 'item',
                                mix: {block: 'toner', js: true, mods: {tone: tone, size: size, view: 'default'}},
                                content: {
                                    block: 'radiobox',
                                    mods: {view: 'default', theme: 'normal', size: size, tone: tone},
                                    name: tone + size,
                                    content: [
                                        {
                                            elem: 'radio',
                                            content: 'Радио 1',
                                            value: 'val-1'
                                        },
                                        {
                                            elem: 'radio',
                                            elemMods: {checked: 'yes'},
                                            content: 'Радио 2',
                                            value: 'val-2'
                                        },
                                        {
                                            elem: 'radio',
                                            content: 'Радио 3',
                                            value: 'val-3'
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
