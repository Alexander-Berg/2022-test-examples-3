var exts = ['none',
                    'zip', 'rar', 'tar', 'gz', '7z',
                    'flv',
                    'm4a', 'ogg',
                    'gif', 'jpg', 'png', 'eml',
                    'exe', 'mov', 'wmv', 'mp4', 'avi',
                    'xls', 'doc', 'txt', 'pdf', 'ppt', 'mp3', 'wav', 'wma'];
({
    block: 'b-page',
    title: 'attach',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'gemini',
            mix: {block: 'gemini-ext'},
            attrs: {style: 'padding: 10px;'},
            content: exts.map(function(ext) {
                return {
                    block: 'attach',
                    js: {ext: ext},
                    attrs: {style: 'margin-left: 5px;'},
                    mods: {size: 's', theme: 'normal'},
                    content: [
                        {
                            block: 'button',
                            mods: {size: 's', theme: 'normal'},
                            mix: {block: 'attach', elem: 'button'},
                            tabindex: 1,
                            id: 'bla',
                            content: {
                                block: 'i-bem', elem: 'i18n', keyset: 'attach', key: 'button-text'
                            }
                        },
                        {
                            elem: 'holder',
                            content: {
                                block: 'i-bem', elem: 'i18n', keyset: 'attach', key: 'no-file'
                            }
                        }
                    ]
                };
            })
        },


        {
            block: 'gemini',
            mix: {block: 'gemini-normal'},
            content: [
                {
                    attrs: {style: 'margin: 20px'},
                    content: [
                        'Какой-то текст   ',
                        {
                            block: 'attach',
                            attrs: {style: 'margin-left: 10px;'},
                            mods: {size: 's', theme: 'normal'},
                            tabindex: 1,
                            id: 'bla',
                            holder: true
                        }
                    ]
                },
                {
                    attrs: {style: 'margin: 20px'},
                    content: [
                        'Какой-то текст   ',
                        {
                            block: 'attach',
                            attrs: {style: 'margin-right: 1em;margin-left: 10px;'},
                            mods: {size: 'm', theme: 'normal'},
                            tabindex: 2,
                            name: 'customName',
                            id: 'bla-2',
                            holder: true
                        },
                        {
                            block: 'button2',
                            mods: {theme: 'normal', size: 'm'},
                            type: 'button',
                            text: 'Я.Submit'
                        }
                    ]
                }
            ]
        },

        {
            block: 'gemini',
            mix: {block: 'gemini-button-2'},
            content: [
                {
                    attrs: {style: 'margin: 20px'},
                    content: [
                        'Какой-то текст   ',
                        {
                            block: 'attach',
                            attrs: {style: 'margin-left: 10px;'},
                            mods: {size: 's', theme: 'normal', use: 'button2'},
                            tabindex: 1,
                            id: 'bla',
                            holder: true
                        }
                    ]
                }
            ]
        },

        {
            block: 'gemini',
            mix: {block: 'gemini-disabled'},
            content: [
                {
                    attrs: {style: 'margin: 20px'},
                    content: [
                        'Какой-то текст   ',
                        {
                            block: 'attach',
                            attrs: {style: 'margin-right: 1em;'},
                            mods: {size: 's', theme: 'normal', disabled: 'yes'},
                            content: [
                                {
                                    block: 'button',
                                    mods: {size: 's', theme: 'normal'},
                                    mix: [{block: 'attach', elem: 'button'}],
                                    tabindex: 1,
                                    name: 'customName',
                                    id: 'bla',
                                    content: {
                                        block: 'i-bem', elem: 'i18n', keyset: 'attach', key: 'button-text'
                                    }
                                },
                                {
                                    elem: 'holder',
                                    content: {
                                        block: 'i-bem', elem: 'i18n', keyset: 'attach', key: 'no-file'
                                    }
                                }
                            ]
                        }
                    ]
                }
            ]
        },

        {
            block: 'gemini',
            mix: {block: 'gemini-fixed'},
            content: [
                {
                    block: 'attach',
                    js: {textWidth: 30},
                    mods: {theme: 'normal', size: 's', use: 'button2'},
                    holder: true
                }
            ]
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
