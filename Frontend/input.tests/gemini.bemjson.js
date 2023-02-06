({
    block: 'b-page',
    title: 'input',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'gemini-common',
            mix: {block: 'gemini-simple'},
            content: [
                {
                    block: 'input',
                    mods: {size: 's'},
                    content: [
                        {elem: 'shadow'},
                        {elem: 'control'},
                        {
                            elem: 'samples',
                            content: [
                                {
                                    block: 'link',
                                    mods: {theme: false, pseudo: 'yes'},
                                    mix: [{block: 'input', elem: 'sample'}],
                                    content: 'Sample'
                                }
                            ]
                        }
                    ]
                },
                {tag: 'br'},
                {tag: 'br'},
                {
                    block: 'input',
                    mods: {size: 'm', width: 'content', theme: 'websearch'},
                    content: [
                        {
                            elem: 'found',
                            content: '– 142 млн ответов'
                        },
                        {elem: 'control'}
                    ],
                    value: 'Сразу с текстом'
                },
                {tag: 'br'},
                {tag: 'br'},
                {
                    block: 'input',
                    mods: {size: 's'},
                    content: {elem: 'control'},
                    value: 'Очень большая строка, которая не влезает в текстовое поле'
                },
                {tag: 'br'},
                {tag: 'br'},
                {
                    block: 'input',
                    mods: {size: 's'},
                    value: 'С лейблами',
                    content: [
                        {elem: 'label', content: 'Я метка'},
                        {elem: 'control'},
                        {
                            elem: 'message',
                            elemMods: {type: 'error', visibility: 'visible'},
                            content: 'Я ошибка'
                        }
                    ]
                },
                {tag: 'br'},
                {tag: 'br'},
                {
                    block: 'input',
                    mods: {size: 's'},
                    content: {elem: 'control', attrs: {type: 'search'}},
                    value: 'type: search | Текстовое поле с поисковым типом и длинным текстом внутри ' +
                        'должно корректно обрезаться многоточием'
                },
                {tag: 'br'},
                {tag: 'br'},
                {
                    block: 'input',
                    mods: {size: 's', suggest: 'yes', 'tap-ahead': 'yes'},
                    placeholder: 'Tap-Ahead',
                    js: {
                        dataprovider: {
                            name: 'mock-data'
                        }
                    },
                    content: {elem: 'control', attrs: {name: 'autocomplete-tpah'}}
                },
                {tag: 'br'},
                {tag: 'br'},
                {
                    block: 'input',
                    mods: {size: 'm', suggest: 'yes', 'tap-ahead': 'yes'},
                    placeholder: 'Tap-Ahead',
                    js: {
                        dataprovider: {
                            name: 'mock-data'
                        }
                    },
                    content: {elem: 'control', attrs: {name: 'autocomplete-tpah'}}
                }
            ]
        },
        {tag: 'br'},
        {tag: 'br'},

        {
            block: 'gemini-common',
            mix: {block: 'gemini-textarea'},
            content: [
                {
                    block: 'input',
                    mods: {type: 'textarea', size: 's'},
                    content: {
                        elem: 'control',
                        attrs: {rows: '3'}
                    },
                    value: 'Я текстовая область размера S с переполненным контентом чтобы проверить ' +
                    'правильность применения padding в современных браузерах и border в IE'
                },
                {tag: 'br'},
                {tag: 'br'},
                {
                    block: 'input',
                    mods: {type: 'textarea', size: 'm'},
                    content: {
                        elem: 'control',
                        attrs: {rows: '3'}
                    },
                    value: 'Я текстовая область размера M с переполненным контентом чтобы проверить ' +
                    'правильность применения padding в современных браузерах и border в IE'
                },
                {tag: 'br'},
                {tag: 'br'},
                {
                    block: 'input',
                    mods: {type: 'textarea', disabled: 'yes', size: 's'},
                    content: {
                        elem: 'control',
                        attrs: {rows: '3'}
                    },
                    value: 'Я недоступная текстовая область размера S'
                }
            ]
        },
        {tag: 'br'},
        {tag: 'br'},

        {
            block: 'gemini-common',
            mix: {block: 'gemini-with-icon'},
            content: ['xs', 's', 'm'].map(function(size) {
                return [
                    {
                        block: 'icon-lock-' + size,
                        attrs: {style: 'display: inline-block; width: 130px; padding: 5px;'},
                        content: {
                            block: 'input',
                            mods: {size: size},
                            placeholder: 'Слева от текста',
                            iconLeft: {mods: {type: 'lock'}},
                            content: {elem: 'control'}
                        }
                    },
                    {
                        block: 'icon-eye-' + size,
                        attrs: {style: 'display: inline-block; width: 130px; padding: 5px;'},
                        content: {
                            block: 'input',
                            mods: {size: size},
                            placeholder: 'Справа от текста',
                            iconRight: {mods: {type: 'eye'}},
                            content: {elem: 'control'}
                        }
                    },
                    {
                        block: 'icon-double-' + size,
                        attrs: {style: 'display: inline-block; width: 130px; padding: 5px;'},
                        content: {
                            block: 'input',
                            mods: {size: size},
                            placeholder: 'С двух сторон',
                            iconLeft: {mods: {type: 'lock'}},
                            iconRight: {mods: {type: 'eye'}},
                            content: {elem: 'control'}
                        }
                    }
                ];
            })
        },
        {tag: 'br'},
        {tag: 'br'},

        {
            block: 'gemini-common',
            mix: {block: 'gemini-pin'},
            content: [
                    ['round-round'],
                    ['round-clear', 'no', 'clear-brick', 'clear-round'],
                    ['round-brick', 'clear-clear', 'brick-clear', 'brick-round']
                ].map(function(row) {
                    return {
                        elem: 'row',
                        content: row.map(function(pin) {
                            return {
                                block: 'input',
                                mods: {size: 'xs', clear: 'no', pin: pin},
                                placeholder: pin === 'no' ? '' : pin.replace(/[ouaei]/g, ''),
                                content: [
                                    {elem: 'control'}
                                ]
                            };
                        })
                    };
                })
        },
        {tag: 'br'},
        {tag: 'br'},

        {
            block: 'gemini-common',
            mix: {block: 'gemini-bg-color'},
            attrs: {style: 'background: #f5f5ea; display: inline-block;'},
            content: [
                {
                    content: [
                        {
                            block: 'input',
                            mods: {size: 'm'},
                            value: 'input на цветном фоне',
                            content: {elem: 'control'}
                        }
                    ]
                },
                {tag: 'br'},
                {tag: 'br'},
                {
                    content: {
                        block: 'input',
                        mods: {type: 'textarea', size: 'm'},
                        value: 'textarea на цветном фоне',
                        content: {elem: 'control'}
                    }
                }
            ]
        },

        {block: 'x-deps', content: {block: 'mock-data'}},

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
