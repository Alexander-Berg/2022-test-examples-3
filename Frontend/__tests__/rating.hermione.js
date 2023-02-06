specs({
    feature: 'rating',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид из json', function() {
        return this.browser
            .url('/turbo?stub=rating/lots-of-stars.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид из xml', function() {
        return this.browser
            .url('/turbo?stub=rating/default.xml')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });

    hermione.only.in('chrome-phone', 'setOrientation() используем только в chrome-phone');
    hermione.only.notIn('safari13');
    it('Внешний вид из xml [ландшафтная ориентация]', function() {
        return this.browser
            .url('/turbo?stub=rating/default.xml')
            .setOrientation('landscape')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });

    hermione.only.in('chrome-phone', 'setOrientation() используем только в chrome-phone');
    hermione.only.notIn('safari13');
    it('Внешний вид из json [ландшафтная ориентация]', function() {
        return this.browser
            .url('/turbo?stub=rating/lots-of-stars.json')
            .setOrientation('landscape')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });
});
