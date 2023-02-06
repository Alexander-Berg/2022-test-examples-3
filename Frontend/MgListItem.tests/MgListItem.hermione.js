specs({
    feature: 'MgListItem',
}, () => {
    hermione.only.notIn('safari13');
    it('Размеры', function() {
        return this.browser
            .url('/turbo?stub=mglistitem/sizes.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.mgListItems());
    });

    hermione.only.notIn('safari13');
    it('Иконки слева', function() {
        return this.browser
            .url('/turbo?stub=mglistitem/icon-left.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.mgListItems());
    });

    hermione.only.notIn('safari13');
    it('Иконки справа', function() {
        return this.browser
            .url('/turbo?stub=mglistitem/icon-right.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.mgListItems());
    });

    hermione.only.notIn('safari13');
    it('Иконки с обеих сторон', function() {
        return this.browser
            .url('/turbo?stub=mglistitem/icon-both.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.mgListItems());
    });
});
