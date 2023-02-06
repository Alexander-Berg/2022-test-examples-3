({
    block: 'b-page',
    title: 'Gemini Tests',
    head: [
        {elem: 'css', url: '_large.css', ie: false},
        {elem: 'css', url: '_large', ie: true}
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
                attrs: {style: 'height: 535px; outline: 0;'},
                content: {
                    block: 'header2',
                    mods: {
                        'show-websearch': 'yes',
                        border: 'transparent',
                        layout: 'simple'
                    },
                    logo: {
                        elem: 'logo',
                        url: 'https://yandex.ru'
                    },
                    left: {
                        block: 'search2', mods: {template: 'websearch'},
                        mix: [
                            {
                                block: 'suggest2-form',
                                js: {
                                    popupName: options.name
                                }
                            },
                            {block: 'suggest2-form', elem: 'node'}
                        ],
                        input: [{
                            block: 'input',
                            mods: {size: 'ws-head', theme: 'websearch'},
                            mix: {block: 'suggest2-form', elem: 'input'},
                            content: {
                                elem: 'control',
                                attrs: {
                                    name: 'text',
                                    autocomplete: 'off',
                                    maxlength: 400,
                                    value: 'длинная персональная подсказка простая фиолетовая и эллипсисом в конце'
                                }
                            }
                        }, {
                            block: options.name,
                            mods: options.mods,
                            js: options.js,
                            directions: options.directions,
                            mix: [{
                                block: 'suggest2',
                                mods: {
                                    type: 'all',
                                    theme: 'large',
                                    adaptive: 'yes'
                                },
                                js: {
                                    url: '//example.com'
                                }
                            }, {
                                block: 'suggest2-detect', js: true
                            }],
                            content: {elem: 'content'}
                        }]
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
        {elem: 'js', url: '_large.js'}
    ]
});
