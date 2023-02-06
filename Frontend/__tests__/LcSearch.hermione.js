function test(page) {
    return function() {
        return this.browser
            .url(`/turbo?stub=lcsearch/${page}.json`)
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaScrollPageToBottom()
            .yaScrollPage(PO.lcSearch())
            .assertView(page, PO.lcSearch());
    };
}

specs({
    feature: 'LcSearch',
}, () => {
    hermione.only.notIn('safari13');
    it('Маленький размер', test('small'));
    hermione.only.notIn('safari13');
    it('Большой размер', test('large'));
});
