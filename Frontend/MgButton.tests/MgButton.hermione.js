specs({
    feature: 'MgButton',
}, () => {
    function checkSize(browser, theme) {
        return browser.url(`/turbo?stub=mgbutton/theme-${theme}.json`)
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    }

    hermione.only.notIn('safari13');
    it('Тема Secondary', function() {
        return checkSize(this.browser, 'secondary');
    });

    hermione.only.notIn('safari13');
    it('Тема Primary', function() {
        return checkSize(this.browser, 'primary');
    });

    hermione.only.notIn('safari13');
    it('Тема Clear', function() {
        return checkSize(this.browser, 'clear');
    });

    hermione.only.notIn('safari13');
    it('Тема Secondary dark', function() {
        return checkSize(this.browser, 'secondary-dark');
    });

    hermione.only.notIn('safari13');
    it('Тема Primary dark', function() {
        return checkSize(this.browser, 'primary-dark');
    });

    hermione.only.notIn('safari13');
    it('Тема Clear dark', function() {
        return checkSize(this.browser, 'clear-dark');
    });
});
