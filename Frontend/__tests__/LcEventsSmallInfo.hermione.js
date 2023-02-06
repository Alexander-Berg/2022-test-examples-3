specs({
    feature: 'LcEventsSmallInfo',
}, () => {
    hermione.only.notIn('safari13');
    it('Дефолтная секция инфо (мини)', function() {
        return this.browser
            .url('/turbo?stub=lceventssmallinfo/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcEventsSmallInfo());
    });
    hermione.only.notIn('safari13');
    it('Cекция инфо (мини) с открытой регистрацией', function() {
        return this.browser
            .url('/turbo?stub=lceventssmallinfo/registration-opened.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcEventsSmallInfo());
    });
});
