specs({
    feature: 'LcLink',
}, () => {
    hermione.only.notIn('safari13');
    it('Обычная ссылка', function() {
        return this.browser
            .url('/turbo?stub=lclink/default.json')
            .yaWaitForVisible(PO.lcLink(), 'Ссылка не появилась')
            .assertView('plain', PO.lcLink());
    });

    hermione.only.notIn('safari13');
    it('Ссылка с контентом', function() {
        return this.browser
            .url('/turbo?stub=lclink/with-children.json')
            .yaWaitForVisible(PO.lcLink(), 'Ссылка не появилась')
            .assertView('plain', PO.lcLink());
    });

    hermione.only.notIn('safari13');
    it('Проверка target ссылки', function() {
        return this.browser
            .url('/turbo?stub=lclink/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckLinkOpener(
                PO.lcLink(),
                'Ссылка должна быть кликабельной и открываться в новом окне',
                { target: '_blank' }
            )
            .then(url => {
                assert.include(url.href, 'https://yandex.ru', 'Неверная ссылка');
            });
    });
});
