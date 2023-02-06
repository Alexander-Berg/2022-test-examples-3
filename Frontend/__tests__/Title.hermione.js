specs({
    feature: 'Title',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид', function() {
        return this.browser
            .url('?stub=title/default.json')
            .yaWaitForVisible(PO.page())
            .assertView('plain', PO.page());
    });
});
