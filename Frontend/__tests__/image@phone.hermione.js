specs({
    feature: 'Картинка',
}, () => {
    describe('Бесконечная лента', () => {
        hermione.only.notIn('safari13');
        it('Первая картинка во второй статье в pd20', function() {
            return this.browser
                .url('/turbo?text=https://www.drive.ru/brands/bmw/models/2011/6_series_coupe&hermione_no-lazy=1')
                .yaWaitForVisible(PO.page(), 'Страница должна загрузиться')
                .yaIndexify(PO.page.result())
                .yaScrollPageToBottom()
                .yaWaitForVisible(PO.page.result1(), 'Не загрузилась вторая статья')
                .assertView('plain', PO.page.result1.media());
        });
    });
});
