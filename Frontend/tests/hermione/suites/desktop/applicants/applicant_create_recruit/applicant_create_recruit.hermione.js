const { setAjaxHash } = require('../../../../helpers');

const PO = require('../../../../page-objects/pages/candidate');

function openForm(browser) {
    return browser
        .conditionalLogin('marat')
        .preparePage('', '/candidates/200015381/applications')
        .disableAnimations('.modal__content')
        .disableAnimations('.modal_theme_normal')
        .disableAnimations('.input_theme_normal .input__box:before')
        .waitForVisible(PO.applicationCreateButton())
        .assertView('application_create_button', PO.applicationCreateButton())
        .click(PO.applicationCreateButton())
        .waitForVisible(PO.applicationCreateForm())
        .waitForHidden(PO.aplicationCreateFormSpinner())
        .assertView('application_create_form', PO.applicationCreateForm());
}
describe('Претендент / Создание.Рекрутер', function() {
    it('Проверка обязательных полей', function() {
        return this.browser
            .then(() => openForm(this.browser))
            .click(PO.applicationCreateForm.submit())
            .waitForVisible(PO.applicationCreateForm.vacancies.error())
            .assertView('application_create_form_error', PO.applicationCreateForm());
    });
    it('Добавление кандидата на вакансию', function() {
        const VAC_ID = '52149'; // должна быть в работе

        return this.browser
            .then(() => openForm(this.browser))
            .setSuggestValue({
                block: PO.applicationCreateForm.vacancies.input(),
                menu: PO.vacancySuggest.items(),
                text: VAC_ID,
                item: PO.vacancySuggest.selectedVacancy(),
            })
            // Чтобы надежно спрятать выпадающий список вакансий
            .click(PO.applicationCreateForm.header())
            .click(PO.applicationCreateForm.createActivated.yes())
            .assertView('application_create_form_filled', PO.applicationCreateForm())
            .execute(setAjaxHash, 'after_application_created')
            .setFixedDateTime()
            .click(PO.applicationCreateForm.submit())
            .waitForHidden(PO.applicationCreateForm())
            .waitForVisible(PO.actualApplications.firstApplication())
            .waitUntil(() => {
                return this.browser
                    .getText(PO.actualApplications.firstApplication.vacancy.id())
                    .then(text => {
                        return text === `VAC ${VAC_ID}`;
                    });
            }, 0, 'П-во не появилось в списке актуальных П-в', 100)
            .waitUntil(() => {
                return this.browser
                    .getText(PO.applPane.activeConsiderationApplications.firstApplication.vacancy.id())
                    .then(text => {
                        return text === `VAC ${VAC_ID}`;
                    });
            }, 0, 'П-во не появилось в списке П-в в табе Вакансии', 100)
            .assertView('application_created', [
                PO.actualApplications(),
                PO.applPane.activeConsiderationApplications(),
            ]);
    });
});
