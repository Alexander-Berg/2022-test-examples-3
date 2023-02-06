[{
    mustDeps: [
        {block: 'i-bem', elem: ['dom']}
    ],
    shouldDeps: [
        {block: 'suggest'},
        {
            block: 'suggest-item',
            mods: {
                type: [
                    'fact',
                    'text',
                    'icon',
                    'nav',
                    'text',
                    'traffic',
                    'weather'
                ]
            }
        }
    ]
}, {
    tech: 'js',
    shouldDeps: [
        {
            tech: 'bemhtml',
            block: 'suggest-item',
            mods: {
                type: [
                    'fact',
                    'text',
                    'icon',
                    'nav',
                    'text',
                    'traffic',
                    'weather'
                ]
            }
        }
    ]
}];
