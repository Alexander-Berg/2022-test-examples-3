specs({
    feature: 'Яндекс.Здоровье',
    type: 'Статья',
}, () => {
    hermione.only.notIn('safari13');
    it('Рецензент', function() {
        return this.browser
            .url('/turbo?stub=healthexpertremark/review-default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('review', PO.healthExpertRemark());
    });

    hermione.only.notIn('safari13');
    it('Автор', function() {
        return this.browser
            .url('/turbo?stub=healthexpertremark/author.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('author', PO.healthExpertRemark());
    });
});
