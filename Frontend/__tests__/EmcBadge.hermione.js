specs({
    feature: 'EmcBadge',
}, () => {
    hermione.only.notIn('safari13');
    it('Бейдж Apple', function() {
        return this.browser
            .url('/turbo?stub=emcbadge/apple.json')
            .yaWaitForVisible(PO.emcBadge(), 'Страница не загрузилась')
            .assertView('plain', PO.emcBadge());
    });

    hermione.only.notIn('safari13');
    it('Бейдж Google', function() {
        return this.browser
            .url('/turbo?stub=emcbadge/google.json')
            .yaWaitForVisible(PO.emcBadge(), 'Страница не загрузилась')
            .assertView('plain', PO.emcBadge());
    });

    hermione.only.notIn('safari13');
    it('Бейдж Microsoft', function() {
        return this.browser
            .url('/turbo?stub=emcbadge/microsoft.json')
            .yaWaitForVisible(PO.emcBadge(), 'Страница не загрузилась')
            .assertView('plain', PO.emcBadge());
    });

    hermione.only.notIn('safari13');
    it('Бейдж Huawei', function() {
        return this.browser
            .url('/turbo?stub=emcbadge/huawei.json')
            .yaWaitForVisible(PO.emcBadge(), 'Страница не загрузилась')
            .assertView('plain', PO.emcBadge());
    });

    hermione.only.notIn('safari13');
    it('Бейдж Windows', function() {
        return this.browser
            .url('/turbo?stub=emcbadge/windows.json')
            .yaWaitForVisible(PO.emcBadge(), 'Страница не загрузилась')
            .assertView('plain', PO.emcBadge());
    });
});
