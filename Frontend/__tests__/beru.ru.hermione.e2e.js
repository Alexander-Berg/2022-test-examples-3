describe('Beru.ru', function() {
    it('Сервис открывается', function() {
        return this.browser
            .yaOpenTurboService({
                query: { text: 'https://beru.ru/product/1731009938' },
                service: 'turbo',
            })
            .yaWaitForVisible(PO.blocks.beruHeader());
    });
});
