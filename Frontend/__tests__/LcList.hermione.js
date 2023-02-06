function test(page) {
    return function() {
        return this.browser
            .url(`/turbo?stub=lclist/${page}.json`)
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaScrollPageToBottom()
            .yaScrollPage(PO.lcList())
            .assertView(page, PO.lcList());
    };
}

specs({
    feature: 'LcList',
}, () => {
    hermione.only.notIn('safari13');
    it('Нумерованный список', test('numerable'));
    hermione.only.notIn('safari13');
    it('Чекбоксы', test('ticks'));
    hermione.only.notIn('safari13');
    it('Булиты', test('bullets'));
    hermione.only.notIn('safari13');
    it('Свое изображение', test('custom'));
    hermione.only.notIn('safari13');
    it('Вертикальный нумерованный список по центру', test('numerable-vertical'));
    hermione.only.notIn('safari13');
    it('Вертикальные чекбоксы', test('ticks-vertical'));
    hermione.only.notIn('safari13');
    it('Большое свое изображение', test('custom-large'));
    hermione.only.notIn('safari13');
    it('Вертикальное свое изображение', test('custom-vertical'));
});
