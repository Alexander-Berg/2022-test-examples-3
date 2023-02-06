describe('Страница со списком доступных приложений', () => {
    it('Внешний вид должен соответствовать шаблонному', function() {
        /* alias: 1-view */
        return this.browser
            // зайти в раздел со списком приложений
            .login({ retpath: '/portal/downloads' })
            .waitForExist('.download-item__mail')
            // внешний вид [downloads]
            .assertView('downloads', 'body');
    });
});
