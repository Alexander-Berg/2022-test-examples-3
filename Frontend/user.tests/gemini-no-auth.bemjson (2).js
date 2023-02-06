({
    block: 'b-page',
    head: [
        {elem: 'css', url: '_gemini-no-auth.css', ie: false}
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
            condition: 'gt IE 8',
            others: true,
            content: {block: 'i-jquery', mods: {version: 'default'}}
        },
        {elem: 'js', url: '_gemini-no-auth.js'}
    ]
});
