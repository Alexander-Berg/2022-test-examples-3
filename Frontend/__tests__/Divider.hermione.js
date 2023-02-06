specs({
    feature: 'divider',
}, () => {
    hermione.only.notIn('safari13');
    it('Рендеринг Разделителя', function() {
        return this.browser
            .url('/turbo?stub=divider/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('default', PO.hermioneContainer());
    });

    hermione.only.notIn('safari13');
    it('Внутри Cover', function() {
        return this.browser
            .url('/turbo?stub=divider/inside-cover.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('inside-cover', PO.hermioneContainer());
    });

    hermione.only.notIn('safari13');
    it('Кастомная Тема', function() {
        return this.browser
            .url('/turbo?stub=divider/custom-theme.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('custom-theme', PO.hermioneContainer());
    });

    hermione.only.notIn('safari13');
    it('Кастомный CSS', function() {
        return this.browser
            .url('/turbo?stub=divider/custom-css.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('custom-css', PO.hermioneContainer());
    });
});
