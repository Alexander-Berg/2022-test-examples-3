specs({
    feature: 'LcJobsIndication',
}, () => {
    hermione.only.notIn(['safari13']);
    it('Базовый вид', function() {
        return this.browser
            .url('turbo?stub=lcjobsindication/default.json')
            .yaWaitForVisible(PO.lcJobsIndication(), 'Блок не появился')
            .assertView('default', PO.lcJobsIndication());
    });
});
