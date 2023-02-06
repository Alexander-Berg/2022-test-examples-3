({
    block: 'b-page',
    head: [
        { elem: 'css', url: '_10-base_bem.css', ie: false },
        { elem: 'css', url: '_10-base_bem', ie: true }
    ],
    content: [
        {
            block: 'i-bem',
            elem: 'test',
            content: [
                { block: 'i-bem', elem: 'html' }
            ]
        },
        { block: 'i-jquery', mods: { version: '1.8.3' } },
        { elem: 'js', url: '_10-base_bem.js' }
    ]
})