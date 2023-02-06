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
            { block: 'dev-jquery' },
            { block: 'i-bem', elems: ['html', 'dom', 'i18n'] },
            { block: 'i-bem', elem: 'dom', mods: { init: 'auto' } },
            { block: 'i-global', mods: { type: 'simple' } },
            { block: 'i-const' },
            { block: 'i-model' },
            { block: 'i-utils', elems: ['test', 'i18n'] }
        ]
    }
])
