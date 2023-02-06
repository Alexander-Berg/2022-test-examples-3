function patchStyle(vacancySelector) {
    const style = document.createElement('style');

    style.setAttribute('type', 'text/css');
    style.innerHTML = vacancySelector + '{ margin: 0 !important; }';

    document.body.appendChild(style);
}

specs({
    feature: 'LcJobsVacancyCard',
}, () => {
    hermione.only.notIn('safari13');
    it('default', function() {
        return this.browser
            .url('turbo?stub=lcjobsvacancycard/default.json')
            .execute(patchStyle, PO.lcJobsVacancyCard())
            // no hover
            .moveToObject('body', 0, 300)
            .yaWaitForVisible(PO.lcJobsVacancyCard(), 15000, 'Вакансия не появилась')
            .assertView('default', PO.lcJobsVacancyCard());
    });

    hermione.only.notIn('safari13');
    it('hovered', function() {
        return this.browser
            .url('turbo?stub=lcjobsvacancycard/default.json')
            .execute(patchStyle, PO.lcJobsVacancyCard())
            .moveToObject(PO.lcJobsVacancyCard())
            .yaWaitForVisible(PO.lcJobsVacancyCard(), 15000, 'Вакансия не появилась')
            // { allowViewportOverflow: true } for iphone
            .assertView('hovered', PO.lcJobsVacancyCard(), { allowViewportOverflow: true });
    });
});
