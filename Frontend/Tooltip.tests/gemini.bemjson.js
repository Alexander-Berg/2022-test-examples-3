({
    block: 'b-page',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        ['success', 'normal', 'error', 'white'].map(function(theme) {
            return {
                block: 'example-group',
                attrs: {style: 'display: inline-block;'},
                content: ['xs', 's', 'm', 'l'].map(function(size) {
                    return {
                        block: 'example',
                        mods: {
                            size: size,
                            theme: theme,

                            // Для использования в gemini-тестах.
                            // Такой же модификатор прокидывается в tooltip.
                            testcase: [theme, size].join('-')
                        }
                    };
                })
            };
        }),
        {
            block: 'example-group',
            attrs: {style: 'display: inline-block;'},
            content: {
                block: 'example',
                mods: {tail: 'without'}
            }
        },
        {
            block: 'example-group',
            content: ['xs', 's', 'm', 'l'].map(function(size) {
                return {
                    block: 'example',
                    mods: {
                        size: size,
                        theme: 'normal',
                        testcase: 'normal-' + size + '-multiline',
                        multiline: true
                    }
                };
            })
        },
        {
            block: 'example-group',
            content: ['xs', 's', 'm', 'l', 'n'].map(function(size, i) {
                var tones = ['default', 'red', 'dark', 'grey'];

                return {
                    block: 'example',
                    mods: {
                        view: 'default',
                        size: size,
                        tone: tones[i] || 'dark',
                        theme: 'normal',
                        testcase: 'default-' + size
                    }
                };
            })
        },
        {
            block: 'example-group',
            content: {
                    block: 'example',
                    mods: {
                        size: 'l',
                        theme: 'normal',
                        testcase: 'content'
                    }
                }
        },
        {
            block: 'example-group',
            content: {
                block: 'example',
                mods: {
                    size: 's',
                    theme: 'promo',
                    tone: 'default',
                    testcase: 'promo-s-default'
                }
            }
        },
        {
            block: 'example-group',
            content: {
                block: 'example',
                mods: {
                    size: 's',
                    theme: 'promo',
                    tone: 'dark',
                    testcase: 'promo-s-dark'
                }
            }
        },
        {
            block: 'example-group',
            content: {
                block: 'example',
                mods: {
                    size: 'm',
                    theme: 'promo',
                    tone: 'default',
                    testcase: 'promo-m-default'
                }
            }
        },
        {
            block: 'example-group',
            content: {
                block: 'example',
                mods: {
                    size: 'm',
                    theme: 'promo',
                    tone: 'dark',
                    testcase: 'promo-m-dark'
                }
            }
        },
        {content: ' '}, /* Для Opera 12 */
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
