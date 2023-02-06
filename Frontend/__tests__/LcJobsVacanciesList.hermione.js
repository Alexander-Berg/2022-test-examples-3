function patchStyle(vacancySelector) {
    const style = document.createElement('style');

    style.setAttribute('type', 'text/css');
    style.innerHTML = vacancySelector + '{ margin: 0 !important; }';

    document.body.appendChild(style);
}

specs({
    feature: 'LcJobsVacanciesList',
}, () => {
    hermione.only.notIn('safari13');
    it('default', function() {
        return this.browser
            .url('turbo?stub=lcjobsvacancieslist/default.json')
            .execute(patchStyle, PO.lcJobsVacancyCard())
            .moveToObject(PO.lcJobsVacancyCard())
            .yaWaitForVisible(PO.lcJobsVacanciesList(), 'Список вакансий не появился')
            .assertView('default', PO.lcJobsVacanciesList());
    });
});
