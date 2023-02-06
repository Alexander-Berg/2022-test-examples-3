({
    block: 'b-page',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [

        {
            block: 'native-scroll',
            attrs: {style: 'width: 100px; height: 30px;'},
            content: {
                tag: 'span',
                attrs: {style: 'white-space: nowrap; line-height: 30px;'},
                content: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit.'
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
