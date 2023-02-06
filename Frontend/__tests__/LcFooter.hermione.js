specs({
    feature: 'LcFooter',
}, () => {
    var setDefaultFont = function() {
        const element = document.createElement('style');
        element.setAttribute('type', 'text/css');
        element.appendChild(document.createTextNode(
            ' .lc-footer { font-family: sans-serif !important; }'
        ));
        document.head.appendChild(element);
    };

    hermione.skip.in(['chrome-phone', 'searchapp'], 'Тесты проходят нестабильно. Появляются артефакты');
    hermione.only.notIn('safari13');
    it('Внешний вид блока default', function() {
        return this.browser
            .url('/turbo?stub=lcfooter/default.json')
            .yaWaitForVisible(PO.lcFooter(), 'Блок не загрузился')
            .execute(setDefaultFont)
            .assertView('plain', PO.lcFooter());
    });

    hermione.skip.in(['chrome-phone', 'searchapp'], 'Тесты проходят нестабильно. Появляются артефакты');
    hermione.only.notIn('safari13');
    it('Внешний вид блока short', function() {
        return this.browser
            .url('/turbo?stub=lcfooter/short.json')
            .yaWaitForVisible(PO.lcFooter(), 'Блок не загрузился')
            .execute(setDefaultFont)
            .assertView('plain', PO.lcFooter());
    });

    hermione.skip.in(['chrome-phone', 'searchapp'], 'Тесты проходят нестабильно. Появляются артефакты');
    hermione.only.notIn('safari13');
    it('Внешний вид блока full', function() {
        return this.browser
            .url('/turbo?stub=lcfooter/full.json')
            .yaWaitForVisible(PO.lcFooter(), 'Блок не загрузился')
            .execute(setDefaultFont)
            .assertView('plain', PO.lcFooter());
    });

    hermione.skip.in(['chrome-phone', 'searchapp'], 'Тесты проходят нестабильно. Появляются артефакты');
    hermione.only.notIn('safari13');
    it('Внешний вид блока full-big-menu', function() {
        return this.browser
            .url('/turbo?stub=lcfooter/full-big-menu.json')
            .yaWaitForVisible(PO.lcFooter(), 'Блок не загрузился')
            .execute(setDefaultFont)
            .assertView('plain', PO.lcFooter());
    });

    hermione.skip.in(['chrome-phone', 'searchapp'], 'Тесты проходят нестабильно. Появляются артефакты');
    hermione.only.notIn('safari13');
    it('Внешний вид блока full-light', function() {
        return this.browser
            .url('/turbo?stub=lcfooter/full-light.json')
            .yaWaitForVisible(PO.lcFooter(), 'Блок не загрузился')
            .execute(setDefaultFont)
            .assertView('plain', PO.lcFooter());
    });

    hermione.skip.in(['chrome-phone', 'searchapp'], 'Тесты проходят нестабильно. Появляются артефакты');
    hermione.only.notIn('safari13');
    it('Внешний вид блока full-light-flat-menu', function() {
        return this.browser
            .url('/turbo?stub=lcfooter/full-light-flat-menu.json')
            .yaWaitForVisible(PO.lcFooter(), 'Блок не загрузился')
            .execute(setDefaultFont)
            .assertView('plain', PO.lcFooter());
    });

    hermione.skip.in(['chrome-phone', 'searchapp'], 'Тесты проходят нестабильно. Появляются артефакты');
    hermione.only.notIn('safari13');
    it('Внешний вид блока full-small-menu', function() {
        return this.browser
            .url('/turbo?stub=lcfooter/full-small-menu.json')
            .yaWaitForVisible(PO.lcFooter(), 'Блок не загрузился')
            .execute(setDefaultFont)
            .assertView('plain', PO.lcFooter());
    });

    hermione.skip.in(['chrome-phone', 'searchapp'], 'Тесты проходят нестабильно. Появляются артефакты');
    hermione.only.notIn('safari13');
    it('Внешний вид блока minimal-light', function() {
        return this.browser
            .url('/turbo?stub=lcfooter/minimal-light.json')
            .yaWaitForVisible(PO.lcFooter(), 'Блок не загрузился')
            .execute(setDefaultFont)
            .assertView('plain', PO.lcFooter());
    });
});
