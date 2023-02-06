hermione.skip.in(/.*/); // TODO расскипать после LPC-11753

specs({
    feature: 'LcGrid2',
}, () => {
    it('Секция LcGrid2', function() {
        return this.browser
            .url('/turbo?stub=lcgrid2/default.json')
            .yaWaitForVisible(PO.lcGrid2(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGrid2());
    });
});
