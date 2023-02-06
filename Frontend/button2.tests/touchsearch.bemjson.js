({
    block: 'b-page',
    head: [
        {elem: 'css', url: '_touchsearch.css', ie: false}
    ],
    content: [
        // Размеры.
        {
            block: 'gemini',
            id: 'touchsearch',
            content: {
                elem: 'item',
                content: {
                    block: 'button2',
                    mods: {theme: 'touchsearch', size: 'head'},
                    text: 'button'
                }
            }
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
