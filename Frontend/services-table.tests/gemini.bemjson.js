({
    block: 'b-page',
    attrs: {style: 'padding: 20px;'},
    title: 'services-table',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        ['ru', 'com', 'turkish', 'ad'].map(function(type) {
            return {
                block: 'gemini',
                attrs: {style: 'margin-bottom: 50px;'},
                content: {
                    block: 'services-table',
                    mods: {type: type}
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
        {elem: 'js', url: '_gemini.js'}
    ]
});
