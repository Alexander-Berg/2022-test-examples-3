({
    block: 'b-page',
    title: 'b-link',
    head: [
        {elem: 'css', url: '_gemini.css'}
    ],
    content: [
        {
            block: 'test',
            cls: 'gemini-link',
            content: {
                block: 'b-link',
                url: '#',
                content: 'Link'
            }
        },
        '&nbsp',
        {
            block: 'test',
            cls: 'gemini-link-pseudo',
            content: {
                block: 'b-link',
                mods: {pseudo: 'yes'},
                content: 'Pseudo link'
            }
        },
        {
            elem: 'cc',
            condition: 'IE 8',
            content: {elem: 'js', url: 'https://yastatic.net/es5-shims/0.0.1/es5-shims.min.js'}
        },
        {attrs: {style: 'height: 10px'}} // Распорка для оперы
    ]
});
