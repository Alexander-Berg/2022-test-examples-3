({
    block: 'b-page',
    title: 'Gemini Tests',
    head: [
        {elem: 'css', url: '_group.css', ie: false},
        {elem: 'css', url: '_group', ie: true}
    ],
    mods: {theme: 'normal', margin: 'no'},
    content: [
        {
            block: 'gemini-simple',
            mix: [{
                block: 'example',
                mods: {
                    theme: 'normal'
                }
            }],
            attrs: {style: 'padding: 20px; height: 425px; width: 520px; outline: 0;'},
            content: {
                block: 'form',
                tag: 'form',
                mix: [
                    {block: 'suggest2-form', js: true}
                ],
                content: {
                    block: 'suggest2-form',
                    elem: 'node',
                    content: [
                        {
                            block: 'input',
                            mods: {size: 'm', theme: 'normal'},
                            mix: {block: 'suggest2-form', elem: 'input'},
                            attrs: {style: 'width: 80%'},
                            content: {
                                elem: 'control',
                                attrs: {
                                    name: 'text',
                                    autocomplete: 'off',
                                    maxlength: 400,
                                    value: 'тест'
                                }
                            }
                        },
                        {
                            block: 'button2',
                            mods: {size: 'm', theme: 'normal', pin: 'clear-round'},
                            type: 'submit',
                            mix: {block: 'suggest2-form', elem: 'button'},
                            text: 'Найти'
                        },
                        {
                            block: 'popup',
                            mods: {autoclosable: 'yes', adaptive: 'no', animate: 'no'},
                            js: {directions: 'bottom-left'},
                            mix: [{
                                block: 'suggest2',
                                mods: {
                                    type: 'all',
                                    theme: 'normal',
                                    size: 'm',
                                    adaptive: 'yes',
                                    group: 'label'
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
        },
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
                left: [
                    {
                        block: 'search2', mods: {template: 'websearch'},
                        mix: [
                            {block: 'suggest2-form', js: true},
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
                                    value: 'тест'
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
                                    group: 'label',
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
        {elem: 'js', url: '_group.js'}
    ]
});
