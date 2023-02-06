specs({
    feature: 'LcEventsMap',
}, () => {
    hermione.only.notIn('safari13');
    it('Карта Москвы', function() {
        return this.browser
            .url('/turbo?stub=lceventsmap/moscow.json')
            .yaWaitForVisible(PO.lcEventsMap(), 'Карта не появилась')
            .assertView('moscow', PO.lcEventsMap(), {
                ignoreElements: PO.lcEventsMap.mapWrapper(),
            });
    });

    hermione.only.notIn('safari13');
    it('Карта Нижнего Новгорода', function() {
        return this.browser
            .url('/turbo?stub=lceventsmap/nn.json')
            .yaWaitForVisible(PO.lcEventsMap(), 'Карта не появилась')
            .assertView('nn', PO.lcEventsMap(), {
                ignoreElements: PO.lcEventsMap.mapWrapper(),
            });
    });
});
