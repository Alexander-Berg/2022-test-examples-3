specs({
    feature: 'EmcSocial',
}, () => {
    hermione.only.notIn('safari13');
    it('Выравнивание по левому краю, квадратные черные иконки, c отступами и фоном секции', function() {
        return this.browser
            .url('/turbo?stub=emcsocial/black-square-icon.json')
            .yaWaitForVisible(PO.emcSocial(), 'Соцсети не появились')
            .assertView('emcsocial', PO.emcSocial());
    });

    hermione.only.notIn('safari13');
    it('Выравнивание по левому краю, круглые цветные иконки и текст', function() {
        return this.browser
            .url('/turbo?stub=emcsocial/colorful-round-icon-and-text.json')
            .yaWaitForVisible(PO.emcSocial(), 'Соцсети не появились')
            .assertView('emcsocial', PO.emcSocial());
    });

    hermione.only.notIn('safari13');
    it('Выравнивание по левому краю, квадратные цветные иконки', function() {
        return this.browser
            .url('/turbo?stub=emcsocial/colorful-square-icon.json')
            .yaWaitForVisible(PO.emcSocial(), 'Соцсети не появились')
            .assertView('emcsocial', PO.emcSocial());
    });

    hermione.only.notIn('safari13');
    it('Выравнивание по центру, текст', function() {
        return this.browser
            .url('/turbo?stub=emcsocial/text.json')
            .yaWaitForVisible(PO.emcSocial(), 'Соцсети не появились')
            .assertView('emcsocial', PO.emcSocial());
    });

    hermione.only.notIn('safari13');
    it('Выравнивание по левому краю, текст', function() {
        return this.browser
            .url('/turbo?stub=emcsocial/text-left.json')
            .yaWaitForVisible(PO.emcSocial(), 'Соцсети не появились')
            .assertView('emcsocial', PO.emcSocial());
    });

    hermione.only.notIn('safari13');
    it('Выравнивание по центру, круглые белые иконки', function() {
        return this.browser
            .url('/turbo?stub=emcsocial/white-round-icon.json')
            .yaWaitForVisible(PO.emcSocial(), 'Соцсети не появились')
            .assertView('emcsocial', PO.emcSocial());
    });

    hermione.only.notIn('safari13');
    it('Секция без заголовка', function() {
        return this.browser
            .url('/turbo?stub=emcsocial/wout-title.json')
            .yaWaitForVisible(PO.emcSocial(), 'Соцсети не появились')
            .assertView('emcsocial', PO.emcSocial());
    });

    hermione.only.notIn('safari13');
    it('Секция с большим числом сервисов', function() {
        return this.browser
            .url('/turbo?stub=emcsocial/a-lot-of-items.json')
            .yaWaitForVisible(PO.emcSocial(), 'Соцсети не появились')
            .assertView('emcsocial', PO.emcSocial());
    });

    hermione.only.notIn('safari13');
    it('Выравнивание по центру, emc-rich-text', function() {
        return this.browser
            .url('/turbo?stub=emcsocial/emc-rich-text.json')
            .yaWaitForVisible(PO.emcSocial(), 'Соцсети не появились')
            .assertView('emcsocial', PO.emcSocial());
    });

    hermione.only.notIn('safari13');
    it('Выравнивание по левому краю, квадратные черные иконки, c отступами и фоном секции (в колонке)', function() {
        return this.browser
            .url('/turbo?stub=emcsocial/black-square-icon-columns.json')
            .yaWaitForVisible(PO.emcSocial(), 'Соцсети не появились')
            .assertView('emcsocial', PO.emcSocial());
    });

    hermione.only.notIn('safari13');
    it('Выравнивание по левому краю, круглые цветные иконки и текст (в колонке)', function() {
        return this.browser
            .url('/turbo?stub=emcsocial/colorful-round-icon-and-text-columns.json')
            .yaWaitForVisible(PO.emcSocial(), 'Соцсети не появились')
            .assertView('emcsocial', PO.emcSocial());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид секций с различными настройками размера иконок', function() {
        return this.browser
            .url('/turbo?stub=emcsocial/sizes.json')
            .yaWaitForVisible(PO.emcPage(), 'Страница не появилась')
            .assertView('emcsocial', PO.emcPage());
    });
});
