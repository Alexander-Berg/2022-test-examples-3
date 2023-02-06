specs({
    feature: 'EmcLogo',
}, () => {
    hermione.only.notIn('safari13');
    it('Логотип + иконка', function() {
        return this.browser
            .url('/turbo?stub=emclogo/default.json')
            .yaWaitForVisible(PO.emcLogo(), 'Логотип не появился')
            .assertView('emclogo', PO.emcLogo());
    });

    hermione.only.notIn('safari13');
    it('Логотип + иконка(темная тема)', function() {
        return this.browser
            .url('/turbo?stub=emclogo/dark-logo-icon.json')
            .yaWaitForVisible(PO.emcLogo(), 'Логотип не появился')
            .assertView('emclogo', PO.emcLogo());
    });

    hermione.only.notIn('safari13');
    it('Логотип + иконка по центру', function() {
        return this.browser
            .url('/turbo?stub=emclogo/logo-icon-center.json')
            .yaWaitForVisible(PO.emcLogo(), 'Логотип не появился')
            .assertView('emclogo', PO.emcLogo());
    });

    hermione.only.notIn('safari13');
    it('Логотип + иконка (компактная версия)', function() {
        return this.browser
            .url('/turbo?stub=emclogo/logo-icon-compact-version.json')
            .yaWaitForVisible(PO.emcLogo(), 'Логотип не появился')
            .assertView('emclogo', PO.emcLogo());
    });

    hermione.only.notIn('safari13');
    it('Логотип + иконка (первая буква красная)', function() {
        return this.browser
            .url('/turbo?stub=emclogo/logo-icon-first-letter.json')
            .yaWaitForVisible(PO.emcLogo(), 'Логотип не появился')
            .assertView('emclogo', PO.emcLogo());
    });

    hermione.only.notIn('safari13');
    it('Логотип по правому краю', function() {
        return this.browser
            .url('/turbo?stub=emclogo/logo-right.json')
            .yaWaitForVisible(PO.emcLogo(), 'Логотип не появился')
            .assertView('emclogo', PO.emcLogo());
    });

    hermione.only.notIn('safari13');
    it('Логотип по центру(темная тема)', function() {
        return this.browser
            .url('/turbo?stub=emclogo/dark-logo-center.json')
            .yaWaitForVisible(PO.emcLogo(), 'Логотип не появился')
            .assertView('emclogo', PO.emcLogo());
    });

    hermione.only.notIn('safari13');
    it('Иконка алисы по левому краю', function() {
        return this.browser
            .url('/turbo?stub=emclogo/icon-left.json')
            .yaWaitForVisible(PO.emcLogo(), 'Логотип не появился')
            .assertView('emclogo', PO.emcLogo());
    });

    hermione.only.notIn('safari13');
    it('Логотип + иконка (в колонке)', function() {
        return this.browser
            .url('/turbo?stub=emclogo/default-columns.json')
            .yaWaitForVisible(PO.emcLogo(), 'Логотип не появился')
            .assertView('emclogo', PO.emcLogo());
    });

    hermione.only.notIn('safari13');
    it('Логотип + иконка по центру (в колонке)', function() {
        return this.browser
            .url('/turbo?stub=emclogo/logo-icon-center-columns.json')
            .yaWaitForVisible(PO.emcLogo(), 'Логотип не появился')
            .assertView('emclogo', PO.emcLogo());
    });
});
