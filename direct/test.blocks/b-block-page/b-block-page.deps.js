([
    {
        tech: 'js',
        mustDeps: [
            {
                block: 'i-bem',
                elem: 'html',
                tech: 'bemhtml'
            }
        ]
    },
    {
        mustDeps: [
            { block: 'i-bem', elems: ['html'] },
            { block: 'i-bem', elem: 'dom', mods: { init: 'auto' } },
            { block: 'i-global', mods: { type: 'simple' } },
            { block: 'i-test-stubs' },
            { block: 'i-polyfill', elems:['promise'] }
        ]
    }
])
