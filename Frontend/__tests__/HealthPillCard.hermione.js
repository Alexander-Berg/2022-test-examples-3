specs({
    feature: 'Яндекс.Здоровье',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид карточки лекарств', function() {
        return this.browser
            .url('/turbo?stub=healthpillcard/source.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.healthPillCard());
    });
});
