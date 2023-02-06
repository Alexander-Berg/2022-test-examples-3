const PO = require('../../../../page-objects/pages/vacancy');

/**
 * Открывает страницу редактирования вакансии
 * @param {Object} browser
 * @param {String} url урл создания вакансии
 * @returns {Object}
 */
function openUrl(browser, url) {
    return browser
        .conditionalLogin('marat')
        .preparePageExtended(url, [2022, 4, 7])
        .waitForVisible(PO.fPageVacancy.actions())
        .disableAnimations('.input_theme_normal .input__box:before')
        .disableFiScrollTo()
        .assertView('vacancy_action_update', PO.fPageVacancy.actions.actionActionUpdate());
}

describe('Вакансии / Редактирование', function() {
    it('Редактирование вакансии в статусе На согласовании', function() {
        return this.browser
            .then(() => openUrl(this.browser, '/vacancies/57736/'))
            .click(PO.fPageVacancy.actions.actionActionUpdate())
            .waitForVisible(PO.vacancyForm())
            .assertView('form', PO.vacancyForm())
            .setReactSFieldValue(PO.vacancyForm.title(), 'Новая вакансия', 'input')
            .addReactSuggestValue({
                block: PO.vacancyForm.interviewers(),
                text: 'marat',
                position: 1,
                clickToFocus: true,
            })
            .addReactSuggestValue({
                block: PO.vacancyForm.observers(),
                text: 'marat',
                position: 1,
                clickToFocus: true,
            })
            .setReactSFieldValue(PO.vacancyForm.publicationTitle(), 'Новая вакансия', 'input')
            .setReactSFieldValue(PO.vacancyForm.publicationContent(), 'Новая вакансия', 'textarea')
            .setReactSFieldValue(PO.vacancyForm.isPublished(), 'true', 'radio')
            .assertView('form_filled', PO.vacancyForm())
            .click(PO.vacancyForm.submit())
            .waitForVisible(PO.fPageVacancy.actions())
            .assertView('vacancy_edited', PO.fPageVacancy());
    });
    it('Редактирование вакансии в статусе В работе', function() {
        return this.browser
            .then(() => openUrl(this.browser, '/vacancies/57672/'))
            .click(PO.fPageVacancy.actions.actionActionUpdate())
            .waitForVisible(PO.vacancyForm())
            .assertView('form', PO.vacancyForm())
            .setReactSFieldValue(PO.vacancyForm.isHidden(), 'true', 'radio')
            .addReactSuggestValue({
                block: PO.vacancyForm.hiringManager(),
                text: 'bakuta-ad',
                position: 1,
                clickToFocus: true,
            })
            .addReactSuggestValue({
                block: PO.vacancyForm.mainRecruiter(),
                text: 'bakuta-ad',
                position: 1,
                clickToFocus: true,
            })
            .addReactSuggestValue({
                block: PO.vacancyForm.recruiters(),
                text: 'bakuta-ad',
                position: 1,
                clickToFocus: true,
            })
            .addReactSuggestValue({
                block: PO.vacancyForm.responsibles(),
                text: 'bakuta-ad',
                position: 1,
                clickToFocus: true,
            })
            .scroll(PO.vacancyForm.profType())
            .setReactSFieldValue(PO.vacancyForm.proLevelMax(), 4, 'select')
            .assertView('form_filled', PO.vacancyForm())
            .click(PO.vacancyForm.submit())
            .waitForVisible(PO.fPageVacancy.actions())
            .assertView('vacancy_edited', PO.fPageVacancy());
    });
    it('Редактирование вакансии в статусе Приостановлена', function() {
        return this.browser
            .then(() => openUrl(this.browser, '/vacancies/57654/'))
            .click(PO.fPageVacancy.actions.actionActionUpdate())
            .waitForVisible(PO.vacancyForm())
            .assertView('form', PO.vacancyForm())
            .click(PO.vacancyForm.submit())
            .waitForVisible(PO.fPageVacancy.actions())
            .assertView('vacancy_edited', PO.fPageVacancy());
    });
    it('Редактирование вакансии в статусе Оффер принят', function() {
        return this.browser
            .then(() => openUrl(this.browser, '/vacancies/57569/'))
            .click(PO.fPageVacancy.actions.actionActionUpdate())
            .waitForVisible(PO.vacancyForm())
            .assertView('form', PO.vacancyForm())
            .click(PO.vacancyForm.reset())
            .waitForVisible(PO.fPageVacancy.actions())
            .assertView('vacancy_edited', PO.fPageVacancy());
    });
    it('Редактирование вакансии в статусе Делаем оффер', function() {
        return this.browser
            .then(() => openUrl(this.browser, '/vacancies/57678/'))
            .click(PO.fPageVacancy.actions.actionActionUpdate())
            .waitForVisible(PO.vacancyForm())
            .assertView('form', PO.vacancyForm());
    });
});
