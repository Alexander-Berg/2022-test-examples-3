specs({
    feature: 'Меню с тегами',
}, () => {
    hermione.only.in(['chrome-desktop']);
    hermione.only.notIn('safari13');
    it('Внешний вид меню с тегами', function() {
        return this.browser
            .url('?stub=newsnavigationmenu/subrubric.json')
            .yaWaitForVisible('.news-navigation-menu')
            .windowHandleSize({ width: 1800, height: 770 })
            .assertView('layout-1800', '.news-navigation-menu')
            .windowHandleSize({ width: 1000, height: 770 })
            .assertView('layout-1000', '.news-navigation-menu')
            .windowHandleSize({ width: 700, height: 770 })
            .assertView('layout-700', '.news-navigation-menu');
    });
});
