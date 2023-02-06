specs({
    feature: 'EmcGroup',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид группы с несколькими секциями', function() {
        return this.browser
            .url('/turbo?stub=emcgroup/default.json')
            .yaWaitForVisible(PO.emcGroup(), 'Группа не появилась')
            .assertView('emcgroup', PO.emcGroup());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид группы с фоновым цветом и различными настройками отступов', function() {
        return this.browser
            .url('/turbo?stub=emcgroup/offsets.json')
            .yaWaitForVisible(PO.emcPage(), 'Страница не загрузилась')
            .assertView('emcgroup', PO.emcPage());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид группы с различными настройками размера фоновой картинки', function() {
        return this.browser
            .url('/turbo?stub=emcgroup/sizes.json')
            .yaWaitForVisible(PO.emcPage(), 'Страница не загрузилась')
            .assertView('emcgroup', PO.emcPage());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид группы с различными настройками позиционирования фоновой картинки', function() {
        return this.browser
            .url('/turbo?stub=emcgroup/positions.json')
            .yaWaitForVisible(PO.emcPage(), 'Страница не загрузилась')
            .assertView('emcgroup', PO.emcPage());
    });
});
