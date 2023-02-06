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
            { block: 'i-bem', elems: ['html', 'dom', 'i18n'] },
            { block: 'i-bem', elem: 'dom', mods: { init: 'auto' } },
            { block: 'common-i18n' },
            { block: 'i-global', mods: { type: 'simple' } },
            { block: 'i-test-stubs' },
            { block: 'i-const' },
            { block: 'i-model' },
            { block: 'i-utils', elems: ['test', 'i18n'] },
            { block: 'i-test-stubs' },
            { block: 'b-chai', mod: 'custom', val: 'matchers' }
        ]
    }
])
