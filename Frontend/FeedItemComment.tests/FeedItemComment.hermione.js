specs({
    feature: 'FeedItemComment',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=feeditemcomment/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', '.turbo-feed-item-comment');
    });
});
