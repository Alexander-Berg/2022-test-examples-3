specs({
    feature: 'EmcLink',
}, () => {
    hermione.only.notIn('safari13');
    it('Cсылка в письме', function() {
        return this.browser
            .url('/turbo?stub=emclink/default.json')
            .yaWaitForVisible(PO.emcLink(), 'Страница не загрузилась')
            .assertView('plain', PO.emcLink());
    });
});
