specs({
    feature: 'beruBreadcrumbs',
}, () => {
    hermione.only.notIn('safari13');
    it('Базовый вид блока', function() {
        return this.browser
            .url('/turbo?stub=berubreadcrumbs/default.json')
            .yaWaitForVisible(PO.blocks.beruBreadcrumbs(), 'Блок не появился')
            .assertView('default', PO.blocks.beruBreadcrumbs());
    });
    hermione.only.notIn('safari13');
    it('Проверка кликабельности блока с хлебной крошкой', function() {
        return this.browser
            .url('/turbo?stub=berubreadcrumbs/default.json')
            .yaWaitForVisible(PO.page(), 'Блок не появился')
            .yaCheckLinkOpener(
                PO.blocks.beruBreadcrumbs(),
                'Блок должен быть кликабельный и открываться в новом окне'
            )
            .then(url => {
                assert.include(url.href, 'https://touch.bluemarket.fslb.beru.ru', 'Неверная ссылка');
            });
    });
});
