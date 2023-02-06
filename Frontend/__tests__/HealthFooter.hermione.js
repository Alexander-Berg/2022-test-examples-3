specs({
    feature: 'Яндекс.Здоровье',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид футера', function() {
        return this.browser
            .url('/turbo?stub=healthfooter/source.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.healthFooter());
    });
});
