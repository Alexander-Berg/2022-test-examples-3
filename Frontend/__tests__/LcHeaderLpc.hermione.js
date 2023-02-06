specs({
    feature: 'LcHeaderLpc',
}, () => {
    hermione.only.notIn('safari13');
    it('Default', function() {
        return this.browser
            .url('/turbo?stub=lcheaderlpc/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcHeaderLpc());
    });

    hermione.only.notIn('safari13');
    it('Fixed', function() {
        return this.browser
            .url('/turbo?stub=lcheaderlpc/fixed.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            // фиксированная шапка рендерится через портал
            // из-за этого селектор из PO формируется неправильно
            .assertView('plain', ['.lc-header-lpc__fixed-wrapper']);
    });

    hermione.only.notIn('safari13');
    it('Fixed with transparency at the top', function() {
        return this.browser
            .url('/turbo?stub=lcheaderlpc/fixed-with-transparency.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', ['.lc-header-lpc__fixed-wrapper']);
    });

    hermione.only.notIn('safari13');
    it('User login unauthorised', function() {
        return this.browser
            .url('/turbo?stub=lcheaderlpc/user-login.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcHeaderLpc());
    });

    hermione.only.notIn('safari13');
    it('User login', function() {
        return this.browser
            .url('/turbo?stub=lcheaderlpc/user.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcHeaderLpc());
    });

    hermione.only.notIn('safari13');
    it('Hidden login', function() {
        return this.browser
            .url('/turbo?stub=lcheaderlpc/user-hidden.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcHeaderLpc());
    });
});
