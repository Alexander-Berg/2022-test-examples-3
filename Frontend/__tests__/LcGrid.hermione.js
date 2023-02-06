hermione.only.in(['chrome-desktop', 'firefox']);
specs({
    feature: 'LcGrid',
}, () => {
    hermione.only.notIn('safari13');
    it('Секция LcGrid', function() {
        return this.browser
            .url('/turbo?stub=lcgrid/default.json')
            .yaWaitForVisible(PO.lcGridPattern(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGrid());
    });

    hermione.only.notIn('safari13');
    it('Секция LcGrid в режиме not-edit-mode', function() {
        return this.browser
            .url('/turbo?stub=lcgrid/not-edit-mode.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGrid());
    });

    hermione.only.notIn('safari13');
    it('Кастомный размер gaps', function() {
        return this.browser
            .url('/turbo?stub=lcgrid/custom-gaps.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGrid());
    });

    hermione.only.notIn('safari13');
    it('Кастомный паттерн', function() {
        return this.browser
            .url('/turbo?stub=lcgrid/custom-pattern.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGrid());
    });
});
