({
    block: 'b-page',
    title: 'input',
    head: [
        {elem: 'css', url: '_touchsearch.css', ie: false},
        {elem: 'css', url: '_touchsearch', ie: true}
    ],
    content: [
        {
            block: 'gemini-touchsearch',
            attrs: {style: 'padding: 20px;'},
            content: [
                {
                    block: 'input',
                    mods: {size: 's', theme: 'touchsearch'},
                    value: 'Touchsearch',
                    content: [
                        {elem: 'control'}
                    ]
                }
            ]
        },

        {
            elem: 'cc',
            condition: 'gt IE 8',
            others: true,
            content: {block: 'i-jquery', mods: {version: 'default'}}
        },
        {elem: 'js', url: '_touchsearch.js'}
    ]
});
