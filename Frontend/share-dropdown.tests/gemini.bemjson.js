({
    block: 'x-page',
    title: 'Поделиться',
    content: [
        {
            block: 'i-global',
            params: {
                url: '//hahahahhahahahha.com'
            }
        },
        {
            block: 'b-page',
            title: 'Поделиться',
            head: [
                {elem: 'css', url: '_gemini.css', ie: false},
                {elem: 'css', url: '_gemini', ie: true}
            ],
            attrs: {style: 'padding: 20px;'},
            content: [
                {
                    block: 'example',
                    attrs: {style: 'padding: 15px; height: 350px;'},
                    content: [
                        {
                            block: 'share-dropdown',
                            js: {
                                popupParams: {
                                    shareDropdown: 'm'
                                }
                            },
                            share: {
                                title: 'Это title ст #разницы',
                                description: 'Это описание страницы (description)',
                                image: 'https://yastatic.net/lego/_/X31pO5JJJKEifJ7sfvuf3mGeD_8.png'
                            },
                            content: [
                                {
                                    elem: 'share-item',
                                    service: 'facebook'
                                },
                                {
                                    elem: 'share-item',
                                    service: 'pinterest'
                                },
                                {
                                    elem: 'share-item',
                                    service: 'twitter'
                                },
                                {
                                    elem: 'share-item',
                                    service: 'vkontakte'
                                },
                                {
                                    elem: 'share-item',
                                    service: 'odnoklassniki'
                                },
                                {
                                    elem: 'share-item',
                                    service: 'gplus'
                                },
                                {
                                    elem: 'share-item',
                                    service: 'moimir'
                                }
                            ]
                        },
                        {
                            block: 'share-dropdown',
                            buttonMods: {theme: 'pseudo', size: 's'},
                            js: {
                                popupParams: {
                                    shareDropdown: 's'
                                }
                            },
                            share: {
                                title: 'Это title ст #разницы',
                                description: 'Это описание страницы (description)',
                                image: 'https://yastatic.net/lego/_/X31pO5JJJKEifJ7sfvuf3mGeD_8.png'
                            },
                            content: [
                                {
                                    elem: 'share-item',
                                    service: 'facebook'
                                },
                                {
                                    elem: 'share-item',
                                    service: 'pinterest'
                                },
                                {
                                    elem: 'share-item',
                                    service: 'twitter'
                                },
                                {
                                    elem: 'share-item',
                                    service: 'vkontakte'
                                },
                                {
                                    elem: 'share-item',
                                    service: 'odnoklassniki'
                                },
                                {
                                    elem: 'share-item',
                                    service: 'gplus'
                                },
                                {
                                    elem: 'share-item',
                                    service: 'moimir'
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
                    condition: 'gt IE 8',
                    others: true,
                    content: {block: 'i-jquery', mods: {version: 'default'}}
                },
                {block: 'i-jquery', mods: {version: 'default'}},
                {elem: 'js', url: '_gemini.js'}
            ]
        }
    ]
});
