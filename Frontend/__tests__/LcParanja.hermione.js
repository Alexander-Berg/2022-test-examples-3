hermione.skip.in(['chrome-phone', 'searchapp'], 'Тесты проходят нестабильно.');
specs({
    feature: 'LcParanja',
}, () => {
    hermione.only.notIn('safari13');
    it('Обычная паранджа', function() {
        return this.browser
            .url('/turbo?stub=lcparanja/default.json')
            .yaWaitForVisible(PO.page(), 'Паранджа не загрузилась')
            .assertView('plain', PO.lcParanja());
    });
    hermione.only.notIn('safari13');
    it('Паранджа для загрузки в Chrome c логотипом в футере', function() {
        return this.browser
            .url('/turbo?stub=lcparanja/chromeDwnldAndFooterLogo.json')
            .yaWaitForVisible(PO.page(), 'Паранджа не загрузилась')
            .assertView('plain', PO.lcParanja());
    });
    hermione.only.notIn('safari13');
    it('Паранджа для расширения в Chrome c логотипом в шапке', function() {
        return this.browser
            .url('/turbo?stub=lcparanja/chromeExtAndHeaderLogo.json')
            .yaWaitForVisible(PO.page(), 'Паранджа не загрузилась')
            .assertView('plain', PO.lcParanja());
    });
    hermione.only.notIn('safari13');
    it('Паранджа для Edge c логотипом в тексте', function() {
        return this.browser
            .url('/turbo?stub=lcparanja/edgeTextLogo.json')
            .yaWaitForVisible(PO.page(), 'Паранджа не загрузилась')
            .assertView('plain', PO.lcParanja());
    });
    hermione.only.notIn('safari13');
    it('Паранджа для Firefox c логотипом в тексте', function() {
        return this.browser
            .url('/turbo?stub=lcparanja/firefoxDwnldAndTextLogo.json')
            .yaWaitForVisible(PO.page(), 'Паранджа не загрузилась')
            .assertView('plain', PO.lcParanja());
    });
    hermione.only.notIn('safari13');
    it('Паранджа для Firefox c маленькой стрелкой и логотипом в шапке', function() {
        return this.browser
            .url('/turbo?stub=lcparanja/firefoxDwnldSmllAndHeaderLogo.json')
            .yaWaitForVisible(PO.page(), 'Паранджа не загрузилась')
            .assertView('plain', PO.lcParanja());
    });
    hermione.only.notIn('safari13');
    it('Паранджа для IE c логотипом в футере', function() {
        return this.browser
            .url('/turbo?stub=lcparanja/ieAndFooterLogo.json')
            .yaWaitForVisible(PO.page(), 'Паранджа не загрузилась')
            .assertView('plain', PO.lcParanja());
    });
    hermione.only.notIn('safari13');
    it('Паранджа для Opera c логотипом в футере', function() {
        return this.browser
            .url('/turbo?stub=lcparanja/operaAndFooterLogo.json')
            .yaWaitForVisible(PO.page(), 'Паранджа не загрузилась')
            .assertView('plain', PO.lcParanja());
    });
});
