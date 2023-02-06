hermione.only.in(['iphone', 'chrome-desktop'], 'Ускоряем браузеронезависимые тесты');

specs({
    feature: 'newsHeader',
}, () => {
    hermione.only.notIn('safari13');
    it('Видна шапка для спортивной вертикали', function() {
        return this.browser
            .url('/turbo?stub=newsheader/sport.json')
            .yaWaitForVisible(PO.blocks.newsHeader(), 'Блок не появился')
            .assertView('plain', PO.blocks.newsHeader());
    });

    hermione.only.notIn('safari13');
    it('Видна шапка для турбо-сюжета', function() {
        return this.browser
            .url('/turbo?stub=newsheader/turbo.json')
            .yaWaitForVisible(PO.blocks.newsHeader(), 'Блок не появился')
            .assertView('plain', PO.blocks.newsHeader());
    });
});
