specs({
    feature: 'MgUserpic',
}, () => {
    hermione.only.notIn('safari13');
    it('Наличие аватарки', function() {
        return this.browser
            .url('/turbo?stub=mguserpic/default.json')
            .yaWaitForVisible(PO.mgUserpics(), 'Страница не загрузилась')
            .assertView('plain', PO.mgUserpics());
    });

    hermione.only.notIn('safari13');
    it('Отсутствие аватарки', function() {
        return this.browser
            .url('/turbo?stub=mguserpic/stub.json')
            .yaWaitForVisible(PO.mgUserpics(), 'Страница не загрузилась')
            .assertView('plain', PO.mgUserpics());
    });

    hermione.only.notIn('safari13');
    it('Размеры аватарок', function() {
        return this.browser
            .url('/turbo?stub=mguserpic/sizes.json')
            .yaWaitForVisible(PO.mgUserpics(), 'Страница не загрузилась')
            .assertView('plain', PO.mgUserpics());
    });

    hermione.only.notIn('safari13');
    it('Формы аватарок', function() {
        return this.browser
            .url('/turbo?stub=mguserpic/shapes.json')
            .yaWaitForVisible(PO.mgUserpics(), 'Страница не загрузилась')
            .assertView('plain', PO.mgUserpics());
    });

    hermione.only.notIn('safari13');
    it('Аватарки удаленных пользователей', function() {
        return this.browser
            .url('/turbo?stub=mguserpic/banned.json')
            .yaWaitForVisible(PO.mgUserpics(), 'Страница не загрузилась')
            .assertView('plain', PO.mgUserpics());
    });
});
