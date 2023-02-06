specs({
    feature: 'source',
}, () => {
    hermione.only.notIn('safari13');
    it('Базовый вид блока', function() {
        return this.browser
            .url('/turbo?stub=source/default.json')
            .yaWaitForVisible(PO.source())
            .assertView('default', PO.source())
            .yaCheckBaobabServerCounter({
                path: '$page.$main.$result.source.button',
                attrs: { target: 'source', from: 'source' },
            });
    });

    hermione.only.notIn('safari13');
    it('Локализация', function() {
        return this.browser
            .url('/turbo?stub=source/default.json&l10n=tr')
            .yaWaitForVisible(PO.source())
            .assertView('localization', PO.source())
            .getAttribute(PO.blocks.button(), 'target')
            .then(target => {
                assert.equal(target, '_blank', 'Источник должен открываться в новой вкладке');
            })
            .getAttribute(PO.blocks.button(), 'rel')
            .then(target => {
                assert.equal(target, 'noopener', 'Для всех внешних ссылок должен быть проставлен аттрибут noopener');
            });
    });

    hermione.only.notIn('safari13');
    it('Паникод', function() {
        return this.browser
            .url('/turbo?stub=source/punycode.json')
            .yaWaitForVisible(PO.source())
            .assertView('punycode', PO.source());
    });
});
