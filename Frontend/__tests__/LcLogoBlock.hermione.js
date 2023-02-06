specs({
    feature: 'LcLogoBlock',
}, () => {
    hermione.only.notIn('safari13');
    it('Логотип и иконка', function() {
        return this.browser
            .url('/turbo?stub=lclogoblock/logo-and-icon.json')
            .yaWaitForVisible(PO.page(), 'Логотип и иконка не загрузились')
            .assertView('logo-and-icon', PO.lcLogoBlock());
    });

    hermione.only.notIn('safari13');
    it('Только логотип', function() {
        return this.browser
            .url('/turbo?stub=lclogoblock/logo.json')
            .yaWaitForVisible(PO.page(), 'Логотип не загрузился')
            .assertView('logo', PO.lcLogoBlock());
    });

    hermione.only.notIn('safari13');
    it('Светлый логотип', function() {
        return this.browser
            .url('/turbo?stub=lclogoblock/logo-dark-theme.json')
            .yaWaitForVisible(PO.page(), 'Логотип не загрузился')
            .assertView('logo', PO.lcLogoBlock());
    });

    hermione.only.notIn('safari13');
    it('Светлый логотип - первая буква красная', function() {
        return this.browser
            .url('/turbo?stub=lclogoblock/logo-first-letter-red.json')
            .yaWaitForVisible(PO.page(), 'Логотип не загрузился')
            .assertView('logo', PO.lcLogoBlock());
    });

    hermione.only.notIn('safari13');
    it('Логотип + иконка (компактная версия)', function() {
        return this.browser
            .url('/turbo?stub=lclogoblock/logo-icon-compact-version.json')
            .yaWaitForVisible(PO.page(), 'Логотип не появился')
            .assertView('logo', PO.lcLogoBlock());
    });

    hermione.only.notIn('safari13');
    it('Новая версия логотипа', function() {
        return this.browser
            .url('/turbo?stub=lclogoblock/new-logo-icon.json')
            .yaWaitForVisible(PO.page(), 'Логотип не загрузился')
            .assertView('logo', PO.lcLogoBlock());
    });

    hermione.only.notIn('safari13');
    it('Только иконка', function() {
        return this.browser
            .url('/turbo?stub=lclogoblock/icon.json')
            .yaWaitForVisible(PO.page(), 'Иконка не загрузилась')
            .assertView('icon', PO.lcLogoBlock());
    });
});
