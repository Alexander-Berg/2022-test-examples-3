([{
    shouldDeps: [
        {
            block: 'suggest2-popup',
            mods: {
                'for': ['popup', 'popup2']
            }
        },
        {
            block: 'button2',
            mods: {theme: 'normal', size: 'm', pin: 'clear-round'}
        }
    ]
}, {
    mustDeps: [
        {block: 'i-services', elems: ['uri']}
    ]
}]);
