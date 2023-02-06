specs({
    feature: 'LcJobsActualVacancies',
}, () => {
    it('Базовый вид', function() {
        return this.browser
            .url('turbo?stub=lcjobsactualvacancies/default.json')
            .assertView('default', PO.lcJobsActualVacancies(), { allowViewportOverflow: true });
    });

    it('С сервисом', function() {
        return this.browser
            .url('turbo?stub=lcjobsactualvacancies/with_services.json')
            .assertView('with_services', PO.lcJobsActualVacancies());
    });
});
