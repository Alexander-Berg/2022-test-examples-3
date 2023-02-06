({
    block: 'b-page',
    title: 'Gemini Tests',
    head: [
        {elem: 'css', url: '_small.css', ie: false},
        {elem: 'css', url: '_small', ie: true}
    ],
    mods: {theme: 'normal', margin: 'no'},
    content: [
        [{
            name: 'popup',
            mods: {autoclosable: 'yes', animate: 'no', adaptive: 'no'},
            js: {directions: 'bottom-left'}
        }, {
            name: 'popup2',
            mods: {target: 'anchor', theme: 'clear', autoclosable: 'yes'},
            directions: ['bottom-left']
        }].map(function(options) {
            return {
                block: 'gemini-simple',
                mix: [{
                    block: 'example',
                    mods: {
                        'for': options.name
                    }
                }],
                attrs: {
                    style: 'padding: 20px; height: 160px; width: 502px; outline: 0;'
                },
                content: {
                    block: 'form',
                    tag: 'form',
                    mix: [{
                        block: 'suggest2-form',
                        js: {
                            popupName: options.name
                        }
                    }],
                    content: {
                        block: 'suggest2-form',
                        elem: 'node',
                        content: [
                            {
                                block: 'input',
                                mods: {size: 's', theme: 'normal'},
                                attrs: {style: 'width: 400px'},
                                mix: {block: 'suggest2-form', elem: 'input'},
                                content: {
                                    elem: 'control',
                                    attrs: {
                                        value: 'тест тест тест'
                                    }
                                }
                            },
                            {
                                block: 'button2',
                                mods: {size: 's', theme: 'normal', pin: 'clear-round'},
                                type: 'submit',
                                mix: {block: 'suggest2-form', elem: 'button'},
                                text: 'Найти'
                            },
                            {
                                block: options.name,
                                mods: options.mods,
                                js: options.js,
                                directions: options.directions,
                                mix: [{
                                    block: 'suggest2',
                                    mods: {
                                        type: 'all',
                                        theme: 'normal',
                                        size: 's'
                                    },
                                    js: {
                                        url: '//example.com'
                                    }
                                }, {
                                    block: 'suggest2-detect', js: true
                                }],
                                content: {elem: 'content'}
                            }
                        ]
                    }
                }
            };
        }),
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
        {elem: 'js', url: '_small.js'}
    ]
});
