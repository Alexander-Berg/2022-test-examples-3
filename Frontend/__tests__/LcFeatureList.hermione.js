specs({
    feature: 'LcFeatureList',
}, () => {
    describe('Количество колонок', function() {
        hermione.only.notIn('safari13');
        it('одна', function() {
            return this.browser
                .url('/turbo?stub=lcfeaturelist/one_column.json')
                .yaWaitForVisible(PO.page(), 'Блок не загрузился')
                .assertView('plain', PO.page());
        });

        hermione.only.notIn('safari13');
        it('две', function() {
            return this.browser
                .url('/turbo?stub=lcfeaturelist/two_columns.json')
                .yaWaitForVisible(PO.page(), 'Блок не загрузился')
                .assertView('plain', PO.page());
        });

        hermione.only.notIn('safari13');
        it('три', function() {
            return this.browser
                .url('/turbo?stub=lcfeaturelist/three_columns.json')
                .yaWaitForVisible(PO.page(), 'Блок не загрузился')
                .assertView('plain', PO.page());
        });

        hermione.only.notIn('safari13');
        it('четыре', function() {
            return this.browser
                .url('/turbo?stub=lcfeaturelist/four_columns.json')
                .yaWaitForVisible(PO.page(), 'Блок не загрузился')
                .assertView('plain', PO.page());
        });
    });

    hermione.only.notIn('safari13');
    it('Стилистическое оформление', function() {
        return this.browser
            .url('/turbo?stub=lcfeaturelist/styles.json')
            .yaWaitForVisible(PO.page(), 'Блок не загрузился')
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Без изображений', function() {
        return this.browser
            .url('/turbo?stub=lcfeaturelist/two_columns_without_image.json')
            .yaWaitForVisible(PO.page(), 'Блок не загрузился')
            .assertView('plain', PO.page());
    });
});
