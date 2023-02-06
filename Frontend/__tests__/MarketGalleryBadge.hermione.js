specs({
    feature: 'marketGalleryBadge',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид бейджа type_new', function() {
        return this.browser
            .url('/turbo?stub=marketgallerybadge%2Fnew.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('new', PO.blocks.marketGalleryBadge());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид бейджа type_customer-choice', function() {
        return this.browser
            .url('/turbo?stub=marketgallerybadge%2Fcustomer-choise.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('customer-choice', PO.blocks.marketGalleryBadge());
    });
});
