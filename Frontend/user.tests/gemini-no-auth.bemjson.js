({
    block: 'b-page',
    title: 'user',
    head: [
        {elem: 'css', url: '_gemini-no-auth.css', ie: false},
        {elem: 'css', url: '_gemini-no-auth', ie: true}
    ],
    content: [
        {
            block: 'gemini',
            content: [
                {
                    block: 'gemini-no-auth',
                    mix: {block: 'x-area'},
                    content: {
                        block: 'user',
                        content: [
                            {elem: 'enter'}
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
        {elem: 'js', url: '_gemini-no-auth.js'}
    ]
});
