({
    block: 'x-page',
    mods: {theme: 'normal', margin: 'no'},
    content: [
        {
            block: 'gemini-simple',
            mix: [{
                block: 'example',
                mods: {
                    theme: 'large'
                }
            }],
            attrs: {style: 'height: 455px; outline: 0;'},
            content: {
                block: 'header2',
                mods: {
                    'show-websearch': 'yes',
                    layout: 'simple'
                },
                logo: {
                    elem: 'logo',
                    url: 'https://yandex.ru'
                },
                left: {
                    block: 'search2', mods: {template: 'websearch'},
                    mix: [
                        {block: 'suggest2-form', js: true},
                        {block: 'suggest2-form', elem: 'node'}
                    ],
                    input: [{
                        block: 'input',
                        mods: {size: 'ws-head', theme: 'websearch'},
                        mix: [{block: 'suggest2-form', elem: 'input'}],
                        content: {
                            elem: 'control',
                            attrs: {
                                name: 'text',
                                autocomplete: 'off',
                                maxlength: 400
                            }
                        }
                    }, {
                        block: 'popup',
                        mods: {autoclosable: 'yes', adaptive: 'no', animate: 'no'},
                        js: {directions: 'bottom-left'},
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
        }
    ]
});
