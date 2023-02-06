specs({
    feature: 'LcJobsInfoblock',
}, () => {
    hermione.only.notIn(['safari13']);
    it('Базовый вид', function() {
        return this.browser
            .url('turbo?stub=lcjobsinfoblock/default.json')
            .yaWaitForVisible(PO.lcJobsInfoblock(), 'Инфоблок не появился')
            .assertView('default', PO.lcJobsInfoblock());
    });

    it('Компактный вид', function() {
        return this.browser
            .url('turbo?stub=lcjobsinfoblock/compact.json')
            .yaWaitForVisible(PO.lcJobsInfoblock(), 'Инфоблок не появился')
            .assertView('compact', PO.lcJobsInfoblock());
    });
});
