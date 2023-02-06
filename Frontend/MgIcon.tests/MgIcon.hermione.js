specs({
    feature: 'MgIcon',
}, () => {
    hermione.only.notIn('safari13');
    it('Иконки размера S', function() {
        return this.browser
            .url('/turbo?stub=mgicon/size_s.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.mgIcons());
    });

    hermione.only.notIn('safari13');
    it('Иконки размера M', function() {
        return this.browser
            .url('/turbo?stub=mgicon/size_m.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.mgIcons());
    });
});
