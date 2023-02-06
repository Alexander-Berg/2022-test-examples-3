({
    block: 'b-page',
    title: 'user',
    head: [
        {elem: 'css', url: '_gemini-multiauth.css', ie: false},
        {elem: 'css', url: '_gemini-multiauth', ie: true}
    ],
    content: [
        {
            block: 'gemini',
            content: [
                {
                    block: 'gemini-multiauth-one-user',
                    content: [
                        {
                            block: 'i-global',
                            params: {login: 'constantine'}
                        },
                        {
                            block: 'user',
                            mods: {menu: 'multiauth'},
                            js: {mail: 101, uid: '12345678', dataProvider: 'user_menu_multiline__provider_one_user'},
                            content: [
                                {elem: 'icon'},
                                {elem: 'name'}
                            ]
                        }
                    ]
                },
                {
                    block: 'gemini-multiauth-many-users',
                    content: [
                        {
                            block: 'i-global',
                            params: {login: 'constantine'}
                        },
                        {
                            block: 'user',
                            mods: {menu: 'multiauth'},
                            js: {mail: 101, uid: '12345678', dataProvider: 'user_menu_multiline__provider_many_users'},
                            content: [
                                {elem: 'icon'},
                                {elem: 'name'}
                            ]
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
        {elem: 'js', url: '_gemini-multiauth.js'}
    ]
});
