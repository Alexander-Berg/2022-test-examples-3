specs({
    feature: 'Feed',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид', function() {
        return this.browser
            .url('/turbo?stub=feed%2Fplain.json')
            .yaWaitForVisible(PO.page())
            .assertView('plain', PO.feed());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид без эскиза', function() {
        return this.browser
            .url('/turbo?stub=feed%2Fno-thumb.json')
            .yaWaitForVisible(PO.page())
            .assertView('no-thumb', PO.feed());
    });

    hermione.only.notIn('safari13');
    it('Проверка счётчиков', function() {
        return this.browser
            .url('/turbo?stub=feed%2Fplain.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckBaobabCounter(PO.feed.firstChild(), {
                path: '$page.$main.$result.feed.link'
            });
    });
});
