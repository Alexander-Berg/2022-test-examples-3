function patchStyle() {
    const selector = '.page__content';

    const style = document.createElement('style');

    style.setAttribute('type', 'text/css');
    style.innerHTML = selector + '{ margin: 0 !important; }';

    document.body.appendChild(style);
}

specs({
    feature: 'LcJobsVacancy',
}, () => {
    hermione.only.notIn(['safari13']);
    it('default', function() {
        return this.browser
            .url('turbo?stub=lcjobsvacancy/default.json')
            .execute(patchStyle)
            .yaWaitForVisible(PO.lcJobsVacancy(), 'Вакансия не появилась')
            .assertView('default', PO.lcJobsVacancy());
    });

    it('withOffsets', function() {
        return this.browser
            .url('turbo?stub=lcjobsvacancy/withOffsets.json')
            .execute(patchStyle)
            .yaWaitForVisible(PO.lcJobsVacancy(), 'Вакансия не появилась')
            .assertView('withOffsets', PO.lcJobsVacancy());
    });
});
