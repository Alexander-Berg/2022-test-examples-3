specs({
    feature: 'LcJobsFiltersWrapper',
}, () => {
    hermione.skip.in(['iphone', 'chrome-phone', 'searchapp'], 'Проверяем отображение только в десктопных браузерах');
    hermione.only.notIn('safari13');
    it('Базовый вид', function() {
        return this.browser
            .url('turbo?stub=lcjobsfilterswrapper/default.json')
            .yaWaitForVisible(PO.lcJobsFiltersWrapper(), 'Обертка для фильтров не появилась')
            .assertView('default', PO.lcJobsFiltersWrapper());
    });

    hermione.skip.in(['chrome-desktop', 'firefox'], 'Проверяем отображение только в мобильных браузерах');
    hermione.only.notIn('safari13');
    it('Мобильный вид', function() {
        return this.browser
            .url('turbo?stub=lcjobsfilterswrapper/default.json')
            .yaWaitForVisible(PO.lcJobsContainer(), 'Обертка для фильтров не появилась')
            .assertView('default', PO.lcJobsContainer())
            .click(PO.lcJobsFiltersWrapper.filtersButton())
            .pause(1000)
            .assertView('open_filters', PO.lcJobsFiltersWrapper());
    });

    it('Недостаточно данных', function() {
        return this.browser
            .url('turbo?stub=lcjobsfilterswrapper/with_error.json')
            .yaWaitForVisible(PO.lcJobsFiltersWrapper(), 'Обертка для фильтров не появилась')
            .assertView('no_data', PO.lcJobsFiltersWrapper(), { allowViewportOverflow: true });
    });
});
