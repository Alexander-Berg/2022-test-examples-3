specs({
    feature: 'Яндекс.Здоровье',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид фильтров', async function() {
        await this.browser
            .url('/turbo?stub=healthstatfilter/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.healthContainer());
    });
});
