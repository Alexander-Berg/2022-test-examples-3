specs({
    feature: 'LcCustomHtml',
}, () => {
    hermione.only.notIn('safari13');
    it('Рендер LcCustomHtml', function() {
        return this.browser.url('/turbo?stub=lccustomhtml/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcCustomHtml(), 'Компонент не появился');
    });
});
