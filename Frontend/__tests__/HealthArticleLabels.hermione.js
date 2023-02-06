specs({
    feature: 'Яндекс.Здоровье',
}, () => {
    hermione.only.notIn('safari13');
    it('Список тегов под статьей', function() {
        return this.browser
            .url('/turbo?stub=healtharticlelabels/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', '.health-article-labels');
    });
});
