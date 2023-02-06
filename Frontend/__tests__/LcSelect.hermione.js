specs({
    feature: 'LcSelect',
}, () => {
    hermione.only.notIn('safari13');
    it('Обычный селект', function() {
        return this.browser
            .url('/turbo?stub=lcselect/default.json')
            .yaWaitForVisible(PO.lcSelect(), 'Селект не появился')
            .assertView('plain', PO.lcSelect());
    });

    hermione.only.notIn('safari13');
    it('Селект с выбранным вариантом', function() {
        return this.browser
            .url('/turbo?stub=lcselect/selected.json')
            .yaWaitForVisible(PO.lcSelect(), 'Селект не появился')
            .assertView('selected', PO.lcSelect());
    });

    hermione.only.notIn('safari13');
    it('Невалидный селект', function() {
        return this.browser
            .url('/turbo?stub=lcselect/invalid.json')
            .yaWaitForVisible(PO.lcSelect(), 'Селект не появился')
            .assertView('invalid', PO.lcSelect());
    });

    hermione.only.in(['chrome-desktop', 'firefox']);
    hermione.only.notIn('safari13');
    it('Открытый селект на десктопе', function() {
        return this.browser
            .url('/turbo?stub=lcselect/selected.json')
            .yaWaitForVisible(PO.lcSelect.desktop(), 'Селект не появился')
            .click(PO.lcSelect.desktop())
            .yaWaitForVisible(PO.lcSelect.desktopMenu(), 'Меню селекта не появилось')
            .assertView('opened-desktop', PO.page());
    });
});
