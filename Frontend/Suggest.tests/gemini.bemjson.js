({
    block: 'b-page',
    title: 'Подсказка',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'gemini-suggest',
            content: {
                block: 'example',
                js: true,
                content: {
                    block: 'input',
                    mods: {size: 'm', theme: 'normal'},
                    content: {
                        elem: 'control',
                        attrs: {
                            name: 'text',
                            tabindex: 1,
                            autocomplete: 'off',
                            maxlength: 400
                        }
                    }
                }
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
