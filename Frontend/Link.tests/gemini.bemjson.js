({
    block: 'b-page',
    title: 'Link',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        (function() {
            return {
                block: 'gemini',
                id: 'static',
                content: getCombinations({
                    theme: ['normal', 'black', 'ghost', 'outer', 'strong'],
                    hovered: ['', 'yes'],
                    focused: ['', 'yes'],
                    disabled: ['']
                }, {
                    theme: ['normal', 'black', 'ghost', 'outer', 'strong'],
                    hovered: [''],
                    focused: [''],
                    disabled: ['yes']
                },
                {
                    theme: ['pseudo'],
                    pseudo: ['yes'],
                    hovered: ['', 'yes'],
                    focused: ['', 'yes'],
                    disabled: ['']
                }, {
                    theme: ['pseudo'],
                    pseudo: ['yes'],
                    hovered: [''],
                    focused: [''],
                    disabled: ['yes']
                }).map(function(props) {
                    return {
                        elem: 'item',
                        content: {
                            block: 'link',
                            mods: props,
                            url: 'https://yandex.ru',
                            text: 'link'
                        }
                    };
                })
            };

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
            id: 'dynamic',
            content: [
                {
                    block: 'link',
                    mods: {
                        theme: 'normal'
                    },
                    url: 'http://ya.ru',
                    text: 'Просто-ссылка'
                },
                {
                    block: 'link',
                    mods: {
                        theme: 'normal',
                        disabled: 'yes'
                    },
                    url: 'http://ya.ru',
                    text: 'Просто-ссылка'
                },
                {
                    block: 'link',
                    mods: {
                        theme: 'pseudo',
                        pseudo: 'yes'
                    },
                    url: 'http://ya.ru',
                    text: 'Псевдо-ссылка <a>'
                },
                {
                    block: 'link',
                    mods: {
                        theme: 'pseudo',
                        pseudo: 'yes',
                        disabled: 'yes'
                    },
                    url: 'http://ya.ru',
                    text: 'Псевдо-ссылка <a>'
                },
                {
                    block: 'link',
                    mods: {
                        theme: 'pseudo',
                        pseudo: 'yes'
                    },
                    text: 'Псевдо-ссылка <span>'
                },
                {
                    block: 'link',
                    mods: {
                        theme: 'pseudo',
                        pseudo: 'yes',
                        disabled: 'yes'
                    },
                    text: 'Псевдо-ссылка <span>'
                },
                {
                    block: 'link',
                    mods: {
                        theme: 'normal',
                        inner: 'yes'
                    },
                    attrs: {
                        style: 'font-size: 16px; margin-bottom: 10px;'
                    },
                    url: 'https://yandex.ru',
                    text: 'Просто-ссылка с иконкой',
                    icon: {
                        block: 'icon',
                        mods: {type: 'load'},
                        mix: [{block: 'link', elem: 'icon'}],
                        alt: '16x16 icon'
                    }
                },
                {
                    block: 'link',
                    mods: {
                        theme: 'normal',
                        inner: 'yes',
                        disabled: 'yes'
                    },
                    attrs: {
                        style: 'font-size: 16px; margin-bottom: 10px;'
                    },
                    url: 'https://yandex.ru',
                    text: 'Просто-ссылка с иконкой',
                    icon: {
                        block: 'icon',
                        mods: {type: 'load'},
                        mix: [{block: 'link', elem: 'icon'}],
                        alt: '16x16 icon'
                    }
                },
                {
                    block: 'link',
                    mods: {
                        theme: 'pseudo',
                        pseudo: 'yes',
                        inner: 'yes'
                    },
                    attrs: {
                        style: 'font-size: 16px; margin-bottom: 10px;'
                    },
                    url: 'https://yandex.ru',
                    text: 'Псевдо-ссылка с иконкой',
                    icon: {
                        block: 'icon',
                        mods: {type: 'load'},
                        mix: [{block: 'link', elem: 'icon'}],
                        alt: '16x16 icon'
                    }
                },
                {
                    block: 'link',
                    mods: {
                        theme: 'pseudo',
                        pseudo: 'yes',
                        inner: 'yes',
                        disabled: 'yes'
                    },
                    attrs: {
                        style: 'font-size: 16px; margin-bottom: 10px;'
                    },
                    url: 'https://yandex.ru',
                    text: 'Псевдо-ссылка с иконкой',
                    icon: {
                        block: 'icon',
                        mods: {type: 'load'},
                        mix: [{block: 'link', elem: 'icon'}],
                        alt: '16x16 icon'
                    }
                }
            ].map(function(link, i) {
                return [
                    {
                        elem: 'item',
                        attrs: {
                            id: 'case_' + i
                        },
                        content: link
                    },
                    {tag: 'br'}
                ];
            })
        },

        {
            block: 'gemini',
            id: 'disabled',
            content: ['outer', 'ghost', 'strong', 'normal', 'pseudo', 'black'].map(function(theme, i) {
                return [
                    {
                        elem: 'item',
                        attrs: {
                            id: 'disabled_case_' + i
                        },
                        content: {
                            block: 'link',
                            mods: {
                                theme: theme,
                                hovered: 'yes',
                                pseudo: 'yes',
                                inner: 'yes',
                                disabled: 'yes'
                            },
                            attrs: {
                                style: 'font-size: 16px; margin-bottom: 10px;'
                            },
                            url: 'https://yandex.ru',
                            text: theme + '-ссылка с иконкой',
                            icon: {
                                block: 'icon',
                                mods: {type: 'load'},
                                mix: [{block: 'link', elem: 'icon'}],
                                alt: '16x16 icon'
                            }
                        }
                    },
                    {tag: 'br'}
                ];
            })
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
