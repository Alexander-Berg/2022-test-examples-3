specs({
    feature: 'LcLabel',
}, () => {
    hermione.only.notIn('safari13');
    it('Обычный лейбл', function() {
        return this.browser
            .url('/turbo?stub=lclabel/default.json')
            .yaWaitForVisible(PO.lcLabel(), 'Лейбл не появился')
            .assertView('plain', PO.lcLabel());
    });

    hermione.only.notIn('safari13');
    it('Required лейбл', function() {
        return this.browser
            .url('/turbo?stub=lclabel/required.json')
            .yaWaitForVisible(PO.lcLabel(), 'Лейбл не появился')
            .assertView('required', PO.lcLabel());
    });
});
