specs({
    feature: 'beru-logo',
}, () => {
    beforeEach(function() {
        return this.browser.url('/turbo?stub=berulogo/default.json');
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Проверка кликабельности блока', function() {
        return this.browser
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckLinkOpener(
                PO.blocks.clickableBeruLogo(),
                'Логотип должен быть кликабельный и открываться в новом окне',
                { target: '_blank' }
            )
            .then(url => {
                assert.include(url.href, 'https://m.beru.ru', 'Неверная ссылка');
            });
    });
});
