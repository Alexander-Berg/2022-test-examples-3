describe('Главная страница админки', () => {
    it('проверка открытия страницы', function() {
        return this.browser
            .yaLogin()
            .yaOpenPage('/admin/')
            .waitForExist(PO.AdminPanel());
    });
});
