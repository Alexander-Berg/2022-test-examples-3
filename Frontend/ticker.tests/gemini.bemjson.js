({
    block: 'b-page',
    title: 'ticker',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'gemini',
            mix: {block: 'gemini-abstract'},
            attrs: {style: 'padding: 20px;'},
            content: {
                block: 'ticker',
                url: '#',
                count: 7
            }
        },
        {
            block: 'gemini',
            mix: {block: 'gemini-more'},
            attrs: {style: 'padding: 20px;'},
            content: {
                block: 'ticker',
                url: '#',
                count: 9999
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
        {elem: 'js', url: '_gemini.js'}
    ]
});
