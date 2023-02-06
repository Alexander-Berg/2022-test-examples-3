specs({
    feature: 'LcGood',
}, () => {
    it('Внешний вид блока. Размер XS, desktop', function() {
        return this.browser
            .url('/turbo?stub=lcgood/desktopSizeXS.json')
            .yaWaitForVisible(PO.lcGood(), 'Блок не загрузился')
            .assertView('plain', PO.lcGood())
            .yaCheckClientErrors();
    });

    it('Внешний вид блока. Размер S, desktop', function() {
        return this.browser
            .url('/turbo?stub=lcgood/desktopSizeS.json')
            .yaWaitForVisible(PO.lcGood(), 'Блок не загрузился')
            .assertView('plain', PO.lcGood())
            .yaCheckClientErrors();
    });

    it('Внешний вид блока. Размер M, desktop', function() {
        return this.browser
            .url('/turbo?stub=lcgood/desktopSizeM.json')
            .yaWaitForVisible(PO.lcGood(), 'Блок не загрузился')
            .assertView('plain', PO.lcGood())
            .yaCheckClientErrors();
    });

    it('Внешний вид блока. Размер L, desktop', function() {
        return this.browser
            .url('/turbo?stub=lcgood/desktopSizeL.json')
            .yaWaitForVisible(PO.lcGood(), 'Блок не загрузился')
            .assertView('plain', PO.lcGood())
            .yaCheckClientErrors();
    });
});
