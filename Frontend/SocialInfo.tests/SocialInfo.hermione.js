specs({
    feature: 'SocialInfo',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=socialinfo/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', '.turbo-social-info');
    });

    hermione.only.notIn('safari13');
    it('Внешний вид даты на автомордах', function() {
        return this.browser
            .url('/turbo?stub=socialinfo/autoface.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', '.date');
    });
});
