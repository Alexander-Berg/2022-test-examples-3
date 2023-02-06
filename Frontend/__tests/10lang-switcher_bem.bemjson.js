({
    block: 'b-page',
    title: 'b-lang-switcher',
    head: [
        {elem: 'css', url: '_10lang-switcher_bem.css', ie: false},
        {elem: 'css', url: '_10lang-switcher_bem', ie: true}
    ],
    content: [
        {
            block: 'i-lego-example',
            attrs: {style: 'padding: 140px 40px 10px 40px;'},
            content: [
                {
                    block: 'b-lang-switcher',
                    lang: {code: 'ru', name: 'Ru'},
                    content: [
                        {
                            elem: 'lang',
                            lang: {code: 'ua', name: 'Ua'},
                            url: '#/bla'
                        },
                        {
                            elem: 'lang',
                            lang: {code: 'en', name: 'En'},
                            url: '#/bla'
                        },
                        {
                            elem: 'lang',
                            lang: {code: 'kz', name: 'Kz'},
                            url: '#/bla'
                        },
                        {
                            elem: 'lang',
                            lang: {code: 'by', name: 'By'},
                            url: '#/bla'
                        },
                        {
                            elem: 'lang',
                            selected: 'yes',
                            lang: {code: 'ru', name: 'Ru'},
                            url: '#/bla'
                        }
                    ]
                },
                ' — ',
                {
                    block: 'b-lang-switcher',
                    lang: {code: 'ru', name: 'Ru'},
                    content: [
                        {
                            elem: 'lang',
                            lang: {code: 'ua', name: 'Ua'},
                            url: '#/bla'
                        },
                        {
                            elem: 'lang',
                            lang: {code: 'en', name: 'En'},
                            url: '#/bla'
                        },
                        {
                            elem: 'lang',
                            lang: {code: 'ru', name: 'Ru'},
                            selected: 'yes',
                            url: '#/bla'
                        }
                    ]
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
            condition: '!IE 8',
            content: {block: 'i-jquery', mods: {version: 'default'}}
        },
        {elem: 'js', url: '_10lang-switcher_bem.js'}
    ]
});
