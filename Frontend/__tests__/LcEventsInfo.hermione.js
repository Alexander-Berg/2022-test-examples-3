specs({
    feature: 'LcEventsInfo',
}, () => {
    hermione.only.notIn('safari13');
    it('Дефолтная секция инфо', function() {
        return this.browser
            .url('/turbo?stub=lceventsinfo/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcEventsInfo());
    });
    hermione.only.notIn('safari13');
    it('Cекция инфо с открытой регистрацией', function() {
        return this.browser
            .url('/turbo?stub=lceventsinfo/registration-opened.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcEventsInfo());
    });
    hermione.only.notIn('safari13');
    it('Cекция инфо с трансляцией и чатом', function() {
        return this.browser
            .url('/turbo?stub=lceventsinfo/with-translation.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcEventsInfo(), {
                ignoreElements: ['.lc-video-block__video-object', '.ya-chat-widget'],
            });
    });
});
