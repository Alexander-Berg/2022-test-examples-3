specs({
    feature: 'Meta',
}, () => {
    hermione.only.notIn('safari13');
    it('Основные проверки', function() {
        return this.browser
            .url('?stub=meta/on-react.json')
            .yaWaitForVisible(PO.turboMeta())
            .assertView('plain', PO.page());
    });
});
