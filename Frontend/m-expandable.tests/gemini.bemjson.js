([{
    block: 'b-page',
    title: 'm-expandable',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'm-expandable',
            attrs: {style: 'display:table-cell'},
            js: true,
            content: [
                {
                    block: 'm-expandable',
                    elem: 'trigger',
                    content: 'Toggle'
                },
                {
                    block: 'm-expandable',
                    elem: 'cropper',
                    content: 'Content'
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
