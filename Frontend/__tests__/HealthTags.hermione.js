specs({
    feature: 'Яндекс.Здоровье',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока тегов', function() {
        return this.browser
            .url('/turbo?stub=healthtags/source.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.healthTags());
    });
});
