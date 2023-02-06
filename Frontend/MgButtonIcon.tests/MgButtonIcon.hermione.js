specs({
    feature: 'MgButtonIcon',
}, () => {
    hermione.only.notIn('safari13');
    it('Размеры', function() {
        return this.browser
            .url('/turbo?stub=mgbuttonicon/size.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.mgButtonIcons());
    });

    hermione.only.notIn('safari13');
    it('Темы', function() {
        return this.browser
            .url('/turbo?stub=mgbuttonicon/theme.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.mgButtonIcons());
    });

    hermione.only.notIn('safari13');
    it('Скругленность', function() {
        return this.browser
            .url('/turbo?stub=mgbuttonicon/pin.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.mgButtonIcons());
    });

    hermione.only.notIn('safari13');
    it('Disabled', function() {
        return this.browser
            .url('/turbo?stub=mgbuttonicon/disabled.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.mgButtonIcons());
    });

    hermione.only.notIn('safari13');
    it('Appearance', function() {
        return this.browser
            .url('/turbo?stub=mgbuttonicon/appearance.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.mgButtonIcons());
    });
});
