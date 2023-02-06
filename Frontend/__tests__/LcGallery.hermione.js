specs({
    feature: 'LcGallery',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=lcgallery/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGallery());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока без описания', function() {
        return this.browser
            .url('/turbo?stub=lcgallery/emptyDescription.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGallery());
    });
});
