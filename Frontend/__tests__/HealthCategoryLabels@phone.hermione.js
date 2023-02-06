specs({
    feature: 'Яндекс.Здоровье',
    type: 'Поиск по тегам',
}, () => {
    hermione.only.in(['chrome-phone'], 'setOrientation() проверяем только в chrome-phone');
    hermione.only.notIn('safari13');
    it('Плашка тегов на странице поиска по тегу в горизонтальной ориентации', function() {
        return this.browser
            .url('/turbo?stub=healthcategorylabels/default.json')
            .setOrientation('landscape')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', '.health-category-labels');
    });
});
