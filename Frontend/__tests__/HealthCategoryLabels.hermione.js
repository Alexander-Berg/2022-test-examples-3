specs({
    feature: 'Яндекс.Здоровье',
    type: 'Поиск по тегам',
}, () => {
    hermione.only.notIn('safari13');
    it('Плашка тегов на странице поиска по тегу', function() {
        return this.browser
            .url('/turbo?stub=healthcategorylabels/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', '.health-category-labels');
    });
});
