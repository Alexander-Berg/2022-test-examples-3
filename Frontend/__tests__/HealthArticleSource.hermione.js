specs({
    feature: 'Яндекс.Здоровье',
    type: 'Статья',
}, () => {
    hermione.only.notIn('safari13');
    it('Источник со стандартным лого', function() {
        return this.browser
            .url('/turbo?stub=healtharticlesource/source.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('health-article-source', PO.healthArticleSource());
    });

    hermione.only.notIn('safari13');
    it('Источник с лого из списка избранных', function() {
        return this.browser
            .url('/turbo?stub=healtharticlesource/favorite.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('health-article-source', PO.healthArticleSource());
    });
});
