specs({
    feature: 'Панель навигации',
}, () => {
    it('Базовый вид блока', function() {
        return this.browser
            .url('/turbo?stub=navigationmenu/default.json')
            .yaWaitForVisible('.turbo-navigation-menu', 'Блок не появился')
            .assertView('plain', PO.page());
    });
});
