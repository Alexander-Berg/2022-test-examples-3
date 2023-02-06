({
    shouldDeps: [
        {
            block: 'suggest2-item',
            mods: {
                type: [
                    'fact',
                    'nav',
                    'traffic',
                    'weather',
                    'icon',
                    'html',
                    'bemjson'
                ]
            },
            elems: {elem: 'icon', mods: {size: 'm'}}
        },
        {
            block: 'suggest2-item',
            elem: 'text',
            elemMods: {type: ['green-url', 'title-url']}
        },
        {
            block: 'suggest2-popup',
            mods: {
                'for': 'popup'
            }
        }
    ]
});
