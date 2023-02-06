([{
    block: 'b-page',
    title: 'domik',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'gemini',
            js: true,
            attrs: {style: 'height: 1000px;'},
            content: [
                {
                    block: 'gemini-page',
                    attrs: {style: 'margin: 40px;'},
                    content: {
                        block: 'domik',
                        mods: {type: 'page'},
                        content: {
                            block: 'auth',
                            mods: {content: 'auto'}
                        }
                    }
                },
                {
                    block: 'gemini-page-y',
                    // Свойство display: inline-block ломает попапы в Opera12 в gemini-гриде
                    attrs: {style: 'margin: 40px; float:left;'},
                    content: {
                        block: 'domik',
                        mods: {type: 'page', logo: 'en'},
                        content: {
                            block: 'auth',
                            mods: {content: 'auto'}
                        }
                    }
                },
                {
                    block: 'gemini-modal',
                    attrs: {style: 'position: absolute; top:0; left: 500px'},
                    content: [
                        'modal ',
                        {
                            block: 'user',
                            content: {elem: 'enter'}
                        }
                    ]
                },
                {
                    block: 'gemini-modal-y',
                    attrs: {style: 'position: absolute;top:0; left: 650px'},
                    content: [
                        'modal-y ',
                        {
                            block: 'user',
                            js: {
                                domikMods: {logo: 'en'}
                            },
                            content: {elem: 'enter'}
                        }
                    ]
                },
                {
                    block: 'gemini-explain',
                    attrs: {style: 'margin: 30px; display: inline-block; padding: 10px;'},
                    content: {
                        block: 'domik',
                        mods: {type: 'page', logo: 'ru'},
                        content: [
                            {
                                elem: 'explain',
                                content: 'Пояснительный текст'
                            },
                            {
                                block: 'auth',
                                mods: {content: 'auto'}
                            }
                        ]
                    }

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
}]);
