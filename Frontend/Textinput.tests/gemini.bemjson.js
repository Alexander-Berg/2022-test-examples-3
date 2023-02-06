var sizes = ['xs', 's', 'm'],
    data = ['', 'это очень длинный текст и не влазит нифигашечки'],
    newSizes = ['n'],
    tones = ['default', 'red', 'grey', 'dark'];

({
    block: 'x-page',
    title: 'textinput',
    content: [].concat([
        {
            block: 'gemini',
            attrs: {id: 'websearch'},
            content: {
                elem: 'capture',
                content: {
                    elem: 'variation',
                    content: data.map(text => {
                        return {
                            elem: 'item',
                            content: {
                                block: 'textinput',
                                mods: {theme: 'websearch'},
                                text: text
                            }
                        };
                    })
                }
            }
        },
        {
            block: 'gemini',
            attrs: {id: 'pin'},
            content: {
                elem: 'capture',
                content: [
                    ['round-round'],
                    ['round-clear', 'brick-brick', 'clear-brick', 'clear-round'],
                    ['round-brick', 'clear-clear', 'brick-clear', 'brick-round']
                ].map((row, i) => {
                    return {
                        elem: 'variation',
                        attrs: {id: `row_${i}`},
                        content: {
                            elem: 'item',
                            content: row.map(pin => {
                                return {
                                    block: 'textinput',
                                    attrs: {style: 'width: 150px;'},
                                    mods: {theme: 'normal', size: 'm', pin: pin}
                                };
                            })
                        }
                    };
                })
            }
        },
        {
            block: 'gemini',
            attrs: {id: 'base'},
            content: sizes.map(size => {
                const mods = Object.assign({theme: 'normal'}, {size: size});
                return {
                    elem: 'capture',
                    elemMods: {size: size},
                    content: [
                        {},
                        {disabled: 'yes'},
                        {'has-clear': 'yes'}
                    ].map(state => {
                        const extend = Object.assign({}, mods, state);
                        return {
                            elem: 'variation',
                            elemMods: state,
                            content: data.map(text => {
                                return {
                                    elem: 'item',
                                    elemMods: {'has-value': text && 'yes'},
                                    content: {
                                        block: 'textinput',
                                        mods: extend,
                                        text: text
                                    }
                                };
                            })
                        };
                    })
                };
            })
        },
        {
            block: 'gemini',
            attrs: {id: 'number'},
            content: sizes.map(size => {
                const type = {type: 'number'},
                    mods = Object.assign({theme: 'normal'}, type, {size: size});
                return {
                    elem: 'capture',
                    elemMods: Object.assign(type, {size: size}),
                    content: {
                        elem: 'variation',
                        content: ['', 1234].map(text => {
                            return {
                                elem: 'item',
                                elemMods: {'has-value': text && 'yes'},
                                content: {
                                    block: 'textinput',
                                    mods: mods,
                                    text: text
                                }
                            };
                        })
                    }
                };
            })
        },
        {
            block: 'gemini',
            attrs: {id: 'icons'},
            content: sizes.map(size => {
                const mods = Object.assign({theme: 'normal'}, {size: size});
                return {
                    elem: 'capture',
                    elemMods: {size: size},
                    content: [
                        {'has-icon': 'yes'},
                        {'has-icon': 'yes', 'has-clear': 'yes'}
                    ].map(state => {
                        const extend = Object.assign({}, mods, state);
                        return {
                            elem: 'variation',
                            elemMods: state,
                            content: data.map(text => {
                                return [
                                    {
                                        elem: 'item',
                                        content: {
                                            block: 'textinput',
                                            mods: extend,
                                            icon: {block: 'icon', mods: {size: size, type: 'eye'}},
                                            text: text
                                        }
                                    },
                                    {
                                        elem: 'item',
                                        content: {
                                            block: 'textinput',
                                            mods: extend,
                                            iconLeft: {block: 'icon', mods: {size: size, type: 'lock'}},
                                            text: text
                                        }
                                    },
                                    {
                                        elem: 'item',
                                        content: {
                                            block: 'textinput',
                                            mods: extend,
                                            iconLeft: {block: 'icon', mods: {size: size, type: 'lock'}},
                                            iconRight: {block: 'icon', mods: {size: size, type: 'eye'}},
                                            text: text
                                        }
                                    }
                                ];
                            })
                        };
                    })
                };
            })
        },

        {
            block: 'x-deps',
            content: [
                {block: 'textinput', elem: 'clear', elemMods: {theme: 'normal'}}
            ]
        }

    ], tones.map(tone => [
        {
            block: 'gemini',
            attrs: {id: `tone-${tone}-placeholder`},
            mix: {block: 'toner', js: true, mods: {tone, view: 'default'}},
            content: {
                elem: 'capture',
                content: {
                    elem: 'variation',
                    content: ['typical', 'focused', 'hovered'].map(state => ({
                        elem: 'item',
                        content: {
                            block: 'textinput',
                            attrs: {style: 'width: 150px'},
                            mods: Object.assign({view: 'default', tone, theme: 'normal'}, {[state]: 'yes'}),
                            placeholder: `A ${state} placeholder`
                        }
                    }))
                }
            }
        },
        {
            block: 'gemini',
            attrs: {id: `tone-${tone}-pin`},
            mix: {block: 'toner', js: true, mods: {tone, view: 'default'}},
            content: {
                elem: 'capture',
                content: [
                    [['round-round', 'brick-round', 'clear-round'], {}],
                    [['round-brick', 'brick-brick', 'clear-brick'], {}],
                    [['round-clear', 'brick-clear', 'clear-clear'], {}],
                    [['round-round', 'brick-round', 'clear-round'], {focused: 'yes'}],
                    [['round-brick', 'brick-brick', 'clear-brick'], {focused: 'yes'}],
                    [['round-clear', 'brick-clear', 'clear-clear'], {focused: 'yes'}],
                    [['round-round', 'brick-round', 'clear-round'], {hovered: 'yes'}],
                    [['round-brick', 'brick-brick', 'clear-brick'], {hovered: 'yes'}],
                    [['round-clear', 'brick-clear', 'clear-clear'], {hovered: 'yes'}]
                ].map((row, i) => {
                    return {
                        elem: 'variation',
                        attrs: {id: `row_${i}`},
                        content: {
                            elem: 'item',
                            attrs: {style: 'padding: 5px'},
                            content: row[0].map(pin => ({
                                block: 'textinput',
                                attrs: {style: 'width:150px;margin:0 5px 0 0'},
                                mods: Object.assign({view: 'default', tone, theme: 'normal', pin: pin}, row[1]),
                                placeholder: pin
                            }))
                        }
                    };
                })
            }
        },
        {
            block: 'gemini',
            attrs: {id: `tone-${tone}-base`},
            mix: {block: 'toner', js: true, mods: {tone, view: 'default'}},
            content: newSizes.map(size => {
                const mods = Object.assign({view: 'default', tone, theme: 'normal'}, {size: size});
                return {
                    elem: 'capture',
                    elemMods: {size: size},
                    content: [
                        {},
                        {disabled: 'yes'},
                        {'has-clear': 'yes'}
                    ].map(state => {
                        const extend = Object.assign({}, mods, state);
                        return {
                            elem: 'variation',
                            elemMods: state,
                            content: data.map(text => {
                                return {
                                    elem: 'item',
                                    elemMods: {'has-value': text && 'yes'},
                                    content: {
                                        block: 'textinput',
                                        mods: extend,
                                        text: text
                                    }
                                };
                            })
                        };
                    })
                };
            })
        },
        {
            block: 'gemini',
            attrs: {id: `tone-${tone}-number`},
            mix: {block: 'toner', js: true, mods: {tone, view: 'default'}},
            content: newSizes.map(size => {
                const type = {type: 'number'},
                    mods = Object.assign({view: 'default', tone, theme: 'normal'}, type, {size: size});
                return {
                    elem: 'capture',
                    elemMods: Object.assign(type, {size: size}),
                    content: {
                        elem: 'variation',
                        content: ['', 1234].map(text => {
                            return {
                                elem: 'item',
                                elemMods: {'has-value': text && 'yes'},
                                content: {
                                    block: 'textinput',
                                    mods: mods,
                                    text: text
                                }
                            };
                        })
                    }
                };
            })
        },
        {
            block: 'gemini',
            attrs: {id: `tone-${tone}-icons`},
            mix: {block: 'toner', js: true, mods: {tone, view: 'default'}},
            content: {
                elem: 'capture',
                content: [
                    {'has-icon': 'yes'},
                    {'has-icon': 'yes', 'has-clear': 'yes'}
                ].map(state =>
                    newSizes.map(size => {
                        const icon = type => ({
                            block: 'icon',
                            mods: {size: size, glyph: 'type-' + type}
                        });
                        const mods = Object.assign({view: 'default', tone, theme: 'normal', size: size}, state);
                        return {
                            elem: 'variation',
                            elemMods: state,
                            content: data.map(text => {
                                return [
                                    {
                                        elem: 'item',
                                        content: {
                                            block: 'textinput',
                                            mods,
                                            icon: icon('eye'),
                                            text
                                        }
                                    },
                                    {
                                        elem: 'item',
                                        content: {
                                            block: 'textinput',
                                            mods,
                                            iconLeft: icon('lock'),
                                            text
                                        }
                                    },
                                    {
                                        elem: 'item',
                                        content: {
                                            block: 'textinput',
                                            mods,
                                            iconLeft: icon('lock'),
                                            iconRight: icon('eye'),
                                            text
                                        }
                                    }
                                ];
                            })
                        };
                    })
                )
            }
        },
        {
            block: 'x-deps',
            content: {block: 'textinput', mods: {tone}}
        }
    ]), [
        {
            block: 'x-deps',
            content: [
                {block: 'textinput', elem: 'clear', elemMods: {theme: 'normal'}}
            ]
        }
    ])
});
