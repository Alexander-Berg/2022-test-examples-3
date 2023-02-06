hermione.skip.in(['searchapp', 'chrome-phone'], 'Тесты проходят нестабильно. Появляются артефакты');
specs({
    feature: 'LcEventsProgram',
}, () => {
    hermione.only.notIn('safari13');
    it('Программа событий', function() {
        return this.browser
            .url('/turbo?stub=lceventsprogram/default.json')
            .yaWaitForVisible(PO.page(), 'Cтраница не загрузилась')
            .assertView('plain', PO.lcEventsProgram());
    });
    hermione.only.notIn('safari13');
    it('Программа событий отсортированная по дате', function() {
        return this.browser
            .url('/turbo?stub=lceventsprogram/sort-by-start-date.json')
            .yaWaitForVisible(PO.page(), 'Cтраница не загрузилась')
            .assertView('plain', PO.lcEventsProgram());
    });
    hermione.only.notIn('safari13');
    it('Программа событий с материалами', function() {
        return this.browser
            .url('/turbo?stub=lceventsprogram/materials-published.json')
            .yaWaitForVisible(PO.page(), 'Cтраница не загрузилась')
            .assertView('plain', PO.lcEventsProgram());
    });
});
