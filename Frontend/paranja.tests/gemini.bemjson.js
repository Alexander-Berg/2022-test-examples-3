({
    block: 'b-page',
    title: 'paranja',
    attrs: {style: 'padding: 100px;'},
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'gemini',
            content: [
                {
                    block: 'gemini-background',
                    tag: 'span',
                    attrs: {style: 'padding: 50px;'},
                    content: 'Показать паранджу'
                },
                {
                    block: 'paranja',
                    js: {rel: [{elem: '.gemini-background', event: 'click', method: 'open'}]}
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
