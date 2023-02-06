({
    block: 'b-page',
    title: 'modal',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    zoom: true,
    content: [
        {
            block: 'test',
            attrs: {style: 'width: 200px;height: 200px;'},
            js: true,
            content: [
                {
                    elem: 'clicker',
                    content: 'open modal'
                },
                {
                    block: 'modal',
                    attrs: {style: 'width: 200px;height: 200px;'},
                    mods: {theme: 'normal'},
                    content: 'Модальное окно'
                }
            ]
        },

        {tag: 'div', attrs: {style: 'height: 10px;'}}, // Распорка для Оперы

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
