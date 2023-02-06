function test(page) {
    return function() {
        return this.browser
            .url(`/turbo?stub=lcbreadcrumbs/${page}.json`)
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView(page, PO.lcBreadcrumbs());
    };
}

specs({
    feature: 'LcBreadcrumbs',
}, () => {
    hermione.only.notIn('safari13');
    it('Хлебные крошки', test('default'));
    hermione.only.notIn('safari13');
    it('Одна крошка', test('oneItem'));
    hermione.only.notIn('safari13');
    it('Много крошек', test('manyItems'));
});
