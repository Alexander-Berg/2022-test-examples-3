specs({
    feature: 'Панель навигации',
}, () => {
    hermione.only.notIn('safari13');
    it('Базовый вид блока', function() {
        return this.browser
            .url('/turbo?stub=navigationmenu/default.json')
            .yaWaitForVisible('.turbo-navigation-menu', 'Блок не появился')
            .assertView('plain', PO.page());
    });
    hermione.only.notIn('safari13');
    it('С подрубрикатором', function() {
        return this.browser
            .url('/turbo?stub=navigationmenu/subrubric.json')
            .yaWaitForVisible('.turbo-navigation-menu_theme_subrubric', 'Блок не появился')
            .assertView('withSubMenu', [
                '.turbo-navigation-menu__wrapper_theme_classic',
                '.turbo-navigation-menu__wrapper_theme_subrubric',
            ]);
    });
});
