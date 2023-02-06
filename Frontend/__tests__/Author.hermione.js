specs({
    feature: 'Author',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид', function() {
        return this.browser
            .url('/turbo?stub=author/default.json')
            .yaWaitForVisible(PO.author())
            .assertView('plain', PO.author());
    });
});
