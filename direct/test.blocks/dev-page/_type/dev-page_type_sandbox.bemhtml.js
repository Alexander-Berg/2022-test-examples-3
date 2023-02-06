block('dev-page').mod('type', 'sandbox').elem('body')(
    def()(function() {
        return applyNext({
            'ctx.mix': [].concat(this.ctx.mix || []).concat({ block: 'i-ua', mods: { inlinesvg: 'yes' } })
        });
    })
);
