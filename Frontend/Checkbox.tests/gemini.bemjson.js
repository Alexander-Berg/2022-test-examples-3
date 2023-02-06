({
    block: 'b-page',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        (function() {
            return [
                {
                    block: 'gemini',
                    id: 'static',
                    content: getCombinations({
                        theme: ['normal', 'pseudo'],
                        size: ['s', 'm'],
                        hovered: ['', 'yes'],
                        focused: ['', 'yes'],
                        pressed: ['', 'yes'],
                        checked: ['', 'yes'],
                        disabled: [''],
                        lines: ['multi']
                    }, {
                        theme: ['normal', 'pseudo'],
                        size: ['s', 'm'],
                        hovered: [''],
                        focused: [''],
                        pressed: [''],
                        checked: ['', 'yes'],
                        disabled: ['yes'],
                        lines: ['multi']
                    }).map(function(props) {
                        return {
                            elem: 'item',
                            content: [
                                {
                                    block: 'checkbox',
                                    mods: props,
                                    js: {
                                        live: true // Иначе скинутся _focused_yes.
                                    }
                                },
                                '\u00a0',
                                {
                                    block: 'checkbox',
                                    mods: props,
                                    text: 'checkbox',
                                    js: {
                                        live: true
                                    }
                                }
                            ]
                        };
                    })
                },
                {
                    block: 'gemini',
                    id: 'dynamic',
                    content: getCombinations({
                        theme: ['normal', 'pseudo'],
                        size: ['s'],
                        hovered: [''],
                        focused: [''],
                        pressed: [''],
                        checked: [''],
                        disabled: [''],
                        lines: ['multi']
                    }).map(function(props) {
                        return {
                            elem: 'item',
                            elemMods: props,
                            content: {
                                block: 'checkbox',
                                mods: props,
                                text: 'checkbox'
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
        })(),
        {
            block: 'gemini',
            id: 'baseline',
            content: {
                elem: 'item',
                content: [
                    'Чекбоксы\u00a0\u00a0',
                    {
                        block: 'checkbox',
                        mods: {
                            theme: 'normal',
                            size: 'm',
                            lines: 'multi'
                        },
                        text: 'выравниваются'
                    },
                    '\u00a0по базовой\u00a0\u00a0',
                    {
                        block: 'checkbox',
                        mods: {
                            theme: 'normal',
                            size: 's',
                            lines: 'multi'
                        },
                        text: ' линии'
                    },
                    '.'
                ]
            }
        },
        {
            block: 'gemini',
            id: 'lines',
            content: [
                {
                    elem: 'item',
                    elemMods: {
                        size: 's'
                    },
                    content: {
                        block: 'checkbox',
                        mods: {
                            theme: 'normal',
                            size: 's',
                            lines: 'multi'
                        },
                        text: 'Lorem Ipsum is simply dummy text of the printing and typesetting industry.'
                    }
                },
                {
                    tag: 'br'
                },
                {
                    elem: 'item',
                    elemMods: {
                        size: 'm'
                    },
                    content: {
                        block: 'checkbox',
                        mods: {
                            theme: 'normal',
                            size: 'm',
                            lines: 'multi'
                        },
                        text: 'Lorem Ipsum is simply dummy text of the printing and typesetting industry.'
                    }
                }
            ]
        },
        {
            block: 'gemini',
            id: 'new',
            content: (function() {
                var tones = ['default', 'red', 'grey', 'dark'],
                    sizes = ['s', 'm', 'n'];

                return sizes.map(function(size) {
                    return [
                        tones.map(function(tone) {
                            return {
                                elem: 'item',
                                mix: {block: 'toner', js: true, mods: {tone: tone, size: size, view: 'default'}},
                                content: [
                                    {
                                        block: 'checkbox',
                                        mods: {theme: 'normal', view: 'default', tone, size, lines: 'multi'},
                                        js: {live: true}
                                    },
                                    '\u00a0',
                                    {
                                        block: 'checkbox',
                                        mods: {theme: 'normal', checked: 'yes', view: 'default',
                                               tone, size, lines: 'multi'},
                                        text: 'checkbox',
                                        js: {live: true}
                                    }
                                ]
                            };
                        }),
                        {tag: 'br'}
                    ];
                });
            })()
        },
        {
            block: 'gemini',
            id: 'baseline-view-default',
            content: {
                elem: 'item',
                content: [
                    'Чекбоксы\u00a0\u00a0',
                    {
                        block: 'checkbox',
                        mods: {
                            theme: 'normal',
                            size: 'n',
                            view: 'default',
                            tone: 'default',
                            lines: 'multi'
                        },
                        text: 'выравниваются'
                    },
                    '\u00a0по базовой\u00a0\u00a0',
                    {
                        block: 'checkbox',
                        mods: {
                            theme: 'normal',
                            size: 'm',
                            view: 'default',
                            tone: 'default',
                            lines: 'multi'
                        },
                        text: ' линии,'
                    },
                    '\u00a0размер\u00a0n\u00a0\u00a0',
                    {
                        block: 'checkbox',
                        mods: {
                            theme: 'normal',
                            size: 'n',
                            view: 'default',
                            tone: 'default',
                            lines: 'multi'
                        },
                        text: '\u00a0не исключение'
                    },
                    '.'
                ]
            }
        },
        {
            block: 'gemini',
            id: 'lines-false',
            content: ['s', 'm', 'n'].map(function(size) {
                return {
                    elem: 'item',
                    elemMods: {
                        size: size
                    },
                    content: [
                        {
                            block: 'checkbox',
                            mods: {
                                theme: 'normal',
                                view: 'default',
                                tone: 'default',
                                size: size,
                                lines: false
                            },
                            text: 'Однострочный checkbox рядом с иконкой'
                        },
                        '\u00a0',
                        {block: 'icon', mods: {type: 'camera'}}
                    ]
                };
            })
        },
        {
            block: 'gemini',
            id: 'lines-one',
            content: ['s', 'm', 'n'].map(function(size) {
                return {
                    elem: 'item',
                    elemMods: {
                        size: size
                    },
                    content: [
                        {
                            block: 'checkbox',
                            mods: {
                                theme: 'normal',
                                view: 'default',
                                tone: 'default',
                                size: size,
                                lines: 'one'
                            },
                            text: 'Однострочный checkbox с длинной подписью'
                        }
                    ]
                };
            })
        },
        {
            block: 'gemini',
            id: 'baseline-lines-one',
            content: {
                elem: 'item',
                content: [
                    'Чекбоксы\u00a0\u00a0',
                    {
                        block: 'checkbox',
                        mods: {
                            theme: 'normal',
                            size: 'n',
                            view: 'default',
                            tone: 'default',
                            lines: 'one'
                        },
                        text: 'выравниваются'
                    },
                    '\u00a0по\u00a0базовой\u00a0\u00a0',
                    {
                        block: 'checkbox',
                        mods: {
                            theme: 'normal',
                            size: 'm',
                            view: 'default',
                            tone: 'default',
                            lines: 'one'
                        },
                        text: ' линии,'
                    },
                    '\u00a0однострочные\u00a0',
                    {
                        block: 'checkbox',
                        mods: {
                            theme: 'normal',
                            size: 'n',
                            view: 'default',
                            tone: 'default',
                            lines: 'one'
                        },
                        text: '\u00a0не исключение'
                    },
                    '.'
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
