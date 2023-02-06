specs({
    feature: 'highlight',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид', function() {
        return this.browser
            .url('?stub=highlight/default.json')
            .yaWaitForVisible(PO.blocks.paragraph())
            .assertView('plain', PO.blocks.page());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид подсветки опечатки (type misspell)', function() {
        return this.browser
            .url('?stub=highlight/type-misspell.json')
            .yaWaitForVisible(PO.blocks.paragraph())
            .assertView('plain', PO.blocks.page());
    });
});
