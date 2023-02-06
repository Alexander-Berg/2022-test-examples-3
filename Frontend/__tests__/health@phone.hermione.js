hermione.skip.in(['iphone'], 'на iphone постоянно появляются несколько скроллов');
specs({
    feature: 'Яндекс.Здоровье',
}, () => {
    hermione.only.in(['chrome-phone'], 'setOrientation() проверяем только в chrome-phone');
    hermione.only.notIn('safari13');
    it('Статья в горизонтальной ориентации', function() {
        return this.browser
            .url('?stub=health/article.json&brand=health&hermione_no-lazy=1')
            .setOrientation('landscape')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Список статей', function() {
        return this.browser
            .url('?stub=health/articles.json&brand=health&exp_flags=adv-disabled=0&hermione_advert=stub&hermione_no-lazy=1')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitAdvert(PO, 'Реклама не загрузилась')
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Страница "Не найдено"', function() {
        return this.browser
            .url('?stub=health/not-found.json&brand=health&exp_flags=adv-disabled=0&hermione_advert=stub&hermione_no-lazy=1')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitAdvert(PO, 'Реклама не загрузилась')
            .assertView('not-found', PO.page());
    });
});
