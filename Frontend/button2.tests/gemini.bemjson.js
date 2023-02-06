({
    block: 'b-page',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        // Комбинации состояний/тем.
        {

            content: (function() {
                return [
                    {
                        block: 'gemini',
                        id: 'static',
                        content: getCombinations({
                            type: [''],
                            size: ['s'],
                            theme: ['normal', 'pseudo', 'clear'],
                            hovered: ['', 'yes'],
                            pressed: ['', 'yes'],
                            checked: ['', 'yes'],
                            focused: ['', 'yes'],
                            disabled: ['']
                        }, {
                            type: [''],
                            size: ['s'],
                            theme: ['action'],
                            hovered: ['', 'yes'],
                            pressed: ['', 'yes'],
                            checked: [''],
                            focused: ['', 'yes'],
                            disabled: ['']
                        }, {
                            type: [''],
                            size: ['s'],
                            theme: ['normal', 'pseudo', 'clear'],
                            hovered: [''],
                            pressed: [''],
                            checked: ['', 'yes'],
                            focused: [''],
                            disabled: ['yes']
                        }, {
                            type: [''],
                            size: ['s'],
                            theme: ['action'],
                            hovered: [''],
                            pressed: [''],
                            checked: [''],
                            focused: [''],
                            disabled: ['yes']
                        }).map(function(props) {
                            return {
                                elem: 'item',
                                data: props,
                                content: {
                                    block: 'button2',
                                    text: 'button',
                                    mods: props
                                }
                            };
                        })
                    },
                    {
                        block: 'gemini',
                        id: 'dynamic',
                        content: getCombinations({
                            type: ['', 'check', 'radio'],
                            size: ['s'],
                            theme: ['normal', 'pseudo', 'action', 'clear'],
                            hovered: [''],
                            pressed: [''],
                            checked: [''],
                            focused: [''],
                            disabled: ['']
                        }).map(function(props) {
                            return {
                                elem: 'item',
                                data: props,
                                content: {
                                    block: 'button2',
                                    text: 'button',
                                    mods: props
                                }
                            };
                        })
                    }
                ];

                function getCombinations(obj) {
                    if(arguments.length > 1) {
                        return [].slice.call(arguments).reduce(function(res, obj) {
                            return res.concat(getCombinations(obj));
                        }, []);
                    }

                    var keys = Object.keys(obj),
                        vals = keys.map(function(key) {
                            return Array.isArray(obj[key]) ? obj[key] : [obj[key]];
                        });

                    return (function self(arr) {
                        return arr.length === 1 ? arr[0] : arr[0].reduce(function(result, base) {
                            self(arr.slice(1)).forEach(function(tail) {
                                result.push([base].concat(tail));
                            });
                            return result;
                        }, []);
                    })(vals).map(function(arr) {
                        return arr.reduce(function(result, val, i) {
                            result[keys[i]] = val;
                            return result;
                        }, {});
                    });
                }
            })()
        },

        // Размеры.
        {
            block: 'gemini',
            id: 'sizes',
            content: (['xs', 's', 'm', 'l']).map(function(size) {
                return [{
                    elem: 'item',
                    data: {size: size},
                    content: [
                        {
                            block: 'button2',
                            mods: {theme: 'normal', size: size},
                            icon: {mods: {type: 'load'}}
                        },
                        '\u00a0',
                        {
                            block: 'button2',
                            mods: {theme: 'normal', size: size},
                            text: 'button'
                        },
                        '\u00a0',
                        {
                            block: 'button2',
                            mods: {theme: 'normal', size: size},
                            iconLeft: {mods: {type: 'load'}},
                            text: 'button'
                        },
                        '\u00a0',
                        {
                            block: 'button2',
                            mods: {theme: 'normal', size: size},
                            iconLeft: {mods: {type: 'arrow', direction: 'left'}},
                            text: 'button'
                        },
                        '\u00a0',
                        {
                            block: 'button2',
                            mods: {theme: 'normal', size: size},
                            text: 'button',
                            iconRight: {mods: {type: 'load'}}
                        },
                        '\u00a0',
                        {
                            block: 'button2',
                            mods: {theme: 'normal', size: size},
                            text: 'button',
                            iconRight: {mods: {type: 'arrow', direction: 'right'}}
                        },
                        '\u00a0',
                        {
                            block: 'button2',
                            mods: {theme: 'normal', size: size},
                            iconLeft: {mods: {type: 'load'}},
                            text: 'button',
                            iconRight: {mods: {type: 'load'}}
                        },
                        '\u00a0',
                        {
                            block: 'button2',
                            mods: {theme: 'normal', size: size},
                            iconLeft: {mods: {type: 'arrow', direction: 'left'}},
                            text: 'button',
                            iconRight: {mods: {type: 'arrow', direction: 'right'}}
                        }
                    ]
                }, {tag: 'br'}];
            })
        },

        // Выравнивание по базовой линии.
        {
            block: 'gemini',
            id: 'baseline',
            content: {
                elem: 'item',
                content: [
                    ' Кнопки ',
                    {
                        block: 'button2',
                        mods: {theme: 'normal', size: 'xs'},
                        text: 'button'
                    },
                    ' выравниваются ',
                    {
                        block: 'button2',
                        mods: {theme: 'normal', size: 's'},
                        text: 'button'
                    },
                    ' по базовой ',
                    {
                        block: 'button2',
                        mods: {theme: 'normal', size: 'm'},
                        text: 'button'
                    },
                    ' линии. '
                ]
            }
        },

        // Модификатор width.
        {
            block: 'gemini',
            id: 'width',
            content: ['200', '100'].map(function(width) {
                return {
                    elem: 'item',
                    data: {width: width},
                    attrs: {style: 'width:' + width + 'px;'},
                    content: [
                        {
                            block: 'button2',
                            mods: {theme: 'normal', size: 's'},
                            text: 'синхрофазотрон'
                        },
                        {tag: 'br'},
                        {tag: 'br'},
                        {
                            block: 'button2',
                            mods: {theme: 'normal', size: 's', width: 'auto'},
                            text: 'синхрофазотрон'
                        },
                        {tag: 'br'},
                        {tag: 'br'},
                        {
                            block: 'button2',
                            mods: {theme: 'normal', size: 's', width: 'max'},
                            text: 'синхрофазотрон'
                        }
                    ]
                };
            })
        },

        // Pale
        {
            block: 'gemini',
            id: 'pale',
            content: [
                {
                    elem: 'item',
                    content: [
                        {
                            block: 'button2',
                            mods: {theme: 'normal', size: 's', pale: 'yes'},
                            text: 'pale'
                        }
                    ]
                },
                {
                    elem: 'item',
                    content: [
                        {
                            block: 'button2',
                            mods: {theme: 'clear', size: 's', pale: 'yes'},
                            text: 'pale'
                        }
                    ]
                },
                {
                    elem: 'item',
                    content: [
                        {
                            block: 'button2',
                            mods: {theme: 'normal', size: 's', pale: 'yes'},
                            iconLeft: {mods: {type: 'load'}},
                            text: 'pale'
                        }
                    ]
                }
            ]
        },

        // Составные.
        {
            block: 'gemini',
            id: 'pin',
            content: {
                elem: 'item',
                content: ([
                    'round-clear', 'clear-round', 'round-brick', 'brick-round',
                    'brick-clear', 'clear-brick', 'brick-brick', 'clear-clear'
                ]).map(function(pin) {
                    return [{
                        block: 'button2',
                        mods: {theme: 'normal', size: 's', pin: pin},
                        text: 'button'
                    }, {elem: 'gap'}];
                })
            }
        },
        {
            block: 'gemini',
            id: 'circle',
            content: {
                elem: 'item',
                content: ([
                    'circle-circle', 'circle-brick', 'brick-circle',
                    'clear-circle', 'circle-clear'
                ]).map(function(circlePin) {
                    return [{
                        block: 'button2',
                        mods: {theme: 'normal', size: 's', pin: circlePin},
                        text: 'button'
                    }, {elem: 'gap'}];
                })
            }
        },
        {
            block: 'gemini',
            id: 'icon-circle',
            content: {
                elem: 'item',
                content: (['location', 'home', 'play']).map(function(icon) {
                    return [{
                        block: 'button2',
                        icon: {mods: {type: icon}},
                        mods: {theme: 'normal', size: 'l', pin: 'circle-circle'}
                    }, {elem: 'gap'}];
                })
            }
        },
        {
            block: 'gemini',
            id: 'icon-glyph',
            content: {
                elem: 'item',
                content: ['classic', 'default'].map(function(view) {
                    return [
                        {
                            block: 'button2',
                            mods: {
                                theme: 'normal',
                                size: 'm',
                                tone: 'red',
                                view: view
                            },
                            text: 'Кнопка',
                            iconLeft: {block: 'icon', mods: {glyph: 'type-load'}}
                        },
                        {elem: 'gap'},
                        {
                            block: 'button2',
                            mods: {
                                theme: 'normal',
                                size: 'm',
                                tone: 'red',
                                view: view
                            },
                            text: 'Кнопка',
                            iconRight: {block: 'icon', mods: {glyph: 'type-load'}}
                        },
                        {elem: 'gap'},
                        {
                            block: 'button2',
                            mods: {
                                theme: 'normal',
                                size: 'm',
                                tone: 'red',
                                view: view
                            },
                            text: 'Кнопка',
                            iconRight: {block: 'icon', mods: {glyph: 'type-load'}},
                            iconLeft: {block: 'icon', mods: {glyph: 'type-load'}}
                        },
                        {elem: 'gap'},
                        {
                            block: 'button2',
                            mods: {
                                theme: 'normal',
                                size: 'm',
                                tone: 'red',
                                view: view
                            },
                            icon: {block: 'icon', mods: {glyph: 'type-load'}}
                        },
                        {tag: 'br'},
                        {tag: 'br'}
                    ];
                })
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
