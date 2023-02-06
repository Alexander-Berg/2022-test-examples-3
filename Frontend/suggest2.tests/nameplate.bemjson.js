({
    block: 'b-page',
    title: 'Gemini Tests',
    head: [
        {elem: 'css', url: '_nameplate.css', ie: false},
        {elem: 'css', url: '_nameplate', ie: true}
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
                attrs: {style: 'height: 235px; outline: 0;'},
                content: {
                    block: 'header2',
                    left: {
                        block: 'search2',
                        mods: {
                            template: 'service',
                            layout: 'simple'
                        },
                        label: 'Поиск по сервису',
                        mix: [
                            {
                                block: 'suggest2-form',
                                js: {
                                    popupName: options.name
                                }
                            },
                            {block: 'suggest2-form', elem: 'node'}
                        ],
                        content: [{
                            block: 'nameplate',
                            mix: {block: 'header2', elem: 'nameplate'},
                            service: 'market'
                        },
                            {
                                block: 'input',
                                mods: {size: 'm', type: 'normal', focus: 'thin', 'after-nameplate': 'yes'},
                                mix: {block: 'suggest2-form', elem: 'input'},
                                attrs: {style: 'max-width: 300px;'},
                                content: {
                                    elem: 'control',
                                    attrs: {
                                        name: 'text',
                                        autocomplete: 'off',
                                        maxlength: 400,
                                        value: 'тест тест тест'
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
                                block: options.name,
                                mods: options.mods,
                                js: options.js,
                                directions: options.directions,
                                mix: [{
                                    block: 'suggest2',
                                    mods: {
                                        type: 'all',
                                        theme: 'large',
                                        adaptive: 'yes',
                                        nameplate: 'yes'
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
        {elem: 'js', url: '_nameplate.js'}
    ]
});
