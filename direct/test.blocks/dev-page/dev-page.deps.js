({
    mustDeps: [
        { block: 'dev-jquery' },
        { block: 'dev-chai-matches' },
        { block: 'i-bem', elems: ['dom', 'elem', 'interface', 'i18n'] },
        { block: 'i-model' },
        { block: 'i-utils', elems: ['test', 'iget-stub', 'consts-stub'] },
        { block: 'i-polyfill', elem: 'promise' }
    ]
})
