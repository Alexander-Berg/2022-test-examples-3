({
    block: 'b-page',
    title: 'auth',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'gemini-simple',
            attrs: {style: 'width: 300px; padding: 40px;'},
            content: {
                block: 'auth',
                mods: {content: 'auto'}
            }
        },
        {
            block: 'gemini-missing',
            attrs: {style: 'width: 300px; padding: 40px;'},
            content: {
                block: 'auth',
                mods: {content: 'auto'},
                content: [
                    {elem: 'username'},
                    {elem: 'password'},
                    {elem: 'button'}
                ]
            }
        },
        {
            block: 'gemini-layout',
            attrs: {style: 'width: 300px; padding: 40px;'},
            content: {
                block: 'auth',
                mods: {content: 'auto'},
                content: [
                    {
                        elem: 'right',
                        content: [
                            {
                                block: 'link',
                                mods: {theme: 'normal'},
                                url: '/',
                                text: 'Вспомнить пароль'
                            },
                            '\u00a0\u00a0',
                            {
                                block: 'link',
                                mods: {theme: 'strong'},
                                url: '/',
                                text: 'Зарегистрироваться'
                            }
                        ]
                    },
                    {elem: 'username'},
                    {elem: 'password'},
                    {
                        elem: 'row',
                        elemMods: {
                            button: 'yes'
                        },
                        content: [
                            {elem: 'haunter'},
                            {elem: 'button'}
                        ]
                    }
                ]
            }
        },
        {
            block: 'gemini-simple-error',
            attrs: {style: 'width: 500px; padding: 40px;'},
            content: {
                block: 'gemini-wrap',
                attrs: {style: 'width: 300px'},
                content: {
                    block: 'auth',
                    mods: {content: 'auto'}
                }
            }
        },
        {
            block: 'gemini-click-area',
            content: '\u00a0'
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
