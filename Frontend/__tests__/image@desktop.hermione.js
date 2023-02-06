specs({
    feature: 'Картинка',
}, () => {
    describe('Бесконечная лента', () => {
        hermione.only.in('chrome-desktop');
        it('Первая картинка во второй статье в pd20', function() {
            return this.browser
                .url('/turbo?text=https://www.drive.ru/brands/bmw/models/2011/6_series_coupe')
                .yaIndexify(PO.page.result())
                .yaWaitForVisible(PO.page.result1(), 'Не загрузилась вторая статья')
                .assertView('plain', PO.page.result1.media());
        });
    });
});
